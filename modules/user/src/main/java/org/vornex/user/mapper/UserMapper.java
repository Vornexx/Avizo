package org.vornex.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.vornex.user.dto.internal.UpdateProfileDto;
import org.vornex.user.dto.internal.UserDto;
import org.vornex.user.dto.response.PublicUserDto;
import org.vornex.user.dto.response.UserProfileDto;
import org.vornex.user.entity.Role;
import org.vornex.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // основный user -> dto Для админки и внутреннего использования
    @Mapping(target = "roles", expression = "java(mapRolesToNames(user.getRoles()))")
    UserDto toUserDto(User user);

    // /me
    UserProfileDto toUserProfilesDto(User user);

    // публичный профиль для других пользователей
    PublicUserDto toPublicUserDto(User user);

//    //обратный маппинг при необходимости
//    User toEntity(UserDto userDto); из role string в Role проблема.

    void updateFromDto(UpdateProfileDto updateProfileDto, @MappingTarget User user);


    // кастомный маппинг: roles -> role names
    default Set<String> mapRolesToNames(Set<Role> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
