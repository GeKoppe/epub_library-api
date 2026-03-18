package org.koppe.epub.api.epub_library_api.jpa.service;

import java.time.LocalDate;
import java.util.Optional;

import org.koppe.epub.api.epub_library_api.exceptions.NoSuchUserException;
import org.koppe.epub.api.epub_library_api.jpa.model.User;
import org.koppe.epub.api.epub_library_api.jpa.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    /**
     * JPA repository for working with users in the database
     */
    private final UserRepository users;
    /**
     * Password encoder
     */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // #region add user
    /**
     * 
     * @param name
     * @param pw
     * @return
     * @throws IllegalArgumentException
     */
    @Transactional
    public User addUser(String name, String pw) throws IllegalArgumentException {
        if (name == null || name.isBlank() || pw == null || pw.isBlank()) {
            logger.info("No user or password given");
            throw new IllegalArgumentException("No user or password given");
        }

        User user = new User(null, name, encoder.encode(pw), LocalDate.now());
        User added = users.save(user);

        logger.info("Added user {}", added);
        return added;
    }

    // #region exists by name
    @Transactional(readOnly = true)
    public boolean userExistsByName(String name) throws IllegalArgumentException {
        if (name == null || name.isBlank()) {
            logger.info("No name given");
            throw new IllegalArgumentException("No name given");
        }

        Optional<User> userOpt = users.findByName(name);
        if (userOpt.isEmpty()) {
            logger.info("User with name {} does not exist", name);
            return false;
        }
        logger.info("User with name {} does exist", name);
        return true;
    }

    // #region exists by id
    @Transactional(readOnly = true)
    public boolean userExistsById(long id) {
        return users.existsById(id);
    }

    // #region find by id
    @Transactional(readOnly = true)
    public User findById(long id) throws IllegalArgumentException {
        if (!userExistsById(id)) {
            throw new IllegalArgumentException("User with given id does not exist");
        }

        return users.findById(id).get();
    }

    // #region validate user pw comb
    @Transactional(readOnly = true)
    public boolean validateUserPasswordCombination(String username, String password)
            throws IllegalArgumentException, NoSuchUserException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            logger.info("No username or password given");
            throw new IllegalArgumentException();
        }

        Optional<User> userOpt = users.findByName(username);
        if (userOpt.isEmpty()) {
            logger.info("Invalid username given");
            throw new NoSuchUserException("Username " + username + " is invalid");
        }

        User user = userOpt.get();
        if (!encoder.matches(password, user.getPwHash())) {
            logger.info("Username and password don't match");
            return false;
        }
        logger.info("Successfully validated password");
        return true;
    }

    // #region delete user by id
    @Transactional
    public User deleteUserById(long id) {
        if (!userExistsById(id)) {
            logger.info("User with given id does not exist");
            throw new IllegalArgumentException("User with given id does not exist");
        }

        User user = users.findById(id).get();
        logger.info("Deletign user {}", id);
        users.delete(user);

        return user;
    }

    @Transactional
    public User updateUser(long id, String name, String password) {
        if (!userExistsById(id)) {
            logger.info("User with given id does not exist");
            throw new IllegalArgumentException("User with given id does not exist");
        }

        User user = users.findById(id).get();
        if (name != null && !name.isBlank() && !name.equals(user.getName())) {
            if (userExistsByName(name)) {
                logger.info("Cannot update user, name already taken");
                return null;
            }
            user.setName(name);
        }

        if (password != null && !password.isBlank()) {
            user.setPwHash(encoder.encode(password));
        }
        return users.save(user);
    }

    @Transactional(readOnly = true)
    public long count() {
        return users.count();
    }
}
