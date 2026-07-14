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
class PluginIntegrationTests {

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void publicPlugins_accessibleAnonymously() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(uri("/plugins")).GET().build();
        HttpResponse<String> resp = send(req);

        assertEquals(200, resp.statusCode());
        String body = resp.body();
        assertTrue(body.contains("\"key\":\"log\""));
        assertTrue(body.contains("\"key\":\"http_call\""));
        assertTrue(body.contains("\"key\":\"wait\""));
        assertTrue(body.contains("\"name\":\"Log Message\""));
    }

    @Test
    void pluginLifecycle_authenticated() throws Exception {
        // 1. Signup & Login to get token
        String email = "plugin-test-" + UUID.randomUUID() + "@local.test";
        HttpResponse<String> signup = send(jsonPost("/auth/signup", 
                "{\"email\":\"" + email + "\",\"password\":\"Pass123!\",\"name\":\"Plugin Tester\"}"));
        assertEquals(201, signup.statusCode());

        HttpResponse<String> login = send(jsonPost("/auth/login", 
                "{\"email\":\"" + email + "\",\"password\":\"Pass123!\"}"));
        assertEquals(200, login.statusCode());
        String token = extractJsonValue(login.body(), "token");
        assertNotNull(token);

        // 2. Fetch authenticated plugins list
        HttpRequest listReq = HttpRequest.newBuilder(uri("/api/plugins"))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> listResp = send(listReq);
        assertEquals(200, listResp.statusCode());
        assertTrue(listResp.body().contains("\"key\":\"log\""));

        // 3. Register a new custom plugin
        String customPluginKey = "custom_step_" + UUID.randomUUID().toString().substring(0, 8);
        String createPayload = "{"
                + "\"key\":\"" + customPluginKey + "\","
                + "\"name\":\"Custom Test Step\","
                + "\"description\":\"A plugin registered during integration tests\","
                + "\"category\":\"Utility\","
                + "\"icon\":\"cpu\","
                + "\"configSchema\":\"{\\\"type\\\":\\\"object\\\"}\","
                + "\"active\":true"
                + "}";
        HttpRequest createReq = HttpRequest.newBuilder(uri("/api/plugins"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createPayload))
                .build();
        HttpResponse<String> createResp = send(createReq);
        assertEquals(201, createResp.statusCode());
        String createdId = extractJsonValue(createResp.body(), "id");
        assertNotNull(createdId);

        // 4. Verify custom plugin appears in public catalog
        HttpRequest publicCatalogReq = HttpRequest.newBuilder(uri("/plugins")).GET().build();
        HttpResponse<String> publicCatalogResp = send(publicCatalogReq);
        assertTrue(publicCatalogResp.body().contains(customPluginKey));

        // 5. Update the plugin name
        String updatePayload = "{"
                + "\"key\":\"" + customPluginKey + "\","
                + "\"name\":\"Updated Test Step Name\","
                + "\"category\":\"Utility\","
                + "\"active\":true"
                + "}";
        HttpRequest updateReq = HttpRequest.newBuilder(uri("/api/plugins/" + createdId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updatePayload))
                .build();
        HttpResponse<String> updateResp = send(updateReq);
        assertEquals(200, updateResp.statusCode());
        assertTrue(updateResp.body().contains("Updated Test Step Name"));

        // 6. Get single plugin detail
        HttpRequest getReq = HttpRequest.newBuilder(uri("/api/plugins/" + createdId))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> getResp = send(getReq);
        assertEquals(200, getResp.statusCode());
        assertTrue(getResp.body().contains("Updated Test Step Name"));

        // 7. Delete the plugin
        HttpRequest deleteReq = HttpRequest.newBuilder(uri("/api/plugins/" + createdId))
                .header("Authorization", "Bearer " + token)
                .DELETE().build();
        HttpResponse<String> deleteResp = send(deleteReq);
        assertEquals(204, deleteResp.statusCode()); // standard NO_CONTENT

        // 8. Verify the plugin is deleted
        HttpRequest getDeletedReq = HttpRequest.newBuilder(uri("/api/plugins/" + createdId))
                .header("Authorization", "Bearer " + token)
                .GET().build();
        HttpResponse<String> getDeletedResp = send(getDeletedReq);
        assertEquals(404, getDeletedResp.statusCode());
    }

    @Test
    void workflowSpecValidation_checkSupportedStepTypes() throws Exception {
        // 1. Signup & Login
        String email = "val-test-" + UUID.randomUUID() + "@local.test";
        send(jsonPost("/auth/signup", 
                "{\"email\":\"" + email + "\",\"password\":\"Pass123!\",\"name\":\"Validator Tester\"}"));
        HttpResponse<String> login = send(jsonPost("/auth/login", 
                "{\"email\":\"" + email + "\",\"password\":\"Pass123!\"}"));
        String token = extractJsonValue(login.body(), "token");

        // 2. Try creating a workflow with VALID step type (e.g. LOG or HTTP_CALL)
        String validWorkflowPayload = "{"
                + "\"name\":\"Valid Workflow\","
                + "\"active\":true,"
                + "\"spec\":\"{\\\"steps\\\":[{\\\"type\\\":\\\"LOG\\\",\\\"config\\\":{\\\"message\\\":\\\"Test\\\"}}]}\""
                + "}";
        HttpRequest validReq = HttpRequest.newBuilder(uri("/api/workflows"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(validWorkflowPayload))
                .build();
        HttpResponse<String> validResp = send(validReq);
        assertEquals(200, validResp.statusCode());

        // 3. Try creating a workflow with INVALID step type (e.g. UNSUPPORTED_XYZ)
        String invalidWorkflowPayload = "{"
                + "\"name\":\"Invalid Workflow\","
                + "\"active\":true,"
                + "\"spec\":\"{\\\"steps\\\":[{\\\"type\\\":\\\"UNSUPPORTED_XYZ\\\",\\\"config\\\":{}}]}\""
                + "}";
        HttpRequest invalidReq = HttpRequest.newBuilder(uri("/api/workflows"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidWorkflowPayload))
                .build();
        HttpResponse<String> invalidResp = send(invalidReq);
        assertEquals(400, invalidResp.statusCode());
        assertTrue(invalidResp.body().contains("Unsupported step type: UNSUPPORTED_XYZ"));
    }

    private HttpRequest jsonPost(String path, String json) {
        return HttpRequest.newBuilder(uri(path))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
    }

    private HttpResponse<String> send(HttpRequest req) throws IOException, InterruptedException {
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
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
