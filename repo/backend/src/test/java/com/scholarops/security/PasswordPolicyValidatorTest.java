package com.scholarops.security;

import com.scholarops.exception.PasswordPolicyViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyValidatorTest {

    private PasswordPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordPolicyValidator();
    }

    @Test
    void testValidPassword() {
        assertDoesNotThrow(() -> validator.validate("StrongPass1!xy"));
        assertTrue(validator.isValid("StrongPass1!xy"));
    }

    @Test
    void testTooShort() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate("Short1!a")
        );
        assertTrue(ex.getViolations().stream()
                .anyMatch(v -> v.contains("at least 12 characters")));
    }

    @Test
    void testNoUppercase() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate("nouppercase1!abc")
        );
        assertTrue(ex.getViolations().stream()
                .anyMatch(v -> v.contains("uppercase")));
    }

    @Test
    void testNoLowercase() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate("NOLOWERCASE1!ABC")
        );
        assertTrue(ex.getViolations().stream()
                .anyMatch(v -> v.contains("lowercase")));
    }

    @Test
    void testNoDigit() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate("NoDigitHere!abcd")
        );
        assertTrue(ex.getViolations().stream()
                .anyMatch(v -> v.contains("digit")));
    }

    @Test
    void testNoSymbol() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate("NoSymbol1abcdef")
        );
        assertTrue(ex.getViolations().stream()
                .anyMatch(v -> v.contains("special character")));
    }

    @Test
    void testNullPassword() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate(null)
        );
        assertTrue(ex.getViolations().stream()
                .anyMatch(v -> v.contains("at least 12 characters")));
    }

    @Test
    void testExactlyMinLength() {
        // Exactly 12 characters meeting all criteria
        assertDoesNotThrow(() -> validator.validate("Abcdefgh1!xy"));
        assertTrue(validator.isValid("Abcdefgh1!xy"));
    }

    @Test
    void testMultipleViolations() {
        PasswordPolicyViolationException ex = assertThrows(
                PasswordPolicyViolationException.class,
                () -> validator.validate("short")
        );
        // Should have violations for: length, uppercase, digit, symbol
        assertTrue(ex.getViolations().size() >= 3);
    }
}
