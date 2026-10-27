package com.example.backend.user;

import com.example.backend.entity.User;
import com.example.backend.user.vo.UserProfileResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileResponseTest {

    @Test
    void includesUploadedAvatarUrlInProfileResponse() {
        User user = new User();
        user.setId(1L);
        user.setUsername("一盏茗");
        user.setAvatar("/uploads/avatars/avatar.png");
        user.setCity("苏州市");

        UserProfileResponse response = UserProfileResponse.from(user, false);

        assertThat(response.avatar()).isEqualTo("/uploads/avatars/avatar.png");
    }
}
