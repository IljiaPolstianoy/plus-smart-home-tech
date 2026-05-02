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
public class NoPaymentFoundException extends RuntimeException {

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

    // Конструкторы
    public NoPaymentFoundException(String message) {
        super(message);
    }

    public NoPaymentFoundException(String message, Throwable cause) {
        super(message, cause);
        if (cause != null) {
            this.errorCause = new Cause();
            this.errorCause.setMessage(cause.getMessage());
            this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
        }
    }

    public NoPaymentFoundException(String message, String userMessage, String httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }

    public NoPaymentFoundException(String message, String userMessage, HttpStatus httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.name();
    }

    public NoPaymentFoundException(String message, String userMessage, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.name();
        if (cause != null) {
            this.errorCause = new Cause();
            this.errorCause.setMessage(cause.getMessage());
            this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
        }
    }

    // Фабричные методы для удобного создания исключений
    public NoPaymentFoundException(String paymentId, String paymentType) {
        super(String.format("No %s payment found with ID: %s", paymentType, paymentId));
        this.userMessage = String.format("Платеж типа '%s' с ID %s не найден", paymentType, paymentId);
        this.httpStatus = HttpStatus.NOT_FOUND.name();
    }

    public static NoPaymentFoundException forPaymentId(String paymentId) {
        return new NoPaymentFoundException(
                String.format("Payment not found with ID: %s", paymentId),
                String.format("Платеж с идентификатором %s не найден", paymentId),
                HttpStatus.NOT_FOUND
        );
    }

    public static NoPaymentFoundException forOrder(String orderId) {
        return new NoPaymentFoundException(
                String.format("No payment found for order: %s", orderId),
                String.format("Для заказа %s не найден связанный платеж", orderId),
                HttpStatus.NOT_FOUND
        );
    }

    public static NoPaymentFoundException forTransaction(String transactionId) {
        return new NoPaymentFoundException(
                String.format("No payment found with transaction ID: %s", transactionId),
                String.format("Платеж с идентификатором транзакции %s не найден", transactionId),
                HttpStatus.NOT_FOUND
        );
    }

    public static NoPaymentFoundException forStatus(String status) {
        return new NoPaymentFoundException(
                String.format("No payments found with status: %s", status),
                String.format("Не найдены платежи со статусом: %s", status),
                HttpStatus.NOT_FOUND
        );
    }

    public static NoPaymentFoundException forUser(String userId) {
        return new NoPaymentFoundException(
                String.format("No payments found for user: %s", userId),
                String.format("Для пользователя %s не найдены платежи", userId),
                HttpStatus.NOT_FOUND
        );
    }
}