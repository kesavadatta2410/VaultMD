package com.vaultmd.backend.assistant;

import com.vaultmd.backend.exception.AccessDeniedApiException;
import com.vaultmd.backend.model.AccessType;
import com.vaultmd.backend.model.Consent;
import com.vaultmd.backend.model.EmergencyAccess;
import com.vaultmd.backend.repository.ConsentRepository;
import com.vaultmd.backend.repository.EmergencyAccessRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * The single choke point for VaultMD's core security rule: a doctor may only
 * query a patient's records via the assistant if an ACTIVE consent or a
 * recent EmergencyAccess exists for that (doctor, patient) pair. This is the
 * only class that constructs an {@link AccessGrant} - see that class's
 * Javadoc for why that matters.
 */
@Service
public class AccessControlService {

    private final ConsentRepository consentRepository;
    private final EmergencyAccessRepository emergencyAccessRepository;
    private final int emergencyValidityHours;

    public AccessControlService(ConsentRepository consentRepository,
                                 EmergencyAccessRepository emergencyAccessRepository,
                                 @Value("${vaultmd.emergency-access.validity-hours}") int emergencyValidityHours) {
        this.consentRepository = consentRepository;
        this.emergencyAccessRepository = emergencyAccessRepository;
        this.emergencyValidityHours = emergencyValidityHours;
    }

    /**
     * @return an AccessGrant proving this doctor may query this patient's data
     * @throws AccessDeniedApiException (mapped to HTTP 403) if neither an
     *                                   active consent nor a recent emergency
     *                                   access exists
     */
    public AccessGrant authorize(Long doctorId, Long patientId) {
        Optional<Consent> consent = consentRepository.findByPatientIdAndDoctorId(patientId, doctorId);
        if (consent.isPresent() && consent.get().isCurrentlyActive()) {
            return new AccessGrant(doctorId, patientId, AccessType.CONSENT, consent.get().getId());
        }

        Instant cutoff = Instant.now().minus(emergencyValidityHours, ChronoUnit.HOURS);
        Optional<EmergencyAccess> emergency = emergencyAccessRepository
                .findFirstByDoctorIdAndPatientIdAndCreatedAtAfterOrderByCreatedAtDesc(doctorId, patientId, cutoff);
        if (emergency.isPresent()) {
            return new AccessGrant(doctorId, patientId, AccessType.EMERGENCY, emergency.get().getId());
        }

        throw new AccessDeniedApiException(
                "No active consent or recent emergency access found for this patient. " +
                        "Ask the patient to grant consent, or log an emergency access request first.");
    }
}
