package com.ziyan.account.controller;

import com.ziyan.account.dto.AccountResponse;
import com.ziyan.account.dto.CreateAccountRequest;
import com.ziyan.account.dto.UpdateAccountRequest;
import com.ziyan.account.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@RequestBody CreateAccountRequest request) {
        try {
            log.info("Creating account for username: {}", request.getUsername());
            AccountResponse response = accountService.createAccount(request);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Account creation failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long id) {
        try {
            log.info("Fetching account with id: {}", id);
            AccountResponse response = accountService.getAccount(id);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Account lookup failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable Long id,
            @RequestBody UpdateAccountRequest request) {
        try {
            log.info("Updating account with id: {}", id);
            AccountResponse response = accountService.updateAccount(id, request);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Account update failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{id}/addresses")
    public ResponseEntity<AccountResponse> addAddress(
            @PathVariable Long id,
            @RequestBody com.ziyan.account.entity.Address address) {
        try {
            log.info("Adding address for account: {}", id);
            AccountResponse response = accountService.addAddress(id, address);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Address creation failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @PathVariable Long addressId) {
        try {
            log.info("Deleting address {} for account: {}", addressId, id);
            accountService.deleteAddress(id, addressId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            log.warn("Address deletion failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{id}/payment-methods")
    public ResponseEntity<AccountResponse> addPaymentMethod(
            @PathVariable Long id,
            @RequestBody com.ziyan.account.entity.PaymentMethod paymentMethod) {
        try {
            log.info("Adding payment method for account: {}", id);
            AccountResponse response = accountService.addPaymentMethod(id, paymentMethod);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            log.warn("Payment method creation failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}/payment-methods/{paymentMethodId}")
    public ResponseEntity<Void> deletePaymentMethod(
            @PathVariable Long id,
            @PathVariable Long paymentMethodId) {
        try {
            log.info("Deleting payment method {} for account: {}", paymentMethodId, id);
            accountService.deletePaymentMethod(id, paymentMethodId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException e) {
            log.warn("Payment method deletion failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
