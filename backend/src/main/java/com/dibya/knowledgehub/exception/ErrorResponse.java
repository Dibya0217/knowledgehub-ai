package com.dibya.knowledgehub.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final String correlationId;
    private final Instant timestamp;
    private final Map<String, String> errors;

    public static ErrorResponse of(int status, String title, String detail, String instance) {
        return ErrorResponse.builder()
                .type("about:blank")
                .title(title)
                .status(status)
                .detail(detail)
                .instance(instance)
                .correlationId(MDC.get("correlationId"))
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(int status, String title, String detail, String instance,
                                   Map<String, String> errors) {
        return ErrorResponse.builder()
                .type("about:blank")
                .title(title)
                .status(status)
                .detail(detail)
                .instance(instance)
                .correlationId(MDC.get("correlationId"))
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }
}
