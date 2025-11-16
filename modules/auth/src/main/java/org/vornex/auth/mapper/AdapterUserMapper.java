package org.vornex.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.dto.UserRegistrationDto;
import org.vornex.userapi.UserAccountDto;

@Mapper(componentModel = "spring")
public interface AdapterUserMapper {
    UserAccountDto toUserAccountDto(UserLoginDto loginDto);

    // используем expression, чтобы подставить второй параметр напрямую
    @Mapping(target = "password", expression = "java(hashedPassword)")
    @Mapping(target = "id", ignore = true)
    UserAccountDto toUserAccountDto(UserRegistrationDto regDto, String hashedPassword);

}
