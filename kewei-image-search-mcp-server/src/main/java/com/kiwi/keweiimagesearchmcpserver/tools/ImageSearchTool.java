package com.kiwi.keweiimagesearchmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ImageSearchTool {

    private static final String PEXELS_SEARCH_URL = "https://api.pexels.com/v1/search";

    @Value("${pexels.api-key}")
    private String pexelsApiKey;

    @Tool(description = "Search image from Pexels website based on a query")
    public String searchImage(@ToolParam(description = "search query keyword") String query) {
        if (StrUtil.isBlank(query)) {
            return "query must not be blank";
        }

        String apiKey = resolveApiKey();
        if (StrUtil.isBlank(apiKey)) {
            return "missing pexels api key, please set env PEXELS_API_KEY or config pexels.api-key";
        }

        try (HttpResponse response = HttpRequest.get(PEXELS_SEARCH_URL)
                .header("Authorization", apiKey)
                .form("query", query)
                .form("per_page", 10)
                .timeout(10000)
                .execute()) {
            if (!response.isOk()) {
                return "pexels request failed: http " + response.getStatus();
            }

            JSONObject body = JSONUtil.parseObj(response.body());
            JSONArray photos = body.getJSONArray("photos");
            if (photos == null || photos.isEmpty()) {
                return "";
            }

            List<String> mediumUrls = new ArrayList<>();
            for (Object photoObj : photos) {
                JSONObject photo = (JSONObject) photoObj;
                JSONObject src = photo.getJSONObject("src");
                if (src == null) {
                    continue;
                }
                String medium = src.getStr("medium");
                if (StrUtil.isNotBlank(medium)) {
                    mediumUrls.add(medium);
                }
            }

            return StrUtil.join("\n", mediumUrls);
        } catch (Exception e) {
            return "pexels request error: " + e.getMessage();
        }
    }

    private String resolveApiKey() {
        if (StrUtil.isNotBlank(pexelsApiKey)) {
            return pexelsApiKey;
        }
        return System.getenv("PEXELS_API_KEY");
    }
}
