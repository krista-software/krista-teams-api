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

package app.krista.extensions.essentials.collaboration.teams.impl.connectors;

import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Covers the whole class: the constructor's delegation to
 * {@link app.krista.sdk.msauth.extension.service.MsAuthGraphClientProvider} and the
 * {@link GraphServiceClientProvider#getTeamsAttributes()} typed accessor.
 */
class GraphServiceClientProviderTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";

    @Test
    void getTeamsAttributesReturnsExactAttributesPassedToConstructor() {
        TeamsApisAttributes attrs = new TeamsApisAttributes(
                "client-id", "secret", "tenant-id", "user@example.com", Constants.PUBLIC, ROUTING);

        GraphServiceClientProvider provider = new GraphServiceClientProvider(
                mock(KeyValueStore.class), attrs, mock(RequestContext.class),
                mock(AuthorizationContext.class), "auth-context-id");

        assertSame(attrs, provider.getTeamsAttributes());
    }

    @Test
    void constructsForPrivateAuthTypeToo() {
        TeamsApisAttributes attrs = new TeamsApisAttributes(
                "client-id", "secret", "tenant-id", "user@example.com", Constants.PRIVATE, ROUTING);

        GraphServiceClientProvider provider = new GraphServiceClientProvider(
                mock(KeyValueStore.class), attrs, mock(RequestContext.class),
                mock(AuthorizationContext.class), null);

        assertSame(attrs, provider.getTeamsAttributes());
    }
}
