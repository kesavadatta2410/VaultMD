package com.vaultmd.backend.repository;

import com.vaultmd.backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByPatientIdOrderByAccessedAtDesc(Long patientId);
}
