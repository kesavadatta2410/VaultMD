package com.vaultmd.backend.service;

import com.vaultmd.backend.assistant.AccessControlService;
import com.vaultmd.backend.assistant.AccessGrant;
import com.vaultmd.backend.assistant.AiServiceClient;
import com.vaultmd.backend.assistant.AiServiceModels;
import com.vaultmd.backend.dto.AssistantQueryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssistantService {

    private final AccessControlService accessControlService;
    private final AiServiceClient aiServiceClient;
    private final AuditLogService auditLogService;

    public AssistantService(AccessControlService accessControlService,
                             AiServiceClient aiServiceClient,
                             AuditLogService auditLogService) {
        this.accessControlService = accessControlService;
        this.aiServiceClient = aiServiceClient;
        this.auditLogService = auditLogService;
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
        List<String> sources = aiResponse.sources() != null ? aiResponse.sources() : List.of();

        auditLogService.record(grant, question, sources);

        List<AiServiceModels.ChunkPreview> chunkPreviews =
                aiResponse.chunkPreviews() != null ? aiResponse.chunkPreviews() : List.of();
        return new AssistantQueryResponse(aiResponse.answer(), sources, grant.getAccessType(), chunkPreviews);
    }
}
