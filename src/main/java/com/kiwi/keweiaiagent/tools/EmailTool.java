package com.kiwi.keweiaiagent.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailTool {

    @Value("${mail.smtp.host:}")
    private String configuredHost;
    @Value("${mail.smtp.port:}")
    private String configuredPort;
    @Value("${mail.smtp.username:}")
    private String configuredUsername;
    @Value("${mail.smtp.password:}")
    private String configuredPassword;
    @Value("${mail.smtp.from:}")
    private String configuredFrom;
    @Value("${mail.smtp.ssl:}")
    private String configuredSsl;

    @Tool(description = "Send an email to a user via SMTP config from Spring properties or env/system properties", returnDirect = false)
    public String sendEmail(
            @ToolParam(description = "Receiver email address") String to,
            @ToolParam(description = "Email subject") String subject,
            @ToolParam(description = "Email body content") String content,
            @ToolParam(description = "Whether body is HTML, default false") Boolean html
    ) {
        if (StrUtil.isBlank(to)) {
            return "Error: receiver email 'to' is required.";
        }
        if (!Validator.isEmail(to)) {
            return "Error: receiver email format is invalid.";
        }
        if (StrUtil.isBlank(subject)) {
            return "Error: email subject is required.";
        }
        if (StrUtil.isBlank(content)) {
            return "Error: email content is required.";
        }

        String host = readConfig(configuredHost, "mail.smtp.host", "MAIL_SMTP_HOST", "SMTP_HOST");
        String portText = readConfig(configuredPort, "mail.smtp.port", "MAIL_SMTP_PORT", "SMTP_PORT");
        String username = readConfig(configuredUsername, "mail.smtp.username", "MAIL_SMTP_USERNAME", "SMTP_USERNAME");
        String password = readConfig(configuredPassword, "mail.smtp.password", "MAIL_SMTP_PASSWORD", "SMTP_PASSWORD");
        String from = readConfig(configuredFrom, "mail.smtp.from", "MAIL_SMTP_FROM", "SMTP_FROM");
        String sslText = readConfig(configuredSsl, "mail.smtp.ssl", "MAIL_SMTP_SSL", "SMTP_SSL");

        if (StrUtil.hasBlank(host, username, password, from)) {
            return "Error: missing SMTP config. Required: mail.smtp.host, mail.smtp.username, mail.smtp.password, mail.smtp.from"
                    + " (or env MAIL_SMTP_HOST/MAIL_SMTP_USERNAME/MAIL_SMTP_PASSWORD/MAIL_SMTP_FROM)."
                    + " Current loaded status => host=" + maskPresent(host)
                    + ", username=" + maskPresent(username)
                    + ", passwordSet=" + maskPresent(password)
                    + ", from=" + maskPresent(from);
        }

        int port = parsePortOrDefault(portText, 465);
        boolean ssl = parseBooleanOrDefault(sslText, true);

        MailAccount account = new MailAccount();
        account.setHost(host);
        account.setPort(port);
        account.setAuth(true);
        account.setUser(username);
        account.setPass(password);
        account.setFrom(from);
        account.setSslEnable(ssl);

        try {
            String messageId = MailUtil.send(account, to, subject, content, Boolean.TRUE.equals(html));
            return "Email sent successfully. to=" + to + ", messageId=" + messageId;
        } catch (Exception e) {
            return "Error sending email: " + e.getMessage();
        }
    }

    private String readConfig(String primaryValue, String propertyKey, String... envKeys) {
        if (StrUtil.isNotBlank(primaryValue)) {
            return primaryValue;
        }
        String value = System.getProperty(propertyKey);
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        for (String envKey : envKeys) {
            value = System.getenv(envKey);
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private int parsePortOrDefault(String portText, int defaultPort) {
        if (StrUtil.isBlank(portText)) {
            return defaultPort;
        }
        try {
            return Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            return defaultPort;
        }
    }

    private boolean parseBooleanOrDefault(String value, boolean defaultValue) {
        if (StrUtil.isBlank(value)) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private String maskPresent(String value) {
        return StrUtil.isBlank(value) ? "missing" : "set";
    }
}
