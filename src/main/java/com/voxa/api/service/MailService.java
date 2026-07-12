package com.voxa.api.service;

import jakarta.mail.MessagingException;

public interface MailService {
    void sendUserVerification(String email, String token) throws MessagingException;
}
