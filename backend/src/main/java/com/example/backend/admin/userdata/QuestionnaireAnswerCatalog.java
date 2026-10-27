package com.example.backend.admin.userdata;

import com.example.backend.enums.ConstitutionType;
import com.example.backend.enums.FlavorPreference;
import com.example.backend.enums.InsomniaStatus;
import com.example.backend.enums.StrengthPreference;
import com.example.backend.enums.TeaPurpose;

import java.util.Arrays;
import java.util.List;

/** 问卷答案编码与中文名称之间的对应表。 */
public final class QuestionnaireAnswerCatalog {
    public record Option(Integer code, String label) {
    }

    private QuestionnaireAnswerCatalog() {
    }

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
