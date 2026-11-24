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
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ChannelNameValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.HTMLMessageContentValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.TeamNameValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Send HTML Message to Teams Channel validation functionality.
 * Contains all the logic for validating team name, channel name, and HTML message content before sending messages.
 */
@Singleton
@Service
public class SendHTMLMessageToChannelValidationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendHTMLMessageToChannelValidationHelper.class);

    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final TeamNameValidator teamNameValidator;
    private final ChannelNameValidator channelNameValidator;
    private final HTMLMessageContentValidator htmlMessageContentValidator;
    private final ChatService chatService;

    @Inject
    public SendHTMLMessageToChannelValidationHelper(ValidationOrchestrator validationOrchestrator,
                                                    ExtensionResponseGenerator responseGenerator,
                                                    ErrorHandlingStateManager internalStateManager,
                                                    TeamNameValidator teamNameValidator,
                                                    ChannelNameValidator channelNameValidator,
                                                    HTMLMessageContentValidator htmlMessageContentValidator, ChatService chatService) {
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.teamNameValidator = teamNameValidator;
        this.channelNameValidator = channelNameValidator;
        this.htmlMessageContentValidator = htmlMessageContentValidator;
        this.chatService = chatService;

        // Register validators
        registerValidators();

        LOGGER.info("SendHTMLMessageToChannelValidationHelper initialized successfully");
    }

    private void registerValidators() {
        try {
            LOGGER.debug("SendHTMLMessageToChannelValidationHelper.registerValidators() - Starting validator registration");

            // Register the Team Name validator
            validationOrchestrator.registerValidator(Validator.ValidationResource.TEAM_NAME_FOR_SEND_HTML_MESSAGE, teamNameValidator);
            LOGGER.info("Registered TeamNameValidator for TEAM_NAME_FOR_SEND_HTML_MESSAGE");

            // Register the Channel Name validator
            validationOrchestrator.registerValidator(Validator.ValidationResource.CHANNEL_NAME_FOR_SEND_HTML_MESSAGE, channelNameValidator);
            LOGGER.info("Registered ChannelNameValidator for CHANNEL_NAME_FOR_SEND_HTML_MESSAGE");

            // Register the HTML Message Content validator
            validationOrchestrator.registerValidator(Validator.ValidationResource.HTML_MESSAGE_CONTENT_FOR_CHANNEL, htmlMessageContentValidator);
            LOGGER.info("Registered HTMLMessageContentValidator for HTML_MESSAGE_CONTENT_FOR_CHANNEL");

            LOGGER.info("SendHTMLMessageToChannelValidationHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators in SendHTMLMessageToChannelValidationHelper: {}", e.getMessage(), e);
        }
    }

    /**
     * Send HTML message to Teams channel with team name, channel name, and HTML message content validation.
     * This method validates all inputs before sending the message.
     *
     * @param teamName    The name of the team.
     * @param channelName The name of the channel.
     * @param message     The HTML message content.
     * @param allowRetry  Whether to allow retry on validation failure.
     * @return ExtensionResponse containing the result or validation errors.
     */
    public ExtensionResponse sendHTMLMessageToChannelWithValidation(String teamName, String channelName, String message, Boolean allowRetry) {
        try {
            LOGGER.info("SendHTMLMessageToChannelValidationHelper.sendHTMLMessageToChannelWithValidation() called with teamName: {}, channelName: {}, message length: {}, allowRetry: {}",
                    teamName, channelName, message != null ? message.length() : 0, allowRetry);

            // Validate input parameters - validate Team Name, Channel Name, and HTML Message Content
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.TEAM_NAME_FOR_SEND_HTML_MESSAGE, teamName != null ? teamName : "",
                            Validator.ValidationResource.CHANNEL_NAME_FOR_SEND_HTML_MESSAGE, channelName != null ? channelName : "",
                            Validator.ValidationResource.HTML_MESSAGE_CONTENT_FOR_CHANNEL, message != null ? message : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Team name, channel name, and/or HTML message content validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, teamName, channelName, message, allowRetry);
            }

            // If validation passes, proceed with sending the HTML message
            LOGGER.debug("Team name, channel name, and HTML message content validation passed, sending HTML message to channel");
            Boolean result = chatService.sendMessageToChannel(teamName, channelName, message, true);
            LOGGER.info("Successfully processed send HTML message to channel request for team: {}, channel: {}, result: {}",
                    teamName, channelName, result);

            // Create response message based on result
            String responseMessage;
            if (Boolean.TRUE.equals(result)) {
                responseMessage = "HTML message successfully sent to channel '" + channelName + "' in team '" + teamName + "'";
            } else {
                responseMessage = "Failed to send HTML message to channel '" + channelName + "' in team '" + teamName + "'";
            }

            // Create response with the result and response message
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Is Message Delivered", result,
                            "Response Message", responseMessage))
                    .build();

        } catch (Exception e) {
            LOGGER.error("Error in sendHTMLMessageToChannelWithValidation for team: {}, channel: {}", teamName, channelName, e);

            // Create error response with error message
            String errorMessage = "Error sending HTML message to channel '" + channelName + "' in team '" + teamName + "': " + e.getMessage();
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Is Message Delivered", false,
                            "Response Message", errorMessage))
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String teamName, String channelName, String message, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Send HTML Message to Channel");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("Send HTML Message to Channel validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("Send HTML Message to Channel validation failed, generating confirmation response");

            // Create a map with the necessary data for the confirmation step
            Map<String, Object> map = Map.of(
                    TeamsResources.STATE_ID, stateId,
                    TeamsResources.TEAM_NAME, teamName != null ? teamName : "",
                    TeamsResources.CHANNEL_NAME, channelName != null ? channelName : "",
                    TeamsResources.HTML_MESSAGE_CONTENT_FOR_CHANNEL, message != null ? message : ""
            );

            // Generate a response that will trigger the confirmation subcatalog request
            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_SEND_HTML_MESSAGE_TO_CHANNEL,
                    map);
        }

        LOGGER.info("Allow Retry = false, returning error directly for Send HTML Message to Channel");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }
}

