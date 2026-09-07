package com.vaultmd.backend.dto;

import com.vaultmd.backend.model.EmergencyAccess;

import java.time.Instant;

public class EmergencyAccessResponse {

    private final Long id;
    private final Long patientId;
    private final String justification;
    private final Instant createdAt;
    private final int validForHours;

    public EmergencyAccessResponse(EmergencyAccess emergencyAccess, int validForHours) {
        this.id = emergencyAccess.getId();
        this.patientId = emergencyAccess.getPatient().getId();
        this.justification = emergencyAccess.getJustification();
        this.createdAt = emergencyAccess.getCreatedAt();
        this.validForHours = validForHours;
    }

    public Long getId() {
        return id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getJustification() {
        return justification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getValidForHours() {
        return validForHours;
    }
}
