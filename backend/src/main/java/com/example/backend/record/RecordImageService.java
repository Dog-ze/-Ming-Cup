package com.example.backend.record;

import com.example.backend.common.BizException;
import com.example.backend.common.ImageContentValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 将品茶记录照片保存到后端本机 uploads/records 文件夹。
 */
@Service
@AllArgsConstructor
public class RecordImageService {
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final RecordImageProperties properties;

    /**
     * 校验并保存一张照片，返回随机生成的安全文件名。
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择一张图片");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BizException(400, "单张图片不能超过 5MB");
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null
                : IMAGE_EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null) {
            throw new BizException(400, "只能上传 JPG、PNG 或 WebP 图片");
        }
        ImageContentValidator.validate(file, extension);

        try {
            Path directory = Path.of(properties.getRecordDirectory()).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(fileName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return fileName;
        } catch (IOException exception) {
            throw new BizException(500, "图片保存失败，请稍后重试");
        }
    }
}
