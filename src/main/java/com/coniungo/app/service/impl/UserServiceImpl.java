package com.coniungo.app.service.impl;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.coniungo.app.dao.DatabaseService;
import com.coniungo.app.dao.PgDataService;
import com.coniungo.app.dto.UserDTO;
import com.coniungo.app.mappers.UserMapper;
import com.coniungo.app.model.User;
import com.coniungo.app.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserServiceImpl implements UserService {

    private final DatabaseService<User> dbService =
            new PgDataService<>();

    private static final String TABLE_NAME = "\"User\"";

    @Override
    public Optional<UserDTO> getUserById(Long id, LambdaLogger logger) {

        logger.log("Service: Fetching user with ID " + id);

        User user = dbService.read(
                TABLE_NAME,
                "id",
                id,
                rs -> User.builder()
                        .id(rs.getLong("id"))
                        .username(rs.getString("username"))
                        .email(rs.getString("email"))
                        .build()
        );

        return Optional.ofNullable(user)
                .map(UserMapper::toDTO);
    }

    @Override
    public List<UserDTO> getAllUsers(
            int pageSize,
            int pageNumber,
            LambdaLogger logger
    ) {

        logger.log("Service: Fetching paginated users");

        List<User> users = dbService.readPaginated(
                TABLE_NAME,
                pageSize,
                pageNumber,
                rs -> User.builder()
                        .id(rs.getLong("id"))
                        .username(rs.getString("username"))
                        .email(rs.getString("email"))
                        .build()
        );

        return users.stream()
                .map(UserMapper::toDTO)
                .collect(Collectors.toList());
    }
}