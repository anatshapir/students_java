package com.javaedu.sandbox;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * Executes tasks with a timeout, killing runaway student code.
 */
@Slf4j
public class TimeoutExecutor {

    private final int timeoutSeconds;
    private final ExecutorService executor;

    public TimeoutExecutor(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public <T> ExecutionResult<T> execute(Callable<T> task) {
        Future<T> future = executor.submit(task);

        try {
            T result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            return ExecutionResult.success(result);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("Task execution timed out after {} seconds", timeoutSeconds);
            return ExecutionResult.timeout();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return ExecutionResult.error("Execution interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityException) {
                return ExecutionResult.securityViolation(cause.getMessage());
            }
            return ExecutionResult.error(cause.getMessage());
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public static class ExecutionResult<T> {
        private final T result;
        private final boolean success;
        private final boolean timeout;
        private final boolean securityViolation;
        private final String errorMessage;

        private ExecutionResult(T result, boolean success, boolean timeout, boolean securityViolation, String errorMessage) {
            this.result = result;
            this.success = success;
            this.timeout = timeout;
            this.securityViolation = securityViolation;
            this.errorMessage = errorMessage;
        }

        public static <T> ExecutionResult<T> success(T result) {
            return new ExecutionResult<>(result, true, false, false, null);
        }

        public static <T> ExecutionResult<T> timeout() {
            return new ExecutionResult<>(null, false, true, false, "Execution timed out");
        }

        public static <T> ExecutionResult<T> error(String message) {
            return new ExecutionResult<>(null, false, false, false, message);
        }

        public static <T> ExecutionResult<T> securityViolation(String message) {
            return new ExecutionResult<>(null, false, false, true, message);
        }

        public T getResult() {
            return result;
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isTimeout() {
            return timeout;
        }

        public boolean isSecurityViolation() {
            return securityViolation;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
