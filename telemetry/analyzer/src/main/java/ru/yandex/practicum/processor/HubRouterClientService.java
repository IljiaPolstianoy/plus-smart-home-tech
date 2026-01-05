package ru.yandex.practicum.processor;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubRouterClientService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    @Value("${grpc.client.hub-router.address:not-set}")
    private String grpcAddress;

    private boolean grpcAvailable = false;
    private int retryCount = 0;
    private final int MAX_RETRIES = 3;

    @PostConstruct
    public void init() {
        log.info("🔧 Инициализация gRPC клиента для Hub Router");
        log.info("📡 Адрес Hub Router: {}", grpcAddress);

        testGrpcConnection();
    }

    private void testGrpcConnection() {
        log.debug("🔍 Тестовое подключение к Hub Router...");

        try {
            // Создаем тестовый DeviceActionProto
            DeviceActionProto testAction = DeviceActionProto.newBuilder()
                    .setSensorId("test-sensor-id")
                    .setType(ActionTypeProto.ACTIVATE)
                    .build();

            DeviceActionRequest testRequest = DeviceActionRequest.newBuilder()
                    .setHubId("test-hub-id")
                    .setScenarioName("test-scenario")
                    .setAction(testAction)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .build())
                    .build();

            // Пробуем отправить с коротким таймаутом - ВЫЗЫВАЕМ НАПРЯМУЮ, а не через sendDeviceAction
            try {
                hubRouterClient
                        .withDeadlineAfter(1, TimeUnit.SECONDS)
                        .handleDeviceAction(testRequest);

                grpcAvailable = true;
                retryCount = 0;
                log.info("✅ Hub Router доступен по адресу: {}", grpcAddress);

            } catch (StatusRuntimeException e) {
                // ... обработка ошибок
            }

        } catch (Exception e) {
            grpcAvailable = false;
            log.warn("⚠️ Hub Router недоступен: {}", e.getMessage());
        }
    }

    public void sendDeviceAction(String hubId, String scenarioName, DeviceActionProto action) {
        log.info("📤 === НАЧАЛО ОТПРАВКИ gRPC ===");
        log.info("📍 Параметры: hub={}, scenario={}, sensor={}, type={}, hasValue={}",
                hubId, scenarioName, action.getSensorId(), action.getType(), action.hasValue());

        try {
            // Проверяем доступность
            if (!grpcAvailable && retryCount >= MAX_RETRIES) {
                log.error("🚫 ПРЕРЫВАЕМ: gRPC недоступен после {} попыток", MAX_RETRIES);
                return;
            }

            if (!grpcAvailable) {
                log.warn("gRPC недоступен, проверяем повторно...");
                testGrpcConnection();
                if (!grpcAvailable) {
                    log.error("🚫 Hub Router все еще недоступен");
                    return;
                }
            }

            log.info("🚀 СОЗДАНИЕ gRPC ЗАПРОСА...");

            // Создаем запрос напрямую из полученного proto
            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(action)  // Используем переданный proto
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            log.debug("📝 Сформирован запрос:\n" +
                            "  Hub ID: {}\n" +
                            "  Scenario: {}\n" +
                            "  Sensor: {}\n" +
                            "  Action Type: {}\n" +
                            "  Value: {}",
                    hubId, scenarioName, action.getSensorId(), action.getType(),
                    action.hasValue() ? action.getValue() : "null");

            try {
                log.info("🔄 ВЫЗОВ hubRouterClient.handleDeviceAction()...");

                var startTime = System.currentTimeMillis();

                var response = hubRouterClient
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .handleDeviceAction(request);

                var duration = System.currentTimeMillis() - startTime;

                grpcAvailable = true;
                retryCount = 0;

                log.info("✅ УСПЕХ! gRPC запрос обработан за {} мс", duration);
                log.debug("Полный ответ: {}", response);

                System.out.println("=== GITHUB_DEBUG_GRPC_SUCCESS ===");
                System.out.println("✅ gRPC ЗАПРОС УСПЕШЕН!");
                System.out.println("   Hub: " + hubId);
                System.out.println("   Scenario: " + scenarioName);
                System.out.println("   Время: " + duration + " мс");

            } catch (StatusRuntimeException e) {
                retryCount++;
                grpcAvailable = false;

                log.error("❌ gRpc ОШИБКА: {}", e.getStatus().getCode());
                log.error("📋 Описание: {}", e.getStatus().getDescription());

                System.out.println("=== GITHUB_DEBUG_GRPC_ERROR ===");
                System.out.println("❌ gRPC ОШИБКА: " + e.getStatus().getCode());
                System.out.println("   Описание: " + e.getStatus().getDescription());

                if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
                    log.error("🔌 СЕРВИС НЕДОСТУПЕН: {}", grpcAddress);
                } else if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                    log.error("⏰ ТАЙМАУТ: Запрос превысил 5 секунд");
                } else {
                    log.error("❌ ОШИБКА gRPC: {}", e.getStatus().getCode());
                }

            } catch (Exception e) {
                retryCount++;
                grpcAvailable = false;

                log.error("❌ НЕОЖИДАННАЯ ОШИБКА: {} - {}",
                        e.getClass().getSimpleName(), e.getMessage());

                System.out.println("=== GITHUB_DEBUG_GRPC_UNEXPECTED ===");
                System.out.println("❌ НЕОЖИДАННАЯ ОШИБКА: " + e.getClass().getSimpleName());
                System.out.println("   Сообщение: " + e.getMessage());
            }

        } catch (Exception e) {
            log.error("❌ КРИТИЧЕСКАЯ ОШИБКА: {} - {}",
                    e.getClass().getSimpleName(), e.getMessage());

            System.out.println("=== GITHUB_DEBUG_GRPC_CRITICAL ===");
            System.out.println("❌ КРИТИЧЕСКАЯ ОШИБКА: " + e.getClass().getSimpleName());
            System.out.println("   Сообщение: " + e.getMessage());
        }

        log.info("🏁 === ЗАВЕРШЕНИЕ ОТПРАВКИ gRPC ===");
    }

    /**
     * Вспомогательный метод для удобства
     */
    public void sendDeviceAction(String hubId, String scenarioName,
                                 String sensorId, ActionTypeProto actionType,
                                 Integer value) {
        log.info("📤 Вызов sendDeviceAction с отдельными параметрами");

        try {
            DeviceActionProto.Builder actionBuilder = DeviceActionProto.newBuilder()
                    .setSensorId(sensorId)
                    .setType(actionType);

            if (value != null) {
                actionBuilder.setValue(value);
            }

            DeviceActionProto action = actionBuilder.build();

            // Вызываем основной метод
            sendDeviceAction(hubId, scenarioName, action);

        } catch (Exception e) {
            log.error("❌ Ошибка создания DeviceActionProto: {}", e.getMessage());
        }
    }

    /**
     * Вспомогательный метод без значения
     */
    public void sendDeviceAction(String hubId, String scenarioName,
                                 String sensorId, ActionTypeProto actionType) {
        sendDeviceAction(hubId, scenarioName, sensorId, actionType, null);
    }

    public boolean isGrpcAvailable() {
        return grpcAvailable;
    }

    public String getGrpcStatus() {
        return String.format("gRPC: %s (адрес: %s, попытки: %d/%d)",
                grpcAvailable ? "ДОСТУПЕН" : "НЕДОСТУПЕН",
                grpcAddress, retryCount, MAX_RETRIES);
    }
}