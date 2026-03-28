package ru.LevLezhnin.NauJava.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.LevLezhnin.NauJava.model.UserRole;

import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final UserDetailsService userDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(UserDetailsService userDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public PasswordEncoder passwordEncryptor() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Статика и публичные страницы
                        .requestMatchers("/", "/login", "/register", "/forbidden", "/files", "/upload", "/profile", "/download/**", "/users/list", "/css/**", "/js/**", "/assets/**").permitAll()

                        // Actuator
                        .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll()
                        .requestMatchers("/actuator/**").hasRole(UserRole.ADMIN.name())

                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/admin/**").hasRole(UserRole.ADMIN.name())

                        // Auth
                        .requestMatchers("/api/v*/auth/**").permitAll()

                        // Админский API
                        .requestMatchers("/api/v*/admin/**").hasRole(UserRole.ADMIN.name())

                        // Публичный API
                        .requestMatchers("/api/**").hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())

                        // Другое
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(((request, response, authException) -> {

                            log.debug("Попытка неавторизованного доступа: {} {}", request.getMethod(), request.getRequestURI());

                            ObjectMapper objectMapper = new ObjectMapper();

                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "error", "Unauthorized",
                                    "message", "Неавторизованный доступ к ресурсу",
                                    "path", request.getRequestURI()
                            ));
                        }))
                        .accessDeniedHandler(((request, response, accessDeniedException) -> {
                            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

                            log.warn("Попытка обращения к запрещённому ресурсу: Логин = '{}', Роли = {}, URI = {}",
                                    auth != null ? auth.getName() : "Аноним",
                                    auth != null ? auth.getAuthorities() : "Нет",
                                    request.getRequestURI());

                            ObjectMapper objectMapper = new ObjectMapper();

                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            objectMapper.writeValue(response.getOutputStream(), Map.of(
                                    "error", "Forbidden",
                                    "message", "Запрещён доступ к ресурсу",
                                    "path", request.getRequestURI()
                            ));
                        })))
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(PasswordEncoder passwordEncryptor) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncryptor);
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
