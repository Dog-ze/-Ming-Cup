package com.example.backend.record;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 品茶记录照片在本机的保存位置和单张图片大小限制。
 */
@Data
@ConfigurationProperties(prefix = "app.record-image")
public class RecordImageProperties {
    private String recordDirectory = "uploads/records";
    private long maxFileSizeBytes = 5L * 1024 * 1024;
}
