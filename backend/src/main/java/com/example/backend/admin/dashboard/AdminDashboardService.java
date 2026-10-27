package com.example.backend.admin.dashboard;

import com.example.backend.admin.dashboard.mapper.AdminDashboardMapper;
import com.example.backend.admin.dashboard.mapper.UserGrowthRow;
import com.example.backend.admin.dashboard.vo.DashboardVO;
import com.example.backend.admin.dashboard.vo.UserGrowthPointVO;
import com.example.backend.admin.dashboard.vo.UserGrowthVO;
import com.example.backend.common.BizException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
public class AdminDashboardService {
    private static final int DEFAULT_TREND_DAYS = 30;
    private static final Set<Integer> ALLOWED_TREND_DAYS = Set.of(7, 30, 90);

    private final AdminDashboardMapper dashboardMapper;
    private final Clock dashboardClock;

    /**
     * 获取后台首页数据，包括用户总数、启用用户数、茶叶总数、推广店铺总数、活跃推广店铺数、茶叶记录总数、问卷总数，以及用户增长趋势。
     * @param requestedDays
     * @return
     */
    public DashboardVO getDashboard(Integer requestedDays) {
        int days = resolveTrendDays(requestedDays);
        LocalDate today = LocalDate.now(dashboardClock);
        LocalDate startDate = today.minusDays(days - 1L);
        LocalDate tomorrow = today.plusDays(1);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endExclusiveAt = tomorrow.atStartOfDay();
        LocalDateTime now = LocalDateTime.now(dashboardClock);

        UserGrowthVO userGrowth = buildUserGrowth(days, startDate, startAt, endExclusiveAt);

        return new DashboardVO(
                dashboardMapper.countAllUsers(),
                dashboardMapper.countEnabledUsers(),
                dashboardMapper.countTeas(),
                dashboardMapper.countPromotionPartnerStores(),
                dashboardMapper.countActivePromotionStores(now),
                dashboardMapper.countTeaRecordsBetween(today.atStartOfDay(), endExclusiveAt),
                dashboardMapper.countQuestionnaires(),
                userGrowth
        );
    }

    /**
     * 解析请求的趋势天数，如果未提供则使用默认值。如果提供的天数不在允许的范围内，则抛出异常。
     * @param requestedDays 请求的趋势天数
     * @return 解析后的趋势天数
     */
    private int resolveTrendDays(Integer requestedDays) {
        if (requestedDays == null) {
            return DEFAULT_TREND_DAYS;
        }
        if (!ALLOWED_TREND_DAYS.contains(requestedDays)) {
            throw new BizException(400, "趋势范围仅支持 7、30 或 90 天");
        }
        return requestedDays;
    }

    /**
     * 构建用户增长数据，包括每天的新用户数量和总新用户数量。
     * @param days
     * @param startDate
     * @param startAt
     * @param endExclusiveAt
     * @return
     */
    private UserGrowthVO buildUserGrowth(int days, LocalDate startDate,
                                         LocalDateTime startAt, LocalDateTime endExclusiveAt) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (UserGrowthRow row : dashboardMapper.selectUserGrowth(startAt, endExclusiveAt)) {
            if (row.getDate() != null && row.getNewUserCount() != null) {
                counts.put(row.getDate(), row.getNewUserCount());
            }
        }

        List<UserGrowthPointVO> points = IntStream.range(0, days)
                .mapToObj(offset -> {
                    LocalDate date = startDate.plusDays(offset);
                    return new UserGrowthPointVO(date, counts.getOrDefault(date, 0L));
                })
                .toList();
        long totalNewUsers = points.stream().mapToLong(UserGrowthPointVO::newUserCount).sum();
        return new UserGrowthVO(days, totalNewUsers, points);
    }
}