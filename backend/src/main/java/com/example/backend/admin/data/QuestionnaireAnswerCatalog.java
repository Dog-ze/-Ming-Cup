package com.example.backend.admin.data;

import com.example.backend.enums.ConstitutionType;
import com.example.backend.enums.FlavorPreference;
import com.example.backend.enums.InsomniaStatus;
import com.example.backend.enums.StrengthPreference;
import com.example.backend.enums.TeaPurpose;

import java.util.Arrays;
import java.util.List;
/**
 * 问卷答案的选项目录。
 */
public final class QuestionnaireAnswerCatalog {
    public record Option(Integer code, String label) { }

    private QuestionnaireAnswerCatalog() { }

    /**
     * 根据问卷题目和选项 code 获取选项 label。
     *
     * @param questionKey 问卷题目 key
     * @param code        选项 code
     * @return 选项 label
     */
    public static String labelOf(String questionKey, Integer code) {
        if (code == null) {
            return "未填写";
        }
        return optionsOf(questionKey).stream()
                .filter(option -> option.code().equals(code))
                .map(Option::label)
                .findFirst()
                .orElse("未知（" + code + "）");
    }

    /**
     * 根据问卷题目获取所有选项。
     *
     * @param questionKey 问卷题目 key
     * @return 选项列表
     */
    public static List<Option> optionsOf(String questionKey) {
        return switch (questionKey) {
            case "purpose" -> Arrays.stream(TeaPurpose.values())
                    .map(item -> new Option(item.getCode(), item.getLabel())).toList();
            case "constitutionType" -> Arrays.stream(ConstitutionType.values())
                    .map(item -> new Option(item.getCode(), item.getLabel())).toList();
            case "insomnia" -> Arrays.stream(InsomniaStatus.values())
                    .map(item -> new Option(item.getCode(), item.getLabel())).toList();
            case "flavorPreference" -> Arrays.stream(FlavorPreference.values())
                    .map(item -> new Option(item.getCode(), item.getLabel())).toList();
            case "strengthPreference" -> Arrays.stream(StrengthPreference.values())
                    .map(item -> new Option(item.getCode(), item.getLabel())).toList();
            default -> throw new IllegalArgumentException("不支持的问卷题目：" + questionKey);
        };
    }
}