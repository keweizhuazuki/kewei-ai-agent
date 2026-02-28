package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class WebSearchTool {

    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";
    @Value("${search-api.api-key:${searchapi.key:}}")
    private String configuredApiKey;
    @Autowired(required = false)
    private Environment environment;

    @PostConstruct
    public void logKeyStatusAtStartup() {
        String key = resolveApiKey();
        String activeProfiles = environment == null ? "" : String.join(",", environment.getActiveProfiles());
        if (StrUtil.isBlank(key)) {
            log.warn("WebSearchTool init: SearchAPI key is missing. activeProfiles={}", activeProfiles);
            return;
        }
        log.info("WebSearchTool init: SearchAPI key loaded. length={}, activeProfiles={}", key.length(), activeProfiles);
    }

    @Tool(description = "Search websites by Google engine and return concise top web results",returnDirect = false)
    public String searchWebsite(
            @ToolParam(description = "Search query keywords") String q,
            @ToolParam(description = "Optional canonical location. Example: New York,United States") String location,
            @ToolParam(description = "Optional country code, default us") String gl,
            @ToolParam(description = "Optional language code, default en") String hl,
            @ToolParam(description = "Optional page number, default 1") Integer page,
            @ToolParam(description = "Optional time period filter: last_hour, last_day, last_week, last_month, last_year") String timePeriod
    ) {
        if (StrUtil.isBlank(q)) {
            return "Error: query 'q' is required.";
        }

        String apiKey = resolveApiKey();
        if (StrUtil.isBlank(apiKey)) {
            return "Error: missing SearchAPI key. Set config key searchapi.key (or search-api.api-key), env SEARCHAPI_API_KEY, or -Dsearchapi.api-key.";
        }

        HttpRequest request = HttpRequest.get(SEARCH_API_URL)
                .form("engine", "google")
                .form("q", q)
                .form("api_key", apiKey)
                .form("gl", StrUtil.blankToDefault(gl, "us"))
                .form("hl", StrUtil.blankToDefault(hl, "en"))
                .form("page", page == null || page < 1 ? 1 : page)
                .timeout(20_000);

        if (StrUtil.isNotBlank(location)) {
            request.form("location", location);
        }
        if (StrUtil.isNotBlank(timePeriod)) {
            request.form("time_period", timePeriod);
        }

        try (HttpResponse response = request.execute()) {
            if (response.getStatus() < 200 || response.getStatus() >= 300) {
                return "Search API request failed, status=" + response.getStatus() + ", body=" + response.body();
            }

            JSONObject root = JSONUtil.parseObj(response.body());
            return toConciseResult(root, q);
        } catch (Exception e) {
            return "Search failed: " + e.getMessage();
        }
    }

    String resolveApiKey() {
        String apiKey = configuredApiKey;
        if (StrUtil.isBlank(apiKey) && environment != null) {
            apiKey = environment.getProperty("searchapi.key");
        }
        if (StrUtil.isBlank(apiKey) && environment != null) {
            apiKey = environment.getProperty("search-api.api-key");
        }
        if (StrUtil.isBlank(apiKey)) {
            apiKey = System.getProperty("searchapi.api-key");
        }
        if (StrUtil.isBlank(apiKey)) {
            apiKey = System.getenv("SEARCHAPI_API_KEY");
        }
        return apiKey;
    }

    String toConciseResult(JSONObject root, String query) {
        List<String> lines = new ArrayList<>();
        lines.add("query: " + query);

        JSONObject info = root.getJSONObject("search_information");
        if (info != null) {
            Object total = info.get("total_results");
            if (total != null) {
                lines.add("total_results: " + total);
            }
        }

        JSONArray organic = root.getJSONArray("organic_results");
        if (organic == null || organic.isEmpty()) {
            lines.add("No organic results.");
            return String.join("\n", lines);
        }

        int max = Math.min(5, organic.size());
        lines.add("top_results:");
        for (int i = 0; i < max; i++) {
            JSONObject item = organic.getJSONObject(i);
            String title = item.getStr("title", "");
            String link = item.getStr("link", "");
            String snippet = item.getStr("snippet", "");
            lines.add((i + 1) + ". " + title);
            lines.add("   link: " + link);
            if (StrUtil.isNotBlank(snippet)) {
                lines.add("   snippet: " + snippet);
            }
        }
        return String.join("\n", lines);
    }
}
