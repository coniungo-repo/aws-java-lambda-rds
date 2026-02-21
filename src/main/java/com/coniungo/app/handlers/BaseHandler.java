package com.coniungo.app.handlers;


import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.coniungo.app.model.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public abstract class BaseHandler {
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    protected APIGatewayProxyResponseEvent buildErrorResponse(int status, String message) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(Map.of("Content-Type", "application/json"));
        response.setStatusCode(status);

        try {
            ApiResponse<Object> error = ApiResponse.builder()
                    .statusCode(status)
                    .isSuccessful(false)
                    .message(message)
                    .data(null)
                    .build();
            response.setBody(objectMapper.writeValueAsString(error));
        } catch (Exception e) {
            // Fallback if JSON serialization itself fails
            response.setBody("{\"isSuccessful\":false,\"message\":\"Internal Server Error\"}");
        }
        return response;
    }
}
