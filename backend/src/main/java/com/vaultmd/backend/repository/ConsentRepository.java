package com.vaultmd.backend.repository;

import com.vaultmd.backend.model.Consent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsentRepository extends JpaRepository<Consent, Long> {
    Optional<Consent> findByPatientIdAndDoctorId(Long patientId, Long doctorId);

    List<Consent> findByPatientId(Long patientId);
}
