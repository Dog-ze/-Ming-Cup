package com.example.backend.admin.image;

import com.example.backend.common.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminImageServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesTeaImageUnderTheTeaDirectory() throws Exception {
        AdminImageProperties properties = new AdminImageProperties();
        properties.setDirectory(temporaryDirectory.resolve("uploads/admin").toString());
        AdminImageService service = new AdminImageService(properties);

        String relativePath = service.store("tea", new MockMultipartFile(
                "file", "longjing.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}));

        assertThat(relativePath).startsWith("tea/").endsWith(".jpg");
        assertThat(Files.exists(temporaryDirectory.resolve("uploads/admin").resolve(relativePath))).isTrue();
    }

    @Test
    void rejectsAnUnsupportedImageScene() {
        AdminImageProperties properties = new AdminImageProperties();
        properties.setDirectory(temporaryDirectory.resolve("uploads/admin").toString());
        AdminImageService service = new AdminImageService(properties);

        assertThatThrownBy(() -> service.store("avatar", new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", new byte[]{1})))
                .isInstanceOf(BizException.class)
                .hasMessage("图片上传场景不正确");
    }
}
