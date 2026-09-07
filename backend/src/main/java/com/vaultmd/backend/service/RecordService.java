package com.vaultmd.backend.service;

import com.vaultmd.backend.assistant.AiServiceClient;
import com.vaultmd.backend.dto.RecordResponse;
import com.vaultmd.backend.dto.RecordUploadRequest;
import com.vaultmd.backend.exception.NotFoundApiException;
import com.vaultmd.backend.exception.UpstreamServiceException;
import com.vaultmd.backend.model.HealthRecord;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.repository.HealthRecordRepository;
import com.vaultmd.backend.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);

    private final HealthRecordRepository healthRecordRepository;
    private final PatientRepository patientRepository;
    private final AiServiceClient aiServiceClient;

    public RecordService(HealthRecordRepository healthRecordRepository,
                          PatientRepository patientRepository,
                          AiServiceClient aiServiceClient) {
        this.healthRecordRepository = healthRecordRepository;
        this.patientRepository = patientRepository;
        this.aiServiceClient = aiServiceClient;
    }

    /**
     * ASSUMPTION: ingestion into the ai-service is best-effort. If the
     * ai-service is temporarily unreachable, the record is still saved
     * (the patient's data isn't lost) but won't be queryable via the
     * assistant until re-ingested. A production version would retry via a
     * queue instead of silently degrading; noted in the README.
     */
    public RecordResponse upload(Long patientId, RecordUploadRequest req) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundApiException("Patient not found"));

        HealthRecord record = healthRecordRepository.save(
                new HealthRecord(patient, req.getTitle(), req.getType(), req.getText()));

        boolean ingested = true;
        try {
            aiServiceClient.ingest(patientId, record.getId(), req.getText());
        } catch (UpstreamServiceException e) {
            ingested = false;
            log.warn("Record {} saved but ai-service ingestion failed: {}", record.getId(), e.getMessage());
        }

        return new RecordResponse(record.getId(), record.getTitle(), record.getType(), record.getCreatedAt(), ingested);
    }
}
