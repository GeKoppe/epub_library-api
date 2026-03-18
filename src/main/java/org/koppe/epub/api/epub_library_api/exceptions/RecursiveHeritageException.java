package org.koppe.epub.api.epub_library_api.exceptions;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RecursiveHeritageException extends Exception {
    public RecursiveHeritageException(String message) {
        super(message);
    }

    public RecursiveHeritageException(String message, Throwable cause) {
        super(message, cause);
    }
}
