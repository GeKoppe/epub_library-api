package org.koppe.epub.api.epub_library_api.exceptions;

import org.springframework.http.MediaType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MediaTypeException extends Exception {
    private MediaType[] allowed;
    private MediaType provided;

    public MediaTypeException(String message, Throwable cause, MediaType[] allowed, MediaType provided) {
        super(message, cause);
        this.allowed = allowed;
        this.provided = provided;
    }
}
