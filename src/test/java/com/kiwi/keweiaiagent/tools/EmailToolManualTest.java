package com.kiwi.keweiaiagent.tools;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 手动邮件发送测试，默认禁用，避免误发。
 */
class EmailToolManualTest {

    @Test
    @Disabled("Manual test only. Fill real SMTP properties and receiver, then remove @Disabled.")
    void sendEmailManually() {
        // 这里可改为你自己的 SMTP 参数，也可以在启动参数里传 -Dmail.smtp.xxx
        System.setProperty("mail.smtp.host", "smtp.example.com");
        System.setProperty("mail.smtp.port", "465");
        System.setProperty("mail.smtp.username", "your_email@example.com");
        System.setProperty("mail.smtp.password", "your_smtp_authorization_code");
        System.setProperty("mail.smtp.from", "your_email@example.com");
        System.setProperty("mail.smtp.ssl", "true");

        EmailTool tool = new EmailTool();
        String result = tool.sendEmail(
                "receiver@example.com",
                "Kewei AI Agent test mail",
                "This is a test email from EmailTool.",
                false
        );

        System.out.println(result);
    }
}
