package com.seninsight.backend.business.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.UserCreateDTO;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.UserDTO;
import sn.gainde2000.senegallicenseplatformbackend.business.user.dtos.UserUpdateDTO;
import sn.gainde2000.senegallicenseplatformbackend.business.user.mappers.UserMapper;
import sn.gainde2000.senegallicenseplatformbackend.config.exceptions.BusinessException;
import sn.gainde2000.senegallicenseplatformbackend.config.exceptions.DuplicateResourceException;
import sn.gainde2000.senegallicenseplatformbackend.enums.UserStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        log.info("Récupération de tous les utilisateurs");
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getUsersByRole(String roleCode) {
        log.info("Récupération des utilisateurs avec le rôle: {}", roleCode);
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND",
                        "Rôle non trouvé: " + roleCode, null));

        return userRepository.findByRolesContaining(role).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getInstructeurs() {
        return getUsersByRole("INSTRUCTEUR");
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getDemandeurs() {
        return getUsersByRole("DEMANDEUR");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserById(UUID id) {
        log.info("Récupération de l'utilisateur avec l'ID: {}", id);
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Utilisateur non trouvé avec l'ID: " + id, id));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        log.info("Récupération de l'utilisateur avec l'email: {}", email);
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Utilisateur non trouvé avec l'email: " + email, email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getUserByUsername(String username) {
        log.info("Récupération de l'utilisateur avec le username: {}", username);
        return userRepository.findByUsername(username)
                .map(userMapper::toDto)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Utilisateur non trouvé avec le username: " + username, username));
    }

    @Override
    public UserDTO createUser(UserCreateDTO createDTO) {
        log.info("Création d'un nouvel utilisateur: {}", createDTO.getUsername());

        validateUserCreation(createDTO);

        User user = User.builder()
                .username(createDTO.getUsername())
                .email(createDTO.getEmail())
                .fullName(createDTO.getFullName())
                .phone(createDTO.getPhone())
                .adresse(createDTO.getAdresse())
                .password(passwordEncoder.encode(createDTO.getPassword()))
                .status(UserStatus.ACTIF)
                .build();

        // Assigner les rôles
        Set<Role> roles = assignRoles(createDTO.getRoleCodes());
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        log.info("Utilisateur créé avec succès: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDTO updateUser(UUID id, UserUpdateDTO updateDTO) {
        log.info("Mise à jour de l'utilisateur: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Utilisateur non trouvé avec l'ID: " + id, id));

        if (updateDTO.getEmail() != null && !user.getEmail().equals(updateDTO.getEmail())) {
            if (userRepository.existsByEmail(updateDTO.getEmail())) {
                throw new DuplicateResourceException("USER", "Email", updateDTO.getEmail());
            }
            user.setEmail(updateDTO.getEmail());
        }

        if (updateDTO.getUsername() != null && !user.getUsername().equals(updateDTO.getUsername())) {
            if (userRepository.existsByUsername(updateDTO.getUsername())) {
                throw new DuplicateResourceException("USER", "Username", updateDTO.getUsername());
            }
            user.setUsername(updateDTO.getUsername());
        }

        if (updateDTO.getFullName() != null) {
            user.setFullName(updateDTO.getFullName());
        }
        if (updateDTO.getPhone() != null) {
            user.setPhone(updateDTO.getPhone());
        }
        if (updateDTO.getAdresse() != null) {
            user.setAdresse(updateDTO.getAdresse());
        }

        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("Utilisateur mis à jour avec succès: {}", id);

        return userMapper.toDto(updatedUser);
    }

    @Override
    public void deleteUser(UUID id) {
        log.info("Desactivation de l'utilisateur (soft-delete): {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Utilisateur non trouve avec l'ID: " + id, id));

        user.setStatus(UserStatus.INACTIF);
        userRepository.save(user);
        log.info("Utilisateur desactive avec succes: {}", id);
    }

    @Override
    public UserDTO activateUser(UUID id) {
        log.info("Activation de l'utilisateur: {}", id);
        return updateUserStatus(id, UserStatus.ACTIF);
    }

    @Override
    public UserDTO deactivateUser(UUID id) {
        log.info("Désactivation de l'utilisateur: {}", id);
        return updateUserStatus(id, UserStatus.INACTIF);
    }

    @Override
    public UserDTO suspendUser(UUID id) {
        log.info("Suspension de l'utilisateur: {}", id);
        return updateUserStatus(id, UserStatus.SUSPENDU);
    }

    private void validateUserCreation(UserCreateDTO createDTO) {
        if (userRepository.existsByEmail(createDTO.getEmail())) {
            throw new DuplicateResourceException("USER", "Email", createDTO.getEmail());
        }

        if (userRepository.existsByUsername(createDTO.getUsername())) {
            throw new DuplicateResourceException("USER", "Username", createDTO.getUsername());
        }

        if (createDTO.getPassword() == null || createDTO.getPassword().isEmpty()) {
            throw new BusinessException("PASSWORD_REQUIRED",
                    "Le mot de passe est obligatoire", null);
        }

        if (createDTO.getRoleCodes() == null || createDTO.getRoleCodes().isEmpty()) {
            throw new BusinessException("ROLE_REQUIRED",
                    "Au moins un rôle est requis", null);
        }
    }

    private Set<Role> assignRoles(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(code -> roleRepository.findByCode(code)
                        .orElseThrow(() -> new BusinessException("ROLE_NOT_FOUND",
                                "Rôle non trouvé: " + code, code)))
                .collect(Collectors.toSet());
    }

    private UserDTO updateUserStatus(UUID id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                        "Utilisateur non trouvé avec l'ID: " + id, id));

        user.setStatus(status);
        User updatedUser = userRepository.save(user);

        return userMapper.toDto(updatedUser);
    }
}