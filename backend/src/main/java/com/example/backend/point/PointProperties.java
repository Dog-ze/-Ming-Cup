package com.example.backend.point;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.points")
public class PointProperties {
    private String timezone = "Asia/Shanghai";
    private int dailyClaim = 20;
    private int questionnaireFirst = 20;
    private int teaRecordDaily = 10;
    private int brewGuideCost = 5;
    private int chatCost = 10;
}
