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
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Utility class for processing ExtensionResponse objects and handling telemetry.
 * This class provides reusable methods for response validation, data extraction,
 * and telemetry recording that can be used across different catalog requests.
 */
@Singleton
@Service
public class SendMessageToUserResponseProcessingUtils {

    private static final Logger logger = LoggerFactory.getLogger(SendMessageToUserResponseProcessingUtils.class);
    
    private final TelemetryHelper telemetryHelper;

    @Inject
    public SendMessageToUserResponseProcessingUtils(TelemetryHelper telemetryHelper) {
        this.telemetryHelper = telemetryHelper;
        logger.info("ResponseProcessingUtils initialized successfully");
    }

    /**
     * Checks if the response indicates a validation error.
     *
     * @param response The ExtensionResponse to check
     * @return true if this is a validation error response, false otherwise
     */
    public boolean isValidationErrorResponse(ExtensionResponse response) {
        return response.getRemediationActions() != null &&
               response.getRemediationActions().getSubCatalogRequestName() != null;
    }

    /**
     * Handles validation error responses by recording telemetry and logging.
     *
     * @param response The validation error response
     * @param operationName The name of the operation
     * @param startTime The operation start time
     * @param contextInfo Additional context information for logging (e.g., user email, entity ID)
     * @return The validation error response
     */
    public ExtensionResponse handleValidationError(ExtensionResponse response, String operationName,
                                                  long startTime, String contextInfo) {
        long duration = System.currentTimeMillis() - startTime;
        telemetryHelper.recordOperationFailure(operationName, duration, new RuntimeException("Validation failed"));
        logger.warn("Validation failed for {} with context: {}", operationName, contextInfo);
        return response;
    }

    /**
     * Processes successful responses by extracting data and recording telemetry.
     *
     * @param response The successful response
     * @param operationName The name of the operation
     * @param startTime The operation start time
     * @param contextInfo Additional context information for logging
     * @return The processed response
     */
    public ExtensionResponse processSuccessfulResponse(ExtensionResponse response, String operationName,
                                                      long startTime, String contextInfo) {
        // Extract and validate result data
        Map<String, Object> responseData = extractResponseData(response, operationName, startTime, contextInfo);
        if (responseData == null) {
            return response; // Error already handled in extractResponseData
        }

        // Record telemetry and log results for messaging operations
        if (isMessagingOperation(operationName)) {
            recordMessagingTelemetryAndLogResults(responseData, operationName, startTime, contextInfo);
        } else {
            recordGenericTelemetryAndLogResults(responseData, operationName, startTime, contextInfo);
        }

        return response;
    }

    /**
     * Extracts and validates response data from ExtensionResponse.
     *
     * @param response The ExtensionResponse to extract data from
     * @param operationName The name of the operation
     * @param startTime The operation start time
     * @param contextInfo Additional context information for logging
     * @return The extracted response data as Map, or null if extraction failed
     */
    public Map<String, Object> extractResponseData(ExtensionResponse response, String operationName,
                                                  long startTime, String contextInfo) {
        Object result = response.getResult();
        if (result == null) {
            long duration = System.currentTimeMillis() - startTime;
            telemetryHelper.recordOperationFailure(operationName, duration, new RuntimeException("No result data"));
            logger.error("No result data in response for {} with context: {}", operationName, contextInfo);
            return null;
        }

        if (!(result instanceof Map)) {
            long duration = System.currentTimeMillis() - startTime;
            telemetryHelper.recordOperationFailure(operationName, duration,
                new RuntimeException("Unexpected result type: " + result.getClass()));
            logger.error("Unexpected result type in response for {} with context: {}, type: {}",
                operationName, contextInfo, result.getClass());
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> responseData = (Map<String, Object>) result;
        return responseData;
    }

    /**
     * Records telemetry and logs results for messaging operations based on status code.
     *
     * @param responseData The response data containing status information
     * @param operationName The name of the operation
     * @param startTime The operation start time
     * @param contextInfo Additional context information for logging
     */
    public void recordMessagingTelemetryAndLogResults(Map<String, Object> responseData, String operationName,
                                                     long startTime, String contextInfo) {
        String statusCode = (String) responseData.get("Status Code");
        long duration = System.currentTimeMillis() - startTime;

        if ("200".equals(statusCode) || "201".equals(statusCode)) {
            telemetryHelper.recordOperationSuccess(operationName, duration);
            logger.info("Message successfully processed for {}: {}, messageId: {}, chatId: {}",
                       operationName, contextInfo, responseData.get("Message Id"), responseData.get("Chat Id"));
        } else {
            logger.warn("Message processing failed for {}: {}, status: {}, response: {}",
                       operationName, contextInfo, statusCode, responseData.get("Response Message"));
            telemetryHelper.recordOperationSuccess(operationName, duration);
        }
    }

    /**
     * Records telemetry and logs results for generic operations.
     *
     * @param responseData The response data containing operation results
     * @param operationName The name of the operation
     * @param startTime The operation start time
     * @param contextInfo Additional context information for logging
     */
    public void recordGenericTelemetryAndLogResults(Map<String, Object> responseData, String operationName,
                                                   long startTime, String contextInfo) {
        long duration = System.currentTimeMillis() - startTime;
        
        // For generic operations, we assume success if we have response data
        telemetryHelper.recordOperationSuccess(operationName, duration);
        logger.info("Operation {} completed successfully for context: {}, response data keys: {}",
                   operationName, contextInfo, responseData.keySet());
    }

    /**
     * Determines if an operation is a messaging operation based on the operation name.
     *
     * @param operationName The name of the operation
     * @return true if this is a messaging operation, false otherwise
     */
    private boolean isMessagingOperation(String operationName) {
        return operationName != null && (
            operationName.toLowerCase().contains("message") ||
            operationName.toLowerCase().contains("send") ||
            operationName.toLowerCase().contains("chat")
        );
    }

    /**
     * Records operation failure telemetry and logs the error.
     *
     * @param operationName The name of the operation
     * @param startTime The operation start time
     * @param contextInfo Additional context information for logging
     * @param exception The exception that occurred
     */
    public void recordOperationFailure(String operationName, long startTime, String contextInfo, Exception exception) {
        long duration = System.currentTimeMillis() - startTime;
        logger.error("Error in {} for context: {}", operationName, contextInfo, exception);
        telemetryHelper.recordOperationFailure(operationName, duration, exception);
    }
}
