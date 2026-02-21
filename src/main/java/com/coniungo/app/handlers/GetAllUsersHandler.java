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

import java.util.List;
import java.util.Map;

public class GetAllUsersHandler extends BaseHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final UserService userService = new UserServiceImpl();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log(":::: START GetAllUsersHandler.handleRequest ::::");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setHeaders(Map.of("Content-Type", "application/json"));

        try {
            // Check for optional pagination parameters (defaults provided if missing)
            Map<String, String> queryParams = input.getQueryStringParameters();
            int pageSize = 50;
            int pageNumber = 1;

            if (queryParams != null) {
                if (queryParams.containsKey("pageSize")) {
                    pageSize = Integer.parseInt(queryParams.get("pageSize"));
                }
                if (queryParams.containsKey("pageNumber")) {
                    pageNumber = Integer.parseInt(queryParams.get("pageNumber"));
                }
            }

            logger.log(String.format("INFO: Fetching users (Page: %d, Size: %d)", pageNumber, pageSize));

            // Call Service Layer
            List<UserDTO> users = userService.getAllUsers(pageSize, pageNumber, logger);

            // Success Response Logic
            logger.log("SUCCESS: Retrieved " + users.size() + " users");

            ApiResponse<List<UserDTO>> apiResponse = ApiResponse.<List<UserDTO>>builder()
                    .statusCode(200)
                    .isSuccessful(true)
                    .message("Users retrieved successfully")
                    .data(users)
                    .build();

            response.setStatusCode(200);
            response.setBody(objectMapper.writeValueAsString(apiResponse));

        } catch (NumberFormatException e) {
            logger.log("WARN: Invalid pagination parameters: " + e.getMessage());
            return buildErrorResponse(400, "Pagination parameters must be numeric");
        } catch (Exception e) {
            logger.log("CRITICAL ERROR in GetAllUsersHandler: " + e.getMessage());
            e.printStackTrace();
            return buildErrorResponse(500, "Internal Server Error: " + e.getMessage());
        }

        logger.log(":::: END GetAllUsersHandler.handleRequest [SUCCESS] ::::");
        return response;
    }
}
