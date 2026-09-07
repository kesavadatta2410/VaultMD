package com.vaultmd.backend.dto;

import com.vaultmd.backend.model.Consent;
import com.vaultmd.backend.model.ConsentStatus;

import java.time.Instant;

public class ConsentResponse {

    private final Long id;
    private final Long doctorId;
    private final String doctorName;
    private final ConsentStatus status;
    private final Instant grantedAt;
    private final Instant expiresAt;

    public ConsentResponse(Consent consent) {
        this.id = consent.getId();
        this.doctorId = consent.getDoctor().getId();
        this.doctorName = consent.getDoctor().getFullName();
        this.status = consent.getStatus();
        this.grantedAt = consent.getGrantedAt();
        this.expiresAt = consent.getExpiresAt();
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

    public ConsentStatus getStatus() {
        return status;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
