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
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers {@link AuthenticationResource#getExtensionMetadata}, {@link AuthenticationResource#customTabs}
 * and the {@link AuthenticationResource#testConnection} branches not already exercised by
 * {@link AuthenticationResourcePopupTest} (PRIVATE/Client-Credentials verification, and the generic
 * non-{@code MustAuthorizeException} failure path).
 */
class AuthenticationResourceMetadataAndConnectionTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";
    private static final String AUTH_CONTEXT_ID = "authContextId_AC9";

    private TeamsApisAttributeStore attributeStore;
    private GraphServiceClientProviderFactory providerFactory;
    private GraphServiceClientProvider provider;
    private AuthenticationResource resource;

    @BeforeEach
    void setUp() {
        attributeStore = mock(TeamsApisAttributeStore.class);
        providerFactory = mock(GraphServiceClientProviderFactory.class);
        provider = mock(GraphServiceClientProvider.class, RETURNS_DEEP_STUBS);

        Invoker invoker = mock(Invoker.class, RETURNS_DEEP_STUBS);
        when(invoker.getRoutingInfo().getRoutingURL(any(), any())).thenReturn(ROUTING);
        when(invoker.getInvokerId()).thenReturn("INV_test");

        resource = new AuthenticationResource(attributeStore, mock(RefreshTokenStore.class), providerFactory,
                invoker, mock(AuthorizationContext.class), mock(AuthorizationListener.class),
                mock(TeamsAuditService.class));
    }

    private static JsonObject privatePayload(String email) {
        JsonObject payload = new JsonObject();
        payload.addProperty(Constants.AUTH_TYPE, Constants.PRIVATE);
        payload.addProperty(Constants.TENANT_ID, "tid");
        payload.addProperty(Constants.EMAIL, email);
        payload.addProperty(Constants.CLIENT_ID, "cid");
        payload.addProperty(Constants.CLIENT_SECRET, "secret");
        return payload;
    }

    // ---------------- getExtensionMetadata ----------------

    @Test
    void getExtensionMetadataReturnsMetadataForRegisteredExtension() {
        String json = resource.getExtensionMetadata("teams");
        assertFalse(json.contains("\"error\""), "expected metadata json but got: " + json);
    }

    @Test
    void getExtensionMetadataReturnsErrorJsonForUnknownExtension() {
        String json = resource.getExtensionMetadata("definitely-unregistered-extension-id");
        assertEquals("{\"error\":\"Extension not found: definitely-unregistered-extension-id\"}", json);
    }

    // ---------------- customTabs ----------------

    @Test
    void customTabsServesPatchedAuthHtml() throws IOException {
        try (InputStream in = resource.customTabs("index.html")) {
            assertNotNull(in);
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains("return m?m[1]:'teams'"));
        }
    }

    // ---------------- testConnection: PRIVATE auth type ----------------

    @Test
    void testConnectionForPrivateAuthTypeVerifiesViaUsersEndpoint() {
        when(providerFactory.createAttributes(any(TeamsApisAttributes.class))).thenReturn(AUTH_CONTEXT_ID);
        when(providerFactory.create(AUTH_CONTEXT_ID)).thenReturn(provider);
        // RETURNS_DEEP_STUBS: getGraphServiceClientForAdmin().users(email).joinedTeams().buildRequest().get() -> null

        String json = resource.testConnection(privatePayload("svc@example.com"));

        JsonObject r = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(r.get("isSuccess").getAsBoolean());
        verify(provider).getGraphServiceClientForAdmin();
    }

    // ---------------- testConnection: generic exception (non-MustAuthorizeException) ----------------

    @Test
    void testConnectionGenericFailureReturnsIsSuccessFalseWithMessage() {
        when(providerFactory.createAttributes(any(TeamsApisAttributes.class))).thenReturn(AUTH_CONTEXT_ID);
        when(providerFactory.create(AUTH_CONTEXT_ID)).thenReturn(provider);
        when(provider.getGraphServiceClientForAdmin()).thenThrow(new RuntimeException("graph unreachable"));

        JsonObject r = JsonParser.parseString(resource.testConnection(privatePayload("svc@example.com")))
                .getAsJsonObject();

        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("graph unreachable", r.get("errorMessage").getAsString());
        assertFalse(r.has("url"), "Gson omits null fields by default, so url must be absent, not null");
        verify(attributeStore).remove(AUTH_CONTEXT_ID);
    }

    @Test
    void testConnectionFailureBeforeAuthContextIdIsAssignedSkipsCleanup() {
        // createAttributes() itself throws, so authContextId is never assigned — the finally
        // block's cleanup (authUrl == null && authContextId != null) must not run store.remove(...).
        when(providerFactory.createAttributes(any(TeamsApisAttributes.class)))
                .thenThrow(new RuntimeException("store unavailable"));

        JsonObject r = JsonParser.parseString(resource.testConnection(privatePayload("svc@example.com")))
                .getAsJsonObject();

        assertFalse(r.get("isSuccess").getAsBoolean());
        verify(attributeStore, never()).remove(anyString());
    }
}
