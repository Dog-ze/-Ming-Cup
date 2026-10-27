package com.example.backend.admin.data;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 固定旧用户数据模块的冻结状态：兼容期内仍可保留路由，
 * 但 Controller 与 Service 必须明确标记为准备移除，避免新代码继续依赖它们。
 */
@SuppressWarnings("removal")
class LegacyAdminDataModuleTest {

    @Test
    void legacyControllerIsMarkedForRemoval() {
        Deprecated marker = AdminDataQueryController.class.getAnnotation(Deprecated.class);

        assertThat(marker).isNotNull();
        assertThat(marker.since()).isEqualTo("2026-10");
        assertThat(marker.forRemoval()).isTrue();
    }

    @Test
    void legacyServiceIsMarkedForRemoval() {
        Deprecated marker = AdminDataQueryService.class.getAnnotation(Deprecated.class);

        assertThat(marker).isNotNull();
        assertThat(marker.since()).isEqualTo("2026-10");
        assertThat(marker.forRemoval()).isTrue();
    }
}
