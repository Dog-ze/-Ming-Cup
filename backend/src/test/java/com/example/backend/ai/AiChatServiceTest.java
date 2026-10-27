package com.example.backend.ai;

import com.example.backend.ai.dto.AiChatRequest;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.Tea;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.weather.WeatherService;
import com.example.backend.weather.vo.WeatherResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    @Test
    void streamsDeepSeekChunksUsingTheUsersQuestionnaireWeatherAndEnabledTeaCatalog() {
        TeaMapper teaMapper = mock(TeaMapper.class);
        QuestionnaireMapper questionnaireMapper = mock(QuestionnaireMapper.class);
        WeatherService weatherService = mock(WeatherService.class);
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        AiBrewGuideRateLimiter rateLimiter = mock(AiBrewGuideRateLimiter.class);
        AiChatService service = new AiChatService(
                teaMapper, questionnaireMapper, weatherService, deepSeekClient, rateLimiter);

        when(questionnaireMapper.selectOne(any())).thenReturn(questionnaire());
        when(teaMapper.selectTopRankedEnabledTea(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(tea()));
        when(weatherService.getWeather("泉州市", null, null)).thenReturn(weather());
        List<String> receivedChunks = new ArrayList<>();
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(3);
            onDelta.accept("建议试试铁观音，");
            onDelta.accept("午后少量饮用。");
            return null;
        }).when(deepSeekClient).stream(any(), any(), anyInt(), any());

        service.stream(8L, new AiChatRequest("下午犯困，适合喝什么茶？", "泉州市", null, null), receivedChunks::add);

        assertThat(receivedChunks).containsExactly("建议试试铁观音，", "午后少量饮用。");
        verify(rateLimiter).check(8L, "chat");
        verify(deepSeekClient).stream(any(), contains("用户问题：下午犯困，适合喝什么茶？"), anyInt(), any());
        verify(deepSeekClient).stream(any(), contains("铁观音"), anyInt(), any());
    }

    private Questionnaire questionnaire() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setPurpose(0);
        questionnaire.setInsomnia(0);
        questionnaire.setFlavorPreference(0);
        questionnaire.setStrengthPreference(0);
        return questionnaire;
    }

    private Tea tea() {
        Tea tea = new Tea();
        tea.setId(12L);
        tea.setName("铁观音");
        tea.setCategory("青茶");
        tea.setNature("平");
        tea.setCaffeine("中");
        tea.setBrewTemp(95);
        tea.setBrewTime(15);
        tea.setEnabled(1);
        return tea;
    }

    private WeatherResponse weather() {
        return new WeatherResponse("泉州市", new BigDecimal("118.6"), new BigDecimal("24.9"),
                new BigDecimal("25"), 70, "多云", "mild", "live",
                OffsetDateTime.now(ZoneOffset.ofHours(8)));
    }
}
