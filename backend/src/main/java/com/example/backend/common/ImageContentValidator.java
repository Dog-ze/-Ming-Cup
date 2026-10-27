package com.example.backend.common;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/** 根据文件头校验图片格式，不能只信任客户端传来的 Content-Type。 */
public final class ImageContentValidator {
    private ImageContentValidator() {
    }

    public static void validate(MultipartFile file, String extension) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (extension) {
                case "jpg" -> header.length >= 3 && unsigned(header[0]) == 0xFF
                        && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF;
                case "png" -> header.length >= 8 && unsigned(header[0]) == 0x89 && header[1] == 0x50
                        && header[2] == 0x4E && header[3] == 0x47 && header[4] == 0x0D
                        && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
                case "webp" -> header.length >= 12 && header[0] == 'R' && header[1] == 'I'
                        && header[2] == 'F' && header[3] == 'F' && header[8] == 'W'
                        && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
                default -> false;
            };
            if (!valid) throw new BizException(400, "图片内容与文件类型不匹配");
        } catch (IOException exception) {
            throw new BizException(400, "图片读取失败");
        }
    }

    private static int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }
}
