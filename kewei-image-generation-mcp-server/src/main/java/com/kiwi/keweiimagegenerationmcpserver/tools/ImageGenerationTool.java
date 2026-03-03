package com.kiwi.keweiimagegenerationmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

@Service
public class ImageGenerationTool {

    private static final String TXT2IMG_API_PATH = "/sdapi/v1/txt2img";

    @Value("${drawthings.base-url:http://127.0.0.1:7860}")
    private String drawThingsBaseUrl;

    @Value("${drawthings.timeout-ms:180000}")
    private int drawThingsTimeoutMs;

    @Value("${drawthings.output-dir:tmp/generated-images}")
    private String outputDir;

    @Tool(description = "Generate an image by Draw Things and return saved image absolute file path", returnDirect = true)
    public String generateImage(
            @ToolParam(description = "positive prompt") String prompt,
            @ToolParam(description = "negative prompt") String negativePrompt,
            @ToolParam(description = "output image width") Integer width,
            @ToolParam(description = "output image height") Integer height,
            @ToolParam(description = "sampling steps") Integer steps,
            @ToolParam(description = "cfg scale") Double cfgScale,
            @ToolParam(description = "sampler name") String sampler
    ) {
        if (StrUtil.isBlank(prompt)) {
            return "prompt must not be blank";
        }

        JSONObject payload = new JSONObject();
        payload.set("prompt", prompt);
        payload.set("negative_prompt", StrUtil.blankToDefault(negativePrompt, ""));
        payload.set("width", width == null ? 512 : width);
        payload.set("height", height == null ? 512 : height);
        payload.set("steps", steps == null ? 20 : steps);
        payload.set("cfg_scale", cfgScale == null ? 7.0 : cfgScale);
        payload.set("sampler_name", StrUtil.blankToDefault(sampler, "Euler a"));

        String apiUrl = StrUtil.removeSuffix(drawThingsBaseUrl, "/") + TXT2IMG_API_PATH;

        try (HttpResponse response = HttpRequest.post(apiUrl)
                .contentType(ContentType.JSON.toString())
                .body(payload.toString())
                .timeout(drawThingsTimeoutMs)
                .execute()) {
            if (!response.isOk()) {
                return "draw things request failed: http " + response.getStatus();
            }

            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray images = body.getJSONArray("images");
            if (images == null || images.isEmpty()) {
                return "draw things response has no images";
            }

            String firstImage = images.getStr(0);
            if (StrUtil.isBlank(firstImage)) {
                return "draw things response first image is empty";
            }
            return saveImageToFile(firstImage);
        } catch (Exception e) {
            return "draw things request error: " + e.getMessage();
        }
    }

    private String saveImageToFile(String base64Image) {
        try {
            Path outputDirectory = Paths.get(outputDir);
            Files.createDirectories(outputDirectory);
            Path filePath = outputDirectory.resolve("drawthings-" + UUID.randomUUID() + ".png");
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            Files.write(filePath, imageBytes);
            return filePath.toAbsolutePath().toString();
        } catch (Exception e) {
            return "failed to save image: " + e.getMessage();
        }
    }
}
