package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PptWriterToolTest {

    @TempDir
    Path tempDir;

    @Test
    void createPptxFromFlatSlidesMarkdown() throws Exception {
        PptWriterTool tool = new PptWriterTool();
        Path output = tempDir.resolve("love.pptx");

        String slidesMarkdown = """
                ## 送礼物
                - 选择符合她喜好的礼物
                - 注重仪式感和时机
                Notes: 强调礼物重点不在贵，而在用心。

                ## 制造惊喜
                - 提前准备小惊喜
                - 结合她近期情绪状态
                Notes: 惊喜要自然，不要造成压力。
                """;

        Map<String, Object> result = tool.create_pptx(
                new PptWriterTool.PptSpec("如何让女朋友更爱我", slidesMarkdown, output.toString())
        );

        assertEquals(true, result.get("success"));
        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
        assertEquals(output.toAbsolutePath().toString(), result.get("path"));
        assertEquals(3, result.get("slides"));
    }
}
