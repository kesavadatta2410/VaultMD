package com.vaultmd.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class ConsentRequest {

    @NotNull
    private Long doctorId;

    /** Optional: null means the consent does not expire on its own (still revocable). */
    private Instant expiresAt;

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
