package ru.yandex.practicum.processor;

import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouterClientService {

    @GrpcClient("hub-router") // Имя должно совпадать с конфигурацией в application.yml
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    private boolean grpcAvailable = false;
    private int retryCount = 0;
    private final int MAX_RETRIES = 3;

    public void sendDeviceAction(String hubId, String scenarioName, DeviceActionProto action) {
        try {
            if (!grpcAvailable && retryCount >= MAX_RETRIES) {
                log.warn("gRPC сервис недоступен после {} попыток, пропускаем действие", MAX_RETRIES);
                return;
            }

            System.out.println("=== GITHUB_DEBUG_GRPC ===");
            System.out.println("🚀 ОТПРАВКА gRPC: hub=" + hubId +
                    ", scenario=" + scenarioName +
                    ", sensor=" + action.getSensorId() +
                    ", type=" + action.getType() +
                    ", value=" + action.getValue());

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(action)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            log.debug("📨 gRPC запрос: hubId={}, scenario={}", hubId, scenarioName);

            try {
                var response = hubRouterClient.handleDeviceAction(request);
                grpcAvailable = true;
                retryCount = 0;
                System.out.println("✅ gRPC запрос успешно отправлен");
                log.debug("✅ gRPC запрос успешно обработан");

            } catch (StatusRuntimeException e) {
                retryCount++;
                grpcAvailable = false;

                System.out.println("❌ gRPC ОШИБКА: " + e.getStatus().getCode() + " - " + e.getMessage());
                log.error("❌ gRPC ошибка при отправке действия: {} - {}",
                        e.getStatus().getCode(), e.getMessage());

                // Не логируем stack trace для UNAVAILABLE - это нормально при недоступности сервиса
                if (e.getStatus().getCode() != io.grpc.Status.Code.UNAVAILABLE) {
                    log.error("❌ Детали gRPC ошибки:", e);
                }

            } catch (Exception e) {
                retryCount++;
                grpcAvailable = false;

                System.out.println("❌ Неожиданная ошибка gRPC: " + e.getClass().getSimpleName());
                log.error("❌ Неожиданная ошибка gRPC при отправке действия", e);
            }

        } catch (Exception e) {
            System.out.println("❌ Ошибка подготовки gRPC запроса: " + e.getMessage());
            log.error("❌ Ошибка подготовки gRPC запроса", e);
        }
    }
}