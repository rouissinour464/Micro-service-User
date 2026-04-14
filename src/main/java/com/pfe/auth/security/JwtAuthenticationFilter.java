package com.pfe.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    // ✅ URL PUBLIQUES (pas de JWT obligatoire)
    private static final List<String> PUBLIC_URLS = List.of(
            "/api/auth/login",
            "/api/auth/register-admin",
            "/api/auth/refresh"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ✅ 1. Ignorer les routes publiques
        if (PUBLIC_URLS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ 2. Vérifier présence du header Authorization
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // ✅ 3. Vérifier validité du token
        if (!jwtUtils.isTokenValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        // ✅ 4. Extraire email depuis JWT
        String email = jwtUtils.extractEmail(token);

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ✅ 5. Charger utilisateur depuis la BD
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // ✅ 6. Créer l'objet d'authentification
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // ✅ 7. Définir l'utilisateur authentifié
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        // ✅ 8. Continuer la chaîne
        chain.doFilter(request, response);
    }
}