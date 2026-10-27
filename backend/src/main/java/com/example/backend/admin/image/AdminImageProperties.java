package com.example.backend.admin.image;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.admin-image")
public class AdminImageProperties {
    private String directory = "uploads/admin";
    private long maxFileSizeBytes = 5L * 1024 * 1024;
}