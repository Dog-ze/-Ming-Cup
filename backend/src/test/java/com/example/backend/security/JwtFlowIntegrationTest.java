package com.example.backend.security;

import com.example.backend.common.Result;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-must-contain-at-least-thirty-two-characters",
        "app.jwt.expire-hours=1",
        "ADMIN_JWT_SECRET=test-admin-secret-must-contain-at-least-thirty-two-characters"
})
@Import(JwtFlowIntegrationTest.SecureTestController.class)
class JwtFlowIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwtService jwtService;

    /**
     * JWT 流程测试不依赖真实 MySQL。
     * 这里模拟拦截器需要的“用户存在且状态启用”结果。
     */
    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        User enabledUser = new User();
        enabledUser.setId(42L);
        enabledUser.setStatus(1);
        given(userMapper.selectById(42L)).willReturn(enabledUser);
    }

    @Test
    void protectedApiRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/secure-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.msg").value("请先登录"));
    }

    @Test
    void pairingApiRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/pairing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.msg").value("请先登录"));
    }

    @Test
    void protectedApiReadsUserIdFromValidToken() throws Exception {
        String token = jwtService.createToken(42L, "tea_user");

        mockMvc.perform(get("/api/secure-test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(42));
    }

    @Test
    void createTokenResultReturnsTokenAndFutureExpiration() {
        JwtService.TokenResult result = jwtService.createTokenResult(42L, "tea_user");

        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(jwtService.parseUserId(result.token())).isEqualTo(42L);
    }

    @Test
    void interceptorClearsCurrentUserAfterRequestCompletes() throws Exception {
        String token = jwtService.createToken(42L, "tea_user");

        mockMvc.perform(get("/api/secure-test")
                .header("Authorization", "Bearer " + token));

        assertThatThrownBy(CurrentUser::id)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("当前请求未登录");
    }

    @RestController
    static class SecureTestController {
        @GetMapping("/api/secure-test")
        Result<Long> secure() {
            return Result.ok(CurrentUser.id());
        }
    }
}