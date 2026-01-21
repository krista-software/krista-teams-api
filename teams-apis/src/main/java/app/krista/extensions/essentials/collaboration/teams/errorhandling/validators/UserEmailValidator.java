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
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Validator for Email validation for Send Message to User operations.
 * Validates that the provided email exists in Microsoft Teams and is accessible.
 */
@Singleton
@Service
public class UserEmailValidator extends BaseTeamsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserEmailValidator.class);

    @Inject
    public UserEmailValidator(ChatService chatService) {
        super(TeamsResources.USER_EMAIL, FieldTypes.TEXT_FIELD, chatService);
        LOGGER.info("UserEmailValidator constructor called with ChatService");
    }

    @Override
    protected boolean isValidFormat(String trimmedValue) {
        // Email format validation using the base class EMAIL_PATTERN
        return isEmail(trimmedValue);
    }

    @Override
    protected ValidationResult performBusinessValidation(String trimmedValue, Map<ValidationResource, String> context) {
        // Check if user exists in Microsoft Teams
        if (userExistsInTeams(trimmedValue)) {
            return success();
        } else {
            return failure("User not found with email: " + trimmedValue);
        }
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid email address that exists in Microsoft Teams.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "Please enter a valid email address that exists in Microsoft Teams.";
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid email format: '" + value + "'. Please enter a valid email address.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        return "Email '" + value + "' not found in Microsoft Teams or not accessible.";
    }
}
