package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.ScenarioAction;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, Long> {

    @Modifying
    @Query("DELETE FROM ScenarioAction sa WHERE sa.sensor.id = :sensorId")
    void deleteBySensorId(@Param("sensorId") String sensorId);

    @Modifying
    @Query("DELETE FROM ScenarioAction sa WHERE sa.scenario.id = :scenarioId")
    void deleteByScenarioId(@Param("scenarioId") Long scenarioId);
}