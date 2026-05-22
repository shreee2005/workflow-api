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
class ApiIntegrationTests {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void startupCheck_corsAndAuthFromLocalhost() throws Exception {
        HttpRequest corsReq = HttpRequest.newBuilder(uri("/auth/login"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type, Idempotency-Key")
                .build();
        HttpResponse<String> corsResp = send(corsReq);
        assertEquals(200, corsResp.statusCode());
        assertEquals("http://localhost:5173", corsResp.headers().firstValue("Access-Control-Allow-Origin").orElse(null));

        HttpResponse<String> authResp = send(HttpRequest.newBuilder(uri("/api/workflows")).GET().build());
        assertEquals(401, authResp.statusCode());
        assertTrue(authResp.body().contains("\"code\":\"unauthorized\""));
    }

    @Test
    void authGroup_integration() throws Exception {
        HttpResponse<String> signup = send(jsonPost("/auth/signup", "{\"email\":\"frontend-user@local.test\",\"password\":\"Secret123!\",\"name\":\"Frontend User\"}"));
        assertEquals(201, signup.statusCode());

        HttpResponse<String> login = send(jsonPost("/auth/login", "{\"email\":\"frontend-user@local.test\",\"password\":\"Secret123!\"}"));
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"token\""));

        String token = extractJsonValue(login.body(), "token");
        HttpRequest setPassword = HttpRequest.newBuilder(uri("/auth/set-password"))
                .POST(HttpRequest.BodyPublishers.ofString("{\"newPassword\":\"NewSecret123!\"}"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
        HttpResponse<String> setPasswordResp = send(setPassword);
        assertEquals(200, setPasswordResp.statusCode());

        HttpResponse<String> linkPassword = send(jsonPost("/auth/link-password", "{\"email\":\"frontend-user@local.test\",\"verificationToken\":\"demo\",\"newPassword\":\"x\"}"));
        assertEquals(501, linkPassword.statusCode());
        assertTrue(linkPassword.body().contains("\"code\""));
    }

    @Test
    void workflowsGroup_routesExist() throws Exception {
        assertNot404(send(get("/api/workflows")));
        assertNot404(send(get("/api/workflows/" + UUID.randomUUID())));
        assertNot404(send(post("/api/workflows/" + UUID.randomUUID() + "/activate")));
        assertNot404(send(post("/api/workflows/" + UUID.randomUUID() + "/deactivate")));
        assertNot404(send(get("/api/workflows/" + UUID.randomUUID() + "/runs")));
        assertNot404(send(get("/api/runs/" + UUID.randomUUID())));
    }

    @Test
    void templatesGroup_routesExist() throws Exception {
        assertNot404(send(get("/api/templates")));
        assertNot404(send(get("/api/templates/" + UUID.randomUUID())));
        assertNot404(send(post("/api/templates")));
        assertNot404(send(put("/api/templates/" + UUID.randomUUID())));
        assertNot404(send(delete("/api/templates/" + UUID.randomUUID())));
        assertNot404(send(post("/api/templates/" + UUID.randomUUID() + "/instantiate")));
    }

    @Test
    void teamsAndKeysGroup_routesExist() throws Exception {
        UUID teamId = UUID.randomUUID();
        assertNot404(send(post("/api/teams")));
        assertNot404(send(get("/api/teams")));
        assertNot404(send(get("/api/teams/" + teamId + "/members")));
        assertNot404(send(post("/api/teams/" + teamId + "/invite")));
        assertNot404(send(post("/api/teams/" + teamId + "/invites/" + UUID.randomUUID() + "/accept")));
        assertNot404(send(post("/api/teams/" + teamId + "/keys")));
        assertNot404(send(get("/api/teams/" + teamId + "/keys")));
        assertNot404(send(post("/api/teams/" + teamId + "/keys/" + UUID.randomUUID() + "/revoke")));
    }

    @Test
    void teamsGroup_ownerAndAcceptedMemberCanListTeam() throws Exception {
        String ownerEmail = "team-owner@local.test";
        String ownerPassword = "OwnerPass123!";
        String memberEmail = "team-member@local.test";
        String memberPassword = "MemberPass123!";

        assertEquals(201, send(jsonPost("/auth/signup", "{\"email\":\"" + ownerEmail + "\",\"password\":\"" + ownerPassword + "\",\"name\":\"Owner\"}")).statusCode());
        assertEquals(201, send(jsonPost("/auth/signup", "{\"email\":\"" + memberEmail + "\",\"password\":\"" + memberPassword + "\",\"name\":\"Member\"}")).statusCode());

        String ownerToken = loginToken(ownerEmail, ownerPassword);
        String memberToken = loginToken(memberEmail, memberPassword);

        HttpResponse<String> createTeamResp = send(authJsonPost("/api/teams", "{\"name\":\"Invite Test Team\"}", ownerToken));
        assertEquals(201, createTeamResp.statusCode());
        String teamId = extractJsonValue(createTeamResp.body(), "teamId");
        assertNotNull(teamId);

        HttpResponse<String> inviteResp = send(authJsonPost("/api/teams/" + teamId + "/invite", "{\"email\":\"" + memberEmail + "\"}", ownerToken));
        assertEquals(200, inviteResp.statusCode());
        String inviteId = extractJsonValue(inviteResp.body(), "inviteId");
        assertNotNull(inviteId);

        HttpResponse<String> acceptResp = send(authJsonPost("/api/teams/" + teamId + "/invites/" + inviteId + "/accept", "{}", memberToken));
        assertEquals(200, acceptResp.statusCode());

        HttpResponse<String> listAsMember = send(authGet("/api/teams", memberToken));
        assertEquals(200, listAsMember.statusCode());
        assertNotEquals("[]", listAsMember.body().trim());
        assertTrue(listAsMember.body().contains("Invite Test Team"));

        HttpResponse<String> membersResp = send(authGet("/api/teams/" + teamId + "/members", memberToken));
        assertEquals(200, membersResp.statusCode());
        assertNotNull(membersResp.body());
    }

    @Test
    void hooksDebugAndUtilityGroup_routesExist() throws Exception {
        HttpResponse<String> hookResp = send(post("/hooks/" + UUID.randomUUID()));
        assertTrue(hookResp.statusCode() == 202 || hookResp.statusCode() == 404 || hookResp.statusCode() == 400);
        HttpResponse<String> callbackResp = send(post("/hooks/callback/correlation-1"));
        assertEquals(202, callbackResp.statusCode());
        assertTrue(send(get("/api/debug/verify")).statusCode() != 500);
        assertTrue(send(get("/api/debug/headers")).statusCode() != 500);
        assertEquals(200, send(get("/")).statusCode());
        assertTrue(send(get("/actuator/health")).statusCode() != 500);
        send(get("/actuator/info"));
        send(get("/actuator/prometheus"));
    }

    private HttpRequest jsonPost(String path, String json) {
        return HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
    }

    private HttpRequest get(String path) {
        return HttpRequest.newBuilder(uri(path)).GET().build();
    }

    private HttpRequest post(String path) {
        return HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();
    }

    private HttpRequest authGet(String path, String token) {
        return HttpRequest.newBuilder(uri(path))
                .GET()
                .header("Authorization", "Bearer " + token)
                .build();
    }

    private HttpRequest authJsonPost(String path, String json, String token) {
        return HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .build();
    }

    private HttpRequest put(String path) {
        return HttpRequest.newBuilder(uri(path))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();
    }

    private HttpRequest delete(String path) {
        return HttpRequest.newBuilder(uri(path))
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();
    }

    private HttpResponse<String> send(HttpRequest req) throws IOException, InterruptedException {
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private void assertNot404(HttpResponse<String> response) {
        assertNotEquals(404, response.statusCode());
    }

    private String extractJsonValue(String json, String key) {
        String tokenKey = "\"" + key + "\":\"";
        int start = json.indexOf(tokenKey);
        if (start < 0) return null;
        int from = start + tokenKey.length();
        int to = json.indexOf("\"", from);
        return to > from ? json.substring(from, to) : null;
    }

    private String loginToken(String email, String password) throws IOException, InterruptedException {
        HttpResponse<String> login = send(jsonPost("/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
        assertEquals(200, login.statusCode());
        String token = extractJsonValue(login.body(), "token");
        assertNotNull(token);
        return token;
    }
}
