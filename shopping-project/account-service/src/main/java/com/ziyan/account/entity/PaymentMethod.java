package com.ziyan.account.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payment_methods")
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String methodType; // CARD, BANK, etc

    @JsonIgnore
    private String cardNumber; // Encrypted in production

    private String lastFour; // Last 4 digits only

    private String expiryMonth;

    private String expiryYear;

    @Column(nullable = false)
    private Boolean isDefault = false;
}
