package controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    @PostMapping
    public Map<String, Object> createWorkflow(@RequestBody Map<String, Object> body) {

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Workflow created");
        response.put("received", body);

        return response;
    }

    @GetMapping
    public Map<String, Object> listWorkflows() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("workflows", new String[] { "sample-1", "sample-2" });

        return response;
    }
}
