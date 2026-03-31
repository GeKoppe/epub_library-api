package org.koppe.epub.api.epub_library_api.security;

import java.io.IOException;

import org.koppe.epub.api.epub_library_api.jpa.service.UserService;
import org.koppe.epub.api.epub_library_api.utility.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.lang.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    /**
     * Service for working with users
     */
    private final UserService users;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank() || authHeader.indexOf("Bearer") != 0) {
            logger.info("No valid auth header given");
            writeUnauthorized(response, "No authorization token given");
            return;
        }

        String jwt = authHeader.substring(7);
        if (!JwtUtils.validate(jwt, users)) {
            logger.info("Invalid jwt given");
            writeUnauthorized(response, "Invalid jwt given");
            return;
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user",
                JwtUtils.getUser(jwt), Collections.emptyList());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    /**
     * Writes the unauthorized response
     * 
     * @param response
     * @param message
     * @throws IOException
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.contains("/auth/login") ||
                path.contains("/auth/refresh") ||
                path.contains("/v1/api-docs") ||
                path.contains("/v3/api-docs") ||
                path.contains("/releases") ||
                path.contains("/swagger-ui");
    }

}
