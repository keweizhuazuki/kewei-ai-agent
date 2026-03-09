package com.kiwi.keweiaiagent.tools;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public interface OpenClawCommandRunner {

    CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException;

    record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
