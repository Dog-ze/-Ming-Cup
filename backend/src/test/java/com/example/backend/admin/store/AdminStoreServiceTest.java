package com.example.backend.admin.store;

import com.example.backend.admin.store.dto.AdminStoreSaveRequest;
import com.example.backend.admin.store.dto.StorePromotionRequest;
import com.example.backend.admin.operationlog.AdminOperationLogService;
import com.example.backend.common.BizException;
import com.example.backend.entity.PairingStore;
import com.example.backend.mapper.PairingStoreMapper;
import com.example.backend.security.AdminCurrentUser;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminStoreServiceTest {

    @BeforeAll
    static void initializeStoreMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), PairingStore.class);
    }

    @AfterAll
    static void clearStoreMetadata() {
        TableInfoHelper.remove(PairingStore.class);
    }

    @AfterEach
    void clearCurrentAdmin() {
        AdminCurrentUser.clear();
    }

    @Test
    void createSerializesStoreTagsAndSetsSafeDefaults() {
        PairingStoreMapper storeMapper = mock(PairingStoreMapper.class);
        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        AdminStoreService service = new AdminStoreService(storeMapper, logService, new ObjectMapper());
        AdminCurrentUser.set(1L, java.time.Instant.now().plusSeconds(3600));

        service.create(saveRequest(new BigDecimal("119.0077"), new BigDecimal("25.4541")));

        ArgumentCaptor<PairingStore> captor = ArgumentCaptor.forClass(PairingStore.class);
        verify(storeMapper).insert(captor.capture());
        PairingStore saved = captor.getValue();
        assertThat(saved.getTeaCategories()).isEqualTo("[\"绿茶\",\"白茶\"]");
        assertThat(saved.getFoodTags()).isEqualTo("[\"水果\",\"清淡糕点\"]");
        assertThat(saved.getStatus()).isEqualTo(1);
        assertThat(saved.getPromotionEnabled()).isZero();
        assertThat(saved.getPromotionScore()).isZero();
    }

    @Test
    void createRejectsOnlyOneCoordinate() {
        PairingStoreMapper storeMapper = mock(PairingStoreMapper.class);
        AdminStoreService service = new AdminStoreService(storeMapper, mock(AdminOperationLogService.class), new ObjectMapper());

        assertThatThrownBy(() -> service.create(saveRequest(new BigDecimal("119.0077"), null)))
                .isInstanceOf(BizException.class)
                .hasMessage("经度和纬度必须同时填写");
    }

    @Test
    void promotionRejectsMissingDatesWhenEnabled() {
        PairingStoreMapper storeMapper = mock(PairingStoreMapper.class);
        AdminStoreService service = new AdminStoreService(storeMapper, mock(AdminOperationLogService.class), new ObjectMapper());

        assertThatThrownBy(() -> service.updatePromotion(8L, new StorePromotionRequest(1, 20, null, null)))
                .isInstanceOf(BizException.class)
                .hasMessage("开启推广时必须填写开始和结束时间");
    }

    @Test
    void disablingPromotionClearsItsScoreAndDates() {
        PairingStoreMapper storeMapper = mock(PairingStoreMapper.class);
        when(storeMapper.selectById(8L)).thenReturn(new PairingStore());
        AdminStoreService service = new AdminStoreService(storeMapper, mock(AdminOperationLogService.class), new ObjectMapper());
        AdminCurrentUser.set(1L, java.time.Instant.now().plusSeconds(3600));

        service.updatePromotion(8L, new StorePromotionRequest(
                0, 20,
                LocalDateTime.of(2026, 10, 10, 0, 0),
                LocalDateTime.of(2026, 10, 31, 23, 59)
        ));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<PairingStore>> captor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper.class);
        verify(storeMapper).update(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet())
                .contains("promotion_enabled", "promotion_score", "promotion_start_at", "promotion_end_at");
        assertThat(captor.getValue().getParamNameValuePairs().values())
                .contains(0, null);
    }

    @Test
    void updateWritesNullOptionalFieldsSoExistingStoreValuesAreRemoved() {
        PairingStoreMapper storeMapper = mock(PairingStoreMapper.class);
        when(storeMapper.selectById(8L)).thenReturn(new PairingStore());
        AdminStoreService service = new AdminStoreService(
                storeMapper, mock(AdminOperationLogService.class), new ObjectMapper()
        );
        AdminCurrentUser.set(1L, java.time.Instant.now().plusSeconds(3600));

        service.update(8L, saveRequest(null, null));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PairingStore>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(storeMapper).update(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet())
                .contains("cover_image", "longitude", "latitude", "phone");
        assertThat(captor.getValue().getParamNameValuePairs().values()).containsNull();
    }

    @Test
    void deleteRemovesStoreAndWritesOperationLog() {
        PairingStoreMapper storeMapper = mock(PairingStoreMapper.class);
        AdminOperationLogService logService = mock(AdminOperationLogService.class);
        PairingStore store = new PairingStore();
        store.setId(8L);
        when(storeMapper.selectById(8L)).thenReturn(store);
        AdminStoreService service = new AdminStoreService(storeMapper, logService, new ObjectMapper());
        AdminCurrentUser.set(1L, java.time.Instant.now().plusSeconds(3600));

        service.delete(8L);

        verify(storeMapper).deleteById(8L);
        verify(logService).recordCurrentAdmin(
                org.mockito.ArgumentMatchers.eq("pairing_store"),
                org.mockito.ArgumentMatchers.eq("delete"),
                org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.anyMap()
        );
    }

    private AdminStoreSaveRequest saveRequest(BigDecimal longitude, BigDecimal latitude) {
        return new AdminStoreSaveRequest(
                "茶香小馆", "", "莆田市城厢区示例路 88 号", longitude, latitude,
                "0594-1234567", List.of("绿茶", "白茶"), List.of("水果", "清淡糕点"),
                "提供清淡茶点与安静品茶空间。"
        );
    }
}
