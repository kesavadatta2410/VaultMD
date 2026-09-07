package com.vaultmd.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "health_records")
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private String title;

    /** Free-text category, e.g. "lab_result", "allergy", "note". Not an enum - kept simple for the demo. */
    @Column(nullable = false)
    private String type;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public HealthRecord() {
    }

    public HealthRecord(Patient patient, String title, String type, String rawText) {
        this.patient = patient;
        this.title = title;
        this.type = type;
        this.rawText = rawText;
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public String getRawText() {
        return rawText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
