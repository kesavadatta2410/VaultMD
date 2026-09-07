package com.vaultmd.backend.repository;

import com.vaultmd.backend.model.EmergencyAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface EmergencyAccessRepository extends JpaRepository<EmergencyAccess, Long> {

    /**
     * Most recent EmergencyAccess for this (doctor, patient) pair created at or
     * after {@code cutoff}. Used to decide whether a "recent" emergency grant
     * still counts as an active access window.
     */
    Optional<EmergencyAccess> findFirstByDoctorIdAndPatientIdAndCreatedAtAfterOrderByCreatedAtDesc(
            Long doctorId, Long patientId, Instant cutoff);
}
