package ru.LevLezhnin.NauJava.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import ru.LevLezhnin.NauJava.exceptions.EntityNotFoundException;
import ru.LevLezhnin.NauJava.model.User;
import ru.LevLezhnin.NauJava.repository.jpa.UserRepository;

import java.util.List;

@Component
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new EntityNotFoundException("Пользователь с логином '%s' не найден".formatted(username)));
            return new AppUserDetails(
                    user.getId(),
                    user.getUsername(),
                    user.getPasswordHash(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                    user.isActive());
        } catch (EntityNotFoundException e) {
            throw new UsernameNotFoundException("Пользователь с логином '%s' не найден".formatted(username), e);
        }
    }
}
