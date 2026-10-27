package com.example.backend.point;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.BizException;
import com.example.backend.entity.UserPointAccount;
import com.example.backend.entity.UserPointTransaction;
import com.example.backend.mapper.UserPointAccountMapper;
import com.example.backend.mapper.UserPointTransactionMapper;
import com.example.backend.point.vo.PointTransactionPageVO;
import com.example.backend.point.vo.PointTransactionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PointService {
    private static final String PENDING = "PENDING";
    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";

    private final UserPointAccountMapper accountMapper;
    private final UserPointTransactionMapper transactionMapper;
    private final PointProperties properties;

    /** 每日领取积分。同一账户先加行锁，避免并发请求重复入账。 */
    @Transactional
    public int claimDaily(Long userId) {
        ensureAndLock(userId);
        String key = PointPolicy.dailyKey("daily-claim", userId, today());
        if (exists(key)) throw new BizException(40901, "今日积分已领取");
        return increase(userId, properties.getDailyClaim(), PointType.DAILY_CLAIM, key, "每日领取积分", SUCCESS);
    }

    @Transactional
    public void rewardFirstQuestionnaire(Long userId) {
        ensureAndLock(userId);
        String key = "questionnaire-first:" + userId;
        if (!exists(key)) {
            increase(userId, properties.getQuestionnaireFirst(), PointType.QUESTIONNAIRE_FIRST, key,
                    "首次完成饮茶问卷奖励", SUCCESS);
        }
    }

    @Transactional
    public void rewardDailyTeaRecord(Long userId, Long recordId) {
        ensureAndLock(userId);
        String key = PointPolicy.dailyKey("tea-record", userId, today());
        if (!exists(key)) {
            increase(userId, properties.getTeaRecordDaily(), PointType.TEA_RECORD_DAILY, key,
                    "每日首条茶记奖励", SUCCESS);
        }
    }

    /** 同一个 requestId 会命中同一业务键，重复到达时不会再次扣分。 */
    @Transactional
    public boolean prepareBrewGuide(Long userId, String requestId) {
        ensureAndLock(userId);
        String freeKey = brewFreeKey(userId, requestId);
        UserPointTransaction existingFree = transactionByKey(freeKey);
        if (existingFree != null) {
            if (FAILED.equals(existingFree.getStatus())) transitionStatus(freeKey, FAILED, PENDING);
            return true;
        }

        if (!hasBrewGuideFreeUsage(userId)) {
            record(userId, 0, PointType.AI_BREW_FREE, freeKey, "今日首次 AI 泡法免费", PENDING);
            return true;
        }

        deduct(userId, properties.getBrewGuideCost(), PointType.AI_BREW_DEBIT,
                "ai-brew:" + userId + ":" + requestId, "AI 泡法扣除积分");
        return false;
    }

    @Transactional
    public void completeFreeBrewGuide(Long userId, String requestId) {
        transitionStatus(brewFreeKey(userId, requestId), PENDING, SUCCESS);
    }

    /** AI 泡法失败时保留流水，只将免费预留标记为失败。 */
    @Transactional
    public void failFreeBrewGuide(Long userId, String requestId) {
        transitionStatus(brewFreeKey(userId, requestId), PENDING, FAILED);
    }

    @Transactional
    public void prepareChat(Long userId, String requestId) {
        ensureAndLock(userId);
        deduct(userId, properties.getChatCost(), PointType.AI_CHAT_DEBIT,
                "ai-chat:" + userId + ":" + requestId, "AI 问茶扣除积分");
    }

    @Transactional
    public void refundBrewGuide(Long userId, String requestId) {
        refund(userId, properties.getBrewGuideCost(), PointType.AI_BREW_REFUND,
                "ai-brew-refund:" + userId + ":" + requestId, "AI 泡法失败，积分已退回");
    }

    @Transactional
    public void refundChat(Long userId, String requestId) {
        refund(userId, properties.getChatCost(), PointType.AI_CHAT_REFUND,
                "ai-chat-refund:" + userId + ":" + requestId, "AI 问茶失败，积分已退回");
    }

    public PointTransactionPageVO transactions(Long userId, long page, long pageSize) {
        Page<UserPointTransaction> result = transactionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserPointTransaction>()
                        .eq(UserPointTransaction::getUserId, userId)
                        .orderByDesc(UserPointTransaction::getCreatedAt)
                        .orderByDesc(UserPointTransaction::getId));
        return new PointTransactionPageVO(result.getRecords().stream()
                .map(item -> new PointTransactionVO(item.getId(), item.getType(), item.getChangeAmount(), item.getBalanceAfter(),
                        item.getStatus(), item.getDescription(), item.getCreatedAt()))
                .toList(), page, pageSize, result.getTotal());
    }

    /** 后台查看某个用户实际扣除的积分流水。 */
    public PointTransactionPageVO consumptions(Long userId, long page, long pageSize) {
        Page<UserPointTransaction> result = transactionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<UserPointTransaction>()
                        .eq(UserPointTransaction::getUserId, userId)
                        .lt(UserPointTransaction::getChangeAmount, 0)
                        .orderByDesc(UserPointTransaction::getCreatedAt)
                        .orderByDesc(UserPointTransaction::getId));
        return new PointTransactionPageVO(result.getRecords().stream()
                .map(item -> new PointTransactionVO(item.getId(), item.getType(), item.getChangeAmount(), item.getBalanceAfter(),
                        item.getStatus(), item.getDescription(), item.getCreatedAt()))
                .toList(), page, pageSize, result.getTotal());
    }

    public int balance(Long userId) {
        ensure(userId);
        return currentBalance(userId);
    }

    public boolean isBrewGuideFreeAvailable(Long userId) {
        return !hasBrewGuideFreeUsage(userId);
    }

    public boolean isDailyClaimed(Long userId) {
        return exists(PointPolicy.dailyKey("daily-claim", userId, today()));
    }

    @Transactional
    public int grantByAdmin(Long adminId, Long userId, int amount, String reason) {
        ensureAndLock(userId);
        accountMapper.increase(userId, amount);
        UserPointTransaction transaction = new UserPointTransaction();
        transaction.setUserId(userId);
        transaction.setType(PointType.ADMIN_GRANT.name());
        transaction.setChangeAmount(amount);
        transaction.setBalanceAfter(currentBalance(userId));
        transaction.setBusinessKey("admin-grant:" + adminId + ":" + System.nanoTime());
        transaction.setStatus(SUCCESS);
        transaction.setDescription("管理员发放积分：" + reason.trim());
        transaction.setAdminId(adminId);
        transactionMapper.insert(transaction);
        return transaction.getBalanceAfter();
    }

    private void deduct(Long userId, int amount, PointType type, String key, String description) {
        if (exists(key)) return;
        if (accountMapper.deductIfEnough(userId, amount) != 1) {
            throw new BizException(40902, "积分不足，请先领取积分或完成任务");
        }
        record(userId, -amount, type, key, description, SUCCESS);
    }

    private void refund(Long userId, int amount, PointType type, String key, String description) {
        ensureAndLock(userId);
        if (!exists(key)) increase(userId, amount, type, key, description, SUCCESS);
    }

    private int increase(Long userId, int amount, PointType type, String key, String description, String status) {
        accountMapper.increase(userId, amount);
        record(userId, amount, type, key, description, status);
        return currentBalance(userId);
    }

    private void record(Long userId, int amount, PointType type, String key, String description, String status) {
        UserPointTransaction transaction = new UserPointTransaction();
        transaction.setUserId(userId);
        transaction.setType(type.name());
        transaction.setChangeAmount(amount);
        transaction.setBalanceAfter(currentBalance(userId));
        transaction.setBusinessKey(key);
        transaction.setStatus(status);
        transaction.setDescription(description);
        transactionMapper.insert(transaction);
    }

    private boolean hasBrewGuideFreeUsage(Long userId) {
        String legacyKey = PointPolicy.dailyKey("ai-brew-free", userId, today());
        String keyPrefix = legacyKey + ":";
        return transactionMapper.selectCount(new LambdaQueryWrapper<UserPointTransaction>()
                .eq(UserPointTransaction::getUserId, userId)
                .eq(UserPointTransaction::getType, PointType.AI_BREW_FREE.name())
                .in(UserPointTransaction::getStatus, PENDING, SUCCESS)
                .and(query -> query.eq(UserPointTransaction::getBusinessKey, legacyKey)
                        .or().likeRight(UserPointTransaction::getBusinessKey, keyPrefix))) > 0;
    }

    private UserPointTransaction transactionByKey(String key) {
        return transactionMapper.selectOne(new LambdaQueryWrapper<UserPointTransaction>()
                .eq(UserPointTransaction::getBusinessKey, key));
    }

    private boolean transitionStatus(String key, String from, String to) {
        UserPointTransaction update = new UserPointTransaction();
        update.setStatus(to);
        return transactionMapper.update(update, new LambdaQueryWrapper<UserPointTransaction>()
                .eq(UserPointTransaction::getBusinessKey, key)
                .eq(UserPointTransaction::getStatus, from)) == 1;
    }

    private boolean exists(String key) {
        return transactionMapper.selectCount(new LambdaQueryWrapper<UserPointTransaction>()
                .eq(UserPointTransaction::getBusinessKey, key)) > 0;
    }

    private void ensureAndLock(Long userId) {
        ensure(userId);
        accountMapper.lockByUserId(userId);
    }

    private void ensure(Long userId) {
        accountMapper.createIfAbsent(userId);
    }

    private int currentBalance(Long userId) {
        return accountMapper.selectById(userId).getBalance();
    }

    private String brewFreeKey(Long userId, String requestId) {
        return PointPolicy.dailyKey("ai-brew-free", userId, today()) + ":" + requestId;
    }

    private LocalDate today() {
        return LocalDate.now(ZoneId.of(properties.getTimezone()));
    }
}
