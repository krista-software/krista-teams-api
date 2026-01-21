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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.validators;

import java.util.Map;

/**
 * Interface for validators that check input parameters.
 * Implement this interface to create validators for specific input types.
 */
public interface Validator {

    /**
     * Enum of validation resources that can be validated.
     * Add your specific validation resources here.
     */
    enum ValidationResource {
        CHANNEL_ID,
        TEAM_ID,
        CHAT_ID,
        FROM_DATE,
        TO_DATE,
        GROUP_NAME,
        CHANNEL_TEAM_LINK,

        CHANNEL_ID_FOR_REPLIES,
        TEAM_ID_FOR_REPLIES,
        CHANNEL_MESSAGE_ID_FOR_REPLIES,
        TEAM_ID_FOR_CHANNELS,

        // Send Message to User validation resources
        USER_EMAIL_FOR_SEND_MESSAGE,
        MESSAGE_CONTENT_FOR_SEND_MESSAGE,

        // Send HTML Message to User validation resources
        USER_EMAIL_FOR_SEND_HTML_MESSAGE,
        HTML_MESSAGE_CONTENT_FOR_SEND_MESSAGE,

        // Send HTML Message to Teams Channel validation resources
        TEAM_NAME_FOR_SEND_HTML_MESSAGE,
        CHANNEL_NAME_FOR_SEND_HTML_MESSAGE,
        HTML_MESSAGE_CONTENT_FOR_CHANNEL
    }

    /**
     * Validates the given resource ID in the given context.
     *
     * @param resourceId The resource ID to validate
     * @param context    The context in which to validate the resource ID
     * @return true if the resource ID is valid, false otherwise
     */
    Boolean validate(String resourceId, Map<ValidationResource, String> context);

    /**
     * Gets the field name for fetching a new value.
     *
     * @return The field name
     */
    String getFetchFieldName();

    /**
     * Gets the field type for the UI.
     *
     * @return The field type
     */
    String getFieldType();

    /**
     * Gets the message to display when fetching a new value.
     *
     * @return The fetch step message
     */
    String getFetchStepMessage();

    /**
     * Gets the message to display when confirming a value.
     *
     * @param resourceId The resource ID being confirmed
     * @param context    The context in which the resource ID is being confirmed
     * @return The confirmation step message
     */
    String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context);

    /**
     * Gets the error message to display for an invalid value.
     *
     * @param resourceId The invalid resource ID
     * @return The error message
     */
    String getErrMessage(String resourceId);
}
