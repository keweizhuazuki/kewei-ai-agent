package com.kiwi.keweiaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class KeweiManusTest {

    @Resource
    private KeweiManus manus;

    @Test
    public void run(){
        String userPrompt = """
                我的另一半居住在上海静安区，请帮我找到五公里内和值得约会地点，并结合一些网络图片，制定一份详细的约会计划
                """;

        String res = manus.run(userPrompt);
        System.out.println(res);
        Assertions.assertNotNull(res);
    }

}