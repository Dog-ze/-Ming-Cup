package com.example.backend.admin.tea;

import com.example.backend.admin.tea.dto.AdminTeaSaveRequest;
import com.example.backend.admin.tea.mapper.AdminTeaMapper;
import com.example.backend.admin.tea.vo.AdminTeaDetailVO;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.entity.Tea;
import com.example.backend.mapper.TeaMapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminTeaServiceTest {

    @BeforeAll
    static void initializeTeaMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Tea.class);
    }

    @AfterAll
    static void clearTeaMetadata() {
        TableInfoHelper.remove(Tea.class);
    }

    @Test
    void createMapsTheAdminRequestToExistingTeaColumns() {
        AdminTeaMapper adminTeaMapper = mock(AdminTeaMapper.class);
        TeaMapper teaMapper = mock(TeaMapper.class);
        AdminTeaService service = new AdminTeaService(
                adminTeaMapper, teaMapper, mock(AdminOperationLogService.class)
        );
        AdminTeaSaveRequest request = new AdminTeaSaveRequest(
                "西湖龙井", "绿茶", "凉", "中", "[\"清淡鲜爽\"]", "[\"提神\"]",
                "适中", 85, 90, "local", "/uploads/admin/tea/longjing.jpg", 1
        );

        service.create(request);

        ArgumentCaptor<Tea> captor = ArgumentCaptor.forClass(Tea.class);
        verify(teaMapper).insert(captor.capture());
        Tea tea = captor.getValue();
        assertThat(tea.getName()).isEqualTo("西湖龙井");
        assertThat(tea.getNature()).isEqualTo("凉");
        assertThat(tea.getFlavorTags()).isEqualTo("[\"清淡鲜爽\"]");
        assertThat(tea.getEffectTags()).isEqualTo("[\"提神\"]");
        assertThat(tea.getBrewTemp()).isEqualTo(85);
        assertThat(tea.getBrewTime()).isEqualTo(90);
        assertThat(tea.getImageUrl()).isEqualTo("/uploads/admin/tea/longjing.jpg");
        assertThat(tea.getEnabled()).isEqualTo(1);
    }

    @Test
    void deleteRemovesAnExistingTea() {
        AdminTeaMapper adminTeaMapper = mock(AdminTeaMapper.class);
        TeaMapper teaMapper = mock(TeaMapper.class);
        AdminTeaService service = new AdminTeaService(
                adminTeaMapper, teaMapper, mock(AdminOperationLogService.class)
        );
        when(adminTeaMapper.selectDetailById(8L)).thenReturn(new AdminTeaDetailVO(
                8L, "白毫银针", "白茶", "凉", "低", "[]", "[]", "适中",
                85, 90, "local", null, 1, LocalDateTime.now()
        ));

        service.delete(8L);

        verify(teaMapper).deleteById(8L);
    }

    @Test
    void updateWritesNullImageUrlSoExistingImageIsRemoved() {
        AdminTeaMapper adminTeaMapper = mock(AdminTeaMapper.class);
        TeaMapper teaMapper = mock(TeaMapper.class);
        AdminTeaService service = new AdminTeaService(
                adminTeaMapper, teaMapper, mock(AdminOperationLogService.class)
        );
        when(adminTeaMapper.selectDetailById(8L)).thenReturn(new AdminTeaDetailVO(
                8L, "白毫银针", "白茶", "凉", "低", "[]", "[]", "适中",
                85, 90, "local", "/uploads/admin/tea/old.jpg", 1, LocalDateTime.now()
        ));

        service.update(8L, new AdminTeaSaveRequest(
                "白毫银针", "白茶", "凉", "低", "[]", "[]", "适中",
                85, 90, "local", "", 1
        ));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Tea>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(teaMapper).update(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("image_url");
        assertThat(captor.getValue().getParamNameValuePairs().values()).containsNull();
    }
}
