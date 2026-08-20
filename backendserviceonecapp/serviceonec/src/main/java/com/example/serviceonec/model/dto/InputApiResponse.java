package com.example.serviceonec.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputApiResponse<T> {

    private String status;
    private int code;
    private String message;
    private Instant timestamp;
    private T data;
    private List<FieldError> errors;
    private PageInfo page;

    public static <T> InputApiResponse<List<T>> success(List<T> data) {

        return InputApiResponse.<List<T>>builder()
                .status("success")
                .code(200)
                .message("Ok")
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static <T> InputApiResponse<List<T>> successWithPagination(List<T> data, PageInfo page) {

        return InputApiResponse.<List<T>>builder()
                .status("success")
                .code(200)
                .message("Ok")
                .timestamp(Instant.now())
                .data(data)
                .build();
    }

    public static <T> InputApiResponse<T> error(int code, String message, List<FieldError> errors) {

        return InputApiResponse.<T>builder()
                .status("error")
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .errors(errors)
                .build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
