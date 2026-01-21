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

import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Validator for Group Name validation.
 * Validates that the provided group name exists and is accessible.
 */
public class GroupNameValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupNameValidator.class);

    private final ChatService chatService;

    @Inject
    public GroupNameValidator(ChatService chatService) {
        this.chatService = chatService;
        LOGGER.info("GroupNameValidator constructor called");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("GroupNameValidator.validate() called with groupName: {}", resourceId);

        // Check if group name is null or empty
        if (resourceId == null || resourceId.trim().isEmpty()) {
            LOGGER.warn("Group name validation failed: null or empty");
            return false;
        }

        // Check if group name is too short (less than 2 characters)
        if (resourceId.trim().length() < 2) {
            LOGGER.warn("Group name validation failed: too short - {}", resourceId.trim().length());
            return false;
        }

        try {
            // Try to find the group chat by name
            var group = chatService.getGroupChatByName(resourceId.trim());

            if (group == null) {
                LOGGER.warn("Group name validation failed: group not found - {}", resourceId);
                return false;
            }

            LOGGER.info("Group name validation passed for: {}", resourceId);
            return true;

        } catch (Exception e) {
            LOGGER.error("Error during group name validation for '{}': {}", resourceId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        LOGGER.trace("GroupNameValidator.getFetchFieldName() called");
        return "Group Name";
    }

    @Override
    public String getFieldType() {
        LOGGER.trace("GroupNameValidator.getFieldType() called");
        return "com.krista.fields.Text";
    }

    @Override
    public String getFetchStepMessage() {
        LOGGER.trace("GroupNameValidator.getFetchStepMessage() called");
        return "Please enter a valid Group Name that you have access to in your Teams environment.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.trace("GroupNameValidator.getConfirmationStepMessage() called for groupName: {}", resourceId);
        return "The group name you entered is either invalid or you are not authorized to access it. Please enter a valid group name.";
    }

    @Override
    public String getErrMessage(String resourceId) {
        LOGGER.trace("GroupNameValidator.getErrMessage() called for groupName: {}", resourceId);
        return String.format("The group name '%s' is either invalid or you are not authorized to access it. Please enter a valid group name.", resourceId);
    }
}
