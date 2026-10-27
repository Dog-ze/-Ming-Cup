package com.example.backend.admin.userdata;

import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.admin.userdata.vo.AdminQuestionnaireStatisticsVO;
import com.example.backend.admin.userdata.vo.AdminUserDataListItemVO;
import com.example.backend.admin.userdata.vo.AdminUserQuestionnaireVO;
import com.example.backend.admin.userdata.vo.AdminUserTeaRecordVO;
import com.example.backend.common.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/admin/user-data")
public class AdminUserDataController {
    private final AdminUserDataService service;

    public AdminUserDataController(AdminUserDataService service) {
        this.service = service;
    }

    /**
     * 分页获取用户数据列表。
     *
     * @param page                  页码，默认值为 1
     * @param pageSize              每页数量，默认值为 10，最大值为 100
     * @param keyword               可选的搜索关键字，用于模糊匹配用户名、手机号或邮箱
     * @param questionnaireCompleted 可选的问卷完成状态，0 表示未完成，1 表示已完成
     * @return 分页的用户数据列表
     */
    @GetMapping
    public Result<AdminPageVO<AdminUserDataListItemVO>> userPage(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer questionnaireCompleted
    ) {
        return Result.ok(service.userPage(page, pageSize, keyword, questionnaireCompleted));
    }

    /**
     * 分页获取指定用户的品茶记录。
     *
     * @param userId    用户 ID
     * @param page      页码，默认值为 1
     * @param pageSize  每页数量，默认值为 10，最大值为 100
     * @param keyword   可选的搜索关键字，用于模糊匹配品茶记录的内容
     * @param startDate 可选的开始日期，用于筛选品茶记录的时间范围
     * @param endDate   可选的结束日期，用于筛选品茶记录的时间范围
     * @return 分页的品茶记录列表
     */
    @GetMapping("/{userId}/tea-records")
    public Result<AdminPageVO<AdminUserTeaRecordVO>> teaRecords(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return Result.ok(service.teaRecordPage(userId, page, pageSize, keyword, startDate, endDate));
    }


    /**
     * 获取指定用户的问卷数据。
     *
     * @param userId 用户 ID
     * @return 用户的问卷数据
     */
    @GetMapping("/{userId}/questionnaire")
    public Result<AdminUserQuestionnaireVO> questionnaire(@PathVariable @Positive Long userId) {
        return Result.ok(service.questionnaire(userId));
    }


    /**
     * 获取问卷调查的统计数据。
     *
     * @param keyword               可选的搜索关键字，用于模糊匹配用户名、手机号或邮箱
     * @param questionnaireCompleted 可选的问卷完成状态，0 表示未完成，1 表示已完成
     * @return 问卷调查的统计数据
     */
    @GetMapping("/questionnaire-statistics")
    public Result<AdminQuestionnaireStatisticsVO> questionnaireStatistics(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer questionnaireCompleted
    ) {
        return Result.ok(service.questionnaireStatistics(keyword, questionnaireCompleted));
    }
}
