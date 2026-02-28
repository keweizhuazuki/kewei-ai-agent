package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ResourceDownloadToolTest {

    @Test
    void shouldReturnErrorWhenUrlMissing() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String result = tool.downloadResource("", "a.txt");
        Assertions.assertTrue(result.contains("url is required"));
    }

    @Test
    void shouldReturnErrorWhenFileNameMissing() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String result = tool.downloadResource("https://example.com", "");
        Assertions.assertTrue(result.contains("fileName is required"));
    }
}
