package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.Sensor;

import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, String> {
    boolean existsById(String id);

    Optional<Sensor> findById(String id);

    void deleteById(String id);

    Optional<Sensor> findByIdAndHubId(String id, String hubId);

    boolean existsByIdInAndHubId(java.util.List<String> ids, String hubId);
}