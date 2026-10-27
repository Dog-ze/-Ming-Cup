package com.example.backend.admin.operationlog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminOperationLogQueryMapper {

    /**
     * 查询管理员操作日志分页数据。
     *
     * @param adminId         管理员ID，用于筛选特定管理员的操作日志。
     * @param module          模块名称，用于筛选特定模块的操作日志。
     * @param action          操作类型，用于筛选特定操作类型的日志。
     * @param startAt         日志创建时间的起始时间，用于筛选在此时间之后创建的日志。
     * @param endExclusiveAt  日志创建时间的结束时间（不包含），用于筛选在此时间之前创建的日志。
     * @param offset          分页查询的偏移量，用于指定从哪条记录开始查询。
     * @param pageSize        分页查询的每页大小，用于指定每页返回多少条记录。
     * @return                返回符合条件的管理员操作日志列表，每条记录包含日志信息和对应管理员信息。
     */
    @Select("""
            <script>
            SELECT log.id,
                   log.admin_id AS adminId,
                   admin.username AS adminUsername,
                   admin.display_name AS adminDisplayName,
                   log.module,
                   log.action,
                   log.target_id AS targetId,
                   log.detail,
                   log.created_at AS createdAt
            FROM admin_operation_log log
            LEFT JOIN admin_user admin ON admin.id = log.admin_id
            <where>
              <if test='adminId != null'>
                AND log.admin_id = #{adminId}
              </if>
              <if test='module != null and module != ""'>
                AND log.module = #{module}
              </if>
              <if test='action != null and action != ""'>
                AND log.action = #{action}
              </if>
              <if test='startAt != null'>
                AND log.created_at <![CDATA[>=]]> #{startAt}
              </if>
              <if test='endExclusiveAt != null'>
                AND log.created_at <![CDATA[<]]> #{endExclusiveAt}
              </if>
            </where>
            ORDER BY log.created_at DESC, log.id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AdminOperationLogQueryRow> selectPage(
            @Param("adminId") Long adminId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt,
            @Param("offset") long offset,
            @Param("pageSize") long pageSize
    );

    /**
     * 统计管理员操作日志总数。
     *
     * @param adminId         管理员ID，用于筛选特定管理员的操作日志。
     * @param module          模块名称，用于筛选特定模块的操作日志。
     * @param action          操作类型，用于筛选特定操作类型的日志。
     * @param startAt         日志创建时间的起始时间，用于筛选在此时间之后创建的日志。
     * @param endExclusiveAt  日志创建时间的结束时间（不包含），用于筛选在此时间之前创建的日志。
     * @return                返回符合条件的管理员操作日志总数。
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM admin_operation_log log
            <where>
              <if test='adminId != null'>
                AND log.admin_id = #{adminId}
              </if>
              <if test='module != null and module != ""'>
                AND log.module = #{module}
              </if>
              <if test='action != null and action != ""'>
                AND log.action = #{action}
              </if>
              <if test='startAt != null'>
                AND log.created_at <![CDATA[>=]]> #{startAt}
              </if>
              <if test='endExclusiveAt != null'>
                AND log.created_at <![CDATA[<]]> #{endExclusiveAt}
              </if>
            </where>
            </script>
            """)
    long count(
            @Param("adminId") Long adminId,
            @Param("module") String module,
            @Param("action") String action,
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusiveAt") LocalDateTime endExclusiveAt
    );
}