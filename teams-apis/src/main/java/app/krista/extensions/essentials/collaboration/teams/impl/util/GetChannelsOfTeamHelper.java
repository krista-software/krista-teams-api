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

package app.krista.extensions.essentials.collaboration.teams.impl.util;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Channel;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.TeamIdValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import app.krista.extensions.essentials.collaboration.teams.service.TeamService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Get Channels of Team by Team ID with Team ID validation.
 */
@Singleton
@Service
public class GetChannelsOfTeamHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetChannelsOfTeamHelper.class);

    private final ChannelService channelService;
    private final TeamService teamService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;

    @Inject
    public GetChannelsOfTeamHelper(ChannelService channelService,
                                   TeamService teamService,
                                   ValidationOrchestrator validationOrchestrator,
                                   ExtensionResponseGenerator responseGenerator,
                                   ErrorHandlingStateManager internalStateManager) {
        this.channelService = channelService;
        this.teamService = teamService;
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;

        LOGGER.info("GetChannelsOfTeamHelper constructor called");
        registerValidators();
        LOGGER.info("GetChannelsOfTeamHelper constructor completed");
    }

    private void registerValidators() {
        try {
            // Register Team ID validator for channels
            TeamIdValidator teamIdValidator = new TeamIdValidator(this.teamService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.TEAM_ID, teamIdValidator);
            LOGGER.info("Registered TeamIdForChannelsValidator for TEAM_ID_FOR_CHANNELS");

            LOGGER.info("GetChannelsOfTeamHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getChannelsOfTeamWithValidation(String teamId, Boolean allowRetry) {
            LOGGER.info("GetChannelsOfTeamHelper.getChannelsOfTeamWithValidation() called with teamId: {}, allowRetry: {}", teamId, allowRetry);

            // Validate input parameters - validate Team Id
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.TEAM_ID, teamId != null ? teamId : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Team ID validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, teamId, allowRetry);
            }

            // If validation passes, proceed with retrieving channels
            LOGGER.debug("Validation passed, retrieving channels of team");
            List<Channel> result = channelService.getChannelsOfTeam(teamId, null);

            if (result == null || result.isEmpty()) {
                LOGGER.warn("No channels found for team ID: {}", teamId);

                String responseMessage = "No channels found for team ID: " + teamId;
                return new ExtensionResponseBuilder()
                        .success(Map.of(
                                "Channels", Collections.emptyList(),
                                "Response Message", responseMessage))
                        .build();
            }

            LOGGER.info("Successfully retrieved {} channels for team: {}", result.size(), teamId);

            String responseMessage = "Successfully retrieved " + result.size() + " channel(s) for team ID: " + teamId;
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Channels", result,
                            "Response Message", responseMessage))
                    .build();

    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String teamId, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Get Channels of Team");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("Team ID validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("Team ID validation failed, generating confirmation response");

            // Create a map with the necessary data for the confirmation step
            Map<String, Object> map = Map.of(
                    TeamsResources.STATE_ID, stateId,
                    TeamsResources.TEAM_ID, teamId != null ? teamId : ""
            );

            // Generate a response that will trigger the confirmation subcatalog request
            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_TEAM_ID_VALIDATION_GET_CHANNELS_OF_TEAM,
                    map);
        }

        LOGGER.info("Allow Retry = false, returning error directly for Get Channels of Team");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }
}
