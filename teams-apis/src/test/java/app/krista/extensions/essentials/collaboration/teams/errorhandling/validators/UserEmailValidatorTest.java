/*
 * Teams Api Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.essentials.collaboration.teams.errorhandling.validators;

import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Comprehensive test suite for UserEmailValidator.
 * Tests email format validation and user existence validation in Microsoft Teams.
 */
@DisplayName("UserEmailValidator Test Suite")
class UserEmailValidatorTest {

    @Mock
    private ChatService chatService;

    private UserEmailValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new UserEmailValidator(chatService);
    }

    @Nested
    @DisplayName("Email Format Validation Tests")
    class EmailFormatValidationTests {

        @Test
        @DisplayName("Should fail validation for null email")
        void testNullEmail() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(null, context);

            assertFalse(result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  ", "\t", "\n"})
        @DisplayName("Should fail validation for empty or whitespace email")
        void testEmptyEmail(String email) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(email, context);

            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "invalid",
                "@example.com",
                "user@",
                "user @example.com",
                "user@example",
                "user..name@example.com",
                "user@.example.com",
                "user@example..com",
                "user name@example.com",
                "user@exam ple.com",
                "user@@example.com",
                "user@example.c",
                "user@example.com.",
                ".user@example.com"
        })
        @DisplayName("Should fail validation for invalid email format")
        void testInvalidEmailFormat(String email) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(email, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("User Existence Validation Tests")
    class UserExistenceValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "test.user@example.com",
                "user+tag@example.co.uk",
                "user_name@example.com",
                "user-name@example.com"
        })
        @DisplayName("Should pass validation when user exists in Teams")
        void testUserExistsInTeams(String email) {
            // Mock user exists in Teams
            when(chatService.validateUserExists(anyString())).thenReturn(true);

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(email, context);

            assertTrue(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "nonexistent@example.com",
                "invalid.user@example.com"
        })
        @DisplayName("Should fail validation when user does not exist in Teams")
        void testUserDoesNotExistInTeams(String email) {
            // Mock user does not exist in Teams
            when(chatService.validateUserExists(anyString())).thenReturn(false);

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(email, context);

            assertFalse(result);
        }

    }

    @Nested
    @DisplayName("Error Message Tests")
    class ErrorMessageTests {

        @Test
        @DisplayName("Should provide appropriate error message for invalid format")
        void testFormatErrorMessage() {
            String invalidEmail = "invalid-email";

            String errorMessage = validator.getErrMessage(invalidEmail);

            assertNotNull(errorMessage);
            assertTrue(errorMessage.contains("invalid-email") ||
                      errorMessage.toLowerCase().contains("invalid") ||
                      errorMessage.toLowerCase().contains("email"));
        }

        @Test
        @DisplayName("Should provide appropriate error message for non-existent user")
        void testBusinessLogicErrorMessage() {
            when(chatService.validateUserExists(anyString())).thenReturn(false);

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            String email = "user@example.com";

            Boolean result = validator.validate(email, context);

            assertFalse(result);

            String errorMessage = validator.getErrMessage(email);
            assertNotNull(errorMessage);
        }
    }

    @Nested
    @DisplayName("Validation Messages Tests")
    class ValidationMessagesTests {

        @Test
        @DisplayName("Should provide fetch step message")
        void testFetchStepMessage() {
            String message = validator.getFetchStepMessage();

            assertNotNull(message);
            assertFalse(message.isEmpty());
            assertTrue(message.toLowerCase().contains("email") ||
                      message.toLowerCase().contains("valid"));
        }

        @Test
        @DisplayName("Should provide confirmation step message")
        void testConfirmationStepMessage() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();
            String message = validator.getConfirmationStepMessage("user@example.com", context);

            assertNotNull(message);
            assertFalse(message.isEmpty());
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle email with leading/trailing whitespace")
        void testEmailWithWhitespace() {
            when(chatService.validateUserExists(anyString())).thenReturn(true);

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate("  user@example.com  ", context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle very long email address")
        void testVeryLongEmail() {
            String longEmail = "a".repeat(250) + "@example.com";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(longEmail, context);

            // Should fail due to length or format
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle email with special characters")
        void testEmailWithSpecialCharacters() {
            when(chatService.validateUserExists(anyString())).thenReturn(true);

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate("user+tag@example.com", context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle case sensitivity appropriately")
        void testCaseSensitivity() {
            when(chatService.validateUserExists(anyString())).thenReturn(true);

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result1 = validator.validate("User@Example.Com", context);
            Boolean result2 = validator.validate("user@example.com", context);

            // Both should be valid (email is case-insensitive)
            assertTrue(result1);
            assertTrue(result2);
        }
    }
}

