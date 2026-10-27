package com.example.backend.pairing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.BizException;
import com.example.backend.entity.PairingRule;
import com.example.backend.mapper.PairingRuleMapper;
import com.example.backend.pairing.vo.PairingFoodVO;
import com.example.backend.pairing.vo.PairingVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PairingService {
    private static final String DISCLAIMER = "本内容仅供日常饮食参考，不构成医疗建议；如有不适请咨询专业人士。";
    private static final Set<String> TEA_CATEGORIES = Set.of("绿茶", "白茶", "黄茶", "青茶", "红茶", "黑茶", "花草");

    private final PairingRuleMapper pairingRuleMapper;
    private final ObjectMapper objectMapper;

    /**
     * 获取某类茶的食物搭配建议。
     * @param teaCategory
     * @return
     */
    public PairingVO getPairing(String teaCategory) {
        String category = normalizeCategory(teaCategory);
        PairingRule localRule = pairingRuleMapper.selectOne(
                new LambdaQueryWrapper<PairingRule>()
                        .eq(PairingRule::getTeaCategory, category)
        );
        if (localRule != null) {
            return new PairingVO(category, parseFoods(localRule.getFoods()),
                    localRule.getAdvice(), "local_rule", DISCLAIMER);
        }

        return new PairingVO(category, List.of(
                new PairingFoodVO("清淡糕点", null),
                new PairingFoodVO("水果", null),
                new PairingFoodVO("坚果", null)
        ),
                "暂未找到更具体的搭配规则，建议选择清淡、适量且不过甜的食物。",
                "generic", DISCLAIMER);
    }

    /**
     * 规范化茶类输入，若为空则默认使用“绿茶”，并检查是否在允许的茶类列表中。
     * @param teaCategory
     * @return
     */
    private String normalizeCategory(String teaCategory) {
        String category = teaCategory == null || teaCategory.isBlank() ? "绿茶" : teaCategory.trim();
        if (!TEA_CATEGORIES.contains(category)) {
            throw new BizException(400, "请选择正确的茶类");
        }
        return category;
    }

    /**
     * 将 JSON 字符串解析为食物列表，若解析失败则返回默认食物列表。
     * @param foodsJson
     * @return
     */
    private List<PairingFoodVO> parseFoods(String foodsJson) {
        if (foodsJson == null || foodsJson.isBlank()) return defaultFoods();
        try {
            JsonNode root = objectMapper.readTree(foodsJson);
            if (!root.isArray()) return defaultFoods();
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
            return foods.isEmpty() ? defaultFoods() : foods;
        } catch (JacksonException exception) {
            return defaultFoods();
        }
    }

    private List<PairingFoodVO> defaultFoods() {
        return List.of(
                new PairingFoodVO("清淡糕点", null),
                new PairingFoodVO("水果", null),
                new PairingFoodVO("坚果", null)
        );
    }

}
