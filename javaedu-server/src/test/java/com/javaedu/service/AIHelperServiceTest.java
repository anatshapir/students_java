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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIHelperServiceTest {

    @Mock
    private AIInteractionRepository interactionRepository;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private AIHelperService aiHelperService;
    private User testUser;
    private Exercise testExercise;

    @BeforeEach
    void setUp() {
        aiHelperService = new AIHelperService(interactionRepository, exerciseRepository, webClientBuilder, objectMapper);

        // Set configuration values via reflection
        ReflectionTestUtils.setField(aiHelperService, "aiProvider", "claude");
        ReflectionTestUtils.setField(aiHelperService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiHelperService, "requestsPerHour", 10);
        ReflectionTestUtils.setField(aiHelperService, "requestsPerDay", 50);

        testUser = User.builder()
                .id(1L)
                .name("Test Student")
                .email("student@test.com")
                .role(User.Role.STUDENT)
                .build();

        testExercise = Exercise.builder()
                .id(1L)
                .title("Hello World")
                .description("Write a program that prints Hello World")
                .difficulty(Exercise.Difficulty.BEGINNER)
                .build();
    }

    @Test
    void askQuestion_WithValidRequest_ReturnsResponse() {
        // Mock rate limit checks
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(testExercise));

        // Mock WebClient chain for Claude API
        setupWebClientMocks();

        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", "Let me help you understand the problem...")),
                "usage", Map.of("input_tokens", 100, "output_tokens", 50)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AIHelperService.AIResponse result = aiHelperService.askQuestion(
                testUser, 1L, "How do I print to console?", "public class Main {}");

        assertNotNull(result);
        assertEquals("Let me help you understand the problem...", result.response());
        assertTrue(result.remainingRequests() >= 0);

        // Verify interaction was saved
        verify(interactionRepository).save(any(AIInteraction.class));
    }

    @Test
    void askQuestion_ExceedsHourlyLimit_ThrowsException() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(10); // At the limit

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.askQuestion(testUser, 1L, "Help me!", null));

        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void askQuestion_ExceedsDailyLimit_ThrowsException() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(5); // Under hourly limit
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(50); // At daily limit

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.askQuestion(testUser, 1L, "Help me!", null));

        assertTrue(exception.getMessage().contains("Daily limit exceeded"));
    }

    @Test
    void askQuestion_WithoutExercise_StillWorks() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();
        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", "General Java help...")),
                "usage", Map.of("input_tokens", 50, "output_tokens", 30)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AIHelperService.AIResponse result = aiHelperService.askQuestion(
                testUser, null, "What is a loop?", null);

        assertNotNull(result);
        assertEquals("General Java help...", result.response());
    }

    @Test
    void askQuestion_NoApiKey_ReturnsNotConfiguredMessage() {
        // Set empty API key
        ReflectionTestUtils.setField(aiHelperService, "apiKey", "");

        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AIHelperService.AIResponse result = aiHelperService.askQuestion(
                testUser, null, "Help?", null);

        assertNotNull(result);
        assertTrue(result.response().contains("not configured"));
    }

    @Test
    void askQuestion_UnsupportedProvider_ReturnsNotConfiguredMessage() {
        ReflectionTestUtils.setField(aiHelperService, "aiProvider", "unsupported");

        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AIHelperService.AIResponse result = aiHelperService.askQuestion(
                testUser, null, "Help?", null);

        assertNotNull(result);
        assertTrue(result.response().contains("not configured"));
    }

    @Test
    void askQuestion_SavesInteraction() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(exerciseRepository.findById(1L)).thenReturn(Optional.of(testExercise));

        setupWebClientMocks();
        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", "Here is some help...")),
                "usage", Map.of("input_tokens", 100, "output_tokens", 50)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        ArgumentCaptor<AIInteraction> captor = ArgumentCaptor.forClass(AIInteraction.class);
        when(interactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        aiHelperService.askQuestion(testUser, 1L, "My question", "my code");

        AIInteraction saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertEquals(testExercise, saved.getExercise());
        assertEquals("My question", saved.getQuestion());
        assertEquals("Here is some help...", saved.getResponse());
        assertEquals(150, saved.getTokensUsed());
    }

    @Test
    void markInteractionHelpful_UpdatesInteraction() {
        AIInteraction interaction = AIInteraction.builder()
                .id(1L)
                .user(testUser)
                .question("Test")
                .response("Response")
                .wasHelpful(null)
                .build();

        when(interactionRepository.findById(1L)).thenReturn(Optional.of(interaction));
        when(interactionRepository.save(any(AIInteraction.class))).thenReturn(interaction);

        aiHelperService.markInteractionHelpful(1L, true);

        assertTrue(interaction.getWasHelpful());
        verify(interactionRepository).save(interaction);
    }

    @Test
    void markInteractionHelpful_NotFound_DoesNothing() {
        when(interactionRepository.findById(999L)).thenReturn(Optional.empty());

        aiHelperService.markInteractionHelpful(999L, true);

        verify(interactionRepository, never()).save(any(AIInteraction.class));
    }

    @Test
    void getInteractionsForExercise_ReturnsInteractions() {
        List<AIInteraction> interactions = List.of(
                AIInteraction.builder().id(1L).question("Q1").build(),
                AIInteraction.builder().id(2L).question("Q2").build()
        );

        when(interactionRepository.findByExerciseIdOrderByTimestampDesc(1L))
                .thenReturn(interactions);

        List<AIInteraction> result = aiHelperService.getInteractionsForExercise(1L);

        assertEquals(2, result.size());
    }

    @Test
    void askQuestion_ApiError_ReturnsErrorMessage() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        // Set up WebClient to throw an exception
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("API Error"));

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AIHelperService.AIResponse result = aiHelperService.askQuestion(
                testUser, null, "Help?", null);

        assertNotNull(result);
        assertTrue(result.response().contains("trouble connecting"));
    }

    private void setupWebClientMocks() {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    // ==================== generateExercise Tests ====================

    @Test
    void generateExercise_WithValidPrompt_ReturnsGeneratedExercise() throws Exception {
        // Mock rate limit checks
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();

        String jsonResponse = """
            {
                "title": "Reverse Array",
                "description": "Write a method to reverse an array",
                "starterCode": "public class Solution { }",
                "solutionCode": "public class Solution { public int[] reverse(int[] arr) { return arr; } }",
                "difficulty": "EASY",
                "category": "ARRAYS",
                "points": 100,
                "testCases": [
                    {
                        "name": "Test basic case",
                        "testCode": "assert true;",
                        "isHidden": false,
                        "points": 25,
                        "description": "Basic test"
                    }
                ],
                "hints": ["Think about swapping elements"]
            }
            """;

        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", jsonResponse)),
                "usage", Map.of("input_tokens", 200, "output_tokens", 300)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        GeneratedExerciseResponse expectedResponse = GeneratedExerciseResponse.builder()
                .title("Reverse Array")
                .description("Write a method to reverse an array")
                .starterCode("public class Solution { }")
                .solutionCode("public class Solution { public int[] reverse(int[] arr) { return arr; } }")
                .difficulty("EASY")
                .category("ARRAYS")
                .points(100)
                .testCases(List.of(
                        GeneratedExerciseResponse.GeneratedTestCase.builder()
                                .name("Test basic case")
                                .testCode("assert true;")
                                .isHidden(false)
                                .points(25)
                                .description("Basic test")
                                .build()
                ))
                .hints(List.of("Think about swapping elements"))
                .build();

        when(objectMapper.readValue(anyString(), eq(GeneratedExerciseResponse.class)))
                .thenReturn(expectedResponse);

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create a method to reverse an array")
                .numberOfTestCases(4)
                .build();

        GeneratedExerciseResponse result = aiHelperService.generateExercise(testUser, request);

        assertNotNull(result);
        assertEquals("Reverse Array", result.getTitle());
        assertEquals("EASY", result.getDifficulty());
        assertEquals("ARRAYS", result.getCategory());
        assertEquals(1, result.getTestCases().size());

        verify(interactionRepository).save(any(AIInteraction.class));
    }

    @Test
    void generateExercise_WithDifficultyOverride_AppliesOverride() throws Exception {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();

        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", "{}")),
                "usage", Map.of("input_tokens", 100, "output_tokens", 200)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        GeneratedExerciseResponse parsedResponse = GeneratedExerciseResponse.builder()
                .title("Test Exercise")
                .difficulty("EASY")
                .category("BASICS")
                .build();

        when(objectMapper.readValue(anyString(), eq(GeneratedExerciseResponse.class)))
                .thenReturn(parsedResponse);

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create an exercise")
                .difficulty("HARD")
                .category("ALGORITHMS")
                .build();

        GeneratedExerciseResponse result = aiHelperService.generateExercise(testUser, request);

        assertEquals("HARD", result.getDifficulty());
        assertEquals("ALGORITHMS", result.getCategory());
    }

    @Test
    void generateExercise_ExceedsRateLimit_ThrowsException() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(10); // At the limit

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create an exercise")
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.generateExercise(testUser, request));

        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
    }

    @Test
    void generateExercise_NoApiKey_ThrowsException() {
        ReflectionTestUtils.setField(aiHelperService, "apiKey", "");

        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create an exercise")
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.generateExercise(testUser, request));

        assertTrue(exception.getMessage().contains("not configured"));
    }

    @Test
    void generateExercise_InvalidJsonResponse_ThrowsException() throws Exception {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();

        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", "invalid json {")),
                "usage", Map.of("input_tokens", 100, "output_tokens", 50)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        when(objectMapper.readValue(anyString(), eq(GeneratedExerciseResponse.class)))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create an exercise")
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.generateExercise(testUser, request));

        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    void generateExercise_WithImage_CallsVisionAPI() throws Exception {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();

        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", "{}")),
                "usage", Map.of("input_tokens", 500, "output_tokens", 400)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        GeneratedExerciseResponse parsedResponse = GeneratedExerciseResponse.builder()
                .title("Image-based Exercise")
                .difficulty("MEDIUM")
                .category("OOP")
                .build();

        when(objectMapper.readValue(anyString(), eq(GeneratedExerciseResponse.class)))
                .thenReturn(parsedResponse);

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // Small base64 image (just a few bytes for testing)
        String smallBase64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create exercise from this image")
                .image(smallBase64Image)
                .imageMediaType("image/png")
                .build();

        GeneratedExerciseResponse result = aiHelperService.generateExercise(testUser, request);

        assertNotNull(result);
        assertEquals("Image-based Exercise", result.getTitle());
        verify(interactionRepository).save(any(AIInteraction.class));
    }

    @Test
    void generateExercise_ImageTooLarge_ThrowsException() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        // Create a large base64 string (simulating >5MB image)
        // 5MB binary = ~6.85MB base64, so we need ~7MB of base64 chars
        String largeImage = "x".repeat(8_000_000);

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create exercise")
                .image(largeImage)
                .imageMediaType("image/png")
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.generateExercise(testUser, request));

        assertTrue(exception.getMessage().contains("5MB"));
    }

    @Test
    void generateExercise_ApiError_ThrowsException() {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("API Error"));

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create an exercise")
                .build();

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                aiHelperService.generateExercise(testUser, request));

        assertTrue(exception.getMessage().contains("Failed to generate"));
    }

    @Test
    void generateExercise_SavesInteractionWithCorrectData() throws Exception {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();

        String jsonResponse = "{}";
        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", jsonResponse)),
                "usage", Map.of("input_tokens", 150, "output_tokens", 250)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        GeneratedExerciseResponse parsedResponse = GeneratedExerciseResponse.builder()
                .title("Test")
                .build();

        when(objectMapper.readValue(anyString(), eq(GeneratedExerciseResponse.class)))
                .thenReturn(parsedResponse);

        ArgumentCaptor<AIInteraction> captor = ArgumentCaptor.forClass(AIInteraction.class);
        when(interactionRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create a sorting algorithm exercise")
                .build();

        aiHelperService.generateExercise(testUser, request);

        AIInteraction saved = captor.getValue();
        assertEquals(testUser, saved.getUser());
        assertTrue(saved.getQuestion().contains("Generate exercise"));
        assertTrue(saved.getQuestion().contains("Create a sorting algorithm exercise"));
        assertEquals(400, saved.getTokensUsed()); // 150 + 250
    }

    @Test
    void generateExercise_CleansMarkdownCodeBlocks() throws Exception {
        when(interactionRepository.countInteractionsInLastHour(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);
        when(interactionRepository.countByUserIdSince(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0);

        setupWebClientMocks();

        // AI sometimes wraps JSON in markdown code blocks
        String wrappedJson = "```json\n{}\n```";
        Map<String, Object> apiResponse = Map.of(
                "content", List.of(Map.of("text", wrappedJson)),
                "usage", Map.of("input_tokens", 100, "output_tokens", 100)
        );
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(apiResponse));

        GeneratedExerciseResponse parsedResponse = GeneratedExerciseResponse.builder()
                .title("Test")
                .build();

        // Verify the cleaned JSON (without markdown) is passed to ObjectMapper
        when(objectMapper.readValue(eq("{}"), eq(GeneratedExerciseResponse.class)))
                .thenReturn(parsedResponse);

        when(interactionRepository.save(any(AIInteraction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GenerateExerciseRequest request = GenerateExerciseRequest.builder()
                .prompt("Create exercise")
                .build();

        GeneratedExerciseResponse result = aiHelperService.generateExercise(testUser, request);

        assertNotNull(result);
        verify(objectMapper).readValue(eq("{}"), eq(GeneratedExerciseResponse.class));
    }
}
