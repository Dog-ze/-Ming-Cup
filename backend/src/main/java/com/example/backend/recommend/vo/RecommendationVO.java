package com.example.backend.recommend.vo;

/**
 * 一款茶的推荐结果
 * @param teaId
 * @param name
 * @param category
 * @param imageUrl 茶叶在后台维护并保存到数据库的图片地址，可为空
 * @param score
 * @param reason
 */
public record RecommendationVO(
        Long teaId,
        String name,
        String category,
        String imageUrl,
        Integer score,
        String reason
) {
}
