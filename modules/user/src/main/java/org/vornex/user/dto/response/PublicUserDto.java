package org.vornex.user.dto.response;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserDto {
    @Size(max = 50)
    private String firstName;

    @Size(max = 50)
    private String lastName;

    @Pattern(regexp = "^[0-9\\-+()\\s]+$", message = "Некорректный номер телефона")
    private String phoneNumber;

    @Size(max = 255)
    private String avatarUrl;

    @Size(max = 100)
    private String city;
}