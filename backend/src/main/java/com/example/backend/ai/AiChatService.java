package com.example.backend.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.common.BizException;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.Tea;
import com.example.backend.enums.FlavorPreference;
import com.example.backend.enums.InsomniaStatus;
import com.example.backend.enums.StrengthPreference;
import com.example.backend.enums.TeaPurpose;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.weather.WeatherService;
import com.example.backend.weather.vo.WeatherResponse;
import com.example.backend.point.PointService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Consumer;

/** 为流式问茶准备可信的用户上下文，再将 AI 文字分段交给调用方。 */
@Service
public class AiChatService {
    private final TeaMapper teaMapper;
    private final QuestionnaireMapper questionnaireMapper;
    private final WeatherService weatherService;
    private final DeepSeekClient deepSeekClient;
    private final AiBrewGuideRateLimiter rateLimiter;
    private final PointService pointService;
    private final AiProperties aiProperties;

    public AiChatService(TeaMapper teaMapper, QuestionnaireMapper questionnaireMapper, WeatherService weatherService,
                         DeepSeekClient deepSeekClient, AiBrewGuideRateLimiter rateLimiter) {
        this(teaMapper, questionnaireMapper, weatherService, deepSeekClient, rateLimiter, null, new AiProperties());
    }
    @Autowired
    public AiChatService(TeaMapper teaMapper, QuestionnaireMapper questionnaireMapper, WeatherService weatherService,
                         DeepSeekClient deepSeekClient, AiBrewGuideRateLimiter rateLimiter, PointService pointService,
                         AiProperties aiProperties) {
        this.teaMapper = teaMapper; this.questionnaireMapper = questionnaireMapper; this.weatherService = weatherService;
        this.deepSeekClient = deepSeekClient; this.rateLimiter = rateLimiter; this.pointService = pointService;
        this.aiProperties = aiProperties;
    }

    public void stream(Long userId, AiChatRequest request, Consumer<String> onDelta) {
        validateLocation(request);
        Questionnaire questionnaire = questionnaireMapper.selectOne(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getUserId, userId));
        WeatherResponse weather = weatherService.getWeather(request.city(), request.longitude(), request.latitude());
        List<Tea> teas = findContextTeas(weather, questionnaire);

        rateLimiter.check(userId, "chat");
        String requestId = request.requestId();
        if (pointService != null) pointService.prepareChat(userId, requestId);
        try {
            deepSeekClient.stream(systemPrompt(), buildUserPrompt(request.message(), questionnaire, weather, teas),
                    aiProperties.getChatMaxTokens(), onDelta);
        } catch (RuntimeException exception) {
            if (pointService != null) pointService.refundChat(userId, requestId);
            throw exception;
        }
    }

    /** 与首页推荐共用排序规则，只把最匹配的三款启用茶叶交给 AI。 */
    private List<Tea> findContextTeas(WeatherResponse weather, Questionnaire questionnaire) {
        return teaMapper.selectTopRankedEnabledTea(
                weather.climateTag(),
                questionnaire == null ? null : TeaPurpose.fromCode(questionnaire.getPurpose()).getLabel(),
                questionnaire == null ? null : FlavorPreference.fromCode(questionnaire.getFlavorPreference()).getLabel(),
                questionnaire == null ? null : StrengthPreference.fromCode(questionnaire.getStrengthPreference()).getLabel(),
                questionnaire == null ? null : questionnaire.getConstitutionType(),
                questionnaire != null && questionnaire.getInsomnia() == InsomniaStatus.EASY.getCode()
        );
    }

    private void validateLocation(AiChatRequest request) {
        boolean hasCity = StringUtils.hasText(request.city());
        boolean hasLongitude = request.longitude() != null;
        boolean hasLatitude = request.latitude() != null;
        if (!hasCity && !hasLongitude && !hasLatitude) {
            throw new BizException(40001, "请提供城市，或同时提供经纬度");
        }
        if (hasLongitude != hasLatitude) {
            throw new BizException(40001, "经度和纬度需要同时提供");
        }
    }

    private String systemPrompt() {
        return """
                你是一盏茗 App 的日常饮茶助手。请根据用户问题、问卷偏好、天气和提供的茶叶库给出日常饮茶建议。
                仅能推荐提供的茶叶库中的茶；不确定时要明确说明原因，不能虚构茶叶资料。
                不要诊断疾病、承诺治疗效果或替代医生建议。用户提到疾病、孕期、用药或严重不适时，应建议咨询医生或药师。
                回答使用简体中文、纯文本、短段落。不要使用 Markdown 标题、加粗或无序列表标记。
                """;
    }

    private String buildUserPrompt(String message, Questionnaire questionnaire,
                                   WeatherResponse weather, List<Tea> teas) {
        return """
                用户问题：%s

                当前天气：%s，%s℃，湿度 %s%%，%s。

                用户饮茶偏好：
                %s

                可推荐茶叶库：
                %s
                """.formatted(message.trim(), weather.city(), weather.temperature(), weather.humidity(),
                weather.weatherText(), questionnaireText(questionnaire), teaCatalogText(teas));
    }

    private String questionnaireText(Questionnaire questionnaire) {
        if (questionnaire == null) {
            return "用户尚未填写问卷。";
        }
        return "饮茶目的：%s；睡眠影响：%s；口感偏好：%s；浓度偏好：%s。".formatted(
                TeaPurpose.fromCode(questionnaire.getPurpose()).getLabel(),
                InsomniaStatus.fromCode(questionnaire.getInsomnia()).getLabel(),
                FlavorPreference.fromCode(questionnaire.getFlavorPreference()).getLabel(),
                StrengthPreference.fromCode(questionnaire.getStrengthPreference()).getLabel()
        );
    }

    private String teaCatalogText(List<Tea> teas) {
        if (teas == null || teas.isEmpty()) {
            return "当前没有可推荐的茶叶。";
        }
        return teas.stream()
                .map(tea -> "%s（%s，茶性%s，咖啡因%s，建议%s℃、%s秒）".formatted(
                        tea.getName(), tea.getCategory(), tea.getNature(), tea.getCaffeine(),
                        tea.getBrewTemp(), tea.getBrewTime()))
                .reduce((left, right) -> left + "；" + right)
                .orElse("当前没有可推荐的茶叶。");
    }
}
