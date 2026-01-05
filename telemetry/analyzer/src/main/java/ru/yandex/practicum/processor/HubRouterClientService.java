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

@Service
@Slf4j
public class HubRouterClientService {

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    @Value("${grpc.client.hub-router.address:static://localhost:59090}")
    private String grpcAddress;

    private volatile boolean connectionTested = false;
    private volatile boolean grpcAvailable = false;

    public void sendDeviceAction(String hubId, String scenarioName, DeviceActionProto action) {
        log.info("🎯 ОТПРАВКА КОМАНДЫ: hub={}, scenario={}, sensor={}, type={}",
                hubId, scenarioName, action.getSensorId(), action.getType());

        try {
            // Если еще не тестировали подключение, делаем это сейчас
            if (!connectionTested) {
                testConnection();
            }

            if (!grpcAvailable) {
                log.warn("⚠️ Hub Router недоступен, пропускаем команду");
                return;
            }

            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(action)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .build())
                    .build();

            log.debug("📨 Отправка gRPC запроса...");

            var response = hubRouterClient
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .handleDeviceAction(request);

            log.info("✅ Команда отправлена успешно");
            log.debug("Ответ: {}", response);

        } catch (StatusRuntimeException e) {
            grpcAvailable = false;
            log.error("❌ gRPC ошибка: {} - {}", e.getStatus().getCode(), e.getMessage());

        } catch (Exception e) {
            grpcAvailable = false;
            log.error("❌ Ошибка отправки команды: {}", e.getMessage());
        }
    }

    private synchronized void testConnection() {
        if (connectionTested) return;

        log.info("🔌 Тестирование подключения к Hub Router: {}", grpcAddress);

        try {
            // Простой тест - пытаемся вызвать метод
            DeviceActionProto testAction = DeviceActionProto.newBuilder()
                    .setSensorId("test")
                    .setType(ActionTypeProto.ACTIVATE)
                    .build();

            DeviceActionRequest testRequest = DeviceActionRequest.newBuilder()
                    .setHubId("test")
                    .setScenarioName("test")
                    .setAction(testAction)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .build())
                    .build();

            hubRouterClient
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .handleDeviceAction(testRequest);

            grpcAvailable = true;
            log.info("✅ Hub Router доступен");

        } catch (Exception e) {
            grpcAvailable = false;
            log.warn("⚠️ Hub Router недоступен: {}", e.getMessage());
            log.info("ℹ️ Команды будут пропускаться до восстановления связи");
        }

        connectionTested = true;
    }
}