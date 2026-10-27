package com.example.backend.admin.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.admin.user.vo.AdminTeaRecordSummaryVO;
import com.example.backend.admin.user.vo.AdminUserDetailVO;
import com.example.backend.admin.user.vo.AdminUserListItemVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.TeaRecord;
import com.example.backend.entity.User;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaRecordMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserPointAccountMapper;
import com.example.backend.point.PointService;
import com.example.backend.point.vo.PointTransactionPageVO;
import com.example.backend.security.AdminCurrentUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AdminUserService {
    private final UserMapper userMapper;
    private final QuestionnaireMapper questionnaireMapper;
    private final TeaRecordMapper teaRecordMapper;
    private final AdminOperationLogService operationLogService;
    private final UserPointAccountMapper pointAccountMapper;
    private final PointService pointService;

    /**
     * 分页查询用户列表
     * @param page
     * @param pageSize
     * @param keyword
     * @param status
     * @return
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminUserListItemVO> list(long page, long pageSize, String keyword, Integer status) {
        validatePage(page, pageSize);
        if (status != null && status != 0 && status != 1) throw new BizException(400, "用户状态只能是 0 或 1");
        String value = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>().eq(status != null, User::getStatus, status)
                .and(!value.isBlank(), wrapper -> wrapper.like(User::getUsername, value)
                        .or().like(User::getPhone, value).or().like(User::getEmail, value))
                .orderByDesc(User::getCreatedAt);
        Page<User> result = userMapper.selectPage(new Page<>(page, pageSize), query);
        List<Long> userIds = result.getRecords().stream().map(User::getId).toList();
        Map<Long, Integer> balances = userIds.isEmpty() ? Map.of() : pointAccountMapper.selectBalancesByUserIds(userIds)
                .stream().collect(Collectors.toMap(row -> ((Number) row.get("userId")).longValue(), row -> ((Number) row.get("balance")).intValue()));
        List<AdminUserListItemVO> list = result.getRecords().stream().map(user -> toListItem(user, balances.getOrDefault(user.getId(), 0))).toList();
        return new AdminPageVO<>(list, page, pageSize, result.getTotal());
    }

    /**
     * 获取用户详情，包括基本信息、问卷完成情况和最近的茶叶记录
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public AdminUserDetailVO getDetail(Long id) {
        User user = requireUser(id);
        boolean questionnaireCompleted = questionnaireMapper.selectCount(new LambdaQueryWrapper<Questionnaire>()
                .eq(Questionnaire::getUserId, id)) > 0;
        List<AdminTeaRecordSummaryVO> records = teaRecordMapper.selectList(new LambdaQueryWrapper<TeaRecord>()
                .eq(TeaRecord::getUserId, id).orderByDesc(TeaRecord::getCreatedAt).last("LIMIT 5"))
                .stream().map(record -> new AdminTeaRecordSummaryVO(record.getId(), record.getTeaName(),
                        record.getCategory(), record.getRating(), record.getCreatedAt())).toList();
        return new AdminUserDetailVO(user.getId(), user.getUsername(), user.getPhone(), user.getEmail(),
                user.getAvatar(), user.getCity(), user.getStatus(), user.getCreatedAt(), questionnaireCompleted, pointsBalance(id), records);
    }

    /**
     * 更新用户状态
     * @param id
     * @param status
     * @return
     */
    @Transactional
    public AdminUserListItemVO updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) throw new BizException(400, "用户状态只能是 0 或 1");
        User user = requireUser(id);
        if (!status.equals(user.getStatus())) {
            Integer beforeStatus = user.getStatus();
            user.setStatus(status);
            userMapper.updateById(user);
            operationLogService.recordCurrentAdmin(
                    "user",
                    status == 1 ? "enable" : "disable",
                    id,
                    Map.of("beforeStatus", beforeStatus, "afterStatus", status)
            );
        }
        return toListItem(user);
    }

    @Transactional
    public int grantPoints(Long userId, int amount, String reason) {
        requireUser(userId);
        int balance = pointService.grantByAdmin(AdminCurrentUser.id(), userId, amount, reason);
        operationLogService.recordCurrentAdmin("user", "update", userId, Map.of("operation", "point_grant", "amount", amount, "reason", reason.trim()));
        return balance;
    }

    @Transactional(readOnly = true)
    public PointTransactionPageVO pointConsumptions(Long userId, long page, long pageSize) {
        validatePage(page, pageSize);
        requireUser(userId);
        return pointService.consumptions(userId, page, pageSize);
    }

    /**
     * 获取用户信息，如果不存在则抛出异常
     * @param id
     * @return
     */
    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BizException(404, "用户不存在");
        return user;
    }

    /**
     * 验证分页参数是否合法，页码和每页数量必须大于 0，每页数量不能超过 100
     * @param page
     * @param pageSize
     */
    private void validatePage(long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new BizException(400, "分页参数不正确");
    }

    /**
     * 将 User 实体转换为 AdminUserListItemVO
     * @param user
     * @return
     */
    private AdminUserListItemVO toListItem(User user) { return toListItem(user, pointsBalance(user.getId())); }
    private AdminUserListItemVO toListItem(User user, int balance) {
        return new AdminUserListItemVO(user.getId(), user.getUsername(), user.getPhone(), user.getEmail(),
                user.getCity(), user.getStatus(), balance, user.getCreatedAt());
    }

    private int pointsBalance(Long userId) {
        if (pointAccountMapper == null) return 0;
        var account = pointAccountMapper.selectById(userId);
        return account == null ? 0 : account.getBalance();
    }
}
