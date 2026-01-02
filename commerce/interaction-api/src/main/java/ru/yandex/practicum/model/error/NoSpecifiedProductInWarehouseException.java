package ru.yandex.practicum.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NoSpecifiedProductInWarehouseException extends RuntimeException {

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

    // Вложенная структура StackTraceElement
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

    // Вложенная структура Cause
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
    public NoSpecifiedProductInWarehouseException(String message) {
        super(message);
    }

    public NoSpecifiedProductInWarehouseException(
            String message, Throwable cause) {
        super(message, cause);
        this.errorCause = new Cause();
        this.errorCause.setMessage(cause.getMessage());
        this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
    }

    public NoSpecifiedProductInWarehouseException(
            String message, String userMessage, String httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }
}
