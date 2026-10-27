package com.example.backend.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ExceptionDemoController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void okCreatesStandardSuccessResponse() {
        Result<String> result = Result.ok("up");

        assertThat(result.getCode()).isZero();
        assertThat(result.getMsg()).isEqualTo("ok");
        assertThat(result.getData()).isEqualTo("up");
    }

    @Test
    void businessExceptionReturnsItsCodeAndMessage() throws Exception {
        mockMvc.perform(get("/business-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.msg").value("参数错误"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void validationExceptionReturnsFirstFieldErrorMessage() throws Exception {
        mockMvc.perform(post("/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("名称不能为空"));
    }

    @Test
    void unknownExceptionDoesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/unknown-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("服务暂时不可用，请稍后再试"))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("内部异常"))));
    }

    @RestController
    static class ExceptionDemoController {
        @GetMapping("/business-error")
        String businessError() {
            throw new BizException(40001, "参数错误");
        }

        @PostMapping("/validation-error")
        String validationError(@Valid @RequestBody NameRequest request) {
            return request.name();
        }

        @GetMapping("/unknown-error")
        String unknownError() {
            throw new IllegalStateException("内部异常");
        }
    }

    record NameRequest(@NotBlank(message = "名称不能为空") String name) {
    }
}
