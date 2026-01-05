package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scenario_actions")
@IdClass(ScenarioActionId.class)
@RequiredArgsConstructor
@Getter
@Setter
public class ScenarioAction {

    @Id
    @ManyToOne
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Id
    @Column(name = "sensor_id")
    private String sensorId;

    @Id
    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action action;

}