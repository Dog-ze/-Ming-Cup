package com.example.backend.admin.store;

import com.example.backend.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminStoreImageService {
    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final StoreImageProperties properties;

    /**
     * 保存封面图片
     * @param file 图片文件
     * @return 图片相对路径
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择一张封面图片");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BizException(400, "封面图片不能超过5MB");
        }

        String contentType = file.getContentType();
        String extension = contentType == null ? null : IMAGE_EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
        if (extension == null || !extension.equals(extensionFromFileName(file.getOriginalFilename()))) {
            throw new BizException(400, "只能上传JPG、PNG或WebP格式的封面图片");
        }

        try {
            Path baseDirectory = Path.of(properties.getDirectory()).toAbsolutePath().normalize();
            String dateDirectory = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Path directory = baseDirectory.resolve(dateDirectory).normalize();
            if (!directory.startsWith(baseDirectory)) {
                throw new BizException(400, "封面图片保存目录不正确");
            }
            Files.createDirectories(directory);

            String fileName = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(fileName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return dateDirectory + "/" + fileName;
        } catch (IOException exception) {
            throw new BizException(500, "封面图片保存失败，请稍后重试");
        }
    }

    /**
     * 从文件名中提取扩展名
     * @param originalFilename 原始文件名
     * @return 扩展名（小写），如果没有扩展名则返回空字符串
     */
    private String extensionFromFileName(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int separator = originalFilename.lastIndexOf('.');
        if (separator < 0 || separator == originalFilename.length() - 1) {
            return "";
        }
        String extension = originalFilename.substring(separator + 1).toLowerCase(Locale.ROOT);
        return "jpeg".equals(extension) ? "jpg" : extension;
    }
}
