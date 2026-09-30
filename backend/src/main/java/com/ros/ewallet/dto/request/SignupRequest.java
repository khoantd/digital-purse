package com.ros.ewallet.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ros.ewallet.validator.NotCommonPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.ros.ewallet.common.Constants.PASSWORD_MAX_LENGTH;
import static com.ros.ewallet.common.Constants.PASSWORD_MIN_LENGTH;

/**
 * Data Transfer Object for signup request.
 * Roles are not client-controlled (SEC-01); signup always assigns ROLE_USER server-side.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignupRequest {

    private Long id;

    @Size(min = 3, max = 50, message = "{validation.user.firstname.length}")
    @NotBlank(message = "{validation.user.firstname.required}")
    private String firstName;

    @Size(min = 3, max = 50, message = "{validation.user.lastname.length}")
    @NotBlank(message = "{validation.user.lastname.required}")
    private String lastName;

    @Size(min = 3, max = 20, message = "{validation.user.username.length}")
    @NotBlank(message = "{validation.user.username.required}")
    private String username;

    @Email(message = "{validation.user.email.format}")
    @Size(min = 6, max = 50, message = "{validation.user.email.length}")
    @NotBlank(message = "{validation.user.email.required}")
    private String email;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH, message = "{validation.user.password.length}")
    @NotBlank(message = "{validation.user.password.required}")
    @NotCommonPassword
    private String password;
}
