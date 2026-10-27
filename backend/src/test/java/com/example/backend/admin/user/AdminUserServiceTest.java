package com.example.backend.admin.user;

import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.entity.User;
import com.example.backend.mapper.QuestionnaireMapper;
import com.example.backend.mapper.TeaRecordMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.UserPointAccountMapper;
import com.example.backend.point.PointService;
import com.example.backend.point.vo.PointTransactionPageVO;
import com.example.backend.point.vo.PointTransactionVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    @Test
    void pointConsumptionsReturnsOnlyTheSpecifiedUsersDebitRecords() {
        UserMapper userMapper = mock(UserMapper.class);
        PointService pointService = mock(PointService.class);
        User user = new User();
        user.setId(8L);
        when(userMapper.selectById(8L)).thenReturn(user);
        PointTransactionPageVO page = new PointTransactionPageVO(List.of(new PointTransactionVO(
                12L, "AI_CHAT_DEBIT", -10, 30, "SUCCESS", "AI 问茶扣除积分",
                LocalDateTime.of(2026, 10, 27, 10, 0)
        )), 1, 10, 1);
        when(pointService.consumptions(8L, 1, 10)).thenReturn(page);
        AdminUserService service = new AdminUserService(userMapper, mock(QuestionnaireMapper.class),
                mock(TeaRecordMapper.class), mock(AdminOperationLogService.class),
                mock(UserPointAccountMapper.class), pointService);

        PointTransactionPageVO result = service.pointConsumptions(8L, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).singleElement().satisfies(item -> {
            assertThat(item.changeAmount()).isEqualTo(-10);
            assertThat(item.description()).isEqualTo("AI 问茶扣除积分");
        });
        verify(pointService).consumptions(8L, 1, 10);
    }
}
