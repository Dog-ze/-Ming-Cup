package com.example.backend.user;

import com.example.backend.common.Result;
import com.example.backend.user.dto.ProfileUpdateRequest;
import com.example.backend.user.vo.UserProfileResponse;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户的个人信息。
     * @return
     */
    @GetMapping("/profile")
    public Result<UserProfileResponse> getProfile() {
        return Result.ok(userService.getProfile());
    }

    /**
     * 更新当前登录用户的个人信息。
     * @param request
     * @return
     */
    @PutMapping("/profile")
    public Result<UserProfileResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.ok(userService.updateProfile(request));
    }
}