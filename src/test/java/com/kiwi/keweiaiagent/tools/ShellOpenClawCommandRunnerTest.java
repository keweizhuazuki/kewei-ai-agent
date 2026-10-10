package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ShellOpenClawCommandRunnerTest {

    @TempDir
    Path tempDir;

    private final ShellOpenClawCommandRunner runner =
            new ShellOpenClawCommandRunner(new File(System.getProperty("user.dir")));

    @Test
    void shouldDrainLargeStdoutAndStderrWithoutBlocking() throws Exception {
        var result = runner.run(command("output", tempDir.resolve("unused")), Duration.ofSeconds(10));

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("stdout-line-19999"));
        assertTrue(result.stderr().contains("stderr-line-4999"));
    }

    @Test
    void shouldTerminateProcessOnTimeout() throws Exception {
        Path pidFile = tempDir.resolve("timeout.pid");
        try {
            var result = runner.run(command("sleep", pidFile), Duration.ofSeconds(2));

            assertEquals(124, result.exitCode());
            assertTrue(result.stderr().contains("Timed out"));
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (isAlive(pidFile)) Thread.sleep(10);
            });
        } finally {
            cleanUpProcess(pidFile);
        }
    }

    @Test
    void shouldTerminateProcessWhenCallingThreadIsInterrupted() throws Exception {
        Path pidFile = tempDir.resolve("interrupted.pid");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread caller = new Thread(() -> {
            try {
                runner.run(command("sleep", pidFile), Duration.ofMinutes(1));
                failure.set(new AssertionError("Expected interruption"));
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        try {
            caller.start();
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (!Files.exists(pidFile) || Files.size(pidFile) == 0) Thread.sleep(10);
            });
            caller.interrupt();
            caller.join(5_000);

            assertFalse(caller.isAlive(), "Interrupted caller should finish promptly");
            assertInstanceOf(InterruptedException.class, failure.get());
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (isAlive(pidFile)) Thread.sleep(10);
            });
        } finally {
            caller.interrupt();
            cleanUpProcess(pidFile);
            caller.join(5_000);
        }
    }

    private static List<String> command(String mode, Path pidFile) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        return List.of(java, "-cp", classpath, ProcessFixture.class.getName(), mode, pidFile.toString());
    }

    private static boolean isAlive(Path pidFile) throws Exception {
        if (!Files.exists(pidFile)) return false;
        long pid = Long.parseLong(Files.readString(pidFile));
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    private static void cleanUpProcess(Path pidFile) throws Exception {
        if (Files.exists(pidFile) && Files.size(pidFile) > 0) {
            ProcessHandle.of(Long.parseLong(Files.readString(pidFile))).ifPresent(ProcessHandle::destroyForcibly);
        }
    }

    public static class ProcessFixture {
        public static void main(String[] args) throws Exception {
            if ("output".equals(args[0])) {
                for (int i = 0; i < 20_000; i++) System.out.println("stdout-line-" + i);
                for (int i = 0; i < 5_000; i++) System.err.println("stderr-line-" + i);
            } else {
                Files.writeString(Path.of(args[1]), Long.toString(ProcessHandle.current().pid()));
                Thread.sleep(60_000);
            }
        }
    }
}
