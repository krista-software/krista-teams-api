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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility methods for validation.
 */
public class ValidationUtils {
    private ValidationUtils() {
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Checks if a string is null or blank.
     *
     * @param input The string to check
     * @return true if the string is null or blank, false otherwise
     */
    public static boolean isStringNullOrBlank(String input) {
        return input == null || input.trim().isEmpty();
    }

    /**
     * Checks if a list is null or empty.
     *
     * @param input The list to check
     * @param <T>   The type of the list elements
     * @return true if the list is null or empty, false otherwise
     */
    public static <T> boolean isListNullOrEmpty(List<T> input) {
        return input == null || input.isEmpty();
    }

    /**
     * Checks if a map is null or empty.
     *
     * @param input The map to check
     * @param <K>   The type of the map keys
     * @param <V>   The type of the map values
     * @return true if the map is null or empty, false otherwise
     */
    public static <K, V> boolean isMapNullOrEmpty(Map<K, V> input) {
        return input == null || input.isEmpty();
    }

    /**
     * Checks if a string is a valid email address.
     *
     * @param email The string to check
     * @return true if the string is a valid email address, false otherwise
     */
    public static boolean isEmailValid(String email) {
        if (isStringNullOrBlank(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if a string is a valid number.
     *
     * @param number The string to check
     * @return true if the string is a valid number, false otherwise
     */
    public static boolean isNumberValid(String number) {
        if (isStringNullOrBlank(number)) {
            return false;
        }
        try {
            double value = Double.parseDouble(number);
            // Reject NaN, Infinity, and -Infinity
            return !Double.isNaN(value) && !Double.isInfinite(value);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if a number is within a range.
     *
     * @param number The number to check
     * @param min    The minimum value (inclusive)
     * @param max    The maximum value (inclusive)
     * @return true if the number is within the range, false otherwise
     */
    public static boolean isNumberInRange(double number, double min, double max) {
        return number >= min && number <= max;
    }

    /**
     * Checks if a string has a minimum length.
     *
     * @param input     The string to check
     * @param minLength The minimum length
     * @return true if the string has at least the minimum length, false otherwise
     */
    public static boolean hasMinLength(String input, int minLength) {
        if (input == null) {
            return false;
        }
        // For minLength of 0, even empty string is valid
        if (minLength == 0) {
            return true;
        }
        // For other cases, check if trimmed input meets minimum length
        if (input.trim().isEmpty()) {
            return false;
        }
        return input.length() >= minLength;
    }

    /**
     * Checks if a string has a maximum length.
     *
     * @param input     The string to check
     * @param maxLength The maximum length
     * @return true if the string has at most the maximum length, false otherwise
     */
    public static boolean hasMaxLength(String input, int maxLength) {
        if (isStringNullOrBlank(input)) {
            return true;
        }
        return input.length() <= maxLength;
    }
}
