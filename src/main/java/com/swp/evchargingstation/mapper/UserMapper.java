package com.swp.evchargingstation.mapper;

import com.swp.evchargingstation.dto.request.UserCreationRequest;
import com.swp.evchargingstation.dto.request.UserUpdateRequest;
import com.swp.evchargingstation.dto.response.UserResponse;
import com.swp.evchargingstation.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);
    @Mapping(target = "role", ignore = true)
    void updateUser(@MappingTarget User user, UserUpdateRequest request);
}
