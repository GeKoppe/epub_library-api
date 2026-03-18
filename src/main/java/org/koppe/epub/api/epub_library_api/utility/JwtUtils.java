package org.koppe.epub.api.epub_library_api.utility;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.koppe.epub.api.epub_library_api.exceptions.TokenException;
import org.koppe.epub.api.epub_library_api.jpa.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;

public final class JwtUtils {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    /**
     * Secret key
     */
    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();
    /**
     * Expiration of tokens (60 minutes)
     */
    private static final long EXPIRATION_TIME = 3600000L;
    /**
     * Issuer for tokens
     */
    private static final String ISSUER = "epub-library";
    /**
     * ID for refresh tokens. Gets resetted when application restarts, therefore all
     * refresh tokens can be invalidated by simple server restart.
     */
    private static final String REFRESH_ID = UUID.randomUUID().toString();

    public static final String generateToken(String userName) {
        return Jwts.builder()
                .subject(userName)
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .claim("user", userName)
                .signWith(SECRET_KEY)
                .compact();
    }

    public static String generateRefreshToken(String userName) {
        return Jwts.builder()
                .subject(userName)
                .issuer(ISSUER)
                .claim("refresh", userName)
                .claim("refresh-id", REFRESH_ID)
                .issuedAt(new Date())
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validate(String token, UserService srv) {
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token);
            String user = claims.getPayload().get("user", String.class);
            if (user == null || user.isBlank()) {
                return false;
            }
            if (!srv.userExistsByName(user)) {
                return false;
            }
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Validates the given refresh token. Checks if it is a token at all, if it is a
     * refresh token and if the user name is given
     * 
     * @param token Token to be validated
     * @return True, if token is valid, false otherwise
     */
    private static boolean validateRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token);
            return ((String) claims.getPayload().get("refresh-id")).equals(REFRESH_ID)
                    && ((String) claims.getPayload().get("refresh")) != null
                    && !((String) claims.getPayload().get("refresh")).isBlank();
        } catch (Exception ex) {
            logger.info("Exception occurred during validation of refresh token", ex);
            return false;
        }
    }

    /**
     * 
     * @param refreshToken
     * @return
     * @throws TokenException
     */
    public static String refresh(String refreshToken) throws TokenException {
        if (!validateRefreshToken(refreshToken)) {
            return null;
        }

        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(refreshToken);
            String userName = (String) claims.getPayload().get("refresh");
            return generateToken(userName);
        } catch (Exception ex) {
            logger.info("Exception occurred while creating a new token", ex);
            throw new TokenException("Could not create a new token", ex);
        }
    }

    public static String getUser(String jwt) {
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(jwt);
            String user = claims.getPayload().get("user", String.class);
            if (user == null || user.isBlank()) {
                return null;
            }
            return user;
        } catch (Exception ex) {
            return null;
        }
    }
}
