package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "scenario_conditions")
@IdClass(ScenarioConditionId.class)
public class ScenarioCondition {

    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Id
    @ManyToOne
    @JoinColumn(name = "condition_id")
    private Condition condition;
}