package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Tea;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TeaMapper extends BaseMapper<Tea> {
    /**
     * 让数据库按与 RecommendService 相同的规则完成排序，只返回首页真正需要的三条记录。
     */
    @Select("""
            SELECT t.*
            FROM tea t
            WHERE t.enabled = 1
              AND (#{excludeHighCaffeine} = FALSE OR t.caffeine <> '高')
            ORDER BY (
                CASE
                    WHEN #{climateTag} = 'hot_humid' AND t.category IN ('绿茶', '白茶', '花草') THEN 20
                    WHEN #{climateTag} = 'hot' AND t.category IN ('绿茶', '白茶', '花草') THEN 15
                    WHEN #{climateTag} = 'cold' AND t.category IN ('红茶', '黑茶') THEN 20
                    WHEN #{climateTag} = 'mild' AND t.category IN ('青茶', '白茶') THEN 10
                    ELSE 0
                END
                + CASE WHEN #{purpose} IS NOT NULL
                    AND JSON_CONTAINS(t.effect_tags, JSON_QUOTE(#{purpose})) THEN 25 ELSE 0 END
                + CASE WHEN #{flavor} IS NOT NULL
                    AND JSON_CONTAINS(t.flavor_tags, JSON_QUOTE(#{flavor})) THEN 20 ELSE 0 END
                + CASE WHEN #{strength} IS NOT NULL AND t.suitable_strength = #{strength} THEN 10 ELSE 0 END
                + CASE
                    WHEN #{constitutionType} = 0 AND t.nature = '温' THEN 10
                    WHEN #{constitutionType} = 1 AND t.nature IN ('寒', '凉') THEN 10
                    ELSE 0
                END
            ) DESC, t.id ASC
            LIMIT 3
            """)
    List<Tea> selectTopRankedEnabledTea(
            @Param("climateTag") String climateTag,
            @Param("purpose") String purpose,
            @Param("flavor") String flavor,
            @Param("strength") String strength,
            @Param("constitutionType") Integer constitutionType,
            @Param("excludeHighCaffeine") boolean excludeHighCaffeine
    );
}
