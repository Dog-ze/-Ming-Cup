package com.example.backend.admin.pairing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.admin.pairing.dto.PairingFoodSaveRequest;
import com.example.backend.admin.pairing.dto.PairingRuleSaveRequest;
import com.example.backend.admin.pairing.vo.AdminPairingRuleVO;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.common.BizException;
import com.example.backend.entity.PairingRule;
import com.example.backend.mapper.PairingRuleMapper;
import com.example.backend.pairing.vo.PairingFoodVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminPairingRuleService {
    private static final Set<String> CATEGORIES = Set.of("绿茶", "白茶", "黄茶", "青茶", "红茶", "黑茶", "花草");
    private final PairingRuleMapper pairingRuleMapper;
    private final AdminOperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询搭配规则
     * @param page
     * @param pageSize
     * @param teaCategory
     * @return
     */
    @Transactional(readOnly = true)
    public AdminPageVO<AdminPairingRuleVO> page(long page, long pageSize, String teaCategory) {
        validatePage(page, pageSize);
        String category = teaCategory == null ? "" : teaCategory.trim();
        if (!category.isBlank()) validateCategory(category);
        Page<PairingRule> result = pairingRuleMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<PairingRule>()
                        .eq(!category.isBlank(), PairingRule::getTeaCategory, category)
                        .orderByDesc(PairingRule::getUpdatedAt));
        return new AdminPageVO<>(result.getRecords().stream().map(this::toVO).toList(), page, pageSize, result.getTotal());
    }

    /**
     * 获取搭配规则详情
     * @param id
     * @return
     */
    @Transactional(readOnly = true)
    public AdminPairingRuleVO detail(Long id) {
        return toVO(requireRule(id));
    }

    /**
     * 创建搭配规则
     * @param request
     * @return
     */
    @Transactional
    public Long create(PairingRuleSaveRequest request) {
        String category = normalizeAndValidateCategory(request.teaCategory());
        if (pairingRuleMapper.selectCount(new LambdaQueryWrapper<PairingRule>().eq(PairingRule::getTeaCategory, category)) > 0) {
            throw new BizException(409, "该茶类已经有搭配规则，请编辑原规则");
        }
        PairingRule rule = new PairingRule();
        copy(rule, category, request);
        pairingRuleMapper.insert(rule);
        operationLogService.recordCurrentAdmin("pairing_rule", "create", rule.getId(), Map.of(
                "teaCategory", category,
                "foodCount", request.foods().size()
        ));
        return rule.getId();
    }

    /**
     * 更新搭配规则
     * @param id
     * @param request
     */
    @Transactional
    public void update(Long id, PairingRuleSaveRequest request) {
        PairingRule rule = requireRule(id);
        String category = normalizeAndValidateCategory(request.teaCategory());
        PairingRule duplicate = pairingRuleMapper.selectOne(new LambdaQueryWrapper<PairingRule>().eq(PairingRule::getTeaCategory, category));
        if (duplicate != null && !duplicate.getId().equals(id)) throw new BizException(409, "该茶类已经有搭配规则，请编辑原规则");
        copy(rule, category, request);
        pairingRuleMapper.updateById(rule);
        operationLogService.recordCurrentAdmin("pairing_rule", "update", id, Map.of(
                "teaCategory", category,
                "foodCount", request.foods().size()
        ));
    }

    /**
     * 删除搭配规则
     * @param id
     */
    @Transactional
    public void delete(Long id) {
        PairingRule rule = requireRule(id);
        pairingRuleMapper.deleteById(id);
        operationLogService.recordCurrentAdmin("pairing_rule", "delete", id, Map.of(
                "teaCategory", rule.getTeaCategory()
        ));
    }

    /**
     * 将请求数据复制到实体
     * @param rule
     * @param category
     * @param request
     */
    private void copy(PairingRule rule, String category, PairingRuleSaveRequest request) {
        rule.setTeaCategory(category);
        rule.setFoods(writeFoods(request.foods()));
        rule.setAdvice(request.advice().trim());
    }

    /**
     * 将实体转换为 VO
     * @param rule
     * @return
     */
    private AdminPairingRuleVO toVO(PairingRule rule) {
        return new AdminPairingRuleVO(rule.getId(), rule.getTeaCategory(), readFoods(rule.getFoods()), rule.getAdvice(), rule.getUpdatedAt());
    }

    /**
     * 读取食品列表
     * @param foods
     * @return
     */
    private List<PairingFoodVO> readFoods(String foodsJson) {
        if (foodsJson == null || foodsJson.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(foodsJson);
            if (!root.isArray()) return List.of();
            List<PairingFoodVO> foods = new ArrayList<>();
            for (JsonNode node : root) {
                if (node.isTextual()) {
                    String name = node.asText().trim();
                    if (!name.isEmpty()) foods.add(new PairingFoodVO(name, null));
                    continue;
                }
                String name = node.path("name").asText().trim();
                String imageUrl = node.path("imageUrl").asText().trim();
                if (!name.isEmpty()) foods.add(new PairingFoodVO(name, imageUrl.isEmpty() ? null : imageUrl));
            }
            return foods;
        } catch (JacksonException exception) {
            return List.of();
        }
    }

    /**
     * 写入食品列表
     * @param foods
     * @return
     */
    private String writeFoods(List<PairingFoodSaveRequest> foods) {
        LinkedHashMap<String, PairingFoodVO> normalized = new LinkedHashMap<>();
        for (PairingFoodSaveRequest food : foods) {
            String name = food.name().trim();
            String imageUrl = food.imageUrl() == null ? null : food.imageUrl().trim();
            normalized.putIfAbsent(name, new PairingFoodVO(name, imageUrl == null || imageUrl.isEmpty() ? null : imageUrl));
        }
        if (normalized.isEmpty()) throw new BizException(400, "请至少填写一种推荐食品");
        try { return objectMapper.writeValueAsString(normalized.values()); }
        catch (JacksonException exception) { throw new BizException(500, "食品信息保存失败"); }
    }

    /**
     * 获取搭配规则，如果不存在则抛出异常
     * @param id
     * @return
     */
    private PairingRule requireRule(Long id) {
        PairingRule rule = pairingRuleMapper.selectById(id);
        if (rule == null) throw new BizException(404, "搭配规则不存在");
        return rule;
    }

    /**
     * 规范化并验证茶类
     * @param category
     * @return
     */
    private String normalizeAndValidateCategory(String category) {
        String value = category.trim();
        validateCategory(value);
        return value;
    }

    /**
     * 验证茶类是否正确
     * @param category
     */
    private void validateCategory(String category) {
        if (!CATEGORIES.contains(category)) throw new BizException(400, "请选择正确的茶类");
    }

    /**
     * 验证分页参数
     * @param page
     * @param pageSize
     */
    private void validatePage(long page, long pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new BizException(400, "分页参数不正确");
    }

}
