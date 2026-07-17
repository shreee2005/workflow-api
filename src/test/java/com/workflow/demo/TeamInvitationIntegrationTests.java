package com.workflow.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TeamInvitationIntegrationTests {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void teamInvitationLifecycle_test() throws Exception {
        // 1. Create Owner & Member Accounts
        String ownerEmail = "owner-" + UUID.randomUUID() + "@local.test";
        String memberEmail = "member-" + UUID.randomUUID() + "@local.test";

        assertEquals(201, send(jsonPost("/auth/signup", 
                "{\"email\":\"" + ownerEmail + "\",\"password\":\"Pass123!\",\"name\":\"Owner User\"}")).statusCode());
        assertEquals(201, send(jsonPost("/auth/signup", 
                "{\"email\":\"" + memberEmail + "\",\"password\":\"Pass123!\",\"name\":\"Invited User\"}")).statusCode());

        String ownerToken = loginToken(ownerEmail, "Pass123!");
        String memberToken = loginToken(memberEmail, "Pass123!");

        // 2. Owner creates a new team
        HttpResponse<String> createTeamResp = send(authJsonPost("/api/teams", 
                "{\"name\":\"Collab Sandbox\"}", ownerToken));
        assertEquals(201, createTeamResp.statusCode());
        String teamId = extractJsonValue(createTeamResp.body(), "teamId");
        assertNotNull(teamId);

        // 3. Owner invites Member to the team
        HttpResponse<String> inviteResp = send(authJsonPost("/api/teams/" + teamId + "/invite", 
                "{\"email\":\"" + memberEmail + "\"}", ownerToken));
        assertEquals(200, inviteResp.statusCode());
        String inviteId = extractJsonValue(inviteResp.body(), "inviteId");
        assertNotNull(inviteId);

        // 4. Member queries pending invitations list
        HttpRequest listReq = HttpRequest.newBuilder(uri("/api/teams/invitations"))
                .header("Authorization", "Bearer " + memberToken)
                .GET().build();
        HttpResponse<String> listResp = send(listReq);
        assertEquals(200, listResp.statusCode());
        assertTrue(listResp.body().contains("Collab Sandbox"));
        assertTrue(listResp.body().contains(inviteId));

        // 5. Member declines the invitation
        HttpResponse<String> declineResp = send(authJsonPost("/api/teams/" + teamId + "/invites/" + inviteId + "/decline", 
                "{}", memberToken));
        assertEquals(200, declineResp.statusCode());
        assertTrue(declineResp.body().contains("REMOVED"));

        // 6. Member queries invitations list again, should be empty
        HttpResponse<String> listRespAfter = send(listReq);
        assertEquals(200, listRespAfter.statusCode());
        assertEquals("[]", listRespAfter.body().trim());
    }

    @Test
    void teamInvitationPendingAndCancel_test() throws Exception {
        // 1. Create Owner & Member Accounts
        String ownerEmail = "owner-cancel-" + UUID.randomUUID() + "@local.test";
        String memberEmail = "member-cancel-" + UUID.randomUUID() + "@local.test";

        assertEquals(201, send(jsonPost("/auth/signup", 
                "{\"email\":\"" + ownerEmail + "\",\"password\":\"Pass123!\",\"name\":\"Owner User\"}")).statusCode());
        assertEquals(201, send(jsonPost("/auth/signup", 
                "{\"email\":\"" + memberEmail + "\",\"password\":\"Pass123!\",\"name\":\"Invited User\"}")).statusCode());

        String ownerToken = loginToken(ownerEmail, "Pass123!");

        // 2. Owner creates a new team
        HttpResponse<String> createTeamResp = send(authJsonPost("/api/teams", 
                "{\"name\":\"Pending Cancel Sandbox\"}", ownerToken));
        assertEquals(201, createTeamResp.statusCode());
        String teamId = extractJsonValue(createTeamResp.body(), "teamId");
        assertNotNull(teamId);

        // 3. Owner invites Member to the team
        HttpResponse<String> inviteResp = send(authJsonPost("/api/teams/" + teamId + "/invite", 
                "{\"email\":\"" + memberEmail + "\"}", ownerToken));
        assertEquals(200, inviteResp.statusCode());
        String inviteId = extractJsonValue(inviteResp.body(), "inviteId");
        assertNotNull(inviteId);

        // 4. Owner fetches pending invitations list for the team
        HttpRequest pendingListReq = HttpRequest.newBuilder(uri("/api/teams/" + teamId + "/invites/pending"))
                .header("Authorization", "Bearer " + ownerToken)
                .GET().build();
        HttpResponse<String> pendingListResp = send(pendingListReq);
        assertEquals(200, pendingListResp.statusCode());
        assertTrue(pendingListResp.body().contains(memberEmail));
        assertTrue(pendingListResp.body().contains(inviteId));

        // 5. Owner cancels the pending invitation
        HttpRequest cancelReq = HttpRequest.newBuilder(uri("/api/teams/" + teamId + "/invites/" + inviteId))
                .header("Authorization", "Bearer " + ownerToken)
                .DELETE().build();
        HttpResponse<String> cancelResp = send(cancelReq);
        assertEquals(204, cancelResp.statusCode());

        // 6. Owner queries pending invitations list again, should be empty
        HttpResponse<String> pendingListRespAfter = send(pendingListReq);
        assertEquals(200, pendingListRespAfter.statusCode());
        assertEquals("[]", pendingListRespAfter.body().trim());
    }

    private HttpRequest jsonPost(String path, String json) {
        return HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
    }

    private HttpRequest authJsonPost(String path, String json, String token) {
        return HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
    }

    private HttpResponse<String> send(HttpRequest req) throws IOException, InterruptedException {
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String loginToken(String email, String password) throws IOException, InterruptedException {
        HttpResponse<String> login = send(jsonPost("/auth/login", 
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
        assertEquals(200, login.statusCode());
        String token = extractJsonValue(login.body(), "token");
        assertNotNull(token);
        return token;
    }

    private String extractJsonValue(String json, String key) {
        String tokenKey = "\"" + key + "\":\"";
        int start = json.indexOf(tokenKey);
        if (start < 0) return null;
        int from = start + tokenKey.length();
        int to = json.indexOf("\"", from);
        return to > from ? json.substring(from, to) : null;
    }
}
