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

import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.FieldTypes;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.ValidationUtils;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validator for Teams Chat ID format.
 * Validates that the Chat ID follows the expected Teams format.
 *
 * Supported formats:
 * - Group chats: 19:[hex]@thread.v2 (e.g., 19:abc123def456@thread.v2)
 * - Meeting chats: 19:meeting_[base64]@thread.v2 (e.g., 19:meeting_MDMwYjRlMWEtYjIyYi00ZjAzLTgxMmYtNGZhN2ZkMDZmNDky@thread.v2)
 * - Compound format: 19:[id]@thread.v2-19:[id]@thread.v2
 */
@Service
public class ChatIdValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatIdValidator.class);

    // Teams Chat ID format: Accepts various valid formats
    // - Group chats: 19:[hex]@thread.v2
    // - Meeting chats: 19:meeting_[base64]@thread.v2
    // - Compound format: 19:[id]@thread.v2-19:[id]@thread.v2
    // The ID part can contain alphanumeric characters, underscores, and hyphens
    private static final Pattern CHAT_ID_PATTERN = Pattern.compile(
            "^19:[a-zA-Z0-9_-]+@thread\\.v2(?:-19:[a-zA-Z0-9_-]+@thread\\.v2)?$"
    );

    /**
     * Creates a new ChatIdValidator.
     */
    public ChatIdValidator() {
        super("Chat ID");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        try {
            // Check if Chat ID is not empty and matches the expected format
            boolean isValid = !ValidationUtils.isStringNullOrBlank(resourceId) &&
                    CHAT_ID_PATTERN.matcher(resourceId).matches();
            return isValid;
        } catch (Exception e) {
            return logExceptionAndReturnFalse(e);
        }
    }

    @Override
    public String getFetchFieldName() {
        return TeamsResources.CHAT_ID;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Chat ID. Supported formats:\n" +
               "  • Group chats: 19:xxxxx@thread.v2\n" +
               "  • Meeting chats: 19:meeting_xxxxx@thread.v2\n" +
               "  • Compound format: 19:xxxxx@thread.v2-19:xxxxx@thread.v2";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided Chat ID '%s' is invalid. Please provide a valid Chat ID in one of these formats:\n" +
                           "  • Group chats: 19:xxxxx@thread.v2\n" +
                           "  • Meeting chats: 19:meeting_xxxxx@thread.v2\n" +
                           "  • Compound format: 19:xxxxx@thread.v2-19:xxxxx@thread.v2", resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid Chat ID: %s. Please provide a valid Chat ID that exists and you have access to.", resourceId);
    }
}