package com.example.backend.questionnaire.vo;

import com.example.backend.entity.Questionnaire;

import java.time.LocalDateTime;

/**
 * 返回给前端的问卷答案。
 * 不返回 userId，防止前端看到或修改其他用户的信息。
 */
public record QuestionnaireVO(
        Integer purpose,
        Integer constitutionType,
        Integer insomnia,
        Integer flavorPreference,
        Integer strengthPreference,
        LocalDateTime updatedAt
) {
    /**
     * 将数据库实体类 Questionnaire 转换为接口返回对象 QuestionnaireVO。
     */
    public static QuestionnaireVO from(Questionnaire entity) {
        if (entity == null) {
            return null;
        }

        return new QuestionnaireVO(
                entity.getPurpose(),
                entity.getConstitutionType(),
                entity.getInsomnia(),
                entity.getFlavorPreference(),
                entity.getStrengthPreference(),
                entity.getUpdatedAt()
        );
    }
}