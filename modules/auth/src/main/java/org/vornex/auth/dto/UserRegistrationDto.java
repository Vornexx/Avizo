package org.vornex.auth.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @Email(message = "Email must be valid")
    @Size(max = 100)
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be valid")
    private String phoneNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100)
    private String password;

    @AssertTrue(message = "You must agree to terms")
    private boolean agreeTerms;

    @AssertTrue(message = "Either email or phone must be provided")
    public boolean isEmailOrPhoneProvided() {
        return (email != null && !email.isBlank()) || (phoneNumber != null && !phoneNumber.isBlank());
    }
}
