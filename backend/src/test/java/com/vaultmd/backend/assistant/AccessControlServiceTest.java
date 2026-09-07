package com.vaultmd.backend.assistant;

import com.vaultmd.backend.exception.AccessDeniedApiException;
import com.vaultmd.backend.model.AccessType;
import com.vaultmd.backend.model.Consent;
import com.vaultmd.backend.model.ConsentStatus;
import com.vaultmd.backend.model.Doctor;
import com.vaultmd.backend.model.EmergencyAccess;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.repository.ConsentRepository;
import com.vaultmd.backend.repository.EmergencyAccessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Verifies the app's core security rule in isolation: a doctor may query a
 * patient's data via the assistant only with an active consent or a recent
 * emergency access - nothing else should ever produce an AccessGrant.
 */
class AccessControlServiceTest {

    @Mock
    private ConsentRepository consentRepository;
    @Mock
    private EmergencyAccessRepository emergencyAccessRepository;

    private AccessControlService accessControlService;

    private static final Long DOCTOR_ID = 1L;
    private static final Long PATIENT_ID = 2L;
    private static final int EMERGENCY_VALIDITY_HOURS = 12;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accessControlService = new AccessControlService(
                consentRepository, emergencyAccessRepository, EMERGENCY_VALIDITY_HOURS);
    }

    @Test
    void authorize_withActiveConsent_returnsConsentGrant() throws Exception {
        Consent consent = consentWith(ConsentStatus.ACTIVE, null);
        setId(consent, 99L);
        when(consentRepository.findByPatientIdAndDoctorId(PATIENT_ID, DOCTOR_ID))
                .thenReturn(Optional.of(consent));

        AccessGrant grant = accessControlService.authorize(DOCTOR_ID, PATIENT_ID);

        assertEquals(AccessType.CONSENT, grant.getAccessType());
        assertEquals(99L, grant.getAccessRecordId());
        assertEquals(PATIENT_ID, grant.getPatientId());
        assertEquals(DOCTOR_ID, grant.getDoctorId());
    }

    @Test
    void authorize_withRevokedConsent_andNoEmergencyAccess_throws() {
        Consent consent = consentWith(ConsentStatus.REVOKED, null);
        when(consentRepository.findByPatientIdAndDoctorId(PATIENT_ID, DOCTOR_ID))
                .thenReturn(Optional.of(consent));
        when(emergencyAccessRepository.findFirstByDoctorIdAndPatientIdAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(DOCTOR_ID), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedApiException.class,
                () -> accessControlService.authorize(DOCTOR_ID, PATIENT_ID));
    }

    @Test
    void authorize_withExpiredConsent_fallsBackToEmergencyAccess() throws Exception {
        Consent expiredConsent = consentWith(ConsentStatus.ACTIVE, Instant.now().minus(1, ChronoUnit.DAYS));
        when(consentRepository.findByPatientIdAndDoctorId(PATIENT_ID, DOCTOR_ID))
                .thenReturn(Optional.of(expiredConsent));

        EmergencyAccess emergencyAccess = emergencyAccessStub();
        setId(emergencyAccess, 55L);
        when(emergencyAccessRepository.findFirstByDoctorIdAndPatientIdAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(DOCTOR_ID), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(Optional.of(emergencyAccess));

        AccessGrant grant = accessControlService.authorize(DOCTOR_ID, PATIENT_ID);

        assertEquals(AccessType.EMERGENCY, grant.getAccessType());
        assertEquals(55L, grant.getAccessRecordId());
    }

    @Test
    void authorize_withNoConsentAndNoEmergencyAccess_throwsAccessDenied() {
        when(consentRepository.findByPatientIdAndDoctorId(PATIENT_ID, DOCTOR_ID))
                .thenReturn(Optional.empty());
        when(emergencyAccessRepository.findFirstByDoctorIdAndPatientIdAndCreatedAtAfterOrderByCreatedAtDesc(
                eq(DOCTOR_ID), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThrows(AccessDeniedApiException.class,
                () -> accessControlService.authorize(DOCTOR_ID, PATIENT_ID));
    }

    // --- test helpers (entities have no-setter constructors + private ids, so
    //     we build minimal instances via reflection rather than a real DB) ---

    private Consent consentWith(ConsentStatus status, Instant expiresAt) {
        Consent consent = new Consent(new Patient(), new Doctor(), status, expiresAt);
        return consent;
    }

    private EmergencyAccess emergencyAccessStub() {
        return new EmergencyAccess(new Doctor(), new Patient(), "test justification, at least 10 chars");
    }

    private void setId(Object entity, Long id) throws Exception {
        Field idField = entity.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(entity, id);
    }
}
