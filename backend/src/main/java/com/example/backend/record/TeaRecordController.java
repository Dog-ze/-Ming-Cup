package com.example.backend.record;

import com.example.backend.common.Result;
import com.example.backend.record.dto.TeaRecordSaveRequest;
import com.example.backend.record.vo.PageVO;
import com.example.backend.record.vo.TeaRecordVO;
import com.example.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/records")
@AllArgsConstructor
public class TeaRecordController {
    private final TeaRecordService teaRecordService;

    @PostMapping
    public Result<TeaRecordVO> create(@Valid @RequestBody TeaRecordSaveRequest request) {
        return Result.ok(teaRecordService.create(CurrentUser.id(), request));
    }

    @GetMapping
    public Result<PageVO<TeaRecordVO>> list(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) long pageSize
    ) {
        return Result.ok(teaRecordService.list(CurrentUser.id(), page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<TeaRecordVO> detail(@PathVariable Long id) {
        return Result.ok(teaRecordService.getDetail(CurrentUser.id(), id));
    }

    @PutMapping("/{id}")
    public Result<TeaRecordVO> update(
            @PathVariable Long id,
            @Valid @RequestBody TeaRecordSaveRequest request
    ) {
        return Result.ok(teaRecordService.update(CurrentUser.id(), id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(teaRecordService.delete(CurrentUser.id(), id));
    }
}