package org.koppe.epub.api.epub_library_api.web.controller;

import static org.mockito.ArgumentMatchers.any;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDate;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.service.AuthorService;
import org.koppe.epub.api.epub_library_api.jpa.service.EpubService;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(AuthorController.class)
@SuppressWarnings({ "unused" })
public class TestAuthorController {
    @MockitoBean
    private AuthorService as;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc client;
    private ObjectMapper mapper = new ObjectMapper();;

    private AuthorDto dto;
    private Author jpa;

    @BeforeEach
    public void BeforeEach() {
        dto = new AuthorDto(1L, "Test", "Test", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1),
                "Tests things",
                new HashSet<>(), new HashSet<>());

        jpa = new Author(1L, "Test", "Test", "Tests things", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1),
                new HashSet<>(), new HashSet<>());

        client = MockMvcBuilders.standaloneSetup(new AuthorController(as)).build();
    }

    public void testAddAuthor() throws JsonProcessingException, Exception {
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(""))
                .andExpect(status().isBadRequest());

        AuthorDto a = new AuthorDto();
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)))
                .andExpect(status().isBadRequest());

        a.setFirstName("");
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)))
                .andExpect(status().isBadRequest());

        a.setFirstName("     ");
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)))
                .andExpect(status().isBadRequest());

        a.setFirstName("Test");
        a.setSurname("");
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)))
                .andExpect(status().isBadRequest());

        a.setSurname("      ");
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)))
                .andExpect(status().isBadRequest());

        given(as.addAuthor(any(Author.class))).willReturn(jpa);
        client.perform(
                post("/authors")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writerWithDefaultPrettyPrinter()
                                .writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L));
    }
}
