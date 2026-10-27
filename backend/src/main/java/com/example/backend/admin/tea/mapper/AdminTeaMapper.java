package com.example.backend.admin.tea.mapper;

import com.example.backend.admin.tea.vo.AdminTeaDetailVO;
import com.example.backend.admin.tea.vo.AdminTeaListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AdminTeaMapper {

    @Select("""
            <script>
            SELECT id, name, category, nature, caffeine, source, enabled,
                   created_at AS createdAt
            FROM tea
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (name LIKE CONCAT('%', #{keyword}, '%')
                  OR category LIKE CONCAT('%', #{keyword}, '%')
                  OR nature LIKE CONCAT('%', #{keyword}, '%')
                  OR source LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='category != null and category != ""'>AND category = #{category}</if>
              <if test='enabled != null'>AND enabled = #{enabled}</if>
            </where>
            ORDER BY created_at DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AdminTeaListItemVO> selectPage(@Param("keyword") String keyword,
                                        @Param("category") String category,
                                        @Param("enabled") Integer enabled,
                                        @Param("offset") long offset,
                                        @Param("pageSize") long pageSize);

    @Select("""
            <script>
            SELECT COUNT(*) FROM tea
            <where>
              <if test='keyword != null and keyword != ""'>
                AND (name LIKE CONCAT('%', #{keyword}, '%')
                  OR category LIKE CONCAT('%', #{keyword}, '%')
                  OR nature LIKE CONCAT('%', #{keyword}, '%')
                  OR source LIKE CONCAT('%', #{keyword}, '%'))
              </if>
              <if test='category != null and category != ""'>AND category = #{category}</if>
              <if test='enabled != null'>AND enabled = #{enabled}</if>
            </where>
            </script>
            """)
    long count(@Param("keyword") String keyword,
               @Param("category") String category,
               @Param("enabled") Integer enabled);

    @Select("""
            SELECT id, name, category, nature, caffeine,
                   flavor_tags AS flavorTags, effect_tags AS effectTags,
                   suitable_strength AS suitableStrength, brew_temp AS brewTemp,
                   brew_time AS brewTime, source, image_url AS imageUrl, enabled, created_at AS createdAt
            FROM tea WHERE id = #{id}
            """)
    AdminTeaDetailVO selectDetailById(@Param("id") Long id);

    @Select("SELECT DISTINCT category FROM tea WHERE category IS NOT NULL AND category <> '' ORDER BY category")
    List<String> selectCategories();

}
