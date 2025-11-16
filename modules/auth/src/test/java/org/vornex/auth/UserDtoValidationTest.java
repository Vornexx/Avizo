package org.vornex.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.vornex.auth.dto.UserLoginDto;
import org.vornex.auth.dto.UserRegistrationDto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for bean validation on DTOs.
 * <p>
 * Цель: показать как работает javax.validation (hibernate-validator),
 * какие правила срабатывают и как проверять сообщения об ошибках.
 */
public class UserDtoValidationTest {

    private static ValidatorFactory vf;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        // Стандартный способ получить validator
        vf = Validation.buildDefaultValidatorFactory();
        validator = vf.getValidator();
    }

    @AfterAll
    static void close() {
        if (vf != null) {
            vf.close();
        }
    }

    @Test
    void userLoginDto_whenNoEmailAndNoPhone_thenConstraintViolation() {
        // Arrange: создаём DTO, который нарушает условие isEmailOrPhoneProvided()
        UserLoginDto dto = UserLoginDto.builder()
                .email(null)
                .phoneNumber(null)
                .password("validPassword123")
                .build();

        // Act: валидируем
        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto); //если хотябы 1 нарушение валидации создается ConstraintViolation с текстами ошибок (и не только)

        // Assert: ожидаем, что есть как минимум одно нарушение от @AssertTrue
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .anyMatch(msg -> msg.contains("Either email or phone must be provided"));
    }

    @Test
    void userLoginDto_whenInvalidEmail_thenViolation() {
        UserLoginDto dto = UserLoginDto.builder()
                .email("not-an-email")
                .password("password")
                .build();

        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        // ожидаем сообщение про неверный формат email
        assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .anyMatch(msg -> msg.contains("Invalid email format"));
    }

    @Test
    void userLoginDto_whenValidEmailAndPassword_thenNoViolations() {
        UserLoginDto dto = UserLoginDto.builder()
                .email("user@example.com")
                .password("strongPassword1")
                .build();

        Set<ConstraintViolation<UserLoginDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    // Аналогично для UserRegistrationDto — проверим trim/size/agreeTerms
    @Test
    void userRegistrationDto_whenMissingAgreeTerms_thenViolation() {
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .username("user")
                .firstName("First")
                .lastName("Last")
                .email("user@example.com")
                .password("password123")
                .agreeTerms(false) // не согласен — должно быть @AssertTrue
                .build();

        Set<ConstraintViolation<UserRegistrationDto>> violations = validator.validate(dto);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().map(ConstraintViolation::getMessage))
                .anyMatch(msg -> msg.contains("You must agree to terms"));
    }
}