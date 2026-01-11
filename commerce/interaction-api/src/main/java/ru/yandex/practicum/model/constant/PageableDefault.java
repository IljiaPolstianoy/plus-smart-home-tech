package ru.yandex.practicum.model.constant;

import org.springframework.data.domain.Sort;

public class PageableDefault {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT_BY = "orderId";
    public static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;
}
