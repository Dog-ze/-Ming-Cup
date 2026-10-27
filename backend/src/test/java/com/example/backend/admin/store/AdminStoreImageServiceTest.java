package com.example.backend.admin.store;

import com.example.backend.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminStoreImageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesCoverUnderADateDirectoryAndReturnsOnlyRelativePath() throws Exception {
        StoreImageProperties properties = new StoreImageProperties();
        properties.setDirectory(temporaryDirectory.resolve("uploads/stores").toString());
        AdminStoreImageService service = new AdminStoreImageService(properties);

        String relativePath = service.store(new MockMultipartFile(
                "file", "cover.png", "image/png", new byte[]{1, 2, 3}));

        assertThat(relativePath).matches("\\d{8}/[a-f0-9-]+\\.png");
        assertThat(relativePath).doesNotContain(temporaryDirectory.toString());
        assertThat(Files.exists(temporaryDirectory.resolve("uploads/stores").resolve(relativePath))).isTrue();
    }

    @Test
    void rejectsAFileWhoseExtensionDoesNotMatchItsMimeType() {
        StoreImageProperties properties = new StoreImageProperties();
        properties.setDirectory(temporaryDirectory.resolve("uploads/stores").toString());
        AdminStoreImageService service = new AdminStoreImageService(properties);

        assertThatThrownBy(() -> service.store(new MockMultipartFile(
                "file", "cover.png", "image/jpeg", new byte[]{1})))
                .isInstanceOf(BizException.class)
                .hasMessage("只能上传JPG、PNG或WebP格式的封面图片");
    }
}
