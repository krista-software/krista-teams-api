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

package app.krista.extensions.essentials.collaboration.teams.impl.audit;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.util.KeyValueStore;
import app.krista.sdk.msauth.extension.audit.AuditData;
import app.krista.sdk.msauth.extension.audit.AuditRecord;
import app.krista.sdk.msauth.extension.service.MsAuthAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link TeamsAuditService} is a pure delegate around the SDK's {@link MsAuthAuditService} — every
 * public method just forwards its arguments (and return value, where applicable) to a single
 * {@code delegate} instance constructed internally in the constructor. Since that delegate is
 * built with {@code new MsAuthAuditService(keyValueStore, "teams")} rather than injected, the only
 * way to verify each method's forwarding behavior in isolation (without exercising the real SDK's
 * KV-backed persistence logic) is to intercept that construction with
 * {@link org.mockito.Mockito#mockConstruction}, which requires the inline mock maker (see
 * src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker).
 */
class TeamsAuditServiceTest {

    @Test
    void constructorForwardsKeyValueStoreAndTeamsExtensionId() {
        KeyValueStore keyValueStore = mock(KeyValueStore.class);
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class,
                (delegateMock, context) -> {
                    assertEquals(2, context.arguments().size());
                    assertSame(keyValueStore, context.arguments().get(0));
                    assertEquals("teams", context.arguments().get(1));
                })) {
            new TeamsAuditService(keyValueStore);
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void recordForwardsAllArgumentsToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            Map<String, String> metadata = Map.of("triggeredBy", "user@example.com");
            service.record("invoker-1", "CATALOG_REQUEST_SUCCESS", "success", "Get All Teams",
                    "reason text", "SomeException", metadata);

            verify(delegate).record("invoker-1", "CATALOG_REQUEST_SUCCESS", "success",
                    "Get All Teams", "reason text", "SomeException", metadata);
        }
    }

    @Test
    void recordAuthErrorForwardsToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);
            MustAuthorizeException cause = new MustAuthorizeException("please authorize", new ArrayList<>());

            service.recordAuthError("invoker-1", "Get All Teams", cause);

            verify(delegate).recordAuthError("invoker-1", "Get All Teams", cause);
        }
    }

    @Test
    void getDataForwardsArgumentsAndReturnsDelegateResult() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);
            AuditData expected = mock(AuditData.class);
            when(delegate.getData("invoker-1", 100L, 200L, "CATALOG_REQUEST_SUCCESS", "success"))
                    .thenReturn(expected);

            AuditData actual = service.getData("invoker-1", 100L, 200L, "CATALOG_REQUEST_SUCCESS", "success");

            assertSame(expected, actual);
        }
    }

    @Test
    void resetAuthCountersForwardsToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            service.resetAuthCounters();

            verify(delegate).resetAuthCounters();
        }
    }

    @Test
    void clearRecordsForwardsArgumentsToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            service.clearRecords("invoker-1", 100L, 200L);

            verify(delegate).clearRecords("invoker-1", 100L, 200L);
        }
    }

    @Test
    void startRequestForwardsToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            service.startRequest();

            verify(delegate).startRequest();
        }
    }

    @Test
    void getGlobalCounterReturnsDelegateResult() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);
            Map<String, Object> expected = Map.of("totalRequests", 42);
            when(delegate.getGlobalCounter()).thenReturn(expected);

            Map<String, Object> actual = service.getGlobalCounter();

            assertSame(expected, actual);
        }
    }

    @Test
    void getArchiveIndexReturnsDelegateResult() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);
            List<Map<String, Object>> expected = List.of(Map.of("archiveKey", "archive-1"));
            when(delegate.getArchiveIndex()).thenReturn(expected);

            List<Map<String, Object>> actual = service.getArchiveIndex();

            assertSame(expected, actual);
        }
    }

    @Test
    void getArchivedRecordsForwardsKeyAndReturnsDelegateResult() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);
            List<AuditRecord> expected = List.of(new AuditRecord());
            when(delegate.getArchivedRecords("archive-1")).thenReturn(expected);

            List<AuditRecord> actual = service.getArchivedRecords("archive-1");

            assertSame(expected, actual);
        }
    }

    @Test
    void deleteArchiveForwardsKeyToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            service.deleteArchive("archive-1");

            verify(delegate).deleteArchive("archive-1");
        }
    }

    @Test
    void archiveAllForwardsToDelegate() {
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            service.archiveAll();

            verify(delegate).archiveAll();
        }
    }

    @Test
    void recordAcceptsNullReasonAndExceptionClassAndMetadata() {
        // Real-world callers (e.g. AuthenticationResource on success) pass null reason/exceptionClass;
        // pins that TeamsAuditService.record does not itself impose non-null constraints.
        try (MockedConstruction<MsAuthAuditService> mocked = mockConstruction(MsAuthAuditService.class)) {
            TeamsAuditService service = new TeamsAuditService(mock(KeyValueStore.class));
            MsAuthAuditService delegate = mocked.constructed().get(0);

            service.record("invoker-1", "CATALOG_REQUEST_SUCCESS", "success", "Get All Teams",
                    null, null, Map.of());

            verify(delegate).record(eq("invoker-1"), eq("CATALOG_REQUEST_SUCCESS"), eq("success"),
                    eq("Get All Teams"), isNull(), isNull(), eq(Map.of()));
        }
    }
}
