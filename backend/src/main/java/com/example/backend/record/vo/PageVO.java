package com.example.backend.record.vo;

import java.util.List;

/**
 * 分页接口统一返回的数据。
 *
 * @param records 当前页的品茶记录
 * @param page 当前页码，从 1 开始
 * @param pageSize 每页数量
 * @param total 全部记录数量
 */
public record PageVO<T>(List<T> records, long page, long pageSize, long total) {
}
