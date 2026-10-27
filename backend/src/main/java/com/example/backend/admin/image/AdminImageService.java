package com.example.backend.admin.image;

import com.example.backend.common.BizException;
import com.example.backend.common.ImageContentValidator;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class AdminImageService {
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final AdminImageProperties properties;

    /**
     * 保存图片
     * @param scene 图片上传场景
     * @param file 图片文件
     * @return 图片相对路径
     */
    public String store(String scene, MultipartFile file) {
        if (!"tea".equals(scene) && !"pairing-food".equals(scene)) {
            throw new BizException(400, "图片上传场景不正确");
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择一张图片");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BizException(400, "单张图片不能超过5MB");
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null
                : IMAGE_EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null) {
            throw new BizException(400, "只能上传JPG、PNG或WebP图片");
        }
        ImageContentValidator.validate(file, extension);

        try {
            Path baseDirectory = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
            Path directory = baseDirectory.resolve(scene).normalize();
            if (!directory.startsWith(baseDirectory)) {
                throw new BizException(400, "图片保存目录不正确");
            }
            Files.createDirectories(directory);
            String fileName = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(fileName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return scene + "/" + fileName;
        } catch (IOException exception) {
            throw new BizException(500, "图片保存失败，请稍后重试");
        }
    }
}
