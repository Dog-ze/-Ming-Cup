package com.example.backend.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UploadUrlTest {

    @Test
    void joinsPrefixAndFileName() {
        assertThat(UploadUrl.relative("/uploads/admin/", "tea/x.png"))
                .isEqualTo("/uploads/admin/tea/x.png");
    }

    @Test
    void normalizesMissingLeadingAndTrailingSlashesInPrefix() {
        assertThat(UploadUrl.relative("uploads/admin", "tea/x.png"))
                .isEqualTo("/uploads/admin/tea/x.png");
        assertThat(UploadUrl.relative("/uploads/admin", "tea/x.png"))
                .isEqualTo("/uploads/admin/tea/x.png");
        assertThat(UploadUrl.relative("uploads/admin/", "tea/x.png"))
                .isEqualTo("/uploads/admin/tea/x.png");
    }

    @Test
    void stripsLeadingSlashesFromFileNameToAvoidDoubleSlash() {
        assertThat(UploadUrl.relative("/uploads/admin/", "/tea/x.png"))
                .isEqualTo("/uploads/admin/tea/x.png");
        assertThat(UploadUrl.relative("/uploads/avatars/", "\\avatar.png"))
                .isEqualTo("/uploads/avatars/avatar.png");
    }

    @Test
    void rejectsBlankFileName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", ""));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", "   "));
    }

    @Test
    void rejectsBlankPrefix() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative(null, "tea/x.png"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("  ", "tea/x.png"));
    }

    @Test
    void rejectsParentDirectoryTraversal() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", "../secret.png"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", "tea/../../secret.png"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", "..\\..\\secret.png"));
    }

    @Test
    void rejectsAbsoluteUrlsPassedAsFileName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("/uploads/admin/", "http://localhost:8080/x.png"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UploadUrl.relative("http://localhost:8080/uploads/admin/", "x.png"));
    }

    @Test
    void neverReturnsHostSchemeOrContextPath() {
        String url = UploadUrl.relative("/uploads/records/", "2026/10/photo.jpg");

        assertThat(url).isEqualTo("/uploads/records/2026/10/photo.jpg");
        assertThat(url).doesNotContain("http://");
        assertThat(url).doesNotContain("https://");
        assertThat(url).doesNotContain("localhost");
        assertThat(url).doesNotContain("8080");
        assertThat(url).startsWith("/");
    }
}
