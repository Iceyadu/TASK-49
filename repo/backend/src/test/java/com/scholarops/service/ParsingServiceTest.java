package com.scholarops.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParsingServiceTest {

    private ParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new ParsingService();
    }

    @Test
    void testExtractWithCssSelector() {
        String html = "<html><body><h1 class='title'>Hello World</h1>"
                + "<p class='desc'>Description</p></body></html>";

        List<String> results = parsingService.extractWithCssSelector(html, "h1.title");

        assertEquals(1, results.size());
        assertEquals("Hello World", results.get(0));
    }

    @Test
    void testExtractWithRegex() {
        String content = "Price: $100, Tax: $15, Total: $115";

        List<String> results = parsingService.extractWithRegex(content, "\\$(\\d+)");

        assertEquals(3, results.size());
        assertEquals("100", results.get(0));
        assertEquals("15", results.get(1));
        assertEquals("115", results.get(2));
    }

    @Test
    void testExtractWithJsonPath() {
        String json = "{\"store\":{\"books\":[{\"title\":\"Book A\"},{\"title\":\"Book B\"}]}}";

        List<String> results = parsingService.extractWithJsonPath(json, "$.store.books[*].title");

        assertEquals(2, results.size());
        assertTrue(results.contains("Book A"));
        assertTrue(results.contains("Book B"));
    }
}
