package com.nexplm.common.api;

import java.util.List;

/**
 * 分页响应。items 为当前页数据，total 为满足条件的总数。
 */
public record PageResult<T>(List<T> items, long total, int page, int size) {

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        return new PageResult<>(items, total, page, size);
    }

    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size);
    }
}
