package com.vaultmd.backend.dto;

import com.vaultmd.backend.assistant.AiServiceModels;
import com.vaultmd.backend.model.AccessType;

import java.util.List;

public class AssistantQueryResponse {

    private final String answer;
    private final List<String> sources;
    private final AccessType accessType;
    private final List<AiServiceModels.ChunkPreview> chunkPreviews;

    public AssistantQueryResponse(String answer, List<String> sources, AccessType accessType,
                                   List<AiServiceModels.ChunkPreview> chunkPreviews) {
        this.answer = answer;
        this.sources = sources;
        this.accessType = accessType;
        this.chunkPreviews = chunkPreviews;
    }

    public String getAnswer() {
        return answer;
    }

    public List<String> getSources() {
        return sources;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public List<AiServiceModels.ChunkPreview> getChunkPreviews() {
        return chunkPreviews;
    }
}
