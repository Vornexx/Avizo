package org.vornex.user.dto.response;

public record PublicUserDto(
        String firstName,
        String lastName,
        String phoneNumber,
        String avatarUrl,
        String city
) {
}