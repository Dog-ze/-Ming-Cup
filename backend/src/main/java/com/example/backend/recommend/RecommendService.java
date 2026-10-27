package com.example.backend.recommend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.Tea;
import com.example.backend.enums.ConstitutionType;
import com.example.backend.enums.FlavorPreference;
import com.example.backend.enums.InsomniaStatus;
import com.example.backend.enums.StrengthPreference;
import com.example.backend.enums.TeaPurpose;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.recommend.vo.RecommendResponseVO;
import com.example.backend.recommend.vo.RecommendationVO;
import com.example.backend.security.CurrentUser;
import com.example.backend.weather.WeatherService;
import com.example.backend.weather.vo.WeatherResponse;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class RecommendService {
    private static final int MAX_RECOMMENDATIONS = 3;

    private final WeatherService weatherService;
    private final TeaMapper teaMapper;
    private final QuestionnaireMapper questionnaireMapper;
    private final ObjectMapper objectMapper;

    /**
     * 根据当前天气和当前用户的问卷偏好，生成最多三条茶叶推荐。
     * 游客没有问卷时只按天气推荐；登录且已填写问卷时才加入个性化分数。
     *
     * @param city 前端传来的城市名称，可为空
     * @param longitude 前端传来的经度，可为空；有值时必须与 latitude 同时存在
     * @param latitude 前端传来的纬度，可为空；有值时必须与 longitude 同时存在
     * @return 天气、是否个性化以及推荐茶叶列表
     */
    public RecommendResponseVO recommend(String city, BigDecimal longitude, BigDecimal latitude) {
        WeatherResponse weather = weatherService.getWeather(city, longitude, latitude);
        Questionnaire questionnaire = findCurrentQuestionnaire();
        boolean personalized = questionnaire != null;

        List<RecommendationVO> recommendations = teaMapper.selectTopRankedEnabledTea(
                        weather.climateTag(),
                        questionnaire == null ? null : TeaPurpose.fromCode(questionnaire.getPurpose()).getLabel(),
                        questionnaire == null ? null : FlavorPreference.fromCode(questionnaire.getFlavorPreference()).getLabel(),
                        questionnaire == null ? null : StrengthPreference.fromCode(questionnaire.getStrengthPreference()).getLabel(),
                        questionnaire == null ? null : questionnaire.getConstitutionType(),
                        questionnaire != null && questionnaire.getInsomnia() == InsomniaStatus.EASY.getCode()
                ).stream()
                .map(tea -> toRecommendation(tea, weather, questionnaire))
                .toList();

        return new RecommendResponseVO(weather, personalized, recommendations);
    }

    /**
     * 读取当前登录用户已经保存的问卷。
     * 游客没有用户 ID，因此返回 null。
     *
     * @return 当前用户的问卷；游客或未填写时返回 null
     */
    private Questionnaire findCurrentQuestionnaire() {
        Long userId = CurrentUser.idOrNull();
        if (userId == null) {
            return null;
        }
        return questionnaireMapper.selectOne(
                new LambdaQueryWrapper<Questionnaire>()
                        .eq(Questionnaire::getUserId, userId)
        );
    }

    /**
     * 将一条茶叶资料转换成可以返回前端的推荐对象。
     * 此方法会同时计算分数，并生成用户能看懂的推荐理由。
     *
     * @param tea 候选茶叶
     * @param weather 本次推荐依据的实时天气
     * @param questionnaire 当前用户的问卷；游客时为 null
     * @return 一条完整的推荐结果
     */
    private RecommendationVO toRecommendation(Tea tea, WeatherResponse weather,
                                               Questionnaire questionnaire) {
        ScoreDetail detail = score(tea, weather, questionnaire);
        return new RecommendationVO(
                tea.getId(),
                tea.getName(),
                tea.getCategory(),
                tea.getImageUrl(),
                detail.score(),
                buildReason(tea, weather, questionnaire, detail)
        );
    }

    /**
     * 计算单款茶的总匹配分数，并记录口感和目的是否真正命中。
     *
     * @param tea 候选茶叶
     * @param weather 本次推荐依据的实时天气
     * @param questionnaire 当前用户问卷；游客时为 null
     * @return 分数以及用于生成理由的命中结果
     */
    private ScoreDetail score(Tea tea, WeatherResponse weather, Questionnaire questionnaire) {
        int score = weatherScore(tea, weather.climateTag());
        boolean purposeMatched = false;
        boolean flavorMatched = false;

        if (questionnaire != null) {
            String purpose = TeaPurpose.fromCode(questionnaire.getPurpose()).getLabel();
            String flavor = FlavorPreference.fromCode(questionnaire.getFlavorPreference()).getLabel();
            String strength = StrengthPreference.fromCode(questionnaire.getStrengthPreference()).getLabel();

            purposeMatched = containsTag(tea.getEffectTags(), purpose);
            flavorMatched = containsTag(tea.getFlavorTags(), flavor);
            if (purposeMatched) score += 25;
            if (flavorMatched) score += 20;
            if (strength.equals(tea.getSuitableStrength())) score += 10;

            int constitution = questionnaire.getConstitutionType();
            if (constitution == ConstitutionType.COLD_SENSITIVE.getCode() && "温".equals(tea.getNature())) {
                score += 10;
            }
            if (constitution == ConstitutionType.HEAT_SENSITIVE.getCode()
                    && ("寒".equals(tea.getNature()) || "凉".equals(tea.getNature()))) {
                score += 10;
            }
        }

        return new ScoreDetail(score, purposeMatched, flavorMatched);
    }

    /**
     * 根据天气标签为茶叶增加基础分数。
     * 这是游客和登录用户都会生效的推荐规则。
     *
     * @param tea 候选茶叶
     * @param climateTag 天气服务返回的气候标签
     * @return 天气匹配得到的分数
     */
    private int weatherScore(Tea tea, String climateTag) {
        return switch (climateTag) {
            case "hot_humid" -> inCategory(tea, "绿茶", "白茶", "花草") ? 20 : 0;
            case "hot" -> inCategory(tea, "绿茶", "白茶", "花草") ? 15 : 0;
            case "cold" -> inCategory(tea, "红茶", "黑茶") ? 20 : 0;
            case "mild" -> inCategory(tea, "青茶", "白茶") ? 10 : 0;
            default -> 0;
        };
    }

    /**
     * 判断茶叶类别是否属于给定的任意类别。
     *
     * @param tea 候选茶叶
     * @param categories 允许命中的茶叶类别
     * @return 类别命中时返回 true
     */
    private boolean inCategory(Tea tea, String... categories) {
        for (String category : categories) {
            if (category.equals(tea.getCategory())) return true;
        }
        return false;
    }

    /**
     * 从茶叶标签 JSON 数组中判断是否包含指定中文标签。
     * 标签格式错误时按未命中处理，避免单条脏数据导致整个推荐接口失败。
     *
     * @param jsonArray 茶叶表中的 JSON 标签字符串
     * @param expected 需要匹配的中文标签
     * @return 标签存在时返回 true
     */
    private boolean containsTag(String jsonArray, String expected) {
        try {
            return objectMapper.readValue(jsonArray, new TypeReference<List<String>>() { })
                    .contains(expected);
        } catch (JacksonException exception) {
            return false;
        }
    }

    /**
     * 生成展示给用户的推荐理由。
     * 只写入实际天气和确实命中的问卷偏好，不写医疗承诺。
     *
     * @param tea 被推荐的茶叶
     * @param weather 本次推荐依据的实时天气
     * @param questionnaire 当前用户问卷；游客时为 null
     * @param detail 本茶的分数和偏好命中情况
     * @return 完整、可解释的中文推荐理由
     */
    private String buildReason(Tea tea, WeatherResponse weather, Questionnaire questionnaire,
                               ScoreDetail detail) {
        List<String> parts = new ArrayList<>();
        parts.add("今天" + weather.city() + " " + weather.temperature() + "°C、湿度 "
                + weather.humidity() + "%" + "，天气" + climateLabel(weather.climateTag()));

        if (questionnaire != null && detail.flavorMatched()) {
            parts.add("你偏好" + FlavorPreference.fromCode(questionnaire.getFlavorPreference()).getLabel());
        }
        if (questionnaire != null && detail.purposeMatched()) {
            parts.add("适合你这次想要" + TeaPurpose.fromCode(questionnaire.getPurpose()).getLabel());
        }
        return String.join("；", parts) + "，因此推荐" + tea.getName() + "。";
    }

    /**
     * 将后端内部使用的英文天气标签转换为用户能看懂的中文。
     *
     * @param climateTag 例如 hot_humid、hot、cold、mild
     * @return 例如“偏湿热”“炎热”“偏冷”“温和”
     */
    private String climateLabel(String climateTag) {
        return switch (climateTag) {
            case "hot_humid" -> "偏湿热";
            case "hot" -> "炎热";
            case "cold" -> "偏冷";
            default -> "温和";
        };
    }

    private record ScoreDetail(int score, boolean purposeMatched, boolean flavorMatched) {
    }
}
