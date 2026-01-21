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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ValidationUtils.
 * Tests all utility methods for validation including string, list, map, email, and number validation.
 */
@DisplayName("ValidationUtils Test Suite")
class ValidationUtilsTest {

    @Nested
    @DisplayName("String Null or Blank Tests")
    class StringNullOrBlankTests {

        @Test
        @DisplayName("Should return true for null string")
        void testNullString() {
            assertTrue(ValidationUtils.isStringNullOrBlank(null));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  ", "\t", "\n", "   \t\n   "})
        @DisplayName("Should return true for empty or whitespace strings")
        void testEmptyAndWhitespaceStrings(String input) {
            assertTrue(ValidationUtils.isStringNullOrBlank(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"a", "test", " test ", "  test  ", "hello world"})
        @DisplayName("Should return false for non-empty strings")
        void testNonEmptyStrings(String input) {
            assertFalse(ValidationUtils.isStringNullOrBlank(input));
        }
    }

    @Nested
    @DisplayName("List Null or Empty Tests")
    class ListNullOrEmptyTests {

        @Test
        @DisplayName("Should return true for null list")
        void testNullList() {
            assertTrue(ValidationUtils.isListNullOrEmpty(null));
        }

        @Test
        @DisplayName("Should return true for empty list")
        void testEmptyList() {
            assertTrue(ValidationUtils.isListNullOrEmpty(new ArrayList<>()));
            assertTrue(ValidationUtils.isListNullOrEmpty(Collections.emptyList()));
        }

        @Test
        @DisplayName("Should return false for non-empty list")
        void testNonEmptyList() {
            List<String> list = Arrays.asList("item1", "item2");
            assertFalse(ValidationUtils.isListNullOrEmpty(list));
        }

        @Test
        @DisplayName("Should return false for list with single element")
        void testSingleElementList() {
            List<String> list = Collections.singletonList("item");
            assertFalse(ValidationUtils.isListNullOrEmpty(list));
        }
    }

    @Nested
    @DisplayName("Map Null or Empty Tests")
    class MapNullOrEmptyTests {

        @Test
        @DisplayName("Should return true for null map")
        void testNullMap() {
            assertTrue(ValidationUtils.isMapNullOrEmpty(null));
        }

        @Test
        @DisplayName("Should return true for empty map")
        void testEmptyMap() {
            assertTrue(ValidationUtils.isMapNullOrEmpty(new HashMap<>()));
            assertTrue(ValidationUtils.isMapNullOrEmpty(Collections.emptyMap()));
        }

        @Test
        @DisplayName("Should return false for non-empty map")
        void testNonEmptyMap() {
            Map<String, String> map = new HashMap<>();
            map.put("key1", "value1");
            assertFalse(ValidationUtils.isMapNullOrEmpty(map));
        }

        @Test
        @DisplayName("Should return false for map with single entry")
        void testSingleEntryMap() {
            Map<String, String> map = Collections.singletonMap("key", "value");
            assertFalse(ValidationUtils.isMapNullOrEmpty(map));
        }
    }

    @Nested
    @DisplayName("Email Validation Tests")
    class EmailValidationTests {

        @Test
        @DisplayName("Should return false for null email")
        void testNullEmail() {
            assertFalse(ValidationUtils.isEmailValid(null));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  "})
        @DisplayName("Should return false for empty or whitespace email")
        void testEmptyEmail(String email) {
            assertFalse(ValidationUtils.isEmailValid(email));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "user@example.com",
                "test.user@example.com",
                "user+tag@example.co.uk",
                "user_name@example.com",
                "user-name@example.com",
                "user123@example.com",
                "a@b.co"
        })
        @DisplayName("Should return true for valid email addresses")
        void testValidEmails(String email) {
            assertTrue(ValidationUtils.isEmailValid(email));
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
        @DisplayName("Should return false for invalid email addresses")
        void testInvalidEmails(String email) {
            assertFalse(ValidationUtils.isEmailValid(email));
        }
    }

    @Nested
    @DisplayName("Number Validation Tests")
    class NumberValidationTests {

        @Test
        @DisplayName("Should return false for null number string")
        void testNullNumber() {
            assertFalse(ValidationUtils.isNumberValid(null));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  "})
        @DisplayName("Should return false for empty or whitespace number string")
        void testEmptyNumber(String number) {
            assertFalse(ValidationUtils.isNumberValid(number));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0",
                "1",
                "123",
                "-123",
                "123.45",
                "-123.45",
                "0.0",
                "1.0",
                "999999999",
                "-999999999"
        })
        @DisplayName("Should return true for valid number strings")
        void testValidNumbers(String number) {
            assertTrue(ValidationUtils.isNumberValid(number));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "abc",
                "12.34.56",
                "12a",
                "a12",
                "12 34",
                "12,34",
                "12.34.56.78",
                "NaN",
                "Infinity",
                "-Infinity"
        })
        @DisplayName("Should return false for invalid number strings")
        void testInvalidNumbers(String number) {
            assertFalse(ValidationUtils.isNumberValid(number));
        }
    }

    @Nested
    @DisplayName("Number Range Tests")
    class NumberRangeTests {

        @Test
        @DisplayName("Should return true for number within range")
        void testNumberWithinRange() {
            assertTrue(ValidationUtils.isNumberInRange(5, 0, 10));
            assertTrue(ValidationUtils.isNumberInRange(0, 0, 10));
            assertTrue(ValidationUtils.isNumberInRange(10, 0, 10));
        }

        @Test
        @DisplayName("Should return false for number outside range")
        void testNumberOutsideRange() {
            assertFalse(ValidationUtils.isNumberInRange(-1, 0, 10));
            assertFalse(ValidationUtils.isNumberInRange(11, 0, 10));
            assertFalse(ValidationUtils.isNumberInRange(100, 0, 10));
        }

        @Test
        @DisplayName("Should handle negative ranges")
        void testNegativeRanges() {
            assertTrue(ValidationUtils.isNumberInRange(-5, -10, 0));
            assertTrue(ValidationUtils.isNumberInRange(-10, -10, 0));
            assertTrue(ValidationUtils.isNumberInRange(0, -10, 0));
            assertFalse(ValidationUtils.isNumberInRange(-11, -10, 0));
            assertFalse(ValidationUtils.isNumberInRange(1, -10, 0));
        }

        @Test
        @DisplayName("Should handle decimal numbers")
        void testDecimalNumbers() {
            assertTrue(ValidationUtils.isNumberInRange(5.5, 0.0, 10.0));
            assertTrue(ValidationUtils.isNumberInRange(0.1, 0.0, 10.0));
            assertTrue(ValidationUtils.isNumberInRange(9.9, 0.0, 10.0));
            assertFalse(ValidationUtils.isNumberInRange(10.1, 0.0, 10.0));
        }
    }

    @Nested
    @DisplayName("Minimum Length Tests")
    class MinimumLengthTests {

        @Test
        @DisplayName("Should return false for null string")
        void testNullString() {
            assertFalse(ValidationUtils.hasMinLength(null, 5));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  "})
        @DisplayName("Should return false for empty or whitespace strings")
        void testEmptyStrings(String input) {
            assertFalse(ValidationUtils.hasMinLength(input, 5));
        }

        @Test
        @DisplayName("Should return true when string meets minimum length")
        void testMeetsMinLength() {
            assertTrue(ValidationUtils.hasMinLength("hello", 5));
            assertTrue(ValidationUtils.hasMinLength("hello world", 5));
        }

        @Test
        @DisplayName("Should return false when string is shorter than minimum length")
        void testBelowMinLength() {
            assertFalse(ValidationUtils.hasMinLength("hi", 5));
            assertFalse(ValidationUtils.hasMinLength("test", 5));
        }

        @Test
        @DisplayName("Should handle zero minimum length")
        void testZeroMinLength() {
            assertTrue(ValidationUtils.hasMinLength("", 0));
            assertTrue(ValidationUtils.hasMinLength("test", 0));
        }
    }

    @Nested
    @DisplayName("Maximum Length Tests")
    class MaximumLengthTests {

        @Test
        @DisplayName("Should return true for null string")
        void testNullString() {
            assertTrue(ValidationUtils.hasMaxLength(null, 5));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"", " ", "  "})
        @DisplayName("Should return true for empty or whitespace strings")
        void testEmptyStrings(String input) {
            assertTrue(ValidationUtils.hasMaxLength(input, 5));
        }

        @Test
        @DisplayName("Should return true when string is within maximum length")
        void testWithinMaxLength() {
            assertTrue(ValidationUtils.hasMaxLength("hi", 5));
            assertTrue(ValidationUtils.hasMaxLength("hello", 5));
        }

        @Test
        @DisplayName("Should return false when string exceeds maximum length")
        void testExceedsMaxLength() {
            assertFalse(ValidationUtils.hasMaxLength("hello world", 5));
            assertFalse(ValidationUtils.hasMaxLength("testing", 5));
        }

        @Test
        @DisplayName("Should handle zero maximum length")
        void testZeroMaxLength() {
            assertTrue(ValidationUtils.hasMaxLength("", 0));
            assertFalse(ValidationUtils.hasMaxLength("test", 0));
        }
    }
}

