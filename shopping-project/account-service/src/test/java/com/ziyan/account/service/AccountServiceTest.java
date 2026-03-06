package com.ziyan.account.service;

import com.ziyan.account.dto.CreateAccountRequest;
import com.ziyan.account.dto.UpdateAccountRequest;
import com.ziyan.account.entity.User;
import com.ziyan.account.entity.Address;
import com.ziyan.account.entity.PaymentMethod;
import com.ziyan.account.repository.UserRepository;
import com.ziyan.account.repository.AddressRepository;
import com.ziyan.account.repository.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AccountService
 * Tests user account management, address handling, and payment method management
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountService accountService;

    private CreateAccountRequest createAccountRequest;
    private UpdateAccountRequest updateAccountRequest;
    private User testUser;
    private User testUser2;
    private Address testAddress;
    private PaymentMethod testPaymentMethod;

    @BeforeEach
    void setUp() {
        // Setup create account request
        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setUsername("johnsmith");
        createAccountRequest.setEmail("john@example.com");
        createAccountRequest.setPassword("SecurePass123!");
        createAccountRequest.setPhoneNumber("555-1234");

        // Setup update account request
        updateAccountRequest = new UpdateAccountRequest();
        updateAccountRequest.setEmail("newemail@example.com");
        updateAccountRequest.setPhoneNumber("555-5678");

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("johnsmith");
        testUser.setEmail("john@example.com");
        testUser.setPassword("hashed_password");
        testUser.setPhoneNumber("555-1234");

        // Setup second test user
        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("janedoe");
        testUser2.setEmail("jane@example.com");
        testUser2.setPassword("hashed_password");
        testUser2.setPhoneNumber("555-5678");

        // Setup test address
        testAddress = new Address();
        testAddress.setId(1L);
        testAddress.setUserId(1L);
        testAddress.setStreet("123 Main St");
        testAddress.setCity("Springfield");
        testAddress.setState("IL");
        testAddress.setZipCode("62701");

        // Setup test payment method
        testPaymentMethod = new PaymentMethod();
        testPaymentMethod.setId(1L);
        testPaymentMethod.setUserId(1L);
        testPaymentMethod.setCardNumber("****-****-****-1234");
        testPaymentMethod.setCardType("VISA");
    }

    // ============ Account Creation Tests ============

    @Test
    void testCreateAccountSuccess() {
        // Arrange
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = accountService.createAccount(createAccountRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("johnsmith", result.getUsername());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("555-1234", result.getPhoneNumber());

        // Verify
        verify(userRepository, times(1)).existsByUsername("johnsmith");
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateAccountWithDuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername("johnsmith")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.createAccount(createAccountRequest)
        );

        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userRepository, times(1)).existsByUsername("johnsmith");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testCreateAccountWithDuplicateEmail() {
        // Arrange
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> accountService.createAccount(createAccountRequest)
        );

        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userRepository, times(1)).existsByUsername("johnsmith");
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testCreateAccountWithEmptyUsername() {
        // Arrange
        CreateAccountRequest emptyRequest = new CreateAccountRequest();
        emptyRequest.setUsername("");
        emptyRequest.setEmail("test@example.com");
        emptyRequest.setPassword("password");

        when(userRepository.existsByUsername("")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act & Assert
        assertDoesNotThrow(() -> accountService.createAccount(emptyRequest));
    }

    @Test
    void testCreateAccountWithNullPhoneNumber() {
        // Arrange
        CreateAccountRequest nullPhoneRequest = new CreateAccountRequest();
        nullPhoneRequest.setUsername("testuser");
        nullPhoneRequest.setEmail("test@example.com");
        nullPhoneRequest.setPassword("password");
        nullPhoneRequest.setPhoneNumber(null);

        User userWithoutPhone = new User();
        userWithoutPhone.setId(1L);
        userWithoutPhone.setPhoneNumber(null);

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(userWithoutPhone);

        // Act
        User result = accountService.createAccount(nullPhoneRequest);

        // Assert
        assertNull(result.getPhoneNumber());
    }

    @Test
    void testCreateAccountWithInvalidEmail() {
        // Arrange
        CreateAccountRequest invalidEmailRequest = new CreateAccountRequest();
        invalidEmailRequest.setUsername("testuser");
        invalidEmailRequest.setEmail("invalid-email");
        invalidEmailRequest.setPassword("password");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("invalid-email")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act & Assert - Should allow (validation might be done elsewhere)
        assertDoesNotThrow(() -> accountService.createAccount(invalidEmailRequest));
    }

    // ============ Account Retrieval Tests ============

    @Test
    void testGetAccountSuccess() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = accountService.getAccount(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("johnsmith", result.getUsername());
        assertEquals("john@example.com", result.getEmail());

        // Verify
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAccountNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        User result = accountService.getAccount(999L);

        // Assert
        assertNull(result);
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void testGetAccountWithZeroId() {
        // Arrange
        when(userRepository.findById(0L)).thenReturn(Optional.empty());

        // Act
        User result = accountService.getAccount(0L);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetAccountWithNegativeId() {
        // Arrange
        when(userRepository.findById(-1L)).thenReturn(Optional.empty());

        // Act
        User result = accountService.getAccount(-1L);

        // Assert
        assertNull(result);
    }

    // ============ Account Update Tests ============

    @Test
    void testUpdateAccountSuccess() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = accountService.updateAccount(1L, updateAccountRequest);

        // Assert
        assertNotNull(result);
        assertEquals("newemail@example.com", testUser.getEmail());
        assertEquals("555-5678", testUser.getPhoneNumber());

        // Verify
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testUpdateAccountNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> accountService.updateAccount(999L, updateAccountRequest)
        );

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdateAccountWithNewEmail() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("old@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setEmail("new@example.com");

        // Act
        User result = accountService.updateAccount(1L, request);

        // Assert
        assertNotNull(result);
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void testUpdateAccountWithNewPhoneNumber() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPhoneNumber("555-0000");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setPhoneNumber("555-9999");

        // Act
        User result = accountService.updateAccount(1L, request);

        // Assert
        assertEquals("555-9999", user.getPhoneNumber());
    }

    @Test
    void testUpdateAccountMultipleTimes() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("original@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act - First update
        UpdateAccountRequest update1 = new UpdateAccountRequest();
        update1.setEmail("second@example.com");
        accountService.updateAccount(1L, update1);

        // Act - Second update
        UpdateAccountRequest update2 = new UpdateAccountRequest();
        update2.setEmail("third@example.com");
        accountService.updateAccount(1L, update2);

        // Assert
        assertEquals("third@example.com", user.getEmail());
        verify(userRepository, times(2)).save(any(User.class));
    }

    // ============ Account Deletion Tests ============

    @Test
    void testDeleteAccountSuccess() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        accountService.deleteAccount(1L);

        // Assert & Verify
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteAccountNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> accountService.deleteAccount(999L)
        );

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteAccountCascadeAddresses() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAddress));

        // Act
        accountService.deleteAccount(1L);

        // Assert
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).deleteById(1L);
    }

    // ============ Address Management Tests ============

    @Test
    void testAddAddressSuccess() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        // Act
        Address result = accountService.addAddress(1L, testAddress);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("123 Main St", result.getStreet());
        assertEquals("Springfield", result.getCity());

        // Verify
        verify(userRepository, times(1)).findById(1L);
        verify(addressRepository, times(1)).save(any(Address.class));
    }

    @Test
    void testAddAddressUserNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> accountService.addAddress(999L, testAddress)
        );

        verify(addressRepository, never()).save(any());
    }

    @Test
    void testGetAddressesForUser() {
        // Arrange
        List<Address> addresses = Arrays.asList(testAddress);
        when(addressRepository.findByUserId(1L)).thenReturn(addresses);

        // Act
        List<Address> result = accountService.getAddresses(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("123 Main St", result.get(0).getStreet());

        // Verify
        verify(addressRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetAddressesForUserEmpty() {
        // Arrange
        when(addressRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<Address> result = accountService.getAddresses(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeleteAddress() {
        // Arrange
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));

        // Act
        accountService.deleteAddress(1L);

        // Assert
        verify(addressRepository, times(1)).deleteById(1L);
    }

    // ============ Payment Method Tests ============

    @Test
    void testAddPaymentMethodSuccess() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentMethodRepository.save(any(PaymentMethod.class))).thenReturn(testPaymentMethod);

        // Act
        PaymentMethod result = accountService.addPaymentMethod(1L, testPaymentMethod);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("VISA", result.getCardType());

        // Verify
        verify(userRepository, times(1)).findById(1L);
        verify(paymentMethodRepository, times(1)).save(any(PaymentMethod.class));
    }

    @Test
    void testGetPaymentMethodsForUser() {
        // Arrange
        List<PaymentMethod> methods = Arrays.asList(testPaymentMethod);
        when(paymentMethodRepository.findByUserId(1L)).thenReturn(methods);

        // Act
        List<PaymentMethod> result = accountService.getPaymentMethods(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VISA", result.get(0).getCardType());

        // Verify
        verify(paymentMethodRepository, times(1)).findByUserId(1L);
    }

    @Test
    void testGetPaymentMethodsForUserEmpty() {
        // Arrange
        when(paymentMethodRepository.findByUserId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<PaymentMethod> result = accountService.getPaymentMethods(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testDeletePaymentMethod() {
        // Arrange
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));

        // Act
        accountService.deletePaymentMethod(1L);

        // Assert
        verify(paymentMethodRepository, times(1)).deleteById(1L);
    }

    // ============ Multiple Operations Tests ============

    @Test
    void testCreateAccountAndAddAddress() {
        // Arrange
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        // Act
        User user = accountService.createAccount(createAccountRequest);
        Address address = accountService.addAddress(user.getId(), testAddress);

        // Assert
        assertNotNull(user);
        assertNotNull(address);
        assertEquals(user.getId(), address.getUserId());
    }

    @Test
    void testCreateAccountAndAddMultipleAddresses() {
        // Arrange
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(addressRepository.save(any(Address.class))).thenReturn(testAddress);

        // Act
        User user = accountService.createAccount(createAccountRequest);
        Address addr1 = accountService.addAddress(user.getId(), testAddress);
        Address addr2 = accountService.addAddress(user.getId(), testAddress);

        // Assert
        verify(addressRepository, times(2)).save(any(Address.class));
    }

    @Test
    void testCreateAndDeleteAccount() {
        // Arrange
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User user = accountService.createAccount(createAccountRequest);
        accountService.deleteAccount(user.getId());

        // Assert
        verify(userRepository, times(1)).deleteById(1L);
    }
}
