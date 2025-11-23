package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "scenario_actions")
@Data
public class ScenarioAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @Column(name = "sensor_id")  // Обратите внимание на название колонки в БД
    private String sensorId;     // И название поля в Java

    @ManyToOne
    @JoinColumn(name = "action_id")
    private Action action;
}