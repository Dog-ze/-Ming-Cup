package com.example.backend.admin.userdata;

import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.admin.userdata.mapper.AdminUserDataMapper;
import com.example.backend.admin.userdata.mapper.AdminUserQuestionnaireRow;
import com.example.backend.admin.userdata.mapper.AdminUserTeaRecordRow;
import com.example.backend.admin.userdata.mapper.QuestionnaireStatisticRow;
import com.example.backend.admin.userdata.vo.AdminAnswerOptionVO;
import com.example.backend.admin.userdata.vo.AdminQuestionnaireStatisticsVO;
import com.example.backend.admin.userdata.vo.AdminUserDataListItemVO;
import com.example.backend.admin.userdata.vo.AdminUserQuestionnaireVO;
import com.example.backend.admin.userdata.vo.AdminUserTeaRecordVO;
import com.example.backend.common.BizException;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AdminUserDataService {
    private static final long MAX_PAGE_SIZE = 100;
    private static final long MAX_DATE_RANGE_DAYS = 366;

    private final AdminUserDataMapper mapper;
    private final ObjectMapper objectMapper;


    /**
     * 分页查询用户列表。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @param keyword 用户名关键词，支持模糊匹配
     * @param questionnaireCompleted 问卷状态，0 未完成，1 已完成，null 不限制
     * @return 用户列表分页结果
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminUserDataListItemVO> userPage(
            long page, long pageSize, String keyword, Integer questionnaireCompleted
    ) {
        validatePage(page, pageSize);
        String normalizedKeyword = normalizeKeyword(keyword);
        validateQuestionnaireCompleted(questionnaireCompleted);

        long total = mapper.countUsers(normalizedKeyword, questionnaireCompleted);
        List<AdminUserDataListItemVO> list = mapper.selectUserPage(
                        normalizedKeyword, questionnaireCompleted, (page - 1) * pageSize, pageSize
                ).stream()
                .map(row -> new AdminUserDataListItemVO(
                        row.userId(), row.username(), row.teaRecordCount(), row.questionnaireCompleted()
                ))
                .toList();
        return new AdminPageVO<>(list, page, pageSize, total);
    }


    /**
     * 分页查询用户的品茶记录。
     *
     * @param userId 用户 ID
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @param keyword 茶名关键词，支持模糊匹配
     * @param startDate 查询开始日期，包含
     * @param endDate 查询结束日期，包含
     * @return 用户品茶记录分页结果
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminUserTeaRecordVO> teaRecordPage(
            Long userId, long page, long pageSize, String keyword, LocalDate startDate, LocalDate endDate
    ) {
        validatePage(page, pageSize);
        DateRange dateRange = validateDateRange(startDate, endDate);
        requireUsername(userId);
        String normalizedKeyword = normalizeKeyword(keyword);

        long total = mapper.countTeaRecords(
                userId, normalizedKeyword, dateRange.startAt(), dateRange.endExclusiveAt()
        );
        List<AdminUserTeaRecordVO> list = mapper.selectTeaRecordPage(
                        userId, normalizedKeyword, dateRange.startAt(), dateRange.endExclusiveAt(),
                        (page - 1) * pageSize, pageSize
                ).stream()
                .map(this::toTeaRecord)
                .toList();
        return new AdminPageVO<>(list, page, pageSize, total);
    }

    /**
     * 查询用户的问卷调查结果。
     *
     * @param userId 用户 ID
     * @return 用户问卷调查结果
     */
    @Transactional(readOnly = true)
    public AdminUserQuestionnaireVO questionnaire(Long userId) {
        String username = requireUsername(userId);
        AdminUserQuestionnaireRow row = mapper.selectQuestionnaireByUserId(userId);
        if (row == null) {
            return new AdminUserQuestionnaireVO(
                    userId, username, false, null, null, null, null, null, null
            );
        }
        return new AdminUserQuestionnaireVO(
                userId,
                username,
                true,
                QuestionnaireAnswerCatalog.labelOf("purpose", row.purpose()),
                QuestionnaireAnswerCatalog.labelOf("constitutionType", row.constitutionType()),
                QuestionnaireAnswerCatalog.labelOf("insomnia", row.insomnia()),
                QuestionnaireAnswerCatalog.labelOf("flavorPreference", row.flavorPreference()),
                QuestionnaireAnswerCatalog.labelOf("strengthPreference", row.strengthPreference()),
                row.updatedAt()
        );
    }


    /**
     * 查询问卷调查统计数据。
     *
     * @param keyword 关键字，用于模糊搜索用户的用户名、手机号、邮箱
     * @param questionnaireCompleted 问卷状态，0 未完成，1 已完成，null 不限制
     * @return 问卷调查统计数据
     */
    @Transactional(readOnly = true)
    public AdminQuestionnaireStatisticsVO questionnaireStatistics(String keyword, Integer questionnaireCompleted) {
        String normalizedKeyword = normalizeKeyword(keyword);
        validateQuestionnaireCompleted(questionnaireCompleted);

        long sampleCount = mapper.countQuestionnaireStatisticsSamples(normalizedKeyword, questionnaireCompleted);
        Map<String, Map<Integer, Long>> counts = mapper.selectQuestionnaireStatistics(
                        normalizedKeyword, questionnaireCompleted
                ).stream()
                .collect(Collectors.groupingBy(
                        QuestionnaireStatisticRow::questionKey,
                        Collectors.toMap(
                                QuestionnaireStatisticRow::answerCode,
                                QuestionnaireStatisticRow::answerCount,
                                Long::sum,
                                HashMap::new
                        )
                ));

        return new AdminQuestionnaireStatisticsVO(
                sampleCount,
                statisticItems("purpose", counts, sampleCount),
                statisticItems("constitutionType", counts, sampleCount),
                statisticItems("insomnia", counts, sampleCount),
                statisticItems("flavorPreference", counts, sampleCount),
                statisticItems("strengthPreference", counts, sampleCount)
        );
    }

    /**
     * 将数据库查询结果行转换为品茶记录视图对象。
     *
     * @param row 数据库查询结果行
     * @return 品茶记录视图对象
     */
    private AdminUserTeaRecordVO toTeaRecord(AdminUserTeaRecordRow row) {
        return new AdminUserTeaRecordVO(
                row.id(), row.teaName(), row.category(), row.brand(), row.acquisitionMethod(),
                row.price(), row.brewTemp(), row.rating(), row.comment(), parseImages(row.images()), row.createdAt()
        );
    }


    /**
     * 解析图片 JSON 字符串为图片 URL 列表。
     *
     * @param images 图片 JSON 字符串
     * @return 图片 URL 列表
     */
    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() {
            });
        } catch (JacksonException exception) {
            return List.of();
        }
    }


    /**
     * 统计问卷调查选项的数量和百分比。
     *
     * @param questionKey 问题键
     * @param allCounts   所有问题的答案计数映射
     * @param sampleCount 样本总数
     * @return 统计结果列表，每个元素包含选项编码、标签、数量和百分比
     */
    private List<AdminAnswerOptionVO> statisticItems(
            String questionKey, Map<String, Map<Integer, Long>> allCounts, long sampleCount
    ) {
        Map<Integer, Long> counts = allCounts.getOrDefault(questionKey, Map.of());
        return QuestionnaireAnswerCatalog.optionsOf(questionKey).stream()
                .map(option -> {
                    long count = counts.getOrDefault(option.code(), 0L);
                    double percent = sampleCount == 0
                            ? 0D
                            : Math.round(count * 10000D / sampleCount) / 100D;
                    return new AdminAnswerOptionVO(option.code(), option.label(), count, percent);
                })
                .toList();
    }

    /**
     * 验证用户 ID 是否有效，并返回对应的用户名。
     *
     * @param userId 用户 ID
     * @return 用户名
     * @throws BizException 如果用户 ID 无效或用户不存在
     */
    private String requireUsername(Long userId) {
        if (userId == null || userId < 1) {
            throw new BizException(400, "用户 ID 必须为正整数");
        }
        String username = mapper.selectUsernameById(userId);
        if (username == null) {
            throw new BizException(404, "用户不存在");
        }
        return username;
    }


    /**
     * 验证分页参数是否合法。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @throws BizException 如果分页参数不合法
     */
    private void validatePage(long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BizException(400, "分页参数不正确");
        }
    }


    /**
     * 规范化关键字参数，去除前后空格，并检查长度。
     *
     * @param keyword 关键字
     * @return 规范化后的关键字，如果为空或仅包含空格，则返回 null
     * @throws BizException 如果关键字长度超过 100 个字符
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalizedKeyword = keyword.trim();
        if (normalizedKeyword.length() > 100) {
            throw new BizException(400, "关键词不能超过 100 个字符");
        }
        return normalizedKeyword;
    }

    /**
     * 验证问卷状态参数是否合法。
     *
     * @param questionnaireCompleted 问卷状态，0 未完成，1 已完成，null 不限制
     * @throws BizException 如果问卷状态不合法
     */
    private void validateQuestionnaireCompleted(Integer questionnaireCompleted) {
        if (questionnaireCompleted != null && questionnaireCompleted != 0 && questionnaireCompleted != 1) {
            throw new BizException(400, "问卷状态只能是 0 或 1");
        }
    }

    /**
     * 验证日期范围是否合法，并返回对应的起始和结束时间。
     *
     * @param startDate 查询开始日期，包含
     * @param endDate 查询结束日期，包含
     * @return 日期范围对象，包含起始时间和结束时间（不包含）
     * @throws BizException 如果日期范围不合法
     */
    private DateRange validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new BizException(400, "开始日期不能晚于结束日期");
            }
            if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_DATE_RANGE_DAYS) {
                throw new BizException(400, "查询日期范围不能超过 366 天");
            }
        }
        return new DateRange(
                startDate == null ? null : startDate.atStartOfDay(),
                endDate == null ? null : endDate.plusDays(1).atStartOfDay()
        );
    }

    private record DateRange(LocalDateTime startAt, LocalDateTime endExclusiveAt) {
    }
}
