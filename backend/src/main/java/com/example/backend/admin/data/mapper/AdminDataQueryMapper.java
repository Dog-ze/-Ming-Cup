package com.example.backend.admin.data.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminDataQueryMapper {

    /**
     * 查询茶饮记录分页数据。
     *
     * @param keyword         关键字，用于模糊搜索茶饮记录的茶名、类别、品牌、获取方式、评论，以及用户的用户名、手机号、邮箱。
     * @param userId          用户ID，用于筛选特定用户的茶饮记录。
     * @param startAt         茶饮记录创建时间的起始时间，用于筛选在此时间之后创建的记录。
     * @param endExclusiveAt  茶饮记录创建时间的结束时间（不包含），用于筛选在此时间之前创建的记录。
     * @param offset          分页查询的偏移量，用于指定从哪条记录开始查询。
     * @param pageSize        分页查询的每页大小，用于指定每页返回多少条记录。
     * @return                返回符合条件的茶饮记录列表，每条记录包含茶饮信息和对应用户信息。
     */
    @Select("""
            <script>
            SELECT r.id, r.user_id AS userId, u.username,
                   r.tea_name AS teaName, r.category, r.brand,
                   r.acquisition_method AS acquisitionMethod, r.price,
                   r.brew_temp AS brewTemp, r.rating, r.comment, r.images,
                   r.created_at AS createdAt
            FROM tea_record r
            INNER JOIN `user` u ON u.id = r.user_id
            <where>
              <if test='userId != null'>AND r.user_id = #{userId}</if>
              <if test='startAt != null'>AND r.created_at &gt;= #{startAt}</if>
              <if test='endExclusiveAt != null'>AND r.created_at &lt; #{endExclusiveAt}</if>
              <if test='keyword != null and keyword != ""'>
                AND (
                     r.tea_name LIKE CONCAT('%', #{keyword}, '%')
                  OR r.category LIKE CONCAT('%', #{keyword}, '%')
                  OR r.brand LIKE CONCAT('%', #{keyword}, '%')
                  OR r.acquisition_method LIKE CONCAT('%', #{keyword}, '%')
                  OR r.comment LIKE CONCAT('%', #{keyword}, '%')
                  OR u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            ORDER BY r.created_at DESC, r.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AdminTeaRecordRow> selectTeaRecordPage(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize
    );


    /**
     * 统计茶饮记录总数。
     *
     * @param keyword         关键字，用于模糊搜索茶饮记录的茶名、类别、品牌、获取方式、评论，以及用户的用户名、手机号、邮箱。
     * @param userId          用户ID，用于筛选特定用户的茶饮记录。
     * @param startAt         茶饮记录创建时间的起始时间，用于筛选在此时间之后创建的记录。
     * @param endExclusiveAt  茶饮记录创建时间的结束时间（不包含），用于筛选在此时间之前创建的记录。
     * @return                返回符合条件的茶饮记录总数。
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM tea_record r
            INNER JOIN `user` u ON u.id = r.user_id
            <where>
              <if test='userId != null'>AND r.user_id = #{userId}</if>
              <if test='startAt != null'>AND r.created_at &gt;= #{startAt}</if>
              <if test='endExclusiveAt != null'>AND r.created_at &lt; #{endExclusiveAt}</if>
              <if test='keyword != null and keyword != ""'>
                AND (
                     r.tea_name LIKE CONCAT('%', #{keyword}, '%')
                  OR r.category LIKE CONCAT('%', #{keyword}, '%')
                  OR r.brand LIKE CONCAT('%', #{keyword}, '%')
                  OR r.acquisition_method LIKE CONCAT('%', #{keyword}, '%')
                  OR r.comment LIKE CONCAT('%', #{keyword}, '%')
                  OR u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            </script>
            """)
    long countTeaRecords(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt
    );


    /**
     * 查询问卷调查分页数据。
     *
     * @param keyword         关键字，用于模糊搜索用户的用户名、手机号、邮箱。
     * @param userId          用户ID，用于筛选特定用户的问卷调查记录。
     * @param startAt         问卷调查更新时间的起始时间，用于筛选在此时间之后更新的记录。
     * @param endExclusiveAt  问卷调查更新时间的结束时间（不包含），用于筛选在此时间之前更新的记录。
     * @param offset          分页查询的偏移量，用于指定从哪条记录开始查询。
     * @param pageSize        分页查询的每页大小，用于指定每页返回多少条记录。
     * @return                返回符合条件的问卷调查记录列表，每条记录包含问卷信息和对应用户信息。
     */
    @Select("""
            <script>
            SELECT q.id, q.user_id AS userId, u.username,
                   q.purpose, q.constitution_type AS constitutionType,
                   q.insomnia, q.flavor_preference AS flavorPreference,
                   q.strength_preference AS strengthPreference,
                   q.updated_at AS updatedAt
            FROM questionnaire q
            INNER JOIN `user` u ON u.id = q.user_id
            <where>
              <if test='userId != null'>AND q.user_id = #{userId}</if>
              <if test='startAt != null'>AND q.updated_at &gt;= #{startAt}</if>
              <if test='endExclusiveAt != null'>AND q.updated_at &lt; #{endExclusiveAt}</if>
              <if test='keyword != null and keyword != ""'>
                AND (
                     u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            ORDER BY q.updated_at DESC, q.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AdminQuestionnaireRow> selectQuestionnairePage(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize
    );


    /**
     * 统计问卷调查总数。
     *
     * @param keyword         关键字，用于模糊搜索用户的用户名、手机号、邮箱。
     * @param userId          用户ID，用于筛选特定用户的问卷调查记录。
     * @param startAt         问卷调查更新时间的起始时间，用于筛选在此时间之后更新的记录。
     * @param endExclusiveAt  问卷调查更新时间的结束时间（不包含），用于筛选在此时间之前更新的记录。
     * @return                返回符合条件的问卷调查记录总数。
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM questionnaire q
            INNER JOIN `user` u ON u.id = q.user_id
            <where>
              <if test='userId != null'>AND q.user_id = #{userId}</if>
              <if test='startAt != null'>AND q.updated_at &gt;= #{startAt}</if>
              <if test='endExclusiveAt != null'>AND q.updated_at &lt; #{endExclusiveAt}</if>
              <if test='keyword != null and keyword != ""'>
                AND (
                     u.username LIKE CONCAT('%', #{keyword}, '%')
                  OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                  OR u.email LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            </where>
            </script>
            """)
    long countQuestionnaires(
            @Param("keyword") String keyword,
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt
    );


    /**
     * 查询问卷调查统计数据。
     *
     * @param keyword         关键字，用于模糊搜索用户的用户名、手机号、邮箱。
     * @param userId          用户ID，用于筛选特定用户的问卷调查记录。
     * @param startAt         问卷调查更新时间的起始时间，用于筛选在此时间之后更新的记录。
     * @param endExclusiveAt  问卷调查更新时间的结束时间（不包含），用于筛选在此时间之前更新的记录。
     * @return                返回符合条件的问卷调查统计数据，每条记录包含问题键、答案编码和答案数量。
     */
    @Select("""
            <script>
            WITH filtered_questionnaire AS (
              SELECT q.purpose, q.constitution_type, q.insomnia,
                     q.flavor_preference, q.strength_preference
              FROM questionnaire q
              INNER JOIN `user` u ON u.id = q.user_id
              <where>
                <if test='userId != null'>AND q.user_id = #{userId}</if>
                <if test='startAt != null'>AND q.updated_at &gt;= #{startAt}</if>
                <if test='endExclusiveAt != null'>AND q.updated_at &lt; #{endExclusiveAt}</if>
                <if test='keyword != null and keyword != ""'>
                  AND (
                       u.username LIKE CONCAT('%', #{keyword}, '%')
                    OR u.phone LIKE CONCAT('%', #{keyword}, '%')
                    OR u.email LIKE CONCAT('%', #{keyword}, '%')
                  )
                </if>
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
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt
    );
}