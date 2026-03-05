package com.kiwi.keweiimagegenerationmcpserver.tools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "drawthings.timeout-ms=300000")
@EnabledIfEnvironmentVariable(named = "RUN_DRAW_THINGS_E2E", matches = "true")
class ImageGenerationToolRealTest {

    @Autowired
    private ImageGenerationTool imageGenerationTool;

    @Test
    void generateImageShouldReturnImageFilePathFromRealDrawThings() throws Exception {
        String result = imageGenerationTool.generateImage(
                "a cute cat, high quality",
                "low quality, blurry",
                256,
                256,
                8,
                7.0,
                "Euler a"
        );
        System.out.println("ImageGenerationToolRealTest result: " + result);

        assertNotNull(result);
        assertFalse(result.isBlank());
        assertFalse(result.startsWith("draw things request"));
        assertFalse(result.startsWith("failed to save image"));

        Path imagePath = Path.of(result);
        assertTrue(Files.exists(imagePath));
        assertTrue(Files.size(imagePath) > 0);
    }
}
