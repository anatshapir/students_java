package com.javaedu.eclipse.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javaedu.eclipse.model.*;

/**
 * HTTP client for communicating with the JavaEdu server.
 */
public class ApiClient {

    private static ApiClient instance;
    private final Gson gson = new Gson();
    private String baseUrl = "http://localhost:8080/api";

    private ApiClient() {
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Login with email and password.
     */
    public LoginResponse login(String email, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);

        String response = post("/auth/login", body.toString(), false);
        return gson.fromJson(response, LoginResponse.class);
    }

    /**
     * Register a new user.
     */
    public LoginResponse register(String name, String email, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("role", "STUDENT");

        String response = post("/auth/register", body.toString(), false);
        return gson.fromJson(response, LoginResponse.class);
    }

    /**
     * Get list of courses for current user.
     */
    public List<Course> getCourses() throws Exception {
        String response = get("/courses");
        JsonArray array = JsonParser.parseString(response).getAsJsonArray();

        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            courses.add(gson.fromJson(array.get(i), Course.class));
        }
        return courses;
    }

    /**
     * Get exercises for a course.
     */
    public List<Exercise> getExercisesForCourse(Long courseId) throws Exception {
        String response = get("/exercises/course/" + courseId);
        JsonArray array = JsonParser.parseString(response).getAsJsonArray();

        List<Exercise> exercises = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            exercises.add(gson.fromJson(array.get(i), Exercise.class));
        }
        return exercises;
    }

    /**
     * Get exercise details.
     */
    public Exercise getExercise(Long exerciseId) throws Exception {
        String response = get("/exercises/" + exerciseId);
        return gson.fromJson(response, Exercise.class);
    }

    /**
     * Submit code for an exercise.
     */
    public Submission submitCode(Long exerciseId, String code) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("exerciseId", exerciseId);
        body.addProperty("code", code);

        String response = post("/submissions", body.toString(), true);
        return gson.fromJson(response, Submission.class);
    }

    /**
     * Get submission status.
     */
    public Submission getSubmission(Long submissionId) throws Exception {
        String response = get("/submissions/" + submissionId);
        return gson.fromJson(response, Submission.class);
    }

    /**
     * Get user's submissions.
     */
    public List<Submission> getMySubmissions() throws Exception {
        String response = get("/submissions/my");
        JsonArray array = JsonParser.parseString(response).getAsJsonArray();

        List<Submission> submissions = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            submissions.add(gson.fromJson(array.get(i), Submission.class));
        }
        return submissions;
    }

    /**
     * Get hints for an exercise (progressive reveal).
     */
    public JsonObject getHints(Long exerciseId, int upTo) throws Exception {
        String response = get("/exercises/" + exerciseId + "/hints?upTo=" + upTo);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    /**
     * Ask the AI helper a question.
     */
    public AIResponse askAIHelper(Long exerciseId, String question, String currentCode) throws Exception {
        JsonObject body = new JsonObject();
        if (exerciseId != null) {
            body.addProperty("exerciseId", exerciseId);
        }
        body.addProperty("question", question);
        if (currentCode != null) {
            body.addProperty("currentCode", currentCode);
        }

        String response = post("/ai/ask", body.toString(), true);
        return gson.fromJson(response, AIResponse.class);
    }

    // HTTP helper methods

    private String get(String endpoint) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");

        String token = AuthManager.getInstance().getAccessToken();
        if (token != null) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }

        return readResponse(conn);
    }

    private String post(String endpoint, String body, boolean authenticate) throws Exception {
        URL url = new URL(baseUrl + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        if (authenticate) {
            String token = AuthManager.getInstance().getAccessToken();
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();

        BufferedReader reader;
        if (responseCode >= 200 && responseCode < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (responseCode == 401) {
            AuthManager.getInstance().logout();
            throw new Exception("Session expired. Please log in again.");
        }

        if (responseCode >= 400) {
            throw new Exception("HTTP Error " + responseCode + ": " + response.toString());
        }

        return response.toString();
    }

    /**
     * Response model for login/register.
     */
    public static class LoginResponse {
        private String accessToken;
        private String refreshToken;
        private Long userId;
        private String email;
        private String name;
        private String role;

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public Long getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }
    }
}
