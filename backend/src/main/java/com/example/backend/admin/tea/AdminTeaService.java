package com.example.backend.admin.tea;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.admin.tea.dto.AdminTeaSaveRequest;
import com.example.backend.admin.tea.mapper.AdminTeaMapper;
import com.example.backend.admin.tea.vo.AdminTeaDetailVO;
import com.example.backend.admin.tea.vo.AdminTeaListItemVO;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.common.BizException;
import com.example.backend.entity.Tea;
import com.example.backend.mapper.TeaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminTeaService {
    private final AdminTeaMapper adminTeaMapper;
    private final TeaMapper teaMapper;
    private final AdminOperationLogService operationLogService;

    /**
     * 获取茶叶详情
     * @param id 茶叶ID
     * @return 茶叶详情
     */
    public AdminTeaDetailVO detail(Long id) {
        AdminTeaDetailVO tea = adminTeaMapper.selectDetailById(id);
        if (tea == null) throw new BizException(40401, "茶叶不存在");
        return tea;
    }

    /**
     * 分页查询茶叶列表
     * @param page
     * @param pageSize
     * @param keyword
     * @param category
     * @param status
     * @return
     */
    public AdminPageVO<AdminTeaListItemVO> page(long page, long pageSize,
                                                 String keyword, String category, Integer status) {
        long offset = (page - 1) * pageSize;
        return new AdminPageVO<>(
                adminTeaMapper.selectPage(keyword, category, status, offset, pageSize),
                page, pageSize, adminTeaMapper.count(keyword, category, status)
        );
    }

    /**
     * 获取茶叶分类列表
     * @return 茶叶分类列表
     */
    public java.util.List<String> categories() {
        return adminTeaMapper.selectCategories();
    }

    /**
     * 创建茶叶
     * @param request 茶叶信息
     * @return 茶叶ID
     */
    @Transactional
    public Long create(AdminTeaSaveRequest request) {
        Tea tea = toEntity(null, request);
        teaMapper.insert(tea);
        operationLogService.recordCurrentAdmin("tea", "create", tea.getId(), Map.of(
                "name", tea.getName(),
                "category", tea.getCategory(),
                "enabled", tea.getEnabled()
        ));
        return tea.getId();
    }

    /**
     * 更新茶叶信息
     * @param id 茶叶ID
     * @param request 茶叶信息
     */
    @Transactional
    public void update(Long id, AdminTeaSaveRequest request) {
        detail(id);
        Tea tea = toEntity(id, request);
        teaMapper.update(null, new LambdaUpdateWrapper<Tea>()
                .eq(Tea::getId, tea.getId())
                .set(Tea::getName, tea.getName())
                .set(Tea::getCategory, tea.getCategory())
                .set(Tea::getNature, tea.getNature())
                .set(Tea::getCaffeine, tea.getCaffeine())
                .set(Tea::getFlavorTags, tea.getFlavorTags())
                .set(Tea::getEffectTags, tea.getEffectTags())
                .set(Tea::getSuitableStrength, tea.getSuitableStrength())
                .set(Tea::getBrewTemp, tea.getBrewTemp())
                .set(Tea::getBrewTime, tea.getBrewTime())
                .set(Tea::getSource, tea.getSource())
                .set(Tea::getImageUrl, tea.getImageUrl())
                .set(Tea::getEnabled, tea.getEnabled()));
        operationLogService.recordCurrentAdmin("tea", "update", id, Map.of(
                "name", tea.getName(),
                "category", tea.getCategory(),
                "enabled", tea.getEnabled()
        ));
    }

    /**
     * 更新茶叶状态
     * @param id 茶叶ID
     * @param status 茶叶状态
     */
    @Transactional
    public void updateStatus(Long id, Integer status) {
        AdminTeaDetailVO current = detail(id);
        Tea tea = new Tea();
        tea.setId(id);
        tea.setEnabled(status);
        teaMapper.updateById(tea);
        operationLogService.recordCurrentAdmin(
                "tea",
                status == 1 ? "enable" : "disable",
                id,
                Map.of("name", current.name(), "beforeStatus", current.enabled(), "afterStatus", status)
        );
    }

    /**
     * 删除一条茶叶资料。
     *
     * @param id 茶叶ID
     */
    @Transactional
    public void delete(Long id) {
        AdminTeaDetailVO current = detail(id);
        teaMapper.deleteById(id);
        operationLogService.recordCurrentAdmin("tea", "delete", id, Map.of(
                "name", current.name(),
                "category", current.category()
        ));
    }

    /**
     * 将请求对象转换为实体对象
     * @param id
     * @param request
     * @return
     */
    private Tea toEntity(Long id, AdminTeaSaveRequest request) {
        Tea tea = new Tea();
        tea.setId(id);
        tea.setName(request.name());
        tea.setCategory(request.category());
        tea.setNature(request.nature());
        tea.setCaffeine(request.caffeine());
        tea.setFlavorTags(request.flavorTags());
        tea.setEffectTags(request.effectTags());
        tea.setSuitableStrength(request.suitableStrength());
        tea.setBrewTemp(request.brewTemp());
        tea.setBrewTime(request.brewTime());
        tea.setSource(request.source());
        tea.setImageUrl(request.imageUrl() == null || request.imageUrl().isBlank()
                ? null : request.imageUrl().trim());
        tea.setEnabled(request.enabled());
        return tea;
    }
}
