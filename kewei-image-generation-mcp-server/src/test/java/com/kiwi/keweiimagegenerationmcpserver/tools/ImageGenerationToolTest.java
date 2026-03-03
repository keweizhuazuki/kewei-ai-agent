package com.kiwi.keweiimagegenerationmcpserver.tools;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageGenerationToolTest {

    private HttpServer server;
    private ImageGenerationTool tool;

    @BeforeEach
    void setUp() {
        tool = new ImageGenerationTool();
        ReflectionTestUtils.setField(tool, "outputDir", "target/generated-images-test");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void generateImageShouldRejectBlankPrompt() {
        String result = tool.generateImage("   ", null, null, null, null, null, null);
        assertEquals("prompt must not be blank", result);
    }

    @Test
    void generateImageShouldReturnSavedImagePathWhenApiRespondsSuccessfully() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sdapi/v1/txt2img", exchange -> {
            String json = "{\"images\":[\"aGVsbG8=\"],\"info\":\"ok\"}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        ReflectionTestUtils.setField(tool, "drawThingsBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

        String result = tool.generateImage("a cat", null, 512, 512, 20, 7.0, "Euler a");

        Path imagePath = Path.of(result);
        assertTrue(Files.exists(imagePath));
        assertEquals("hello", Files.readString(imagePath));
    }

    @Test
    void generateImageShouldReturnErrorWhenApiStatusIsNotOk() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/sdapi/v1/txt2img", exchange -> {
            byte[] body = "bad request".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(400, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        ReflectionTestUtils.setField(tool, "drawThingsBaseUrl", "http://127.0.0.1:" + server.getAddress().getPort());

        String result = tool.generateImage("a dog", null, null, null, null, null, null);

        assertTrue(result.contains("draw things request failed"));
        assertTrue(result.contains("400"));
    }
}
