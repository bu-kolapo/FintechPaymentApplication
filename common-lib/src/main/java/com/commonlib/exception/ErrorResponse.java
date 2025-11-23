package com.commonlib.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
@Data
@NoArgsConstructor
public class ErrorResponse {

    private int status;
    private String code;
    private String message;
    private String path;
    private LocalDateTime timestamp = LocalDateTime.now();
    private List<ValidationError> validationErrors;

    public ErrorResponse(int status, String code, String message, String path) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.path = path;
    }

}