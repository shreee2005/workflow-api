package com.workflow.demo.service;

import java.util.UUID;

public interface EmailService {
    void sendInvitationEmail(String toEmail, String teamName, UUID teamId, UUID inviteId);
}
