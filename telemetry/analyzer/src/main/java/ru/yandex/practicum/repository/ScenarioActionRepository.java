package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.model.ScenarioAction;
import ru.yandex.practicum.model.ScenarioActionId;

public interface ScenarioActionRepository extends JpaRepository<ScenarioAction, ScenarioActionId> {  // Исправь на ScenarioActionId

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioAction sa WHERE sa.sensorId = :sensorId")
    void deleteBySensorId(@Param("sensorId") String sensorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioAction sa WHERE sa.scenario.id = :scenarioId")
    void deleteByScenarioId(@Param("scenarioId") Long scenarioId);
}