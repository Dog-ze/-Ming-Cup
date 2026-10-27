package com.example.backend.admin.operationlog;

import com.example.backend.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {
    private final AdminOperationLogService service;

    /**
     * 分页获取管理员操作日志。
     *
     * @param page      页码，默认值为 1
     * @param pageSize  每页数量，默认值为 10
     * @param adminId   可选的管理员 ID，用于筛选特定管理员的操作日志
     * @param module    可选的模块名称，用于筛选特定模块的操作日志
     * @param action    可选的操作类型，用于筛选特定操作类型的操作日志
     * @param startDate 可选的开始日期，用于筛选操作日志的时间范围
     * @param endDate   可选的结束日期，用于筛选操作日志的时间范围
     * @return 分页的管理员操作日志列表
     */
    @GetMapping
    public Result<?> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(service.page(page, pageSize, adminId, module, action, startDate, endDate));
    }
}