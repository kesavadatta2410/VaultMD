package com.vaultmd.backend.assistant;

import com.vaultmd.backend.exception.UpstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AiServiceClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AiServiceClient(RestTemplate restTemplate,
                            @Value("${vaultmd.ai-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Called right after a patient uploads a record. No access-control gate
     * needed here: a patient ingesting their own newly-uploaded record isn't
     * the operation the consent/emergency rule protects against - that rule
     * is specifically about a *doctor* reading a patient's data (see query()).
     */
    public AiServiceModels.IngestResponse ingest(Long patientId, Long recordId, String text) {
        AiServiceModels.IngestRequest request = new AiServiceModels.IngestRequest(
                String.valueOf(patientId), String.valueOf(recordId), text);
        try {
            ResponseEntity<AiServiceModels.IngestResponse> response = restTemplate.postForEntity(
                    baseUrl + "/ingest", request, AiServiceModels.IngestResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Failed to ingest record into ai-service", e);
        }
    }

    /**
     * The ONLY method in the whole app that calls the ai-service's /query
     * endpoint. Requiring an {@link AccessGrant} parameter makes it a compile
     * error to reach this line without first calling
     * {@link AccessControlService#authorize}. The grant's own patientId
     * (not any patientId a caller might otherwise pass in) is what's sent,
     * so a caller cannot construct a grant for one patient and then query
     * for a different one.
     */
    public AiServiceModels.QueryResponse query(AccessGrant grant, String question) {
        AiServiceModels.QueryRequest request = new AiServiceModels.QueryRequest(
                String.valueOf(grant.getPatientId()), question);
        try {
            ResponseEntity<AiServiceModels.QueryResponse> response = restTemplate.postForEntity(
                    baseUrl + "/query", request, AiServiceModels.QueryResponse.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new UpstreamServiceException("Failed to query ai-service", e);
        }
    }
}
