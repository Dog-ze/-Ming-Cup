package com.example.backend.user;

import com.example.backend.entity.User;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.security.CurrentUser;
import com.example.backend.user.dto.ProfileUpdateRequest;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @BeforeAll
    static void initializeUserMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
    }

    @AfterAll
    static void clearUserMetadata() {
        TableInfoHelper.remove(User.class);
    }

    @AfterEach
    void clearCurrentUser() {
        CurrentUser.clear();
    }

    @Test
    void updateProfileWritesNullCoordinatesSoExistingLocationIsRemoved() {
        UserMapper userMapper = mock(UserMapper.class);
        QuestionnaireMapper questionnaireMapper = mock(QuestionnaireMapper.class);
        User user = new User();
        user.setId(1L);
        user.setUsername("一盏茗");
        user.setCity("苏州市");
        user.setLongitude(new BigDecimal("120.0000"));
        user.setLatitude(new BigDecimal("31.0000"));
        when(userMapper.selectById(1L)).thenReturn(user);
        when(questionnaireMapper.selectCount(any())).thenReturn(0L);
        CurrentUser.set(1L);
        UserService service = new UserService(userMapper, questionnaireMapper);

        service.updateProfile(new ProfileUpdateRequest(null, "上海市", null, null));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(userMapper).update(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("city", "longitude", "latitude");
        assertThat(captor.getValue().getParamNameValuePairs().values()).containsNull();
    }
}
