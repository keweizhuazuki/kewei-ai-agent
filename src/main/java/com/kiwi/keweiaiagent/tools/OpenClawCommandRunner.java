package com.kiwi.keweiaiagent.tools;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * OpenClaw 命令执行接口，抽象外部研究命令的运行方式。
 */
public interface OpenClawCommandRunner {

    CommandResult run(List<String> command, Duration timeout) throws IOException, InterruptedException;

    /**
     * 命令执行结果对象，封装退出码、标准输出和错误输出。
     */
    record CommandResult(int exitCode, String stdout, String stderr) {
    }
}
