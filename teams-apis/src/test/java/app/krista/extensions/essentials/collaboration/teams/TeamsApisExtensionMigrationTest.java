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

package app.krista.extensions.essentials.collaboration.teams;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.sdk.msauth.MicrosoftAuthSdk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the {@code INVOKER_LOADED} legacy-attribute migration in
 * {@link TeamsApisExtension#load()}.
 *
 * <p>The pre-3.0 extension used a delegated auth-code flow with the customer's own
 * Azure app (user sign-in), so legacy credentials must be remapped to
 * {@code authType=Public, publicAuthMode=own} — never to {@code Private}
 * (app-only client credentials), which would silently change call semantics.</p>
 */
class TeamsApisExtensionMigrationTest {

    private static final String ROUTING_URL = "https://appliance.example.com/extension/api/x";
    private static final String INVOKER_ID = "invoker-1";

    private Invoker invoker;
    private TeamsApisAttributeStore attributeStore;
    private AuthorizationContext authorizationContext;
    private GraphServiceClientProviderFactory providerFactory;
    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        invoker = mock(Invoker.class, RETURNS_DEEP_STUBS);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);
        when(invoker.getRoutingInfo().getRoutingURL(anyString(), any(RoutingInfo.Type.class)))
                .thenReturn(ROUTING_URL);
        attributeStore = mock(TeamsApisAttributeStore.class);
        authorizationContext = mock(AuthorizationContext.class, RETURNS_DEEP_STUBS);
        providerFactory = mock(GraphServiceClientProviderFactory.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
    }

    private Map<String, Object> legacyAttributes() {
        Map<String, Object> legacy = new HashMap<>();
        legacy.put(Constants.CLIENT_ID, "legacy-client-id");
        legacy.put(Constants.CLIENT_SECRET, "legacy-client-secret");
        legacy.put(Constants.TENANT_ID, "legacy-tenant-id");
        legacy.put(Constants.EMAIL, "legacy-user@example.com");
        return legacy;
    }

    @Test
    void migratesLegacyAttributesToPublicOwnWhenStoreEmpty() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        when(invoker.getAttributes()).thenReturn(legacyAttributes());

        TeamsApisExtension extension = new TeamsApisExtension(invoker, attributeStore, authorizationContext, providerFactory, refreshTokenStore);
        extension.load();

        org.mockito.ArgumentCaptor<TeamsApisAttributes> captor =
                org.mockito.ArgumentCaptor.forClass(TeamsApisAttributes.class);
        verify(attributeStore, times(1)).save(captor.capture(), eq(INVOKER_ID));

        TeamsApisAttributes saved = captor.getValue();
        assertEquals(Constants.PUBLIC, saved.getAuthType());
        assertEquals("own", saved.getPublicAuthMode());
        assertEquals("legacy-client-id", saved.getClientId());
        assertEquals("legacy-tenant-id", saved.getTenantId());
        assertEquals("legacy-user@example.com", saved.getEmail());
    }

    @Test
    void doesNotMigrateWhenStoreAlreadyPopulated() {
        TeamsApisAttributes existing = new TeamsApisAttributes("cid", "cs", "tid", "a@b.com",
                Constants.PUBLIC, ROUTING_URL, "own", null);
        when(attributeStore.load(INVOKER_ID)).thenReturn(existing);

        TeamsApisExtension extension = new TeamsApisExtension(invoker, attributeStore, authorizationContext, providerFactory, refreshTokenStore);
        extension.load();

        verify(attributeStore, never()).save(any(TeamsApisAttributes.class), anyString());
    }

    @Test
    void doesNotMigrateWhenLegacyClientSecretMissing() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        Map<String, Object> legacy = legacyAttributes();
        legacy.remove(Constants.CLIENT_SECRET);
        when(invoker.getAttributes()).thenReturn(legacy);

        TeamsApisExtension extension = new TeamsApisExtension(invoker, attributeStore, authorizationContext, providerFactory, refreshTokenStore);
        extension.load();

        verify(attributeStore, never()).save(any(TeamsApisAttributes.class), anyString());
    }

    @Test
    void reKeysLegacyRefreshTokenToSdkKeyOnMigration() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        when(invoker.getAttributes()).thenReturn(legacyAttributes());
        // Old 2.0.9 stored the refresh token under the bare email.
        when(refreshTokenStore.get("legacy-user@example.com")).thenReturn("legacy-refresh-token");

        new TeamsApisExtension(invoker, attributeStore, authorizationContext, providerFactory, refreshTokenStore).load();

        // It must be copied to the SDK composite key so the migrated connection needs no re-auth.
        String expectedKey = MicrosoftAuthSdk.buildTokenKey(
                "legacy-user@example.com", "legacy-client-id", Constants.PUBLIC);
        verify(refreshTokenStore).put(expectedKey, "legacy-refresh-token");
    }

    @Test
    void doesNotReKeyWhenNoLegacyRefreshToken() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        when(invoker.getAttributes()).thenReturn(legacyAttributes());
        when(refreshTokenStore.get("legacy-user@example.com")).thenReturn(null);

        new TeamsApisExtension(invoker, attributeStore, authorizationContext, providerFactory, refreshTokenStore).load();

        verify(refreshTokenStore, never()).put(anyString(), anyString());
    }
}
