package com.vaultmd.backend.controller;

import com.vaultmd.backend.dto.RecordResponse;
import com.vaultmd.backend.dto.RecordUploadRequest;
import com.vaultmd.backend.security.UserPrincipal;
import com.vaultmd.backend.service.RecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public ResponseEntity<RecordResponse> upload(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody RecordUploadRequest request) {
        // patientId always comes from the authenticated principal, never from
        // the request body - a patient can only ever upload their own record.
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.upload(principal.id(), request));
    }
}
