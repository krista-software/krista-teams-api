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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DateRangeValidator.
 * Tests date range validation including null checks, chronological order, and range limits.
 */
@DisplayName("DateRangeValidator Test Suite")
class DateRangeValidatorTest {

    private DateRangeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();
    }

    @Nested
    @DisplayName("Null Validation Tests")
    class NullValidationTests {

        @Test
        @DisplayName("Should pass validation for null from date and null to date")
        void testNullFromDate() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(null, context);

            // When both dates are null, validation passes
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Valid Date Range Tests")
    class ValidDateRangeTests {

        @Test
        @DisplayName("Should pass validation for valid date range")
        void testValidDateRange() {
            Long fromDate = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation when to date is null")
        void testToDateNull() {
            Long fromDate = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for same from and to date")
        void testSameFromAndToDate() {
            Long date = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, date.toString());
            context.put(Validator.ValidationResource.TO_DATE, date.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for one day range")
        void testOneDayRange() {
            Long fromDate = Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for one month range")
        void testOneMonthRange() {
            Long fromDate = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Invalid Date Range Tests")
    class InvalidDateRangeTests {

        @Test
        @DisplayName("Should fail validation when from date is after to date")
        void testFromDateAfterToDate() {
            Long fromDate = Instant.now().toEpochMilli();
            Long toDate = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertFalse(result);
        }

        @Test
        @DisplayName("Should pass validation for negative from date (old dates are valid)")
        void testNegativeFromDate() {
            Long fromDate = -1000L;
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            // Negative dates (before epoch) are valid as long as they're before toDate
            assertTrue(result);
        }

        @Test
        @DisplayName("Should pass validation for zero from date (epoch time is valid)")
        void testZeroFromDate() {
            Long fromDate = 0L;
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            // Zero (epoch time) is valid as long as it's before toDate
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very old from date")
        void testVeryOldFromDate() {
            Long fromDate = Instant.parse("2000-01-01T00:00:00Z").toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should fail validation for future from date")
        void testFutureFromDate() {
            Long fromDate = Instant.now().plus(7, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().plus(14, ChronoUnit.DAYS).toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            // Future dates are not allowed
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle very large date range")
        void testVeryLargeDateRange() {
            Long fromDate = Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle millisecond precision")
        void testMillisecondPrecision() {
            Long fromDate = 1704067200000L; // 2024-01-01 00:00:00.000
            Long toDate = 1704067200001L;   // 2024-01-01 00:00:00.001

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Context Validation Tests")
    class ContextValidationTests {

        @Test
        @DisplayName("Should handle null context")
        void testNullContext() {
            Boolean result = validator.validate(null, null);

            // Null context should fail due to NullPointerException
            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle empty context")
        void testEmptyContext() {
            Map<Validator.ValidationResource, String> context = new HashMap<>();

            Boolean result = validator.validate(null, context);

            // Empty context (no dates) should pass
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle invalid to date in context")
        void testInvalidToDateInContext() {
            Long fromDate = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, "invalid");

            Boolean result = validator.validate(null, context);

            // Invalid date format should fail
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Specific Date Tests")
    class SpecificDateTests {

        @Test
        @DisplayName("Should validate specific date range - January 2024")
        void testJanuary2024Range() {
            Long fromDate = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
            Long toDate = Instant.parse("2024-01-31T23:59:59Z").toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should validate specific date range - Last 7 days")
        void testLast7Days() {
            Long fromDate = Instant.now().minus(7, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }

        @Test
        @DisplayName("Should validate specific date range - Last 30 days")
        void testLast30Days() {
            Long fromDate = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();
            Long toDate = Instant.now().toEpochMilli();

            Map<Validator.ValidationResource, String> context = new HashMap<>();
            context.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
            context.put(Validator.ValidationResource.TO_DATE, toDate.toString());

            Boolean result = validator.validate(null, context);

            assertTrue(result);
        }
    }
}

