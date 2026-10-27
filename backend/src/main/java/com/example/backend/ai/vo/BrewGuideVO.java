package com.example.backend.ai.vo;

/** AI 生成的完整日常泡茶建议。 */
public record BrewGuideVO(
        Long teaId,
        String teaName,
        String content
) {
}
