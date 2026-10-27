package com.example.backend.admin.data;

import com.example.backend.admin.data.mapper.AdminDataQueryMapper;
import com.example.backend.admin.data.mapper.AdminQuestionnaireRow;
import com.example.backend.admin.data.mapper.AdminTeaRecordRow;
import com.example.backend.admin.data.mapper.QuestionnaireStatisticRow;
import com.example.backend.admin.data.vo.AdminAnswerOptionVO;
import com.example.backend.admin.data.vo.AdminQuestionnaireListItemVO;
import com.example.backend.admin.data.vo.AdminQuestionnaireStatisticsVO;
import com.example.backend.admin.data.vo.AdminTeaRecordListItemVO;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.common.BizException;
import lombok.AllArgsConstructor;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 旧版后台用户数据查询服务。
 *
 * <p>新页面和新接口统一使用 {@code AdminUserDataService}。本服务仅为旧接口兼容期保留，
 * 不允许继续增加查询或统计功能；确认没有调用方后删除。</p>
 */
@Deprecated(since = "2026-10", forRemoval = true)
@Service
@AllArgsConstructor
public class AdminDataQueryService {
    private static final long MAX_PAGE_SIZE = 100;
    private static final long MAX_DATE_RANGE_DAYS = 366;

    private final AdminDataQueryMapper dataQueryMapper;
    private final ObjectMapper objectMapper;

    /**
     * 查询茶饮记录列表。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @param keyword 茶饮名称或品牌关键词，可选
     * @param userId 用户 ID，可选
     * @param startDate 开始日期，可选
     * @param endDate 结束日期，可选
     * @return 茶饮记录分页结果
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminTeaRecordListItemVO> listTeaRecords(
            long page, long pageSize, String keyword, Long userId,
            LocalDate startDate, LocalDate endDate
    ) {
        QueryCondition condition = normalizeAndValidate(page, pageSize, keyword, userId, startDate, endDate);
        long total = dataQueryMapper.countTeaRecords(
                condition.keyword(), condition.userId(), condition.startAt(), condition.endExclusiveAt()
        );
        List<AdminTeaRecordListItemVO> list = dataQueryMapper.selectTeaRecordPage(
                        condition.keyword(), condition.userId(), condition.startAt(), condition.endExclusiveAt(),
                        (page - 1) * pageSize, pageSize
                ).stream()
                .map(this::toTeaRecordItem)
                .toList();
        return new AdminPageVO<>(list, page, pageSize, total);
    }

    /**
     * 查询问卷列表。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @param keyword 用户名或昵称关键词，可选
     * @param userId 用户 ID，可选
     * @param startDate 开始日期，可选
     * @param endDate 结束日期，可选
     * @return 问卷分页结果
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminQuestionnaireListItemVO> listQuestionnaires(
            long page, long pageSize, String keyword, Long userId,
            LocalDate startDate, LocalDate endDate
    ) {
        QueryCondition condition = normalizeAndValidate(page, pageSize, keyword, userId, startDate, endDate);
        long total = dataQueryMapper.countQuestionnaires(
                condition.keyword(), condition.userId(), condition.startAt(), condition.endExclusiveAt()
        );
        List<AdminQuestionnaireListItemVO> list = dataQueryMapper.selectQuestionnairePage(
                        condition.keyword(), condition.userId(), condition.startAt(), condition.endExclusiveAt(),
                        (page - 1) * pageSize, pageSize
                ).stream()
                .map(this::toQuestionnaireItem)
                .toList();
        return new AdminPageVO<>(list, page, pageSize, total);
    }


    /**
     * 查询问卷统计数据。
     *
     * @param keyword 用户名或昵称关键词，可选
     * @param userId 用户 ID，可选
     * @param startDate 开始日期，可选
     * @param endDate 结束日期，可选
     * @return 问卷统计结果
     */
    @Transactional(readOnly = true)
    public AdminQuestionnaireStatisticsVO questionnaireStatistics(
            String keyword, Long userId, LocalDate startDate, LocalDate endDate
    ) {
        QueryCondition condition = normalizeAndValidate(1, 1, keyword, userId, startDate, endDate);
        long sampleCount = dataQueryMapper.countQuestionnaires(
                condition.keyword(), condition.userId(), condition.startAt(), condition.endExclusiveAt()
        );
        Map<String, Map<Integer, Long>> counts = dataQueryMapper.selectQuestionnaireStatistics(
                        condition.keyword(), condition.userId(), condition.startAt(), condition.endExclusiveAt()
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
     * 将数据库行对象转换为茶饮记录列表项 VO。
     *
     * @param row 数据库行对象
     * @return 茶饮记录列表项 VO
     */
    private AdminTeaRecordListItemVO toTeaRecordItem(AdminTeaRecordRow row) {
        return new AdminTeaRecordListItemVO(
                row.id(), row.userId(), row.username(), row.teaName(), row.category(), row.brand(),
                row.acquisitionMethod(), row.price(), row.brewTemp(), row.rating(), row.comment(),
                parseImages(row.images()), row.createdAt()
        );
    }


    /**
     * 将数据库行对象转换为问卷列表项 VO。
     *
     * @param row 数据库行对象
     * @return 问卷列表项 VO
     */
    private AdminQuestionnaireListItemVO toQuestionnaireItem(AdminQuestionnaireRow row) {
        return new AdminQuestionnaireListItemVO(
                row.id(), row.userId(), row.username(),
                QuestionnaireAnswerCatalog.labelOf("purpose", row.purpose()),
                QuestionnaireAnswerCatalog.labelOf("constitutionType", row.constitutionType()),
                QuestionnaireAnswerCatalog.labelOf("insomnia", row.insomnia()),
                QuestionnaireAnswerCatalog.labelOf("flavorPreference", row.flavorPreference()),
                QuestionnaireAnswerCatalog.labelOf("strengthPreference", row.strengthPreference()),
                row.updatedAt()
        );
    }


    /**
     * 统计问卷选项的数量和百分比。
     *
     * @param questionKey 问题键
     * @param allCounts 所有问题的计数映射
     * @param sampleCount 样本总数
     * @return 统计结果列表
     */
    private List<AdminAnswerOptionVO> statisticItems(
            String questionKey, Map<String, Map<Integer, Long>> allCounts, long sampleCount
    ) {
        Map<Integer, Long> questionCounts = allCounts.getOrDefault(questionKey, Map.of());
        return QuestionnaireAnswerCatalog.optionsOf(questionKey).stream()
                .map(option -> {
                    long count = questionCounts.getOrDefault(option.code(), 0L);
                    double percent = sampleCount == 0 ? 0D
                            : Math.round((count * 10000D / sampleCount)) / 100D;
                    return new AdminAnswerOptionVO(option.code(), option.label(), count, percent);
                })
                .toList();
    }


    /**
     * 解析图片 JSON 字符串为列表。
     *
     * @param images 图片 JSON 字符串
     * @return 图片列表
     */
    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(images, new TypeReference<List<String>>() { });
        } catch (JacksonException exception) {
            return List.of();
        }
    }


    /**
     * 规范化和验证查询条件。
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键词
     * @param userId 用户 ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 规范化后的查询条件对象
     */
    private QueryCondition normalizeAndValidate(
            long page, long pageSize, String keyword, Long userId,
            LocalDate startDate, LocalDate endDate
    ) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new BizException(400, "分页参数不正确");
        }
        if (userId != null && userId < 1) {
            throw new BizException(400, "用户 ID 必须为正整数");
        }
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.length() > 100) {
            throw new BizException(400, "关键词不能超过 100 个字符");
        }
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new BizException(400, "开始日期不能晚于结束日期");
            }
            if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_DATE_RANGE_DAYS) {
                throw new BizException(400, "查询日期范围不能超过 366 天");
            }
        }
        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endExclusiveAt = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        return new QueryCondition(normalizedKeyword, userId, startAt, endExclusiveAt);
    }


    /**
     * 内部查询条件对象，用于封装查询参数。
     *
     * @param keyword 关键词
     * @param userId 用户 ID
     * @param startAt 开始时间
     * @param endExclusiveAt 结束时间（不包含）
     */
    private record QueryCondition(
            String keyword, Long userId, LocalDateTime startAt, LocalDateTime endExclusiveAt
    ) { }
}
