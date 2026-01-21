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
import app.krista.extensions.essentials.collaboration.teams.errorhandling.managers.ErrorHandlingStateManager;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.generators.ExtensionResponseGenerator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.Validator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ChannelIdValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ChannelTeamLinkValidator;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.TeamIdValidator;

import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import app.krista.extensions.essentials.collaboration.teams.service.TeamService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.DateRangeValidator;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;

@Service
public class GetChannelIDHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetChannelIDHelper.class);
    private static final String STATE_ID = "stateId";

    private final ValidationOrchestrator validationOrchestrator;
    private final ErrorHandlingStateManager internalStateManager;
    private final ExtensionResponseGenerator responseGenerator;
    private final ChatService chatService;
    private final ChannelService channelService;
    private final TeamService teamService;

    @Inject
    public GetChannelIDHelper(ValidationOrchestrator validationOrchestrator,
                              ErrorHandlingStateManager internalStateManager,
                              ExtensionResponseGenerator responseGenerator,
                              ChatService chatService,
                              ChannelService channelService,
                              TeamService teamService) {
        LOGGER.info("GetChannelIDHelper constructor called - Initializing with dependencies");

        this.validationOrchestrator = validationOrchestrator;
        this.internalStateManager = internalStateManager;
        this.responseGenerator = responseGenerator;
        this.chatService = chatService;
        this.channelService = channelService;
        this.teamService = teamService;

        // Register validators if not already registered
        registerValidators();

        LOGGER.info("GetChannelIDHelper constructor completed - Initialization completed successfully");
    }

    private void registerValidators() {
        try {
            LOGGER.debug("GetChannelIDHelper.registerValidators() - Starting validator registration");

            // Register Channel ID validator with ChannelService dependency
            ChannelIdValidator channelIdValidator = new ChannelIdValidator(this.channelService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.CHANNEL_ID, channelIdValidator);
            LOGGER.debug("Registered ChannelIdValidator for CHANNEL_ID");

            // Register Team ID validator with API validation
            TeamIdValidator teamIdValidator = new TeamIdValidator(this.teamService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.TEAM_ID, teamIdValidator);
            LOGGER.debug("Registered TeamIdValidator for TEAM_ID with API validation");

            // Register Date Range validator
            DateRangeValidator dateRangeValidator = new DateRangeValidator();
            validationOrchestrator.registerValidator(Validator.ValidationResource.FROM_DATE, dateRangeValidator);
            LOGGER.debug("Registered DateRangeValidator for FROM_DATE");

            // Register Channel Team Link validator
            ChannelTeamLinkValidator channelTeamLinkValidator = new ChannelTeamLinkValidator(channelService);
            validationOrchestrator.registerValidator(Validator.ValidationResource.CHANNEL_TEAM_LINK, channelTeamLinkValidator);
            LOGGER.debug("Registered ChannelTeamLinkValidator for CHANNEL_TEAM_LINK");

            LOGGER.info("GetChannelIDHelper.registerValidators() completed successfully - All validators registered");

        } catch (Exception e) {
            LOGGER.error("Error occurred while registering validators: {}", e.getMessage(), e);
        }
    }

    public ExtensionResponse getChannelMessagesWithValidation(String channelID, String teamID, Long fromDate, Long toDate, Boolean allowRetry) {
        try {
            LOGGER.info("GetChannelIDHelper.getChannelMessagesWithValidation() called with channelID={}, teamID={}, fromDate={}, toDate={}, allowRetry={}",
                    channelID, teamID, fromDate, toDate, allowRetry);

            // Step 1: Validate Channel ID and Team ID
            ExtensionResponse channelTeamValidationResponse = validateChannelAndTeamIds(channelID, teamID, allowRetry);
            if (channelTeamValidationResponse != null) {
                return channelTeamValidationResponse;
            }

            // Step 2: Validate Channel-Team link
            ExtensionResponse linkValidationResponse = validateChannelTeamLink(channelID, teamID, allowRetry);
            if (linkValidationResponse != null) {
                return linkValidationResponse;
            }

            // Step 3: Validate date range if provided
            ExtensionResponse dateValidationResponse = validateDateRange(channelID, teamID, fromDate, toDate, allowRetry);
            if (dateValidationResponse != null) {
                return dateValidationResponse;
            }

            // Step 4: Retrieve and return messages
            return retrieveAndReturnMessages(channelID, teamID, fromDate, toDate);
        } catch (Exception cause) {
            LOGGER.error("Error occurred while retrieving channel messages: {}", cause.getMessage(), cause);
            return generateErrorResponse("Error occurred while retrieving channel messages");
        }
    }

    /**
     * Validates the Channel ID and Team ID.
     *
     * @param channelID The Channel ID to validate
     * @param teamID    The Team ID to validate
     * @param allowRetry Whether to allow retry on validation failure
     * @return An ExtensionResponse if validation fails, null if validation passes
     */
    private ExtensionResponse validateChannelAndTeamIds(String channelID, String teamID, Boolean allowRetry) {
        List<ValidationOrchestrator.ValidationResult> validationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.CHANNEL_ID, channelID != null ? channelID : "",
                        Validator.ValidationResource.TEAM_ID, teamID != null ? teamID : ""
                )
        );

        if (!validationResults.isEmpty()) {
            LOGGER.warn("GetChannelIDHelper.validateChannelAndTeamIds() - Channel/Team validation failed with {} errors",
                    validationResults.size());
            return handleValidationFailure(validationResults, channelID, teamID, null, null, allowRetry);
        }

        return null;
    }

    /**
     * Validates that the Channel ID is linked with the Team ID.
     *
     * @param channelID The Channel ID to validate
     * @param teamID    The Team ID to validate
     * @param allowRetry Whether to allow retry on validation failure
     * @return An ExtensionResponse if validation fails, null if validation passes
     */
    private ExtensionResponse validateChannelTeamLink(String channelID, String teamID, Boolean allowRetry) {
        List<ValidationOrchestrator.ValidationResult> linkValidationResults = validationOrchestrator.validate(
                Map.of(
                        Validator.ValidationResource.CHANNEL_TEAM_LINK, "link_check",
                        Validator.ValidationResource.CHANNEL_ID, channelID != null ? channelID : "",
                        Validator.ValidationResource.TEAM_ID, teamID != null ? teamID : ""
                )
        );

        if (!linkValidationResults.isEmpty()) {
            LOGGER.warn("GetChannelIDHelper.validateChannelTeamLink() - Channel-Team link validation failed with {} errors",
                    linkValidationResults.size());
            return handleChannelTeamLinkValidationFailure(linkValidationResults, channelID, teamID, null, null, allowRetry);
        }

        return null;
    }

    /**
     * Validates the date range if dates are provided.
     *
     * @param channelID The Channel ID
     * @param teamID    The Team ID
     * @param fromDate  The start date for message retrieval
     * @param toDate    The end date for message retrieval
     * @param allowRetry Whether to allow retry on validation failure
     * @return An ExtensionResponse if validation fails, null if validation passes
     */
    private ExtensionResponse validateDateRange(String channelID, String teamID, Long fromDate, Long toDate, Boolean allowRetry) {
        if (fromDate == null && toDate == null) {
            return null; // No date validation needed
        }

        Map<Validator.ValidationResource, String> dateContext = new HashMap<>();
        if (fromDate != null) {
            dateContext.put(Validator.ValidationResource.FROM_DATE, fromDate.toString());
        }

        // If toDate is null, set it to current date for validation
        Long effectiveToDate = toDate != null ? toDate : System.currentTimeMillis();
        dateContext.put(Validator.ValidationResource.TO_DATE, effectiveToDate.toString());

        List<ValidationOrchestrator.ValidationResult> dateValidationResults = performDateRangeValidation(dateContext);

        if (!dateValidationResults.isEmpty()) {
            LOGGER.warn("GetChannelIDHelper.validateDateRange() - Date validation failed with {} errors",
                    dateValidationResults.size());
            return handleDateValidationFailure(dateValidationResults, channelID, teamID, fromDate, toDate, allowRetry);
        }

        return null;
    }

    /**
     * Performs the actual date range validation using the DateRangeValidator.
     *
     * @param dateContext The context containing date information
     * @return A list of validation results if validation fails, empty list if validation passes
     */
    private List<ValidationOrchestrator.ValidationResult> performDateRangeValidation(Map<Validator.ValidationResource, String> dateContext) {
        List<ValidationOrchestrator.ValidationResult> dateValidationResults = new ArrayList<>();

        for (Validator validator : validationOrchestrator.getValidators()) {
            if (validator instanceof DateRangeValidator) {
                if (!validator.validate("", dateContext)) {
                    ValidationOrchestrator.ValidationResult result = new ValidationOrchestrator.ValidationResult(
                            validator.getConfirmationStepMessage("", dateContext),
                            validator.getFetchFieldName(),
                            validator.getFetchStepMessage(),
                            validator.getErrMessage(""),
                            validator.getFieldType()
                    );
                    dateValidationResults.add(result);
                }
                break;
            }
        }

        return dateValidationResults;
    }

    /**
     * Retrieves channel messages and builds the response.
     *
     * @param channelID The Channel ID
     * @param teamID    The Team ID
     * @param fromDate  The start date for message retrieval
     * @param toDate    The end date for message retrieval
     * @return An ExtensionResponse containing the retrieved messages or an error message
     */
    private ExtensionResponse retrieveAndReturnMessages(String channelID, String teamID, Long fromDate, Long toDate) {
        LOGGER.debug("All validations passed, proceeding to retrieve channel messages");
        int maxMessagesToProcess = 50;
        List<ChannelMessage> result = chatService.getChannelMessagesWithDateRangeLimit(channelID, teamID, fromDate, toDate, maxMessagesToProcess);

        // Check if no messages were found in the date range
        if (result.isEmpty()) {
            LOGGER.info("No channel messages found in the provided date range for channelID: {}, teamID: {}", channelID, teamID);
            // Return empty list instead of string message to maintain type consistency
            return new ExtensionResponseBuilder()
                    .success(Map.of("Channel Messages", Collections.emptyList()), null, null)
                    .build();
        }

        LOGGER.info("Successfully retrieved {} channel messages", result.size());
        return new ExtensionResponseBuilder()
                .success(Map.of("Channel Messages", result), null, null)
                .build();
    }

    private ExtensionResponse handleValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                      String channelID, String teamID, Long fromDate, Long toDate, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Get Channel Messages");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("GetChannelIDHelper.handleValidationFailure() - Validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, Constants.GSON.toJson(validationResults));
            LOGGER.debug("GetChannelIDHelper.handleValidationFailure() - Generating confirmation response for validation failure");

            // Generate a confirmation response that will prompt the user to re-enter data
            // Create a map that handles null values safely
            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put(STATE_ID, stateId);
            responseData.put(TeamsResources.CHANNEL_ID, channelID);
            responseData.put(TeamsResources.TEAM_ID, teamID);
            responseData.put(TeamsResources.FROM_DATE, fromDate); // Can be null
            responseData.put(TeamsResources.TO_DATE, toDate);     // Can be null

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_REENTER_GET_CHANNEL_MESSAGES,
                    responseData
            );
        }

        LOGGER.info("Allow Retry = false, returning error directly for Get Channel Messages");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }

    private ExtensionResponse handleDateValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                          String channelID, String teamID, Long fromDate, Long toDate, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Date Validation in Get Channel Messages");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("GetChannelIDHelper.handleDateValidationFailure() - Date validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("GetChannelIDHelper.handleDateValidationFailure() - Generating confirmation response for date validation failure");

            // Generate a confirmation response that will prompt the user to re-enter data
            // Create a map that handles null values safely
            Map<String, Object> dateResponseData = new java.util.HashMap<>();
            dateResponseData.put(TeamsResources.STATE_ID, stateId);
            dateResponseData.put(TeamsResources.CHANNEL_ID, channelID);
            dateResponseData.put(TeamsResources.TEAM_ID, teamID);
            dateResponseData.put(TeamsResources.FROM_DATE, fromDate); // Can be null
            dateResponseData.put(TeamsResources.TO_DATE, toDate);     // Can be null

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_DATE_VALIDATION_GET_CHANNEL_MESSAGES,
                    dateResponseData
            );
        }

        LOGGER.info("Allow Retry = false, returning error directly for Date Validation in Get Channel Messages");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }

    private ExtensionResponse handleChannelTeamLinkValidationFailure(List<ValidationOrchestrator.ValidationResult> validationResults,
                                                                     String channelID, String teamID, Long fromDate, Long toDate, Boolean allowRetry) {

        if (Boolean.TRUE.equals(allowRetry)) {
            LOGGER.info("Allow Retry = true, prompting re-entry for Channel-Team Link Validation in Get Channel Messages");

            // Generate a unique state ID to track this validation session
            String stateId = UUID.randomUUID().toString();
            LOGGER.debug("GetChannelIDHelper.handleChannelTeamLinkValidationFailure() - Channel-Team link validation failed, storing stateId: {}", stateId);

            // Store validation results for later retrieval
            internalStateManager.put(stateId, Constants.GSON.toJson(Map.of(SubCatalogConstants.VALIDATION_RESULTS, validationResults)));
            LOGGER.debug("GetChannelIDHelper.handleChannelTeamLinkValidationFailure() - Generating confirmation response for channel-team link validation failure");

            // Generate a confirmation response that will prompt the user to re-enter data
            // Create a map that handles null values safely
            Map<String, Object> linkResponseData = new java.util.HashMap<>();
            linkResponseData.put(TeamsResources.STATE_ID, stateId);
            linkResponseData.put(TeamsResources.CHANNEL_ID, channelID);
            linkResponseData.put(TeamsResources.TEAM_ID, teamID);
            linkResponseData.put(TeamsResources.FROM_DATE, fromDate); // Can be null
            linkResponseData.put(TeamsResources.TO_DATE, toDate);     // Can be null

            return responseGenerator.generateConfirmationResponse(
                    ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                    validationResults,
                    SubCatalogConstants.CONFIRM_CHANNEL_TEAM_LINK_VALIDATION_GET_CHANNEL_MESSAGES,
                    linkResponseData
            );
        }

        LOGGER.info("Allow Retry = false, returning error directly for Channel-Team Link Validation in Get Channel Messages");
        return responseGenerator.generateFetchDenyResponse(
                ExtensionResponse.Error.ExceptionType.INPUT_ERROR,
                validationResults, null, Map.of());
    }

    private ExtensionResponse generateErrorResponse(String message) {
        LOGGER.error("Generating error response: {}", message);
        ExtensionResponse.Error error = new ErrorBuilder()
                .exceptionType(ExtensionResponse.Error.ExceptionType.SYSTEM_ERROR)
                .errorMessage(message)
                .build();
        return new ExtensionResponseBuilder().failure(error).build();
    }

}
