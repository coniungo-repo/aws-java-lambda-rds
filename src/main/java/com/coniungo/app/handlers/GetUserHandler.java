package com.coniungo.app.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.coniungo.app.dto.UserDTO;
import com.coniungo.app.model.ApiResponse;
import com.coniungo.app.service.UserService;
import com.coniungo.app.service.impl.UserServiceImpl;

import java.util.Map;
import java.util.Optional;

public class GetUserHandler extends BaseHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final UserService userService = new UserServiceImpl();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(":::: START GetUserHandler.handleRequest ::::");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(Map.of("Content-Type", "application/json"));

        try {
            // Validate Input
            Map<String, String> queryParams = input.getQueryStringParameters();
            if (queryParams == null || !queryParams.containsKey("id")) {
                logger.log("WARN: Request rejected - Missing 'id' parameter in query string");
                return buildErrorResponse(400, "Missing 'id' parameter");
            }

            String idRaw = queryParams.get("id");
            logger.log("INFO: Processing request for User ID: " + idRaw);

            Long userId = Long.parseLong(idRaw);

            // Call Service Layer
            logger.log("INFO: Invoking UserService.getUserById for ID: " + userId);
            Optional<UserDTO> userDto = userService.getUserById(userId, logger);

            // Success Response Logic
            if (userDto.isPresent()) {
                logger.log("SUCCESS: User data retrieved for ID: " + userId);

                ApiResponse<UserDTO> apiResponse = ApiResponse.<UserDTO>builder()
                        .statusCode(200)
                        .isSuccessful(true)
                        .message("User found")
                        .data(userDto.get())
                        .build();

                response.setStatusCode(200);
                response.setBody(objectMapper.writeValueAsString(apiResponse));

                logger.log(":::: END GetUserHandler.handleRequest [SUCCESS] ::::");
                return response;
            } else {
                logger.log("INFO: User not found in database for ID: " + userId);
                return buildErrorResponse(404, "User not found with ID: " + userId);
            }

        } catch (NumberFormatException e) {
            logger.log("ERROR: Failed to parse User ID. Invalid format: " + e.getMessage());
            return buildErrorResponse(400, "Invalid ID format: must be a number");
        } catch (Exception e) {
            logger.log("CRITICAL ERROR in GetUserHandler: " + e.getMessage());
            // This logs the full stack trace for deep debugging
            e.printStackTrace();
            return buildErrorResponse(500, "Internal Server Error: " + e.getMessage());
        }
    }
}
