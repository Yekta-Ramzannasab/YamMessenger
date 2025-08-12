package com.yamyam.messenger.server;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

public class EmailService {

    public static boolean sendVerificationCode(String recipientEmail, int code) {
        final String senderEmail = System.getenv("GMAIL_USER");
        final String senderPassword = System.getenv("GMAIL_APP_PASSWORD");

        if (senderEmail == null || senderPassword == null) {
            System.err.println("Email credentials are not set in environment variables (GMAIL_USER, GMAIL_APP_PASSWORD)");
            return false;
        }

        try {
            // Creating an Email Using the Builder Template
            Email email = EmailBuilder.startingBlank()
                    .from("YAM Messenger", senderEmail)
                    .to(recipientEmail)
                    .withSubject("Your YAM Messenger Verification Code")
                    .withPlainText("Hello,\n\nYour verification code is: " + code)
                    .buildEmail();

            // Creating a Mailer and Configuring It for Gmail
            Mailer mailer = MailerBuilder
                    .withSMTPServer("smtp.gmail.com", 587, senderEmail, senderPassword)
                    .withTransportStrategy(TransportStrategy.SMTP_TLS)
                    .buildMailer();

            // Sending Email
            mailer.sendMail(email);

            System.out.println("Verification email sent successfully to " + recipientEmail);
            return true;

        } catch (Exception e) {
            // The Simple Java Mail library throws its own errors.
            e.printStackTrace();
            return false;
        }
    }
}