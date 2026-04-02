package com.ziyan.account.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpdateAccountRequest {
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private List<AddressDto> addresses;
    private List<PaymentMethodDto> paymentMethods;
}
