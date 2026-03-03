package com.kiwi.keweiimagesearchmcpserver.tools;


import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
     void testImageSearch() {
        String query = "猫咪";
        String result = imageSearchTool.searchImage(query);
        assert result != null && !result.isEmpty();
     }
}
