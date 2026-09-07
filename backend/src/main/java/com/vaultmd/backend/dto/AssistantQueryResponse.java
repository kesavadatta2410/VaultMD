package com.vaultmd.backend.dto;

import com.vaultmd.backend.model.AccessType;

import java.util.List;

public class AssistantQueryResponse {

    private final String answer;
    private final List<String> sources;
    private final AccessType accessType;

    public AssistantQueryResponse(String answer, List<String> sources, AccessType accessType) {
        this.answer = answer;
        this.sources = sources;
        this.accessType = accessType;
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
}
