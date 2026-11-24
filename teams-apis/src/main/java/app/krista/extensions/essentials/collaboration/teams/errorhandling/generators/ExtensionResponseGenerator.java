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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.generators;

import app.krista.extension.executor.ExtensionResponse;


import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.SubCatalogConstants;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.factories.ExtensionResponseFactory;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.factories.NamedFieldFactory;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.factories.RemediationActionFactory;

import app.krista.extensions.essentials.collaboration.teams.errorhandling.validators.ValidationOrchestrator;
import app.krista.model.field.NamedField;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class ExtensionResponseGenerator {

    public static final String ERROR_MESSAGE = "ErrorMessage";
    public static final String STEP_MESSAGE = "StepMessage";
    public static final String FIELD = "Field";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionResponseGenerator.class);

    public ExtensionResponse generateConfirmationResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("ExtensionResponseGenerator.generateConfirmationResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        List<NamedField> fields = List.of(NamedFieldFactory.createSwitchField(SubCatalogConstants.REENTER));
        Map<String, Object> stringStringMap = generateResponse(validationResults, false);

        // Use the new askAPersonToReenter method for better user experience
        String confirmationMessage = "Input validation failed. Would you like to re-enter the correct values?";
        ExtensionResponse response = ExtensionResponseFactory.create(confirmationMessage, exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), fields)),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateConfirmationResponse() completed successfully - Confirmation response generated");

        return response;
    }

    private Map<String, Object> generateResponse(List<ValidationOrchestrator.ValidationResult> validationResults, boolean fetchResponse) {
        StringBuilder stepMessage = new StringBuilder();
        StringBuilder errMessage = new StringBuilder();
        List<NamedField> fields = new ArrayList<>();
        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            stepMessage.append(!fetchResponse ? validationResult.getConfirmStepMessage() : validationResult.getFetchStepMessage()).append("\n");
            errMessage.append(validationResult.getErrMessage()).append("\n");
            if (fetchResponse) {
                fields.add(NamedFieldFactory.createField(validationResult.getFetchFieldName(), validationResult.getFieldType()));
            }
        }
        return Map.of(STEP_MESSAGE, stepMessage.toString(), ERROR_MESSAGE, errMessage.toString(), FIELD, fields);
    }

    @SuppressWarnings("unchecked")
    public ExtensionResponse generateFetchResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state) {

        LOGGER.info("ExtensionResponseGenerator.generateFetchResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        Map<String, Object> stringStringMap = generateResponse(validationResults, true);

        // Use a user-friendly message for fetch responses
        String fetchMessage = "Please provide the correct values for the highlighted fields.";
        ExtensionResponse response = ExtensionResponseFactory.create(fetchMessage, exceptionType,
                List.of(RemediationActionFactory.createAskAction((String) stringStringMap.get(STEP_MESSAGE), (List<NamedField>) stringStringMap.get(FIELD))),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateFetchResponse() completed successfully - Fetch response generated");

        return response;
    }

    public ExtensionResponse generateFetchDenyResponse(
            ExtensionResponse.Error.ExceptionType exceptionType,
            List<ValidationOrchestrator.ValidationResult> validationResults,
            String subCatalogRequestName,
            Map<String, Object> state
    ) {
        LOGGER.info("ExtensionResponseGenerator.generateFetchDenyResponse() called with {} validation results for subCatalogRequest: {}",
                validationResults.size(), subCatalogRequestName);

        // Build detailed error message with actual validation errors
        StringBuilder detailedMessage = new StringBuilder();
        detailedMessage.append("Validation failed:\n\n");

        for (ValidationOrchestrator.ValidationResult validationResult : validationResults) {
            String fieldName = validationResult.getFetchFieldName();
            String errorMessage = validationResult.getErrMessage();

            // Add field name and error message
            detailedMessage.append("• ").append(fieldName).append(": ").append(errorMessage).append("\n");
        }

        detailedMessage.append("\nPlease correct the errors and try again.");

        // Use the detailed validation error message
        String denyMessage = "Operation cancelled. No values were updated.";
        ExtensionResponse response = ExtensionResponseFactory.create(denyMessage, exceptionType,
                List.of(RemediationActionFactory.createInformActionALLParticipants(detailedMessage.toString(), List.of())),
                subCatalogRequestName, state);

        LOGGER.debug("ExtensionResponseGenerator.generateFetchDenyResponse() completed successfully - Deny response generated with detailed errors");

        return response;
    }
}

