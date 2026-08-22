package com.seninsight.backend.auth;

import com.seninsight.backend.business.user.User;
import com.seninsight.backend.business.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        System.out.println("🔍 CustomUserDetailsService cherche: " + identifier);

        User user = userRepository.findByEmail(identifier)
                .orElse(userRepository.findByUsername(identifier)
                        .orElseThrow(() -> {
                            System.out.println("❌ Utilisateur non trouvé: " + identifier);
                            return new UsernameNotFoundException("Utilisateur non trouvé: " + identifier);
                        }));

        System.out.println("✅ Utilisateur trouvé: " + user.getUsername());

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        System.out.println("📋 Authorities: " + authorities);

        return SecurityUserPrincipal.from(user, authorities);
    }
}