package com.example.backend.admin.store;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.store-image")
public class StoreImageProperties {
    private String directory = "uploads/stores";
    private long maxFileSizeBytes = 5L * 1024 * 1024;
}
