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

package app.krista.extensions.essentials.collaboration.teams.api;

import app.krista.extension.executor.Invoker;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.audit.TeamsAuditService;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.sdk.msauth.MicrosoftAuthSdk;
import app.krista.sdk.msauth.extension.audit.AuditData;
import app.krista.sdk.msauth.extension.audit.AuditRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Covers every method {@link TeamsAuditResource} overrides/declares: the plain delegates
 * (getAuditData, clearAuditRecords, getArchiveIndex, getArchivedRecords, deleteArchiveEntry,
 * getGlobalCounter, archiveAllRecords) and every branch of {@code buildHealthResponse}
 * (no config, Public/Private auth types, present/blank/missing refresh token, load failure,
 * and the auth-event tally loop over audit records).
 */
class TeamsAuditResourceTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";
    private static final String INVOKER_ID = "INV_test";

    private TeamsAuditService auditService;
    private TeamsApisAttributeStore attributeStore;
    private RefreshTokenStore refreshTokenStore;
    private TeamsAuditResource resource;

    @BeforeEach
    void setUp() {
        auditService = mock(TeamsAuditService.class);
        attributeStore = mock(TeamsApisAttributeStore.class);
        refreshTokenStore = mock(RefreshTokenStore.class);

        Invoker invoker = mock(Invoker.class);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);

        resource = new TeamsAuditResource(auditService, attributeStore, refreshTokenStore, invoker);
    }

    private static TeamsApisAttributes attrs(String clientId, String tenantId, String email, String authType) {
        return new TeamsApisAttributes(clientId, "secret", tenantId, email, authType, ROUTING);
    }

    private static AuditRecord record(String eventType, long timestamp) {
        AuditRecord r = new AuditRecord();
        r.eventType = eventType;
        r.timestamp = timestamp;
        return r;
    }

    private void stubAuditData(List<AuditRecord> records) {
        when(auditService.getData(INVOKER_ID, null, null, null, null))
                .thenReturn(new AuditData(null, records));
    }

    // ---------------- plain delegates ----------------

    @Test
    void getAuditDataDelegatesToService() {
        AuditData expected = new AuditData(null, List.of());
        when(auditService.getData(INVOKER_ID, 1L, 2L, "type", "status")).thenReturn(expected);

        AuditData actual = resource.getAuditData(INVOKER_ID, 1L, 2L, "type", "status");

        assertSame(expected, actual);
        verify(auditService).getData(INVOKER_ID, 1L, 2L, "type", "status");
    }

    @Test
    void clearAuditRecordsDelegatesToService() {
        resource.clearAuditRecords(INVOKER_ID, 1L, 2L);

        verify(auditService).clearRecords(INVOKER_ID, 1L, 2L);
    }

    @Test
    void getArchiveIndexDelegatesToService() {
        List<Map<String, Object>> expected = List.of(Map.of("key", "a"));
        when(auditService.getArchiveIndex()).thenReturn(expected);

        assertSame(expected, resource.getArchiveIndex());
    }

    @Test
    void getArchivedRecordsDelegatesToService() {
        List<AuditRecord> expected = List.of(record("TOKEN_REFRESHED", 1L));
        when(auditService.getArchivedRecords("archive-key")).thenReturn(expected);

        assertSame(expected, resource.getArchivedRecords("archive-key"));
    }

    @Test
    void deleteArchiveEntryDelegatesToService() {
        resource.deleteArchiveEntry("archive-key");

        verify(auditService).deleteArchive("archive-key");
    }

    @Test
    void getGlobalCounterDelegatesToService() {
        Map<String, Object> expected = Map.of("total", 5);
        when(auditService.getGlobalCounter()).thenReturn(expected);

        assertSame(expected, resource.getGlobalCounter());
    }

    @Test
    void archiveAllRecordsDelegatesToService() {
        resource.archiveAllRecords();

        verify(auditService).archiveAll();
    }

    // ---------------- buildHealthResponse: credential config branches ----------------

    @Test
    void buildHealthResponseWhenNoConfigStored() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        stubAuditData(List.of());

        Map<String, Object> health = resource.buildHealthResponse();

        assertEquals("not configured", health.get("authType"));
        assertEquals(false, health.get("hasRefreshToken"));
        assertNotNull(health.get("events"));
        verifyNoInteractions(refreshTokenStore);
    }

    @Test
    void buildHealthResponseWhenAttributeStoreThrows() {
        when(attributeStore.load(INVOKER_ID)).thenThrow(new RuntimeException("boom"));
        stubAuditData(List.of());

        Map<String, Object> health = resource.buildHealthResponse();

        assertEquals("error", health.get("authType"));
        assertEquals(false, health.get("hasRefreshToken"));
    }

    @Test
    void buildHealthResponsePrivateAuthTypeSkipsRefreshTokenLookup() {
        TeamsApisAttributes attrs = attrs("client-id-1234567890", "tenant-id-1234567890",
                "user@example.com", Constants.PRIVATE);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        stubAuditData(List.of());

        Map<String, Object> health = resource.buildHealthResponse();

        assertEquals(Constants.PRIVATE, health.get("authType"));
        assertEquals("user@example.com", health.get("email"));
        assertEquals(true, health.get("hasRefreshToken"));
        assertEquals("11/14/2027", health.get("clientSecretExpiry"));
        assertTrue(((String) health.get("tenantId")).endsWith("***"));
        assertTrue(((String) health.get("clientId")).endsWith("***"));
        verifyNoInteractions(refreshTokenStore);
    }

    @Test
    void buildHealthResponsePublicAuthTypeWithPresentRefreshToken() {
        TeamsApisAttributes attrs = attrs("client-id-1234567890", "tenant-id-1234567890",
                "user@example.com", Constants.PUBLIC);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        String compositeKey = MicrosoftAuthSdk.buildTokenKey("user@example.com", "client-id-1234567890", Constants.PUBLIC);
        when(refreshTokenStore.get(compositeKey)).thenReturn("some-refresh-token");
        stubAuditData(List.of());

        Map<String, Object> health = resource.buildHealthResponse();

        assertEquals(Constants.PUBLIC, health.get("authType"));
        assertEquals(true, health.get("hasRefreshToken"));
        verify(refreshTokenStore).get(compositeKey);
    }

    @Test
    void buildHealthResponsePublicAuthTypeWithMissingRefreshToken() {
        TeamsApisAttributes attrs = attrs("client-id-1234567890", "tenant-id-1234567890",
                "user@example.com", Constants.PUBLIC);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        when(refreshTokenStore.get(anyString())).thenReturn(null);
        stubAuditData(List.of());

        Map<String, Object> health = resource.buildHealthResponse();

        assertEquals(false, health.get("hasRefreshToken"));
    }

    @Test
    void buildHealthResponsePublicAuthTypeWithBlankRefreshToken() {
        TeamsApisAttributes attrs = attrs("client-id-1234567890", "tenant-id-1234567890",
                "user@example.com", Constants.PUBLIC);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        when(refreshTokenStore.get(anyString())).thenReturn("");
        stubAuditData(List.of());

        Map<String, Object> health = resource.buildHealthResponse();

        assertEquals(false, health.get("hasRefreshToken"));
    }

    @Test
    void buildHealthResponseFallsBackToDashWhenExpiryPropertiesFailToLoad() {
        TeamsApisAttributes attrs = attrs("client-id-1234567890", "tenant-id-1234567890",
                "user@example.com", Constants.PUBLIC);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        when(refreshTokenStore.get(anyString())).thenReturn("some-refresh-token");
        stubAuditData(List.of());

        try (MockedConstruction<Properties> ignored = mockConstruction(Properties.class,
                (mock, context) -> doThrow(new RuntimeException("boom")).when(mock).load(any(InputStream.class)))) {
            Map<String, Object> health = resource.buildHealthResponse();

            assertEquals("—", health.get("clientSecretExpiry"));
        }
    }

    // ---------------- buildHealthResponse: auth-event tally loop ----------------

    @Test
    @SuppressWarnings("unchecked")
    void buildHealthResponseTalliesKnownAuthEventsAndTracksLatestTimestamp() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        stubAuditData(List.of(
                record("TOKEN_REFRESHED", 100L),
                record("TOKEN_REFRESHED", 200L),           // later timestamp overwrites lastSeen
                record("TOKEN_EXPIRY_DETECTED", 300L),
                record("TOKEN_EXPIRY_DETECTED", 250L),      // earlier timestamp must NOT overwrite
                record("SOME_UNRELATED_EVENT", 999L),       // not a tracked auth event key — ignored
                record(null, 1L)                             // null eventType — must not NPE
        ));

        Map<String, Object> health = resource.buildHealthResponse();

        Map<String, Map<String, Object>> events = (Map<String, Map<String, Object>>) health.get("events");
        assertEquals(6, events.size());

        assertEquals(2, events.get("TOKEN_REFRESHED").get("count"));
        assertEquals(200L, events.get("TOKEN_REFRESHED").get("lastSeen"));

        assertEquals(2, events.get("TOKEN_EXPIRY_DETECTED").get("count"));
        assertEquals(300L, events.get("TOKEN_EXPIRY_DETECTED").get("lastSeen"));

        assertEquals(0, events.get("CLIENT_SECRET_EXPIRY_DETECTED").get("count"));
        assertNull(events.get("CLIENT_SECRET_EXPIRY_DETECTED").get("lastSeen"));
    }
}
