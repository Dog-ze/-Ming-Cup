package com.example.backend.record.vo;

import com.example.backend.entity.TeaRecord;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TeaRecordVO(
        Long id,
        String teaName,
        String category,
        String brand,
        String acquisitionMethod,
        BigDecimal price,
        Integer brewTemp,
        Integer rating,
        String comment,
        List<String> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TeaRecordVO from(TeaRecord entity, ObjectMapper objectMapper) {
        List<String> imageList = List.of();
        if (entity.getImages() != null && !entity.getImages().isBlank()) {
            try {
                imageList = objectMapper.readValue(
                        entity.getImages(), new TypeReference<List<String>>() { }
                );
            } catch (JacksonException ignored) {
                // 历史图片字段格式异常时，不影响整条茶记展示。
            }
        }

        return new TeaRecordVO(
                entity.getId(), entity.getTeaName(), entity.getCategory(), entity.getBrand(),
                entity.getAcquisitionMethod(), entity.getPrice(), entity.getBrewTemp(),
                entity.getRating(), entity.getComment(), imageList,
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}