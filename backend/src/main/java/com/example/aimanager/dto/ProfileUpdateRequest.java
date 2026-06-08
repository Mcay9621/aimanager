package com.example.aimanager.dto;

import jakarta.validation.constraints.Email;

public class ProfileUpdateRequest {
    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
