package com.example.backend.admin.tea;

import com.example.backend.admin.tea.dto.AdminTeaSaveRequest;
import com.example.backend.admin.tea.dto.AdminTeaStatusRequest;
import com.example.backend.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/teas")
@RequiredArgsConstructor
public class AdminTeaController {
    private final AdminTeaService adminTeaService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") @Min(1) long page,
                          @RequestParam(defaultValue = "10") @Min(1) @Max(100) long pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) Integer status) {
        return Result.ok(adminTeaService.page(page, pageSize, keyword, category, status));
    }

    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.ok(adminTeaService.detail(id));
    }

    @GetMapping("/categories")
    public Result<?> categories() {
        return Result.ok(adminTeaService.categories());
    }

    @PostMapping
    public Result<?> create(@Valid @RequestBody AdminTeaSaveRequest request) {
        return Result.ok(adminTeaService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminTeaSaveRequest request) {
        adminTeaService.update(id, request);
        return Result.ok(null);
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody AdminTeaStatusRequest request) {
        adminTeaService.updateStatus(id, request.status());
        return Result.ok(null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminTeaService.delete(id);
        return Result.ok(null);
    }
}
