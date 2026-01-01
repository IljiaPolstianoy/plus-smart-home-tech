package ru.yandex.practicum.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Исключение, указывающее, что указанный продукт не найден на складе.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoSpecifiedProductInWarehouseException extends RuntimeException {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class StackTraceElement {
        private String classLoaderName;
        private String moduleName;
        private String moduleVersion;
        private String methodName;
        private String fileName;
        private Integer lineNumber;
        private String className;
        private Boolean nativeMethod;
    }

    private Cause cause;

    private List<StackTraceElement> stackTrace;

    private String httpStatus;

    private String userMessage;

    private String message;

    private String localizedMessage;

    private List<Cause> suppressed;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Cause {
        private List<StackTraceElement> stackTrace;
        private String message;
        private String localizedMessage;
    }

    public NoSpecifiedProductInWarehouseException(String message) {
        super(message);
        this.message = message;
    }

    public NoSpecifiedProductInWarehouseException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.cause = new Cause();
        this.cause.setMessage(cause.getMessage());
        this.cause.setLocalizedMessage(cause.getLocalizedMessage());
    }

    public NoSpecifiedProductInWarehouseException(
            String message,
            String userMessage,
            HttpStatus httpStatus) {
        super(message);
        this.message = message;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.toString();
    }

    public NoSpecifiedProductInWarehouseException(
            String message,
            String userMessage,
            String httpStatus) {
        super(message);
        this.message = message;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }
}
