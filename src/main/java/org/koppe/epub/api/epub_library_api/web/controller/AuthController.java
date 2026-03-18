package org.koppe.epub.api.epub_library_api.web.controller;

import org.koppe.epub.api.epub_library_api.exceptions.NoSuchUserException;
import org.koppe.epub.api.epub_library_api.exceptions.TokenException;
import org.koppe.epub.api.epub_library_api.jpa.service.UserService;
import org.koppe.epub.api.epub_library_api.utility.JwtUtils;
import org.koppe.epub.api.epub_library_api.web.dto.JwtDto;
import org.koppe.epub.api.epub_library_api.web.dto.RefreshDto;
import org.koppe.epub.api.epub_library_api.web.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/auth")
@Tag(name = "Authorisation", description = "Allows to log in and out of the application")
@RequiredArgsConstructor
public class AuthController {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(AuthController.class);
    /**
     * Service for managing users in the database
     */
    private final UserService users;

    // #region login
    @Operation(summary = "Login to the application", security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = JwtDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing credentials", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JwtDto> login(@RequestBody UserDto user) {
        if (user == null || user.getName() == null || user.getName().isBlank() || user.getPassword() == null
                || user.getPassword().isBlank()) {
            logger.info("Missing username or password");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                    "Missing username or password")).build();
        }

        try {
            if (!users.validateUserPasswordCombination(user.getName(), user.getPassword())) {
                logger.info("Given username and password don't match");
                return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(401),
                        "Invalid combination of username and password")).build();
            }
        } catch (NoSuchUserException ex) {
            logger.info("Given username does not exist in the system");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(401), "No such user exists")).build();
        } catch (Exception ex) {
            logger.info("Exception occurred in validation of username password combination", ex);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid credentials given"))
                    .build();
        }

        String jwtToken = JwtUtils.generateToken(user.getName());
        String refreshToken = JwtUtils.generateRefreshToken(user.getName());

        JwtDto dto = new JwtDto(jwtToken, refreshToken);
        return ResponseEntity
                .status(200)
                .header("Authorization", "Bearer " + jwtToken)
                .body(dto);
    }

    // #region refresh
    @Operation(summary = "Refresh login", security = @SecurityRequirement(name = ""))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refreshed access token"),
            @ApiResponse(responseCode = "400", description = "No refresh token given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token given", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> refresh(@RequestBody RefreshDto dto) {
        if (dto == null || dto.getRefreshToken() == null || dto.getRefreshToken().isBlank()) {
            logger.info("No refresh token given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No refresh token given"))
                    .build();
        }

        String token = null;
        try {
            token = JwtUtils.refresh(dto.getRefreshToken());
        } catch (TokenException e) {
            logger.info("Invalid refresh token given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(401), "Invalid refresh token")).build();
        }
        if (token == null) {
            logger.info("Could not create a new jwt as refresh token is invalid");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(401), "invalid refresh token given"))
                    .build();
        }

        return ResponseEntity.status(200).header("Authorization", "Bearer " + token).build();
    }
}
