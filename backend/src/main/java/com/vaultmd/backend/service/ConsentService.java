package com.vaultmd.backend.service;

import com.vaultmd.backend.dto.ConsentRequest;
import com.vaultmd.backend.dto.ConsentResponse;
import com.vaultmd.backend.exception.NotFoundApiException;
import com.vaultmd.backend.model.Consent;
import com.vaultmd.backend.model.ConsentStatus;
import com.vaultmd.backend.model.Doctor;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.repository.ConsentRepository;
import com.vaultmd.backend.repository.DoctorRepository;
import com.vaultmd.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public ConsentService(ConsentRepository consentRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository) {
        this.consentRepository = consentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Grants (or re-grants) consent for one doctor. Idempotent per (patient, doctor) pair.
     * {@code @Transactional} prevents a race condition where two concurrent grant requests for
     * the same (patient, doctor) pair both pass the findBy check and then both attempt save,
     * which would violate the unique constraint and return a 500 instead of a clean error.
     */
    @Transactional
    public ConsentResponse grant(Long patientId, ConsentRequest req) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundApiException("Patient not found"));
        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new NotFoundApiException("Doctor not found"));

        Consent consent = consentRepository.findByPatientIdAndDoctorId(patientId, doctor.getId())
                .orElseGet(() -> new Consent(patient, doctor, ConsentStatus.ACTIVE, req.getExpiresAt()));

        consent.setStatus(ConsentStatus.ACTIVE);
        consent.setExpiresAt(req.getExpiresAt());
        consent.setGrantedAt(java.time.Instant.now());

        return new ConsentResponse(consentRepository.save(consent));
    }

    public List<ConsentResponse> listForPatient(Long patientId) {
        return consentRepository.findByPatientId(patientId).stream()
                .map(ConsentResponse::new)
                .toList();
    }

    /**
     * Revokes a consent. Returns 404 (not 403) when the consent belongs to a
     * different patient - this avoids confirming to a caller that a given
     * consent id exists at all if it isn't theirs.
     */
    public void revoke(Long patientId, Long consentId) {
        Consent consent = consentRepository.findById(consentId)
                .filter(c -> c.getPatient().getId().equals(patientId))
                .orElseThrow(() -> new NotFoundApiException("Consent not found"));

        consent.setStatus(ConsentStatus.REVOKED);
        consentRepository.save(consent);
    }
}
