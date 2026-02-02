package com.javaedu.security;

import com.javaedu.config.JwtConfig;
import com.javaedu.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        jwtConfig.setSecret("this-is-a-very-long-secret-key-for-testing-purposes-at-least-32-chars");
        jwtConfig.setExpiration(3600000); // 1 hour
        jwtConfig.setRefreshExpiration(604800000); // 7 days

        tokenProvider = new JwtTokenProvider(jwtConfig);
        tokenProvider.init();
    }

    @Test
    void generateToken_CreatesValidToken() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.STUDENT)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();

        String token = tokenProvider.generateToken(userPrincipal);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void getUserIdFromToken_ReturnsCorrectId() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(42L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.TEACHER)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_TEACHER")))
                .build();

        String token = tokenProvider.generateToken(userPrincipal);
        Long userId = tokenProvider.getUserIdFromToken(token);

        assertEquals(42L, userId);
    }

    @Test
    void getEmailFromToken_ReturnsCorrectEmail() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("myemail@example.com")
                .name("Test User")
                .role(User.Role.STUDENT)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();

        String token = tokenProvider.generateToken(userPrincipal);
        String email = tokenProvider.getEmailFromToken(token);

        assertEquals("myemail@example.com", email);
    }

    @Test
    void validateToken_WithValidToken_ReturnsTrue() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.STUDENT)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();

        String token = tokenProvider.generateToken(userPrincipal);

        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_WithInvalidToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_WithEmptyToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateToken_WithNullToken_ReturnsFalse() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void generateRefreshToken_CreatesValidToken() {
        String refreshToken = tokenProvider.generateRefreshToken(1L);

        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(tokenProvider.validateToken(refreshToken));
        assertTrue(tokenProvider.isRefreshToken(refreshToken));
    }

    @Test
    void isRefreshToken_WithRefreshToken_ReturnsTrue() {
        String refreshToken = tokenProvider.generateRefreshToken(1L);

        assertTrue(tokenProvider.isRefreshToken(refreshToken));
    }

    @Test
    void isRefreshToken_WithAccessToken_ReturnsFalse() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .role(User.Role.STUDENT)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .build();

        String accessToken = tokenProvider.generateToken(userPrincipal);

        assertFalse(tokenProvider.isRefreshToken(accessToken));
    }
}
