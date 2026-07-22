package com.julensserver.dto.common;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;


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
