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

import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.sdk.msauth.extension.base.MsAuthAttributes;
import app.krista.sdk.msauth.token.ITokenStore;
import app.krista.sdk.msauth.token.InMemoryTokenStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamsApisRequestAuthenticatorTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/x";

    private static TeamsApisRequestAuthenticator authenticator(TeamsApisAttributeStore store,
                                                                AuthorizationContext authorizationContext) {
        return new TeamsApisRequestAuthenticator(store, "invoker-1", authorizationContext, ROUTING);
    }

    @Test
    void loadAttributesDelegatesToStore() {
        TeamsApisAttributeStore store = mock(TeamsApisAttributeStore.class);
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "a@b.com",
                Constants.PRIVATE, ROUTING);
        when(store.load("invoker-1")).thenReturn(attrs);
        TeamsApisRequestAuthenticator authenticator = authenticator(store, mock(AuthorizationContext.class));
        assertSame(attrs, authenticator.loadAttributesForTest("invoker-1"));
    }

    @Test
    void getTokenStoreReturnsInMemoryTokenStore() {
        TeamsApisRequestAuthenticator authenticator = authenticator(
                mock(TeamsApisAttributeStore.class), mock(AuthorizationContext.class));

        ITokenStore tokenStore = authenticator.getTokenStore();

        assertInstanceOf(InMemoryTokenStore.class, tokenStore);
    }

    @Test
    void createDefaultAttributesReturnsPublicAttributesForRoutingUrl() {
        TeamsApisRequestAuthenticator authenticator = authenticator(
                mock(TeamsApisAttributeStore.class), mock(AuthorizationContext.class));

        MsAuthAttributes defaults = authenticator.createDefaultAttributes(ROUTING);

        assertInstanceOf(TeamsApisAttributes.class, defaults);
        assertEquals(Constants.PUBLIC, defaults.getAuthType());
        assertNull(defaults.getEmail());
    }

    @Test
    void getNotificationPathIsNull() {
        TeamsApisRequestAuthenticator authenticator = authenticator(
                mock(TeamsApisAttributeStore.class), mock(AuthorizationContext.class));

        assertNull(authenticator.getNotificationPath());
    }

    @Test
    void resolveNotificationAccountIdDelegatesToAuthorizationContext() {
        AuthorizationContext authorizationContext = mock(AuthorizationContext.class, RETURNS_DEEP_STUBS);
        when(authorizationContext.getAuthorizedAccount().getAccountId()).thenReturn("account-123");
        TeamsApisRequestAuthenticator authenticator = authenticator(
                mock(TeamsApisAttributeStore.class), authorizationContext);

        assertEquals("account-123", authenticator.resolveNotificationAccountId());
    }
}
