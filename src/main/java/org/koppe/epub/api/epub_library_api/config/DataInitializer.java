package org.koppe.epub.api.epub_library_api.config;

import org.koppe.epub.api.epub_library_api.jpa.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserService users;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        addInitialUser();
    }

    private void addInitialUser() {
        if (users.count() != 0L) {
            return;
        }
        logger.info("No users in database. adding initial user");
        String username = System.getenv("API_ADMIN_USER") != null ? System.getenv("API_ADMIN_USER") : "admin";
        String pw = System.getenv("API_ADMIN_PW") != null ? System.getenv("API_ADMIN_PW") : "admin";
        
        try {
            users.addUser(username, pw);
        } catch (Exception ex) {
            logger.warn("Failed to create initial user");
            return;
        }

        logger.info("Successfully created initial user");
    }

}
