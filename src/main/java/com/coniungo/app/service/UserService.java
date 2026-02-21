package com.coniungo.app.service;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.coniungo.app.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<UserDTO> getUserById(Long id, LambdaLogger logger);
    List<UserDTO> getAllUsers(int pageSize, int pageNumber, LambdaLogger logger);
}
