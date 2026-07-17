package com.workflow.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final String fromAddress;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${frontend.url:http://localhost:5173}") String frontendUrl,
                            @Value("${spring.mail.from:noreply@workflow.app}") String fromAddress) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendInvitationEmail(String toEmail, String teamName, UUID teamId, UUID inviteId) {
        String invitationUrl = String.format("%s/teams/%s/invites/%s/accept", frontendUrl, teamId, inviteId);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Join the team \"" + teamName + "\" on WorkFlow!");
            message.setText("Hello!\n\n" +
                    "You have been invited to join the team \"" + teamName + "\" on WorkFlow.\n" +
                    "Please accept the invitation by clicking the link below:\n\n" +
                    invitationUrl + "\n\n" +
                    "Thanks,\nThe WorkFlow Team");

            mailSender.send(message);
            log.info("Sent SMTP invitation email to {}", toEmail);
        } catch (Exception e) {
            // Log the error but do NOT throw — the invitation was already persisted.
            // Throwing would roll back the @Transactional in TeamService and the owner
            // would see an error even though the invite row was saved.
            log.error("Failed to send SMTP invitation email to {} — invite was saved but email delivery failed", toEmail, e);
        }
    }
}
