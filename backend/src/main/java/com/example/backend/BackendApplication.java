package com.example.backend;

import com.example.backend.admin.image.AdminImageProperties;
import com.example.backend.admin.store.StoreImageProperties;
import com.example.backend.config.CorsProperties;
import com.example.backend.ai.AiProperties;
import com.example.backend.point.PointProperties;
import com.example.backend.pairing.osm.OpenStreetMapProperties;
import com.example.backend.location.AmapProperties;
import com.example.backend.record.RecordImageProperties;
import com.example.backend.user.AvatarProperties;
import com.example.backend.weather.WeatherProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan({
        "com.example.backend.mapper",
        "com.example.backend.admin.dashboard.mapper",
        "com.example.backend.admin.tea.mapper",
        "com.example.backend.admin.data.mapper",
        "com.example.backend.admin.userdata.mapper",
        "com.example.backend.admin.operationlog.mapper"
})
@EnableConfigurationProperties({WeatherProperties.class, RecordImageProperties.class, AvatarProperties.class,
        OpenStreetMapProperties.class, AmapProperties.class, AdminImageProperties.class, StoreImageProperties.class,
        AiProperties.class, PointProperties.class, CorsProperties.class})
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
