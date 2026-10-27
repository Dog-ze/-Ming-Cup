package com.example.backend.record;

import com.example.backend.common.BizException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordImageServiceTest {

    @Test
    void storesPngWithGeneratedSafeFileName() throws Exception {
        Path temporaryDirectory = testDirectory();
        RecordImageProperties properties = new RecordImageProperties();
        properties.setRecordDirectory(temporaryDirectory.toString());
        RecordImageService service = new RecordImageService(properties);
        MockMultipartFile image = new MockMultipartFile(
                "file", "my tea photo.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        String fileName = service.store(image);

        assertThat(fileName).matches("[0-9a-f-]+\\.png");
        assertThat(Files.exists(temporaryDirectory.resolve(fileName))).isTrue();
    }

    @Test
    void rejectsNonImageFile() throws Exception {
        Path temporaryDirectory = testDirectory();
        RecordImageProperties properties = new RecordImageProperties();
        properties.setRecordDirectory(temporaryDirectory.toString());
        RecordImageService service = new RecordImageService(properties);
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "not an image".getBytes()
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(BizException.class)
                .hasMessage("只能上传 JPG、PNG 或 WebP 图片");
    }

    @Test
    void rejectsFakePngThatOnlySpoofsTheContentType() throws Exception {
        Path temporaryDirectory = testDirectory();
        RecordImageProperties properties = new RecordImageProperties();
        properties.setRecordDirectory(temporaryDirectory.toString());
        RecordImageService service = new RecordImageService(properties);
        MockMultipartFile fakeImage = new MockMultipartFile(
                "file", "fake.png", "image/png", "<script>alert(1)</script>".getBytes()
        );

        assertThatThrownBy(() -> service.store(fakeImage))
                .isInstanceOf(BizException.class)
                .hasMessage("图片内容与文件类型不匹配");
    }

    /**
     * Windows 下 JUnit 的系统临时目录偶尔无法自动删除，
     * 因此测试文件统一放在 Maven 的 target 目录中。
     */
    private Path testDirectory() throws Exception {
        return Files.createDirectories(
                Path.of("target", "record-image-test-" + UUID.randomUUID())
        );
    }
}
