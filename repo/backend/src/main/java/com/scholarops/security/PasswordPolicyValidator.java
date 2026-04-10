package com.scholarops.security;

import com.scholarops.exception.PasswordPolicyViolationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 12;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]");

    public void validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.length() < MIN_LENGTH) {
            violations.add("Password must be at least " + MIN_LENGTH + " characters long");
        }

        if (password != null) {
            if (!UPPERCASE_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one uppercase letter");
            }
            if (!LOWERCASE_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one lowercase letter");
            }
            if (!DIGIT_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one digit");
            }
            if (!SYMBOL_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{}|;':\"\\,.<>/?`~)");
            }
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }

    public boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (PasswordPolicyViolationException e) {
            return false;
        }
    }
}
