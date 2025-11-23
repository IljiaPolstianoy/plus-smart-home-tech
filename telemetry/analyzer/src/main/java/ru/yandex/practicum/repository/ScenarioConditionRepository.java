package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.ScenarioCondition;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, Long> {

    @Modifying
    @Query("DELETE FROM ScenarioCondition sc WHERE sc.sensor.id = :sensorId")
    void deleteBySensorId(@Param("sensorId") String sensorId);

    @Modifying
    @Query("DELETE FROM ScenarioCondition sc WHERE sc.scenario.id = :scenarioId")
    void deleteByScenarioId(@Param("scenarioId") Long scenarioId);
}
