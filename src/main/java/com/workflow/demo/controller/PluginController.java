package com.workflow.demo.controller;

import com.workflow.demo.dto.PluginDto;
import com.workflow.demo.service.PluginService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class PluginController {

    private final PluginService pluginService;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    // Public Endpoint: GET /plugins
    @GetMapping("/plugins")
    public List<PluginDto> listPublicPlugins() {
        return pluginService.getActivePlugins();
    }

    // Authenticated Endpoint: GET /api/plugins
    @GetMapping("/api/plugins")
    public List<PluginDto> listPlugins() {
        return pluginService.getActivePlugins();
    }

    // Authenticated Endpoint: GET /api/plugins/{id}
    @GetMapping("/api/plugins/{id}")
    public PluginDto getPlugin(@PathVariable UUID id) {
        return pluginService.getPlugin(id);
    }

    // Authenticated Endpoint: POST /api/plugins (Register custom plugin)
    @PostMapping("/api/plugins")
    @ResponseStatus(HttpStatus.CREATED)
    public PluginDto createPlugin(@RequestBody PluginDto dto) {
        return pluginService.createPlugin(dto);
    }

    // Authenticated Endpoint: PUT /api/plugins/{id}
    @PutMapping("/api/plugins/{id}")
    public PluginDto updatePlugin(@PathVariable UUID id, @RequestBody PluginDto dto) {
        return pluginService.updatePlugin(id, dto);
    }

    // Authenticated Endpoint: DELETE /api/plugins/{id}
    @DeleteMapping("/api/plugins/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlugin(@PathVariable UUID id) {
        pluginService.deletePlugin(id);
    }
}
