package com.javaedu.controller;

import com.javaedu.model.User;
import com.javaedu.repository.UserRepository;
import com.javaedu.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
@Tag(name = "Google OAuth", description = "Google OAuth integration endpoints")
@Slf4j
public class GoogleOAuthController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri:http://localhost:5173/auth/google/callback}")
    private String redirectUri;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private static final String[] REQUIRED_SCOPES = {
            "email",
            "profile",
            "https://www.googleapis.com/auth/classroom.courses.readonly",
            "https://www.googleapis.com/auth/classroom.rosters.readonly",
            "https://www.googleapis.com/auth/classroom.coursework.students"
    };

    @GetMapping
    @Operation(summary = "Get Google OAuth authorization URL")
    public ResponseEntity<?> getAuthUrl() {
        // Check if Google OAuth is configured
        if (clientId == null || clientId.isBlank()) {
            log.warn("Google OAuth client ID is not configured");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Google OAuth not configured",
                "message", "Google Classroom integration requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables to be set."
            ));
        }

        String scope = String.join(" ", REQUIRED_SCOPES);

        String authUrl = UriComponentsBuilder.fromHttpUrl(GOOGLE_AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scope)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toUriString();

        return ResponseEntity.ok(new GoogleAuthUrlResponse(authUrl));
    }

    @PostMapping("/callback")
    @Operation(summary = "Handle Google OAuth callback with authorization code")
    public ResponseEntity<GoogleConnectionResponse> handleCallback(@RequestBody GoogleCallbackRequest request) {
        User currentUser = authService.getCurrentUser();

        try {
            // Exchange authorization code for tokens
            TokenResponse tokens = exchangeCodeForTokens(request.getCode());

            // Verify the token and get user info
            GoogleUserInfo userInfo = getUserInfo(tokens.accessToken());

            // Update user with Google tokens
            currentUser.setGoogleId(userInfo.id());
            currentUser.setGoogleRefreshToken(tokens.refreshToken());
            currentUser.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(tokens.expiresIn()));
            currentUser.setGoogleConnectedAt(LocalDateTime.now());
            userRepository.save(currentUser);

            log.info("User {} connected Google account: {}", currentUser.getId(), userInfo.email());

            return ResponseEntity.ok(new GoogleConnectionResponse(
                    true,
                    userInfo.email(),
                    currentUser.getGoogleConnectedAt(),
                    tokens.accessToken()
            ));
        } catch (Exception e) {
            log.error("Failed to connect Google account for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(new GoogleConnectionResponse(
                    false, null, null, null
            ));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "Check Google connection status")
    public ResponseEntity<GoogleStatusResponse> getStatus() {
        User currentUser = authService.getCurrentUser();

        boolean isConnected = currentUser.getGoogleRefreshToken() != null;
        String email = null;

        if (isConnected && currentUser.getGoogleId() != null) {
            // Try to get email from stored info or refresh token
            try {
                String accessToken = refreshAccessToken(currentUser);
                if (accessToken != null) {
                    GoogleUserInfo userInfo = getUserInfo(accessToken);
                    email = userInfo.email();
                }
            } catch (Exception e) {
                log.warn("Could not fetch Google email for user {}", currentUser.getId());
            }
        }

        return ResponseEntity.ok(new GoogleStatusResponse(
                isConnected,
                email,
                currentUser.getGoogleConnectedAt()
        ));
    }

    @PostMapping("/disconnect")
    @Operation(summary = "Disconnect Google account")
    public ResponseEntity<Void> disconnect() {
        User currentUser = authService.getCurrentUser();

        // Revoke the token if possible
        if (currentUser.getGoogleRefreshToken() != null) {
            try {
                String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + currentUser.getGoogleRefreshToken();
                restTemplate.postForEntity(revokeUrl, null, String.class);
            } catch (Exception e) {
                log.warn("Could not revoke Google token: {}", e.getMessage());
            }
        }

        // Clear Google OAuth data
        currentUser.setGoogleRefreshToken(null);
        currentUser.setGoogleTokenExpiry(null);
        currentUser.setGoogleConnectedAt(null);
        userRepository.save(currentUser);

        log.info("User {} disconnected Google account", currentUser.getId());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Google access token")
    public ResponseEntity<GoogleTokenResponse> refreshToken() {
        User currentUser = authService.getCurrentUser();

        if (currentUser.getGoogleRefreshToken() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String accessToken = refreshAccessToken(currentUser);
            return ResponseEntity.ok(new GoogleTokenResponse(accessToken));
        } catch (Exception e) {
            log.error("Failed to refresh Google token for user {}: {}", currentUser.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    private TokenResponse exchangeCodeForTokens(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(GOOGLE_TOKEN_URL, request, Map.class);

        if (response == null) {
            throw new RuntimeException("Failed to exchange code for tokens");
        }

        return new TokenResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                ((Number) response.get("expires_in")).longValue()
        );
    }

    private String refreshAccessToken(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("refresh_token", user.getGoogleRefreshToken());
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(GOOGLE_TOKEN_URL, request, Map.class);

        if (response == null) {
            throw new RuntimeException("Failed to refresh access token");
        }

        String accessToken = (String) response.get("access_token");
        long expiresIn = ((Number) response.get("expires_in")).longValue();

        // Update token expiry
        user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
        userRepository.save(user);

        return accessToken;
    }

    private GoogleUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                GOOGLE_USERINFO_URL,
                HttpMethod.GET,
                request,
                Map.class
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new RuntimeException("Failed to get user info");
        }

        return new GoogleUserInfo(
                (String) body.get("id"),
                (String) body.get("email"),
                (String) body.get("name")
        );
    }

    // DTOs
    public record GoogleAuthUrlResponse(String authUrl) {}

    @Data
    public static class GoogleCallbackRequest {
        private String code;
    }

    public record GoogleConnectionResponse(
            boolean connected,
            String email,
            LocalDateTime connectedAt,
            String accessToken
    ) {}

    public record GoogleStatusResponse(
            boolean connected,
            String email,
            LocalDateTime connectedAt
    ) {}

    public record GoogleTokenResponse(String accessToken) {}

    private record TokenResponse(String accessToken, String refreshToken, long expiresIn) {}

    private record GoogleUserInfo(String id, String email, String name) {}
}
