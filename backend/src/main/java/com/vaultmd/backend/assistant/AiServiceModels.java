package com.vaultmd.backend.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wire-format records for the ai-service HTTP API. The ai-service speaks
 * snake_case JSON, so field names are mapped explicitly with @JsonProperty
 * rather than relying on a naming strategy.
 */
public final class AiServiceModels {

    private AiServiceModels() {
    }

    public record IngestRequest(
            @JsonProperty("patient_id") String patientId,
            @JsonProperty("record_id") String recordId,
            @JsonProperty("text") String text
    ) {
    }

    public record IngestResponse(
            @JsonProperty("patient_id") String patientId,
            @JsonProperty("record_id") String recordId,
            @JsonProperty("chunks_ingested") int chunksIngested
    ) {
    }

    public record QueryRequest(
            @JsonProperty("patient_id") String patientId,
            @JsonProperty("question") String question
    ) {
    }

    public record ChunkPreview(
            @JsonProperty("record_id") String recordId,
            @JsonProperty("snippet") String snippet,
            @JsonProperty("relevance") double relevance
    ) {
    }

    public record QueryResponse(
            @JsonProperty("answer") String answer,
            @JsonProperty("sources") List<String> sources,
            @JsonProperty("chunk_previews") List<ChunkPreview> chunkPreviews,
            @JsonProperty("collection_empty") boolean collectionEmpty
    ) {
    }
}
