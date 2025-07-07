package org.vornex.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEmailDto {
    @Email(message = "Некорректный формат email")
    @NotBlank(message = "Email не может быть пустым")
    String newEmail;
}
