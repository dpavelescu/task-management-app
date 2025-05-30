package com.taskapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    // Explicit getters in case Lombok is not working
    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
