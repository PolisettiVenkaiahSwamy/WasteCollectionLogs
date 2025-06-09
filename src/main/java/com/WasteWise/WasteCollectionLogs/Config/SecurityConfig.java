package com.WasteWise.WasteCollectionLogs.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Global security configuration for the WasteWise WasteCollectionLogs application.
 * This class sets up the security rules for HTTP requests.
 */
@Configuration // Marks this class as a source of bean definitions for the Spring application context.
@EnableWebSecurity // Enables Spring Security's web security support and provides the Spring Security integration.
public class SecurityConfig {

    /**
     * Defines the security filter chain that Spring Security will use to protect HTTP requests.
     *
     * @param http The HttpSecurity object, which allows configuring web based security for specific http requests.
     * @return A SecurityFilterChain instance that defines the security rules.
     * @throws Exception If an error occurs during the configuration of the HttpSecurity.
     */
    @Bean // Declares a method that instantiates, configures, and initializes a new object to be managed by the Spring IoC container.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disables Cross-Site Request Forgery (CSRF) protection.
            // This is often done for APIs that are stateless or use token-based authentication (like JWT),
            // as CSRF protection is typically for session-based authentication.
            .csrf(csrf -> csrf.disable())
            // Configures authorization rules for HTTP requests.
            .authorizeHttpRequests(authorize -> authorize
                // Allows all incoming HTTP requests to be processed without any authentication or authorization.
                // This means any user can access any endpoint in the application.
                .anyRequest().permitAll() 
            );
        // Builds and returns the SecurityFilterChain configured with the above rules.
        return http.build();
    }
}