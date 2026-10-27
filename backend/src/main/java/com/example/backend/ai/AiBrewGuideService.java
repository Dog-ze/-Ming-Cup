package com.example.backend.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.ai.dto.BrewGuideRequest;
import com.example.backend.ai.vo.BrewGuideVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.Tea;
import com.example.backend.enums.FlavorPreference;
import com.example.backend.enums.InsomniaStatus;
import com.example.backend.enums.StrengthPreference;
import com.example.backend.enums.TeaPurpose;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.point.PointService;
import com.example.backend.security.CurrentUser;
import com.example.backend.weather.WeatherService;
import com.example.backend.weather.vo.WeatherResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 读取当前用户的真实问卷、天气和启用茶叶资料，再交由 AI 生成日常泡茶建议。
 */
@Service
public class AiBrewGuideService {
    private final TeaMapper teaMapper;
    private final QuestionnaireMapper questionnaireMapper;
    private final WeatherService weatherService;
    private final DeepSeekClient deepSeekClient;
    private final AiBrewGuideRateLimiter rateLimiter;
    private final PointService pointService;
    private final AiProperties properties;

    public AiBrewGuideService(TeaMapper teaMapper, QuestionnaireMapper questionnaireMapper, WeatherService weatherService,
                              DeepSeekClient deepSeekClient, AiBrewGuideRateLimiter rateLimiter, PointService pointService) {
        this(teaMapper, questionnaireMapper, weatherService, deepSeekClient, rateLimiter, pointService, new AiProperties());
    }

    @Autowired
    public AiBrewGuideService(TeaMapper teaMapper, QuestionnaireMapper questionnaireMapper, WeatherService weatherService,
                              DeepSeekClient deepSeekClient, AiBrewGuideRateLimiter rateLimiter, PointService pointService,
                              AiProperties properties) {
        this.teaMapper = teaMapper; this.questionnaireMapper = questionnaireMapper; this.weatherService = weatherService;
        this.deepSeekClient = deepSeekClient; this.rateLimiter = rateLimiter; this.pointService = pointService;
        this.properties = properties;
    }

    /**
     * 生成 AI 泡茶建议
     * @param request
     * @return
     */
    public BrewGuideVO generate(BrewGuideRequest request) {
        validateLocation(request);
        Long userId = CurrentUser.id();
        Tea tea = teaMapper.selectOne(new LambdaQueryWrapper<Tea>()
                .eq(Tea::getId, request.teaId())
                .eq(Tea::getEnabled, 1));
        if (tea == null) {
            throw new BizException(40401, "茶叶不存在或已停用");
        }

        Questionnaire questionnaire = questionnaireMapper.selectOne(
                new LambdaQueryWrapper<Questionnaire>()
                        .eq(Questionnaire::getUserId, userId)
        );
        WeatherResponse weather = weatherService.getWeather(
                request.city(), request.longitude(), request.latitude());

        rateLimiter.check(userId, "brew-guide");
        String requestId = request.requestId();
        boolean free = pointService.prepareBrewGuide(userId, requestId);
        try {
            String answer = deepSeekClient.complete(systemPrompt(), buildUserPrompt(tea, questionnaire, weather),
                    properties.getBrewGuideMaxTokens());
            if (free) pointService.completeFreeBrewGuide(userId, requestId);
            return new BrewGuideVO(tea.getId(), tea.getName(), answer);
        } catch (RuntimeException exception) {
            if (free) pointService.failFreeBrewGuide(userId, requestId); else pointService.refundBrewGuide(userId, requestId);
            throw exception;
        }
    }

    private void validateLocation(BrewGuideRequest request) {
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
                你是一盏茗 App 的日常饮茶助手。
                只能根据提供的茶叶资料生成日常泡茶建议，不能诊断疾病或承诺治疗效果。
                如果用户容易受饮茶影响睡眠，只能提醒控制饮用时间和浓度，不能声称治疗失眠。
                用户提到疾病、孕期、用药或严重不适时，应建议咨询医生或药师。
                请用简体中文回答，并包含：建议水温、投茶量、第一泡时间、后续冲泡建议和注意事项。
                直接输出易读的纯文本，不要使用 Markdown 标题、加粗或无序列表标记（例如 #、**、-）。
                """;
    }

    private String buildUserPrompt(Tea tea, Questionnaire questionnaire, WeatherResponse weather) {
        return """
                请为下面这款茶生成泡茶方式与注意事项。

                茶叶资料：
                茶名：%s
                类别：%s
                茶性：%s
                咖啡因等级：%s
                建议水温：%s℃
                建议冲泡时间：%s 秒

                当前天气：
                城市：%s
                气温：%s℃
                湿度：%s%%
                天气：%s

                用户饮茶偏好：
                %s
                """.formatted(
                tea.getName(), tea.getCategory(), tea.getNature(), tea.getCaffeine(),
                tea.getBrewTemp(), tea.getBrewTime(), weather.city(), weather.temperature(),
                weather.humidity(), weather.weatherText(), questionnaireText(questionnaire)
        );
    }

    private String questionnaireText(Questionnaire questionnaire) {
        if (questionnaire == null) {
            return "用户尚未填写问卷，请提供通用且谨慎的日常泡茶建议。";
        }

        return """
                饮茶目的：%s
                是否容易受饮茶影响睡眠：%s
                口感偏好：%s
                浓度偏好：%s
                """.formatted(
                TeaPurpose.fromCode(questionnaire.getPurpose()).getLabel(),
                InsomniaStatus.fromCode(questionnaire.getInsomnia()).getLabel(),
                FlavorPreference.fromCode(questionnaire.getFlavorPreference()).getLabel(),
                StrengthPreference.fromCode(questionnaire.getStrengthPreference()).getLabel()
        );
    }
}
