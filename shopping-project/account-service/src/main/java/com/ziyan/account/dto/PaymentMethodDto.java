package com.ziyan.account.dto;

import lombok.Data;

@Data
public class PaymentMethodDto {
    private Long id;
    private String methodType; // CARD, BANK
    private String cardNumber; // Only last 4 digits visible
    private String lastFour;
    private String expiryMonth;
    private String expiryYear;
    private Boolean isDefault;
}
