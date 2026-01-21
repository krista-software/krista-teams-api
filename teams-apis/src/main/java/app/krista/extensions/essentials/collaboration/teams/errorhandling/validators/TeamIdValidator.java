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
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Teams Team ID validation.
 * Validates that the Team ID exists and is accessible using API calls.
 * This validator performs both format validation and API-based existence validation.
 */
@Service
public class TeamIdValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeamIdValidator.class);

    // UUID format pattern for Team IDs
    private static final Pattern TEAM_ID_PATTERN = Pattern.compile(
            "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"
    );

    private final TeamService teamService;

    /**
     * Creates a new TeamIdValidator with API-based validation.
     */
    @Inject
    public TeamIdValidator(TeamService teamService) {
        super("Team ID", FieldTypes.TEXT_FIELD);
        this.teamService = teamService;
        LOGGER.info("TeamIdValidator constructor called with API validation enabled");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("TeamIdValidator.validate() called with teamId: {}", resourceId);

        try {
            // Step 1: Basic format validation
            if (ValidationUtils.isStringNullOrBlank(resourceId)) {
                LOGGER.warn("Team ID validation failed: team ID is null or blank");
                return false;
            }

            String trimmedTeamId = resourceId.trim();
            if (!TEAM_ID_PATTERN.matcher(trimmedTeamId).matches()) {
                LOGGER.warn("Team ID validation failed: invalid UUID format - {}", resourceId);
                return false;
            }

            // Step 2: API-based validation - check if team exists and user has access
            if (teamService != null) {
                try {
                    List<Team> joinedTeams = teamService.getJoinedTeams(null);
                    if (joinedTeams != null) {
                        boolean teamExists = joinedTeams.stream()
                                .anyMatch(team -> trimmedTeamId.equals(team.iD));

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
            } else {
                LOGGER.warn("TeamService not available, performing format validation only for: {}", resourceId);
                // If TeamService is not available, we've already done format validation above
            }

            LOGGER.info("Team ID validation passed for: {}", resourceId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Exception during team ID validation for '{}': {}", resourceId, e.getMessage(), e);
            return logExceptionAndReturnFalse(e);
        }
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Team ID (UUID format) that you have access to.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided Team ID '%s' is invalid, not in the expected UUID format, or you don't have access to this team. Please enter a valid Team ID from your joined teams.", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid Team ID: %s. Please provide a valid Team ID that exists and you have access to.", resourceId);
    }
}
