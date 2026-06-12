package com.enterprise.payment.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank @Size(min=3, max=50)
    private String username;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    @Size(min=8)
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;
}

