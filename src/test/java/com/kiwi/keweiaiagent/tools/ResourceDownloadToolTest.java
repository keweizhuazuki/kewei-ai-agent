package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ResourceDownloadToolTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnErrorWhenUrlMissing() {
        ResourceDownloadTool tool = new ResourceDownloadTool(tempDir);
        String result = tool.downloadResource("", "a.txt");
        Assertions.assertTrue(result.contains("url is required"));
    }

    @Test
    void shouldReturnErrorWhenFileNameMissing() {
        ResourceDownloadTool tool = new ResourceDownloadTool(tempDir);
        String result = tool.downloadResource("https://example.com", "");
        Assertions.assertTrue(result.contains("fileName is required"));
    }

    @Test
    void shouldDownloadNestedFileAndRejectEscapesBeforeMakingRequests() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("downloads"));
        Path outside = Files.writeString(tempDir.resolve("outside.txt"), "original");
        Files.createSymbolicLink(root.resolve("linked"), tempDir);
        ResourceDownloadTool tool = new ResourceDownloadTool(root);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] content = "下载内容".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, content.length);
            try (var response = exchange.getResponseBody()) {
                response.write(content);
            }
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/resource";
            for (String name : new String[] {"../outside.txt", outside.toString(), ".", "linked/outside.txt"}) {
                assertTrue(tool.downloadResource(url, name).startsWith("Error downloading resource:"));
            }
            assertEquals(0, requests.get());
            assertEquals("original", Files.readString(outside));

            assertTrue(tool.downloadResource(url, "nested/resource.txt").startsWith("Resource downloaded successfully"));
            assertEquals(1, requests.get());
            assertEquals("下载内容", Files.readString(root.resolve("nested/resource.txt")));
        } finally {
            server.stop(0);
        }
    }
}
