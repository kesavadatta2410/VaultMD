package com.vaultmd.backend.service;

import com.vaultmd.backend.assistant.AiServiceClient;
import com.vaultmd.backend.model.Consent;
import com.vaultmd.backend.model.ConsentStatus;
import com.vaultmd.backend.model.Doctor;
import com.vaultmd.backend.model.HealthRecord;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.repository.ConsentRepository;
import com.vaultmd.backend.repository.DoctorRepository;
import com.vaultmd.backend.repository.HealthRecordRepository;
import com.vaultmd.backend.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Populates demo data on first startup so the RAG flow has something real to
 * retrieve. All names, emails, and clinical content below are synthetic -
 * never real patient data (per spec's non-goals).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Shared demo password for every seeded account - see README for the full login list. */
    private static final String DEMO_PASSWORD = "Password123!";

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final ConsentRepository consentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AiServiceClient aiServiceClient;

    public DataSeeder(PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       HealthRecordRepository healthRecordRepository,
                       ConsentRepository consentRepository,
                       PasswordEncoder passwordEncoder,
                       AiServiceClient aiServiceClient) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.healthRecordRepository = healthRecordRepository;
        this.consentRepository = consentRepository;
        this.passwordEncoder = passwordEncoder;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    public void run(String... args) {
        if (patientRepository.existsByEmail("patient1@example.com")) {
            log.info("Seed data already present - skipping.");
            return;
        }

        log.info("Seeding demo data (all synthetic/fake) ...");
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        Patient patient1 = patientRepository.save(new Patient("patient1@example.com", hash, "Asha Rao"));
        Patient patient2 = patientRepository.save(new Patient("patient2@example.com", hash, "Ben Carter"));
        Patient patient3 = patientRepository.save(new Patient("patient3@example.com", hash, "Chidi Okafor"));

        Doctor doctor1 = doctorRepository.save(new Doctor("doctor1@example.com", hash, "Dr. Meera Iyer", "DEMO-LIC-001"));
        Doctor doctor2 = doctorRepository.save(new Doctor("doctor2@example.com", hash, "Dr. Sam Lee", "DEMO-LIC-002"));

        seedRecord(patient1, "Lipid Panel - Jan 2026", "lab_result",
                "Lipid panel: LDL 142 mg/dL (high), HDL 38 mg/dL (low), Triglycerides 210 mg/dL. " +
                        "Recommend dietary counseling and a repeat panel in 3 months.");
        seedRecord(patient1, "Penicillin Allergy", "allergy",
                "Allergy: penicillin, confirmed 2019 via clinical reaction (hives, mild swelling). " +
                        "Avoid all penicillin-class antibiotics; azithromycin was used as an alternative.");
        seedRecord(patient1, "Annual Physical Notes", "note",
                "Annual physical: blood pressure 128/82, resting heart rate 76 bpm, BMI 26.4. " +
                        "Patient reports occasional lower back pain, otherwise well.");

        seedRecord(patient2, "Fasting Glucose Panel", "lab_result",
                "Fasting glucose: 118 mg/dL (elevated, pre-diabetic range). HbA1c: 5.9%. " +
                        "Recommend follow-up in 6 months and lifestyle modification.");
        seedRecord(patient2, "Shellfish Allergy", "allergy",
                "Allergy: shellfish, confirmed 2021 via food challenge. Reaction included hives and mild " +
                        "throat tightness. Patient carries an epinephrine auto-injector.");

        seedRecord(patient3, "Vaccination History", "note",
                "Vaccination history: MMR (2015), Tdap booster (2022), seasonal influenza (annual, most " +
                        "recent Oct 2025). No adverse reactions recorded.");

        // Pre-grant consent from patient1 to doctor1 so the assistant flow can
        // be demoed immediately. patient2 and patient3 intentionally have NO
        // consent for either doctor, so the emergency-access path has
        // something to demo too.
        consentRepository.save(new Consent(patient1, doctor1, ConsentStatus.ACTIVE, null));

        log.info("=== VaultMD demo data ready ===");
        log.info("Shared demo password for every seeded account: {}", DEMO_PASSWORD);
        log.info("Patients: {} (id={}), {} (id={}), {} (id={})",
                patient1.getEmail(), patient1.getId(), patient2.getEmail(), patient2.getId(),
                patient3.getEmail(), patient3.getId());
        log.info("Doctors: {} (id={}), {} (id={})",
                doctor1.getEmail(), doctor1.getId(), doctor2.getEmail(), doctor2.getId());
        log.info("{} already has active consent for {} - try the assistant flow directly.",
                doctor1.getEmail(), patient1.getEmail());
        log.info("{} and {} have NO consent yet for either doctor - try the emergency-access flow on them.",
                patient2.getEmail(), patient3.getEmail());
    }

    private void seedRecord(Patient patient, String title, String type, String text) {
        HealthRecord record = healthRecordRepository.save(new HealthRecord(patient, title, type, text));
        try {
            aiServiceClient.ingest(patient.getId(), record.getId(), text);
        } catch (Exception e) {
            // Best-effort and deliberately single-shot: this runs inside the
            // CommandLineRunner, which blocks app startup (and, on free
            // hosts, can already be accepting HTTP traffic before it
            // finishes - a retry loop here previously caused early requests
            // to see accounts without their seeded consent yet). If
            // ai-service isn't up yet, AssistantService self-heals this at
            // query time instead (it re-ingests a patient's records the
            // first time ai-service reports their collection as empty).
            log.warn("Seed ingestion failed for record {} ({}). It will be re-ingested automatically the first " +
                    "time it's queried - is ai-service up? Error: {}", record.getId(), title, e.getMessage());
        }
    }
}
