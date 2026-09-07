package com.vaultmd.backend.service;

import com.vaultmd.backend.assistant.AccessControlService;
import com.vaultmd.backend.assistant.AccessGrant;
import com.vaultmd.backend.assistant.AiServiceClient;
import com.vaultmd.backend.assistant.AiServiceModels;
import com.vaultmd.backend.dto.AssistantQueryResponse;
import com.vaultmd.backend.model.HealthRecord;
import com.vaultmd.backend.repository.HealthRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AccessControlService accessControlService;
    private final AiServiceClient aiServiceClient;
    private final AuditLogService auditLogService;
    private final HealthRecordRepository healthRecordRepository;

    public AssistantService(AccessControlService accessControlService,
                             AiServiceClient aiServiceClient,
                             AuditLogService auditLogService,
                             HealthRecordRepository healthRecordRepository) {
        this.accessControlService = accessControlService;
        this.aiServiceClient = aiServiceClient;
        this.auditLogService = auditLogService;
        this.healthRecordRepository = healthRecordRepository;
    }

    /**
     * The end-to-end gated flow: check consent/emergency access, forward the
     * question to the ai-service ONLY if authorized, then write the audit
     * log entry. This is the one method AssistantController calls - there is
     * no other path to the ai-service's /query endpoint (see AiServiceClient
     * and AccessGrant for how that's enforced structurally, not just here by
     * convention).
     */
    public AssistantQueryResponse query(Long doctorId, Long patientId, String question) {
        AccessGrant grant = accessControlService.authorize(doctorId, patientId);

        AiServiceModels.QueryResponse aiResponse = aiServiceClient.query(grant, question);

        // Self-heals against backend and ai-service scaling to zero and
        // cold-starting independently (e.g. on Render's free tier): if this
        // patient has HealthRecord rows in Postgres/H2 but ai-service's
        // collection for them is empty - because ai-service was still
        // warming up when DataSeeder (or an earlier upload) tried to ingest,
        // or because an in-memory Chroma restarted on its own - re-ingest
        // from the source of truth and retry once, instead of permanently
        // returning "not found in records" until the backend happens to
        // restart too.
        if (aiResponse.collectionEmpty()) {
            resyncPatientRecords(patientId);
            aiResponse = aiServiceClient.query(grant, question);
        }

        List<String> sources = aiResponse.sources() != null ? aiResponse.sources() : List.of();

        auditLogService.record(grant, question, sources);

        List<AiServiceModels.ChunkPreview> chunkPreviews =
                aiResponse.chunkPreviews() != null ? aiResponse.chunkPreviews() : List.of();
        return new AssistantQueryResponse(aiResponse.answer(), sources, grant.getAccessType(), chunkPreviews);
    }

    private void resyncPatientRecords(Long patientId) {
        List<HealthRecord> records = healthRecordRepository.findByPatientId(patientId);
        log.info("ai-service reported an empty collection for patient {} - re-ingesting {} record(s)",
                patientId, records.size());
        for (HealthRecord record : records) {
            try {
                aiServiceClient.ingest(patientId, record.getId(), record.getRawText());
            } catch (Exception e) {
                log.warn("Resync ingestion failed for record {} (patient {}): {}",
                        record.getId(), patientId, e.getMessage());
            }
        }
    }
}
