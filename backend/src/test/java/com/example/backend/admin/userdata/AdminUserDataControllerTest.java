package com.example.backend.admin.userdata;

import com.example.backend.admin.user.vo.AdminPageVO;
import com.example.backend.admin.userdata.vo.AdminUserDataListItemVO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserDataControllerTest {

    @Test
    void rootUserDataUrlIsHandledInsteadOfFallingBackToStaticResources() throws Exception {
        AdminUserDataService service = mock(AdminUserDataService.class);
        when(service.userPage(1, 10, null, null)).thenReturn(new AdminPageVO<>(
                List.of(new AdminUserDataListItemVO(8L, "小茗", 12L, true)), 1, 10, 1
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserDataController(service)).build();

        mockMvc.perform(get("/api/admin/user-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.list[0].username").value("小茗"))
                .andExpect(jsonPath("$.data.list[0].teaRecordCount").value(12));
    }
}
