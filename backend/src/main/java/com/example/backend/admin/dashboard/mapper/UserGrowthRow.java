package com.example.backend.admin.dashboard.mapper;

import lombok.Data;
import lombok.Getter;

import java.time.LocalDate;

public class UserGrowthRow {
    private LocalDate date;
    private Long newUserCount;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getNewUserCount() {
        return newUserCount;
    }

    public void setNewUserCount(Long newUserCount) {
        this.newUserCount = newUserCount;
    }
}