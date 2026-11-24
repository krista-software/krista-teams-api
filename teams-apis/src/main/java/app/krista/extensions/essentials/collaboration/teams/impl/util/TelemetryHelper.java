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

import app.krista.extension.executor.Invoker;
import app.krista.extensions.essentials.collaboration.teams.impl.audit.TeamsAuditService;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Helper class for recording telemetry metrics for Teams APIs operations.
 *
 * <p>Provides centralized telemetry functionality for all Teams APIs-related operations,
 * including operation tracking, duration measurement, and error reporting. This class uses
 * the Krista SDK TelemetryMetrics to record metrics that can be monitored and analyzed.</p>
 *
 * <h2>Design Pattern: Helper/Utility Pattern with Dependency Injection</h2>
 * <ul>
 *   <li><b>Centralized Telemetry:</b> Single point for all telemetry recording</li>
 *   <li><b>Automatic Timing:</b> TelemetryOperation inner class handles timing automatically</li>
 *   <li><b>Error Handling:</b> Graceful degradation if telemetry recording fails</li>
 *   <li><b>Singleton:</b> Single instance shared across the application</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Using TelemetryOperation for automatic timing
 * TelemetryOperation op = telemetryHelper.startOperation("getChannelsOfTeam");
 * try {
 *     // Perform operation
 *     op.recordSuccess();
 * } catch (Exception e) {
 *     op.recordFailure(e);
 *     throw e;
 * }
 * </pre>
 *
 * @author Krista Software
 * @version 3.0.0
 * @see TelemetryMetrics
 * @see TelemetryOperation
 * @since 3.0.0
 */
@Singleton
@Service
public class TelemetryHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryHelper.class);

    /**
     * TelemetryMetrics instance for recording metrics
     */
    private final TelemetryMetrics telemetryMetrics;

    /**
     * Audit service used to record CATALOG_REQUEST_* audit events alongside telemetry.
     * May be {@code null} when this helper was constructed via the legacy single-arg
     * constructor, in which case audit recording is a no-op.
     */
    private final TeamsAuditService auditService;

    /**
     * Invoker id associated with the current request, used to attribute audit events.
     * May be {@code null} when this helper was constructed via the legacy single-arg
     * constructor, in which case audit recording is a no-op.
     */
    private final String invokerId;

    /**
     * Constructs a new TelemetryHelper with only telemetry metrics support.
     *
     * <p>This constructor is retained for backward compatibility with existing callers
     * and tests. Audit recording is disabled ({@code auditService}/{@code invokerId}
     * are {@code null}) when this constructor is used.</p>
     *
     * @param telemetryMetrics the telemetry metrics instance for recording metrics, must not be null
     * @throws IllegalArgumentException if telemetryMetrics is null
     */
    public TelemetryHelper(TelemetryMetrics telemetryMetrics) {
        if (telemetryMetrics == null) {
            throw new IllegalArgumentException("telemetryMetrics cannot be null");
        }
        this.telemetryMetrics = telemetryMetrics;
        this.auditService = null;
        this.invokerId = null;
        LOGGER.debug("TelemetryHelper initialized successfully");
    }

    /**
     * Constructs a new TelemetryHelper with telemetry metrics and audit recording support.
     *
     * <p>This constructor is used by the HK2 dependency injection framework. The invoker id
     * is resolved from the injected {@link Invoker} so audit events can be attributed to the
     * calling invoker.</p>
     *
     * @param telemetryMetrics the telemetry metrics instance for recording metrics, must not be null
     * @param auditService     the audit service used to record catalog request audit events
     * @param invoker          the invoker whose id is used to attribute audit events
     * @throws IllegalArgumentException if telemetryMetrics is null
     */
    @Inject
    public TelemetryHelper(TelemetryMetrics telemetryMetrics, TeamsAuditService auditService, Invoker invoker) {
        this(telemetryMetrics, auditService, invoker == null ? null : invoker.getInvokerId());
    }

    /**
     * Constructs a new TelemetryHelper with telemetry metrics and audit recording support.
     *
     * @param telemetryMetrics the telemetry metrics instance for recording metrics, must not be null
     * @param auditService     the audit service used to record catalog request audit events
     * @param invokerId        the invoker id used to attribute audit events
     * @throws IllegalArgumentException if telemetryMetrics is null
     */
    public TelemetryHelper(TelemetryMetrics telemetryMetrics, TeamsAuditService auditService, String invokerId) {
        if (telemetryMetrics == null) {
            throw new IllegalArgumentException("telemetryMetrics cannot be null");
        }
        this.telemetryMetrics = telemetryMetrics;
        this.auditService = auditService;
        this.invokerId = invokerId;
        LOGGER.debug("TelemetryHelper initialized successfully");
    }

    /**
     * Records the start of a Teams APIs operation.
     *
     * <p>This method increments a counter for operation starts. If telemetry recording fails,
     * the error is logged but not propagated to avoid disrupting the main operation.</p>
     *
     * @param operationName the name of the operation being started, must not be null
     */
    public void recordOperationStart(String operationName) {
        try {
            telemetryMetrics.incrementCounter("teams_apis.operation.started." + operationName, 1L);
            LOGGER.debug("Telemetry: Started operation {}", operationName);
        } catch (Exception e) {
            LOGGER.warn("Failed to record telemetry for operation start: {}", e.getMessage());
        }
        audit("CATALOG_REQUEST_TRIGGERED", "info", operationName, null, null);
    }

    /**
     * Records the successful completion of a Teams APIs operation.
     *
     * <p>This method records both the completion count and the operation duration.
     * Duration metrics can be used to track performance over time.</p>
     *
     * @param operationName the name of the operation that completed successfully, must not be null
     * @param durationMs    the duration of the operation in milliseconds, must be non-negative
     */
    public void recordOperationSuccess(String operationName, long durationMs) {
        try {
            telemetryMetrics.incrementCounter("teams_apis.operation.completed." + operationName, 1L);
            telemetryMetrics.incrementCounter("teams_apis.operation.duration." + operationName, durationMs);
            LOGGER.debug("Telemetry: Completed operation {} in {}ms", operationName, durationMs);
        } catch (Exception e) {
            LOGGER.warn("Failed to record telemetry for operation success: {}", e.getMessage());
        }
        audit("CATALOG_REQUEST_SUCCESS", "success", operationName, null, null);
    }

    /**
     * Records the failure of a Teams APIs operation.
     *
     * <p>This method records the failure count, operation duration, and error type.
     * Error type metrics help identify common failure patterns.</p>
     *
     * @param operationName the name of the operation that failed, must not be null
     * @param durationMs    the duration of the operation before failure in milliseconds, must be non-negative
     * @param error         the exception that caused the failure, must not be null
     */
    public void recordOperationFailure(String operationName, long durationMs, Exception error) {
        try {
            telemetryMetrics.incrementCounter("teams_apis.operation.failed." + operationName, 1L);
            telemetryMetrics.incrementCounter("teams_apis.operation.duration." + operationName, durationMs);
            telemetryMetrics.incrementCounter("teams_apis.operation.error." + error.getClass().getSimpleName(), 1L);
            LOGGER.debug("Telemetry: Failed operation {} after {}ms with error: {}", operationName, durationMs, error.getMessage());
        } catch (Exception e) {
            LOGGER.warn("Failed to record telemetry for operation failure: {}", e.getMessage());
        }
        audit("CATALOG_REQUEST_FAILURE", "failure", operationName, error.getMessage(), error.getClass().getSimpleName());
    }

    /**
     * Records the count of messages processed in a Teams APIs operation.
     *
     * <p>This method is useful for tracking the volume of data processed by operations
     * like listing messages or batch operations.</p>
     *
     * @param operationName the name of the operation, must not be null
     * @param count         the number of messages processed, must be non-negative
     */
    public void recordEventCount(String operationName, int count) {
        try {
            telemetryMetrics.incrementCounter("teams_apis.message.count." + operationName, (long) count);
            LOGGER.debug("Telemetry: Recorded message count {} for operation {}", count, operationName);
        } catch (Exception e) {
            LOGGER.warn("Failed to record telemetry for message count: {}", e.getMessage());
        }
    }

    /**
     * Records the count of channels processed in a Teams APIs operation.
     *
     * <p>This method is useful for tracking channel-related operations like
     * retrieving or listing channels.</p>
     *
     * @param operationName the name of the operation, must not be null
     * @param count         the number of channels processed, must be non-negative
     */
    public void recordAttendeeCount(String operationName, int count) {
        try {
            telemetryMetrics.incrementCounter("teams_apis.channel.count." + operationName, (long) count);
            LOGGER.debug("Telemetry: Recorded channel count {} for operation {}", count, operationName);
        } catch (Exception e) {
            LOGGER.warn("Failed to record telemetry for channel count: {}", e.getMessage());
        }
    }

    /**
     * Safely records a catalog request audit event, if audit recording is configured.
     *
     * <p>This method is a no-op when the helper was constructed without an audit service
     * or invoker id (e.g. via the legacy single-arg constructor). Any exception raised by
     * the audit service is caught and logged, never propagated, so audit recording failures
     * cannot disrupt the underlying catalog request.</p>
     *
     * @param eventType     the audit event type, e.g. {@code CATALOG_REQUEST_TRIGGERED}
     * @param status        the status associated with the event, e.g. {@code info}/{@code success}/{@code failure}
     * @param operationName the catalog operation name, used as the audit catalog name
     * @param reason        an optional human-readable reason (e.g. exception message), may be null
     * @param exceptionClass an optional exception class simple name, may be null
     */
    private void audit(String eventType, String status, String operationName, String reason, String exceptionClass) {
        if (auditService == null || invokerId == null) {
            return;
        }
        try {
            auditService.record(invokerId, eventType, status, operationName, reason, exceptionClass, Map.of());
        } catch (Exception e) {
            LOGGER.warn("Audit recording failed for {}: {}", operationName, e.getMessage());
        }
    }

    /**
     * Starts a new telemetry operation tracking.
     *
     * <p>This method creates a TelemetryOperation instance that automatically tracks
     * the operation start time and provides convenient methods for recording success
     * or failure with automatic duration calculation.</p>
     *
     * <h3>Usage Example:</h3>
     * <pre>
     * TelemetryOperation op = telemetryHelper.startOperation("scheduleMeeting");
     * try {
     *     // Perform the operation
     *     EventDetails result = calendarService.scheduleMeeting(...);
     *     op.recordSuccess();
     *     return result;
     * } catch (Exception e) {
     *     op.recordFailure(e);
     *     throw e;
     * }
     * </pre>
     *
     * @param operationName the name of the operation to track, must not be null
     * @return TelemetryOperation instance for tracking this operation, never null
     */
    public TelemetryOperation startOperation(String operationName) {
        return new TelemetryOperation(operationName, this);
    }

    /**
     * Inner class for tracking individual operations with automatic timing.
     *
     * <p>This class provides a convenient way to track operations by automatically
     * recording the start time and calculating duration when the operation completes
     * or fails. It follows the "start-success/failure" pattern for telemetry.</p>
     *
     * <h2>Thread Safety:</h2>
     * This class is NOT thread-safe. Each operation should be tracked by a single thread.
     *
     * @see TelemetryHelper#startOperation(String)
     */
    public static class TelemetryOperation {
        private final String operationName;
        private final long startTime;
        private final TelemetryHelper helper;

        /**
         * Constructs a new TelemetryOperation and records the operation start.
         *
         * @param operationName the name of the operation, must not be null
         * @param helper        the TelemetryHelper instance for recording metrics, must not be null
         */
        public TelemetryOperation(String operationName, TelemetryHelper helper) {
            this.operationName = operationName;
            this.helper = helper;
            this.startTime = System.currentTimeMillis();
            helper.recordOperationStart(operationName);
        }

        /**
         * Records successful completion of the operation.
         *
         * <p>Automatically calculates the duration from the start time and records
         * the success metrics.</p>
         */
        public void recordSuccess() {
            long duration = System.currentTimeMillis() - startTime;
            helper.recordOperationSuccess(operationName, duration);
        }

        /**
         * Records successful completion with event count.
         *
         * <p>This is useful for operations that process multiple events, such as
         * listing or batch operations.</p>
         *
         * @param eventCount the number of events processed, must be non-negative
         */
        public void recordSuccess(int eventCount) {
            recordSuccess();
            helper.recordEventCount(operationName, eventCount);
        }

        /**
         * Records successful completion with attendee count.
         *
         * <p>This is useful for operations that involve attendees, such as
         * adding or removing attendees from events.</p>
         *
         * @param attendeeCount the number of attendees processed, must be non-negative
         */
        public void recordSuccessWithAttendees(int attendeeCount) {
            recordSuccess();
            helper.recordAttendeeCount(operationName, attendeeCount);
        }

        /**
         * Records failure of the operation.
         *
         * <p>Automatically calculates the duration from the start time and records
         * the failure metrics including the error type.</p>
         *
         * @param error the exception that caused the failure, must not be null
         */
        public void recordFailure(Exception error) {
            long duration = System.currentTimeMillis() - startTime;
            helper.recordOperationFailure(operationName, duration, error);
        }
    }
}

