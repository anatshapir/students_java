package com.javaedu.eclipse.services;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

/**
 * Manages authentication state and secure token storage.
 */
public class AuthManager {

    private static final String SECURE_NODE = "com.javaedu.eclipse";
    private static final String TOKEN_KEY = "accessToken";
    private static final String REFRESH_TOKEN_KEY = "refreshToken";
    private static final String USER_EMAIL_KEY = "userEmail";
    private static final String USER_NAME_KEY = "userName";
    private static final String USER_ID_KEY = "userId";

    private static AuthManager instance;

    private String accessToken;
    private String refreshToken;
    private String userEmail;
    private String userName;
    private Long userId;
    private boolean initialized = false;

    private AuthManager() {
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    /**
     * Initialize the auth manager and load stored credentials.
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        try {
            ISecurePreferences root = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = root.node(SECURE_NODE);

            accessToken = node.get(TOKEN_KEY, null);
            refreshToken = node.get(REFRESH_TOKEN_KEY, null);
            userEmail = node.get(USER_EMAIL_KEY, null);
            userName = node.get(USER_NAME_KEY, null);
            String userIdStr = node.get(USER_ID_KEY, null);
            if (userIdStr != null) {
                userId = Long.parseLong(userIdStr);
            }

            initialized = true;
        } catch (StorageException e) {
            // Handle storage error
            e.printStackTrace();
        }
    }

    /**
     * Store login credentials securely.
     */
    public void login(String accessToken, String refreshToken, Long userId, String email, String name) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.userEmail = email;
        this.userName = name;

        try {
            ISecurePreferences root = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = root.node(SECURE_NODE);

            node.put(TOKEN_KEY, accessToken, true);
            node.put(REFRESH_TOKEN_KEY, refreshToken, true);
            node.put(USER_ID_KEY, String.valueOf(userId), false);
            node.put(USER_EMAIL_KEY, email, false);
            node.put(USER_NAME_KEY, name, false);
            node.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Clear stored credentials.
     */
    public void logout() {
        this.accessToken = null;
        this.refreshToken = null;
        this.userId = null;
        this.userEmail = null;
        this.userName = null;

        try {
            ISecurePreferences root = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = root.node(SECURE_NODE);

            node.remove(TOKEN_KEY);
            node.remove(REFRESH_TOKEN_KEY);
            node.remove(USER_ID_KEY);
            node.remove(USER_EMAIL_KEY);
            node.remove(USER_NAME_KEY);
            node.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if user is logged in.
     */
    public boolean isLoggedIn() {
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * Get the access token.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Get the refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Get the user email.
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * Get the user name.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Get the user ID.
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Update the access token after refresh.
     */
    public void updateAccessToken(String newAccessToken) {
        this.accessToken = newAccessToken;

        try {
            ISecurePreferences root = SecurePreferencesFactory.getDefault();
            ISecurePreferences node = root.node(SECURE_NODE);
            node.put(TOKEN_KEY, newAccessToken, true);
            node.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
