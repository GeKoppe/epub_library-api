package org.koppe.epub.api.epub_library_api.web.controller;

import org.koppe.epub.api.epub_library_api.jpa.model.User;
import org.koppe.epub.api.epub_library_api.jpa.service.UserService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.UserDto;
import org.koppe.epub.api.epub_library_api.web.dto.UserResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/users")
@Tag(name = "Users", description = "Functionality for managing users")
@RequiredArgsConstructor
public class UserController {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(UserController.class);
    /**
     * Service for managing users in the database
     */
    private final UserService users;

    // #region create user
    @Operation(summary = "Create a new user", description = "Creates a new user in the system. Needs name and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User successfully created", content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing username or password", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Username already taken", content = @Content(schema = @Schema(implementation = ProblemDetail.class))) })
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDto> createUser(@RequestBody UserDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank() || dto.getPassword() == null
                || dto.getPassword().isBlank()) {
            logger.info("Invalid user dto given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No username or password given"))
                    .build();
        }

        if (users.userExistsByName(dto.getName())) {
            logger.info("Username {} already exists", dto.getName());
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), "Given username already exists"))
                    .build();
        }

        User user = users.addUser(dto.getName(), dto.getPassword());
        UserResponseDto response = DtoFactory.convertUserToResponseDto(user);
        return ResponseEntity.ok(response);
    }

    // #region delete user
    @Operation(summary = "Delete a user", description = "Deletes the user with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the user to be deleted", required = true) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted user", content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))

    })
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDto> deleteUser(@PathVariable(name = "id", required = true) Long id) {
        if (id == null || !users.userExistsById(id)) {
            logger.info("User with given id does not exist");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "User with given id not found"))
                    .build();
        }

        User user = users.deleteUserById(id);
        UserResponseDto dto = DtoFactory.convertUserToResponseDto(user);
        return ResponseEntity.ok(dto);
    }

    // #region update user
    @Operation(summary = "Update a user", description = "Updates password or name of a user. If username is already taken, 403 is returned.", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the user to be updated", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated user.", content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Username already taken", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable(name = "id", required = true) Long id,
            @RequestBody UserDto dto) {
        if (id == null || !users.userExistsById(id)) {
            logger.info("No valid user id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No user for given id found"))
                    .build();
        }

        if (dto == null) {
            logger.info("No updated valeus given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No request body given"))
                    .build();
        }

        User updated = users.updateUser(id, dto.getName(), dto.getPassword());
        if (updated == null) {
            logger.info("Username already taken, no update happened");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), "Username already taken"))
                    .build();
        }

        UserResponseDto resopnse = DtoFactory.convertUserToResponseDto(updated);
        return ResponseEntity.ok(resopnse);
    }
}
