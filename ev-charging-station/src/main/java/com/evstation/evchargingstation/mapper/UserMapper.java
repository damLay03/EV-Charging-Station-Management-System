package com.evstation.evchargingstation.mapper;

import com.evstation.evchargingstation.dto.request.UserUpdateRequest;
import com.evstation.evchargingstation.dto.response.UserResponse;
import org.mapstruct.Mapper;

import com.evstation.evchargingstation.dto.request.UserCreationRequest;
import com.evstation.evchargingstation.entity.User;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);
    UserResponse toUserResponse(User user);
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}