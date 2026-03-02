package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class EmailToolTest {

    @Test
    void shouldReturnErrorWhenReceiverMissing() {
        EmailTool tool = new EmailTool();
        String result = tool.sendEmail("", "subject", "content", false);
        Assertions.assertTrue(result.contains("receiver email 'to' is required"));
    }

    @Test
    void shouldReturnErrorWhenReceiverInvalid() {
        EmailTool tool = new EmailTool();
        String result = tool.sendEmail("invalid-email", "subject", "content", false);
        Assertions.assertTrue(result.contains("email format is invalid"));
    }
}
