package com.javaedu.service;

import com.javaedu.config.SandboxConfig;
import com.javaedu.repository.TestResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(MockitoExtension.class)
class TestRunnerServiceTest {

    @Mock
    private TestResultRepository testResultRepository;

    private SandboxConfig sandboxConfig;
    private TestRunnerService testRunnerService;

    @BeforeEach
    void setUp() {
        sandboxConfig = new SandboxConfig();
        sandboxConfig.setTimeoutSeconds(5);
        testRunnerService = new TestRunnerService(sandboxConfig, testResultRepository);
    }

    @Test
    void compile_WithValidCode_ReturnsSuccess() {
        String code = """
            public class HelloWorld {
                public static String greet() {
                    return "Hello, World!";
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        if (!result.isSuccess()) {
            fail("Compilation failed with errors: " + result.getErrors());
        }
        assertEquals("HelloWorld", result.getClassName());
        assertNotNull(result.getCompiledClasses());
        assertTrue(result.getCompiledClasses().containsKey("HelloWorld"));
    }

    @Test
    void compile_WithSyntaxError_ReturnsFailure() {
        String code = """
            public class Broken {
                public static void main(String[] args) {
                    System.out.println("Missing semicolon")
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrors());
        assertTrue(result.getErrors().contains("Line"));
    }

    @Test
    void compile_WithMissingClass_ReturnsFailure() {
        // Code with only a package-private class (no public keyword)
        String code = """
            class PrivateClass {
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        assertFalse(result.isSuccess());
        assertTrue(result.getErrors().contains("Could not find a public class"));
    }

    @Test
    void compile_WithMultipleMethods_ReturnsSuccess() {
        String code = """
            public class Calculator {
                public static int add(int a, int b) {
                    return a + b;
                }

                public static int subtract(int a, int b) {
                    return a - b;
                }

                public static int multiply(int a, int b) {
                    return a * b;
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        if (!result.isSuccess()) {
            fail("Compilation failed with errors: " + result.getErrors());
        }
        assertEquals("Calculator", result.getClassName());
    }

    @Test
    void compile_WithInnerClass_ReturnsSuccess() {
        String code = """
            public class Outer {
                private static class Inner {
                    public int value = 42;
                }

                public static int getValue() {
                    return new Inner().value;
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        if (!result.isSuccess()) {
            fail("Compilation failed with errors: " + result.getErrors());
        }
        assertEquals("Outer", result.getClassName());
        // Should have both Outer and Outer$Inner classes
        assertTrue(result.getCompiledClasses().size() >= 1);
    }

    @Test
    void compile_WithGenerics_ReturnsSuccess() {
        String code = """
            import java.util.ArrayList;
            import java.util.List;

            public class GenericExample {
                public static List<String> createList() {
                    List<String> list = new ArrayList<>();
                    list.add("Hello");
                    return list;
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        if (!result.isSuccess()) {
            fail("Compilation failed with errors: " + result.getErrors());
        }
    }

    @Test
    void compile_WithUndefinedVariable_ReturnsFailure() {
        String code = """
            public class UndefinedVar {
                public static void main(String[] args) {
                    System.out.println(undefinedVariable);
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrors());
    }

    @Test
    void compile_WithTypeMismatch_ReturnsFailure() {
        String code = """
            public class TypeMismatch {
                public static int getValue() {
                    return "not an int";
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrors());
    }

    @Test
    void compile_WithEmptyClass_ReturnsSuccess() {
        String code = """
            public class EmptyClass {
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        if (!result.isSuccess()) {
            fail("Compilation failed with errors: " + result.getErrors());
        }
        assertEquals("EmptyClass", result.getClassName());
    }

    @Test
    void compile_WithJava8Features_ReturnsSuccess() {
        String code = """
            import java.util.Arrays;
            import java.util.List;

            public class Java8Features {
                public static long countEvens(int[] numbers) {
                    return Arrays.stream(numbers)
                            .filter(n -> n % 2 == 0)
                            .count();
                }
            }
            """;

        TestRunnerService.CompilationResult result = testRunnerService.compile(code);

        if (!result.isSuccess()) {
            fail("Compilation failed with errors: " + result.getErrors());
        }
    }
}
