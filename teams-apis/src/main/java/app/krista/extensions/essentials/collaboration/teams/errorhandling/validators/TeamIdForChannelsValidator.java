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
import app.krista.extensions.essentials.collaboration.teams.service.TeamService;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;


/**
 * Validator for Team ID validation in the context of Get Channels of Team.
 * Validates that the provided team ID exists and is accessible using API calls.
 */
public class TeamIdForChannelsValidator extends BaseValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamIdForChannelsValidator.class);

    private final TeamService teamService;

    @Inject
    public TeamIdForChannelsValidator(TeamService teamService) {
        super("Team ID", FieldTypes.TEXT_FIELD);
        this.teamService = teamService;
        LOGGER.info("TeamIdForChannelsValidator constructor called");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("TeamIdForChannelsValidator.validate() called with teamId: {}", resourceId);

        try {
            // Check if Team ID is not empty
            if (ValidationUtils.isStringNullOrBlank(resourceId)) {
                LOGGER.warn("Team ID validation failed: team ID is null or blank");
                return false;
            }

            // Validate using API call to check if the team exists and user has access
            try {
                List<Team> joinedTeams = teamService.getJoinedTeams(null);
                if (joinedTeams != null) {
                    boolean teamExists = joinedTeams.stream()
                            .anyMatch(team -> resourceId.trim().equals(team.iD));

                    if (!teamExists) {
                        LOGGER.warn("Team ID validation failed: team not found or not accessible - {}", resourceId);
                        return false;
                    }
                } else {
                    LOGGER.warn("Could not retrieve joined teams list for validation");
                    return false;
                }
            } catch (Exception e) {
                LOGGER.error("Error during team existence validation for '{}': {}", resourceId, e.getMessage(), e);
                return false;
            }

            LOGGER.info("Team ID validation passed for: {}", resourceId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Exception during team ID validation for '{}': {}", resourceId, e.getMessage(), e);
            return logExceptionAndReturnFalse(e);
        }
    }

    @Override
    public String getFetchFieldName() {
        return "Team ID";
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Team ID that you have access to.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided Team ID '%s' is invalid or not accessible. Please enter a valid team ID that exists and you have permission to access.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid Team ID: %s. Please provide a valid team ID that exists and you have access to.", resourceId);
    }
}
