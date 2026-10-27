package com.example.backend.admin.dashboard;

import com.example.backend.admin.dashboard.vo.DashboardVO;
import com.example.backend.common.Result;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@AllArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping
    public Result<DashboardVO> getDashboard(
            @RequestParam(required = false) Integer userTrendDays
    ) {
        return Result.ok(adminDashboardService.getDashboard(userTrendDays));
    }
}