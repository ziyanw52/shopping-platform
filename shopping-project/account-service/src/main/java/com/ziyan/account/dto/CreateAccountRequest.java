package com.ziyan.account.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateAccountRequest {
    private String username;
    private String email;
    private String password;
    private List<AddressDto> addresses;
    private List<PaymentMethodDto> paymentMethods;
}
