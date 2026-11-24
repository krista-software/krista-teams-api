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

import app.krista.extensions.essentials.collaboration.teams.impl.audit.TeamsAuditService;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelemetryHelperAuditTest {

    @Test
    void operationSuccessRecordsAuditEvent() {
        TelemetryMetrics metrics = mock(TelemetryMetrics.class);
        TeamsAuditService audit = mock(TeamsAuditService.class);
        TelemetryHelper helper = new TelemetryHelper(metrics, audit, "invoker-1");

        TelemetryHelper.TelemetryOperation op = helper.startOperation("getAllTeams");
        op.recordSuccess();

        ArgumentCaptor<String> eventType = ArgumentCaptor.forClass(String.class);
        verify(audit, times(2)).record(eq("invoker-1"), eventType.capture(), anyString(),
                eq("getAllTeams"), any(), any(), anyMap());
        assertEquals("CATALOG_REQUEST_TRIGGERED", eventType.getAllValues().get(0));
        assertEquals("CATALOG_REQUEST_SUCCESS", eventType.getAllValues().get(1));
    }

    @Test
    void auditFailureNeverPropagates() {
        TelemetryMetrics metrics = mock(TelemetryMetrics.class);
        TeamsAuditService audit = mock(TeamsAuditService.class);
        doThrow(new RuntimeException("kv down")).when(audit)
                .record(anyString(), anyString(), anyString(), anyString(), any(), any(), anyMap());
        TelemetryHelper helper = new TelemetryHelper(metrics, audit, "invoker-1");

        TelemetryHelper.TelemetryOperation op = helper.startOperation("getAllTeams");
        op.recordSuccess(); // must not throw
        verify(metrics, atLeastOnce()).incrementCounter(anyString(), anyLong());
    }
}
