package org.vornex.user.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.vornex.userapi.AccountStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    UUID id;
    String username;
    String email;
    String firstName;
    String lastName;
    String phoneNumber;
    String avatarUrl;
    String city;
    AccountStatus status;
    Set<String> roles; // только названия ролей
    List<UUID> favoriteListingIds;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
