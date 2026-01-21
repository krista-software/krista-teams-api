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

import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for TelemetryHelper.
 * Tests telemetry recording, operation tracking, and error handling.
 */
@DisplayName("TelemetryHelper Test Suite")
class TelemetryHelperTest {

    @Mock
    private TelemetryMetrics telemetryMetrics;

    private TelemetryHelper telemetryHelper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        telemetryHelper = new TelemetryHelper(telemetryMetrics);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when telemetryMetrics is null")
        void testConstructorWithNullTelemetryMetrics() {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new TelemetryHelper(null)
            );

            assertEquals("telemetryMetrics cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should successfully create TelemetryHelper with valid telemetryMetrics")
        void testConstructorWithValidTelemetryMetrics() {
            TelemetryMetrics mockMetrics = mock(TelemetryMetrics.class);

            assertDoesNotThrow(() -> new TelemetryHelper(mockMetrics));
        }
    }

    @Nested
    @DisplayName("Operation Start Recording Tests")
    class OperationStartTests {

        @Test
        @DisplayName("Should record operation start with correct metric name")
        void testRecordOperationStart() {
            String operationName = "testOperation";

            telemetryHelper.recordOperationStart(operationName);

            ArgumentCaptor<String> metricNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> valueCaptor = ArgumentCaptor.forClass(Long.class);

            verify(telemetryMetrics).incrementCounter(metricNameCaptor.capture(), valueCaptor.capture());

            assertEquals("teams_apis.operation.started.testOperation", metricNameCaptor.getValue());
            assertEquals(1L, valueCaptor.getValue());
        }

        @Test
        @DisplayName("Should not throw exception when telemetry recording fails")
        void testRecordOperationStart_TelemetryFailure() {
            doThrow(new RuntimeException("Telemetry service unavailable"))
                    .when(telemetryMetrics).incrementCounter(anyString(), anyLong());

            assertDoesNotThrow(() -> telemetryHelper.recordOperationStart("testOperation"));
        }
    }

    @Nested
    @DisplayName("Operation Success Recording Tests")
    class OperationSuccessTests {

        @Test
        @DisplayName("Should record operation success with duration")
        void testRecordOperationSuccess() {
            String operationName = "testOperation";
            long duration = 1500L;

            telemetryHelper.recordOperationSuccess(operationName, duration);

            ArgumentCaptor<String> metricNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> valueCaptor = ArgumentCaptor.forClass(Long.class);

            verify(telemetryMetrics, times(2)).incrementCounter(metricNameCaptor.capture(), valueCaptor.capture());

            assertEquals("teams_apis.operation.completed.testOperation", metricNameCaptor.getAllValues().get(0));
            assertEquals(1L, valueCaptor.getAllValues().get(0));

            assertEquals("teams_apis.operation.duration.testOperation", metricNameCaptor.getAllValues().get(1));
            assertEquals(1500L, valueCaptor.getAllValues().get(1));
        }

        @Test
        @DisplayName("Should not throw exception when telemetry recording fails")
        void testRecordOperationSuccess_TelemetryFailure() {
            doThrow(new RuntimeException("Telemetry service unavailable"))
                    .when(telemetryMetrics).incrementCounter(anyString(), anyLong());

            assertDoesNotThrow(() -> telemetryHelper.recordOperationSuccess("testOperation", 1000L));
        }
    }

    @Nested
    @DisplayName("Operation Failure Recording Tests")
    class OperationFailureTests {

        @Test
        @DisplayName("Should record operation failure with error type")
        void testRecordOperationFailure() {
            String operationName = "testOperation";
            long duration = 500L;
            Exception error = new IllegalArgumentException("Test error");

            telemetryHelper.recordOperationFailure(operationName, duration, error);

            ArgumentCaptor<String> metricNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> valueCaptor = ArgumentCaptor.forClass(Long.class);

            verify(telemetryMetrics, times(3)).incrementCounter(metricNameCaptor.capture(), valueCaptor.capture());

            assertEquals("teams_apis.operation.failed.testOperation", metricNameCaptor.getAllValues().get(0));
            assertEquals(1L, valueCaptor.getAllValues().get(0));

            assertEquals("teams_apis.operation.duration.testOperation", metricNameCaptor.getAllValues().get(1));
            assertEquals(500L, valueCaptor.getAllValues().get(1));

            assertEquals("teams_apis.operation.error.IllegalArgumentException", metricNameCaptor.getAllValues().get(2));
            assertEquals(1L, valueCaptor.getAllValues().get(2));
        }

        @Test
        @DisplayName("Should handle different exception types")
        void testRecordOperationFailure_DifferentExceptionTypes() {
            Exception runtimeException = new RuntimeException("Runtime error");
            telemetryHelper.recordOperationFailure("op1", 100L, runtimeException);

            Exception nullPointerException = new NullPointerException("Null error");
            telemetryHelper.recordOperationFailure("op2", 200L, nullPointerException);

            verify(telemetryMetrics, atLeastOnce()).incrementCounter(
                    eq("teams_apis.operation.error.RuntimeException"), eq(1L));
            verify(telemetryMetrics, atLeastOnce()).incrementCounter(
                    eq("teams_apis.operation.error.NullPointerException"), eq(1L));
        }

        @Test
        @DisplayName("Should not throw exception when telemetry recording fails")
        void testRecordOperationFailure_TelemetryFailure() {
            doThrow(new RuntimeException("Telemetry service unavailable"))
                    .when(telemetryMetrics).incrementCounter(anyString(), anyLong());

            Exception error = new IllegalStateException("Test error");
            assertDoesNotThrow(() -> telemetryHelper.recordOperationFailure("testOperation", 1000L, error));
        }
    }

    @Nested
    @DisplayName("Event Count Recording Tests")
    class EventCountTests {

        @Test
        @DisplayName("Should record event count")
        void testRecordEventCount() {
            String operationName = "testOperation";
            int count = 42;

            telemetryHelper.recordEventCount(operationName, count);

            verify(telemetryMetrics).incrementCounter("teams_apis.message.count.testOperation", 42L);
        }

        @Test
        @DisplayName("Should handle zero count")
        void testRecordEventCount_ZeroCount() {
            telemetryHelper.recordEventCount("testOperation", 0);

            verify(telemetryMetrics).incrementCounter("teams_apis.message.count.testOperation", 0L);
        }

        @Test
        @DisplayName("Should not throw exception when telemetry recording fails")
        void testRecordEventCount_TelemetryFailure() {
            doThrow(new RuntimeException("Telemetry service unavailable"))
                    .when(telemetryMetrics).incrementCounter(anyString(), anyLong());

            assertDoesNotThrow(() -> telemetryHelper.recordEventCount("testOperation", 10));
        }
    }

    @Nested
    @DisplayName("Attendee Count Recording Tests")
    class AttendeeCountTests {

        @Test
        @DisplayName("Should record attendee count")
        void testRecordAttendeeCount() {
            String operationName = "testOperation";
            int count = 15;

            telemetryHelper.recordAttendeeCount(operationName, count);

            verify(telemetryMetrics).incrementCounter("teams_apis.channel.count.testOperation", 15L);
        }

        @Test
        @DisplayName("Should not throw exception when telemetry recording fails")
        void testRecordAttendeeCount_TelemetryFailure() {
            doThrow(new RuntimeException("Telemetry service unavailable"))
                    .when(telemetryMetrics).incrementCounter(anyString(), anyLong());

            assertDoesNotThrow(() -> telemetryHelper.recordAttendeeCount("testOperation", 5));
        }
    }

    @Nested
    @DisplayName("TelemetryOperation Tests")
    class TelemetryOperationTests {

        @Test
        @DisplayName("Should record operation start when TelemetryOperation is created")
        void testTelemetryOperation_RecordsStart() {
            TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("testOp");

            verify(telemetryMetrics).incrementCounter("teams_apis.operation.started.testOp", 1L);
        }

        @Test
        @DisplayName("Should calculate duration and record success")
        void testTelemetryOperation_RecordSuccess() throws InterruptedException {
            TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("testOp");

            // Small delay to ensure duration > 0
            Thread.sleep(10);

            operation.recordSuccess();

            ArgumentCaptor<String> metricNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> valueCaptor = ArgumentCaptor.forClass(Long.class);

            verify(telemetryMetrics, atLeast(2)).incrementCounter(metricNameCaptor.capture(), valueCaptor.capture());

            // Verify completion was recorded
            assertTrue(metricNameCaptor.getAllValues().contains("teams_apis.operation.completed.testOp"));

            // Verify duration was recorded and is > 0
            int durationIndex = metricNameCaptor.getAllValues().indexOf("teams_apis.operation.duration.testOp");
            assertTrue(durationIndex >= 0);
            assertTrue(valueCaptor.getAllValues().get(durationIndex) >= 0);
        }

        @Test
        @DisplayName("Should record success with event count")
        void testTelemetryOperation_RecordSuccessWithEventCount() {
            TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("testOp");

            operation.recordSuccess(25);

            verify(telemetryMetrics).incrementCounter("teams_apis.message.count.testOp", 25L);
            verify(telemetryMetrics).incrementCounter(eq("teams_apis.operation.completed.testOp"), eq(1L));
        }

        @Test
        @DisplayName("Should record success with attendee count")
        void testTelemetryOperation_RecordSuccessWithAttendees() {
            TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("testOp");

            operation.recordSuccessWithAttendees(10);

            verify(telemetryMetrics).incrementCounter("teams_apis.channel.count.testOp", 10L);
            verify(telemetryMetrics).incrementCounter(eq("teams_apis.operation.completed.testOp"), eq(1L));
        }

        @Test
        @DisplayName("Should calculate duration and record failure")
        void testTelemetryOperation_RecordFailure() throws InterruptedException {
            TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("testOp");

            // Small delay to ensure duration > 0
            Thread.sleep(10);

            Exception error = new RuntimeException("Test error");
            operation.recordFailure(error);

            ArgumentCaptor<String> metricNameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> valueCaptor = ArgumentCaptor.forClass(Long.class);

            verify(telemetryMetrics, atLeast(2)).incrementCounter(metricNameCaptor.capture(), valueCaptor.capture());

            // Verify failure was recorded
            assertTrue(metricNameCaptor.getAllValues().contains("teams_apis.operation.failed.testOp"));

            // Verify error type was recorded
            assertTrue(metricNameCaptor.getAllValues().contains("teams_apis.operation.error.RuntimeException"));

            // Verify duration was recorded and is > 0
            int durationIndex = metricNameCaptor.getAllValues().indexOf("teams_apis.operation.duration.testOp");
            assertTrue(durationIndex >= 0);
            assertTrue(valueCaptor.getAllValues().get(durationIndex) >= 0);
        }
    }
}

