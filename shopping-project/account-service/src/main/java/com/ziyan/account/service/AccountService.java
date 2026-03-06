package com.ziyan.account.service;

import com.ziyan.account.dto.AccountResponse;
import com.ziyan.account.dto.CreateAccountRequest;
import com.ziyan.account.dto.UpdateAccountRequest;
import com.ziyan.account.entity.Address;
import com.ziyan.account.entity.PaymentMethod;
import com.ziyan.account.entity.User;
import com.ziyan.account.repository.AddressRepository;
import com.ziyan.account.repository.PaymentMethodRepository;
import com.ziyan.account.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);
        log.info("Account created for user: {}", savedUser.getUsername());

        // Add addresses if provided
        if (request.getAddresses() != null && !request.getAddresses().isEmpty()) {
            request.getAddresses().forEach(addressDto -> {
                Address address = new Address();
                address.setUser(savedUser);
                address.setType(Address.AddressType.valueOf(addressDto.getType().toUpperCase()));
                address.setStreet(addressDto.getStreet());
                address.setCity(addressDto.getCity());
                address.setState(addressDto.getState());
                address.setPostalCode(addressDto.getPostalCode());
                address.setCountry(addressDto.getCountry());
                savedUser.getAddresses().add(address);
            });
        }

        // Add payment methods if provided
        if (request.getPaymentMethods() != null && !request.getPaymentMethods().isEmpty()) {
            request.getPaymentMethods().forEach(pmDto -> {
                PaymentMethod pm = new PaymentMethod();
                pm.setUser(savedUser);
                pm.setMethodType(pmDto.getMethodType());
                pm.setCardNumber(pmDto.getCardNumber());
                pm.setLastFour(pmDto.getLastFour());
                pm.setExpiryMonth(pmDto.getExpiryMonth());
                pm.setExpiryYear(pmDto.getExpiryYear());
                pm.setIsDefault(pmDto.getIsDefault() != null ? pmDto.getIsDefault() : false);
                savedUser.getPaymentMethods().add(pm);
            });
        }

        // Save user with all related entities via cascade
        User finalUser = userRepository.save(savedUser);
        return convertToResponse(finalUser);
    }

    public AccountResponse getAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        log.info("Account lookup for user: {}", user.getUsername());
        return convertToResponse(user);
    }

    @Transactional
    public AccountResponse updateAccount(Long userId, UpdateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if new email is unique (if changed)
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Check if new username is unique (if changed)
        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());

        // Update addresses if provided
        if (request.getAddresses() != null && !request.getAddresses().isEmpty()) {
            // Clear existing addresses
            addressRepository.deleteAll(user.getAddresses());
            
            // Add new addresses
            request.getAddresses().forEach(addressDto -> {
                Address address = new Address();
                address.setUser(user);
                address.setType(Address.AddressType.valueOf(addressDto.getType().toUpperCase()));
                address.setStreet(addressDto.getStreet());
                address.setCity(addressDto.getCity());
                address.setState(addressDto.getState());
                address.setPostalCode(addressDto.getPostalCode());
                address.setCountry(addressDto.getCountry());
                addressRepository.save(address);
            });
        }

        // Update payment methods if provided
        if (request.getPaymentMethods() != null && !request.getPaymentMethods().isEmpty()) {
            // Clear existing payment methods
            paymentMethodRepository.deleteAll(user.getPaymentMethods());
            
            // Add new payment methods
            request.getPaymentMethods().forEach(pmDto -> {
                PaymentMethod pm = new PaymentMethod();
                pm.setUser(user);
                pm.setMethodType(pmDto.getMethodType());
                pm.setCardNumber(pmDto.getCardNumber());
                pm.setLastFour(pmDto.getLastFour());
                pm.setExpiryMonth(pmDto.getExpiryMonth());
                pm.setExpiryYear(pmDto.getExpiryYear());
                pm.setIsDefault(pmDto.getIsDefault() != null ? pmDto.getIsDefault() : false);
                paymentMethodRepository.save(pm);
            });
        }

        User updatedUser = userRepository.save(user);
        log.info("Account updated for user: {}", updatedUser.getUsername());

        // Refresh user with latest data
        User refreshedUser = userRepository.findById(updatedUser.getId()).orElse(updatedUser);
        return convertToResponse(refreshedUser);
    }

    private AccountResponse convertToResponse(User user) {
        AccountResponse response = new AccountResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAddresses(user.getAddresses());
        response.setPaymentMethods(user.getPaymentMethods());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}

