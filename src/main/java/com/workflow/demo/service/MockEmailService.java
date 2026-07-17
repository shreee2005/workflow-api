package com.workflow.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.email.provider", havingValue = "mock", matchIfMissing = true)
public class MockEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    private final String frontendUrl;

    public MockEmailService(@Value("${frontend.url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendInvitationEmail(String toEmail, String teamName, UUID teamId, UUID inviteId) {
        String invitationUrl = String.format("%s/teams/%s/invites/%s/accept", frontendUrl, teamId, inviteId);

        log.info("\n" +
                "========================================================================\n" +
                "✉️  [MOCK EMAIL SENT]\n" +
                "To:      {}\n" +
                "Subject: Join the team \"{}\" on WorkFlow!\n" +
                "Body:\n" +
                "  Hello!\n" +
                "  You have been invited to join the team \"{}\" on WorkFlow.\n" +
                "  Please accept the invitation by clicking the link below:\n\n" +
                "  {}\n" +
                "========================================================================",
                toEmail, teamName, teamName, invitationUrl);
    }
}
