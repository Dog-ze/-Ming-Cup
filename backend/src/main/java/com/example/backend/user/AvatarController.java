package com.example.backend.user;

import com.example.backend.common.Result;
import com.example.backend.common.UploadUrl;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前登录用户的头像上传接口。
 */
@RestController
@RequestMapping("/api/user/avatar")
@AllArgsConstructor
public class AvatarController {
    private final AvatarImageService avatarImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AvatarUploadVO> upload(@RequestPart("file") MultipartFile file) {
        String fileName = avatarImageService.store(file);
        String url = UploadUrl.relative("/uploads/avatars/", fileName);
        return Result.ok(new AvatarUploadVO(url));
    }
}
