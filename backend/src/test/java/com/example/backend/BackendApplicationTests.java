package com.example.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-must-contain-at-least-thirty-two-characters",
        "app.jwt.expire-hours=1",
        "ADMIN_JWT_SECRET=test-admin-secret-must-contain-at-least-thirty-two-characters"
})
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
