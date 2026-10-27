package com.example.backend.admin.store;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.admin.store.dto.AdminStoreSaveRequest;
import com.example.backend.admin.store.dto.StorePromotionRequest;
import com.example.backend.admin.store.vo.AdminStoreDetailVO;
import com.example.backend.admin.store.vo.AdminStoreListItemVO;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.PairingStore;
import com.example.backend.mapper.PairingStoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminStoreService {
    private static final Set<String> TEA_CATEGORIES = Set.of(
            "绿茶", "白茶", "黄茶", "青茶", "红茶", "黑茶", "花草"
    );

    private final PairingStoreMapper pairingStoreMapper;
    private final AdminOperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询店铺
     * @param page 页码，从1开始
     * @param pageSize 每页数量，最大100
     * @param keyword 店铺名称或地址关键字
     * @param status 店铺状态，0-关闭，1-开启
     * @param promotionEnabled 推广状态，0-关闭，1-开启
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminStoreListItemVO> page(
            long page, long pageSize, String keyword, Integer status, Integer promotionEnabled
    ) {
        validatePage(page, pageSize);
        validateBinary(status, "店铺状态不正确");
        validateBinary(promotionEnabled, "推广状态不正确");
        String normalizedKeyword = blankToNull(keyword);

        Page<PairingStore> result = pairingStoreMapper.selectPage(
                new Page<>(page, pageSize),
                new LambdaQueryWrapper<PairingStore>()
                        .and(normalizedKeyword != null, query -> query
                                .like(PairingStore::getStoreName, normalizedKeyword)
                                .or()
                                .like(PairingStore::getAddress, normalizedKeyword))
                        .eq(status != null, PairingStore::getStatus, status)
                        .eq(promotionEnabled != null, PairingStore::getPromotionEnabled, promotionEnabled)
                        .orderByDesc(PairingStore::getUpdatedAt)
                        .orderByDesc(PairingStore::getId)
        );

        return new AdminPageVO<>(
                result.getRecords().stream().map(this::toListItem).toList(),
                page, pageSize, result.getTotal()
        );
    }

    /**
     * 获取店铺详情
     * @param id 店铺ID
     * @return 店铺详情
     */
    @Transactional(readOnly = true)
    public AdminStoreDetailVO detail(Long id) {
        return toDetail(requireStore(id));
    }

    /**
     * 创建店铺
     * @param request 店铺信息
     * @return 创建的店铺ID
     */
    @Transactional
    public Long create(AdminStoreSaveRequest request) {
        validateStoreRequest(request);
        PairingStore store = new PairingStore();
        fillBasicFields(store, request);
        store.setStatus(1);
        store.setPromotionEnabled(0);
        store.setPromotionScore(0);
        pairingStoreMapper.insert(store);
        operationLogService.recordCurrentAdmin("pairing_store", "create", store.getId(), Map.of(
                "storeName", store.getStoreName(),
                "teaCategoryCount", request.teaCategories().size(),
                "foodTagCount", request.foodTags().size()
        ));
        return store.getId();
    }

    /**
     * 更新店铺信息
     * @param id 店铺ID
     * @param request 店铺信息
     */
    @Transactional
    public void update(Long id, AdminStoreSaveRequest request) {
        validateStoreRequest(request);
        requireStore(id);
        PairingStore store = new PairingStore();
        store.setId(id);
        fillBasicFields(store, request);
        pairingStoreMapper.update(null, new LambdaUpdateWrapper<PairingStore>()
                .eq(PairingStore::getId, store.getId())
                .set(PairingStore::getStoreName, store.getStoreName())
                .set(PairingStore::getCoverImage, store.getCoverImage())
                .set(PairingStore::getAddress, store.getAddress())
                .set(PairingStore::getLongitude, store.getLongitude())
                .set(PairingStore::getLatitude, store.getLatitude())
                .set(PairingStore::getPhone, store.getPhone())
                .set(PairingStore::getTeaCategories, store.getTeaCategories())
                .set(PairingStore::getFoodTags, store.getFoodTags())
                .set(PairingStore::getDescription, store.getDescription()));
        operationLogService.recordCurrentAdmin("pairing_store", "update", id, Map.of(
                "storeName", store.getStoreName(),
                "teaCategoryCount", request.teaCategories().size(),
                "foodTagCount", request.foodTags().size()
        ));
    }

    /**
     * 更新店铺状态
     * @param id 店铺ID
     * @param status 店铺状态，0-关闭，1-开启
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        validateBinary(status, "店铺状态不正确");
        PairingStore store = requireStore(id);
        pairingStoreMapper.update(null, new UpdateWrapper<PairingStore>()
                .eq("id", id)
                .set("status", status));
        operationLogService.recordCurrentAdmin(
                "pairing_store",
                status == 1 ? "enable" : "disable",
                id,
                Map.of("beforeStatus", store.getStatus(), "afterStatus", status)
        );
    }

    /**
     * 更新店铺推广信息
     * @param id 店铺ID
     * @param request 推广信息
     */
    @Transactional
    public void updatePromotion(Long id, StorePromotionRequest request) {
        validatePromotion(request);
        requireStore(id);
        UpdateWrapper<PairingStore> update = new UpdateWrapper<PairingStore>()
                .eq("id", id)
                .set("promotion_enabled", request.promotionEnabled());

        if (request.promotionEnabled() == 1) {
            update.set("promotion_score", request.promotionScore())
                    .set("promotion_start_at", request.promotionStartAt())
                    .set("promotion_end_at", request.promotionEndAt());
        } else {
            update.set("promotion_score", 0)
                    .set("promotion_start_at", null)
                    .set("promotion_end_at", null);
        }
        pairingStoreMapper.update(null, update);

        LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
        detail.put("promotionEnabled", request.promotionEnabled());
        detail.put("promotionScore", request.promotionEnabled() == 1 ? request.promotionScore() : 0);
        if (request.promotionEnabled() == 1) {
            detail.put("startAt", request.promotionStartAt());
            detail.put("endAt", request.promotionEndAt());
        }
        operationLogService.recordCurrentAdmin("pairing_store", "promotion", id, detail);
    }

    /**
     * 删除一条店铺资料，并保留删除操作日志。
     *
     * @param id 店铺ID
     */
    @Transactional
    public void delete(Long id) {
        PairingStore store = requireStore(id);
        pairingStoreMapper.deleteById(id);
        LinkedHashMap<String, Object> detail = new LinkedHashMap<>();
        detail.put("storeName", store.getStoreName());
        operationLogService.recordCurrentAdmin("pairing_store", "delete", id, detail);
    }

/**
     * 验证店铺请求参数
     * @param request 店铺请求信息
     */
    private void validateStoreRequest(AdminStoreSaveRequest request) {
        if ((request.longitude() == null) != (request.latitude() == null)) {
            throw new BizException(400, "经度和纬度必须同时填写");
        }
        for (String category : request.teaCategories()) {
            if (category == null || !TEA_CATEGORIES.contains(category.trim())) {
                throw new BizException(400, "包含不支持的茶类");
            }
        }
        if (request.teaCategories().stream().map(String::trim).distinct().count() != request.teaCategories().size()) {
            throw new BizException(400, "茶类不能重复");
        }
        if (request.foodTags().stream().anyMatch(tag -> tag == null || tag.isBlank())
                || request.foodTags().stream().map(String::trim).distinct().count() != request.foodTags().size()) {
            throw new BizException(400, "食品标签不能为空或重复");
        }
    }

    /**
     * 验证推广信息
     * @param request 推广信息
     */
    private void validatePromotion(StorePromotionRequest request) {
        if (request.promotionEnabled() == 0) {
            return;
        }
        if (request.promotionStartAt() == null || request.promotionEndAt() == null) {
            throw new BizException(400, "开启推广时必须填写开始和结束时间");
        }
        if (!request.promotionEndAt().isAfter(request.promotionStartAt())) {
            throw new BizException(400, "推广结束时间必须晚于开始时间");
        }
    }

    /**
     * 填充店铺基本信息
     * @param store 店铺实体
     * @param request 店铺请求信息
     */
    private void fillBasicFields(PairingStore store, AdminStoreSaveRequest request) {
        store.setStoreName(request.storeName().trim());
        store.setCoverImage(blankToNull(request.coverImage()));
        store.setAddress(request.address().trim());
        store.setLongitude(request.longitude());
        store.setLatitude(request.latitude());
        store.setPhone(blankToNull(request.phone()));
        store.setDescription(request.description().trim());
        store.setTeaCategories(writeStringList(request.teaCategories(), "店铺茶类转换失败"));
        store.setFoodTags(writeStringList(request.foodTags(), "食品标签转换失败"));
    }

    /**
     * 根据ID获取店铺实体，如果不存在则抛出异常
     * @param id 店铺ID
     * @return 店铺实体
     */
    private PairingStore requireStore(Long id) {
        PairingStore store = pairingStoreMapper.selectById(id);
        if (store == null) {
            throw new BizException(404, "店铺不存在");
        }
        return store;
    }

    /**
     * 将店铺实体转换为列表项VO
     * @param store 店铺实体
     * @return 列表项VO
     */
    private AdminStoreListItemVO toListItem(PairingStore store) {
        return new AdminStoreListItemVO(
                store.getId(), store.getStoreName(), store.getCoverImage(), store.getAddress(),
                readStringList(store.getTeaCategories()), store.getStatus(), store.getPromotionEnabled(),
                store.getPromotionStartAt(), store.getPromotionEndAt(), store.getUpdatedAt()
        );
    }

    /**
     * 将店铺实体转换为详情VO
     * @param store 店铺实体
     * @return 详情VO
     */
    private AdminStoreDetailVO toDetail(PairingStore store) {
        return new AdminStoreDetailVO(
                store.getId(), store.getStoreName(), store.getCoverImage(), store.getAddress(),
                store.getLongitude(), store.getLatitude(), store.getPhone(),
                readStringList(store.getTeaCategories()), readStringList(store.getFoodTags()), store.getDescription(),
                store.getPromotionEnabled(), store.getPromotionScore(), store.getPromotionStartAt(),
                store.getPromotionEndAt(), store.getStatus(), store.getCreatedAt(), store.getUpdatedAt()
        );
    }

    /**
     * 读取字符串列表
     * @param value JSON字符串
     * @return 字符串列表
     */
    private List<String> readStringList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() { });
        } catch (JacksonException exception) {
            throw new BizException(500, "店铺标签数据格式错误");
        }
    }

    /**
     * 写入字符串列表
     * @param values 字符串列表
     * @param errorMessage 错误信息
     * @return JSON字符串
     */
    private String writeStringList(List<String> values, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(values.stream().map(String::trim).distinct().toList());
        } catch (JacksonException exception) {
            throw new BizException(500, errorMessage);
        }
    }

    /**
     * 验证分页参数
     * @param page 页码，从1开始
     * @param pageSize 每页数量，最大100
     */
    private void validatePage(long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BizException(400, "分页参数不正确");
        }
    }

    /**
     * 验证二进制参数
     * @param value 参数值，0或1
     * @param errorMessage 错误信息
     */
    private void validateBinary(Integer value, String errorMessage) {
        if (value != null && value != 0 && value != 1) {
            throw new BizException(400, errorMessage);
        }
    }

    /**
     * 将空字符串转换为null
     * @param value 字符串
     * @return null或去除空格的字符串
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
