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
public class ProductInShoppingCartLowQuantityInWarehouseException extends RuntimeException {

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

    public ProductInShoppingCartLowQuantityInWarehouseException(String message) {
        super(message);
    }

    public ProductInShoppingCartLowQuantityInWarehouseException(
            String message, Throwable cause) {
        super(message, cause);
        this.errorCause = new Cause();
        this.errorCause.setMessage(cause.getMessage());
        this.errorCause.setLocalizedMessage(cause.getLocalizedMessage());
    }

    public ProductInShoppingCartLowQuantityInWarehouseException(
            String message, String userMessage, String httpStatus) {
        super(message);
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }
}
