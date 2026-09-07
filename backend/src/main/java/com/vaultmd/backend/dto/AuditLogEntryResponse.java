package com.vaultmd.backend.dto;

import com.vaultmd.backend.model.AccessType;
import com.vaultmd.backend.model.AuditLog;

import java.time.Instant;

public class AuditLogEntryResponse {

    private final Long id;
    private final Long doctorId;
    private final String doctorName;
    private final AccessType accessType;
    private final String question;
    private final String citedRecordIds;
    private final Instant accessedAt;

    public AuditLogEntryResponse(AuditLog log) {
        this.id = log.getId();
        this.doctorId = log.getDoctor().getId();
        this.doctorName = log.getDoctor().getFullName();
        this.accessType = log.getAccessType();
        this.question = log.getQuestion();
        this.citedRecordIds = log.getCitedRecordIds();
        this.accessedAt = log.getAccessedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public String getQuestion() {
        return question;
    }

    public String getCitedRecordIds() {
        return citedRecordIds;
    }

    public Instant getAccessedAt() {
        return accessedAt;
    }
}
