package com.voxa.api.service;

import com.samskivert.mustache.Mustache;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@Profile(value = {"dev", "test"})
@RequiredArgsConstructor
public class LocalMailService implements MailService {
    private final JavaMailSender mailSender;

    private final Mustache.Compiler compiler;

    @Value("${spring.application.client-url}")
    private String clientUrl;

    @Value("${spring.application.mail.from}")
    private String emailFrom;

    @Override
    public void sendAccountVerification(String email, String username, String token) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

        String verificationUrl = String.format("%s/verify-account?token=%s", clientUrl, token);

        Map<String, String> data = Map.of(
                "username", username,
                "verificationUrl", verificationUrl
        );

        ClassPathResource resource = new ClassPathResource("templates/emails/verification-email.mustache");

        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            String html = compiler.compile(reader).execute(data);

            mimeMessageHelper.setFrom(emailFrom);
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSentDate(Date.from(Instant.now()));
            mimeMessageHelper.setSubject("Account Verification");
            mimeMessageHelper.setText(html, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mailSender.send(mimeMessage);
    }
}
