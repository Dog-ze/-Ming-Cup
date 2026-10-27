package com.example.backend.record;

import com.example.backend.entity.TeaRecord;
import com.example.backend.mapper.TeaRecordMapper;
import com.example.backend.record.dto.TeaRecordSaveRequest;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeaRecordServiceTest {

    @BeforeAll
    static void initializeTeaRecordMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TeaRecord.class);
    }

    @AfterAll
    static void clearTeaRecordMetadata() {
        TableInfoHelper.remove(TeaRecord.class);
    }

    @Test
    void updateWritesNullOptionalFieldsSoExistingRecordValuesAreRemoved() {
        TeaRecordMapper teaRecordMapper = mock(TeaRecordMapper.class);
        TeaRecord existing = new TeaRecord();
        existing.setId(8L);
        existing.setUserId(1L);
        existing.setBrand("旧品牌");
        existing.setComment("旧评价");
        existing.setImages("[\"/uploads/records/old.jpg\"]");
        when(teaRecordMapper.selectOne(any())).thenReturn(existing);
        TeaRecordService service = new TeaRecordService(teaRecordMapper, new ObjectMapper());

        service.update(1L, 8L, new TeaRecordSaveRequest(
                "白毫银针", "白茶", "", "线上购买", null, null, 5, "", List.of()
        ));

        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TeaRecord>> captor =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper.class);
        verify(teaRecordMapper).update(org.mockito.ArgumentMatchers.isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("brand", "price", "brew_temp", "comment", "images");
        assertThat(captor.getValue().getParamNameValuePairs().values()).containsNull();
    }
}
