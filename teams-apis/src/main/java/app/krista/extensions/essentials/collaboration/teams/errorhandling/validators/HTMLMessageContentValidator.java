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

import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.FieldTypes;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for HTML Message Content validation for Send HTML Message operations.
 * Validates HTML message content requirements for Teams messaging including length,
 * HTML structure, and format restrictions.
 */
@Singleton
@Service
public class HTMLMessageContentValidator extends BaseTeamsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTMLMessageContentValidator.class);

    // Message length constraints
    private static final int MIN_MESSAGE_LENGTH = 1;        // Minimum meaningful message
    
    // Patterns for HTML content validation
    private static final Pattern ONLY_WHITESPACE_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_TAG_PATTERN = Pattern.compile("<iframe[^>]*>.*?</iframe>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    
    // Content quality thresholds
    private static final int MIN_CONTENT_LENGTH_AFTER_STRIP = 1;

    @Inject
    public HTMLMessageContentValidator(ChatService chatService) {
        super(TeamsResources.HTML_MESSAGE_CONTENT, FieldTypes.TEXT_FIELD, chatService);
        LOGGER.info("HTMLMessageContentValidator constructor called");
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // HTML message content format validation
        return isValidHTMLMessageContent(trimmedValue);
    }

    @Override
    protected ValidationResult performBusinessValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // For HTML message content, business validation is the same as format validation
        // No additional Teams-specific business logic needed for HTML message content
        return success();
    }

    /**
     * Validates HTML message content.
     * 
     * @param message The HTML message content to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidHTMLMessageContent(String message) {
        if (message == null) {
            LOGGER.debug("HTML message content validation failed: null");
            return false;
        }

        String trimmed = message.trim();

        // Check minimum length
        if (trimmed.length() < MIN_MESSAGE_LENGTH) {
            LOGGER.debug("HTML message content validation failed: too short - length: {}", trimmed.length());
            return false;
        }

        // Check if only whitespace
        if (ONLY_WHITESPACE_PATTERN.matcher(trimmed).matches()) {
            LOGGER.debug("HTML message content validation failed: only whitespace");
            return false;
        }

        // Check for dangerous HTML tags (script, iframe)
        if (SCRIPT_TAG_PATTERN.matcher(trimmed).find()) {
            LOGGER.warn("HTML message content validation failed: contains script tags");
            return false;
        }

        if (IFRAME_TAG_PATTERN.matcher(trimmed).find()) {
            LOGGER.warn("HTML message content validation failed: contains iframe tags");
            return false;
        }

        // Strip HTML tags and check if there's meaningful content
        String contentWithoutTags = trimmed.replaceAll("<[^>]+>", "").trim();
        if (contentWithoutTags.length() < MIN_CONTENT_LENGTH_AFTER_STRIP) {
            LOGGER.debug("HTML message content validation failed: no meaningful content after stripping HTML tags");
            return false;
        }

        LOGGER.debug("HTML message content validation passed");
        return true;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter valid HTML message content for Teams messaging. " +
                "Avoid script tags, iframes, or empty content.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "Please enter valid HTML message content for Teams messaging.";
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        if (value.length() < MIN_MESSAGE_LENGTH) {
            return "HTML message content is too short. Please provide meaningful HTML content.";
        }

        if (ONLY_WHITESPACE_PATTERN.matcher(value).matches()) {
            return "HTML message content cannot contain only whitespace. Please provide meaningful HTML content.";
        }

        if (SCRIPT_TAG_PATTERN.matcher(value).find()) {
            return "HTML message content cannot contain script tags for security reasons. Please remove script tags.";
        }

        if (IFRAME_TAG_PATTERN.matcher(value).find()) {
            return "HTML message content cannot contain iframe tags for security reasons. Please remove iframe tags.";
        }

        String contentWithoutTags = value.replaceAll("<[^>]+>", "").trim();
        if (contentWithoutTags.length() < MIN_CONTENT_LENGTH_AFTER_STRIP) {
            return "HTML message content must contain meaningful text content, not just HTML tags.";
        }

        return "Invalid HTML message content. Please provide valid HTML for Teams messaging.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        return "HTML message content validation failed. Please provide valid HTML content for Teams messaging.";
    }
}
