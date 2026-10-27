package com.example.backend.admin.user;

import com.example.backend.admin.user.dto.UserStatusUpdateRequest;
import com.example.backend.admin.user.dto.AdminPointGrantRequest;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.admin.user.vo.AdminUserDetailVO;
import com.example.backend.admin.user.vo.AdminUserListItemVO;
import com.example.backend.common.Result;
import com.example.backend.point.vo.PointTransactionPageVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService adminUserService;
    public AdminUserController(AdminUserService adminUserService) { this.adminUserService = adminUserService; }

    /**
     * 获取用户列表
     * @param page
     * @param pageSize
     * @param keyword
     * @param status
     * @return
     */
    @GetMapping
    public Result<AdminPageVO<AdminUserListItemVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(adminUserService.list(page, pageSize, keyword, status));
    }

    /**
     * 获取用户详情
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<AdminUserDetailVO> detail(@PathVariable Long id) {
        return Result.ok(adminUserService.getDetail(id));
    }

    /**
     * 更新用户状态
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/{id}/status")
    public Result<AdminUserListItemVO> updateStatus(@PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return Result.ok(adminUserService.updateStatus(id, request.status()));
    }

    @PostMapping("/{id}/points/grant")
    public Result<Integer> grantPoints(@PathVariable Long id, @Valid @RequestBody AdminPointGrantRequest request) {
        return Result.ok(adminUserService.grantPoints(id, request.amount(), request.reason()));
    }

    @GetMapping("/{id}/points/consumptions")
    public Result<PointTransactionPageVO> pointConsumptions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(adminUserService.pointConsumptions(id, page, pageSize));
    }
}
