package com.example.backend.admin.userdata;

import com.example.backend.admin.userdata.mapper.AdminUserDataMapper;
import com.example.backend.admin.userdata.mapper.AdminUserDataRow;
import com.example.backend.admin.userdata.mapper.AdminUserQuestionnaireRow;
import com.example.backend.admin.userdata.vo.AdminUserQuestionnaireVO;
import com.example.backend.common.BizException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminUserDataServiceTest {

    @Test
    void userPageReturnsOneSummaryRowForEachUser() {
        AdminUserDataMapper mapper = mock(AdminUserDataMapper.class);
        when(mapper.countUsers("小茗", 1)).thenReturn(1L);
        when(mapper.selectUserPage("小茗", 1, 0L, 10L))
                .thenReturn(List.of(new AdminUserDataRow(8L, "小茗", 12L, true)));
        AdminUserDataService service = new AdminUserDataService(mapper, new ObjectMapper());

        var result = service.userPage(1, 10, " 小茗 ", 1);

        assertThat(result.total()).isEqualTo(1L);
        assertThat(result.list()).singleElement().satisfies(item -> {
            assertThat(item.userId()).isEqualTo(8L);
            assertThat(item.username()).isEqualTo("小茗");
            assertThat(item.teaRecordCount()).isEqualTo(12L);
            assertThat(item.questionnaireCompleted()).isTrue();
        });
        verify(mapper).countUsers("小茗", 1);
    }

    @Test
    void questionnaireReturnsReadableEmptyStateWhenUserHasNotFilledIt() {
        AdminUserDataMapper mapper = mock(AdminUserDataMapper.class);
        when(mapper.selectUsernameById(8L)).thenReturn("小茗");
        when(mapper.selectQuestionnaireByUserId(8L)).thenReturn(null);
        AdminUserDataService service = new AdminUserDataService(mapper, new ObjectMapper());

        AdminUserQuestionnaireVO result = service.questionnaire(8L);

        assertThat(result.userId()).isEqualTo(8L);
        assertThat(result.username()).isEqualTo("小茗");
        assertThat(result.completed()).isFalse();
        assertThat(result.purpose()).isNull();
    }

    @Test
    void questionnaireConvertsStoredAnswerCodesToChineseLabels() {
        AdminUserDataMapper mapper = mock(AdminUserDataMapper.class);
        when(mapper.selectUsernameById(8L)).thenReturn("小茗");
        when(mapper.selectQuestionnaireByUserId(8L)).thenReturn(new AdminUserQuestionnaireRow(
                1, 2, 1, 3, 2, LocalDateTime.of(2026, 10, 10, 12, 0)
        ));
        AdminUserDataService service = new AdminUserDataService(mapper, new ObjectMapper());

        AdminUserQuestionnaireVO result = service.questionnaire(8L);

        assertThat(result.completed()).isTrue();
        assertThat(result.updatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 10, 12, 0));
        assertThat(result.purpose()).isNotBlank();
        assertThat(result.constitutionType()).isNotBlank();
    }

    @Test
    void teaRecordPageRejectsReversedDatesBeforeQueryingDatabase() {
        AdminUserDataMapper mapper = mock(AdminUserDataMapper.class);
        AdminUserDataService service = new AdminUserDataService(mapper, new ObjectMapper());

        assertThatThrownBy(() -> service.teaRecordPage(
                8L, 1, 10, null, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)
        )).isInstanceOf(BizException.class).hasMessage("开始日期不能晚于结束日期");
        verifyNoInteractions(mapper);
    }
}
