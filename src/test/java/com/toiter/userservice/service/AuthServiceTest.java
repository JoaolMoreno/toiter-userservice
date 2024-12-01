package com.toiter.userservice.service;

import com.toiter.userservice.entity.User;
import com.toiter.userservice.model.LoginRequest;
import com.toiter.userservice.model.TokenResponse;
import com.toiter.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<String> usernameCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Test for successful authentication with username
    @Test
    void testAuthenticateAndGenerateTokens_WithValidUsername() {
        LoginRequest loginRequest = new LoginRequest("validUsername", "validPassword");
        User user = new User();
        user.setUsername("validUsername");
        user.setPassword("encodedPassword");
        user.setId(1L);

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(loginRequest.getUsernameOrEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getUsername(), user.getId())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user.getUsername(), user.getId())).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        TokenResponse tokenResponse = authService.authenticateAndGenerateTokens(loginRequest);

        assertNotNull(tokenResponse);
        assertEquals("accessToken", tokenResponse.getAccessToken());
        assertEquals("refreshToken", tokenResponse.getRefreshToken());
        assertEquals(3600L, tokenResponse.getExpiresIn());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(userRepository).findByUsername(loginRequest.getUsernameOrEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verify(jwtService).generateToken(user.getUsername(), user.getId());
        verify(jwtService).generateRefreshToken(user.getUsername(), user.getId());
    }

    // Test for successful authentication with email
    @Test
    void testAuthenticateAndGenerateTokens_WithValidEmail() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "validPassword");
        User user = new User();
        user.setUsername("validUsername");
        user.setEmail("user@example.com");
        user.setPassword("encodedPassword");
        user.setId(1L);

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user.getUsername(), user.getId())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user.getUsername(), user.getId())).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        TokenResponse tokenResponse = authService.authenticateAndGenerateTokens(loginRequest);

        assertNotNull(tokenResponse);
        assertEquals("accessToken", tokenResponse.getAccessToken());
        assertEquals("refreshToken", tokenResponse.getRefreshToken());
        assertEquals(3600L, tokenResponse.getExpiresIn());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verify(jwtService).generateToken(user.getUsername(), user.getId());
        verify(jwtService).generateRefreshToken(user.getUsername(), user.getId());
    }

    @Test
    void testAuthenticateAndGenerateTokens_NullLoginRequest() {
        assertThrows(NullPointerException.class, () ->
                authService.authenticateAndGenerateTokens(null));
    }

    // Test for invalid username or email (user not found)
    @Test
    void testAuthenticateAndGenerateTokens_UserNotFound() {
        LoginRequest loginRequest = new LoginRequest("nonExistentUser", "password");

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(loginRequest.getUsernameOrEmail())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.authenticateAndGenerateTokens(loginRequest));

        assertEquals("Invalid username or email", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(userRepository).findByUsername(loginRequest.getUsernameOrEmail());
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testAuthenticateAndGenerateTokens_NullUsernameOrEmail() {
        LoginRequest loginRequest = new LoginRequest(null, "password");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.authenticateAndGenerateTokens(loginRequest));

        assertEquals("Invalid username or email", exception.getMessage());
    }

    // Test for invalid password
    @Test
    void testAuthenticateAndGenerateTokens_InvalidPassword() {
        LoginRequest loginRequest = new LoginRequest("validUsername", "invalidPassword");
        User user = new User();
        user.setUsername("validUsername");
        user.setPassword("encodedPassword");
        user.setId(1L);

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(loginRequest.getUsernameOrEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.authenticateAndGenerateTokens(loginRequest));

        assertEquals("Invalid password", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(userRepository).findByUsername(loginRequest.getUsernameOrEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verifyNoInteractions(jwtService);
    }

    @Test
    void testAuthenticateAndGenerateTokens_NullPassword() {
        LoginRequest loginRequest = new LoginRequest("validUsername", null);
        User user = new User();
        user.setUsername("validUsername");
        user.setPassword("encodedPassword");
        user.setId(1L);

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(loginRequest.getUsernameOrEmail())).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.authenticateAndGenerateTokens(loginRequest));

        // Optionally check the exception message
         assertEquals("Invalid password", exception.getMessage());
    }

    // Test getUserIdFromAuthentication with valid authentication
    @Test
    void testGetUserIdFromAuthentication() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(1L);

        Long userId = authService.getUserIdFromAuthentication(authentication);

        assertNotNull(userId);
        assertEquals(1L, userId);
    }

    @Test
    void testGetUserIdFromAuthentication_PrincipalNotLong() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("notALong");

        assertThrows(ClassCastException.class, () ->
                authService.getUserIdFromAuthentication(authentication));
    }

    @Test
    void testGetUserIdFromAuthentication_PrincipalIsNull() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.getUserIdFromAuthentication(authentication));

        assertEquals("Principal cannot be null", exception.getMessage());
    }

    // Test refreshTokens with valid refresh token
    @Test
    void testRefreshTokens_WithValidRefreshToken() {
        TokenResponse oldTokenResponse = new TokenResponse("oldAccessToken", "validRefreshToken", 3600L);
        String username = "validUsername";
        Long userId = 1L;

        when(jwtService.extractUsername(oldTokenResponse.getAccessToken())).thenReturn(username);
        when(jwtService.extractUserId(oldTokenResponse.getAccessToken())).thenReturn(userId);
        when(jwtService.isTokenValid(oldTokenResponse.getRefreshToken(), username)).thenReturn(true);
        when(jwtService.generateToken(username, userId)).thenReturn("newAccessToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(3600L);

        TokenResponse newTokenResponse = authService.refreshTokens(oldTokenResponse);

        assertNotNull(newTokenResponse);
        assertEquals("newAccessToken", newTokenResponse.getAccessToken());
        assertEquals("validRefreshToken", newTokenResponse.getRefreshToken());
        assertEquals(3600L, newTokenResponse.getExpiresIn());

        verify(jwtService).extractUsername(oldTokenResponse.getAccessToken());
        verify(jwtService).extractUserId(oldTokenResponse.getAccessToken());
        verify(jwtService).isTokenValid(oldTokenResponse.getRefreshToken(), username);
        verify(jwtService).generateToken(username, userId);
    }

    @Test
    void testRefreshTokens_NullTokenResponse() {
        assertThrows(NullPointerException.class, () ->
                authService.refreshTokens(null));
    }

    @Test
    void testRefreshTokens_NullRefreshToken() {
        TokenResponse tokenResponse = new TokenResponse("accessToken", null, 3600L);
        String username = "validUsername";
        Long userId = 1L;

        when(jwtService.extractUsername(tokenResponse.getAccessToken())).thenReturn(username);
        when(jwtService.extractUserId(tokenResponse.getAccessToken())).thenReturn(userId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.refreshTokens(tokenResponse));

        assertEquals("Invalid refresh token", exception.getMessage());
    }

    // Test refreshTokens with invalid refresh token
    @Test
    void testRefreshTokens_WithInvalidRefreshToken() {
        TokenResponse oldTokenResponse = new TokenResponse("oldAccessToken", "invalidRefreshToken", 3600L);
        String username = "validUsername";
        Long userId = 1L;

        // Mock the methods of jwtService
        when(jwtService.extractUsername(oldTokenResponse.getAccessToken())).thenReturn(username);
        when(jwtService.extractUserId(oldTokenResponse.getAccessToken())).thenReturn(userId);
        when(jwtService.isTokenValid(oldTokenResponse.getRefreshToken(), username)).thenReturn(false);

        // Execute the method under test and assert the exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.refreshTokens(oldTokenResponse));

        assertEquals("Invalid refresh token", exception.getMessage());

        // Verify the interactions with jwtService
        verify(jwtService).extractUsername(oldTokenResponse.getAccessToken());
        verify(jwtService).extractUserId(oldTokenResponse.getAccessToken());
        verify(jwtService).isTokenValid(oldTokenResponse.getRefreshToken(), username);
        verifyNoMoreInteractions(jwtService);
    }

    // Test authenticateAndGenerateTokens when password encoder throws an exception
    @Test
    void testAuthenticateAndGenerateTokens_PasswordEncoderThrowsException() {
        LoginRequest loginRequest = new LoginRequest("validUsername", "password");
        User user = new User();
        user.setUsername("validUsername");
        user.setPassword("encodedPassword");
        user.setId(1L);

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail())).thenReturn(Optional.empty());
        when(userRepository.findByUsername(loginRequest.getUsernameOrEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
                .thenThrow(new RuntimeException("Encoder failure"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.authenticateAndGenerateTokens(loginRequest));

        assertEquals("Encoder failure", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verify(userRepository).findByUsername(loginRequest.getUsernameOrEmail());
        verify(passwordEncoder).matches(loginRequest.getPassword(), user.getPassword());
        verifyNoInteractions(jwtService);
    }

    // Test getUserIdFromAuthentication with null authentication
    @Test
    void testGetUserIdFromAuthentication_NullAuthentication() {
        Authentication authentication = null;

        assertThrows(NullPointerException.class, () ->
                authService.getUserIdFromAuthentication(authentication));
    }

    // Test refreshTokens when extractUsername throws an exception
    @Test
    void testRefreshTokens_ExtractUsernameThrowsException() {
        TokenResponse oldTokenResponse = new TokenResponse("invalidAccessToken", "refreshToken", 3600L);

        when(jwtService.extractUsername(oldTokenResponse.getAccessToken()))
                .thenThrow(new IllegalArgumentException("Invalid token"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authService.refreshTokens(oldTokenResponse));

        assertEquals("Invalid token", exception.getMessage());

        verify(jwtService).extractUsername(oldTokenResponse.getAccessToken());
        verifyNoMoreInteractions(jwtService);
    }

    // Test authenticateAndGenerateTokens when userRepository throws an exception
    @Test
    void testAuthenticateAndGenerateTokens_UserRepositoryThrowsException() {
        LoginRequest loginRequest = new LoginRequest("validUsername", "password");

        when(userRepository.findByEmail(loginRequest.getUsernameOrEmail()))
                .thenThrow(new RuntimeException("Database error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.authenticateAndGenerateTokens(loginRequest));

        assertEquals("Database error", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getUsernameOrEmail());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }

    @Test
    void testRefreshTokens_GenerateTokenThrowsException() {
        TokenResponse tokenResponse = new TokenResponse("accessToken", "refreshToken", 3600L);
        String username = "validUsername";
        Long userId = 1L;

        when(jwtService.extractUsername(tokenResponse.getAccessToken())).thenReturn(username);
        when(jwtService.extractUserId(tokenResponse.getAccessToken())).thenReturn(userId);
        when(jwtService.isTokenValid(tokenResponse.getRefreshToken(), username)).thenReturn(true);
        when(jwtService.generateToken(username, userId))
                .thenThrow(new RuntimeException("Token generation failed"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.refreshTokens(tokenResponse));

        assertEquals("Token generation failed", exception.getMessage());
    }
}
