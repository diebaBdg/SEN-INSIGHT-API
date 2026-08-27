package com.seninsight.backend.controller;

import com.seninsight.backend.dto.*;
import com.seninsight.backend.entity.AppUser;
import com.seninsight.backend.entity.Invitation;
import com.seninsight.backend.exception.BadRequestException;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.AppUserRepository;
import com.seninsight.backend.repository.InvitationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@Tag(name = "Utilisateurs", description = "Gestion du profil utilisateur et invitation de nouveaux membres (admin)")
public class UserController {

    private final AppUserRepository userRepo;
    private final InvitationRepository invitationRepo;

    public UserController(AppUserRepository userRepo, InvitationRepository invitationRepo) {
        this.userRepo = userRepo;
        this.invitationRepo = invitationRepo;
    }

    @Operation(summary = "Obtenir mon profil", description = "Retourne les informations de l'utilisateur connecté.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil récupéré",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content)
    })
    @GetMapping("/me")
    public UserDto getProfile(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return toDto(user);
    }

    @Operation(summary = "Mettre à jour mon profil", description = "Met à jour le nom, l'organisation et le téléphone de l'utilisateur connecté.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil mis à jour",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé", content = @Content)
    })
    @PutMapping("/me")
    public UserDto updateProfile(@Valid @RequestBody UpdateProfileRequest req, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getOrganization() != null) user.setOrganization(req.getOrganization());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        userRepo.save(user);
        return toDto(user);
    }

    @Operation(summary = "Lister tous les utilisateurs", description = "Retourne tous les utilisateurs. Réservé aux administrateurs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des utilisateurs",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle admin requis", content = @Content)
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> listUsers() {
        return userRepo.findAll().stream().map(this::toDto).toList();
    }

    @Operation(summary = "Inviter un nouvel utilisateur", description = "Envoie une invitation à rejoindre la plateforme avec un rôle spécifié. Réservé aux administrateurs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Invitation envoyée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Un compte existe déjà avec cet email", content = @Content),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle admin requis", content = @Content)
    })
    @PostMapping("/invite")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> invite(@Valid @RequestBody InviteRequest req, Authentication auth) {
        UUID invitedBy = (UUID) auth.getPrincipal();
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Un compte existe déjà avec cet email");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        Invitation invitation = Invitation.builder()
                .email(req.getEmail())
                .role(req.getRole())
                .invitedBy(invitedBy)
                .token(token)
                .accepted(false)
                .expiresAt(Instant.now().plusSeconds(604800))
                .build();
        invitationRepo.save(invitation);
        return Map.of("message", "Invitation envoyée", "inviteToken", token, "email", req.getEmail());
    }

    private UserDto toDto(AppUser user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .organization(user.getOrganization())
                .phone(user.getPhone())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
