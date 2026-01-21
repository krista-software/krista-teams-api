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
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Validator for Teams Channel ID format.
 * Validates that the Channel ID exists and is accessible using API calls.
 */
@Service
public class ChannelIdValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelIdValidator.class);

    private final ChannelService channelService;

    /**
     * Creates a new ChannelIdValidator.
     */
    public ChannelIdValidator() {
        super("Channel ID");
        this.channelService = null; // For backward compatibility when used without injection
    }

    /**
     * Creates a new ChannelIdValidator with ChannelService dependency.
     */
    @Inject
    public ChannelIdValidator(ChannelService channelService) {
        super("Channel ID");
        this.channelService = channelService;
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("ChannelIdValidator.validate() called with channelID: {}", resourceId);

        try {
            // Check if Channel ID is not empty
            if (ValidationUtils.isStringNullOrBlank(resourceId)) {
                LOGGER.warn("Channel ID validation failed: channel ID is null or blank");
                return false;
            }

            // If ChannelService is available, use API validation
            if (channelService != null) {
                // Get Team ID from context for better validation
                String teamId = context.get(ValidationResource.TEAM_ID);

                // Validate using API call to check if channel exists and is accessible
                boolean channelExists = channelService.validateChannelExists(resourceId.trim(),
                        teamId != null ? teamId.trim() : null);

                if (!channelExists) {
                    LOGGER.warn("Channel ID validation failed: channel {} not found or not accessible", resourceId);
                    return false;
                }

                LOGGER.info("Channel ID validation passed for: {}", resourceId);
                return true;
            } else {
                // Fallback: basic null/blank check only (for backward compatibility)
                LOGGER.warn("ChannelService not available, performing basic validation only for: {}", resourceId);
                return true;
            }

        } catch (Exception e) {
            LOGGER.error("Exception during channel ID validation for '{}': {}", resourceId, e.getMessage(), e);
            return logExceptionAndReturnFalse(e);
        }
    }

    @Override
    public String getFetchFieldName() {
        return TeamsResources.CHANNEL_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        LOGGER.debug("ChannelIdValidator.getFetchStepMessage() - Returning fetch step message");
        return "Please enter a valid Channel ID that you have access to.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("ChannelIdValidator.getConfirmationStepMessage() - Creating message for channelID: {}", resourceId);
        return String.format("The provided Channel ID '%s' is invalid or not accessible. Please enter a valid channel ID that exists and you have permission to access.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        LOGGER.debug("ChannelIdValidator.getErrMessage() - Creating error message for channelID: {}", resourceId);
        return String.format("Invalid Channel ID: %s. Please provide a valid channel ID that exists and you have access to.", resourceId);
    }
}
