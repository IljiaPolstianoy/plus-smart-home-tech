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
public class NotEnoughInfoInOrderToCalculateException extends RuntimeException {

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

    public NotEnoughInfoInOrderToCalculateException(String message) {
        super(message);
    }

    public NotEnoughInfoInOrderToCalculateException(String message, Throwable cause) {
        super(message, cause);
        if (cause != null) {
            this.errorCause = new Cause();
            this.errorCause.setMessage(cause.getMessage());
            this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
        }
    }

    public NotEnoughInfoInOrderToCalculateException(String message, String userMessage, String httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    public NotEnoughInfoInOrderToCalculateException(String message, String userMessage, HttpStatus httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.name();
    }

    public NotEnoughInfoInOrderToCalculateException(String message, String userMessage, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.name();
        if (cause != null) {
            this.errorCause = new Cause();
            this.errorCause.setMessage(cause.getMessage());
            this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
        }
    }

    public NotEnoughInfoInOrderToCalculateException(String orderId, String missingInfo) {
        super(String.format("Not enough information in order %s to calculate: missing %s", orderId, missingInfo));
        this.userMessage = String.format("Для расчета заказа %s недостаточно информации: отсутствует %s", orderId, missingInfo);
        this.httpStatus = HttpStatus.BAD_REQUEST.name();
    }

    public NotEnoughInfoInOrderToCalculateException(String orderId, List<String> missingFields) {
        super(String.format("Not enough information in order %s to calculate: missing fields %s",
                orderId, String.join(", ", missingFields)));
        this.userMessage = String.format("Для расчета заказа %s недостаточно информации: отсутствуют поля: %s",
                orderId, String.join(", ", missingFields));
        this.httpStatus = HttpStatus.BAD_REQUEST.name();
    }

    public static NotEnoughInfoInOrderToCalculateException forDeliveryCalculation(String orderId) {
        return new NotEnoughInfoInOrderToCalculateException(
                String.format("Not enough information in order %s to calculate delivery", orderId),
                String.format("Для расчета доставки заказа %s недостаточно информации", orderId),
                HttpStatus.BAD_REQUEST
        );
    }

    public static NotEnoughInfoInOrderToCalculateException forPaymentCalculation(String orderId) {
        return new NotEnoughInfoInOrderToCalculateException(
                String.format("Not enough information in order %s to calculate payment", orderId),
                String.format("Для расчета оплаты заказа %s недостаточно информации", orderId),
                HttpStatus.BAD_REQUEST
        );
    }

    public static NotEnoughInfoInOrderToCalculateException forTotalCalculation(String orderId) {
        return new NotEnoughInfoInOrderToCalculateException(
                String.format("Not enough information in order %s to calculate total", orderId),
                String.format("Для расчета итоговой суммы заказа %s недостаточно информации", orderId),
                HttpStatus.BAD_REQUEST
        );
    }
}