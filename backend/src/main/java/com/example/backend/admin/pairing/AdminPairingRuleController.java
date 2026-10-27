package com.example.backend.admin.pairing;

import com.example.backend.admin.pairing.dto.PairingRuleSaveRequest;
import com.example.backend.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pairing-rules")
@RequiredArgsConstructor
public class AdminPairingRuleController {
    private final AdminPairingRuleService service;

    /**
     * 分页查询配对规则列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param teaCategory 茶叶分类
     * @return 配对规则列表
     */
    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") @Min(1) long page,
                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
                          @RequestParam(required = false) String teaCategory) {
        return Result.ok(service.page(page, pageSize, teaCategory));
    }

    /**
     * 获取配对规则详情
     * @param id 配对规则ID
     * @return 配对规则详情
     */
    @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id) { return Result.ok(service.detail(id)); }

    /**
     * 创建配对规则
     * @param request 配对规则信息
     * @return 创建的配对规则ID
     */
    @PostMapping public Result<?> create(@Valid @RequestBody PairingRuleSaveRequest request) {
        return Result.ok(service.create(request));
    }

    /**
     * 更新配对规则
     * @param id 配对规则ID
     * @param request 配对规则信息
     * @return 无返回值
     */
    @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @Valid @RequestBody PairingRuleSaveRequest request) {
        service.update(id, request); return Result.ok(null);
    }

    /**
     * 删除配对规则
     * @param id 配对规则ID
     * @return 无返回值
     */
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) {
        service.delete(id); return Result.ok(null);
    }
}