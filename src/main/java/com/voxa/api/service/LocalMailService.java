package com.voxa.api.service;

import com.samskivert.mustache.Mustache;
import com.voxa.api.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
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

    private final MailProperties mailProperties;

    @Override
    public void sendAccountVerification(String email, String username, String token) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

        String verificationUrl = String.format("%s/verify-account?token=%s", mailProperties.targetUrl(), token);

        Map<String, String> data = Map.of(
                "username", username,
                "verificationUrl", verificationUrl
        );

        ClassPathResource resource = new ClassPathResource("templates/emails/verification-email.mustache");

        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            String html = compiler.compile(reader).execute(data);

            mimeMessageHelper.setFrom(mailProperties.sender());
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSentDate(Date.from(Instant.now()));
            mimeMessageHelper.setSubject("Account Verification");
            mimeMessageHelper.setText(html, true);
        } catch (IOException exception) {
            throw new RuntimeException(String.format("Something goes wrong when send the email %s", exception.getMessage()));
        }

        mailSender.send(mimeMessage);
    }

    @Override
    public void sendResetPassword(String email, String username, String token) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

        String url = String.format("%s/reset-password/%s", mailProperties.targetUrl(), token);

        ClassPathResource resource = new ClassPathResource("templates/emails/reset-password.mustache");

        Map<String, String> data = Map.of(
                "username", username,
                "resetPasswordUrl", url
        );

        try (Reader reader = new InputStreamReader(resource.getInputStream())) {
            String html = compiler.compile(reader).execute(data);

            mimeMessageHelper.setFrom(mailProperties.sender());
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSentDate(Date.from(Instant.now()));
            mimeMessageHelper.setSubject("Reset Password Request");
            mimeMessageHelper.setText(html, true);
        } catch (IOException exception) {
            throw new RuntimeException(String.format("Something goes wrong when send the email %s", exception.getMessage()));
        }

        mailSender.send(mimeMessage);
    }
}
