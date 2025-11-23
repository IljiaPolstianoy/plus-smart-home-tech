package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenarios")
@Data
public class Scenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "hub_id")
    private String hubId;

    // Связь с условиями
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL)
    private List<ScenarioCondition> conditions = new ArrayList<>();

    // Связь с действиями
    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL)
    private List<ScenarioAction> actions = new ArrayList<>();
}