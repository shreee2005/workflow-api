package com.workflow.demo.repository;

import com.workflow.demo.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByTeamIdAndRevokedFalse(UUID teamId);
}
