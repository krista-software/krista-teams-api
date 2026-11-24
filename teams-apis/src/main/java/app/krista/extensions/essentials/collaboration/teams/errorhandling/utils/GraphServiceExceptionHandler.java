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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.utils;

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extensions.essentials.collaboration.teams.impl.util.TelemetryHelper;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import com.microsoft.graph.http.GraphServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling GraphServiceException errors related to Microsoft Teams API.
 * Provides centralized error detection and user-friendly error message generation
 * for common Teams API errors, including message-too-large errors and permission errors.
 *
 * <p>This class handles the detection of message size limit errors and permission errors (403)
 * from Microsoft Graph API and provides consistent error handling across all Teams API operations.</p>
 *
 * @author Krista Teams Extension
 * @version 2.0.9
 * @since 2.0.7
 */
public final class GraphServiceExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphServiceExceptionHandler.class);

    /**
     * Microsoft Teams API message limit constant.
     * The Graph API enforces approximately 28,000 characters for messages,
     * even though new Teams clients (2024+) support 80K-100K characters for manual messages.
     */
    public static final int TEAMS_API_MESSAGE_LIMIT = 28000;

    // Private constructor to prevent instantiation
    private GraphServiceExceptionHandler() {
        throw new UnsupportedOperationException("Utility class - do not instantiate");
    }

    /**
     * Checks if a GraphServiceException is due to message being too large.
     * Detects HTTP 400 errors with "InvalidParameter" or "RequestBodyTooLarge" messages.
     *
     * @param gse The GraphServiceException to check
     * @return true if the error is due to message size exceeding Teams API limits, false otherwise
     */
    public static boolean isMessageTooLargeError(GraphServiceException gse) {
        if (gse == null) {
            return false;
        }

        return gse.getResponseCode() == 400 &&
                gse.getServiceError() != null &&
                gse.getServiceError().message != null &&
                (gse.getServiceError().message.contains("InvalidParameter") ||
                 gse.getServiceError().message.contains("RequestBodyTooLarge"));
    }

    /**
     * Creates a user-friendly error message for message-too-large errors.
     *
     * @param targetDescription Description of the target (e.g., "user 'email@example.com'", "group 'GroupName'")
     * @param messageLength The length of the message that was too large
     * @return User-friendly error message
     */
    public static String createMessageTooLargeErrorMessage(String targetDescription, int messageLength) {
        return String.format(
                "Failed to send HTML message to %s. The message is too large (%,d characters). " +
                "Microsoft Teams has a limit of approximately %,d characters for messages. " +
                "Please reduce the message size and try again.",
                targetDescription, messageLength, TEAMS_API_MESSAGE_LIMIT);
    }

    /**
     * Handles GraphServiceException for message-too-large and permission errors, and returns ExtensionResponse.
     * This method is designed for catalog request methods that return ExtensionResponse.
     *
     * <p>Checks if the exception is due to message size exceeding Teams API limits or permission errors (403),
     * and returns a user-friendly error response. For other errors, throws
     * the original GraphServiceException.</p>
     *
     * @param gse The GraphServiceException to handle
     * @param message The message content that was being sent
     * @param targetDescription Description of the target (e.g., "user 'email@example.com'", "group 'GroupName'")
     * @param telemetryConstant The telemetry constant for failure tracking
     * @param operation The telemetry operation to record failure
     * @param telemetryMetrics The telemetry metrics instance
     * @return ExtensionResponse with user-friendly error message if message is too large or permission error
     * @throws GraphServiceException if the error is not related to message size or permissions
     */
    public static ExtensionResponse handleMessageTooLargeError(
            GraphServiceException gse,
            String message,
            String targetDescription,
            String telemetryConstant,
            TelemetryHelper.TelemetryOperation operation,
            TelemetryMetrics telemetryMetrics) {

        operation.recordFailure(gse);
        telemetryMetrics.incrementCounter(telemetryConstant, 1L);

        int messageLength = message != null ? message.length() : 0;
        LOGGER.error("GraphServiceException for {}, HTML message length: {} characters",
                targetDescription, messageLength, gse);

        // Check if error is due to message being too long
        if (isMessageTooLargeError(gse)) {
            String errorMessage = createMessageTooLargeErrorMessage(targetDescription, messageLength);
            LOGGER.warn("Message too large for Teams API: {} characters (limit: ~{})",
                    messageLength, TEAMS_API_MESSAGE_LIMIT);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage(errorMessage)
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
        // For other GraphServiceExceptions, throw as-is
        throw gse;
    }

}

