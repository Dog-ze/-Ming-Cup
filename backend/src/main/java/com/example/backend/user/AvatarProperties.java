package com.example.backend.user;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户头像在本机的保存位置和单张图片大小限制。
 */
@Data
@ConfigurationProperties(prefix = "app.avatar")
public class AvatarProperties {
    private String directory = "uploads/avatars";
    private long maxFileSizeBytes = 5L * 1024 * 1024;
}
