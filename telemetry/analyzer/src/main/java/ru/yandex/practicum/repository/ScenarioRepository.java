package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.Scenario;
import ru.yandex.practicum.model.ScenarioProjection;
import ru.yandex.practicum.model.ScenarioWithDetails;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    @Query(value = """
        SELECT
            s.id as scenarioId,
            s.name as scenarioName,
            s.hub_id as hubId,
            sc.sensor_id as sensorId,
            c.type as conditionType,
            c.operation as conditionOperation,
            c.value as conditionValue,
            a.type as actionType,
            a.value as actionValue,
            sa.sensor_id as actionSensorId
        FROM scenarios s
        LEFT JOIN scenario_conditions sc ON s.id = sc.scenario_id
        LEFT JOIN conditions c ON sc.condition_id = c.id
        LEFT JOIN scenario_actions sa ON s.id = sa.scenario_id
        LEFT JOIN actions a ON sa.action_id = a.id
        WHERE s.hub_id = :hubId
        """, nativeQuery = true)
    List<ScenarioProjection> findScenariosWithDetailsByHubId(@Param("hubId") String hubId);

    java.util.List<Scenario> findByHubId(String hubId);

    Optional<Scenario> findByHubIdAndName(String hubId, String name);
}