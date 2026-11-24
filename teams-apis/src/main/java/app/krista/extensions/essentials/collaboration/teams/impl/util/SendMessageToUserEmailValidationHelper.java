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
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.SendMessageToUserUtils;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.MessageContentValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.UserEmailValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Send Message to User validation functionality.
 * Contains all the logic for validating user email addresses and message content before sending messages.
 * Follows the established pattern from other validation helpers.
 */
@Singleton
@Service
public class SendMessageToUserEmailValidationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendMessageToUserEmailValidationHelper.class);

    private final ValidationOrchestrator validationOrchestrator;
    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final SendMessageToUserUtils sendMessageToUserUtils;
    private final UserEmailValidator userEmailValidator;
    private final MessageContentValidator messageContentValidator;

    @Inject
    public SendMessageToUserEmailValidationHelper(ValidationOrchestrator validationOrchestrator,
                                                  ExtensionResponseGenerator responseGenerator,
                                                  ErrorHandlingStateManager internalStateManager,
                                                  SendMessageToUserUtils sendMessageToUserUtils,
                                                  UserEmailValidator userEmailValidator,
                                                  MessageContentValidator messageContentValidator) {
        this.validationOrchestrator = validationOrchestrator;
        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.sendMessageToUserUtils = sendMessageToUserUtils;
        this.userEmailValidator = userEmailValidator;
        this.messageContentValidator = messageContentValidator;

        // Register validators
        registerValidators();

        LOGGER.info("SendMessageToUserEmailValidationHelper initialized successfully");
    }

    private void registerValidators() {
        try {
            LOGGER.debug("SendMessageToUserEmailValidationHelper.registerValidators() - Starting validator registration");

            // Register the injected User Email validator
            validationOrchestrator.registerValidator(Validator.ValidationResource.USER_EMAIL_FOR_SEND_MESSAGE, userEmailValidator);
            LOGGER.info("Registered injected UserEmailValidator for USER_EMAIL_FOR_SEND_MESSAGE");

            // Register the injected Message Content validator
            validationOrchestrator.registerValidator(Validator.ValidationResource.MESSAGE_CONTENT_FOR_SEND_MESSAGE, messageContentValidator);
            LOGGER.info("Registered injected MessageContentValidator for MESSAGE_CONTENT_FOR_SEND_MESSAGE");

            LOGGER.info("SendMessageToUserEmailValidationHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators in SendMessageToUserEmailValidationHelper: {}", e.getMessage(), e);
        }
    }

    /**
     * Send message to user with email and message content validation.
     * This method validates both the user email and message content before sending the message.
     *
     * @param userEmail       The email address of the user to send the message to.
     * @param messageContent  The message content.
     * @param reuseChatId     Whether to reuse existing chat if found.
     * @param logConversation Whether to store chat ID in memory for reuse.
     * @param allowRetry      Whether to allow retry on validation failure.
     * @return ExtensionResponse containing the result or validation errors.
     */
    public ExtensionResponse sendMessageToUserWithEmailValidation(String userEmail, String messageContent,
                                                                  boolean reuseChatId, boolean logConversation, Boolean allowRetry) {
        return sendMessageToUserWithEmailValidation(userEmail, messageContent, reuseChatId, logConversation, false, allowRetry);
    }

    /**
     * Send message to user with email and message content validation, with HTML support.
     * This method validates both the user email and message content before sending the message.
     *
     * @param userEmail       The email address of the user to send the message to.
     * @param messageContent  The message content.
     * @param reuseChatId     Whether to reuse existing chat if found.
     * @param logConversation Whether to store chat ID in memory for reuse.
     * @param isHtml          Whether the message content is HTML.
     * @param allowRetry      Whether to allow retry on validation failure.
     * @return ExtensionResponse containing the result or validation errors.
     */
    public ExtensionResponse sendMessageToUserWithEmailValidation(String userEmail, String messageContent,
                                                                  boolean reuseChatId, boolean logConversation, boolean isHtml, Boolean allowRetry) {
            LOGGER.info("SendMessageToUserEmailValidationHelper.sendMessageToUserWithEmailValidation() called with userEmail: {}, messageContent length: {}, reuseChatId: {}, logConversation: {}, allowRetry: {}",
                    userEmail, messageContent != null ? messageContent.length() : 0, reuseChatId, logConversation, allowRetry);

            // Validate input parameters - validate both User Email and Message Content
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(
                            Validator.ValidationResource.USER_EMAIL_FOR_SEND_MESSAGE, userEmail != null ? userEmail : "",
                            Validator.ValidationResource.MESSAGE_CONTENT_FOR_SEND_MESSAGE, messageContent != null ? messageContent : ""
                    )
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Email and/or message content validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, userEmail, messageContent, reuseChatId, logConversation, allowRetry);
            }

            // If validation passes, proceed with sending the message
            LOGGER.debug("Email and message content validation passed, sending message to user");
            Map<String, String> result = sendMessageToUserUtils.sendMessageToUserWithDetails(userEmail, messageContent, reuseChatId, logConversation, isHtml);

            if (result == null || result.isEmpty()) {
                LOGGER.warn("No result returned from sendMessageToUserWithDetails for user: {}", userEmail);

                // Return error response
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("Status Code", "");
                errorResponse.put("Message Id", "");
                errorResponse.put("Chat Id", "");
                errorResponse.put("Response Message", "Error: No response received from message service");

                return new ExtensionResponseBuilder()
                        .success(errorResponse)
                        .build();
            }

            LOGGER.info("Successfully processed send message to user request for: {}, Status Code: {}", userEmail, result.get("Status Code"));

            // Create response with the detailed results
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("Status Code", result.get("Status Code"));
            responseData.put("Message Id", result.get("Message Id"));
            responseData.put("Chat Id", result.get("Chat Id"));
            responseData.put("Response Message", result.get("Response Message"));

            return new ExtensionResponseBuilder()
                    .success(responseData)
                    .build();
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String userEmail, String messageContent, boolean reuseChatId, boolean logConversation, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Send Message to User");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("Send Message to User validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("Send Message to User validation failed, generating confirmation response");

            // Create a map with the necessary data for the confirmation step
            Map<String, Object> map = Map.of(
                    TeamsResources.STATE_ID, stateId,
                    TeamsResources.USER_EMAIL, userEmail != null ? userEmail : "",
                    TeamsResources.MESSAGE_CONTENT, messageContent != null ? messageContent : "",
                    "reuseChatId", reuseChatId,
                    "logConversation", logConversation
            );

            // Generate a response that will trigger the confirmation subcatalog request
            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_SEND_MESSAGE_TO_USER,
                    map);
        }

        LOGGER.info("Allow Retry = false, returning error directly for Send Message to User");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }
}
