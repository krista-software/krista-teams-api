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

/**
 * Comprehensive test suite for MessageContentValidator.
 * Tests message content validation including length, format, and quality checks.
 */
@DisplayName("MessageContentValidator Test Suite")
class MessageContentValidatorTest {

    @Mock
    private ChatService chatService;

    private MessageContentValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new MessageContentValidator(chatService);
    }

    @Nested
    @DisplayName("Length Validation Tests")
    class LengthValidationTests {

        @Test
        @DisplayName("Should fail validation for null message")
        void testNullMessage() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(null, context);

            assertFalse(result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should fail validation for empty message")
        void testEmptyMessage(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should pass validation for message within length limits")
        void testValidLength() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate("Hello, this is a test message.", context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for very long message")
        void testVeryLongMessage() {
            // Create a message with varied content to avoid excessive repetition pattern
            StringBuilder sb = new StringBuilder();
            String baseText = "This is a test message with varied content. ";
            while (sb.length() < 50000) {
                sb.append(baseText);
            }
            String longMessage = sb.substring(0, 50000); // Very long message (50000 characters)

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(longMessage, context);

            assertTrue(result); // Should pass - no max length limit
        }

        @Test
        @DisplayName("Should pass validation for extremely long message")
        void testExtremelyLongMessage() {
            // Create a message with varied content to avoid excessive repetition pattern
            StringBuilder sb = new StringBuilder();
            String baseText = "This is a test message with varied content. ";
            while (sb.length() < 100000) {
                sb.append(baseText);
            }
            String maxMessage = sb.substring(0, 100000); // Extremely long message (100000 characters)

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(maxMessage, context);

            assertTrue(result); // Should pass - no max length limit
        }
    }

    @Nested
    @DisplayName("Content Quality Validation Tests")
    class ContentQualityValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {" ", "  ", "\t", "\n", "   \t\n   ", "\r\n"})
        @DisplayName("Should fail validation for whitespace-only content")
        void testWhitespaceOnly(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {"!!!", "###", "***", "@@@", "...", "---"})
        @DisplayName("Should fail validation for special characters only")
        void testSpecialCharactersOnly(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should pass validation for character repetition (no character-level validation)")
        void testCharacterRepetition() {
            String message = "aaaaaaaaaaaaa"; // 13 consecutive 'a's - now allowed (no max length check)

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result); // Should pass - validator only checks word-level patterns, not character repetition
        }

        @Test
        @DisplayName("Should pass validation for normal character repetition")
        void testNormalRepetition() {
            String message = "Hello world"; // Normal text

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Newline Validation Tests")
    class NewlineValidationTests {

        @Test
        @DisplayName("Should fail validation for excessive consecutive newlines")
        void testExcessiveNewlines() {
            // 6 consecutive newlines create 7 empty lines (more than MAX_CONSECUTIVE_NEWLINES of 5)
            String message = "Line 1\n\n\n\n\n\n\nLine 2"; // 7 newlines = 6 empty lines between lines

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should pass validation for acceptable newlines")
        void testAcceptableNewlines() {
            String message = "Line 1\n\nLine 2\n\nLine 3"; // Acceptable newlines

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Spam Pattern Detection Tests")
    class SpamPatternDetectionTests {

        @Test
        @DisplayName("Should fail validation for excessive word repetition")
        void testExcessiveWordRepetition() {
            String message = "buy buy buy buy buy buy buy buy buy buy buy buy"; // Same word repeated excessively

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should fail validation for excessive capitalization")
        void testExcessiveCapitalization() {
            String message = "THIS IS A VERY LONG MESSAGE WITH EXCESSIVE CAPITALIZATION THAT LOOKS LIKE SHOUTING";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should pass validation for normal capitalization")
        void testNormalCapitalization() {
            String message = "This is a normal message with proper capitalization.";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Valid Message Tests")
    class ValidMessageTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "Hello, how are you?",
                "This is a test message.",
                "Meeting at 3 PM today.",
                "Please review the document.",
                "Thanks for your help!",
                "Can you send me the report?",
                "Let's schedule a call tomorrow."
        })
        @DisplayName("Should pass validation for valid messages")
        void testValidMessages(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for message with numbers")
        void testMessageWithNumbers() {
            String message = "The meeting is scheduled for 3:00 PM on 12/25/2024.";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for message with punctuation")
        void testMessageWithPunctuation() {
            String message = "Hello! How are you? I hope you're doing well.";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for multi-line message")
        void testMultiLineMessage() {
            String message = "Line 1\nLine 2\nLine 3";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle message with leading/trailing whitespace")
        void testMessageWithWhitespace() {
            String message = "  Hello, this is a test message.  ";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle message with mixed content")
        void testMixedContent() {
            String message = "Hello! 123 Test @#$ Message";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle single character message")
        void testSingleCharacter() {
            String message = "a";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle message at recommended length boundary")
        void testRecommendedLengthBoundary() {
            // Create a message with varied content to avoid excessive repetition pattern
            StringBuilder sb = new StringBuilder();
            String baseText = "This is a test message with varied content. ";
            while (sb.length() < 2000) {
                sb.append(baseText);
            }
            String message = sb.substring(0, 2000); // At recommended max length

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }
}

