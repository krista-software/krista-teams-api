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
 * Validator for Message Content validation for Send Message to User operations.
 * Validates message content requirements for Teams messaging including length,
 * content quality, and format restrictions.
 */
@Singleton
@Service
public class MessageContentValidator extends BaseTeamsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageContentValidator.class);

    // Message length constraints
    private static final int MIN_MESSAGE_LENGTH = 1;        // Minimum meaningful message
    
    // Patterns for content validation
    private static final Pattern ONLY_WHITESPACE_PATTERN = Pattern.compile("^\\s*$");
    private static final Pattern ONLY_SPECIAL_CHARS_PATTERN = Pattern.compile("^[^a-zA-Z0-9\\s]*$");
    
    // Content quality thresholds
    private static final int MIN_WORD_COUNT = 1;
    private static final int MAX_CONSECUTIVE_NEWLINES = 5;

    @Inject
    public MessageContentValidator(ChatService chatService) {
        super(TeamsResources.MESSAGE_CONTENT, FieldTypes.TEXT_FIELD, chatService);
        LOGGER.info("MessageContentValidator constructor called");
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Message content format validation
        return isValidMessageContent(trimmedValue);
    }

    @Override
    protected ValidationResult performBusinessValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // For message content, business validation is the same as format validation
        // No additional Teams-specific business logic needed for message content
        return success();
    }

    /**
     * Validates message content format and quality.
     */
    private boolean isValidMessageContent(String message) {
        // Step 1: Check minimum length
        if (message.length() < MIN_MESSAGE_LENGTH) {
            LOGGER.warn("Message content validation failed: message too short - length: {}", message.length());
            return false;
        }

        // Step 2: Check for only whitespace content
        if (ONLY_WHITESPACE_PATTERN.matcher(message).matches()) {
            LOGGER.warn("Message content validation failed: message contains only whitespace");
            return false;
        }

        // Step 3: Check for only special characters (no meaningful content)
        if (ONLY_SPECIAL_CHARS_PATTERN.matcher(message).matches()) {
            LOGGER.warn("Message content validation failed: message contains only special characters");
            return false;
        }

        // Step 6: Check word count (ensure meaningful content)
        String[] words = message.split("\\s+");
        if (words.length < MIN_WORD_COUNT) {
            LOGGER.warn("Message content validation failed: insufficient word count - words: {}", words.length);
            return false;
        }

        // Step 7: Check for excessive newlines (formatting abuse)
        if (hasExcessiveNewlines(message)) {
            LOGGER.warn("Message content validation failed: excessive newlines detected");
            return false;
        }

        // Step 8: Check for potentially harmful content patterns
        if (containsSuspiciousPatterns(message)) {
            LOGGER.warn("Message content validation failed: suspicious content patterns detected");
            return false;
        }

        LOGGER.debug("Message content validation passed for message with length: {}", message.length());
        return true;
    }

    /**
     * Checks if the message has excessive consecutive newlines.
     */
    private boolean hasExcessiveNewlines(String message) {
        String[] lines = message.split("\n", -1);
        int consecutiveEmptyLines = 0;
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                consecutiveEmptyLines++;
                if (consecutiveEmptyLines > MAX_CONSECUTIVE_NEWLINES) {
                    return true;
                }
            } else {
                consecutiveEmptyLines = 0;
            }
        }
        return false;
    }

    /**
     * Checks for suspicious content patterns that might indicate spam or abuse.
     */
    private boolean containsSuspiciousPatterns(String message) {
        // Check for excessive use of the same word
        String[] words = message.toLowerCase().split("\\s+");
        if (words.length > 10) {
            Map<String, Integer> wordCount = new java.util.HashMap<>();
            for (String word : words) {
                if (word.length() > 2) { // Only count meaningful words
                    wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
                    if (wordCount.get(word) > words.length / 3) { // Same word appears in >33% of message
                        return true;
                    }
                }
            }
        }

        // Check for excessive capitalization (shouting)
        long upperCaseCount = message.chars().filter(Character::isUpperCase).count();
        long letterCount = message.chars().filter(Character::isLetter).count();
        if (letterCount > 20 && upperCaseCount > letterCount * 0.7) { // >70% uppercase
            return true;
        }

        return false;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a meaningful message content for Teams messaging. " +
                "Avoid excessive repetition or special characters only.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "Please enter a meaningful message content for Teams messaging.";
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        if (value.length() < MIN_MESSAGE_LENGTH) {
            return "Message content is too short. Please provide a meaningful message.";
        }

        if (ONLY_WHITESPACE_PATTERN.matcher(value).matches()) {
            return "Message content cannot contain only whitespace. Please provide a meaningful message.";
        }

        if (ONLY_SPECIAL_CHARS_PATTERN.matcher(value).matches()) {
            return "Message content cannot contain only special characters. Please provide a meaningful message with text.";
        }

        String[] words = value.split("\\s+");
        if (words.length < MIN_WORD_COUNT) {
            return "Message content must contain at least one meaningful word.";
        }

        if (hasExcessiveNewlines(value)) {
            return String.format("Message content contains too many consecutive empty lines (max %d allowed). " +
                    "Please format your message more concisely.", MAX_CONSECUTIVE_NEWLINES);
        }

        if (containsSuspiciousPatterns(value)) {
            return "Message content appears to contain spam-like patterns or excessive repetition. " +
                    "Please provide a more natural, varied message.";
        }

        return "Invalid message content. Please provide a meaningful message for Teams messaging.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        return "Message content validation failed. Please provide a valid message for Teams messaging.";
    }
}
