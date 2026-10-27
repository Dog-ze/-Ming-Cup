package com.example.backend.questionnaire;

import com.example.backend.common.Result;
import com.example.backend.questionnaire.dto.QuestionnaireSubmitRequest;
import com.example.backend.questionnaire.vo.QuestionnaireVO;
import com.example.backend.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questionnaire")
@AllArgsConstructor
public class QuestionnaireController {
    private final QuestionnaireService questionnaireService;

    /**
     * 获取已填写的问卷
     * @return
     */
    @GetMapping
    public Result<QuestionnaireVO> get() {
        return Result.ok(questionnaireService.getByUserId(CurrentUser.id()));
    }

    /**
     * 提交或修改问卷
     * @param request
     * @return
     */
    @PostMapping
    public Result<QuestionnaireVO> submit(@Valid @RequestBody QuestionnaireSubmitRequest request) {
        return Result.ok(questionnaireService.submit(CurrentUser.id(), request));
    }
}