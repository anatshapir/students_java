package com.javaedu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaedu.dto.ai.GenerateExerciseRequest;
import com.javaedu.dto.ai.GeneratedExerciseResponse;
import com.javaedu.exception.BadRequestException;
import com.javaedu.model.AIInteraction;
import com.javaedu.model.Exercise;
import com.javaedu.model.User;
import com.javaedu.repository.AIInteractionRepository;
import com.javaedu.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIHelperService {

    private final AIInteractionRepository interactionRepository;
    private final ExerciseRepository exerciseRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ai.provider:claude}")
    private String aiProvider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.rate-limit.requests-per-hour:10}")
    private int requestsPerHour;

    @Value("${ai.rate-limit.requests-per-day:50}")
    private int requestsPerDay;

    private static final String SYSTEM_PROMPT = """
        You are a Java programming tutor helping students learn to code.

        IMPORTANT RULES:
        1. NEVER provide complete solutions or direct answers to programming problems
        2. NEVER write the code that solves the student's exercise
        3. Instead, guide students by:
           - Asking clarifying questions about their approach
           - Pointing them to relevant Java documentation or concepts
           - Explaining programming concepts in simple terms
           - Suggesting debugging strategies
           - Giving hints that lead them to discover the solution themselves
        4. If a student explicitly asks for the answer, politely redirect them to learning
        5. Be encouraging and supportive, but maintain educational integrity

        Your goal is to help students LEARN, not to do their work for them.
        """;

    private static final String EXERCISE_GENERATION_PROMPT = """
        You are a Java programming exercise creator for an educational platform. Generate exercises as JSON only - no additional text or explanations.

        You MUST respond with ONLY valid JSON in this exact format:
        {
          "title": "Short descriptive title",
          "description": "Markdown description with requirements, examples, and constraints",
          "starterCode": "public class Solution {\\n    // Method stubs for student to implement\\n}",
          "solutionCode": "public class Solution {\\n    // Complete working solution\\n}",
          "difficulty": "EASY|MEDIUM|HARD",
          "category": "BASICS|ARRAYS|STRINGS|LOOPS|CONDITIONALS|METHODS|OOP|ALGORITHMS|DATA_STRUCTURES",
          "points": 100,
          "testCases": [
            {
              "name": "Test descriptive name",
              "testCode": "Solution s = new Solution(); assert s.methodName(input) == expected : \\"Error message\\";",
              "isHidden": false,
              "points": 25,
              "description": "Human-readable description of what this test checks"
            }
          ],
          "hints": ["Optional hint 1", "Optional hint 2"]
        }

        Test code rules:
        - Must be valid Java that runs inside a static method
        - The student's Solution class is available
        - Use assert statements with descriptive error messages
        - Include 2-3 visible tests (isHidden: false) for basic cases shown in description
        - Include 1-2 hidden tests (isHidden: true) for edge cases
        - Common patterns:
          * Simple return: Solution s = new Solution(); assert s.add(2, 3) == 5 : "Expected 5";
          * Array comparison: Solution s = new Solution(); int[] result = s.method(input); assert java.util.Arrays.equals(result, expected);
          * String check: Solution s = new Solution(); assert s.greet("World").equals("Hello, World!");
          * Exception expected: try { new Solution().divide(1, 0); assert false : "Should throw"; } catch (ArithmeticException e) { /* expected */ }

        Starter code rules:
        - Always use public class Solution
        - Include method signatures with proper return types
        - Add TODO comments guiding the student
        - Never include the solution logic

        Description rules:
        - Use Markdown formatting
        - Include clear requirements
        - Show at least one example with expected output
        - List any constraints (e.g., "Array will have at least 1 element")
        """;

    public AIResponse askQuestion(User user, Long exerciseId, String question, String currentCode) {
        checkRateLimit(user);

        Exercise exercise = null;
        if (exerciseId != null) {
            exercise = exerciseRepository.findById(exerciseId).orElse(null);
        }

        String contextPrompt = buildContextPrompt(exercise, currentCode);
        String fullQuestion = contextPrompt + "\n\nStudent's question: " + question;

        String response;
        int tokensUsed = 0;

        if ("claude".equalsIgnoreCase(aiProvider)) {
            var result = callClaudeAPI(fullQuestion);
            response = result.response();
            tokensUsed = result.tokensUsed();
        } else {
            response = "AI provider not configured. Please contact your teacher for assistance.";
        }

        AIInteraction interaction = AIInteraction.builder()
                .user(user)
                .exercise(exercise)
                .question(question)
                .response(response)
                .tokensUsed(tokensUsed)
                .build();

        interactionRepository.save(interaction);

        log.info("AI interaction for user {}: question length={}, response length={}",
                user.getEmail(), question.length(), response.length());

        return new AIResponse(response, getRemainingRequests(user));
    }

    private void checkRateLimit(User user) {
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
        int hourlyCount = interactionRepository.countInteractionsInLastHour(user.getId(), hourAgo);

        if (hourlyCount >= requestsPerHour) {
            throw new BadRequestException(
                    String.format("Rate limit exceeded. You can make %d AI requests per hour. Try again later.",
                            requestsPerHour));
        }

        LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
        int dailyCount = interactionRepository.countByUserIdSince(user.getId(), dayAgo);

        if (dailyCount >= requestsPerDay) {
            throw new BadRequestException(
                    String.format("Daily limit exceeded. You can make %d AI requests per day.",
                            requestsPerDay));
        }
    }

    private int getRemainingRequests(User user) {
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
        int hourlyCount = interactionRepository.countInteractionsInLastHour(user.getId(), hourAgo);
        return Math.max(0, requestsPerHour - hourlyCount - 1);
    }

    private String buildContextPrompt(Exercise exercise, String currentCode) {
        StringBuilder context = new StringBuilder();

        if (exercise != null) {
            context.append("The student is working on an exercise:\n");
            context.append("Title: ").append(exercise.getTitle()).append("\n");
            context.append("Description: ").append(exercise.getDescription()).append("\n");
            context.append("Difficulty: ").append(exercise.getDifficulty()).append("\n\n");
        }

        if (currentCode != null && !currentCode.isBlank()) {
            context.append("The student's current code:\n```java\n");
            context.append(currentCode);
            context.append("\n```\n\n");
        }

        return context.toString();
    }

    private AIResult callClaudeAPI(String question) {
        if (apiKey == null || apiKey.isBlank()) {
            return new AIResult(
                    "AI assistant is not configured. Please contact your teacher for help.",
                    0
            );
        }

        try {
            WebClient client = webClientBuilder
                    .baseUrl("https://api.anthropic.com")
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", "2023-06-01")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> request = Map.of(
                    "model", "claude-3-haiku-20240307",
                    "max_tokens", 1024,
                    "system", SYSTEM_PROMPT,
                    "messages", List.of(
                            Map.of("role", "user", "content", question)
                    )
            );

            Map<String, Object> response = client.post()
                    .uri("/v1/messages")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                if (!content.isEmpty()) {
                    String text = (String) content.get(0).get("text");
                    Map<String, Object> usage = (Map<String, Object>) response.get("usage");
                    int tokens = usage != null ?
                            ((Number) usage.getOrDefault("input_tokens", 0)).intValue() +
                            ((Number) usage.getOrDefault("output_tokens", 0)).intValue() : 0;
                    return new AIResult(text, tokens);
                }
            }

            return new AIResult("I couldn't process your request. Please try again.", 0);

        } catch (Exception e) {
            log.error("Error calling Claude API", e);
            return new AIResult(
                    "Sorry, I'm having trouble connecting to the AI service. Please try again later.",
                    0
            );
        }
    }

    public GeneratedExerciseResponse generateExercise(User user, GenerateExerciseRequest request) {
        checkRateLimit(user);

        String userPrompt = buildExerciseGenerationPrompt(request);

        AIResult result;
        if (request.getImage() != null && !request.getImage().isBlank()) {
            result = callClaudeVisionAPI(userPrompt, request.getImage(), request.getImageMediaType());
        } else {
            result = callClaudeAPIForGeneration(userPrompt);
        }

        GeneratedExerciseResponse response = parseGeneratedExercise(result.response());

        // Apply request overrides if provided
        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            response.setDifficulty(request.getDifficulty().toUpperCase());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            response.setCategory(request.getCategory().toUpperCase());
        }

        // Save interaction for tracking
        AIInteraction interaction = AIInteraction.builder()
                .user(user)
                .question("Generate exercise: " + request.getPrompt())
                .response(result.response())
                .tokensUsed(result.tokensUsed())
                .build();
        interactionRepository.save(interaction);

        log.info("Generated exercise for user {}: title={}", user.getEmail(), response.getTitle());

        return response;
    }

    private String buildExerciseGenerationPrompt(GenerateExerciseRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a Java programming exercise based on this description:\n\n");
        prompt.append(request.getPrompt());

        if (request.getDifficulty() != null && !request.getDifficulty().isBlank()) {
            prompt.append("\n\nDifficulty level: ").append(request.getDifficulty());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            prompt.append("\nCategory: ").append(request.getCategory());
        }
        if (request.getNumberOfTestCases() != null) {
            prompt.append("\nNumber of test cases: ").append(request.getNumberOfTestCases());
        }

        return prompt.toString();
    }

    private AIResult callClaudeAPIForGeneration(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("AI assistant is not configured. Please contact the administrator.");
        }

        try {
            WebClient client = webClientBuilder
                    .baseUrl("https://api.anthropic.com")
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", "2023-06-01")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> request = Map.of(
                    "model", "claude-3-5-sonnet-20241022",
                    "max_tokens", 4096,
                    "system", EXERCISE_GENERATION_PROMPT,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );

            Map<String, Object> response = client.post()
                    .uri("/v1/messages")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractResponseFromClaude(response);

        } catch (Exception e) {
            log.error("Error calling Claude API for exercise generation", e);
            throw new BadRequestException("Failed to generate exercise. Please try again.");
        }
    }

    private AIResult callClaudeVisionAPI(String prompt, String base64Image, String mediaType) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("AI assistant is not configured. Please contact the administrator.");
        }

        // Validate image size (rough estimate: base64 is ~1.37x larger than binary)
        int estimatedSizeBytes = (int) (base64Image.length() * 0.73);
        if (estimatedSizeBytes > 5 * 1024 * 1024) { // 5MB limit
            throw new BadRequestException("Image must be under 5MB.");
        }

        if (mediaType == null || mediaType.isBlank()) {
            mediaType = "image/png";
        }

        try {
            WebClient client = webClientBuilder
                    .baseUrl("https://api.anthropic.com")
                    .defaultHeader("x-api-key", apiKey)
                    .defaultHeader("anthropic-version", "2023-06-01")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            // Build content with image and text
            List<Map<String, Object>> content = new ArrayList<>();

            // Add image
            Map<String, Object> imageSource = new HashMap<>();
            imageSource.put("type", "base64");
            imageSource.put("media_type", mediaType);
            imageSource.put("data", base64Image);

            Map<String, Object> imageContent = new HashMap<>();
            imageContent.put("type", "image");
            imageContent.put("source", imageSource);
            content.add(imageContent);

            // Add text prompt
            Map<String, Object> textContent = new HashMap<>();
            textContent.put("type", "text");
            textContent.put("text", prompt + "\n\nAnalyze the image above and create a Java exercise based on it.");
            content.add(textContent);

            Map<String, Object> request = new HashMap<>();
            request.put("model", "claude-3-5-sonnet-20241022");
            request.put("max_tokens", 4096);
            request.put("system", EXERCISE_GENERATION_PROMPT);
            request.put("messages", List.of(
                    Map.of("role", "user", "content", content)
            ));

            Map<String, Object> response = client.post()
                    .uri("/v1/messages")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractResponseFromClaude(response);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Claude Vision API", e);
            throw new BadRequestException("Failed to process image. Please try again.");
        }
    }

    private AIResult extractResponseFromClaude(Map<String, Object> response) {
        if (response != null && response.containsKey("content")) {
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            if (!content.isEmpty()) {
                String text = (String) content.get(0).get("text");
                Map<String, Object> usage = (Map<String, Object>) response.get("usage");
                int tokens = usage != null ?
                        ((Number) usage.getOrDefault("input_tokens", 0)).intValue() +
                        ((Number) usage.getOrDefault("output_tokens", 0)).intValue() : 0;
                return new AIResult(text, tokens);
            }
        }
        throw new BadRequestException("AI response was invalid. Please try regenerating.");
    }

    private GeneratedExerciseResponse parseGeneratedExercise(String jsonResponse) {
        try {
            // Clean up response - sometimes AI includes markdown code blocks
            String cleaned = jsonResponse.trim();
            if (cleaned.startsWith("```json")) {
                cleaned = cleaned.substring(7);
            } else if (cleaned.startsWith("```")) {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            return objectMapper.readValue(cleaned, GeneratedExerciseResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse generated exercise JSON: {}", jsonResponse, e);
            throw new BadRequestException("AI response was invalid. Please try regenerating.");
        }
    }

    public void markInteractionHelpful(Long interactionId, boolean helpful) {
        interactionRepository.findById(interactionId)
                .ifPresent(interaction -> {
                    interaction.setWasHelpful(helpful);
                    interactionRepository.save(interaction);
                });
    }

    public List<AIInteraction> getInteractionsForExercise(Long exerciseId) {
        return interactionRepository.findByExerciseIdOrderByTimestampDesc(exerciseId);
    }

    private record AIResult(String response, int tokensUsed) {}
    public record AIResponse(String response, int remainingRequests) {}
}
