package com.kiwi.keweiaiagent.tools;

import cn.hutool.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest
@ActiveProfiles({"local"})
class WebSearchToolTest {

    @Autowired
    private WebSearchTool tool;

    @Test
    void shouldReturnErrorWhenQueryMissing() {
        String result = tool.searchWebsite("", null, null, null, null, null);
        Assertions.assertTrue(result.contains("query 'q' is required"));
    }

    @Test
    void shouldBuildConciseResult() {
        JSONObject root = new JSONObject()
                .set("search_information", new JSONObject().set("total_results", 12345))
                .set("organic_results", new Object[]{
                        new JSONObject()
                                .set("title", "A")
                                .set("link", "https://a.com")
                                .set("snippet", "snippet a")
                });

        String result = tool.toConciseResult(root, "chatgpt");
        System.out.println(result);
        Assertions.assertTrue(result.contains("query: chatgpt"));
        Assertions.assertTrue(result.contains("1. A"));
        Assertions.assertTrue(result.contains("https://a.com"));
    }

    @Test
    void searchWebsite() {
            String result = tool.searchWebsite("chatgpt", null, null, null, null, null);
            System.out.println(result);
            Assertions.assertNotNull(result);
    }

    @Test
    void shouldNormalizeSearchArgs() {
        Assertions.assertEquals("zh-CN", tool.normalizeHl("zh"));
        Assertions.assertEquals("en", tool.normalizeHl("en"));
        Assertions.assertEquals("cn", tool.normalizeGl("CN"));
        Assertions.assertEquals("us", tool.normalizeGl("xx"));
        Assertions.assertEquals("Shanghai, China", tool.normalizeLocation("上海，中国"));
        Assertions.assertEquals(1, tool.normalizePage(0));
    }
}
