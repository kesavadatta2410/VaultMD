package com.vaultmd.backend.dto;

import java.time.Instant;

public class RecordResponse {

    private final Long id;
    private final String title;
    private final String type;
    private final Instant createdAt;
    private final boolean ingested;

    public RecordResponse(Long id, String title, String type, Instant createdAt, boolean ingested) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.createdAt = createdAt;
        this.ingested = ingested;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isIngested() {
        return ingested;
    }
}
