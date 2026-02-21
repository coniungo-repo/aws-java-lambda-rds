package com.coniungo.app.mappers;

import com.coniungo.app.dto.UserDTO;
import com.coniungo.app.model.User;

public class UserMapper {

    public static UserDTO toDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
