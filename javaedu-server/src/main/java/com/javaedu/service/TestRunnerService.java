package com.javaedu.service;

import com.javaedu.config.SandboxConfig;
import com.javaedu.model.Submission;
import com.javaedu.model.TestCase;
import com.javaedu.model.TestResult;
import com.javaedu.repository.TestResultRepository;
import com.javaedu.sandbox.SandboxClassLoader;
import com.javaedu.sandbox.TimeoutExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.springframework.stereotype.Service;

import javax.tools.*;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestRunnerService {

    private final SandboxConfig sandboxConfig;
    private final TestResultRepository testResultRepository;

    public CompilationResult compile(String sourceCode) {
        try {
            String className = extractClassName(sourceCode);
            if (className == null) {
                return CompilationResult.failure("Could not find a public class in the submitted code");
            }

            // Try system compiler first, fall back to ECJ
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                compiler = new EclipseCompiler();
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            InMemoryFileManager fileManager = new InMemoryFileManager(
                    compiler.getStandardFileManager(diagnostics, null, null));

            JavaFileObject sourceFile = new InMemoryJavaFileObject(className, sourceCode);
            fileManager.addSource(className, sourceFile);

            List<String> options = Arrays.asList(
                    "-source", "8",
                    "-target", "8",
                    "-Xlint:none",
                    "-proc:none"
            );

            JavaCompiler.CompilationTask task = compiler.getTask(
                    new StringWriter(),
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    Collections.singletonList(sourceFile)
            );

            boolean success = task.call();

            if (!success) {
                StringBuilder errors = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                        errors.append(String.format("Line %d: %s%n",
                                diagnostic.getLineNumber(),
                                diagnostic.getMessage(null)));
                    }
                }
                return CompilationResult.failure(errors.toString());
            }

            Map<String, byte[]> compiledClasses = fileManager.getCompiledClasses();
            return CompilationResult.success(className, compiledClasses);

        } catch (Exception e) {
            log.error("Compilation error", e);
            return CompilationResult.failure("Compilation failed: " + e.getMessage());
        }
    }

    public List<TestResult> runTests(Submission submission, Map<String, byte[]> compiledClasses) {
        List<TestResult> results = new ArrayList<>();
        List<TestCase> testCases = submission.getExercise().getTestCases();

        TimeoutExecutor executor = new TimeoutExecutor(sandboxConfig.getTimeoutSeconds());

        try {
            for (TestCase testCase : testCases) {
                TestResult result = runSingleTest(testCase, compiledClasses, submission, executor);
                result = testResultRepository.save(result);
                results.add(result);
            }
        } finally {
            executor.shutdown();
        }

        return results;
    }

    private TestResult runSingleTest(TestCase testCase, Map<String, byte[]> compiledClasses,
                                     Submission submission, TimeoutExecutor executor) {
        long startTime = System.currentTimeMillis();

        try {
            Map<String, byte[]> allClasses = new HashMap<>(compiledClasses);

            String testClassName = "Test_" + testCase.getId();
            String fullTestCode = wrapTestCode(testClassName, testCase.getTestCode(), extractClassName(submission.getCode()));

            CompilationResult testCompilation = compileTestCode(testClassName, fullTestCode, compiledClasses);
            if (!testCompilation.isSuccess()) {
                return TestResult.builder()
                        .submission(submission)
                        .testCase(testCase)
                        .passed(false)
                        .errorMessage("Test compilation failed: " + testCompilation.getErrors())
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            allClasses.putAll(testCompilation.getCompiledClasses());

            SandboxClassLoader classLoader = new SandboxClassLoader(allClasses, getClass().getClassLoader());

            TimeoutExecutor.ExecutionResult<TestExecutionOutput> executionResult = executor.execute(() -> {
                Class<?> testClass = classLoader.loadClass(testClassName);
                Method testMethod = testClass.getMethod("runTest");

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;

                try {
                    System.setOut(new PrintStream(outputStream));
                    System.setErr(new PrintStream(outputStream));

                    Object result = testMethod.invoke(null);
                    boolean passed = result == null || Boolean.TRUE.equals(result);

                    return new TestExecutionOutput(passed, outputStream.toString(), null);
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }
            });

            long executionTime = System.currentTimeMillis() - startTime;

            if (executionResult.isTimeout()) {
                return TestResult.builder()
                        .submission(submission)
                        .testCase(testCase)
                        .passed(false)
                        .errorMessage("Test execution timed out after " + sandboxConfig.getTimeoutSeconds() + " seconds")
                        .executionTimeMs(executionTime)
                        .build();
            }

            if (executionResult.isSecurityViolation()) {
                return TestResult.builder()
                        .submission(submission)
                        .testCase(testCase)
                        .passed(false)
                        .errorMessage("Security violation: " + executionResult.getErrorMessage())
                        .executionTimeMs(executionTime)
                        .build();
            }

            if (!executionResult.isSuccess()) {
                return TestResult.builder()
                        .submission(submission)
                        .testCase(testCase)
                        .passed(false)
                        .errorMessage(executionResult.getErrorMessage())
                        .executionTimeMs(executionTime)
                        .build();
            }

            TestExecutionOutput output = executionResult.getResult();
            return TestResult.builder()
                    .submission(submission)
                    .testCase(testCase)
                    .passed(output.passed)
                    .actualOutput(output.output)
                    .errorMessage(output.error)
                    .executionTimeMs(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("Error running test case {}", testCase.getName(), e);
            return TestResult.builder()
                    .submission(submission)
                    .testCase(testCase)
                    .passed(false)
                    .errorMessage("Test execution error: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private String wrapTestCode(String testClassName, String testCode, String studentClassName) {
        // Convert assert statements to explicit if-throw since assertions are disabled by default
        String convertedTestCode = convertAssertToThrow(testCode);
        return String.format("""
            public class %s {
                public static Boolean runTest() throws Exception {
                    %s
                    return true;
                }
            }
            """, testClassName, convertedTestCode);
    }

    private String convertAssertToThrow(String testCode) {
        // Pattern to match: assert condition : "message";
        // Convert to: if (!(condition)) throw new AssertionError("message");
        String result = testCode;

        // Match assert with message: assert expr : "msg";
        Pattern assertWithMsg = Pattern.compile(
            "assert\\s+(.+?)\\s*:\\s*\"([^\"]+)\"\\s*;",
            Pattern.DOTALL
        );
        Matcher m1 = assertWithMsg.matcher(result);
        StringBuffer sb1 = new StringBuffer();
        while (m1.find()) {
            String condition = m1.group(1).trim();
            String message = m1.group(2);
            m1.appendReplacement(sb1, "if (!(" + escapeReplacement(condition) + ")) throw new AssertionError(\"" + escapeReplacement(message) + "\");");
        }
        m1.appendTail(sb1);
        result = sb1.toString();

        // Match assert without message: assert expr;
        Pattern assertWithoutMsg = Pattern.compile(
            "assert\\s+([^;:]+)\\s*;",
            Pattern.DOTALL
        );
        Matcher m2 = assertWithoutMsg.matcher(result);
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) {
            String condition = m2.group(1).trim();
            m2.appendReplacement(sb2, "if (!(" + escapeReplacement(condition) + ")) throw new AssertionError(\"Assertion failed\");");
        }
        m2.appendTail(sb2);
        result = sb2.toString();

        return result;
    }

    private String escapeReplacement(String str) {
        return str.replace("\\", "\\\\").replace("$", "\\$");
    }

    private CompilationResult compileTestCode(String className, String sourceCode, Map<String, byte[]> studentClasses) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                compiler = new EclipseCompiler();
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            InMemoryFileManager fileManager = new InMemoryFileManager(
                    compiler.getStandardFileManager(diagnostics, null, null));

            // Add student's compiled classes so test code can reference them
            fileManager.addCompiledClasses(studentClasses);

            JavaFileObject sourceFile = new InMemoryJavaFileObject(className, sourceCode);
            fileManager.addSource(className, sourceFile);

            JavaCompiler.CompilationTask task = compiler.getTask(
                    new StringWriter(),
                    fileManager,
                    diagnostics,
                    Arrays.asList("-source", "8", "-target", "8", "-proc:none"),
                    null,
                    Collections.singletonList(sourceFile)
            );

            if (task.call()) {
                return CompilationResult.success(className, fileManager.getCompiledClasses());
            } else {
                StringBuilder errors = new StringBuilder();
                for (Diagnostic<?> d : diagnostics.getDiagnostics()) {
                    if (d.getKind() == Diagnostic.Kind.ERROR) {
                        errors.append(d.getMessage(null)).append("\n");
                    }
                }
                return CompilationResult.failure(errors.toString());
            }
        } catch (Exception e) {
            return CompilationResult.failure(e.getMessage());
        }
    }

    private String extractClassName(String sourceCode) {
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(sourceCode);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private record TestExecutionOutput(boolean passed, String output, String error) {}

    public static class CompilationResult {
        private final boolean success;
        private final String className;
        private final Map<String, byte[]> compiledClasses;
        private final String errors;

        private CompilationResult(boolean success, String className, Map<String, byte[]> compiledClasses, String errors) {
            this.success = success;
            this.className = className;
            this.compiledClasses = compiledClasses;
            this.errors = errors;
        }

        public static CompilationResult success(String className, Map<String, byte[]> compiledClasses) {
            return new CompilationResult(true, className, compiledClasses, null);
        }

        public static CompilationResult failure(String errors) {
            return new CompilationResult(false, null, null, errors);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getClassName() {
            return className;
        }

        public Map<String, byte[]> getCompiledClasses() {
            return compiledClasses;
        }

        public String getErrors() {
            return errors;
        }
    }

    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    private static class InMemoryClassFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InMemoryClassFileObject(String className) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public java.io.OutputStream openOutputStream() {
            return outputStream;
        }

        byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }

    private static class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
        private final Map<String, InMemoryClassFileObject> classFiles = new HashMap<>();
        private final Map<String, JavaFileObject> sourceFiles = new HashMap<>();
        private final Map<String, byte[]> precompiledClasses = new HashMap<>();

        InMemoryFileManager(StandardJavaFileManager fileManager) {
            super(fileManager);
        }

        void addSource(String className, JavaFileObject source) {
            sourceFiles.put(className, source);
        }

        void addCompiledClasses(Map<String, byte[]> classes) {
            if (classes != null) {
                precompiledClasses.putAll(classes);
            }
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className,
                                                  JavaFileObject.Kind kind) throws java.io.IOException {
            if (kind == JavaFileObject.Kind.SOURCE) {
                JavaFileObject source = sourceFiles.get(className);
                if (source != null) {
                    return source;
                }
            }
            if (kind == JavaFileObject.Kind.CLASS) {
                byte[] bytes = precompiledClasses.get(className);
                if (bytes != null) {
                    return new InMemoryClassInputObject(className, bytes);
                }
            }
            return super.getJavaFileForInput(location, className, kind);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            InMemoryClassFileObject classFile = new InMemoryClassFileObject(className);
            classFiles.put(className, classFile);
            return classFile;
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            return new ClassLoader(getClass().getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    byte[] bytes = precompiledClasses.get(name);
                    if (bytes != null) {
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    throw new ClassNotFoundException(name);
                }
            };
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName,
                                             Set<JavaFileObject.Kind> kinds, boolean recurse) throws java.io.IOException {
            Iterable<JavaFileObject> superList = super.list(location, packageName, kinds, recurse);

            if (kinds.contains(JavaFileObject.Kind.CLASS) && (packageName.isEmpty() || packageName.equals(""))) {
                List<JavaFileObject> result = new ArrayList<>();
                for (JavaFileObject fo : superList) {
                    result.add(fo);
                }
                // Add precompiled classes from default package
                for (Map.Entry<String, byte[]> entry : precompiledClasses.entrySet()) {
                    String className = entry.getKey();
                    if (!className.contains(".")) { // Default package
                        result.add(new InMemoryClassInputObject(className, entry.getValue()));
                    }
                }
                return result;
            }
            return superList;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            if (file instanceof InMemoryClassInputObject) {
                return ((InMemoryClassInputObject) file).getClassName();
            }
            return super.inferBinaryName(location, file);
        }

        Map<String, byte[]> getCompiledClasses() {
            Map<String, byte[]> result = new HashMap<>();
            for (Map.Entry<String, InMemoryClassFileObject> entry : classFiles.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getBytes());
            }
            return result;
        }
    }

    private static class InMemoryClassInputObject extends SimpleJavaFileObject {
        private final byte[] bytes;
        private final String className;

        InMemoryClassInputObject(String className, byte[] bytes) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
            this.bytes = bytes;
            this.className = className;
        }

        @Override
        public java.io.InputStream openInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        String getClassName() {
            return className;
        }
    }
}
