package com.example.backend.point;

import com.example.backend.point.vo.PointTransactionVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PointTransactionVOTest {

    @Test
    void exposesStablePublicIdForRenderingPointHistory() {
        PointTransactionVO transaction = new PointTransactionVO(9L, "DAILY_CLAIM", 20, 20,
                "SUCCESS", "每日领取积分", LocalDateTime.now());

        assertThat(transaction.id()).isEqualTo(9L);
    }
}
