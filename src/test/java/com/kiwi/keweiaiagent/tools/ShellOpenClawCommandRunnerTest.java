package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;

class ShellOpenClawCommandRunnerTest {

    @Test
    void shouldDrainLargeStdoutAndStderrWithoutBlocking() throws Exception {
        ShellOpenClawCommandRunner runner = new ShellOpenClawCommandRunner(new File(System.getProperty("user.dir")));

        OpenClawCommandRunner.CommandResult result = runner.run(
                List.of(
                        "/bin/zsh",
                        "-lc",
                        "awk 'BEGIN{for(i=0;i<20000;i++) print \"stdout-line-\" i; for(i=0;i<5000;i++) print \"stderr-line-\" i > \"/dev/stderr\"}'"
                ),
                Duration.ofSeconds(10)
        );

        Assertions.assertEquals(0, result.exitCode());
        Assertions.assertTrue(result.stdout().contains("stdout-line-19999"));
        Assertions.assertTrue(result.stderr().contains("stderr-line-4999"));
    }
}
