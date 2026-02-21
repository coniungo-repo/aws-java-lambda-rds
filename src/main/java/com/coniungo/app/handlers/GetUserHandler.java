package com.coniungo.app.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class GetUserHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("::::GetUserHandler.handleRequest::::");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            response.setStatusCode(200);
            response.setBody("Hello World");
            response.setHeaders(Map.of("Content-Type", "application/json"));

        } catch (Exception e) {
            logger.log("ERROR: " + e.getMessage());
            response.setStatusCode(500);
            response.setBody("Internal Server Error");
        }

        return response;
    }
}