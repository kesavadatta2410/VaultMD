package com.vaultmd.backend.controller;

import com.vaultmd.backend.dto.AuditLogEntryResponse;
import com.vaultmd.backend.exception.AccessDeniedApiException;
import com.vaultmd.backend.security.UserPrincipal;
import com.vaultmd.backend.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<List<AuditLogEntryResponse>> getForPatient(@AuthenticationPrincipal UserPrincipal principal,
                                                                       @PathVariable Long patientId) {
        // A patient may only view their own audit log - the security-config
        // role check only confirms "this caller is *a* patient", so the
        // id match still has to happen here to stop patient A reading
        // patient B's audit log.
        if (!principal.id().equals(patientId)) {
            throw new AccessDeniedApiException("You may only view your own audit log");
        }
        return ResponseEntity.ok(auditLogService.listForPatient(patientId));
    }
}
