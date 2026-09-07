package com.vaultmd.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "consents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"patient_id", "doctor_id"})
)
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentStatus status;

    @Column(nullable = false)
    private Instant grantedAt = Instant.now();

    /** Nullable: null means "does not expire" for this demo's single on/off scope. */
    private Instant expiresAt;

    public Consent() {
    }

    public Consent(Patient patient, Doctor doctor, ConsentStatus status, Instant expiresAt) {
        this.patient = patient;
        this.doctor = doctor;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsentStatus status) {
        this.status = status;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isCurrentlyActive() {
        if (status != ConsentStatus.ACTIVE) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}
