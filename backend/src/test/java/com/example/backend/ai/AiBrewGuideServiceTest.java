package com.example.backend.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.ai.dto.BrewGuideRequest;
import com.example.backend.ai.vo.BrewGuideVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.Tea;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.point.PointService;
import com.example.backend.security.CurrentUser;
import com.example.backend.weather.WeatherService;
import com.example.backend.weather.vo.WeatherResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiBrewGuideServiceTest {

    @BeforeAll
    static void initializeEntityMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Tea.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Questionnaire.class);
    }

    @AfterAll
    static void clearEntityMetadata() {
        TableInfoHelper.remove(Tea.class);
        TableInfoHelper.remove(Questionnaire.class);
    }

    @AfterEach
    void clearCurrentUser() {
        CurrentUser.clear();
    }

    @Test
    void generatesGuideFromEnabledTeaCurrentUsersQuestionnaireAndWeather() {
        TeaMapper teaMapper = mock(TeaMapper.class);
        QuestionnaireMapper questionnaireMapper = mock(QuestionnaireMapper.class);
        WeatherService weatherService = mock(WeatherService.class);
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        AiBrewGuideRateLimiter rateLimiter = mock(AiBrewGuideRateLimiter.class);
        PointService pointService = mock(PointService.class);
        AiBrewGuideService service = new AiBrewGuideService(
                teaMapper, questionnaireMapper, weatherService, deepSeekClient, rateLimiter, pointService);
        CurrentUser.set(8L);

        when(teaMapper.selectOne(any())).thenReturn(tea());
        when(questionnaireMapper.selectOne(any())).thenReturn(questionnaire());
        when(weatherService.getWeather("泉州市", null, null)).thenReturn(weather());
        when(deepSeekClient.complete(any(), any(), anyInt())).thenReturn("建议使用 95℃ 热水冲泡。");
        when(pointService.prepareBrewGuide(any(), any())).thenReturn(true);

        BrewGuideVO result = service.generate(new BrewGuideRequest(12L, "泉州市", null, null));

        assertThat(result.teaId()).isEqualTo(12L);
        assertThat(result.teaName()).isEqualTo("铁观音");
        assertThat(result.content()).isEqualTo("建议使用 95℃ 热水冲泡。");
        verify(deepSeekClient).complete(any(), contains("茶名：铁观音"), anyInt());
        verify(deepSeekClient).complete(any(), contains("容易"), anyInt());
        verify(rateLimiter).check(8L, "brew-guide");
        verify(pointService).prepareBrewGuide(any(), any());

        ArgumentCaptor<LambdaQueryWrapper<Tea>> teaQuery = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(teaMapper).selectOne(teaQuery.capture());
        assertThat(teaQuery.getValue().getSqlSegment()).contains("id", "enabled");
        assertThat(teaQuery.getValue().getParamNameValuePairs().values()).contains(12L, 1);

        ArgumentCaptor<LambdaQueryWrapper<Questionnaire>> questionnaireQuery = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(questionnaireMapper).selectOne(questionnaireQuery.capture());
        assertThat(questionnaireQuery.getValue().getSqlSegment()).contains("user_id");
        assertThat(questionnaireQuery.getValue().getParamNameValuePairs().values()).contains(8L);
    }

    @Test
    void rejectsMissingOrDisabledTeaBeforeCallingAi() {
        TeaMapper teaMapper = mock(TeaMapper.class);
        QuestionnaireMapper questionnaireMapper = mock(QuestionnaireMapper.class);
        WeatherService weatherService = mock(WeatherService.class);
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        AiBrewGuideRateLimiter rateLimiter = mock(AiBrewGuideRateLimiter.class);
        PointService pointService = mock(PointService.class);
        AiBrewGuideService service = new AiBrewGuideService(
                teaMapper, questionnaireMapper, weatherService, deepSeekClient, rateLimiter, pointService);
        CurrentUser.set(8L);
        when(teaMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.generate(new BrewGuideRequest(99L, "泉州市", null, null)))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(40401));
        verify(deepSeekClient, never()).complete(any(), any(), anyInt());
        verify(rateLimiter, never()).check(any(), any());
    }

    @Test
    void rejectsRequestWithoutCityOrCompleteCoordinates() {
        TeaMapper teaMapper = mock(TeaMapper.class);
        QuestionnaireMapper questionnaireMapper = mock(QuestionnaireMapper.class);
        WeatherService weatherService = mock(WeatherService.class);
        DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
        AiBrewGuideRateLimiter rateLimiter = mock(AiBrewGuideRateLimiter.class);
        PointService pointService = mock(PointService.class);
        AiBrewGuideService service = new AiBrewGuideService(
                teaMapper, questionnaireMapper, weatherService, deepSeekClient, rateLimiter, pointService);
        CurrentUser.set(8L);

        assertThatThrownBy(() -> service.generate(new BrewGuideRequest(12L, null, null, null)))
                .isInstanceOfSatisfying(BizException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(40001));
        verify(teaMapper, never()).selectOne(any());
        verify(deepSeekClient, never()).complete(any(), any(), anyInt());
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

    private Questionnaire questionnaire() {
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setPurpose(0);
        questionnaire.setInsomnia(0);
        questionnaire.setFlavorPreference(0);
        questionnaire.setStrengthPreference(0);
        return questionnaire;
    }

    private WeatherResponse weather() {
        return new WeatherResponse("泉州市", new BigDecimal("118.6"), new BigDecimal("24.9"),
                new BigDecimal("25"), 70, "多云", "mild", "live",
                OffsetDateTime.now(ZoneOffset.ofHours(8)));
    }
}
