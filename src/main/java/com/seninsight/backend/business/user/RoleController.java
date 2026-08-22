package com.seninsight.backend.business.user;

import com.seninsight.backend.business.user.dtos.RoleDTO;
import com.seninsight.backend.config.exceptions.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Gestion des roles utilisateurs")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    // ============ ROLES ============

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Obtenir tous les roles avec leurs permissions")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        List<RoleDTO> roles = roleRepository.findAll()
                .stream()
                .map(this::toDtoWithPermissions)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @Operation(summary = "Obtenir un role par ID")
    public ResponseEntity<RoleDTO> getRoleById(@PathVariable Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return ResponseEntity.ok(toDtoWithPermissions(role));
    }

    @GetMapping("/code/{code}")
    @Transactional(readOnly = true)
    @Operation(summary = "Obtenir un role par code")
    public ResponseEntity<RoleDTO> getRoleByCode(@PathVariable String code) {
        Role role = roleRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", code));
        return ResponseEntity.ok(toDtoWithPermissions(role));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Creer un nouveau role")
    public ResponseEntity<RoleDTO> createRole(@RequestBody RoleDTO dto) {
        if (roleRepository.findByCode(dto.getCode()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Role role = new Role();
        role.setCode(dto.getCode());
        role.setLibelle(dto.getLibelle());
        role.setDescription(dto.getDescription());
        Role saved = roleRepository.save(role);

        if (dto.getPermissionCodes() != null && !dto.getPermissionCodes().isEmpty()) {
            AssignPermissionsRequest req = new AssignPermissionsRequest();
            req.setPermissionCodes(dto.getPermissionCodes());
            doAssignPermissions(saved.getId(), req);
        }

        return new ResponseEntity<>(toDtoWithPermissions(roleRepository.findById(saved.getId()).orElse(saved)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Mettre a jour un role")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable Long id, @RequestBody RoleDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        if (dto.getLibelle() != null) role.setLibelle(dto.getLibelle());
        if (dto.getDescription() != null) role.setDescription(dto.getDescription());
        if (dto.getNiveauAutorisation() != null) role.setNiveauAutorisation(dto.getNiveauAutorisation());
        if (dto.getActif() != null) role.setActif(dto.getActif());
        roleRepository.save(role);

        if (dto.getPermissionCodes() != null) {
            AssignPermissionsRequest req = new AssignPermissionsRequest();
            req.setPermissionCodes(dto.getPermissionCodes());
            doAssignPermissions(id, req);
        }

        return ResponseEntity.ok(toDtoWithPermissions(role));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Supprimer un role")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role", "id", id);
        }
        rolePermissionRepository.deleteAllByRoleId(id);
        roleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ============ PERMISSIONS ============

    @GetMapping("/{id}/permissions")
    @Transactional(readOnly = true)
    @Operation(summary = "Obtenir les permissions d'un role")
    public ResponseEntity<List<PermissionDTO>> getPermissionsByRole(@PathVariable Long id) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role", "id", id);
        }
        List<PermissionDTO> permissions = rolePermissionRepository.findByRoleId(id)
                .stream()
                .map(rp -> toPermissionDto(rp.getPermission()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Assigner des permissions a un role (remplace les existantes)")
    public ResponseEntity<List<PermissionDTO>> assignPermissions(
            @PathVariable Long id,
            @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(doAssignPermissions(id, request));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Assigner des permissions a un role via PUT (remplace les existantes)")
    public ResponseEntity<List<PermissionDTO>> assignPermissionsPut(
            @PathVariable Long id,
            @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(doAssignPermissions(id, request));
    }

    private List<PermissionDTO> doAssignPermissions(Long id, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        rolePermissionRepository.deleteAllByRoleId(id);

        List<Permission> toAssign = resolvePermissions(request);
        for (Permission permission : toAssign) {
            RolePermission rp = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .build();
            rolePermissionRepository.save(rp);
        }

        return rolePermissionRepository.findByRoleId(id)
                .stream()
                .map(rp -> toPermissionDto(rp.getPermission()))
                .collect(Collectors.toList());
    }

    private List<Permission> resolvePermissions(AssignPermissionsRequest request) {
        if (request.getPermissionCodes() != null && !request.getPermissionCodes().isEmpty()) {
            return request.getPermissionCodes().stream()
                    .map(code -> permissionRepository.findByCode(code)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission", "code", code)))
                    .collect(Collectors.toList());
        }
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            return request.getPermissionIds().stream()
                    .map(pid -> permissionRepository.findById(pid)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", pid)))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Operation(summary = "Retirer une permission d'un role")
    public ResponseEntity<Void> removePermission(@PathVariable Long id, @PathVariable Long permissionId) {
        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role", "id", id);
        }
        rolePermissionRepository.deleteByRoleIdAndPermissionId(id, permissionId);
        return ResponseEntity.noContent().build();
    }

    // ============ ALL PERMISSIONS (catalogue) ============

    @GetMapping("/permissions/all")
    @Transactional(readOnly = true)
    @Operation(summary = "Obtenir toutes les permissions disponibles")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        List<PermissionDTO> permissions = permissionRepository.findAll()
                .stream()
                .map(this::toPermissionDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissions);
    }

    // ============ HELPERS ============

    private RoleDTO toDtoWithPermissions(Role role) {
        RoleDTO dto = toBaseDto(role);
        List<PermissionDTO> permissions = rolePermissionRepository.findByRoleId(role.getId())
                .stream()
                .map(rp -> toPermissionDto(rp.getPermission()))
                .collect(Collectors.toList());
        dto.setPermissions(permissions);
        dto.setPermissionCount(permissions.size());
        dto.setPermissionCodes(permissions.stream().map(PermissionDTO::getCode).collect(Collectors.toList()));
        return dto;
    }

    private RoleDTO toBaseDto(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setCode(role.getCode());
        dto.setLibelle(role.getLibelle());
        dto.setDescription(role.getDescription());
        dto.setNiveauAutorisation(role.getNiveauAutorisation());
        dto.setActif(role.getActif());
        dto.setDateCreation(role.getDateCreation());
        return dto;
    }

    private PermissionDTO toPermissionDto(Permission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setCode(permission.getCode());
        dto.setLibelle(permission.getLibelle());
        dto.setDescription(permission.getDescription());
        dto.setModule(permission.getModule());
        dto.setCategorie(permission.getModule());
        dto.setActif(permission.getActif());
        dto.setDateCreation(permission.getDateCreation());
        return dto;
    }
}
