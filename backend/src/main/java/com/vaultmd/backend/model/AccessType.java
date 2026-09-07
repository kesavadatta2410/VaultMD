package com.vaultmd.backend.model;

/**
 * Records which access path authorized a given assistant query, so the
 * audit log always shows *why* a doctor was allowed to see a patient's data.
 */
public enum AccessType {
    CONSENT,
    EMERGENCY
}
