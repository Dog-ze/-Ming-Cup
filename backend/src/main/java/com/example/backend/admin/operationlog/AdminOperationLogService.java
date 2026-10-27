package com.example.backend.admin.operationlog;

import com.example.backend.admin.operationlog.mapper.AdminOperationLogQueryMapper;
import com.example.backend.admin.operationlog.mapper.AdminOperationLogQueryRow;
import com.example.backend.admin.operationlog.vo.AdminOperationLogListItemVO;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.AdminOperationLog;
import com.example.backend.mapper.AdminOperationLogMapper;
import com.example.backend.security.AdminCurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminOperationLogService {
    private static final Set<String> MODULES = Set.of(
            "auth", "user", "tea", "pairing_rule", "pairing_store"
    );
    private static final Set<String> ACTIONS = Set.of(
            "login", "logout", "create", "update", "delete", "enable", "disable", "promotion"
    );
    private static final Set<String> FORBIDDEN_DETAIL_KEYWORDS = Set.of(
            "password", "token", "longitude", "latitude", "phone", "email",
            "address", "path", "file", "avatar"
    );

    private final AdminOperationLogMapper operationLogMapper;
    private final AdminOperationLogQueryMapper queryMapper;
    private final ObjectMapper objectMapper;

    /**
     * 给当前已登录管理员写日志。用户、茶叶、规则和店铺等后台写操作都调用这个方法。
     */
    public void recordCurrentAdmin(String module, String action, Long targetId, Map<String, ?> detail) {
        record(AdminCurrentUser.id(), module, action, targetId, detail);
    }

    /**
     * 给指定管理员写日志。登录成功时拦截器还没有建立 CurrentAdmin，所以登录场景使用此方法。
     */
    public void record(Long adminId, String module, String action, Long targetId, Map<String, ?> detail) {
        if (adminId == null || adminId <= 0) {
            throw new BizException(500, "操作日志缺少管理员信息");
        }
        validateModule(module);
        validateAction(action);

        AdminOperationLog log = new AdminOperationLog();
        log.setAdminId(adminId);
        log.setModule(module);
        log.setAction(action);
        log.setTargetId(targetId);
        log.setDetail(toSafeJson(detail));
        operationLogMapper.insert(log);
    }

    /**
     * 分页查询操作日志。管理员列表页和详情页都调用这个方法。
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminOperationLogListItemVO> page(
            long page,
            long pageSize,
            Long adminId,
            String module,
            String action,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validatePage(page, pageSize);
        if (adminId != null && adminId <= 0) {
            throw new BizException(400, "管理员 ID 必须大于 0");
        }

        String normalizedModule = normalizeOptional(module);
        String normalizedAction = normalizeOptional(action);
        if (normalizedModule != null) validateModule(normalizedModule);
        if (normalizedAction != null) validateAction(normalizedAction);
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BizException(400, "开始日期不能晚于结束日期");
        }

        LocalDateTime startAt = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endExclusiveAt = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        long offset = (page - 1) * pageSize;

        var list = queryMapper.selectPage(
                adminId, normalizedModule, normalizedAction, startAt, endExclusiveAt, offset, pageSize
        ).stream().map(this::toListItem).toList();
        long total = queryMapper.count(adminId, normalizedModule, normalizedAction, startAt, endExclusiveAt);
        return new AdminPageVO<>(list, page, pageSize, total);
    }

    /**
     * 将 detail 转换为安全的 JSON 字符串。禁止包含敏感字段。
     */
    private String toSafeJson(Map<String, ?> detail) {
        Map<String, ?> value = detail == null ? Map.of() : detail;
        for (String key : value.keySet()) {
            String lowerKey = key == null ? "" : key.toLowerCase(Locale.ROOT);
            boolean hasForbiddenKey = FORBIDDEN_DETAIL_KEYWORDS.stream().anyMatch(lowerKey::contains);
            if (hasForbiddenKey) {
                throw new BizException(500, "操作日志不能包含敏感字段：" + key);
            }
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new BizException(500, "操作日志内容生成失败");
        }
    }

    /**
     * 将查询结果行转换为列表项 VO。
     */
    private AdminOperationLogListItemVO toListItem(AdminOperationLogQueryRow row) {
        return new AdminOperationLogListItemVO(
                row.getId(),
                row.getAdminId(),
                row.getAdminUsername(),
                row.getAdminDisplayName(),
                row.getModule(),
                row.getAction(),
                row.getTargetId(),
                row.getDetail(),
                row.getCreatedAt()
        );
    }

    /**
     * 规范化可选参数：如果值为 null 或空白字符串，则返回 null；否则去除前后空格。
     */
    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    /**
     * 验证模块参数是否合法。
     */
    private void validateModule(String module) {
        if (!MODULES.contains(module)) {
            throw new BizException(400, "操作日志模块参数不正确");
        }
    }

    /**
     * 验证动作参数是否合法。
     */
    private void validateAction(String action) {
        if (!ACTIONS.contains(action)) {
            throw new BizException(400, "操作日志动作参数不正确");
        }
    }

    /**
     * 验证分页参数是否合法。
     */
    private void validatePage(long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BizException(400, "分页参数不正确");
        }
    }
}