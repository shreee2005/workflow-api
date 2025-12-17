package com.workflow.demo.repository;

import com.workflow.demo.entity.IncomingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncomingEventRepository extends JpaRepository<IncomingEvent, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
}
