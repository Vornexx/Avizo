package org.vornex.user.dto.internal;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileDto {
    String firstName;
    String lastName;
    @Pattern(regexp = "^[0-9\\-+()\\s]+$", message = "Некорректный номер телефона")
    String phoneNumber;
    String avatarUrl;
    String city;
}
