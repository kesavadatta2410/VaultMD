package com.vaultmd.backend.service;

import com.vaultmd.backend.dto.EmergencyAccessRequest;
import com.vaultmd.backend.dto.EmergencyAccessResponse;
import com.vaultmd.backend.exception.NotFoundApiException;
import com.vaultmd.backend.model.Doctor;
import com.vaultmd.backend.model.EmergencyAccess;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.repository.DoctorRepository;
import com.vaultmd.backend.repository.EmergencyAccessRepository;
import com.vaultmd.backend.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmergencyAccessService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyAccessService.class);

    private final EmergencyAccessRepository emergencyAccessRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final int validityHours;

    public EmergencyAccessService(EmergencyAccessRepository emergencyAccessRepository,
                                   PatientRepository patientRepository,
                                   DoctorRepository doctorRepository,
                                   @Value("${vaultmd.emergency-access.validity-hours}") int validityHours) {
        this.emergencyAccessRepository = emergencyAccessRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.validityHours = validityHours;
    }

    public EmergencyAccessResponse logAccess(Long doctorId, EmergencyAccessRequest req) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundApiException("Doctor not found"));
        Patient patient = patientRepository.findById(req.getPatientId())
                .orElseThrow(() -> new NotFoundApiException("Patient not found"));

        EmergencyAccess emergencyAccess = emergencyAccessRepository.save(
                new EmergencyAccess(doctor, patient, req.getJustification()));

        // NON-GOAL (per spec): a real system would notify the patient (and/or
        // a compliance reviewer) here that emergency access was used on their
        // record. This is the spot that hook would go - for the demo we only
        // log the access itself, which AccessControlService will now honor
        // as an active grant for the next `validityHours` hours.
        log.info("Emergency access logged: doctor={} patient={} justification={}",
                doctorId, req.getPatientId(), req.getJustification());

        return new EmergencyAccessResponse(emergencyAccess, validityHours);
    }
}
