package com.example.backend.point;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PointPolicyTest {

    @Test
    void brewGuideIsFreeOnlyForTheFirstUseOfTheDay() {
        assertEquals(0, PointPolicy.brewGuideCost(false, 5));
        assertEquals(5, PointPolicy.brewGuideCost(true, 5));
    }

    @Test
    void dailyBusinessKeysAreStablePerUserAndDate() {
        assertEquals("daily-claim:42:2026-10-26",
                PointPolicy.dailyKey("daily-claim", 42L, LocalDate.of(2026, 10, 26)));
    }
}
