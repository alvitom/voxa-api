package com.voxa.api.service;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendAccountVerification(String email, String username, String token) throws MessagingException;

    void sendResetPassword(String email, String username, String token) throws MessagingException;
}
