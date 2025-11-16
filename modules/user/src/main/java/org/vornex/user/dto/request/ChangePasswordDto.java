package org.vornex.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@NoArgsConstructor
public class ChangePasswordDto {

    @NotBlank
    String currentPassword;

    @NotBlank
    @Size(min = 8, message = "Пароль должен быть не менее 8 символов")
    String newPassword;
}
