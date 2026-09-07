package com.vaultmd.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessType accessType;

    /** id of the Consent or EmergencyAccess row that authorized this access - kept for traceability. */
    @Column(nullable = false)
    private Long accessRecordId;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    /** Comma-separated HealthRecord ids the answer cited. Empty if none matched. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String citedRecordIds;

    @Column(nullable = false, updatable = false)
    private Instant accessedAt = Instant.now();

    public AuditLog() {
    }

    public AuditLog(Doctor doctor, Patient patient, AccessType accessType, Long accessRecordId,
                     String question, String citedRecordIds) {
        this.doctor = doctor;
        this.patient = patient;
        this.accessType = accessType;
        this.accessRecordId = accessRecordId;
        this.question = question;
        this.citedRecordIds = citedRecordIds;
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

    public AccessType getAccessType() {
        return accessType;
    }

    public Long getAccessRecordId() {
        return accessRecordId;
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
