package com.example.backend.recommend;

import com.example.backend.entity.Tea;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaMapper;
import com.example.backend.recommend.vo.RecommendationVO;
import com.example.backend.weather.WeatherService;
import com.example.backend.weather.vo.WeatherResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendServiceTest {

    @Test
    void returnsTheTeaImageStoredInTheDatabase() {
        WeatherService weatherService = mock(WeatherService.class);
        TeaMapper teaMapper = mock(TeaMapper.class);
        QuestionnaireMapper questionnaireMapper = mock(QuestionnaireMapper.class);
        Tea tea = tea(1L, "铁观音", "青茶", "/uploads/admin/tea/tieguanyin.png");
        when(weatherService.getWeather(any(), any(), any())).thenReturn(weather());
        when(teaMapper.selectTopRankedEnabledTea(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(List.of(tea));

        RecommendService service = new RecommendService(
                weatherService, teaMapper, questionnaireMapper, new ObjectMapper());

        RecommendationVO recommendation = service.recommend("泉州市", null, null)
                .recommendations().get(0);

        assertThat(recommendation).extracting("imageUrl")
                .isEqualTo("/uploads/admin/tea/tieguanyin.png");
        verify(teaMapper).selectTopRankedEnabledTea(any(), any(), any(), any(), any(), anyBoolean());
    }

    private Tea tea(Long id, String name, String category, String imageUrl) {
        Tea tea = new Tea();
        tea.setId(id);
        tea.setName(name);
        tea.setCategory(category);
        tea.setEnabled(1);
        tea.setCaffeine("低");
        tea.setFlavorTags("[]");
        tea.setEffectTags("[]");
        tea.setSuitableStrength("淡");
        tea.setImageUrl(imageUrl);
        return tea;
    }

    private WeatherResponse weather() {
        return new WeatherResponse(
                "泉州市", new BigDecimal("118.6"), new BigDecimal("24.9"),
                new BigDecimal("23.0"), 61, "晴", "mild", "live",
                OffsetDateTime.now(ZoneOffset.ofHours(8))
        );
    }
}
