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

import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates validation of input parameters.
 * This class manages a set of validators and applies them to input parameters.
 */
@Singleton
@Service
public class ValidationOrchestrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationOrchestrator.class);
    private final Map<Validator.ValidationResource, Validator> validators = new HashMap<>();

    /*
     * Registers a validator for a validation resource.
     *
     * @param resource  The validation resource
     * @param validator The validator
     */
    public void registerValidator(Validator.ValidationResource resource, Validator validator) {
        LOGGER.debug("Registering validator for resource: {}", resource);
        validators.put(resource, validator);
    }

    /**
     * Validates the given resources using the registered validators.
     *
     * @param resources The resources to validate
     * @return A list of validation results, empty if all validations pass
     */
    public List<ValidationResult> validate(Map<Validator.ValidationResource, String> resources) {
        List<ValidationResult> results = new ArrayList<>();
        for (Map.Entry<Validator.ValidationResource, String> entry : resources.entrySet()) {
            Validator validator = validators.get(entry.getKey());
            if (validator == null) {
                LOGGER.warn("No validator registered for resource: {}", entry.getKey());
                continue;
            }

            if (!validator.validate(entry.getValue(), resources)) {
                results.add(new ValidationResult(
                        validator.getConfirmationStepMessage(entry.getValue(), resources),
                        validator.getFetchFieldName(),
                        validator.getFetchStepMessage(),
                        validator.getErrMessage(entry.getValue()),
                        validator.getFieldType()
                ));
            }
        }
        return results;
    }

    /**
     * Result of a validation.
     */
    public static class ValidationResult {
        private final String confirmStepMessage;
        private final String fetchFieldName;
        private final String fetchStepMessage;
        private final String errMessage;
        private final String fieldType;

        /**
         * Creates a new ValidationResult.
         *
         * @param confirmStepMessage The message to display when confirming a value
         * @param fetchFieldName     The field name for fetching a new value
         * @param fetchStepMessage   The message to display when fetching a new value
         * @param errMessage         The error message to display
         * @param fieldType          The field type
         */
        public ValidationResult(String confirmStepMessage, String fetchFieldName, String fetchStepMessage, String errMessage, String fieldType) {
            this.confirmStepMessage = confirmStepMessage;
            this.fetchFieldName = fetchFieldName;
            this.fetchStepMessage = fetchStepMessage;
            this.errMessage = errMessage;
            this.fieldType = fieldType;
        }

        public String getConfirmStepMessage() {
            return confirmStepMessage;
        }

        public String getFetchFieldName() {
            return fetchFieldName;
        }

        public String getFetchStepMessage() {
            return fetchStepMessage;
        }

        public String getErrMessage() {
            return errMessage;
        }

        public String getFieldType() {
            return fieldType;
        }
    }

    /**
     * Gets the list of validators.
     *
     * @return The list of validators
     */
    public List<Validator> getValidators() {
        return new ArrayList<>(validators.values());
    }
}
