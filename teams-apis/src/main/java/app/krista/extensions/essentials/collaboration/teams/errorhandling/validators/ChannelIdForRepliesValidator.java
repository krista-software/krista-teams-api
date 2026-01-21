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
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.ValidationUtils;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Validator for Channel ID validation in the context of Get Channel Message Replies.
 * Validates that the provided channel ID exists and is accessible using API calls.
 */
public class ChannelIdForRepliesValidator extends BaseValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelIdForRepliesValidator.class);

    private final ChannelService channelService;

    @Inject
    public ChannelIdForRepliesValidator(ChannelService channelService) {
        super("Channel Id", FieldTypes.TEXT_FIELD);
        this.channelService = channelService;
        LOGGER.info("ChannelIdForRepliesValidator constructor called");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("ChannelIdForRepliesValidator.validate() called with channelId: {}", resourceId);

        try {
            // Check if Channel ID is not empty
            if (ValidationUtils.isStringNullOrBlank(resourceId)) {
                LOGGER.warn("Channel ID validation failed: channel ID is null or blank");
                return false;
            }

            // Get Team ID from context for API validation
            String teamId = context.get(ValidationResource.TEAM_ID_FOR_REPLIES);

            // Validate using API call to check if channel exists and is accessible
            boolean channelExists = channelService.validateChannelExists(resourceId.trim(),
                    teamId != null ? teamId.trim() : null);

            if (!channelExists) {
                LOGGER.warn("Channel ID validation failed: channel {} not found or not accessible", resourceId);
                return false;
            }

            LOGGER.info("Channel ID validation passed for: {}", resourceId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Exception during channel ID validation for '{}': {}", resourceId, e.getMessage(), e);
            return logExceptionAndReturnFalse(e);
        }
    }

    @Override
    public String getFetchFieldName() {
        return "Channel ID";
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Channel ID that you have access to.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided Channel ID '%s' is invalid or not accessible. Please enter a valid channel ID that exists and you have permission to access.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid Channel ID: %s. Please provide a valid channel ID that exists and you have access to.", resourceId);
    }
}
