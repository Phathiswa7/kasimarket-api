
package com.kasimarket.kasimarket_api.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// A "filter" is a guard that looks at EVERY request before it reaches our code.
// OncePerRequestFilter makes sure the guard checks each request exactly once.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // Spring gives us the JwtService, which can read tokens.
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // The app sends its token in a header like: Authorization: Bearer eyJ...
        String header = request.getHeader("Authorization");

        // Only continue if the header exists and starts with "Bearer ".
        if (header != null && header.startsWith("Bearer ")) {

            // Cut off the first 7 characters ("Bearer ") to keep just the token.
            String token = header.substring(7);

            try {
                // Read the token. This throws an error if it is fake, changed or expired.
                Claims claims = jwtService.parseToken(token);

                // Pull out who it belongs to and what role they have.
                String email = claims.getSubject();
                String role = claims.get("role", String.class);

                // Spring Security expects roles to start with "ROLE_".
                var authentication = new UsernamePasswordAuthenticationToken(
                        email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

                // Tell Spring Security: "this request comes from a logged-in person".
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                // Bad token: do nothing. The person stays "not logged in",
                // and the security rules below will turn them away.
            }
        }

        // Pass the request on to the next guard, or to our code.
        filterChain.doFilter(request, response);
    }
}