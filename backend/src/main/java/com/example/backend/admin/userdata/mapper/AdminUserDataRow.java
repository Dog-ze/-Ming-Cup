package com.example.backend.admin.userdata.mapper;

/** 数据库查询出的用户汇总行。 */
public record AdminUserDataRow(
        Long userId,
        String username,
        long teaRecordCount,
        boolean questionnaireCompleted
) {
}
