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
import app.krista.extension.executor.ErrorBuilder;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.GroupChat;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.GroupNameValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Helper class for Get Group Chat by Name operations with validation support.
 */
@Service
public class GetGroupChatByNameHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetGroupChatByNameHelper.class);

    private final ChatService chatService;
    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;

    @Inject
    public GetGroupChatByNameHelper(ChatService chatService,
                                    ValidationOrchestrator validationOrchestrator,
                                    ErrorHandlingStateManager internalStateManager,
                                    ExtensionResponseGenerator responseGenerator) {
        this.chatService = chatService;
        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        LOGGER.info("GetGroupChatByNameHelper initialized");
        registerValidators();
    }

    private void registerValidators() {
        try {
            // Register Group Name validator
            GroupNameValidator groupNameValidator = new GroupNameValidator(chatService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.GROUP_NAME, groupNameValidator);
            LOGGER.info("Registered GroupNameValidator for GROUP_NAME");

            LOGGER.info("GetGroupChatByNameHelper.registerValidators() completed successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getGroupChatByNameWithValidation(String groupName, Boolean allowRetry) {
        try {
            LOGGER.info("GetGroupChatByNameHelper.getGroupChatByNameWithValidation() called with groupName: {}, allowRetry: {}", groupName, allowRetry);

            // Validate input parameters
            List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                    Map.of(Validator.ValidationResource.GROUP_NAME, groupName != null ? groupName : "")
            );

            // If validation fails, handle it
            if (!validationResults.isEmpty()) {
                LOGGER.warn("Group name validation failed with {} errors", validationResults.size());
                return handleValidationFailure(validationResults, groupName, allowRetry);
            }

            // If validation passes, proceed with retrieving group chat
            LOGGER.debug("Validation passed, retrieving group chat");
            GroupChat result = chatService.getGroupChatByName(groupName);

            if (result == null) {
                LOGGER.warn("No group chat found with name: {}", groupName);

                return new ExtensionResponseBuilder()
                        .success(Map.of("Group", "No group chat found with the specified name"), null, null)
                        .build();
            }

            LOGGER.info("Successfully retrieved group chat: {}", result.topic);

            return new ExtensionResponseBuilder()
                    .success(Map.of("Group", result), null, null)
                    .build();

        } catch (Exception cause) {
            LOGGER.error("Error occurred while retrieving group chat by name: {}", cause.getMessage(), cause);
            return generateErrorResponse("Error occurred while retrieving group chat by name");
        }
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String groupName, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Get Group Chat by Name");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("Group name validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("Group name validation failed, generating confirmation response");

            // Create a map with the necessary data for the confirmation step
            Map<String, Object> map = Map.of(
                    TeamsResources.STATE_ID, stateId,
                    TeamsResources.GROUP_NAME, groupName != null ? groupName : ""
            );

            // Generate a response that will trigger the confirmation subcatalog request
            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_GROUP_NAME_VALIDATION_GET_GROUP_CHAT_BY_NAME,
                    map);
        }

        LOGGER.info("Allow Retry = false, returning error directly for Get Group Chat by Name");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }

    private ExtensionResponse generateErrorResponse(String errorMessage) {
        LOGGER.error("Generating error response: {}", errorMessage);
        ExtensionResponse.Error error = new ErrorBuilder()
                .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                .errorMessage(errorMessage)
                .build();

        return new ExtensionResponseBuilder()
                .failure(error)
                .build();
    }
}
