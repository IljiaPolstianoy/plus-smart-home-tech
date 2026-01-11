package ru.yandex.practicum.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoDeliveryFoundException extends RuntimeException {

    @JsonProperty("cause")
    private Cause errorCause;

    @JsonProperty("stackTrace")
    private List<StackTraceElement> jsonStackTrace;

    @JsonProperty("httpStatus")
    private String httpStatus;

    @JsonProperty("userMessage")
    private String userMessage;

    @JsonProperty("message")
    @Override
    public String getMessage() {
        return super.getMessage();
    }

    @JsonProperty("localizedMessage")
    private String localizedMessage;

    @JsonProperty("suppressed")
    private List<Cause> suppressedCauses;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class StackTraceElement {
        @JsonProperty("classLoaderName")
        private String classLoaderName;

        @JsonProperty("moduleName")
        private String moduleName;

        @JsonProperty("moduleVersion")
        private String moduleVersion;

        @JsonProperty("methodName")
        private String methodName;

        @JsonProperty("fileName")
        private String fileName;

        @JsonProperty("lineNumber")
        private Integer lineNumber;

        @JsonProperty("className")
        private String className;

        @JsonProperty("nativeMethod")
        private Boolean nativeMethod;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Cause {
        @JsonProperty("stackTrace")
        private List<StackTraceElement> stackTrace;

        @JsonProperty("message")
        private String message;

        @JsonProperty("localizedMessage")
        private String localizedMessage;
    }

    public NoDeliveryFoundException(String message, Throwable cause) {
        super(message, cause);
        this.errorCause = new Cause();
        this.errorCause.setMessage(cause.getMessage());
        this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
    }

    public NoDeliveryFoundException(String message, String userMessage, String httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    public NoDeliveryFoundException(String message, String userMessage, HttpStatus httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.name();
    }

    public NoDeliveryFoundException(String message, String userMessage, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.name();
        if (cause != null) {
            this.errorCause = new Cause();
            this.errorCause.setMessage(cause.getMessage());
            this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
        }
    }

    public NoDeliveryFoundException(String deliveryId) {
        super(String.format("Delivery with ID %s not found", deliveryId));
        this.userMessage = String.format("Доставка с ID %s не найдена", deliveryId);
        this.httpStatus = HttpStatus.NOT_FOUND.name();
    }

    public NoDeliveryFoundException(String orderId, String deliveryType) {
        super(String.format("No %s delivery found for order %s", deliveryType, orderId));
        this.userMessage = String.format("Доставка типа '%s' не найдена для заказа %s", deliveryType, orderId);
        this.httpStatus = HttpStatus.NOT_FOUND.name();
    }

    public static NoDeliveryFoundException forDeliveryId(String deliveryId) {
        return new NoDeliveryFoundException(
                String.format("Delivery not found with ID: %s", deliveryId),
                String.format("Доставка с идентификатором %s не найдена", deliveryId),
                HttpStatus.NOT_FOUND
        );
    }

    public static NoDeliveryFoundException forOrder(String orderId) {
        return new NoDeliveryFoundException(
                String.format("No delivery associated with order: %s", orderId),
                String.format("Для заказа %s не найдена связанная доставка", orderId),
                HttpStatus.NOT_FOUND
        );
    }

    public static NoDeliveryFoundException forTrackingNumber(String trackingNumber) {
        return new NoDeliveryFoundException(
                String.format("No delivery found with tracking number: %s", trackingNumber),
                String.format("Доставка с трек-номером %s не найдена", trackingNumber),
                HttpStatus.NOT_FOUND
        );
    }
}