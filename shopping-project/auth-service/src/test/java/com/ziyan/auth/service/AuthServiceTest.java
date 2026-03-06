package com.ziyan.auth.service;

import com.ziyan.auth.dto.LoginRequest;
import com.ziyan.auth.dto.LoginResponse;
import com.ziyan.auth.dto.RegisterRequest;
import com.ziyan.auth.entity.User;
import com.ziyan.auth.repository.UserRepository;
import com.ziyan.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * Tests registration, login, and token generation functionality
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Set JWT expiration
        ReflectionTestUtils.setField(authService, "jwtExpiration", 86400000L);

        // Setup test data
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
    }

    // ============ Registration Tests ============

    @Test
    void testRegisterSuccess() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("jwt-token");

        // Act
        LoginResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());

        // Verify
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtTokenProvider, times(1)).generateToken(1L, "testuser");
    }

    @Test
    void testRegisterWithDuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.register(registerRequest)
        );

        assertTrue(exception.getMessage().contains("Username already exists"));
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterWithDuplicateEmail() {
        // Arrange
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.register(registerRequest)
        );

        assertTrue(exception.getMessage().contains("Email already exists"));
        verify(userRepository, times(1)).existsByUsername("testuser");
        verify(userRepository, times(1)).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegisterWithEmptyUsername() {
        // Arrange
        registerRequest.setUsername("");
        User emptyUserNameUser = new User();
        emptyUserNameUser.setId(1L);
        emptyUserNameUser.setUsername("");
        emptyUserNameUser.setEmail("test@example.com");

        when(userRepository.existsByUsername("")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(emptyUserNameUser);
        when(jwtTokenProvider.generateToken(1L, "")).thenReturn("jwt-token");

        // Act
        LoginResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("", response.getUsername());
    }

    @Test
    void testRegisterWithNullPassword() {
        // Arrange
        registerRequest.setPassword(null);
        User userWithNullPass = new User();
        userWithNullPass.setId(1L);
        userWithNullPass.setUsername("testuser");
        userWithNullPass.setPassword("hashedpassword");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(null)).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(userWithNullPass);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("jwt-token");

        // Act
        LoginResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
    }

    // ============ Login Tests ============

    @Test
    void testLoginSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("jwt-token");

        // Act
        LoginResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertEquals("testuser", response.getUsername());
        assertEquals("jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());

        // Verify
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password123", "hashedpassword");
        verify(jwtTokenProvider, times(1)).generateToken(1L, "testuser");
    }

    @Test
    void testLoginWithInvalidUsername() {
        // Arrange
        when(userRepository.findByUsername("invaliduser")).thenReturn(Optional.empty());

        // Act & Assert
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("invaliduser");
        invalidRequest.setPassword("password123");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid username or password"));
        verify(userRepository, times(1)).findByUsername("invaliduser");
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void testLoginWithInvalidPassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        // Act & Assert
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setPassword("wrongpassword");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid username or password"));
        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("wrongpassword", "hashedpassword");
    }

    @Test
    void testLoginWithEmptyPassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("", "hashedpassword")).thenReturn(false);

        // Act & Assert
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername("testuser");
        invalidRequest.setPassword("");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid username or password"));
    }

    @Test
    void testLoginWithNullUsername() {
        // Arrange
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        // Act & Assert
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername(null);
        invalidRequest.setPassword("password123");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> authService.login(invalidRequest)
        );

        assertTrue(exception.getMessage().contains("Invalid username or password"));
    }

    @Test
    void testMultipleRegistrationsWithDifferentUsers() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyLong(), anyString())).thenReturn("jwt-token");

        // Act
        RegisterRequest req1 = new RegisterRequest();
        req1.setUsername("user1");
        req1.setEmail("user1@example.com");
        req1.setPassword("pass1");

        RegisterRequest req2 = new RegisterRequest();
        req2.setUsername("user2");
        req2.setEmail("user2@example.com");
        req2.setPassword("pass2");

        LoginResponse response1 = authService.register(req1);
        LoginResponse response2 = authService.register(req2);

        // Assert
        assertNotNull(response1);
        assertNotNull(response2);
        assertEquals("jwt-token", response1.getToken());
        assertEquals("jwt-token", response2.getToken());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void testLoginAfterSuccessfulRegistration() {
        // Arrange - Register
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(1L, "testuser")).thenReturn("jwt-token");

        // Act - Register
        LoginResponse registerResponse = authService.register(registerRequest);
        assertNotNull(registerResponse);

        // Arrange - Login
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "hashedpassword")).thenReturn(true);

        // Act - Login
        LoginResponse loginResponse = authService.login(loginRequest);

        // Assert
        assertNotNull(loginResponse);
        assertEquals(registerResponse.getUserId(), loginResponse.getUserId());
        assertEquals(registerResponse.getUsername(), loginResponse.getUsername());
    }
}
