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
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.stores.Replies;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.TeamIdForRepliesValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
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
 * Helper class for Get Channel Message Replies with Team ID validation.
 */
@Singleton
@Service
public class GetChannelMessageRepliesTeamIdHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetChannelMessageRepliesTeamIdHelper.class);

    private final ChatService chatService;
    private final TeamService teamService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;

    @Inject
    public GetChannelMessageRepliesTeamIdHelper(ChatService chatService,
                                                TeamService teamService,
                                                ValidationOrchestrator validationOrchestrator,
                                                ExtensionResponseGenerator responseGenerator,
                                                ErrorHandlingStateManager internalStateManager) {
        this.chatService = chatService;
        this.teamService = teamService;
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;

        LOGGER.info("GetChannelMessageRepliesTeamIdHelper constructor called");
        registerValidators();
        LOGGER.info("GetChannelMessageRepliesTeamIdHelper constructor completed");
    }

    private void registerValidators() {
        try {
            // Register Team ID validator for replies
            TeamIdForRepliesValidator teamIdForRepliesValidator = new TeamIdForRepliesValidator(teamService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.TEAM_ID_FOR_REPLIES, teamIdForRepliesValidator);
            LOGGER.info("Registered TeamIdForRepliesValidator for TEAM_ID_FOR_REPLIES");

            LOGGER.info("GetChannelMessageRepliesTeamIdHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getChannelMessageRepliesWithTeamIdValidation(String channelMessageId, String channelId, String teamId) {
        try {
            LOGGER.info("GetChannelMessageRepliesTeamIdHelper.getChannelMessageRepliesWithTeamIdValidation() called with channelMessageId: {}, channelId: {}, teamId: {}",
                    channelMessageId, channelId, teamId);

            // Validate input parameters - validate Team Id
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.TEAM_ID_FOR_REPLIES, teamId != null ? teamId : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Team ID validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, channelMessageId, channelId, teamId);
            }

            // If validation passes, proceed with retrieving replies
            LOGGER.debug("Validation passed, retrieving channel message replies");
            List<Replies> result = chatService.getChannelMessageReplies(channelMessageId, channelId, teamId);

            if (result == null || result.isEmpty()) {
                LOGGER.warn("No replies found for channel message ID: {}", channelMessageId);

                // Return empty list instead of string message to maintain type consistency
                return new ExtensionResponseBuilder()
                        .success(Map.of("Replies List", Collections.emptyList()))
                        .build();
            }

            LOGGER.info("Successfully retrieved {} replies for channel message: {}", result.size(), channelMessageId);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Replies List", result))
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error in getChannelMessageRepliesWithTeamIdValidation for teamId: {}", teamId, cause);

            // Return empty list instead of string message to maintain type consistency
            return new ExtensionResponseBuilder()
                    .success(Map.of("Replies List", Collections.emptyList()))
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String channelMessageId, String channelId, String teamId) {
        // Generate a unique state ID to track this validation session
        String stateId = UUID.randomUUID().toString();
        LOGGER.debug("Team ID validation failed, storing stateId: {}", stateId);

        // Store validation results for later retrieval
        internalStateManager.put(stateId, app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
        LOGGER.debug("Team ID validation failed, generating confirmation response");

        // Create a map with the necessary data for the confirmation step
        Map<String, Object> map = Map.of(
                TeamsResources.STATE_ID, stateId,
                TeamsResources.CHANNEL_MESSAGE_ID, channelMessageId != null ? channelMessageId : "",
                TeamsResources.CHANNEL_ID, channelId != null ? channelId : "",
                TeamsResources.TEAM_ID, teamId != null ? teamId : ""
        );

        // Generate a response that will trigger the confirmation subcatalog request
        return responseGenerator.generateConfirmationResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.CONFIRM_TEAM_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES,
                map);
    }
}
