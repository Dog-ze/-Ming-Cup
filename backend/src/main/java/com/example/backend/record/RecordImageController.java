package com.example.backend.record;

import com.example.backend.common.Result;
import com.example.backend.common.UploadUrl;
import com.example.backend.record.vo.RecordImageUploadVO;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 品茶记录照片上传接口。该接口在 /api 下，因此仍受 JWT 登录保护。
 */
@RestController
@RequestMapping("/api/records/images")
@AllArgsConstructor
public class RecordImageController {
    private final RecordImageService recordImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<RecordImageUploadVO> upload(@RequestPart("file") MultipartFile file) {
        String fileName = recordImageService.store(file);
        String url = UploadUrl.relative("/uploads/records/", fileName);
        return Result.ok(new RecordImageUploadVO(url));
    }
}
