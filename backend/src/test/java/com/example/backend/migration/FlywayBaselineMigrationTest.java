package com.example.backend.migration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayBaselineMigrationTest {

    @Test
    void baselineMigrationContainsOnlyTheCurrentApplicationSchema() throws IOException {
        String migration = readResource("db/migration/V1__baseline_current_schema.sql");

        assertThat(migration)
                .contains("create table action_log", "create table admin_user", "create table tea",
                        "create table user", "create table questionnaire", "create table tea_record")
                .doesNotContainIgnoringCase(
                        "insert into", "drop table", "lock table", "unlock table",
                        "create database", "\nuse ", "show tables", "mysql."
                );
    }

    @Test
    void flywayUsesAnEnvironmentControlledBaselineConfiguration() throws IOException {
        String configuration = readResource("application.yml");

        assertThat(configuration).contains(
                "flyway:",
                "enabled: ${FLYWAY_ENABLED:false}",
                "locations: classpath:db/migration",
                "baseline-on-migrate: true",
                "baseline-version: 1",
                "validate-on-migrate: true",
                "clean-disabled: true"
        );
    }

    @Test
    void pairingRuleKeepsOnlyOneTeaCategoryUniqueConstraint() throws IOException {
        String migration = readResource("db/migration/V1__baseline_current_schema.sql");
        int pairingRuleStart = migration.indexOf("create table pairing_rule");
        int pairingStoreStart = migration.indexOf("create table pairing_store");
        String pairingRuleDefinition = migration.substring(pairingRuleStart, pairingStoreStart);

        long uniqueConstraintCount = pairingRuleDefinition.lines()
                .map(String::trim)
                .filter(line -> line.equals("unique (tea_category),") || line.equals("unique (tea_category)"))
                .count();

        assertThat(uniqueConstraintCount).isEqualTo(1);
        assertThat(pairingRuleDefinition).contains("constraint uk_pairing_rule_tea_category");
    }

    @Test
    void springBootFlywayAutoConfigurationIsAvailable() throws ClassNotFoundException {
        assertThat(Class.forName("org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"))
                .isNotNull();
    }

    private String readResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertThat(resource.exists()).as("资源文件 %s 必须存在", path).isTrue();
        try (var inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
