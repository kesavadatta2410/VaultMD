package com.vaultmd.backend.controller;

import com.vaultmd.backend.dto.AssistantQueryRequest;
import com.vaultmd.backend.dto.AssistantQueryResponse;
import com.vaultmd.backend.security.UserPrincipal;
import com.vaultmd.backend.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/query")
    public ResponseEntity<AssistantQueryResponse> query(@AuthenticationPrincipal UserPrincipal principal,
                                                          @Valid @RequestBody AssistantQueryRequest request) {
        // doctorId comes from the authenticated principal; patientId is
        // whichever patient the doctor named in the request. AssistantService
        // is the only place these two ids meet the consent/emergency check.
        AssistantQueryResponse response = assistantService.query(
                principal.id(), request.getPatientId(), request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
