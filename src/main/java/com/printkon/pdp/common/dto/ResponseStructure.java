package com.printkon.pdp.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseStructure<T> {
    private Boolean success;
    private Integer statusCode;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    // Success response helper methods
    public static <T> ResponseStructure<T> success(T data, String message) {
        return ResponseStructure.<T>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ResponseStructure<T> created(T data, String message) {
        return ResponseStructure.<T>builder()
                .success(true)
                .statusCode(201)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // Error response helper methods
    public static <T> ResponseStructure<T> error(String message, Integer statusCode) {
        return ResponseStructure.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}