package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;
import com.google.protobuf.Empty;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessorInitializer implements ApplicationRunner {

    private final SnapshotProcessor snapshotProcessor;
    private final HubEventProcessor hubEventProcessor;

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterStub;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== НАЧАЛО ИНИЦИАЛИЗАЦИИ PROCESSORS ===");

        // 1. Ждем немного перед проверкой
        Thread.sleep(3000);
        log.info("Ждем загрузки конфигурации...");

        // 2. Проверяем gRPC соединение
        if (testGrpcConnection()) {
            log.info("✅ gRPC соединение установлено успешно");
        } else {
            log.warn("⚠️ gRPC соединение не установлено, но продолжаем инициализацию");
        }

        // 3. Запускаем процессоры в отдельных потоках
        log.info("Запускаем процессоры...");
        hubEventProcessor.start();
        snapshotProcessor.start();

        // 4. Активируем процессоры после небольшой задержки
        Thread.sleep(2000);

        hubEventProcessor.setInitialized(true);
        snapshotProcessor.setInitialized(true);

        log.info("=== ИНИЦИАЛИЗАЦИЯ PROCESSORS ЗАВЕРШЕНА ===");
        log.info("HubEventProcessor: initialized={}", hubEventProcessor.isInitialized());
        log.info("SnapshotProcessor: initialized={}", snapshotProcessor.isInitialized());
    }

    private boolean testGrpcConnection() {
        log.info("Тестируем подключение к Hub Router...");

        try {
            DeviceActionProto action = DeviceActionProto.newBuilder()
                    .setSensorId("test-connection")
                    .setType(ActionTypeProto.ACTIVATE)
                    .setValue(1)
                    .build();

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId("test-hub")
                    .setScenarioName("test-scenario")
                    .setAction(action)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .build())
                    .build();

            log.info("Отправляем тестовый gRPC запрос...");
            Empty response = hubRouterStub
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .handleDeviceAction(request);

            log.info("✅ Тестовый gRPC запрос успешно отправлен");
            return true;

        } catch (Exception e) {
            log.error("❌ Ошибка тестового gRPC запроса: {}", e.getMessage());
            return false;
        }
    }
}