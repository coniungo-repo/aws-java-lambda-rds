package com.coniungo.app.model;


import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private int statusCode;
    private boolean isSuccessful;
    private String message;
    private T data;
}