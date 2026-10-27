package com.example.backend.config;

import com.example.backend.admin.image.AdminImageProperties;
import com.example.backend.admin.store.StoreImageProperties;
import com.example.backend.record.RecordImageProperties;
import com.example.backend.security.AdminJwtInterceptor;
import com.example.backend.security.JwtInterceptor;
import com.example.backend.user.AvatarProperties;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;

@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;
    private final AdminJwtInterceptor adminJwtInterceptor;
    private final RecordImageProperties recordImageProperties;
    private final AvatarProperties avatarProperties;
    private final AdminImageProperties adminImageProperties;
    private final StoreImageProperties storeImageProperties;
    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*").maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminJwtInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/auth/login");
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/admin/**", "/api/auth/register", "/api/auth/login",
                        "/api/health", "/api/weather", "/api/location/reverse");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        addUploadHandler(registry, "/uploads/records/**", recordImageProperties.getRecordDirectory());
        addUploadHandler(registry, "/uploads/avatars/**", avatarProperties.getDirectory());
        addUploadHandler(registry, "/uploads/admin/**", adminImageProperties.getDirectory());
        addUploadHandler(registry, "/uploads/stores/**", storeImageProperties.getDirectory());
    }

    private void addUploadHandler(ResourceHandlerRegistry registry, String pattern, String directory) {
        String uri = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(pattern).addResourceLocations(uri.endsWith("/") ? uri : uri + "/");
    }
}
