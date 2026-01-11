package ru.yandex.practicum.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    @Builder.Default
    @JsonProperty("timestamp")
    private LocalDateTime timestamp = LocalDateTime.now();

    @JsonProperty("httpStatus")
    private HttpStatus httpStatus;

    @JsonProperty("statusCode")
    private Integer statusCode;

    @JsonProperty("error")
    private String error;

    @JsonProperty("message")
    private String message;

    @JsonProperty("userMessage")
    private String userMessage;

    @JsonProperty("localizedMessage")
    private String localizedMessage;

    @JsonProperty("path")
    private String path;

    @JsonProperty("errorCode")
    private String errorCode;

    @JsonProperty("cause")
    private CauseInfo cause;

    @JsonProperty("suppressed")
    private List<CauseInfo> suppressed;

    @JsonProperty("stackTrace")
    private List<StackTraceInfo> stackTrace;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CauseInfo {
        @JsonProperty("message")
        private String message;

        @JsonProperty("localizedMessage")
        private String localizedMessage;

        @JsonProperty("stackTrace")
        private List<StackTraceInfo> stackTrace;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StackTraceInfo {
        @JsonProperty("className")
        private String className;

        @JsonProperty("methodName")
        private String methodName;

        @JsonProperty("fileName")
        private String fileName;

        @JsonProperty("lineNumber")
        private Integer lineNumber;

        @JsonProperty("moduleName")
        private String moduleName;

        @JsonProperty("moduleVersion")
        private String moduleVersion;

        @JsonProperty("classLoaderName")
        private String classLoaderName;

        @JsonProperty("nativeMethod")
        private Boolean nativeMethod;
    }

    public static ApiErrorResponse fromProductNotFoundException(
            final ru.yandex.practicum.model.error.ProductNotFoundException ex,
            final String path) {

        ApiErrorResponseBuilder builder = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .httpStatus(HttpStatus.NOT_FOUND)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .userMessage(ex.getUserMessage())
                .localizedMessage(ex.getLocalizedMessage())
                .path(path)
                .errorCode("PRODUCT_NOT_FOUND");

        // Маппинг cause
        if (ex.getErrorCause() != null) {
            builder.cause(CauseInfo.builder()
                    .message(ex.getErrorCause().getMessage())
                    .localizedMessage(ex.getErrorCause().getLocalizedMessage())
                    .stackTrace(convertStackTrace(ex.getErrorCause().getStackTrace()))
                    .build());
        }

        // Маппинг suppressed
        if (ex.getSuppressedCauses() != null && !ex.getSuppressedCauses().isEmpty()) {
            List<CauseInfo> suppressedCauses = ex.getSuppressedCauses().stream()
                    .map(cause -> CauseInfo.builder()
                            .message(cause.getMessage())
                            .localizedMessage(cause.getLocalizedMessage())
                            .stackTrace(convertStackTrace(cause.getStackTrace()))
                            .build())
                    .toList();
            builder.suppressed(suppressedCauses);
        }

        // Маппинг stackTrace
        if (ex.getJsonStackTrace() != null) {
            builder.stackTrace(convertStackTrace(ex.getJsonStackTrace()));
        }

        return builder.build();
    }

    private static List<StackTraceInfo> convertStackTrace(
            List<ru.yandex.practicum.model.error.ProductNotFoundException.StackTraceElement> stackTrace) {
        if (stackTrace == null) return null;

        return stackTrace.stream()
                .map(element -> StackTraceInfo.builder()
                        .className(element.getClassName())
                        .methodName(element.getMethodName())
                        .fileName(element.getFileName())
                        .lineNumber(element.getLineNumber())
                        .moduleName(element.getModuleName())
                        .moduleVersion(element.getModuleVersion())
                        .classLoaderName(element.getClassLoaderName())
                        .nativeMethod(element.getNativeMethod())
                        .build())
                .toList();
    }
}