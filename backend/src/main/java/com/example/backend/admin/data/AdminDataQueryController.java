package com.example.backend.admin.data;

import com.example.backend.admin.data.vo.AdminQuestionnaireListItemVO;
import com.example.backend.admin.data.vo.AdminQuestionnaireStatisticsVO;
import com.example.backend.admin.data.vo.AdminTeaRecordListItemVO;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.common.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 旧版后台用户数据查询接口。
 *
 * <p>正式接口已经迁移到 {@code /api/admin/user-data}。本 Controller 仅在兼容期内保留，
 * 不允许新增业务功能；确认不存在旧调用方后，将与本包其他代码一并删除。</p>
 */
@Deprecated(since = "2026-10", forRemoval = true)
@SuppressWarnings("removal")
@Validated
@RestController
@RequestMapping("/api/admin")
@AllArgsConstructor
public class AdminDataQueryController {
    private final AdminDataQueryService dataQueryService;

/**
     * 分页查询茶食记录列表
     *
     * @param page      页码
     * @param pageSize  每页数量
     * @param keyword   关键字
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 茶食记录列表
     */
    @GetMapping("/tea-records")
    public Result<AdminPageVO<AdminTeaRecordListItemVO>> teaRecords(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(dataQueryService.listTeaRecords(
                page, pageSize, keyword, userId, startDate, endDate
        ));
    }


    /**
     * 分页查询问卷列表
     *
     * @param page      页码
     * @param pageSize  每页数量
     * @param keyword   关键字
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 问卷列表
     */
    @GetMapping("/questionnaires")
    public Result<AdminPageVO<AdminQuestionnaireListItemVO>> questionnaires(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(dataQueryService.listQuestionnaires(
                page, pageSize, keyword, userId, startDate, endDate
        ));
    }


    /**
     * 查询问卷统计数据
     *
     * @param keyword   关键字
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 问卷统计数据
     */
    @GetMapping("/questionnaires/statistics")
    public Result<AdminQuestionnaireStatisticsVO> questionnaireStatistics(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(dataQueryService.questionnaireStatistics(
                keyword, userId, startDate, endDate
        ));
    }
}
