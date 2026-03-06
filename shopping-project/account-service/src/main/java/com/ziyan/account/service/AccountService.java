package com.ziyan.account.service;

import com.ziyan.account.dto.AccountResponse;
import com.ziyan.account.dto.CreateAccountRequest;
import com.ziyan.account.dto.UpdateAccountRequest;
import com.ziyan.account.entity.User;
import com.ziyan.account.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AccountService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

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

        return convertToResponse(savedUser);
    }

    public AccountResponse getAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        log.info("Account lookup for user: {}", user.getUsername());
        return convertToResponse(user);
    }

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

        User updatedUser = userRepository.save(user);
        log.info("Account updated for user: {}", updatedUser.getUsername());

        return convertToResponse(updatedUser);
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
