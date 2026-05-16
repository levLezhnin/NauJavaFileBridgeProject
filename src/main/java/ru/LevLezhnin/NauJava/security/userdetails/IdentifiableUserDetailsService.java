package ru.LevLezhnin.NauJava.security.userdetails;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface IdentifiableUserDetailsService extends UserDetailsService {
    UserDetails loadUserById(Long id);
}
