package com.example.backend.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.common.BizException;
import com.example.backend.entity.Questionnaire;
import com.example.backend.entity.User;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.CurrentUser;
import com.example.backend.user.dto.ProfileUpdateRequest;
import com.example.backend.user.vo.UserProfileResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final QuestionnaireMapper questionnaireMapper;

    /**
     * 获取当前登录用户的个人信息。
     *
     * @return 当前登录用户的个人信息
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        User user = findCurrentUser();
        return UserProfileResponse.from(user, hasCompletedQuestionnaire(user.getId()));
    }

    /**
     * 更新当前登录用户的个人信息。
     *
     * @param request 包含更新信息的请求对象
     * @return 更新后的用户个人信息
     */
    @Transactional
    public UserProfileResponse updateProfile(ProfileUpdateRequest request) {
        User user = findCurrentUser();
        // 城市定位等场景不会传头像；此时保留用户原来的头像，避免被意外清空。
        if (request.avatar() != null) {
            user.setAvatar(request.avatar());
        }
        user.setCity(request.city().trim());
        user.setLongitude(request.longitude());
        user.setLatitude(request.latitude());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(request.avatar() != null, User::getAvatar, user.getAvatar())
                .set(User::getCity, user.getCity())
                .set(User::getLongitude, user.getLongitude())
                .set(User::getLatitude, user.getLatitude()));

        return UserProfileResponse.from(user, hasCompletedQuestionnaire(user.getId()));
    }

    /**
     * 查找当前登录用户。
     *
     * @return 当前登录用户
     * @throws BizException 如果用户不存在或登录已失效
     */
    private User findCurrentUser() {
        User user = userMapper.selectById(CurrentUser.id());
        if (user == null) {
            throw new BizException(40101, "登录已失效，请重新登录");
        }
        return user;
    }

    /**
     * 判断是否已完成问卷
     * @param userId
     * @return
     */
    private boolean hasCompletedQuestionnaire(Long userId) {
        Long count = questionnaireMapper.selectCount(
                new LambdaQueryWrapper<Questionnaire>().eq(Questionnaire::getUserId, userId)
        );
        return count != null && count > 0;
    }
}
