package com.seninsight.backend.controller;

import com.seninsight.backend.dto.*;
import com.seninsight.backend.entity.AppUser;
import com.seninsight.backend.entity.OtpCode;
import com.seninsight.backend.entity.PasswordReset;
import com.seninsight.backend.exception.BadRequestException;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.AppUserRepository;
import com.seninsight.backend.repository.OtpCodeRepository;
import com.seninsight.backend.repository.PasswordResetRepository;
import com.seninsight.backend.security.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentification", description = "Inscription, connexion, vérification OTP et réinitialisation de mot de passe")
public class AuthController {

    private final AppUserRepository userRepo;
    private final OtpCodeRepository otpRepo;
    private final PasswordResetRepository resetRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AppUserRepository userRepo,
                          OtpCodeRepository otpRepo,
                          PasswordResetRepository resetRepo,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.otpRepo = otpRepo;
        this.resetRepo = resetRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Créer un compte", description = "Inscrit un nouvel utilisateur et génère un code OTP envoyé à l'email fourni.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compte créé avec succès",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Un compte existe déjà avec cet email", content = @Content)
    })
    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }
        AppUser user = AppUser.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .role("user")
                .organization(req.getOrganization())
                .phone(req.getPhone())
                .isActive(true)
                .emailVerified(false)
                .build();
        userRepo.save(user);

        generateOtp(req.getEmail(), user.getId());

        return Map.of("message", "Compte créé. Un code OTP a été envoyé à votre email.",
                "userId", user.getId().toString());
    }

    @Operation(summary = "Connexion", description = "Authentifie un utilisateur et retourne un token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connexion réussie",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identifiants incorrects ou compte désactivé", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        AppUser user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new BadRequestException("Email ou mot de passe incorrect"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Email ou mot de passe incorrect");
        }
        if (!user.getIsActive()) {
            throw new BadRequestException("Compte désactivé");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }

    @Operation(summary = "Vérifier un code OTP", description = "Valide le code OTP envoyé lors de l'inscription et marque l'email comme vérifié.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email vérifié avec succès", content = @Content),
            @ApiResponse(responseCode = "400", description = "Code OTP invalide ou expiré", content = @Content)
    })
    @PostMapping("/otp/verify")
    public Map<String, Object> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        OtpCode otp = otpRepo.findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(req.getEmail(), req.getCode())
                .orElseThrow(() -> new BadRequestException("Code OTP invalide"));
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Code OTP expiré");
        }
        otp.setUsed(true);
        otpRepo.save(otp);

        userRepo.findByEmail(req.getEmail()).ifPresent(u -> {
            u.setEmailVerified(true);
            userRepo.save(u);
        });

        return Map.of("message", "Email vérifié avec succès");
    }

    @Operation(summary = "Renvoyer un code OTP", description = "Génère et envoie un nouveau code OTP à l'email fourni.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Nouveau code OTP envoyé", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content)
    })
    @PostMapping("/otp/resend")
    public Map<String, Object> resendOtp(@Valid @RequestBody OtpResendRequest req) {
        AppUser user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        otpRepo.deleteByEmailAndUsedFalse(req.getEmail());
        generateOtp(req.getEmail(), user.getId());
        return Map.of("message", "Nouveau code OTP envoyé");
    }

    @Operation(summary = "Demander une réinitialisation de mot de passe", description = "Génère un token de réinitialisation si l'email existe. Retourne toujours le même message pour éviter la divulgation d'emails.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lien de réinitialisation généré (si l'email existe)", content = @Content)
    })
    @PostMapping("/password/forgot")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        AppUser user = userRepo.findByEmail(req.getEmail()).orElse(null);
        if (user == null) {
            return Map.of("message", "Si cet email existe, un lien de réinitialisation a été envoyé");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        PasswordReset reset = PasswordReset.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build();
        resetRepo.save(reset);
        return Map.of("message", "Si cet email existe, un lien de réinitialisation a été envoyé",
                "resetToken", token);
    }

    @Operation(summary = "Réinitialiser le mot de passe", description = "Définit un nouveau mot de passe à partir d'un token de réinitialisation valide.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès", content = @Content),
            @ApiResponse(responseCode = "400", description = "Token invalide ou expiré", content = @Content),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content)
    })
    @PostMapping("/password/reset")
    public Map<String, Object> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        PasswordReset reset = resetRepo.findByTokenAndUsedFalse(req.getToken())
                .orElseThrow(() -> new BadRequestException("Token invalide ou expiré"));
        if (reset.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Token expiré");
        }
        AppUser user = userRepo.findById(reset.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);

        reset.setUsed(true);
        resetRepo.save(reset);

        return Map.of("message", "Mot de passe réinitialisé avec succès");
    }

    private void generateOtp(String email, UUID userId) {
        otpRepo.deleteByEmailAndUsedFalse(email);
        String code = String.format("%06d", (int)(Math.random() * 1000000));
        OtpCode otp = OtpCode.builder()
                .userId(userId)
                .email(email)
                .code(code)
                .expiresAt(Instant.now().plusSeconds(300))
                .used(false)
                .build();
        otpRepo.save(otp);
    }
}
