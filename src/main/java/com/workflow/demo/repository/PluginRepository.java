package com.workflow.demo.repository;

import com.workflow.demo.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PluginRepository extends JpaRepository<Plugin, UUID> {
    List<Plugin> findByActiveTrueOrderByCategoryAscNameAsc();
    List<Plugin> findAllByOrderByCategoryAscNameAsc();
    Optional<Plugin> findByKeyIgnoreCase(String key);
}
