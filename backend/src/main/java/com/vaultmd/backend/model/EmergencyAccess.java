package com.vaultmd.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "emergency_accesses")
public class EmergencyAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EmergencyAccess() {
    }

    public EmergencyAccess(Doctor doctor, Patient patient, String justification) {
        this.doctor = doctor;
        this.patient = patient;
        this.justification = justification;
    }

    public Long getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getJustification() {
        return justification;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    // NOTE (non-goal per spec): a real system would fire a notification to the
    // patient / a compliance officer here. For this demo we only log the
    // access - see the code comment in EmergencyAccessService for the exact
    // spot a notification hook would go.
}
