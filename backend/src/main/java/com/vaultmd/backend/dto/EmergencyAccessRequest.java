package com.vaultmd.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EmergencyAccessRequest {

    @NotNull
    private Long patientId;

    @NotBlank
    @Size(min = 10, message = "justification must be at least 10 characters - this is logged and shown to the patient")
    private String justification;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
