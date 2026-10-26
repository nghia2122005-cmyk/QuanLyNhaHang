package vn.ueh.kltn.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import vn.ueh.kltn.restaurant.security.ApiSecurityErrorHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        var authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        var authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(authorities);
        return authenticationConverter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment environment,
            ApiSecurityErrorHandler errors,
            JwtAuthenticationConverter jwtAuthenticationConverter)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorize -> {
                            authorize
                                    .requestMatchers(HttpMethod.POST, "/api/auth/login")
                                    .permitAll();
                            if (environment.acceptsProfiles(Profiles.of("local"))) {
                                authorize
                                        .requestMatchers(
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**")
                                        .permitAll();
                            }
                            authorize.anyRequest().authenticated();
                        })
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(errors)
                                        .accessDeniedHandler(errors))
                .oauth2ResourceServer(
                        resource ->
                                resource.jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter))
                                        .authenticationEntryPoint(errors)
                                        .accessDeniedHandler(errors));
        return http.build();
    }
}
