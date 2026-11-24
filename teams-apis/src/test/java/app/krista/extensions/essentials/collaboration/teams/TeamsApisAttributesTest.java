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

import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeamsApisAttributesTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";

    @Test
    void privateAttributesRoundTripThroughToMap() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "csecret", "tid",
                "admin@example.com", Constants.PRIVATE, ROUTING);
        Map<String, Object> map = attrs.toMap();
        assertEquals("cid", map.get(Constants.CLIENT_ID));
        assertEquals("csecret", map.get(Constants.CLIENT_SECRET));
        assertEquals("tid", map.get(Constants.TENANT_ID));
        assertEquals("admin@example.com", map.get(Constants.EMAIL));
        assertEquals(Constants.PRIVATE, map.get(Constants.AUTH_TYPE));
    }

    @Test
    void publicAttributesFallBackToKristaManagedClientId() {
        TeamsApisAttributes attrs = new TeamsApisAttributes(null, null, null,
                "user@example.com", Constants.PUBLIC, ROUTING);
        assertNotNull(attrs.getClientId(), "public auth must fall back to config.properties clientId");
        assertEquals("krista", attrs.getPublicAuthMode());
    }

    @Test
    void createParsesPrivatePayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty("authType", Constants.PRIVATE);
        payload.addProperty(Constants.CLIENT_ID, "cid");
        payload.addProperty(Constants.CLIENT_SECRET, "cs");
        payload.addProperty(Constants.TENANT_ID, "tid");
        payload.addProperty(Constants.EMAIL, "a@b.com");
        TeamsApisAttributes attrs = TeamsApisAttributes.create(payload, ROUTING);
        assertEquals(Constants.PRIVATE, attrs.getAuthType());
        assertEquals("s2s", attrs.getPrivateAuthMode());
    }

    /**
     * The OAuth callback for the user-authentication flow must stay at the legacy
     * {@code teams-apis/callback} (no "teams" application-path segment), so existing customer
     * Azure app registrations keep working across the 2.0.9 → SDK migration without needing to
     * re-register the redirect URI. Tabs stay under {@code teams-apis/teams/...}; only the
     * callback lives at the jaxrsId root.
     */
    @Test
    void userAuthCallbackEndpointUsesLegacyTeamsApisCallback() {
        TeamsApisAttributes ownCreds = new TeamsApisAttributes("cid", "cs", "tid",
                "a@b.com", Constants.PUBLIC, ROUTING, "own", null);
        assertEquals(ROUTING + "/teams-apis/callback", ownCreds.getCallbackEndPoint());

        TeamsApisAttributes priv = new TeamsApisAttributes("cid", "cs", "tid",
                "a@b.com", Constants.PRIVATE, ROUTING);
        assertEquals(ROUTING + "/teams-apis/callback", priv.getCallbackEndPoint());
    }

    /**
     * TeamsApisAttributes is a value object, always constructed manually or deserialized from
     * the KV store — never injected. Marking it @Service makes the HK2 service scanner try to
     * instantiate it at extension startup and log "NoSuchMethodException: no constructor marked
     * @Inject and no zero argument constructor" on every scan.
     */
    @Test
    void isNotAnHk2Service() {
        assertNull(TeamsApisAttributes.class.getAnnotation(org.jvnet.hk2.annotations.Service.class),
                "TeamsApisAttributes must not be @Service — it has no injectable constructor");
    }
}
