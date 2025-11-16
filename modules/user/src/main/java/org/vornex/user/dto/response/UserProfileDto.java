package org.vornex.user.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        String avatarUrl,
        String city,
        List<UUID> favoriteListingIds,
        LocalDateTime createdAt
) {
}
