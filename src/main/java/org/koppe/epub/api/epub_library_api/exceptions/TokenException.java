package org.koppe.epub.api.epub_library_api.exceptions;

public class TokenException extends Exception {
    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
