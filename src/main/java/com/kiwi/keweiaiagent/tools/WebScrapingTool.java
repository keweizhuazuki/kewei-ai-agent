package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebScrapingTool {

    private static final int DEFAULT_TIMEOUT_MS = 20_000;
    private static final int DEFAULT_MAX_TEXT_LENGTH = 4000;

    @Tool(description = "Scrape a webpage and return title, summary text and optional links",returnDirect = false)
    public String scrapeWebsite(
            @ToolParam(description = "The webpage URL to scrape. Must start with http:// or https://") String url,
            @ToolParam(description = "Max plain-text length for main content, default 4000") Integer maxTextLength,
            @ToolParam(description = "Whether to include top links from the page, default false") Boolean includeLinks
    ) {
        if (StrUtil.isBlank(url)) {
            return "Error: url is required.";
        }
        if (!isSupportedUrl(url)) {
            return "Error: only http/https URLs are supported.";
        }

        int finalMaxTextLength = (maxTextLength == null || maxTextLength <= 0) ? DEFAULT_MAX_TEXT_LENGTH : maxTextLength;
        boolean finalIncludeLinks = Boolean.TRUE.equals(includeLinks);

        try {
            Connection connection = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (compatible; KeweiAiAgent/1.0)")
                    .timeout(DEFAULT_TIMEOUT_MS)
                    .followRedirects(true);

            Document doc = connection.get();
            return buildScrapeResult(url, doc, finalMaxTextLength, finalIncludeLinks);
        } catch (Exception e) {
            return "Web scraping failed: " + e.getMessage();
        }
    }

    private boolean isSupportedUrl(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception ignored) {
            return false;
        }
    }

    String buildScrapeResult(String url, Document doc, int maxTextLength, boolean includeLinks) {
        List<String> lines = new ArrayList<>();
        lines.add("url: " + url);

        String title = doc.title();
        if (StrUtil.isNotBlank(title)) {
            lines.add("title: " + title);
        }

        String description = "";
        Element metaDescription = doc.selectFirst("meta[name=description]");
        if (metaDescription != null) {
            description = metaDescription.attr("content");
        }
        if (StrUtil.isNotBlank(description)) {
            lines.add("description: " + description);
        }

        String text = doc.body() == null ? "" : doc.body().text();
        text = StrUtil.trim(text);
        if (StrUtil.isBlank(text)) {
            lines.add("content: (empty)");
        } else {
            String trimmed = text.length() > maxTextLength ? text.substring(0, maxTextLength) + "..." : text;
            lines.add("content:");
            lines.add(trimmed);
        }

        if (includeLinks) {
            Elements anchors = doc.select("a[href]");
            int maxLinks = Math.min(10, anchors.size());
            lines.add("links:");
            for (int i = 0; i < maxLinks; i++) {
                Element a = anchors.get(i);
                String href = a.absUrl("href");
                if (StrUtil.isBlank(href)) {
                    href = a.attr("href");
                }
                String textLabel = StrUtil.blankToDefault(StrUtil.trim(a.text()), "(no text)");
                lines.add((i + 1) + ". " + textLabel + " -> " + href);
            }
            if (maxLinks == 0) {
                lines.add("(none)");
            }
        }

        return String.join("\n", lines);
    }
}
