package com.example.backend.point;

import com.example.backend.common.Result;
import com.example.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import com.example.backend.point.vo.PointTransactionPageVO;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {
    private final PointService pointService;

    /**
     * 查询积分账户
     * @return
     */
    @GetMapping("/account")
    public Result<Map<String, Object>> account() {
        Long userId = CurrentUser.id();
        return Result.ok(Map.of("balance", pointService.balance(userId), "brewGuideFreeAvailable", pointService.isBrewGuideFreeAvailable(userId), "dailyClaimed", pointService.isDailyClaimed(userId)));
    }

    /**
     * 每日领取 20 积分
     * @return
     */
    @PostMapping("/daily-claim")
    public Result<Map<String, Integer>> claimDaily() {
        return Result.ok(Map.of("balance", pointService.claimDaily(CurrentUser.id())));
    }
    @GetMapping("/transactions")
    public Result<PointTransactionPageVO> transactions(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(pointService.transactions(CurrentUser.id(), Math.max(page, 1), Math.min(Math.max(pageSize, 1), 50)));
    }
}
