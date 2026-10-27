package com.example.backend.admin.store;

import com.example.backend.admin.store.dto.AdminStoreSaveRequest;
import com.example.backend.admin.store.dto.StorePromotionRequest;
import com.example.backend.admin.store.dto.StoreStatusRequest;
import com.example.backend.common.Result;
import com.example.backend.common.UploadUrl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/pairing-stores")
@RequiredArgsConstructor
public class AdminStoreController {
    private final AdminStoreService service;
    private final AdminStoreImageService imageService;

    /**
     * 分页查询门店列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param keyword 关键字
     * @param status 门店状态
     * @param promotionEnabled 是否开启推广
     * @return 门店列表
     */
    @GetMapping public Result<?> page(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "10") long pageSize, @RequestParam(required = false) String keyword, @RequestParam(required = false) Integer status, @RequestParam(required = false) Integer promotionEnabled) { return Result.ok(service.page(page, pageSize, keyword, status, promotionEnabled)); }

    /**
     * 获取门店详情
     * @param id 门店ID
     * @return 门店详情
     */
    @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id) { return Result.ok(service.detail(id)); }

    /**
     * 创建门店
     * @param request 门店信息
     * @return 创建的门店ID
     */
    @PostMapping public Result<?> create(@Valid @RequestBody AdminStoreSaveRequest request) { return Result.ok(service.create(request)); }

    /**
     * 更新门店
     * @param id 门店ID
     * @param request 门店信息
     * @return 无返回值
     */
    @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminStoreSaveRequest request) { service.update(id, request); return Result.ok(null); }

    /**
     * 更新门店状态
     * @param id
     * @param request
     * @return
     */
    @PutMapping("/{id}/status") public Result<Void> status(@PathVariable Long id, @Valid @RequestBody StoreStatusRequest request) { service.updateStatus(id, request.status()); return Result.ok(null); }

    /**
     * 更新门店推广信息
     * @param id 门店ID
     * @param request 门店推广信息
     * @return 无返回值
     */
    @PutMapping("/{id}/promotion") public Result<Void> promotion(@PathVariable Long id, @Valid @RequestBody StorePromotionRequest request) { service.updatePromotion(id, request); return Result.ok(null); }

    /**
     * 删除店铺资料。
     * @param id 店铺ID
     * @return 无返回值
     */
    @DeleteMapping("/{id}") public Result<Void> delete(@PathVariable Long id) { service.delete(id); return Result.ok(null); }

    /**
     * 上传门店图片
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping("/images") public Result<String> upload(@RequestPart("file") MultipartFile file) {
        String relativePath = imageService.store(file);
        String imageUrl = UploadUrl.relative("/uploads/stores/", relativePath);
        return Result.ok(imageUrl);
    }
}
