package ru.yandex.practicum.processor;

import com.google.protobuf.Empty;
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
            DeviceActionRequest request = DeviceActionRequest.newBuilder()
                    .setHubId(hubId)
                    .setScenarioName(scenarioName)
                    .setAction(action)
                    .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                            .setSeconds(Instant.now().getEpochSecond())
                            .setNanos(Instant.now().getNano())
                            .build())
                    .build();

            log.info("Отправка действия для хаба {}: {} -> {}", hubId, scenarioName, action.getSensorId());

            Empty empty = hubRouterClient.handleDeviceAction(request);
            log.info("Действие успешно отправлено для сценария: {}", scenarioName);

        } catch (StatusRuntimeException e) {
            log.error("Ошибка при отправке действия через gRPC: {}", e.getStatus().getDescription());
            throw new RuntimeException("Failed to send device action via gRPC", e);
        }
    }
}