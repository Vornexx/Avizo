package org.vornex.userapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record UserAccountDto(
    UUID id,
    String username,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password,
    boolean agreeTerms
) {}
