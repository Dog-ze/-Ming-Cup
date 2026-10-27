package com.example.backend.questionnaire;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.entity.Questionnaire;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.questionnaire.dto.QuestionnaireSubmitRequest;
import com.example.backend.questionnaire.vo.QuestionnaireVO;
import com.example.backend.point.PointService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class QuestionnaireService {
    private final QuestionnaireMapper questionnaireMapper;
    private final PointService pointService;

    /**
     * 提交、保存问卷
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public QuestionnaireVO submit(Long userId, QuestionnaireSubmitRequest request) {
        Questionnaire questionnaire = questionnaireMapper.selectOne(
                new LambdaQueryWrapper<Questionnaire>()
                        .eq(Questionnaire::getUserId, userId)
        );

        boolean creating = questionnaire == null;
        if (creating) {
            questionnaire = new Questionnaire();
            questionnaire.setUserId(userId);
        }

        questionnaire.setPurpose(request.purpose());
        questionnaire.setConstitutionType(request.constitutionType());
        questionnaire.setInsomnia(request.insomnia());
        questionnaire.setFlavorPreference(request.flavorPreference());
        questionnaire.setStrengthPreference(request.strengthPreference());
        questionnaire.setUpdatedAt(LocalDateTime.now());

        if (creating) {
            questionnaireMapper.insert(questionnaire);
            pointService.rewardFirstQuestionnaire(userId);
        } else {
            questionnaireMapper.updateById(questionnaire);
        }

        return QuestionnaireVO.from(questionnaire);
    }

    /**
     * 获取已填写的问卷
     * @param userId
     * @return
     */
    public QuestionnaireVO getByUserId(Long userId) {
        Questionnaire questionnaire = questionnaireMapper.selectOne(
                new LambdaQueryWrapper<Questionnaire>()
                        .eq(Questionnaire::getUserId, userId)
        );
        return QuestionnaireVO.from(questionnaire);
    }
}
