package com.vaultmd.backend.controller;

import com.vaultmd.backend.dto.ConsentRequest;
import com.vaultmd.backend.dto.ConsentResponse;
import com.vaultmd.backend.security.UserPrincipal;
import com.vaultmd.backend.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consent")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping
    public ResponseEntity<ConsentResponse> grant(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody ConsentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consentService.grant(principal.id(), request));
    }

    @GetMapping
    public ResponseEntity<List<ConsentResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(consentService.listForPatient(principal.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable Long id) {
        consentService.revoke(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
