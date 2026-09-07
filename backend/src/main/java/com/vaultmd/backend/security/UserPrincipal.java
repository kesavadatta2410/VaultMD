package com.vaultmd.backend.security;

import com.vaultmd.backend.model.Role;

/**
 * The authenticated caller, resolved from the JWT on every request.
 * Controllers pull the caller's own id from here instead of trusting any
 * id the client puts in a request body/path - that's what keeps a patient
 * from acting as another patient (or a doctor as another doctor).
 */
public record UserPrincipal(Long id, String email, Role role) {
}
