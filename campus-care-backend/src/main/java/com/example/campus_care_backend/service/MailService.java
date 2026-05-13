package com.example.campus_care_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class MailService {
    @Value("${app.mail.host:}")
    private String host;

    @Value("${app.mail.username:}")
    private String username;

    @Value("${app.mail.password:}")
    private String password;

    @Value("${app.mail.from:}")
    private String from;

    public void sendPlainText(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        String fromEffective = (from != null && !from.isBlank()) ? from.trim() : username;
        if (fromEffective != null && !fromEffective.isBlank()) msg.setFrom(fromEffective);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);

        Exception first = null;
        try {
            buildSenderSsl465().send(msg);
            return;
        } catch (Exception e) {
            first = e;
        }
        try {
            buildSenderStartTls587().send(msg);
        } catch (Exception second) {
            if (first != null) second.addSuppressed(first);
            throw second;
        }
    }

    private JavaMailSenderImpl buildSenderSsl465() {
        JavaMailSenderImpl sender = baseSender(465);
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.ssl.enable", "true");
        p.put("mail.smtp.starttls.enable", "false");
        return sender;
    }

    private JavaMailSenderImpl buildSenderStartTls587() {
        JavaMailSenderImpl sender = baseSender(587);
        Properties p = sender.getJavaMailProperties();
        p.put("mail.smtp.ssl.enable", "false");
        p.put("mail.smtp.starttls.enable", "true");
        return sender;
    }

    private JavaMailSenderImpl baseSender(int port) {
        if (host == null || host.isBlank()) throw new IllegalStateException("app.mail.host is required");
        if (username == null || username.isBlank()) throw new IllegalStateException("app.mail.username is required");
        if (password == null || password.isBlank()) throw new IllegalStateException("app.mail.password is required");

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        sender.setUsername(username.trim());
        sender.setPassword(password);

        Properties p = new Properties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.timeout", "10000");
        p.put("mail.smtp.connectiontimeout", "10000");
        p.put("mail.smtp.writetimeout", "10000");
        sender.setJavaMailProperties(p);
        return sender;
    }
}
