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
 * Comprehensive test suite for HTMLMessageContentValidator.
 * Tests HTML message content validation including security checks, length, and format validation.
 */
@DisplayName("HTMLMessageContentValidator Test Suite")
class HTMLMessageContentValidatorTest {

    @Mock
    private ChatService chatService;

    private HTMLMessageContentValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new HTMLMessageContentValidator(chatService);
    }

    @Nested
    @DisplayName("Basic Validation Tests")
    class BasicValidationTests {

        @Test
        @DisplayName("Should fail validation for null HTML message")
        void testNullMessage() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(null, context);

            assertFalse(result);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should fail validation for empty HTML message")
        void testEmptyMessage(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @ParameterizedTest
        @ValueSource(strings = {" ", "  ", "\t", "\n", "   \t\n   "})
        @DisplayName("Should fail validation for whitespace-only HTML message")
        void testWhitespaceOnly(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Length Validation Tests")
    class LengthValidationTests {

        @Test
        @DisplayName("Should pass validation for HTML message within length limits")
        void testValidLength() {
            String message = "<p>Hello, this is a test message.</p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for very long HTML message")
        void testVeryLongMessage() {
            String longMessage = "<p>" + "a".repeat(50000) + "</p>"; // Very long message (50008 total)

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(longMessage, context);

            assertTrue(result); // Should pass - no max length limit
        }

        @Test
        @DisplayName("Should pass validation for extremely long HTML message")
        void testExtremelyLongMessage() {
            String maxMessage = "a".repeat(100000); // Extremely long message (100000 characters)

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(maxMessage, context);

            assertTrue(result); // Should pass - no max length limit
        }
    }

    @Nested
    @DisplayName("Security Validation Tests - Script Tags")
    class ScriptTagValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "<script>alert('XSS')</script>",
                "<p>Hello</p><script>alert('XSS')</script>",
                "<SCRIPT>alert('XSS')</SCRIPT>",
                "<ScRiPt>alert('XSS')</ScRiPt>",
                "<script type='text/javascript'>alert('XSS')</script>",
                "Text before <script>alert('XSS')</script> text after"
        })
        @DisplayName("Should fail validation for HTML containing script tags")
        void testScriptTags(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should fail validation for nested script tags")
        void testNestedScriptTags() {
            String message = "<div><p><script>alert('XSS')</script></p></div>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Security Validation Tests - Iframe Tags")
    class IframeTagValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "<iframe src='http://evil.com'></iframe>",
                "<p>Hello</p><iframe src='http://evil.com'></iframe>",
                "<IFRAME src='http://evil.com'></IFRAME>",
                "<IFrAmE src='http://evil.com'></IFrAmE>",
                "<iframe width='560' height='315' src='http://evil.com'></iframe>",
                "Text before <iframe src='http://evil.com'></iframe> text after"
        })
        @DisplayName("Should fail validation for HTML containing iframe tags")
        void testIframeTags(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should fail validation for nested iframe tags")
        void testNestedIframeTags() {
            String message = "<div><p><iframe src='http://evil.com'></iframe></p></div>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Valid HTML Message Tests")
    class ValidHTMLMessageTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "<p>Hello, how are you?</p>",
                "<div>This is a test message.</div>",
                "<h1>Meeting at 3 PM today</h1>",
                "<strong>Important:</strong> Please review the document.",
                "<em>Thanks for your help!</em>",
                "<ul><li>Item 1</li><li>Item 2</li></ul>",
                "<a href='http://example.com'>Click here</a>"
        })
        @DisplayName("Should pass validation for valid HTML messages")
        void testValidHTMLMessages(String message) {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for complex valid HTML")
        void testComplexValidHTML() {
            String message = "<div><h2>Title</h2><p>Paragraph with <strong>bold</strong> and <em>italic</em> text.</p><ul><li>Item 1</li><li>Item 2</li></ul></div>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for HTML with attributes")
        void testHTMLWithAttributes() {
            String message = "<p class='message' id='msg1' style='color: blue;'>Hello World</p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for HTML with links")
        void testHTMLWithLinks() {
            String message = "<p>Visit <a href='https://example.com' target='_blank'>our website</a> for more info.</p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for HTML with images")
        void testHTMLWithImages() {
            String message = "<p>Check out this image: <img src='https://example.com/image.png' alt='Example' /></p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Content After HTML Strip Tests")
    class ContentAfterStripTests {

        @Test
        @DisplayName("Should fail validation for HTML with no meaningful content after stripping tags")
        void testNoContentAfterStrip() {
            String message = "<p></p><div></div><span></span>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should pass validation for HTML with meaningful content")
        void testMeaningfulContent() {
            String message = "<p>This is meaningful content</p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle HTML with leading/trailing whitespace")
        void testHTMLWithWhitespace() {
            String message = "  <p>Hello, this is a test message.</p>  ";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle plain text without HTML tags")
        void testPlainText() {
            String message = "This is plain text without any HTML tags.";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle HTML with special characters")
        void testHTMLWithSpecialCharacters() {
            String message = "<p>Price: $100 &amp; Tax: 10%</p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle self-closing tags")
        void testSelfClosingTags() {
            String message = "<p>Line 1<br/>Line 2<hr/>End</p>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle mixed case HTML tags")
        void testMixedCaseTags() {
            String message = "<P>Hello <STRONG>World</STRONG></P>";

            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(message, context);

            assertTrue(result);
        }
    }
}

