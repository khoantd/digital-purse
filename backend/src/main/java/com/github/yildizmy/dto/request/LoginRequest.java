package com.github.yildizmy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.github.yildizmy.common.Constants.PASSWORD_MAX_LENGTH;

/**
 * Data Transfer Object for Login request.
 * Password length is not raised here so existing seed accounts can still authenticate;
 * signup enforces the stronger policy (SEC-13).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @Size(min = 3, max = 20, message = "{validation.user.username.length}")
    @NotBlank(message = "{validation.user.username.required}")
    private String username;

    @Size(max = PASSWORD_MAX_LENGTH, message = "{validation.user.password.length}")
    @NotBlank(message = "{validation.user.password.required}")
    private String password;
}
