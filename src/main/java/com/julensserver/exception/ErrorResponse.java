package com.julensserver.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponse {

    private boolean success;
    private int status;
    private String code;
    private String message;
    private Map<String ,String > errors;


    public static ErrorResponse from(ErrorCode errorCode){
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message){
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                message,
                null
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, Map<String, String> errors){
        return new ErrorResponse(
                false,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                errorCode.getMessage(),
                errors
        );
    }


}
