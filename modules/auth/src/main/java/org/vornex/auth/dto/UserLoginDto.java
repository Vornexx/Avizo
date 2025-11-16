package org.vornex.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginDto {

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "\\+?[0-9]{10,15}$", message = "Invalid phone number")
    private String phoneNumber;

    @NotBlank(message = "Password must not be blank")
    private String password;

    @AssertTrue(message = "Either email or phone must be provided") //метод сам вызовется spring validation
    public boolean isEmailOrPhoneProvided() {
        return (email != null && !email.isBlank()) || (phoneNumber != null && !phoneNumber.isBlank());
    }
}
