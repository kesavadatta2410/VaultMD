package com.vaultmd.backend.controller;

import com.vaultmd.backend.dto.EmergencyAccessRequest;
import com.vaultmd.backend.dto.EmergencyAccessResponse;
import com.vaultmd.backend.security.UserPrincipal;
import com.vaultmd.backend.service.EmergencyAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emergency-access")
public class EmergencyAccessController {

    private final EmergencyAccessService emergencyAccessService;

    public EmergencyAccessController(EmergencyAccessService emergencyAccessService) {
        this.emergencyAccessService = emergencyAccessService;
    }

    @PostMapping
    public ResponseEntity<EmergencyAccessResponse> logAccess(@AuthenticationPrincipal UserPrincipal principal,
                                                               @Valid @RequestBody EmergencyAccessRequest request) {
        // doctorId always comes from the authenticated principal - a doctor
        // cannot log an emergency access request on another doctor's behalf.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(emergencyAccessService.logAccess(principal.id(), request));
    }
}
