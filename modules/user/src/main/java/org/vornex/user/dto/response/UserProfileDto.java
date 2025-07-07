package org.vornex.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    UUID id;
    String username;
    String email;

    String firstName;
    String lastName;
    String phoneNumber;
    String avatarUrl;
    String city;

    List<UUID> favoriteListingIds;

    LocalDateTime createdAt;
}
