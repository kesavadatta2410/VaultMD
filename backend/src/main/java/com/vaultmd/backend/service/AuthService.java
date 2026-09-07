package com.vaultmd.backend.service;

import com.vaultmd.backend.dto.AuthResponse;
import com.vaultmd.backend.dto.LoginRequest;
import com.vaultmd.backend.dto.SignupRequest;
import com.vaultmd.backend.exception.BadRequestApiException;
import com.vaultmd.backend.exception.UnauthorizedApiException;
import com.vaultmd.backend.model.Doctor;
import com.vaultmd.backend.model.Patient;
import com.vaultmd.backend.model.Role;
import com.vaultmd.backend.repository.DoctorRepository;
import com.vaultmd.backend.repository.PatientRepository;
import com.vaultmd.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PatientRepository patientRepository,
                        DoctorRepository doctorRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse signup(SignupRequest req) {
        String email = req.getEmail().toLowerCase().trim();
        String hash = passwordEncoder.encode(req.getPassword());

        if (req.getRole() == Role.PATIENT) {
            if (patientRepository.existsByEmail(email)) {
                throw new BadRequestApiException("A patient with this email already exists");
            }
            Patient patient = patientRepository.save(new Patient(email, hash, req.getFullName()));
            String token = jwtService.generateToken(patient.getId(), patient.getEmail(), Role.PATIENT);
            return new AuthResponse(token, patient.getId(), patient.getFullName(), Role.PATIENT);
        } else {
            if (doctorRepository.existsByEmail(email)) {
                throw new BadRequestApiException("A doctor with this email already exists");
            }
            String licenseId = req.getLicenseId();
            if (licenseId == null || licenseId.isBlank()) {
                throw new BadRequestApiException("licenseId is required for doctor signup");
            }
            Doctor doctor = doctorRepository.save(new Doctor(email, hash, req.getFullName(), licenseId));
            String token = jwtService.generateToken(doctor.getId(), doctor.getEmail(), Role.DOCTOR);
            return new AuthResponse(token, doctor.getId(), doctor.getFullName(), Role.DOCTOR);
        }
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail().toLowerCase().trim();

        if (req.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedApiException("Invalid email or password"));
            if (!passwordEncoder.matches(req.getPassword(), patient.getPasswordHash())) {
                throw new UnauthorizedApiException("Invalid email or password");
            }
            String token = jwtService.generateToken(patient.getId(), patient.getEmail(), Role.PATIENT);
            return new AuthResponse(token, patient.getId(), patient.getFullName(), Role.PATIENT);
        } else {
            Doctor doctor = doctorRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedApiException("Invalid email or password"));
            if (!passwordEncoder.matches(req.getPassword(), doctor.getPasswordHash())) {
                throw new UnauthorizedApiException("Invalid email or password");
            }
            String token = jwtService.generateToken(doctor.getId(), doctor.getEmail(), Role.DOCTOR);
            return new AuthResponse(token, doctor.getId(), doctor.getFullName(), Role.DOCTOR);
        }
    }
}
