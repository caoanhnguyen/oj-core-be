package com.kma.ojcore.mapper;

import com.kma.ojcore.dto.request.users.UpdateUserSdi;
import com.kma.ojcore.dto.response.auth.UserResponse;
import com.kma.ojcore.dto.response.users.UserDetailsSdo;
import com.kma.ojcore.entity.Role;
import com.kma.ojcore.entity.User;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesToRoleNames")
    UserResponse toUserResponse(User user);

    default String ignoreEmail(User user, boolean isMine) {
        return isMine ? user.getEmail() : null;
    }

    @Named("rolesToRoleNames")
    default Set<String> rolesToRoleNames(Set<Role> roles) {
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
    }

    @Mapping(source = "user.roles", target = "roles", qualifiedByName = "rolesToRoleNames")
    @Mapping(target = "email", expression = "java(ignoreEmail(user, isMine))")
    UserDetailsSdo toUserDetailsSdo(User user, boolean isMine);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void UpdateUserFromUpdateSdi(UpdateUserSdi request, @MappingTarget User user);
}


