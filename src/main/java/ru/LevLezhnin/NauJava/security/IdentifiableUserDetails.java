package ru.LevLezhnin.NauJava.security;

import org.springframework.security.core.userdetails.UserDetails;

public interface IdentifiableUserDetails extends UserDetails {
    Long getId();
}
