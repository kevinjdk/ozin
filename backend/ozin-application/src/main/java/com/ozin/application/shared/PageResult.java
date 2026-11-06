package com.ozin.application.shared;

import java.util.List;

public record PageResult<T>(List<T> items, long totalItems, int page, int size) {

    public PageResult {
        items = List.copyOf(items);
    }
}
