package com.example.backend.admin.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminDashboardMapper {

    @Select("SELECT COUNT(*) FROM `user`")
    long countAllUsers();

    @Select("SELECT COUNT(*) FROM `user` WHERE status = 1")
    long countEnabledUsers();

    @Select("SELECT COUNT(*) FROM tea")
    long countTeas();

    @Select("SELECT COUNT(*) FROM pairing_store WHERE promotion_enabled = 1")
    long countPromotionPartnerStores();

    @Select("""
            SELECT COUNT(*)
            FROM pairing_store
            WHERE promotion_enabled = 1
              AND status = 1
              AND (promotion_start_at IS NULL OR promotion_start_at <= #{now})
              AND (promotion_end_at IS NULL OR promotion_end_at >= #{now})
            """)
    long countActivePromotionStores(@Param("now") LocalDateTime now);

    @Select("""
            SELECT COUNT(*)
            FROM tea_record
            WHERE created_at >= #{startAt}
              AND created_at < #{endExclusiveAt}
            """)
    long countTeaRecordsBetween(@Param("startAt") LocalDateTime startAt,
                                @Param("endExclusiveAt") LocalDateTime endExclusiveAt);

    @Select("SELECT COUNT(*) FROM questionnaire")
    long countQuestionnaires();

    @Select("""
            SELECT DATE(created_at) AS date, COUNT(*) AS newUserCount
            FROM `user`
            WHERE created_at >= #{startAt}
              AND created_at < #{endExclusiveAt}
            GROUP BY DATE(created_at)
            ORDER BY date ASC
            """)
    List<UserGrowthRow> selectUserGrowth(@Param("startAt") LocalDateTime startAt,
                                          @Param("endExclusiveAt") LocalDateTime endExclusiveAt);
}