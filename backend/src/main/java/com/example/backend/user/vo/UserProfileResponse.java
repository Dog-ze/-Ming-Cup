package com.example.backend.user.vo;

import com.example.backend.entity.User;
import java.math.BigDecimal;

public record UserProfileResponse(
        /**
         * 用户 ID
         */
        Long id,
        /**
         * 用户名
         */
        String username,
        /**
         * 用户头像访问地址；未上传时为 null
         */
        String avatar,
        /**
         * 城市
         */
        String city,
        /**
         * 经度
         */
        BigDecimal longitude,
        /**
         * 纬度
         */
        BigDecimal latitude,
        /**
         * 问卷是否完成
         */
        boolean questionnaireCompleted
) {
    /**
     * 将 User 实体对象转换为 UserProfileResponse VO 对象。
     *
     * @param user                  用户实体对象
     * @param questionnaireCompleted 问卷是否完成
     * @return UserProfileResponse VO 对象
     */
    public static UserProfileResponse from(User user, boolean questionnaireCompleted) {
        return new UserProfileResponse(
                user.getId(), user.getUsername(), user.getAvatar(), user.getCity(),
                user.getLongitude(), user.getLatitude(), questionnaireCompleted
        );
    }
}
