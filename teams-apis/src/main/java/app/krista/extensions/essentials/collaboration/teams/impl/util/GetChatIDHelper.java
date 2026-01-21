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
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.ChannelMessage;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.ChannelMessage;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ChatIdValidator;

import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.DateRangeValidator;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;

@Service
public class GetChatIDHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetChatIDHelper.class);
    private static final String STATE_ID = "stateId";

    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final ChatService chatService;
    private final ChatIdValidator chatIdValidator;
    private final DateRangeValidator dateRangeValidator;
    private final ErrorHandlingStateManager errorHandlingStateManager;

    @Inject
    public GetChatIDHelper(ValidationOrchestrator validationOrchestrator,
                           ErrorHandlingStateManager internalStateManager,
                           ExtensionResponseGenerator responseGenerator,
                           ChatService chatService,
                           ChatIdValidator chatIdValidator,
                           DateRangeValidator dateRangeValidator) {
        LOGGER.info("GetChatIDHelper constructor called");

        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        this.chatService = chatService;
        this.chatIdValidator = chatIdValidator;
        this.dateRangeValidator = dateRangeValidator;
        this.errorHandlingStateManager = internalStateManager;

        // Register validators if not already registered
        registerValidators();

        LOGGER.info("GetChatIDHelper constructor completed");
    }

    private void registerValidators() {
        try {
            LOGGER.info("GetChatIDHelper.registerValidators() - Registering validators");

            // Register Chat ID validator
            ChatIdValidator chatIdValidator = new ChatIdValidator();
            validationOrchestrator.registerValidator(Validator.ValidationResource.CHAT_ID, chatIdValidator);
            LOGGER.info("Registered ChatIdValidator for CHAT_ID");

            // Register Date Range validator
            DateRangeValidator dateRangeValidator = new DateRangeValidator();
            validationOrchestrator.registerValidator(Validator.ValidationResource.FROM_DATE, dateRangeValidator);

            LOGGER.info("GetChatIDHelper.registerValidators() completed successfully");

        } catch (Exception e) {
            LOGGER.error("Error occurred while registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getGroupChatMessagesWithValidation(String chatID, Long fromDate, Long toDate, Boolean allowRetry) {
        LOGGER.info("GetChatIDHelper.getGroupChatMessagesWithValidation() called with chatID={}, fromDate={}, toDate={}, allowRetry={}",
                chatID, fromDate, toDate, allowRetry);

        // Validate input parameters using ValidationOrchestrator
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.CHAT_ID, chatID != null ? chatID : "",
                        Validator.ValidationResource.FROM_DATE, fromDate != null ? fromDate.toString() : "",
                        Validator.ValidationResource.TO_DATE, toDate != null ? toDate.toString() : ""
                )
        );

        // If validation fails, handle it
        if (!validationResults.isEmpty()) {
            LOGGER.warn("Validation failed with {} errors", validationResults.size());
            return handleValidationFailure(validationResults, chatID, fromDate, toDate, allowRetry);
        }

        try {
            // Retrieve messages using the chat service
            List<ChannelMessage> result = chatService.getGroupChats(chatID, fromDate, toDate);

            if (result.isEmpty()) {
                LOGGER.info("No group chat messages found in the provided date range for chatID: {}", chatID);
                return new ExtensionResponseBuilder()
                        .success(Map.of("Group Chats", "Group chat messages are not present in the provided date range"))
                        .build();
            }

            LOGGER.info("Successfully processed group chat messages. Retrieved {} messages", result.size());
            return new ExtensionResponseBuilder()
                    .success(Map.of("Group Chat Messages", result))
                    .build();
        } catch (Exception cause) {
            LOGGER.error("Failed to fetch group chat messages for chatID: {}", chatID, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to fetch group chat messages: " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String chatID, Long fromDate, Long toDate, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Get Group Chat Messages");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();

            // Store validation results for later retrieval
            internalStateManager.put(stateId, app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.GSON.toJson(validationResults));

            // Generate a confirmation response that will prompt the user to re-enter data
            // Create a map that handles null values safely
            Map<String, Object> chatResponseData = new java.util.HashMap<>();
            chatResponseData.put(STATE_ID, stateId);
            chatResponseData.put(TeamsResources.CHAT_ID, chatID);
            chatResponseData.put(TeamsResources.FROM_DATE, fromDate); // Can be null
            chatResponseData.put(TeamsResources.TO_DATE, toDate);     // Can be null

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_REENTER_GET_GROUP_CHAT_MESSAGES,
                    chatResponseData
            );
        }

        LOGGER.info("Allow Retry = false, returning error directly for Get Group Chat Messages");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }
}
