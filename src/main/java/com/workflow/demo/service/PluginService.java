package com.workflow.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.demo.dto.PluginDto;
import com.workflow.demo.entity.Plugin;
import com.workflow.demo.repository.PluginRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PluginService {

    private final PluginRepository pluginRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PluginService(PluginRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
    }

    @PostConstruct
    public void init() {
        seedDefaultPlugins();
    }

    @Transactional
    public void seedDefaultPlugins() {
        if (pluginRepository.count() > 0) {
            return;
        }

        // 1. Log Message
        createSeed(
                "log",
                "Log Message",
                "Outputs a custom message to the workflow run logs for debugging and audit purposes.",
                "Core",
                "terminal",
                "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\",\"title\":\"Log Message\",\"description\":\"The message to output to the logs. Supports placeholders like {{input.body.key}}.\",\"default\":\"Executing step\"}},\"required\":[\"message\"]}"
        );

        // 2. HTTP Request
        createSeed(
                "http_call",
                "HTTP Request",
                "Sends an HTTP request (GET, POST, PUT, DELETE, etc.) to an external server or API endpoint.",
                "Integration",
                "globe",
                "{\"type\":\"object\",\"properties\":{\"url\":{\"type\":\"string\",\"title\":\"Request URL\",\"description\":\"The API endpoint URL to call.\",\"format\":\"uri\"},\"method\":{\"type\":\"string\",\"title\":\"HTTP Method\",\"enum\":[\"GET\",\"POST\",\"PUT\",\"DELETE\",\"PATCH\"],\"default\":\"POST\"},\"headers\":{\"type\":\"object\",\"title\":\"Headers\",\"description\":\"Key-value pairs of HTTP headers.\",\"additionalProperties\":{\"type\":\"string\"}},\"body\":{\"type\":\"string\",\"title\":\"Request Body\",\"description\":\"The request body content (usually JSON).\"}},\"required\":[\"url\",\"method\"]}"
        );

        // 3. Delay Timer
        createSeed(
                "wait",
                "Delay Timer",
                "Pauses workflow execution for a specified duration or until a callback is received.",
                "Control Flow",
                "clock",
                "{\"type\":\"object\",\"properties\":{\"duration\":{\"type\":\"integer\",\"title\":\"Delay Duration (seconds)\",\"description\":\"Number of seconds to pause the workflow.\",\"minimum\":1},\"correlationId\":{\"type\":\"string\",\"title\":\"Correlation ID\",\"description\":\"An identifier to resume workflow execution on external webhook callback.\"}}}"
        );

        // 4. Send Email
        createSeed(
                "send_email",
                "Send Email",
                "Sends an email notification to specified recipients.",
                "Communication",
                "mail",
                "{\"type\":\"object\",\"properties\":{\"to\":{\"type\":\"string\",\"title\":\"Recipient Email\",\"description\":\"Email address of the recipient.\"},\"subject\":{\"type\":\"string\",\"title\":\"Subject Line\",\"description\":\"The subject of the email.\"},\"body\":{\"type\":\"string\",\"title\":\"Email Body\",\"description\":\"The main text or HTML body of the email.\"}},\"required\":[\"to\",\"subject\",\"body\"]}"
        );

        // 5. Slack Notification
        createSeed(
                "slack_notification",
                "Slack Notification",
                "Sends a message to a Slack channel via incoming webhook.",
                "Communication",
                "slack",
                "{\"type\":\"object\",\"properties\":{\"webhookUrl\":{\"type\":\"string\",\"title\":\"Webhook URL\",\"description\":\"Slack incoming webhook URL.\"},\"message\":{\"type\":\"string\",\"title\":\"Message Text\",\"description\":\"The text message or block formatting to send.\"}},\"required\":[\"webhookUrl\",\"message\"]}"
        );

        // 6. Database Query
        createSeed(
                "database_query",
                "Database Query",
                "Runs a SQL query or command against a configured relational database.",
                "Integration",
                "database",
                "{\"type\":\"object\",\"properties\":{\"connectionString\":{\"type\":\"string\",\"title\":\"Connection String\",\"description\":\"JDBC connection string for the target database.\"},\"query\":{\"type\":\"string\",\"title\":\"SQL Query\",\"description\":\"The SQL query or statement to execute.\"}},\"required\":[\"connectionString\",\"query\"]}"
        );
    }

    private void createSeed(String key, String name, String description, String category, String icon, String configSchema) {
        Plugin plugin = new Plugin();
        plugin.setKey(key);
        plugin.setName(name);
        plugin.setDescription(description);
        plugin.setCategory(category);
        plugin.setIcon(icon);
        plugin.setConfigSchema(configSchema);
        plugin.setActive(true);
        plugin.setCreatedAt(OffsetDateTime.now());
        plugin.setUpdatedAt(OffsetDateTime.now());
        pluginRepository.save(plugin);
    }

    @Transactional(readOnly = true)
    public List<PluginDto> getActivePlugins() {
        return pluginRepository.findByActiveTrueOrderByCategoryAscNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PluginDto> getAllPlugins() {
        return pluginRepository.findAllByOrderByCategoryAscNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PluginDto getPlugin(UUID id) {
        Plugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found"));
        return toDto(plugin);
    }

    @Transactional(readOnly = true)
    public Optional<Plugin> findByKey(String key) {
        return pluginRepository.findByKeyIgnoreCase(key);
    }

    @Transactional
    public PluginDto createPlugin(PluginDto dto) {
        validatePluginDto(dto);

        if (pluginRepository.findByKeyIgnoreCase(dto.getKey().trim()).isPresent()) {
            throw new IllegalArgumentException("Plugin with key " + dto.getKey() + " already exists");
        }

        Plugin plugin = new Plugin();
        plugin.setKey(dto.getKey().trim().toLowerCase());
        plugin.setName(dto.getName().trim());
        plugin.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        plugin.setCategory(dto.getCategory().trim());
        plugin.setIcon(dto.getIcon() != null ? dto.getIcon().trim() : null);
        plugin.setConfigSchema(dto.getConfigSchema() != null ? dto.getConfigSchema().trim() : null);
        plugin.setActive(dto.isActive());
        plugin.setCreatedAt(OffsetDateTime.now());
        plugin.setUpdatedAt(OffsetDateTime.now());

        return toDto(pluginRepository.save(plugin));
    }

    @Transactional
    public PluginDto updatePlugin(UUID id, PluginDto dto) {
        validatePluginDto(dto);

        Plugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found"));

        Optional<Plugin> existingByKey = pluginRepository.findByKeyIgnoreCase(dto.getKey().trim());
        if (existingByKey.isPresent() && !existingByKey.get().getId().equals(id)) {
            throw new IllegalArgumentException("Plugin with key " + dto.getKey() + " already exists");
        }

        plugin.setKey(dto.getKey().trim().toLowerCase());
        plugin.setName(dto.getName().trim());
        plugin.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : null);
        plugin.setCategory(dto.getCategory().trim());
        plugin.setIcon(dto.getIcon() != null ? dto.getIcon().trim() : null);
        plugin.setConfigSchema(dto.getConfigSchema() != null ? dto.getConfigSchema().trim() : null);
        plugin.setActive(dto.isActive());
        plugin.setUpdatedAt(OffsetDateTime.now());

        return toDto(pluginRepository.save(plugin));
    }

    @Transactional
    public void deletePlugin(UUID id) {
        if (!pluginRepository.existsById(id)) {
            throw new IllegalArgumentException("Plugin not found");
        }
        pluginRepository.deleteById(id);
    }

    private void validatePluginDto(PluginDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Plugin details are required");
        }
        if (dto.getKey() == null || dto.getKey().isBlank()) {
            throw new IllegalArgumentException("Plugin key is required");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("Plugin name is required");
        }
        if (dto.getCategory() == null || dto.getCategory().isBlank()) {
            throw new IllegalArgumentException("Plugin category is required");
        }
        if (dto.getConfigSchema() != null && !dto.getConfigSchema().isBlank()) {
            try {
                objectMapper.readTree(dto.getConfigSchema());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Plugin configSchema must be valid JSON");
            }
        }
    }

    private PluginDto toDto(Plugin plugin) {
        PluginDto dto = new PluginDto();
        dto.setId(plugin.getId());
        dto.setKey(plugin.getKey());
        dto.setName(plugin.getName());
        dto.setDescription(plugin.getDescription());
        dto.setCategory(plugin.getCategory());
        dto.setIcon(plugin.getIcon());
        dto.setConfigSchema(plugin.getConfigSchema());
        dto.setActive(plugin.isActive());
        dto.setCreatedAt(plugin.getCreatedAt());
        dto.setUpdatedAt(plugin.getUpdatedAt());
        return dto;
    }
}
