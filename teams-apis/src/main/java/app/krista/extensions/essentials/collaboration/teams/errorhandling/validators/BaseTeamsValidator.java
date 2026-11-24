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

import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.ValidationUtils;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Base validator for Teams-specific validations.
 * Provides common functionality for Teams validators including email validation and user existence checks.
 */
public abstract class BaseTeamsValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseTeamsValidator.class);

    // Email validation pattern - comprehensive regex for email validation
    protected static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    // Email length constraints
    protected static final int MIN_EMAIL_LENGTH = 5;  // Minimum reasonable email length (a@b.c)
    protected static final int MAX_EMAIL_LENGTH = 254; // RFC 5321 limit

    protected final String fieldName;
    protected final String fieldType;
    protected final ChatService chatService;

    /**
     * Creates a new BaseTeamsValidator.
     *
     * @param fieldName   The field name for this validator
     * @param fieldType   The field type for this validator
     * @param chatService The chat service for Teams operations
     */
    protected BaseTeamsValidator(String fieldName, String fieldType, ChatService chatService) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.chatService = chatService;
    }

    @Override
    public String getFetchFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldType() {
        return fieldType;
    }

    /**
     * Validates the resource ID with format and business logic validation.
     */
    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("{}.validate() called with value: {}", this.getClass().getSimpleName(), resourceId);

//        try {
        // Step 1: Basic null/blank validation
        if (ValidationUtils.isStringNullOrBlank(resourceId)) {
            LOGGER.warn("{} validation failed: value is null or blank", fieldName);
            return false;
        }

        String trimmedValue = resourceId.trim();

        // Step 2: Format validation
        if (!isValidFormat(trimmedValue)) {
            LOGGER.warn("{} validation failed: invalid format - {}", fieldName, resourceId);
            return false;
        }

        // Step 3: Business logic validation
        ValidationResult result = performBusinessValidation(trimmedValue, context);
        if (!result.isValid()) {
            LOGGER.warn("{} validation failed: {} - {}", fieldName, result.getErrorMessage(), resourceId);
            return false;
        }

        LOGGER.debug("{} validation passed for: {}", fieldName, resourceId);
        return true;
    }

    /**
     * Validates the format of the input value.
     * Subclasses should override this method to provide specific format validation.
     *
     * @param trimmedValue The trimmed input value
     * @return true if format is valid, false otherwise
     */
    protected abstract boolean isValidFormat(String trimmedValue);

    /**
     * Performs business logic validation (e.g., checking if user exists in Teams).
     * Subclasses should override this method to provide specific business validation.
     *
     * @param trimmedValue The trimmed input value
     * @param context      The validation context
     * @return ValidationResult indicating success or failure with error message
     */
    protected abstract ValidationResult performBusinessValidation(String trimmedValue, Map<ValidationResource, String> context);

    /**
     * Gets the error message for format validation failures.
     * Subclasses should override this method to provide specific format error messages.
     *
     * @param value The invalid value
     * @return The format error message
     */
    protected abstract String getFormatErrorMessage(String value);

    /**
     * Gets the error message for business logic validation failures.
     * Subclasses should override this method to provide specific business logic error messages.
     *
     * @param value The invalid value
     * @return The business logic error message
     */
    protected abstract String getBusinessLogicErrorMessage(String value);

    @Override
    public String getErrMessage(String resourceId) {
        if (ValidationUtils.isStringNullOrBlank(resourceId)) {
            return fieldName + " is required and cannot be blank.";
        }

        String trimmedValue = resourceId.trim();

        // Check format first
        if (!isValidFormat(trimmedValue)) {
            return getFormatErrorMessage(trimmedValue);
        }

        // If format is valid but business validation failed, return business logic error
        return getBusinessLogicErrorMessage(trimmedValue);
    }

    /**
     * Checks if the given string is a valid email format.
     *
     * @param email The email string to validate
     * @return true if valid email format, false otherwise
     */
    protected boolean isEmail(String email) {
        if (ValidationUtils.isStringNullOrBlank(email)) {
            return false;
        }

        String trimmedEmail = email.trim();

        // Check length constraints
        if (trimmedEmail.length() < MIN_EMAIL_LENGTH || trimmedEmail.length() > MAX_EMAIL_LENGTH) {
            return false;
        }

        // Check email pattern
        if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
            return false;
        }

        // Additional structural validation
        String[] parts = trimmedEmail.split("@");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return false;
        }

        // Check domain part has at least one dot
        if (!parts[1].contains(".")) {
            return false;
        }

        // Check for consecutive dots or other invalid patterns
        if (trimmedEmail.contains("..") || trimmedEmail.startsWith(".") || trimmedEmail.endsWith(".")) {
            return false;
        }

        return true;
    }

    /**
     * Checks if a user exists in Teams by attempting to resolve the user via Microsoft Graph API.
     * This validates if an email corresponds to an accessible Teams user without creating a chat.
     *
     * @param email The email address to check
     * @return true if user exists and is accessible, false otherwise
     */
    protected boolean userExistsInTeams(String email) {
        if (chatService == null) {
            LOGGER.warn("ChatService not available, skipping user existence validation for: {}", email);
            return true; // If service is not available, assume valid (format validation already passed)
        }

        LOGGER.debug("Checking if user exists in Teams: {}", email);
        boolean userExists = chatService.validateUserExists(email);

        if (userExists) {
            LOGGER.debug("User validation successful for: {}", email);
            return true;
        } else {
            LOGGER.warn("User not found or not accessible in Teams: {}", email);
            return false;
        }
    }

    /**
     * Creates a successful validation result.
     *
     * @return ValidationResult indicating success
     */
    protected ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * Creates a failed validation result with error message.
     *
     * @param errorMessage The error message
     * @return ValidationResult indicating failure
     */
    protected ValidationResult failure(String errorMessage) {
        return new ValidationResult(false, errorMessage);
    }

    /**
     * Result of a validation operation.
     */
    protected static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
