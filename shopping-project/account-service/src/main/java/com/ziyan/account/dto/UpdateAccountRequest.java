package com.ziyan.account.dto;

import lombok.Data;

@Data
public class UpdateAccountRequest {
    private String email;
    private String username;
}
