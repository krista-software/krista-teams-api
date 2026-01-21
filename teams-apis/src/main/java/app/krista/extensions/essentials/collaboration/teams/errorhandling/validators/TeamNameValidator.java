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
 * Validator for Team Name validation for Send HTML Message to Teams Channel operations.
 * Validates that the provided team name exists in Microsoft Teams and is accessible.
 */
@Singleton
@Service
public class TeamNameValidator extends BaseTeamsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamNameValidator.class);

    @Inject
    public TeamNameValidator(ChatService chatService) {
        super(TeamsResources.TEAM_NAME, FieldTypes.TEXT_FIELD, chatService);
        LOGGER.info("TeamNameValidator constructor called");
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Team name format validation - basic checks
        if (trimmedValue == null || trimmedValue.length() < 2) {
            LOGGER.debug("Team name validation failed: null or too short");
            return false;
        }
        return true;
    }

    @Override
    protected ValidationResult performBusinessValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // For team name, we skip business validation here
        // The actual team existence check will be done in the service layer when sending the message
        // This is because checking team existence requires additional API calls
        return success();
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid team name that exists in Microsoft Teams.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "Please enter a valid team name that exists in Microsoft Teams.";
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid team name format. Team name must be at least 2 characters long.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        return String.format("Team not found with name: %s. Please verify the team name and try again.", value);
    }
}

