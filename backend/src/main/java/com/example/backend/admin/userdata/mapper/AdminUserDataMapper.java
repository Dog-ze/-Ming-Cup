package com.example.backend.admin.userdata.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminUserDataMapper {

    @Select("""
            <script>
            SELECT u.id AS userId, u.username,
                   (SELECT COUNT(*) FROM tea_record r WHERE r.user_id = u.id) AS teaRecordCount,
                   CASE WHEN EXISTS (SELECT 1 FROM questionnaire q WHERE q.user_id = u.id)
                        THEN TRUE ELSE FALSE END AS questionnaireCompleted
            FROM `user` u
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='questionnaireCompleted != null'>
                <choose>
                  <when test='questionnaireCompleted == 1'>
                    AND EXISTS (SELECT 1 FROM questionnaire q WHERE q.user_id = u.id)
                  </when>
                  <otherwise>
                    AND NOT EXISTS (SELECT 1 FROM questionnaire q WHERE q.user_id = u.id)
                  </otherwise>
                </choose>
              </if>
            </where>
            ORDER BY u.created_at DESC, u.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AdminUserDataRow> selectUserPage(
            @Param("keyword") String keyword,
            @Param("questionnaireCompleted") Integer questionnaireCompleted,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize
    );

    @Select("""
            <script>
            SELECT COUNT(*) FROM `user` u
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='questionnaireCompleted != null'>
                <choose>
                  <when test='questionnaireCompleted == 1'>
                    AND EXISTS (SELECT 1 FROM questionnaire q WHERE q.user_id = u.id)
                  </when>
                  <otherwise>
                    AND NOT EXISTS (SELECT 1 FROM questionnaire q WHERE q.user_id = u.id)
                  </otherwise>
                </choose>
              </if>
            </where>
            </script>
            """)
    long countUsers(
            @Param("keyword") String keyword,
            @Param("questionnaireCompleted") Integer questionnaireCompleted
    );

    @Select("SELECT username FROM `user` WHERE id = #{userId}")
    String selectUsernameById(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT id, tea_name AS teaName, category, brand,
                   acquisition_method AS acquisitionMethod, price, brew_temp AS brewTemp,
                   rating, comment, images, created_at AS createdAt
            FROM tea_record
            <where>
              user_id = #{userId}
              <if test='keyword != null and keyword != ""'>
                AND (tea_name LIKE CONCAT('%', #{keyword}, '%')
                  OR category LIKE CONCAT('%', #{keyword}, '%')
                  OR brand LIKE CONCAT('%', #{keyword}, '%')
                  OR acquisition_method LIKE CONCAT('%', #{keyword}, '%')
                  OR comment LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='startAt != null'>AND created_at &gt;= #{startAt}</if>
              <if test='endExclusiveAt != null'>AND created_at &lt; #{endExclusiveAt}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AdminUserTeaRecordRow> selectTeaRecordPage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize
    );

    @Select("""
            <script>
            SELECT COUNT(*) FROM tea_record
            <where>
              user_id = #{userId}
              <if test='keyword != null and keyword != ""'>
                AND (tea_name LIKE CONCAT('%', #{keyword}, '%')
                  OR category LIKE CONCAT('%', #{keyword}, '%')
                  OR brand LIKE CONCAT('%', #{keyword}, '%')
                  OR acquisition_method LIKE CONCAT('%', #{keyword}, '%')
                  OR comment LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='startAt != null'>AND created_at &gt;= #{startAt}</if>
              <if test='endExclusiveAt != null'>AND created_at &lt; #{endExclusiveAt}</if>
            </where>
            </script>
            """)
    long countTeaRecords(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt
    );

    @Select("""
            SELECT purpose, constitution_type AS constitutionType, insomnia,
                   flavor_preference AS flavorPreference,
                   strength_preference AS strengthPreference, updated_at AS updatedAt
            FROM questionnaire
            WHERE user_id = #{userId}
            """)
    AdminUserQuestionnaireRow selectQuestionnaireByUserId(@Param("userId") Long userId);

    @Select("""
            <script>
            WITH filtered_questionnaire AS (
              SELECT q.purpose, q.constitution_type, q.insomnia,
                     q.flavor_preference, q.strength_preference
              FROM questionnaire q
              INNER JOIN `user` u ON u.id = q.user_id
              <where>
                <if test='keyword != null and keyword != ""'>
                  AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                    OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                    OR u.email LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test='questionnaireCompleted != null and questionnaireCompleted == 0'>AND 1 = 0</if>
              </where>
            )
            SELECT 'purpose' AS questionKey, purpose AS answerCode, COUNT(*) AS answerCount
            FROM filtered_questionnaire GROUP BY purpose
            UNION ALL
            SELECT 'constitutionType', constitution_type, COUNT(*)
            FROM filtered_questionnaire GROUP BY constitution_type
            UNION ALL
            SELECT 'insomnia', insomnia, COUNT(*)
            FROM filtered_questionnaire GROUP BY insomnia
            UNION ALL
            SELECT 'flavorPreference', flavor_preference, COUNT(*)
            FROM filtered_questionnaire GROUP BY flavor_preference
            UNION ALL
            SELECT 'strengthPreference', strength_preference, COUNT(*)
            FROM filtered_questionnaire GROUP BY strength_preference
            </script>
            """)
    List<QuestionnaireStatisticRow> selectQuestionnaireStatistics(
            @Param("keyword") String keyword,
            @Param("questionnaireCompleted") Integer questionnaireCompleted
    );

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM questionnaire q
            INNER JOIN `user` u ON u.id = q.user_id
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='questionnaireCompleted != null and questionnaireCompleted == 0'>AND 1 = 0</if>
            </where>
            </script>
            """)
    long countQuestionnaireStatisticsSamples(
            @Param("keyword") String keyword,
            @Param("questionnaireCompleted") Integer questionnaireCompleted
    );
}
