package com.julensserver.dto.common;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    private ApiResponse(boolean success, String message, T data){
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse <T> success (String message, T data){
        return new ApiResponse<>(true, message, data);
    }

    public static ApiResponse<Void> success(String message){
        return new ApiResponse<>(true, message, null);
    }

    public boolean isSuccess(){
        return success;
    }

    public String getMessage(){
        return message;
    }

    public T getData(){
        return data;
    }

}
