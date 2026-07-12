package com.voxa.api.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

// @Service
@RequiredArgsConstructor
public class SmtpMailService implements MailService {
    private final JavaMailSender mailSender;

    @Override
    public void sendUserVerification(String email, String token) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);

        mimeMessageHelper.setTo(email);
        mimeMessageHelper.setSubject("User Verification");
        mimeMessageHelper.setText("""
                <h1>Hello user</h1>
                """, true);

        mailSender.send(mimeMessage);
    }
}
