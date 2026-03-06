package com.example.demo.shortenurl.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private Long userId;
    private String message;
    private boolean success;

    public LoginResponse(String token, String email, Long userId) {
        this.token = token;
        this.email = email;
        this.userId = userId;
        this.success = true;
        this.message = "Login successful";
    }

    public static LoginResponse success(String token, String email, Long userId) {
        return new LoginResponse(token, email, userId);
    }

    public static LoginResponse failure(String message) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
