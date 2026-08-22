package com.seninsight.backend.auth;

import com.seninsight.backend.business.user.RoleRepository;
import com.seninsight.backend.business.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    private Optional<User> findUserByLogin(String login) {
        Optional<User> userByEmail = userRepository.findByEmail(login);
        if (userByEmail.isPresent()) {
            return userByEmail;
        }
        return userRepository.findByUsername(login);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest loginRequest) {
        System.out.println("=== DEBUT TENTATIVE DE CONNEXION ===");
        System.out.println("Login reçu: " + loginRequest.login());
        System.out.println("Longueur du mot de passe reçu: " + (loginRequest.password() != null ? loginRequest.password().length() : "NULL"));

        try {
            System.out.println("Tentative d'authentification avec AuthenticationManager...");

            Optional<User> userOpt = findUserByLogin(loginRequest.login());

            if (userOpt.isEmpty()) {
                System.out.println("ERREUR: Utilisateur non trouvé dans la base");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiant ou mot de passe incorrect");
            }

            User user = userOpt.get();
            System.out.println("Utilisateur trouvé: " + user.getUsername() + " (username), " + user.getEmail() + " (email)");

            String springUsername = user.getUsername();
            System.out.println("Utilisation du username pour Spring Security: " + springUsername);

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(springUsername, loginRequest.password())
            );

            System.out.println("AuthenticationManager a retourné une authentication");

            if (authentication.isAuthenticated()) {
                System.out.println("Authentication.isAuthenticated() = TRUE");
                System.out.println("Utilisateur authentifié: " + authentication.getName());
                System.out.println("Statut utilisateur: " + user.getStatus()); // CORRECTION: getStatus()
                System.out.println("Nombre de rôles: " + user.getRoles().size());

                String token = jwtUtils.generateToken(user);
                System.out.println("Token JWT généré avec succès");
                System.out.println("=== CONNEXION RÉUSSIE ===");

                return ResponseEntity.ok(new LoginResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFullName(),
                        token,
                        user.getRoles().stream()
                                .map(role -> role.getCode())
                                .collect(Collectors.toSet())
                ));
            } else {
                System.out.println("Authentication.isAuthenticated() = FALSE");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Identifiants invalides");
            }

        } catch (AuthenticationException e) {
            System.out.println("ERREUR D'AUTHENTIFICATION: " + e.getMessage());
            System.out.println("Type d'exception: " + e.getClass().getSimpleName());

            System.out.println("Vérification manuelle de l'utilisateur...");
            Optional<User> userOpt = findUserByLogin(loginRequest.login());

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                System.out.println("Utilisateur existe dans la base: " + user.getUsername());
                System.out.println("Hash du mot de passe stocké: " + user.getPassword());
                System.out.println("Longueur du hash: " + user.getPassword().length());

                boolean passwordMatches = passwordEncoder.matches(loginRequest.password(), user.getPassword());
                System.out.println("Vérification manuelle du mot de passe: " + passwordMatches);

                if (!passwordMatches) {
                    System.out.println("Le mot de passe fourni ne correspond pas au hash stocké");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiant ou mot de passe incorrect");
                } else {
                    System.out.println("Le mot de passe correspond mais Spring Security échoue");
                    System.out.println("Cause probable: UserDetailsService cherche par username uniquement");
                }
            } else {
                System.out.println("Utilisateur non trouvé dans la base de données");
            }

            log.error("Erreur d'authentification : {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Identifiant ou mot de passe incorrect");
        } catch (Exception e) {
            System.out.println("ERREUR INATTENDUE: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur");
        }
    }


    @GetMapping("/generate-hash")
    public ResponseEntity<?> generateHash(@RequestParam String password) {
        System.out.println("Génération de hash BCrypt pour: " + password);

        try {
            String encodedPassword = passwordEncoder.encode(password);

            System.out.println("Hash généré: " + encodedPassword);
            System.out.println("Longueur: " + encodedPassword.length());

            // Tester la correspondance
            boolean matches = passwordEncoder.matches(password, encodedPassword);
            System.out.println("Test de correspondance: " + matches);

            Map<String, Object> response = new HashMap<>();
            response.put("password", password);
            response.put("hash", encodedPassword);
            response.put("length", encodedPassword.length());
            response.put("matches_test", matches);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("ERREUR lors de la génération du hash: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiError("Cet email est déjà utilisé"));
            }

            if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiError("Les mots de passe ne correspondent pas"));
            }

            String username = generateUsernameFromEmail(registerRequest.getEmail());

            if (userRepository.findByUsername(username).isPresent()) {
                int counter = 1;
                String baseUsername = username;
                while (userRepository.findByUsername(username).isPresent()) {
                    username = baseUsername + counter;
                    counter++;
                }
            }

            User newUser = User.builder()
                    .username(username)
                    .email(registerRequest.getEmail())
                    .fullName(registerRequest.getNom() + " " + registerRequest.getPrenom()) // Ajouter espace
                    .phone(registerRequest.getPhone())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .status(UserStatus.ACTIF) // Définir le statut
                    .build();

            Role demandeurRole = roleRepository.findByCode("DEMANDEUR")
                    .orElseThrow(() -> new RuntimeException("Rôle DEMANDEUR introuvable. Veuillez initialiser les rôles dans la base."));

            newUser.setRoles(new HashSet<>());
            newUser.getRoles().add(demandeurRole);

            userRepository.save(newUser);

            log.info("Nouvel utilisateur inscrit : {} (username: {})", newUser.getEmail(), newUser.getUsername());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new RegisterResponse(
                            "Inscription réussie ! Vous pouvez maintenant vous connecter.",
                            newUser.getEmail()
                    ));

        } catch (RuntimeException e) {
            log.error("Erreur lors de l'inscription : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError(e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur inattendue lors de l'inscription : {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiError("Une erreur est survenue lors de l'inscription"));
        }
    }

    private String generateUsernameFromEmail(String email) {
        String usernamePart = email.split("@")[0];
        usernamePart = usernamePart.replaceAll("[^a-zA-Z0-9]", "");
        return usernamePart.length() > 50 ? usernamePart.substring(0, 50) : usernamePart;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}