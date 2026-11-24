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

package app.krista.extensions.essentials.collaboration.teams.catalog;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.SubCatalogRequest;


import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;

import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.impl.util.GetChannelIDHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.GetChatIDHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.GetGroupChatByNameHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.GetChannelMessageRepliesHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.GetChannelMessageRepliesTeamIdHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.GetChannelsOfTeamHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.SendMessageToUserEmailValidationHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.SendHTMLMessageToUserValidationHelper;
import app.krista.extensions.essentials.collaboration.teams.impl.util.SendHTMLMessageToChannelValidationHelper;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.ChannelMessage;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;

import org.jetbrains.annotations.NotNull;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class TeamsAPIsAreaSubCatalogRequests {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamsAPIsAreaSubCatalogRequests.class);

    private final ExtensionResponseGenerator responseGenerator;
    private final ErrorHandlingStateManager internalStateManager;
    private final GetChannelIDHelper getChannelIDHelper;
    private final GetChatIDHelper getChatIDHelper;
    private final GetGroupChatByNameHelper getGroupChatByNameHelper;
    private final GetChannelMessageRepliesHelper getChannelMessageRepliesHelper;
    private final GetChannelMessageRepliesTeamIdHelper getChannelMessageRepliesTeamIdHelper;
    private final GetChannelsOfTeamHelper getChannelsOfTeamHelper;
    private final ChatService chatService;
    private final ChannelService channelService;
    private final ValidationOrchestrator validationOrchestrator;
    private final SendMessageToUserEmailValidationHelper sendMessageToUserEmailValidationHelper;
    private final SendHTMLMessageToUserValidationHelper sendHTMLMessageToUserValidationHelper;
    private final SendHTMLMessageToChannelValidationHelper sendHTMLMessageToChannelValidationHelper;

    @Inject
    public TeamsAPIsAreaSubCatalogRequests(ExtensionResponseGenerator responseGenerator,
                                           ErrorHandlingStateManager internalStateManager,
                                           GetChannelIDHelper getChannelIDHelper,
                                           GetChatIDHelper getChatIDHelper,
                                           GetGroupChatByNameHelper getGroupChatByNameHelper,
                                           GetChannelMessageRepliesHelper getChannelMessageRepliesHelper,
                                           GetChannelMessageRepliesTeamIdHelper getChannelMessageRepliesTeamIdHelper,
                                           GetChannelsOfTeamHelper getChannelsOfTeamHelper,
                                           ChatService chatService, ChannelService channelService,
                                           ValidationOrchestrator validationOrchestrator,
                                           SendMessageToUserEmailValidationHelper sendMessageToUserEmailValidationHelper,
                                           SendHTMLMessageToUserValidationHelper sendHTMLMessageToUserValidationHelper,
                                           SendHTMLMessageToChannelValidationHelper sendHTMLMessageToChannelValidationHelper) {
        this.validationOrchestrator = validationOrchestrator;
        LOGGER.info("TeamsAPIsAreaSubCatalogRequests constructor called");

        this.responseGenerator = responseGenerator;
        this.internalStateManager = internalStateManager;
        this.getChannelIDHelper = getChannelIDHelper;
        this.getChatIDHelper = getChatIDHelper;
        this.getGroupChatByNameHelper = getGroupChatByNameHelper;
        this.getChannelMessageRepliesHelper = getChannelMessageRepliesHelper;
        this.getChannelMessageRepliesTeamIdHelper = getChannelMessageRepliesTeamIdHelper;
        this.getChannelsOfTeamHelper = getChannelsOfTeamHelper;
        this.chatService = chatService;
        this.channelService = channelService;
        this.sendMessageToUserEmailValidationHelper = sendMessageToUserEmailValidationHelper;
        this.sendHTMLMessageToUserValidationHelper = sendHTMLMessageToUserValidationHelper;
        this.sendHTMLMessageToChannelValidationHelper = sendHTMLMessageToChannelValidationHelper;

        LOGGER.info("TeamsAPIsAreaSubCatalogRequests constructor completed");
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REENTER_GET_CHANNEL_MESSAGES,
            description = "Checks if user wants to re-enter input and validates it",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmReenterGetChannelMessages(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Channel ID: Text, Team ID: Text, From Date: Date, To Date: Date }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmReenterGetChannelMessages() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("confirmReenterGetChannelMessages() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_REENTER_GET_CHANNEL_MESSAGES,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_REENTER_GET_CHANNEL_MESSAGES,
            description = "Handle reenter channel messages",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Channel Messages", type = "[ Entity ]", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterGetChannelMessages(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Channel ID: Text, Team ID: Text, From Date: Date, To Date: Date}") Map<String, Object> map) {

        LOGGER.debug("handleReenterGetChannelMessages() - Starting with input map: {}", map);

        String channelID = (String) map.get(TeamsResources.CHANNEL_ID);
        String teamID = (String) map.get(TeamsResources.TEAM_ID);
        Long fromDate = map.get(TeamsResources.FROM_DATE) != null ? ((Number) map.get(TeamsResources.FROM_DATE)).longValue() : null;
        Long toDate = map.get(TeamsResources.TO_DATE) != null ? ((Number) map.get(TeamsResources.TO_DATE)).longValue() : null;

        LOGGER.debug("handleReenterGetChannelMessages() - Extracted parameters: channelID={}, teamID={}, fromDate={}, toDate={}",
                channelID, teamID, fromDate, toDate);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.CHANNEL_ID, channelID != null ? channelID : "",
                        Validator.ValidationResource.TEAM_ID, teamID != null ? teamID : ""
                )
        );

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided channel and team IDs are still invalid. Please try again with a new request.")
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Proceed with retrieving messages using the validated data
            List<ChannelMessage> result = chatService.getChannelMessagesWithDateRangeLimit(channelID, teamID, fromDate, toDate, 50);

            // Check if no messages were found in the date range
            if (result.isEmpty()) {
                return new ExtensionResponseBuilder()
                        .success(Map.of("Channel Messages", "Channel messages are not present in the provided date range"))
                        .build();
            }
            return new ExtensionResponseBuilder()
                    .success(Map.of("Channel Messages", result))
                    .build();
        } catch (Exception cause) {
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to fetch channel messages: Please enter valid data with a new request")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }


    // ========== GET CHANNEL MESSAGES - DATE VALIDATION SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_DATE_VALIDATION_GET_CHANNEL_MESSAGES,
            description = "Checks if user wants to re-enter date range for Get Channel Messages",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmDateValidationGetChannelMessages(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Channel ID: Text, Team ID: Text, From Date: Date, To Date: Date }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("confirmDateValidationGetChannelMessages() - Reenter: {}, stateId: {}", reenter, stateId);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmDateValidationGetChannelMessages() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_DATE_VALIDATION_GET_CHANNEL_MESSAGES,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_DATE_VALIDATION_GET_CHANNEL_MESSAGES,
            description = "Handle date validation re-entry for channel messages",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Channel Messages", type = "[ Entity ]", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleDateValidationGetChannelMessages(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Channel ID: Text, Team ID: Text, From Date: Date, To Date: Date}") Map<String, Object> map) {

        String channelID = (String) map.get(TeamsResources.CHANNEL_ID);
        String teamID = (String) map.get(TeamsResources.TEAM_ID);
        Long fromDate = map.get(TeamsResources.FROM_DATE) != null ? ((Number) map.get(TeamsResources.FROM_DATE)).longValue() : null;
        Long toDate = map.get(TeamsResources.TO_DATE) != null ? ((Number) map.get(TeamsResources.TO_DATE)).longValue() : null;

        LOGGER.debug("handleDateValidationGetChannelMessages() - Extracted parameters: channelID={}, teamID={}, fromDate={}, toDate={}",
                channelID, teamID, fromDate, toDate);

        // Validate the re-entered date range
        Map<Validator.ValidationResource, String> dateValidationContext = new HashMap<>();
        if (fromDate != null) {
            dateValidationContext.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
        }

        // If toDate is null, set it to current date for validation
        Long effectiveToDate = toDate != null ? toDate : System.currentTimeMillis();
        dateValidationContext.put(Validator.ValidationResource.TO_DATE, effectiveToDate.toString());

        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(dateValidationContext);

        LOGGER.debug("handleDateValidationGetChannelMessages() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.info("Re-entered date validation failed with {} errors. No more retries allowed.", validationResults.size());

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided date range is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
        ExtensionResponse response = getChannelIDHelper.getChannelMessagesWithValidation(channelID, teamID, fromDate, toDate, null);
        return response;
    }

    // ========== GROUP CHAT MESSAGES SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_REENTER_GET_GROUP_CHAT_MESSAGES,
            description = "Checks if user wants to re-enter input and validates it for Group Chat Messages",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmReenterGetGroupChatMessages(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Chat ID: Text, From Date: Date, To Date: Date }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("confirmReenterGetGroupChatMessages() - Reenter: {}, stateId: {}", reenter, stateId);
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_REENTER_GET_GROUP_CHAT_MESSAGES,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_REENTER_GET_GROUP_CHAT_MESSAGES,
            description = "Handle reenter group chat messages",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Group Chats", type = "[ Entity ]", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleReenterGetGroupChatMessages(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Chat ID: Text, From Date: Date, To Date: Date}") Map<String, Object> map) {

        String chatID = (String) map.get(TeamsResources.CHAT_ID);
        Long fromDate = map.get(TeamsResources.FROM_DATE) != null ? ((Number) map.get(TeamsResources.FROM_DATE)).longValue() : null;
        Long toDate = map.get(TeamsResources.TO_DATE) != null ? ((Number) map.get(TeamsResources.TO_DATE)).longValue() : null;

        LOGGER.debug("handleReenterGetGroupChatMessages() - Extracted parameters: chatID={}, fromDate={}, toDate={}", chatID, fromDate, toDate);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.CHAT_ID, chatID != null ? chatID : "")
        );

        LOGGER.debug("handleReenterGetGroupChatMessages() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return an error response
        if (!validationResults.isEmpty()) {

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided data is still invalid. Please try again with correct values.")
                    .build();

            LOGGER.info("Returning error response");
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        try {
            // Proceed with retrieving messages using the validated data
            LOGGER.info("Validation passed, retrieving messages");
            List<ChannelMessage> result = chatService.getGroupChats(chatID, fromDate, toDate);

            if (result.isEmpty()) {
                String responseMessage = "No group chat messages found in the provided date range for chatID: " + chatID;
                return new ExtensionResponseBuilder()
                        .success(Map.of(
                                "Group Chat Messages", new java.util.ArrayList<>(),
                                "Response Message", responseMessage))
                        .build();
            }

            String responseMessage = "Successfully retrieved " + result.size() + " group chat message(s) for chatID: " + chatID;
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Group Chat Messages", result,
                            "Response Message", responseMessage))
                    .build();
        } catch (Exception cause) {

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to fetch group chat messages: Please enter valid data with a new request")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_DATE_VALIDATION_GET_GROUP_CHAT_MESSAGES,
            description = "Checks if user wants to re-enter date range for Group Chat Messages",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmDateValidationGetGroupChatMessages(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Chat ID: Text, From Date: Date, To Date: Date }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmDateValidationGetGroupChatMessages() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("confirmDateValidationGetGroupChatMessages() - Reenter: {}, stateId: {}", reenter, stateId);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_DATE_VALIDATION_GET_GROUP_CHAT_MESSAGES,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_DATE_VALIDATION_GET_GROUP_CHAT_MESSAGES,
            description = "Handle date validation re-entry for group chat messages",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Group Chats", type = "[ Entity ]", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleDateValidationGetGroupChatMessages(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Chat ID: Text, From Date: Date, To Date: Date}") Map<String, Object> map) {

        String chatID = (String) map.get(TeamsResources.CHAT_ID);
        Long fromDate = map.get(TeamsResources.FROM_DATE) != null ? ((Number) map.get(TeamsResources.FROM_DATE)).longValue() : null;
        Long toDate = map.get(TeamsResources.TO_DATE) != null ? ((Number) map.get(TeamsResources.TO_DATE)).longValue() : null;
        ExtensionResponse response = getChatIDHelper.getGroupChatMessagesWithValidation(chatID, fromDate, toDate, null);

        return response;
    }

    // ========== GET GROUP CHAT BY NAME SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_GROUP_NAME_VALIDATION_GET_GROUP_CHAT_BY_NAME,
            description = "Checks if user wants to re-enter group name for Get Group Chat by Name",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmGroupNameValidationGetGroupChatByName(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Group Name: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmGroupNameValidationGetGroupChatByName() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmGroupNameValidationGetGroupChatByName() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_GROUP_NAME_VALIDATION_GET_GROUP_CHAT_BY_NAME,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_GROUP_NAME_VALIDATION_GET_GROUP_CHAT_BY_NAME,
            description = "Handle group name validation re-entry for Get Group Chat by Name",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Group", type = "Entity", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleGroupNameValidationGetGroupChatByName(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Group Name: Text}") Map<String, Object> map) {

        LOGGER.debug("handleGroupNameValidationGetGroupChatByName() - Starting with input map: {}", map);

        String groupName = (String) map.get(TeamsResources.GROUP_NAME);

        // Validate the re-entered data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.GROUP_NAME, groupName != null ? groupName : "")
        );

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided group name is still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
        ExtensionResponse response = getGroupChatByNameHelper.getGroupChatByNameWithValidation(groupName, null);
        return response;
    }

    // ========== GET CHANNEL MESSAGES - CHANNEL TEAM LINK SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_CHANNEL_TEAM_LINK_VALIDATION_GET_CHANNEL_MESSAGES,
            description = "Checks if user wants to re-enter Channel ID and Team ID for Get Channel Messages",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmChannelTeamLinkValidationGetChannelMessages(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Channel ID: Text, Team ID: Text, From Date: Date, To Date: Date }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmChannelTeamLinkValidationGetChannelMessages() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("confirmChannelTeamLinkValidationGetChannelMessages() - Reenter: {}, stateId: {}", reenter, stateId);

        LOGGER.debug("confirmChannelTeamLinkValidationGetChannelMessages() - Getting validation results");
        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);

        if (Boolean.FALSE.equals(reenter)) {
            LOGGER.info("User chose not to re-enter data");
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_CHANNEL_TEAM_LINK_VALIDATION_GET_CHANNEL_MESSAGES,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_CHANNEL_TEAM_LINK_VALIDATION_GET_CHANNEL_MESSAGES,
            description = "Handle Channel-Team link validation re-entry for Get Channel Messages",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Channel Messages", type = "[ Entity ]", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleChannelTeamLinkValidationGetChannelMessages(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Channel ID: Text, Team ID: Text, From Date: Date, To Date: Date}") Map<String, Object> map) {

        LOGGER.debug("handleChannelTeamLinkValidationGetChannelMessages() - Starting with input map: {}", map);

        String channelID = (String) map.get(TeamsResources.CHANNEL_ID);
        String teamID = (String) map.get(TeamsResources.TEAM_ID);
        Long fromDate = map.get(TeamsResources.FROM_DATE) != null ? ((Number) map.get(TeamsResources.FROM_DATE)).longValue() : null;
        Long toDate = map.get(TeamsResources.TO_DATE) != null ? ((Number) map.get(TeamsResources.TO_DATE)).longValue() : null;

        LOGGER.debug("handleChannelTeamLinkValidationGetChannelMessages() - Extracted parameters: channelID={}, teamID={}, fromDate={}, toDate={}",
                channelID, teamID, fromDate, toDate);

        // Validate the re-entered data for channel-team link
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.CHANNEL_ID, channelID != null ? channelID : "",
                        Validator.ValidationResource.TEAM_ID, teamID != null ? teamID : ""
                )
        );

        LOGGER.debug("handleChannelTeamLinkValidationGetChannelMessages() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.info("Re-entered data validation failed with {} errors. No more retries allowed.", validationResults.size());

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided channel ID and team ID combination is still invalid. Please try again with a new request.")
                    .build();

            LOGGER.info("Returning permanent error response after retry attempt");
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        // Call the main method with corrected data
        LOGGER.info("Calling getChannelMessagesWithValidation with corrected data");
        ExtensionResponse response = getChannelIDHelper.getChannelMessagesWithValidation(channelID, teamID, fromDate, toDate, null);

        return response;
    }

    // ========== GET CHANNEL MESSAGE REPLIES SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_CHANNEL_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES,
            description = "Checks if user wants to re-enter Channel ID for Get Channel Message Replies",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmChannelIdValidationGetChannelMessageReplies(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Channel Message ID: Text, Channel ID: Text, Team ID: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmChannelIdValidationGetChannelMessageReplies() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER); // Use constant instead of string literal
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmChannelIdValidationGetChannelMessageReplies() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            LOGGER.info("User chose not to re-enter data");
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_CHANNEL_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_CHANNEL_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES,
            description = "Handle Channel ID validation re-entry for Get Channel Message Replies",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Replies List", type = "[ Entity ]", required = false)
    @Field.Text(name = "Response Message", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleChannelIdValidationGetChannelMessageReplies(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Channel Message ID: Text, Channel ID: Text, Team ID: Text}") Map<String, Object> map) {

        LOGGER.debug("handleChannelIdValidationGetChannelMessageReplies() - Starting with input map: {}", map);
        LOGGER.debug("Map keys: {}", map.keySet());

        String channelMessageId = getStringFromMap(map, TeamsResources.CHANNEL_MESSAGE_ID);
        String channelID = getStringFromMap(map, TeamsResources.CHANNEL_ID);
        String teamID = getStringFromMap(map, TeamsResources.TEAM_ID);
        String stateID = getStringFromMap(map, TeamsResources.STATE_ID);

        LOGGER.debug("handleChannelIdValidationGetChannelMessageReplies() - Extracted parameters: channelMessageId={}, channelId={}, teamId={}, stateId={}",
                channelMessageId, channelID, teamID, stateID);

        // Add debug logging to verify map contents
        LOGGER.debug("Map contents: {}", map);

        // Validate the re-entered data - validate Channel Message ID, Channel ID, and Team ID
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.CHANNEL_MESSAGE_ID_FOR_REPLIES, channelMessageId != null ? channelMessageId : "",
                        Validator.ValidationResource.CHANNEL_ID, channelID != null ? channelID : "",
                        Validator.ValidationResource.TEAM_ID, teamID != null ? teamID : ""
                )
        );

        LOGGER.debug("handleChannelIdValidationGetChannelMessageReplies() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided Channel Message ID, Channel ID, and Team ID are still invalid. Please try again with a new request.")
                    .build();
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        // Call the main method with corrected data
        LOGGER.info("Calling getChannelMessageRepliesWithValidation with corrected data");
        ExtensionResponse response = getChannelMessageRepliesHelper.getChannelMessageRepliesWithValidation(channelMessageId, channelID, teamID, null);
        return response;
    }

    // ========== GET CHANNELS OF TEAM BY TEAM ID - TEAM ID VALIDATION SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_TEAM_ID_VALIDATION_GET_CHANNELS_OF_TEAM,
            description = "Checks if user wants to re-enter Team ID for Get Channels of Team by Team ID",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmTeamIdValidationGetChannelsOfTeam(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Team ID: Text }",
            required = true) Map<String, Object> map) {

        LOGGER.debug("confirmTeamIdValidationGetChannelsOfTeam() - Starting with input map: {}", map);

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("confirmTeamIdValidationGetChannelsOfTeam() - Reenter: {}, stateId: {}", reenter, stateId);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmTeamIdValidationGetChannelsOfTeam() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            LOGGER.info("User chose not to re-enter data");
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        LOGGER.info("User chose to re-enter data");
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_TEAM_ID_VALIDATION_GET_CHANNELS_OF_TEAM,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_TEAM_ID_VALIDATION_GET_CHANNELS_OF_TEAM,
            description = "Handle Team ID validation re-entry for Get Channels of Team by Team ID",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Desc(name = "Channels", type = "[ Entity ]", required = false)
    @Field.Text(name = "Response Message", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleTeamIdValidationGetChannelsOfTeam(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Team ID: Text}") Map<String, Object> map) {

        LOGGER.debug("handleTeamIdValidationGetChannelsOfTeam() - Starting with input map: {}", map);

        String teamId = (String) map.get(TeamsResources.TEAM_ID);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        LOGGER.debug("handleTeamIdValidationGetChannelsOfTeam() - Extracted parameters: teamId={}, stateId={}",
                teamId, stateId);

        // Validate the re-entered data - validate Team Id
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(Validator.ValidationResource.TEAM_ID, teamId != null ? teamId : "")
        );

        LOGGER.debug("handleTeamIdValidationGetChannelsOfTeam() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return a permanent error response instead of allowing another retry
        if (!validationResults.isEmpty()) {
            LOGGER.info("Re-entered data validation failed with {} errors. No more retries allowed.", validationResults.size());

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.INPUT_ERROR)
                    .errorMessage("The provided Team ID is still invalid. Please try again with a new request.")
                    .build();

            LOGGER.info("Returning permanent error response after retry attempt");
            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }

        // Call the main method with corrected data
        LOGGER.info("Calling getChannelsOfTeamWithValidation with corrected data");
        ExtensionResponse response = getChannelsOfTeamHelper.getChannelsOfTeamWithValidation(teamId, null);
        LOGGER.info("Received response from helper");

        return response;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private List<ValidationOrchestrator.ValidationResult> getValidationResults(String stateId) {
        LOGGER.debug("getValidationResults() - Starting with stateId: {}", stateId);

        List<ValidationOrchestrator.ValidationResult> results = new java.util.ArrayList<>();

        try {
            // Try to get the raw stored data as a string first
            String storedData = internalStateManager.getRaw(stateId);
            LOGGER.debug("getValidationResults() - Retrieved raw data: {}", storedData != null ? "not null" : "null");

            if (storedData != null) {
                // Try to parse as direct ValidationResult list first (from GetChannelIDHelper)
                try {
                    com.google.gson.reflect.TypeToken<List<ValidationOrchestrator.ValidationResult>> typeToken =
                            new com.google.gson.reflect.TypeToken<List<ValidationOrchestrator.ValidationResult>>() {
                            };
                    results = Constants.GSON.fromJson(storedData, typeToken.getType());
                    LOGGER.debug("getValidationResults() - Successfully parsed as direct ValidationResult list, size: {}", results.size());
                    return results;
                } catch (Exception directParseError) {
                    LOGGER.debug("Direct parse failed, trying wrapped format: {}", directParseError.getMessage());

                    // Try to parse as wrapped format (from GetChatIDHelper with VALIDATION_RESULTS key)
                    try {
                        com.google.gson.reflect.TypeToken<Map<String, List<ValidationOrchestrator.ValidationResult>>> wrappedTypeToken =
                                new com.google.gson.reflect.TypeToken<Map<String, List<ValidationOrchestrator.ValidationResult>>>() {
                                };
                        Map<String, List<ValidationOrchestrator.ValidationResult>> wrappedData = Constants.GSON.fromJson(storedData, wrappedTypeToken.getType());

                        if (wrappedData != null && wrappedData.containsKey(SubCatalogConstants.VALIDATION_RESULTS)) {
                            results = wrappedData.get(SubCatalogConstants.VALIDATION_RESULTS);
                            LOGGER.debug("getValidationResults() - Successfully parsed as wrapped format, size: {}", results.size());
                            return results;
                        }
                    } catch (Exception wrappedParseError) {
                        LOGGER.debug("Wrapped parse also failed: {}", wrappedParseError.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error retrieving validation results for stateId: {}, error: {}", stateId, e.getMessage());
        }

        LOGGER.debug("getValidationResults() retrieved {} validation results", results.size());
        return results;
    }

    private Map<String, Object> addMetaDataToLinkedHashMap(Map<String, Object> map, String stateId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>(map);
        result.put(TeamsResources.STATE_ID, stateId);
        return result;
    }

    /**
     * Helper method to safely extract a string value from a map, handling null cases.
     *
     * @param map The map to extract from
     * @param key The key to look for
     * @return The string value, or empty string if not found or null
     */
    private String getStringFromMap(Map<String, Object> map, String key) {
        if (map == null || key == null) {
            LOGGER.warn("Null map or key provided to getStringFromMap: map={}, key={}", map, key);
            return "";
        }

        Object value = map.get(key);
        if (value == null) {
            // Try case-insensitive search as fallback
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (key.equalsIgnoreCase(entry.getKey())) {
                    LOGGER.debug("Found case-insensitive match for key '{}': '{}'", key, entry.getKey());
                    value = entry.getValue();
                    break;
                }
            }
        }

        return value != null ? value.toString() : "";
    }

    // ========== SEND MESSAGE TO USER - EMAIL VALIDATION SUBCATALOG REQUESTS ==========

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SEND_MESSAGE_TO_USER,
            description = "Checks if user wants to re-enter email and message content for Send Message to User",
            type = CatalogRequest.Type.QUERY_SYSTEM
    )
    public ExtensionResponse confirmEmailValidationSendMessageToUser(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, User Email: Text, Message Content: Text, reuseChatId: Boolean, logConversation: Boolean }",
            required = true) Map<String, Object> map) {

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmEmailValidationSendMessageToUser() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            LOGGER.info("User chose not to re-enter email and message content data for Send Message to User");
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        LOGGER.debug("User chose to re-enter email and message content data, generating fetch response");
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SEND_MESSAGE_TO_USER,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SEND_MESSAGE_TO_USER,
            description = "Handle email and message content validation re-entry for Send Message to User",
            type = CatalogRequest.Type.CHANGE_SYSTEM
    )
    @Field.Text(name = "Status Code", required = false)
    @Field.Text(name = "Message Id", required = false)
    @Field.Text(name = "Chat Id", required = false)
    @Field.Text(name = "Response Message", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleEmailValidationSendMessageToUser(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, User Email: Text, Message Content: Text, reuseChatId: Boolean, logConversation: Boolean}") Map<String, Object> map) {

        String userEmail = (String) map.get(TeamsResources.USER_EMAIL);
        String messageContent = (String) map.get(TeamsResources.MESSAGE_CONTENT);
        Boolean reuseChatId = (Boolean) map.get("reuseChatId");
        Boolean logConversation = (Boolean) map.get("logConversation");
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        // Validate the re-entered email and message content data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.USER_EMAIL_FOR_SEND_MESSAGE, userEmail != null ? userEmail : "",
                        Validator.ValidationResource.MESSAGE_CONTENT_FOR_SEND_MESSAGE, messageContent != null ? messageContent : ""
                )
        );

        LOGGER.debug("handleEmailValidationSendMessageToUser() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return an error response
        if (!validationResults.isEmpty()) {
            LOGGER.info("Re-entered email and/or message content validation failed with {} errors. No more retries allowed.", validationResults.size());

            // Create error response following the Send Message to User format
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("Status Code", "400");
            errorResponse.put("Message Id", "");
            errorResponse.put("Chat Id", "");
            errorResponse.put("Response Message", "The provided email address and/or message content are still invalid. Please try again with a new request.");

            return new ExtensionResponseBuilder()
                    .success(errorResponse)
                    .build();
        }

        // Clear the stored state since validation passed
        internalStateManager.clear(stateId);

        try {
            // Proceed with sending the message using the validated email and message content
            LOGGER.info("Email and message content validation passed, sending message to user");

            // Call the main Send Message to User operation with validated data
            ExtensionResponse response = sendMessageToUserEmailValidationHelper.sendMessageToUserWithEmailValidation(
                    userEmail, messageContent,
                    Boolean.TRUE.equals(reuseChatId),
                    Boolean.TRUE.equals(logConversation), null);

            LOGGER.info("Received response from sendMessageToUserWithValidation");
            return response;

        } catch (Exception cause) {
            LOGGER.error("Error in handleEmailValidationSendMessageToUser for user: {}", userEmail, cause);

            // Create error response following the Send Message to User format
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("Status Code", "500");
            errorResponse.put("Message Id", "");
            errorResponse.put("Chat Id", "");
            errorResponse.put("Response Message", "Error: Failed to send message - " + cause.getMessage());

            return new ExtensionResponseBuilder()
                    .success(errorResponse)
                    .build();
        }
    }

    // ========================================
    // Send HTML Message to User - Subcatalog Requests
    // ========================================

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SEND_HTML_MESSAGE_TO_USER,
            description = "Checks if user wants to re-enter email and HTML message content for Send HTML Message to User",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    public ExtensionResponse confirmSendHTMLMessageToUser(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, User Email: Text, HTML Message Content: Text, reuseChatId: Boolean, logConversation: Boolean }",
            required = true) Map<String, Object> map) {
        LOGGER.info("confirmSendHTMLMessageToUser called");

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmSendHTMLMessageToUser() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            LOGGER.info("User chose not to re-enter email and HTML message content data for Send HTML Message to User");
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        LOGGER.debug("User chose to re-enter email and HTML message content data, generating fetch response");
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SEND_HTML_MESSAGE_TO_USER,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SEND_HTML_MESSAGE_TO_USER,
            description = "Handle email and HTML message content validation re-entry for Send HTML Message to User",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Status Code", required = false)
    @Field.Text(name = "Message Id", required = false)
    @Field.Text(name = "Chat Id", required = false)
    @Field.Text(name = "Response Message", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleSendHTMLMessageToUser(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, User Email: Text, HTML Message Content: Text, reuseChatId: Boolean, logConversation: Boolean}") Map<String, Object> map) {

        String userEmail = (String) map.get(TeamsResources.USER_EMAIL);
        String messageContent = (String) map.get(TeamsResources.HTML_MESSAGE_CONTENT);
        Boolean reuseChatId = (Boolean) map.get("reuseChatId");
        Boolean logConversation = (Boolean) map.get("logConversation");
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        // Validate the re-entered email and HTML message content data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.USER_EMAIL_FOR_SEND_HTML_MESSAGE, userEmail != null ? userEmail : "",
                        Validator.ValidationResource.HTML_MESSAGE_CONTENT_FOR_SEND_MESSAGE, messageContent != null ? messageContent : ""
                )
        );

        LOGGER.debug("handleSendHTMLMessageToUser() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return an error response
        if (!validationResults.isEmpty()) {
            LOGGER.info("Re-entered email and/or HTML message content validation failed with {} errors. No more retries allowed.", validationResults.size());

            // Create error response following the Send HTML Message to User format
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("Status Code", "400");
            errorResponse.put("Message Id", "");
            errorResponse.put("Chat Id", "");
            errorResponse.put("Response Message", "The provided email address and/or HTML message content are still invalid. Please try again with a new request.");

            return new ExtensionResponseBuilder()
                    .success(errorResponse)
                    .build();
        }

        // Clear the stored state since validation passed
        internalStateManager.clear(stateId);

        try {
            // Proceed with sending the HTML message using the validated email and HTML message content
            LOGGER.info("Email and HTML message content validation passed, sending HTML message to user");

            // Call the main Send HTML Message to User operation with validated data
            ExtensionResponse response = sendHTMLMessageToUserValidationHelper.sendHTMLMessageToUserWithEmailValidation(
                    userEmail, messageContent,
                    Boolean.TRUE.equals(reuseChatId),
                    Boolean.TRUE.equals(logConversation), null);

            LOGGER.info("Received response from sendHTMLMessageToUserWithEmailValidation");
            return response;

        } catch (Exception cause) {
            LOGGER.error("Error in handleSendHTMLMessageToUser for user: {}", userEmail, cause);

            // Create error response following the Send HTML Message to User format
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("Status Code", "500");
            errorResponse.put("Message Id", "");
            errorResponse.put("Chat Id", "");
            errorResponse.put("Response Message", "Error: Failed to send HTML message - " + cause.getMessage());

            return new ExtensionResponseBuilder()
                    .success(errorResponse)
                    .build();
        }
    }

    // ========================================
    // Send HTML Message to Teams Channel - Subcatalog Requests
    // ========================================

    @SubCatalogRequest(
            name = SubCatalogConstants.CONFIRM_SEND_HTML_MESSAGE_TO_CHANNEL,
            description = "Checks if user wants to re-enter team name, channel name, and HTML message content for Send HTML Message to Channel",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    public ExtensionResponse confirmSendHTMLMessageToChannel(@Field.Desc(
            name = "inputMap",
            type = "{ Reenter: Boolean, stateId: Text, Team Name: Text, Channel Name: Text, HTML Message Content: Text }",
            required = true) Map<String, Object> map) {
        LOGGER.info("confirmSendHTMLMessageToChannel called");

        Boolean reenter = (Boolean) map.get(SubCatalogConstants.REENTER);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        List<ValidationOrchestrator.ValidationResult> validationResults = getValidationResults(stateId);
        LOGGER.debug("confirmSendHTMLMessageToChannel() - Retrieved {} validation results", validationResults.size());

        if (Boolean.FALSE.equals(reenter)) {
            LOGGER.info("User chose not to re-enter team name, channel name, and HTML message content data");
            internalStateManager.clear(stateId);
            return responseGenerator.generateFetchDenyResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    null,
                    Map.of());
        }

        LOGGER.debug("User chose to re-enter team name, channel name, and HTML message content data, generating fetch response");
        return responseGenerator.generateFetchResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults,
                SubCatalogConstants.HANDLE_SEND_HTML_MESSAGE_TO_CHANNEL,
                addMetaDataToLinkedHashMap(map, stateId));
    }

    @SubCatalogRequest(
            name = SubCatalogConstants.HANDLE_SEND_HTML_MESSAGE_TO_CHANNEL,
            description = "Handle team name, channel name, and HTML message content validation re-entry for Send HTML Message to Channel",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Message Delivered", required = false)
    @Field.Text(name = "Response Message", required = false)
    @SuppressWarnings("unchecked")
    public ExtensionResponse handleSendHTMLMessageToChannel(
            @Field.Desc(name = "inputMap", type = "{stateId: Text, Team Name: Text, Channel Name: Text, HTML Message Content: Text}") Map<String, Object> map) {

        String teamName = (String) map.get(TeamsResources.TEAM_NAME);
        String channelName = (String) map.get(TeamsResources.CHANNEL_NAME);
        String message = (String) map.get(TeamsResources.HTML_MESSAGE_CONTENT_FOR_CHANNEL);
        String stateId = (String) map.get(TeamsResources.STATE_ID);

        // Validate the re-entered team name, channel name, and HTML message content data
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.TEAM_NAME_FOR_SEND_HTML_MESSAGE, teamName != null ? teamName : "",
                        Validator.ValidationResource.CHANNEL_NAME_FOR_SEND_HTML_MESSAGE, channelName != null ? channelName : "",
                        Validator.ValidationResource.HTML_MESSAGE_CONTENT_FOR_CHANNEL, message != null ? message : ""
                )
        );

        LOGGER.debug("handleSendHTMLMessageToChannel() - Validation results: {} errors", validationResults.size());

        // If validation still fails, return an error response
        if (!validationResults.isEmpty()) {
            LOGGER.info("Re-entered team name, channel name, and/or HTML message content validation failed with {} errors. No more retries allowed.", validationResults.size());

            String errorMessage = "Validation failed for team '" + teamName + "' and channel '" + channelName + "' after retry";
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Is Message Delivered", false,
                            "Response Message", errorMessage))
                    .build();
        }

        // Clear the stored state since validation passed
        internalStateManager.clear(stateId);

        try {
            // Proceed with sending the HTML message using the validated data
            LOGGER.info("Team name, channel name, and HTML message content validation passed, sending HTML message to channel");

            // Call the main Send HTML Message to Channel operation with validated data
            ExtensionResponse response = sendHTMLMessageToChannelValidationHelper.sendHTMLMessageToChannelWithValidation(
                    teamName, channelName, message, null);

            LOGGER.info("Received response from sendHTMLMessageToChannelWithValidation");
            return response;

        } catch (Exception cause) {
            LOGGER.error("Error in handleSendHTMLMessageToChannel for team: {}, channel: {}", teamName, channelName, cause);

            String errorMessage = "Error sending HTML message to channel '" + channelName + "' in team '" + teamName + "': " + cause.getMessage();
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Is Message Delivered", false,
                            "Response Message", errorMessage))
                    .build();
        }
    }
}
