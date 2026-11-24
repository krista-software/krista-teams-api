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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Base class for validators that provides common functionality.
 * Extend this class to create validators for specific input types.
 */
public abstract class BaseValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseValidator.class);

    /**
     * The field name for this validator.
     */
    protected final String fieldName;

    /**
     * The field type for this validator.
     */
    protected final String fieldType;

    /**
     * Creates a new BaseValidator with the given field name and type.
     *
     * @param fieldName The field name
     * @param fieldType The field type
     */
    protected BaseValidator(String fieldName, String fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    /**
     * Creates a new BaseValidator with the given field name and text type.
     *
     * @param fieldName The field name
     */
    protected BaseValidator(String fieldName) {
        this(fieldName, FieldTypes.TEXT_FIELD);
    }

    @Override
    public String getFetchFieldName() {
        return fieldName;
    }

    @Override
    public String getFieldType() {
        return fieldType;
    }

    @Override
    public String getFetchStepMessage() {
        return String.format("Please enter a valid %s.", fieldName);
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return String.format("The provided %s '%s' is invalid.", fieldName, resourceId);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return String.format("Invalid %s: %s", fieldName, resourceId);
    }

    /**
     * Logs an exception and returns false.
     *
     * @param e The exception to log
     * @return false
     */
    protected boolean logExceptionAndReturnFalse(Exception e) {
        LOGGER.error("Exception in validator: {}", e.getMessage(), e);
        return false;
    }
}
