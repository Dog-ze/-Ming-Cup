package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.backend.enums.AcquisitionMethod;
import com.example.backend.enums.ConstitutionType;
import com.example.backend.enums.FlavorPreference;
import com.example.backend.enums.InsomniaStatus;
import com.example.backend.enums.StrengthPreference;
import com.example.backend.enums.TeaPurpose;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityAndEnumMappingTest {

    @Test
    void sixEntitiesMapToTheDocumentedTables() {
        assertThat(User.class.getAnnotation(TableName.class).value()).isEqualTo("`user`");
        assertThat(Questionnaire.class.getAnnotation(TableName.class).value()).isEqualTo("questionnaire");
        assertThat(Tea.class.getAnnotation(TableName.class).value()).isEqualTo("tea");
        assertThat(TeaRecord.class.getAnnotation(TableName.class).value()).isEqualTo("tea_record");
        assertThat(ActionLog.class.getAnnotation(TableName.class).value()).isEqualTo("action_log");
        assertThat(PairingRule.class.getAnnotation(TableName.class).value()).isEqualTo("pairing_rule");
    }

    @Test
    void questionnaireUsesIntegerCodesAndDocumentedColumnNames() throws Exception {
        assertThat(Questionnaire.class.getDeclaredField("purpose").getType()).isEqualTo(Integer.class);
        assertThat(Questionnaire.class.getDeclaredField("constitutionType").getType()).isEqualTo(Integer.class);
        assertThat(columnName(Questionnaire.class, "constitutionType")).isEqualTo("constitution_type");
        assertThat(columnName(Questionnaire.class, "flavorPreference")).isEqualTo("flavor_preference");
        assertThat(columnName(Questionnaire.class, "strengthPreference")).isEqualTo("strength_preference");
    }

    @Test
    void teaRecordMatchesTheLatestSchema() throws Exception {
        assertThat(TeaRecord.class.getDeclaredField("brand").getType()).isEqualTo(String.class);
        assertThat(TeaRecord.class.getDeclaredField("acquisitionMethod").getType()).isEqualTo(String.class);
        assertThat(TeaRecord.class.getDeclaredField("price").getType()).isEqualTo(BigDecimal.class);
        assertThat(columnName(TeaRecord.class, "acquisitionMethod")).isEqualTo("acquisition_method");
        assertThat(TeaRecord.class.getDeclaredFields()).extracting(Field::getName).doesNotContain("brewTime");
    }

    @Test
    void questionnaireEnumsUseTheDatabaseCodes() {
        assertThat(TeaPurpose.fromCode(0)).isEqualTo(TeaPurpose.DIGEST);
        assertThat(TeaPurpose.fromCode(2).getLabel()).isEqualTo("暖胃");
        assertThat(ConstitutionType.fromCode(1).getLabel()).isEqualTo("怕热");
        assertThat(InsomniaStatus.fromCode(2).getLabel()).isEqualTo("不确定");
        assertThat(FlavorPreference.fromCode(4).getLabel()).isEqualTo("甜润温和");
        assertThat(StrengthPreference.fromCode(0).getLabel()).isEqualTo("清淡");
        assertThat(AcquisitionMethod.fromValue("朋友赠送")).isEqualTo(AcquisitionMethod.GIFT);
        assertThatThrownBy(() -> TeaPurpose.fromCode(6)).isInstanceOf(IllegalArgumentException.class);
    }

    private String columnName(Class<?> type, String fieldName) throws Exception {
        TableField tableField = type.getDeclaredField(fieldName).getAnnotation(TableField.class);
        return tableField.value();
    }
}
