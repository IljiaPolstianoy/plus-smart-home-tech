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
            log.info("🚀 ОТПРАВКА gRPC: hub={}, scenario={}, sensor={}, action={}",
                    hubId, scenarioName, action.getSensorId(), action);

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
            hubRouterClient.handleDeviceAction(request);
            log.info("✅ gRPC запрос успешно отправлен");


        } catch (StatusRuntimeException e) {
            log.error("❌ gRPC ОШИБКА: {}", e.getMessage());
        }
    }
}