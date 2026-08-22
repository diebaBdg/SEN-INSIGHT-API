package com.seninsight.backend.business.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.InstructeurStatsDTO;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.UserCreateDTO;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.UserDTO;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.UserUpdateDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UserController {

    private final IUserService userService;
    private final InstructeurService instructeurService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir tous les utilisateurs")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/instructeurs")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTEUR')")
    @Operation(summary = "Obtenir tous les instructeurs")
    public ResponseEntity<List<UserDTO>> getAllInstructeurs() {
        return ResponseEntity.ok(userService.getInstructeurs());
    }

    @GetMapping("/demandeurs")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTEUR')")
    @Operation(summary = "Obtenir tous les demandeurs")
    public ResponseEntity<List<UserDTO>> getAllDemandeurs() {
        return ResponseEntity.ok(userService.getDemandeurs());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTEUR') or #id == authentication.principal.id")
    @Operation(summary = "Obtenir un utilisateur par ID")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir un utilisateur par email")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir un utilisateur par username")
    public ResponseEntity<UserDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un nouvel utilisateur")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserCreateDTO createDTO) {
        return new ResponseEntity<>(userService.createUser(createDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    @Operation(summary = "Mettre à jour un utilisateur")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateDTO updateDTO) {
        return ResponseEntity.ok(userService.updateUser(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer un utilisateur")
    public ResponseEntity<UserDTO> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.activateUser(id));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Désactiver un utilisateur")
    public ResponseEntity<UserDTO> deactivateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.deactivateUser(id));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspendre un utilisateur")
    public ResponseEntity<UserDTO> suspendUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.suspendUser(id));
    }

    // Endpoints pour les instructeurs
    @GetMapping("/instructeurs/{id}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTEUR')")
    @Operation(summary = "Obtenir les statistiques d'un instructeur")
    public ResponseEntity<InstructeurStatsDTO> getInstructeurStats(@PathVariable UUID id) {
        return ResponseEntity.ok(instructeurService.getInstructeurStats(id));
    }

    @GetMapping("/instructeurs/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir les statistiques de tous les instructeurs")
    public ResponseEntity<List<InstructeurStatsDTO>> getAllInstructeursStats() {
        return ResponseEntity.ok(instructeurService.getAllInstructeursStats());
    }

    @PutMapping("/instructeurs/{id}/stats/reset")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réinitialiser les statistiques d'un instructeur")
    public ResponseEntity<Void> resetInstructeurStats(@PathVariable UUID id) {
        instructeurService.resetStats(id);
        return ResponseEntity.noContent().build();
    }
}