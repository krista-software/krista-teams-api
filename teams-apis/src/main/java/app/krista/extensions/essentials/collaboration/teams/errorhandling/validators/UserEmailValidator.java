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

    // Store the last validation exception for use in error messages
    private ThreadLocal<Exception> lastValidationException = ThreadLocal.withInitial(() -> null);

    @Inject
    public UserEmailValidator(ChatService chatService) {
        super(TeamsResources.USER_EMAIL, FieldTypes.TEXT_FIELD, chatService);
        LOGGER.info("UserEmailValidator constructor called with ChatService");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("UserEmailValidator.validate() called with email: {}", resourceId);

        // Clear previous exception
        lastValidationException.set(null);

        // Call the parent validate method
        return super.validate(resourceId, context);
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

    /**
     * Builds error message with exception details and troubleshooting information.
     *
     * @param baseMessage The base error message to display
     * @param includeTroubleshooting Whether to include troubleshooting guidance
     * @return Formatted error message with exception details or troubleshooting info
     */
    private String buildErrorMessage(String baseMessage, boolean includeTroubleshooting) {
        StringBuilder message = new StringBuilder();
        Exception lastException = lastValidationException.get();

        if (lastException != null) {
            message.append(baseMessage).append("\n\n");
            message.append("❌ ERROR DETAILS:\n");
            message.append("  • Exception: ").append(lastException.getClass().getSimpleName()).append("\n");
            message.append("  • Message: ").append(lastException.getMessage());
        } else {
            message.append(baseMessage);
            if (includeTroubleshooting) {
                message.append("\n\n").append(getTroubleshootingInfo());
            }
        }
        return message.toString();
    }

    /**
     * Returns standardized troubleshooting information for email validation failures.
     *
     * @return Troubleshooting guidance text
     */
    private String getTroubleshootingInfo() {
        return "⚠️ TROUBLESHOOTING:\n" +
               "If you believe this email is correct, please verify:\n" +
               "  • You are authenticated properly with the Teams API extension\n" +
               "  • Your Azure AD application has the required Microsoft Graph permissions:\n" +
               "    - User.Read.All or User.ReadBasic.All (for reading user information)\n" +
               "    - Chat.ReadWrite or Chat.Create (for sending messages)\n" +
               "  • Admin consent has been granted for these permissions\n" +
               "  • The user exists in your organization's Azure AD tenant\n" +
               "  • The user has access to Microsoft Teams\n\n" +
               "To verify your permissions, check the Azure AD application registration under 'API permissions'.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        String baseMessage = String.format("The provided email address '%s' is invalid or the user does not exist in Microsoft Teams. Please enter a valid email address.", resourceId);
        return buildErrorMessage(baseMessage, true);
    }

    @Override
    protected String getFormatErrorMessage(String value) {
        return "Invalid email format: '" + value + "'. Please enter a valid email address.";
    }

    @Override
    protected String getBusinessLogicErrorMessage(String value) {
        String baseMessage = "Email '" + value + "' not found in Microsoft Teams or not accessible.";
        return buildErrorMessage(baseMessage, true);
    }

    @Override
    public String getErrMessage(String resourceId) {
        String baseMessage = String.format("Invalid email address: %s. Please provide a valid email address that exists in Microsoft Teams.", resourceId);
        return buildErrorMessage(baseMessage, true);
    }
}
