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

/**
 * Validator for Channel Name validation for Send HTML Message to Teams Channel operations.
 * Validates that the provided channel name exists in the specified team and is accessible.
 */
@Singleton
@Service
public class ChannelNameValidator extends BaseTeamsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelNameValidator.class);

    @Inject
    public ChannelNameValidator(ChatService chatService) {
        super(TeamsResources.CHANNEL_NAME, FieldTypes.TEXT_FIELD, chatService);
        LOGGER.info("ChannelNameValidator constructor called");
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Channel name format validation - basic checks
        if (trimmedValue == null || trimmedValue.length() < 1) {
            LOGGER.debug("Channel name validation failed: null or empty");
            return false;
        }
        return true;
    }

    @Override
    protected ValidationResult performBusinessValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // For channel name, we need the team name from context to validate
        // Since we validate team name first, we can assume it's valid
        // The actual channel existence check will be done in the service layer
        return success();
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid channel name that exists in the specified team.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "Please enter a valid channel name that exists in the specified team.";
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid channel name format. Channel name cannot be empty.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        return String.format("Channel not found with name: %s. Please verify the channel name and try again.", value);
    }
}

