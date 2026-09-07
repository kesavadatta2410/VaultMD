package com.vaultmd.backend.service;

import com.vaultmd.backend.assistant.AccessGrant;
import com.vaultmd.backend.dto.AuditLogEntryResponse;
import com.vaultmd.backend.exception.NotFoundApiException;
import com.vaultmd.backend.model.AuditLog;
import com.vaultmd.backend.model.Doctor;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.repository.AuditLogRepository;
import com.vaultmd.backend.repository.DoctorRepository;
import com.vaultmd.backend.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                            PatientRepository patientRepository,
                            DoctorRepository doctorRepository) {
        this.auditLogRepository = auditLogRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    public void record(AccessGrant grant, String question, List<String> citedRecordIds) {
        Doctor doctor = doctorRepository.getReferenceById(grant.getDoctorId());
        Patient patient = patientRepository.getReferenceById(grant.getPatientId());

        AuditLog log = new AuditLog(
                doctor,
                patient,
                grant.getAccessType(),
                grant.getAccessRecordId(),
                question,
                String.join(",", citedRecordIds)
        );
        auditLogRepository.save(log);
    }

    public List<AuditLogEntryResponse> listForPatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new NotFoundApiException("Patient not found");
        }
        return auditLogRepository.findByPatientIdOrderByAccessedAtDesc(patientId).stream()
                .map(AuditLogEntryResponse::new)
                .toList();
    }
}
