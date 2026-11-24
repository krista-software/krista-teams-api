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

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.stores.Replies;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ChannelIdValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ChannelMessageIdForRepliesValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
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
 * Helper class for Get Channel Message Replies with validation.
 */
@Singleton
@Service
public class GetChannelMessageRepliesHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetChannelMessageRepliesHelper.class);

    private final ChatService chatService;
    private final ChannelService channelService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;

    @Inject
    public GetChannelMessageRepliesHelper(ChatService chatService,
                                          ChannelService channelService,
                                          ValidationOrchestrator validationOrchestrator,
                                          ExtensionResponseGenerator responseGenerator,
                                          ErrorHandlingStateManager internalStateManager) {
        this.chatService = chatService;
        this.channelService = channelService;
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;

        LOGGER.info("GetChannelMessageRepliesHelper constructor called");
        registerValidators();
        LOGGER.info("GetChannelMessageRepliesHelper constructor completed");
    }

    private void registerValidators() {
        try {
            // Register Channel ID validator for replies
            ChannelIdValidator channelIdValidator = new ChannelIdValidator(channelService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.CHANNEL_ID, channelIdValidator);
            LOGGER.info("Registered ChannelValidator for CHANNEL_ID");

            // Register Channel Message ID validator for replies
            ChannelMessageIdForRepliesValidator channelMessageIdForRepliesValidator = new ChannelMessageIdForRepliesValidator(chatService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.CHANNEL_MESSAGE_ID_FOR_REPLIES, channelMessageIdForRepliesValidator);
            LOGGER.info("Registered ChannelMessageIdForRepliesValidator for CHANNEL_MESSAGE_ID_FOR_REPLIES");

            LOGGER.info("GetChannelMessageRepliesHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getChannelMessageRepliesWithValidation(String channelMessageId, String channelId, String teamId, Boolean allowRetry) {
        try {
            LOGGER.info("GetChannelMessageRepliesHelper.getChannelMessageRepliesWithValidation() called with channelMessageId: {}, channelId: {}, teamId: {}, allowRetry: {}",
                    channelMessageId, channelId, teamId, allowRetry);

            // Validate input parameters - validate Channel Message ID, Channel ID, and Team ID
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.CHANNEL_MESSAGE_ID_FOR_REPLIES, channelMessageId != null ? channelMessageId : "",
                            Validator.ValidationResource.CHANNEL_ID, channelId != null ? channelId : "",
                            Validator.ValidationResource.TEAM_ID, teamId != null ? teamId : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Validation failed with {} errors (Channel Message ID, Channel ID, Team ID)", validationResults.size());
                return handleValidationFailure(validationResults, channelMessageId, channelId, teamId, allowRetry);
            }

            // If validation passes, proceed with retrieving replies
            LOGGER.debug("Validation passed, retrieving channel message replies");
            List<Replies> result = chatService.getChannelMessageReplies(channelMessageId, channelId, teamId);

            if (result == null || result.isEmpty()) {
                LOGGER.warn("No replies found for channel message ID: {}", channelMessageId);

                String responseMessage = "No replies found for channel message ID: " + channelMessageId;
                return new ExtensionResponseBuilder()
                        .success(Map.of(
                                "Replies List", Collections.emptyList(),
                                "Response Message", responseMessage))
                        .build();
            }

            LOGGER.info("Successfully retrieved {} replies for channel message ID: {}", result.size(), channelMessageId);

            String responseMessage = "Successfully retrieved " + result.size() + " reply/replies for channel message ID: " + channelMessageId;
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Replies List", result,
                            "Response Message", responseMessage))
                    .build();
        } catch (Exception e) {
            LOGGER.error("Error retrieving channel message replies: {}", e.getMessage(), e);

            String responseMessage = "Error retrieving channel message replies for message ID '" + channelMessageId + "': " + e.getMessage();
            return new ExtensionResponseBuilder()
                    .failure(new ErrorBuilder()
                            .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                            .errorMessage(responseMessage)
                            .build())
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String channelMessageId, String channelId, String teamId, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Get Channel Message Replies");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("Channel ID validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("Channel ID validation failed, generating confirmation response");

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
                    SubCatalogConstants.CONFIRM_CHANNEL_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES,
                    map);
        }

        LOGGER.info("Allow Retry = false, returning error directly for Get Channel Message Replies");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }
}
