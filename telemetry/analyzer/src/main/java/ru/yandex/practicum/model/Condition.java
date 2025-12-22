package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conditions")
public class Condition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "type", length = Integer.MAX_VALUE)
    private String type;

    @Column(name = "operation", length = Integer.MAX_VALUE)
    private String operation;

    @Column(name = "value")
    private Integer value;

}