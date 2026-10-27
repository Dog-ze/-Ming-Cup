package com.example.backend.admin.userdata.vo;

/** 用户数据主表的一行，只包含后台页面需要的汇总信息。 */
public record AdminUserDataListItemVO(
        Long userId,
        String username,
        long teaRecordCount,
        boolean questionnaireCompleted
) {
}
