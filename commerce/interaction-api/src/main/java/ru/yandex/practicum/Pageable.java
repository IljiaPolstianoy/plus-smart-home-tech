package ru.yandex.practicum;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class Pageable {

    @Min(value = 0, message = "Page must be greater than or equal to 0")
    private int page;

    @Min(value = 1, message = "Size must be greater than or equal to 1")
    private int size;

    private List<String> sort;
}
