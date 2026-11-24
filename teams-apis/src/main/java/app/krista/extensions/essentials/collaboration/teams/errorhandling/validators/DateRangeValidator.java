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
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Validator for date range inputs.
 * Validates that:
 * 1. From Date is not in the future
 * 2. From Date is less than or equal to To Date
 */
@Service
public class DateRangeValidator extends BaseValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateRangeValidator.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Creates a new DateRangeValidator.
     */
    public DateRangeValidator() {
        super("Date Range");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        LOGGER.debug("DateRangeValidator.validate() called with context: {}", context);

        try {
            String fromDateStr = context.get(ValidationResource.FROM_DATE);
            String toDateStr = context.get(ValidationResource.TO_DATE);

            // If no dates provided (null or empty), validation passes
            if ((fromDateStr == null || fromDateStr.trim().isEmpty()) &&
                    (toDateStr == null || toDateStr.trim().isEmpty())) {
                return true;
            }

            // Parse dates if provided (handle empty strings as null)
            Long fromDate = (fromDateStr != null && !fromDateStr.trim().isEmpty()) ? Long.parseLong(fromDateStr) : null;

            // If toDate is not provided (null or empty), use current date as default
            Long toDate;
            if (toDateStr == null || toDateStr.trim().isEmpty()) {
                toDate = System.currentTimeMillis();
            } else {
                toDate = Long.parseLong(toDateStr);
            }

            // Current time for future date check
            long currentTime = System.currentTimeMillis();

            // Check if From Date is in the future
            if (fromDate != null && fromDate > currentTime) {
                return false;
            }

            // Check if From Date is after To Date
            if (fromDate != null && toDate != null && fromDate > toDate) {
                return false;
            }

            System.out.println("DateRangeValidator.validate() - Date range validation passed");
            return true;
        } catch (Exception e) {
            LOGGER.error("Error validating date range: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return TeamsResources.FROM_DATE;
    }

    @Override
    public String getFieldType() {
        return FieldTypes.DATE_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid date range. From Date must not be in the future and must be before or equal to To Date.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        return "Date Range is not Valid. Please correct the date range.";
    }

    @Override
    public String getErrMessage(String resourceId) {
        return "Invalid Date Range. From Date must not be in the future and must be before or equal to To Date.";
    }

    /**
     * Format a timestamp as a readable date string.
     *
     * @param timestamp The timestamp in milliseconds
     * @return A formatted date string
     */
    private String formatDate(Long timestamp) {
        if (timestamp == null) {
            return "null";
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return dateTime.format(DATE_FORMATTER);
    }
}
