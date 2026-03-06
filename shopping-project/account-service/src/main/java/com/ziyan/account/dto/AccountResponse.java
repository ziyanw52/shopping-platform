package com.ziyan.account.dto;

import com.ziyan.account.entity.Address;
import com.ziyan.account.entity.PaymentMethod;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AccountResponse {
    private Long id;
    private String username;
    private String email;
    private List<Address> addresses;
    private List<PaymentMethod> paymentMethods;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
