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

import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.sdk.msauth.MicrosoftAuthSdk;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers {@link AuthenticationResource#staleTokenKeyToRemove}: when the user switches auth mode
 * (e.g. Quick Setup → own credentials) and saves, the previously-authorized refresh token must be
 * dropped so only the current one is stored. Switching back then requires a fresh Authorize
 * (Microsoft popup) instead of silently reporting "connected" off a stale token.
 */
class AuthenticationResourceTokenSwitchTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";

    private static TeamsApisAttributes attrs(String clientId, String email, String authType) {
        return new TeamsApisAttributes(clientId, "secret", "tenant", email, authType, ROUTING);
    }

    @Test
    void returnsPreviousKeyWhenModeSwitches() {
        // Quick Setup (Krista-managed clientId) → own credentials (customer clientId), same user.
        TeamsApisAttributes previous = attrs("krista-client", "user@example.com", Constants.PUBLIC);
        TeamsApisAttributes current = attrs("own-client", "user@example.com", Constants.PUBLIC);

        String stale = AuthenticationResource.staleTokenKeyToRemove(previous, current);

        assertEquals(MicrosoftAuthSdk.buildTokenKey("user@example.com", "krista-client", Constants.PUBLIC), stale,
                "the previously-authorized token key must be returned for removal");
    }

    @Test
    void returnsNullWhenSameCredentialsReauthorized() {
        TeamsApisAttributes previous = attrs("same-client", "user@example.com", Constants.PUBLIC);
        TeamsApisAttributes current = attrs("same-client", "user@example.com", Constants.PUBLIC);

        assertNull(AuthenticationResource.staleTokenKeyToRemove(previous, current),
                "re-authorizing the same mode must keep the freshly stored token");
    }

    @Test
    void returnsNullWhenNoPreviousConfig() {
        TeamsApisAttributes current = attrs("own-client", "user@example.com", Constants.PUBLIC);
        assertNull(AuthenticationResource.staleTokenKeyToRemove(null, current));
    }

    @Test
    void returnsNullWhenPreviousEmailIsNull() {
        TeamsApisAttributes previous = attrs("krista-client", null, Constants.PUBLIC);
        TeamsApisAttributes current = attrs("own-client", "user@example.com", Constants.PUBLIC);
        assertNull(AuthenticationResource.staleTokenKeyToRemove(previous, current));
    }

    @Test
    void returnsPreviousKeyWhenCurrentIsNull() {
        TeamsApisAttributes previous = attrs("krista-client", "user@example.com", Constants.PUBLIC);
        assertEquals(MicrosoftAuthSdk.buildTokenKey("user@example.com", "krista-client", Constants.PUBLIC),
                AuthenticationResource.staleTokenKeyToRemove(previous, null));
    }

    @Test
    void returnsPreviousKeyWhenCurrentEmailIsNull() {
        TeamsApisAttributes previous = attrs("krista-client", "user@example.com", Constants.PUBLIC);
        TeamsApisAttributes current = attrs("own-client", null, Constants.PUBLIC);
        assertEquals(MicrosoftAuthSdk.buildTokenKey("user@example.com", "krista-client", Constants.PUBLIC),
                AuthenticationResource.staleTokenKeyToRemove(previous, current));
    }

}
