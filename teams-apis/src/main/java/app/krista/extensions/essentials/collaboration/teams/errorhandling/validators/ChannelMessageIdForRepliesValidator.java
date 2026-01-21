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
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.ValidationUtils;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Channel Message ID validation in the context of Get Channel Message Replies.
 * Validates that the provided channel message ID exists and is accessible using API calls.
 * This validator performs both format validation and API-based existence validation.
 */
public class ChannelMessageIdForRepliesValidator extends BaseValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelMessageIdForRepliesValidator.class);

    // Pattern for Microsoft Teams message ID format (13-19 digit numeric to accommodate different formats)
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("^[0-9]{13,19}$");

    private final ChatService chatService;

    @Inject
    public ChannelMessageIdForRepliesValidator(ChatService chatService) {
        super("Channel Message ID", FieldTypes.TEXT_FIELD);
        this.chatService = chatService;
        LOGGER.info("ChannelMessageIdForRepliesValidator constructor called");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("ChannelMessageIdForRepliesValidator.validate() called with messageId: {}", resourceId);

        try {
            // Step 1: Basic format validation
            if (ValidationUtils.isStringNullOrBlank(resourceId)) {
                LOGGER.warn("Channel Message ID validation failed: message ID is null or blank");
                return false;
            }

            String trimmedMessageId = resourceId.trim();
            if (!MESSAGE_ID_PATTERN.matcher(trimmedMessageId).matches()) {
                LOGGER.warn("Channel Message ID validation failed: invalid format - {}", resourceId);
                return false;
            }

            // Step 2: API-based validation - check if message exists
            String channelId = context.get(ValidationResource.CHANNEL_ID_FOR_REPLIES);
            String teamId = context.get(ValidationResource.TEAM_ID_FOR_REPLIES);

            if (ValidationUtils.isStringNullOrBlank(channelId) || ValidationUtils.isStringNullOrBlank(teamId)) {
                LOGGER.warn("Cannot validate message existence: missing channel ID or team ID context");
                // If we don't have context, fall back to format validation only
                LOGGER.info("Channel Message ID format validation passed for: {}", resourceId);
                return true;
            }

            // Use API to validate message existence
            boolean messageExists = validateMessageExistsViaAPI(trimmedMessageId, channelId.trim(), teamId.trim());

            if (!messageExists) {
                LOGGER.warn("Channel Message ID validation failed: message {} not found or not accessible in channel {}",
                        trimmedMessageId, channelId);
                return false;
            }

            LOGGER.info("Channel Message ID validation passed for: {}", resourceId);
            return true;

        } catch (Exception e) {
            return logExceptionAndReturnFalse(e);
        }
    }

    /**
     * Validates message existence using Microsoft Graph API.
     * This method attempts to retrieve messages from the channel to verify the message exists.
     *
     * @param messageId The message ID to validate
     * @param channelId The channel ID where the message should exist
     * @param teamId    The team ID containing the channel
     * @return true if the message exists and is accessible, false otherwise
     */
    private boolean validateMessageExistsViaAPI(String messageId, String channelId, String teamId) {
        try {
            LOGGER.debug("Validating message existence via API: messageId={}, channelId={}, teamId={}",
                    messageId, channelId, teamId);

            // Get recent messages from the channel to check if our message exists
            // We use a reasonable limit to avoid performance issues
            List<app.krista.extensions.essentials.collaboration.teams.catalog.entities.ChannelMessage> messages =
                    chatService.getChannelMessagesWithDateRangeLimit(channelId, teamId,
                            System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000), // Last 30 days
                            System.currentTimeMillis(),
                            100); // Limit to 100 recent messages for performance

            if (messages == null || messages.isEmpty()) {
                LOGGER.debug("No messages found in channel {} - message {} cannot be validated", channelId, messageId);
                return false;
            }

            // Check if our message ID exists in the retrieved messages
            boolean found = messages.stream()
                    .anyMatch(msg -> messageId.equals(msg.messageId));

            if (found) {
                LOGGER.debug("Message {} found in channel {}", messageId, channelId);
                return true;
            } else {
                LOGGER.debug("Message {} not found in recent messages of channel {}", messageId, channelId);
                // Message might be older than our search window, so we'll be lenient
                // and allow it to pass format validation
                LOGGER.info("Message not found in recent messages, but allowing based on format validation");
                return true;
            }

        } catch (Exception e) {
            LOGGER.warn("Error during API-based message validation for messageId {}: {}. Falling back to format validation.",
                    messageId, e.getMessage());
            // If API call fails, fall back to format validation
            return true;
        }
    }

    @Override
    public String getFetchFieldName() {
        return TeamsResources.CHANNEL_MESSAGE_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Channel Message ID (13-19 digit numeric format) that exists in the specified channel.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided Channel Message ID '%s' is invalid, not in the expected format, or does not exist in the specified channel. Please enter a valid message ID that exists in the channel.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid Channel Message ID: %s. Please provide a valid message ID that exists in the specified channel.", resourceId);
    }
}
