package ru.practicum.collector;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.collector.handler.hub.HubEventHandler;
import ru.practicum.collector.handler.sensor.SensorEventHandler;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@GrpcService
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;
    private final SendKafka sendKafka;

    public EventController(Set<SensorEventHandler> sensorEventHandlers, Set<HubEventHandler> hubEventHandlers, SendKafka sendKafka) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
        this.sendKafka = sendKafka;
    }

    /**
     * Метод для обработки событий от датчиков.
     * Вызывается при получении нового события от gRPC-клиента.
     *
     * @param request           Событие от датчика
     * @param responseObserver  Ответ для клиента
     */
    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            sendKafka.send(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    /**
     * Метод для обработки событий от хаба.
     * Вызывается при получении нового события от gRPC-клиента.
     *
     * @param request           Событие от хаба
     * @param responseObserver  Ответ для клиента
     */
    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        System.out.println("=== COLLECT HUB EVENT CALLED ===");
        System.out.println("HubId: " + request.getHubId());
        System.out.println("Has timestamp: " + request.hasTimestamp());
        System.out.println("PayloadCase: " + request.getPayloadCase());
        System.out.println("Payload number: " + request.getPayloadCase().getNumber());

        // Проверяем все возможные поля
        System.out.println("Fields check:");
        System.out.println("  hasDeviceAdded: " + request.hasDeviceAdded());
        System.out.println("  hasDeviceRemoved: " + request.hasDeviceRemoved());
        System.out.println("  hasScenarioAdded: " + request.hasScenarioAdded());
        System.out.println("  hasScenarioRemoved: " + request.hasScenarioRemoved());
        try {
            sendKafka.send(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
