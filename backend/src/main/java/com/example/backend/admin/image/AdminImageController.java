package com.example.backend.admin.image;

import com.example.backend.admin.image.vo.AdminImageUploadVO;
import com.example.backend.common.Result;
import com.example.backend.common.UploadUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
public class AdminImageController {
    private final AdminImageService adminImageService;

    /**
     * 上传图片
     * @param scene 上传场景
     * @param file 图片文件
     * @return 图片访问URL
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminImageUploadVO> upload(@RequestParam String scene,
                                               @RequestPart("file") MultipartFile file) {
        String relativePath = adminImageService.store(scene, file);
        String imageUrl = UploadUrl.relative("/uploads/admin/", relativePath);
        return Result.ok(new AdminImageUploadVO(imageUrl));
    }
}