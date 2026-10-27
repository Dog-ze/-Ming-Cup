package com.example.backend.admin.user.vo;

import java.util.List;

public record AdminPageVO<T>(List<T> list, long page, long pageSize, long total) {
}