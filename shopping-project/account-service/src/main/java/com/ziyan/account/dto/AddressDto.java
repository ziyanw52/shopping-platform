package com.ziyan.account.dto;

import lombok.Data;

@Data
public class AddressDto {
    private Long id;
    private String type; // SHIPPING, BILLING
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
