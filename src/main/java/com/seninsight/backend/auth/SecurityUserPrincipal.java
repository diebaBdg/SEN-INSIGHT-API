package com.seninsight.backend.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Principal JWT avec l'identifiant utilisateur (requis pour @PreAuthorize principal.id).
 */
@Getter
public class SecurityUserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean credentialsNonExpired;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserPrincipal(
            UUID id,
            String email,
            String username,
            String password,
            boolean enabled,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    public static SecurityUserPrincipal from(User user, Collection<? extends GrantedAuthority> authorities) {
        boolean active = UserStatus.ACTIF.equals(user.getStatus());
        boolean notSuspended = !UserStatus.SUSPENDU.equals(user.getStatus());
        return new SecurityUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getPassword(),
                active,
                true,
                true,
                notSuspended,
                authorities
        );
    }
}
