package com.kiwi.keweiaiagent.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ShellOpenClawCommandRunner implements OpenClawCommandRunner {

    private final File workingDirectory;

    public ShellOpenClawCommandRunner() {
        this(new File(System.getProperty("user.dir")));
    }

    ShellOpenClawCommandRunner(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDirectory);
        long startNanos = System.nanoTime();
        log.info("Starting OpenClaw command. cwd={}, timeoutSeconds={}, command={}",
                workingDirectory.getAbsolutePath(), timeout.toSeconds(), command);
        Process process = processBuilder.start();
        CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
        CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());
        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            String stdout = awaitStream(stdoutFuture);
            String stderr = awaitStream(stderrFuture);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.warn("OpenClaw command timed out after {} ms. command={}, stdoutPreview={}, stderrPreview={}",
                    elapsedMs, command, preview(stdout), preview(stderr));
            return new CommandResult(124, stdout, firstNonBlank(stderr, "Timed out after " + timeout.toSeconds() + " seconds"));
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        String stdout = awaitStream(stdoutFuture);
        String stderr = awaitStream(stderrFuture);
        log.info("OpenClaw command finished. exitCode={}, elapsedMs={}, stdoutPreview={}, stderrPreview={}",
                process.exitValue(),
                elapsedMs,
                preview(stdout),
                preview(stderr));
        return new CommandResult(process.exitValue(), stdout, stderr);
    }

    private CompletableFuture<String> readStreamAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream in = inputStream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                in.transferTo(out);
                return out.toString(java.nio.charset.StandardCharsets.UTF_8).trim();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String awaitStream(CompletableFuture<String> future) throws IOException, InterruptedException {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException && runtimeException.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to read process output", cause);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException("Timed out while reading process output", e);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 400 ? normalized : normalized.substring(0, 400) + "...";
    }
}
