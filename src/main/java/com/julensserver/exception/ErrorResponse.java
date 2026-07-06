package com.julensserver.exception;

import java.util.Map;

public class ErrorResponse {

    private boolean success;
    private int status;
    private String code;
    private String message;
    private Map<String ,String > errors;

    private ErrorResponse(boolean success, int status, String code, String message, Map<String ,String > errors){
        this.success=success;
        this.status=status;
        this.code=code;
        this.message=message;
        this.errors=errors;
    }

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

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage(){
        return message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

}
