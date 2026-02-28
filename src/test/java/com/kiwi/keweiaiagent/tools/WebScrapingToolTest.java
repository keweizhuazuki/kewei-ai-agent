package com.kiwi.keweiaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WebScrapingToolTest {

    @Test
    void shouldReturnErrorWhenUrlInvalid() {
        WebScrapingTool tool = new WebScrapingTool();
        String result = tool.scrapeWebsite("ftp://baidu.com", null, null);
        Assertions.assertTrue(result.contains("only http/https URLs"));
    }

    @Test
    void shouldBuildScrapeResultFromDocument() {
        WebScrapingTool tool = new WebScrapingTool();
        Document doc = Jsoup.parse("""
                <html>
                  <head>
                    <title>Demo Page</title>
                    <meta name="description" content="desc"/>
                  </head>
                  <body>
                    <h1>Hello</h1>
                    <p>World content</p>
                    <a href="https://example.com/a">A Link</a>
                  </body>
                </html>
                """, "https://example.com");
        String result = tool.buildScrapeResult("https://example.com", doc, 200, true);

        Assertions.assertTrue(result.contains("title: Demo Page"));
        Assertions.assertTrue(result.contains("description: desc"));
        Assertions.assertTrue(result.contains("A Link -> https://example.com/a"));
    }
}
