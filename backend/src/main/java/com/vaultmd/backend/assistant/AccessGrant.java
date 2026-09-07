package com.vaultmd.backend.assistant;

import com.vaultmd.backend.model.AccessType;

/**
 * Proof that a doctor's query for a given patient was authorized.
 * <p>
 * This is the core enforcement mechanism for the app's central security
 * rule: "no code path may call ai-service /query without first checking
 * consent/emergency status." Rather than relying on every caller to
 * remember to run the check (a convention that's easy to forget or
 * bypass), the type system enforces it:
 * <ul>
 *   <li>The constructor is package-private, so only classes in this
 *       {@code assistant} package can create an AccessGrant.</li>
 *   <li>{@link AccessControlService} is the only class in this package
 *       that does so - and only after verifying an active consent or a
 *       recent emergency access exists.</li>
 *   <li>{@link AiServiceClient#query} requires an AccessGrant parameter,
 *       so it is structurally impossible to compile a call to the
 *       ai-service /query endpoint without first obtaining one.</li>
 * </ul>
 */
public final class AccessGrant {

    private final Long doctorId;
    private final Long patientId;
    private final AccessType accessType;
    /** id of the Consent or EmergencyAccess row that authorized this grant. */
    private final Long accessRecordId;

    AccessGrant(Long doctorId, Long patientId, AccessType accessType, Long accessRecordId) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.accessType = accessType;
        this.accessRecordId = accessRecordId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public AccessType getAccessType() {
        return accessType;
    }

    public Long getAccessRecordId() {
        return accessRecordId;
    }
}
