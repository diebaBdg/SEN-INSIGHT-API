package com.seninsight.backend.controller;

import com.seninsight.backend.dto.*;
import com.seninsight.backend.entity.AppUser;
import com.seninsight.backend.entity.Invitation;
import com.seninsight.backend.exception.BadRequestException;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.AppUserRepository;
import com.seninsight.backend.repository.InvitationRepository;
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
@Tag(name = "Utilisateurs")
public class UserController {

    private final AppUserRepository userRepo;
    private final InvitationRepository invitationRepo;

    public UserController(AppUserRepository userRepo, InvitationRepository invitationRepo) {
        this.userRepo = userRepo;
        this.invitationRepo = invitationRepo;
    }

    @GetMapping("/me")
    public UserDto getProfile(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return toDto(user);
    }

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> listUsers() {
        return userRepo.findAll().stream().map(this::toDto).toList();
    }

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
