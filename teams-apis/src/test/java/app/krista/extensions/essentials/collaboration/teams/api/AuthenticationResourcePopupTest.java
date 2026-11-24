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

import app.krista.extension.authorization.MustAuthorizeException;
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
import app.krista.model.field.NamedValuedField;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The auth tab only opens the Microsoft sign-in popup when /testConnection returns JSON with a
 * "url" field ({@code r.url} in auth/index.html's doTest). Rethrowing MustAuthorizeException
 * (the pre-fix behavior) reaches the page as an error body without a url, so no popup ever
 * opens and authorization is impossible. These tests pin the Outlook-4 response contract.
 */
class AuthenticationResourcePopupTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";
    private static final String TOKEN_KEY = "user@example.com_cid_Public";
    private static final String AUTH_CONTEXT_ID = "AC-1";

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
                mock(TeamsAuditService.class, RETURNS_DEEP_STUBS));
    }

    private static MustAuthorizeException mustAuthorize(String userId, String authContextId) {
        List<NamedValuedField> details = new ArrayList<>();
        details.add(new NamedValuedField(Constants.USER_ID, "Text", userId, new HashMap<>(), new HashMap<>()));
        if (authContextId != null) {
            details.add(new NamedValuedField(Constants.AUTH_CONTEXT_ID, "Text", authContextId,
                    new HashMap<>(), new HashMap<>()));
        }
        return new MustAuthorizeException("To complete the setup, please click the 'Authorize' button.", details);
    }

    private static JsonObject publicPayload() {
        JsonObject payload = new JsonObject();
        payload.addProperty(Constants.EMAIL, "user@example.com");
        payload.addProperty(Constants.AUTH_TYPE, Constants.PUBLIC);
        return payload;
    }

    @Test
    void mustAuthorizeReturnsJsonWithAuthorizeUrl() {
        when(providerFactory.createAttributes(any(TeamsApisAttributes.class))).thenReturn(AUTH_CONTEXT_ID);
        when(providerFactory.create(AUTH_CONTEXT_ID)).thenReturn(provider);
        when(provider.getGraphServiceClientForAdmin()).thenThrow(mustAuthorize(TOKEN_KEY, AUTH_CONTEXT_ID));

        JsonObject r = JsonParser.parseString(resource.testConnection(publicPayload())).getAsJsonObject();

        assertFalse(r.get("isSuccess").getAsBoolean());
        assertTrue(r.has("url"), "response must carry the OAuth authorize url so the page opens the popup");
        String url = r.get("url").getAsString();
        assertTrue(url.contains("login.microsoftonline.com"), "authorize url must target Microsoft login: " + url);
        assertTrue(url.contains("state="), "authorize url must carry the state parameter: " + url);
    }

    @Test
    void successReturnsIsSuccessTrueJson() {
        when(providerFactory.createAttributes(any(TeamsApisAttributes.class))).thenReturn(AUTH_CONTEXT_ID);
        when(providerFactory.create(AUTH_CONTEXT_ID)).thenReturn(provider);
        // deep-stubbed provider: me().joinedTeams().buildRequest().get() just returns null

        JsonObject r = JsonParser.parseString(resource.testConnection(publicPayload())).getAsJsonObject();

        assertTrue(r.get("isSuccess").getAsBoolean(),
                "page checks r.isSuccess — bare \"true\" text reads as failure in the auth tab");
    }

    @Test
    void testConnectionAttributesAreTemporaryUnlessAwaitingCallback() {
        when(providerFactory.createAttributes(any(TeamsApisAttributes.class))).thenReturn(AUTH_CONTEXT_ID);
        when(providerFactory.create(AUTH_CONTEXT_ID)).thenReturn(provider);

        resource.testConnection(publicPayload());

        // success → no pending OAuth round trip → the probe attributes must not linger as "current"
        verify(attributeStore).remove(AUTH_CONTEXT_ID);
    }

    @Test
    void statePartsAreResolvedByNameAndPublicAppendsForwardPath() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "user@example.com",
                Constants.PUBLIC, ROUTING);
        // reversed detail order — lookup must be by name, not index
        List<NamedValuedField> details = new ArrayList<>();
        details.add(new NamedValuedField(Constants.AUTH_CONTEXT_ID, "Text", AUTH_CONTEXT_ID,
                new HashMap<>(), new HashMap<>()));
        details.add(new NamedValuedField(Constants.USER_ID, "Text", TOKEN_KEY, new HashMap<>(), new HashMap<>()));
        MustAuthorizeException cause = new MustAuthorizeException("authorize", details);

        String state = AuthenticationResource.buildStateParameter(cause, attrs);

        assertEquals(TOKEN_KEY + Constants.HASH + AUTH_CONTEXT_ID + Constants.HASH + attrs.getForwardPath(), state);
    }

    @Test
    void plainUserIdStateWhenNeitherClientUserIdNorAuthContextIdPresent() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "user@example.com",
                Constants.PRIVATE, ROUTING);
        List<NamedValuedField> details = new ArrayList<>();
        details.add(new NamedValuedField(Constants.USER_ID, "Text", TOKEN_KEY, new HashMap<>(), new HashMap<>()));
        MustAuthorizeException cause = new MustAuthorizeException("authorize", details);

        String state = AuthenticationResource.buildStateParameter(cause, attrs);

        // PRIVATE auth type never appends a forward path (getForwardPath() is Public-only).
        assertEquals(TOKEN_KEY, state);
    }

    @Test
    void missingUserIdFieldFallsBackToFirstDetailValue() {
        // Legacy fallback: if no field is literally named USER_ID, buildStateParameter falls back
        // to the first detail's raw value instead of failing.
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "user@example.com",
                Constants.PRIVATE, ROUTING);
        List<NamedValuedField> details = new ArrayList<>();
        details.add(new NamedValuedField("someOtherField", "Text", TOKEN_KEY, new HashMap<>(), new HashMap<>()));
        MustAuthorizeException cause = new MustAuthorizeException("authorize", details);

        String state = AuthenticationResource.buildStateParameter(cause, attrs);

        assertEquals(TOKEN_KEY, state);
    }

    @Test
    void clientUserIdWithoutAuthContextIdFallsBackToPlainUserIdState() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "user@example.com",
                Constants.PUBLIC, ROUTING);
        List<NamedValuedField> details = new ArrayList<>();
        details.add(new NamedValuedField(Constants.USER_ID, "Text", TOKEN_KEY, new HashMap<>(), new HashMap<>()));
        details.add(new NamedValuedField(Constants.CLIENT_USER_ID, "Text", "wsContact-1", new HashMap<>(), new HashMap<>()));
        MustAuthorizeException cause = new MustAuthorizeException("authorize", details);

        String state = AuthenticationResource.buildStateParameter(cause, attrs);

        assertEquals(TOKEN_KEY + Constants.HASH + attrs.getForwardPath(), state);
    }
}
