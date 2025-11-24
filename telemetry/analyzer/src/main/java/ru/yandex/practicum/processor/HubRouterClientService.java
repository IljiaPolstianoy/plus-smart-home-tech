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

    @GrpcClient("hub-router")
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterClient;

    public void sendDeviceAction(String hubId, String scenarioName, DeviceActionProto action) {
        try {
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

            log.info("📨 gRPC запрос: {}", request);
            var response = hubRouterClient.handleDeviceAction(request);
            System.out.println("✅ gRPC запрос успешно отправлен, получен ответ: " + response);
            log.info("✅ gRPC запрос успешно отправлен");

        } catch (StatusRuntimeException e) {
            System.out.println("❌ gRPC ОШИБКА: " + e.getStatus() + " - " + e.getMessage());
            log.error("❌ gRPC ОШИБКА: {}", e.getStatus(), e);
        } catch (Exception e) {
            System.out.println("❌ Неожиданная ошибка gRPC: " + e.getMessage());
            log.error("❌ Неожиданная ошибка gRPC", e);
        }
    }
}