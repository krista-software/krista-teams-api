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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the platform-level {@code TEST_CONNECTION} invoker request in
 * {@link TeamsApisExtension#testConnection()}. It must actually verify the connection —
 * succeed when the invoker is configured and a usable token exists, and otherwise fail with a
 * clear, actionable message — instead of unconditionally throwing.
 */
class TeamsApisExtensionTestConnectionTest {

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

    private TeamsApisExtension extension() {
        return new TeamsApisExtension(invoker, attributeStore, authorizationContext, providerFactory, refreshTokenStore);
    }

    @Test
    void notConfiguredThrowsConfigureMessage() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> extension().testConnection());
        assertTrue(ex.getMessage().toLowerCase().contains("authentication"),
                "message should point the user to the Authentication tab: " + ex.getMessage());
    }

    @Test
    void configuredAndAuthorizedPublicSucceeds() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "user@example.com",
                Constants.PUBLIC, ROUTING_URL, "own", null);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        GraphServiceClientProvider provider = mock(GraphServiceClientProvider.class, RETURNS_DEEP_STUBS);
        when(providerFactory.create(INVOKER_ID)).thenReturn(provider);

        // deep-stubbed provider: me().joinedTeams().buildRequest().get() returns null → success, no throw
        assertDoesNotThrow(() -> extension().testConnection());
    }

    @Test
    void configuredButNoRefreshTokenThrowsAuthorizeMessage() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "user@example.com",
                Constants.PUBLIC, ROUTING_URL, "own", null);
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs);
        GraphServiceClientProvider provider = mock(GraphServiceClientProvider.class, RETURNS_DEEP_STUBS);
        when(providerFactory.create(INVOKER_ID)).thenReturn(provider);
        when(provider.getGraphServiceClientForAdmin())
                .thenThrow(new MustAuthorizeException("No refresh token found. Re-authorization required.",
                        new ArrayList<>()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> extension().testConnection());
        assertTrue(ex.getMessage().toLowerCase().contains("authorize"),
                "message should tell the user to click Authorize: " + ex.getMessage());
    }
}
