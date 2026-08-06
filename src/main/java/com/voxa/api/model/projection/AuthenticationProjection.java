package com.voxa.api.model.projection;

import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

public interface AuthenticationProjection {
    String getId();
    Set<? extends GrantedAuthority> getAuthorities();
}
