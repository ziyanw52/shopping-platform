package com.ziyan.account.dto;

import lombok.Data;

@Data
public class CreateAccountRequest {
    private String username;
    private String email;
    private String password;
}
