package com.vaultmd.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    /** Fake/demo value only - not validated against any real registry. */
    @Column(nullable = false)
    private String licenseId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Doctor() {
    }

    public Doctor(String email, String passwordHash, String fullName, String licenseId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.licenseId = licenseId;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
