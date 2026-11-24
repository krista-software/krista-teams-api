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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Covers {@link AuthenticationResource#saveCredentials}, {@link AuthenticationResource#getCredentials}
 * and {@link AuthenticationResource#getAuthKey}, plus the private
 * {@code unmaskClientSecretIfNeeded} helper reached only through save/testConnection payload handling.
 */
class AuthenticationResourceCredentialsTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";
    private static final String INVOKER_ID = "INV_test";

    private TeamsApisAttributeStore attributeStore;
    private RefreshTokenStore refreshTokenStore;
    private TeamsAuditService auditService;
    private AuthenticationResource resource;

    @BeforeEach
    void setUp() {
        attributeStore = mock(TeamsApisAttributeStore.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        auditService = mock(TeamsAuditService.class);

        Invoker invoker = mock(Invoker.class, RETURNS_DEEP_STUBS);
        when(invoker.getRoutingInfo().getRoutingURL(any(), any())).thenReturn(ROUTING);
        when(invoker.getInvokerId()).thenReturn(INVOKER_ID);

        resource = new AuthenticationResource(attributeStore, refreshTokenStore,
                mock(GraphServiceClientProviderFactory.class), invoker, mock(AuthorizationContext.class),
                mock(AuthorizationListener.class), auditService);
    }

    private static TeamsApisAttributes attrs(String clientId, String secret, String email, String authType,
                                              String publicMode, String privateMode) {
        return new TeamsApisAttributes(clientId, secret, "tid", email, authType, ROUTING, publicMode, privateMode);
    }

    // ---------------- saveCredentials ----------------

    private static JsonObject publicOwnPayload(String email, String clientId, String secret) {
        JsonObject payload = new JsonObject();
        payload.addProperty(Constants.AUTH_TYPE, Constants.PUBLIC);
        payload.addProperty("publicAuthMode", "own");
        payload.addProperty(Constants.EMAIL, email);
        payload.addProperty(Constants.CLIENT_ID, clientId);
        payload.addProperty(Constants.CLIENT_SECRET, secret);
        return payload;
    }

    @Test
    void saveCredentialsSuccessResetsAuthCountersAndReturnsSuccessJson() {
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID))).thenReturn(true);

        String json = resource.saveCredentials(publicOwnPayload("user@example.com", "cid", "secret"));

        JsonObject r = JsonParser.parseString(json).getAsJsonObject();
        assertTrue(r.get("isSuccess").getAsBoolean());
        assertFalse(r.has("errorMessage"), "Gson omits null fields by default");
        verify(auditService).resetAuthCounters();
        verify(auditService).record(eq(INVOKER_ID), eq("CATALOG_REQUEST_SUCCESS"), eq("success"),
                eq("Save Credentials"), anyString(), isNull(), anyMap());
    }

    @Test
    void saveCredentialsFailureDoesNotResetAuthCountersAndReturnsFailureJson() {
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID))).thenReturn(false);

        String json = resource.saveCredentials(publicOwnPayload("user@example.com", "cid", "secret"));

        JsonObject r = JsonParser.parseString(json).getAsJsonObject();
        assertFalse(r.get("isSuccess").getAsBoolean());
        assertEquals("Failed to save configuration", r.get("errorMessage").getAsString());
        verify(auditService, never()).resetAuthCounters();
    }

    @Test
    void saveCredentialsRemovesStaleTokenOnModeSwitch() {
        TeamsApisAttributes previous = attrs("krista-client", "s", "user@example.com", Constants.PUBLIC, "krista", null);
        when(attributeStore.load(INVOKER_ID)).thenReturn(previous);
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID))).thenReturn(true);

        resource.saveCredentials(publicOwnPayload("user@example.com", "own-client", "secret"));

        String staleKey = app.krista.sdk.msauth.MicrosoftAuthSdk.buildTokenKey(
                "user@example.com", "krista-client", Constants.PUBLIC);
        verify(refreshTokenStore).remove(staleKey);
    }

    @Test
    void saveCredentialsWithUnsupportedAuthTypeAuditsFailureAndRethrows() {
        JsonObject payload = new JsonObject();
        payload.addProperty(Constants.AUTH_TYPE, "Bogus");

        assertThrows(IllegalArgumentException.class, () -> resource.saveCredentials(payload));
        verify(auditService).record(eq(INVOKER_ID), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("Save Credentials"), anyString(), eq("IllegalArgumentException"), anyMap());
    }

    @Test
    void saveCredentialsPropagatesMustAuthorizeExceptionFromStore() {
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID)))
                .thenThrow(new MustAuthorizeException("please authorize", new java.util.ArrayList<>()));

        assertThrows(MustAuthorizeException.class,
                () -> resource.saveCredentials(publicOwnPayload("user@example.com", "cid", "secret")));
        verify(auditService).record(eq(INVOKER_ID), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("Save Credentials"), contains("Authentication required"), eq("MustAuthorizeException"), anyMap());
    }

    @Test
    void saveCredentialsWrapsGenericExceptionFromStoreAsIs() {
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID)))
                .thenThrow(new RuntimeException("db down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resource.saveCredentials(publicOwnPayload("user@example.com", "cid", "secret")));
        assertEquals("db down", ex.getMessage());
        verify(auditService).record(eq(INVOKER_ID), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("Save Credentials"), eq("db down"), eq("RuntimeException"), anyMap());
    }

    @Test
    void saveCredentialsReusesMaskedSecretFromStore() {
        TeamsApisAttributes existing = attrs("cid", "real-secret", "user@example.com", Constants.PUBLIC, "own", null);
        when(attributeStore.load(INVOKER_ID)).thenReturn(existing);
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID))).thenReturn(true);

        resource.saveCredentials(publicOwnPayload("user@example.com", "cid", AuthenticationResource.MASKED_SECRET));

        verify(attributeStore).save(argThat(a -> "real-secret".equals(a.getClientSecret())), eq(INVOKER_ID));
    }

    @Test
    void saveCredentialsWithPlaceholderSecretAndNoExistingAttrsOmitsClientSecretKey() {
        // No previously stored attrs to fall back to, so resolveClientSecret returns null and
        // unmaskClientSecretIfNeeded removes CLIENT_SECRET from the payload entirely rather than
        // persisting the literal placeholder string.
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID))).thenReturn(true);

        String json = resource.saveCredentials(
                publicOwnPayload("user@example.com", "cid", AuthenticationResource.MASKED_SECRET));

        assertTrue(JsonParser.parseString(json).getAsJsonObject().get("isSuccess").getAsBoolean());
        verify(attributeStore).save(
                argThat(a -> !AuthenticationResource.MASKED_SECRET.equals(a.getClientSecret())), eq(INVOKER_ID));
    }

    @Test
    void saveCredentialsWithMissingClientSecretKeyIsNoOp() {
        JsonObject payload = new JsonObject();
        payload.addProperty(Constants.AUTH_TYPE, Constants.PRIVATE);
        payload.addProperty(Constants.TENANT_ID, "tid");
        payload.addProperty(Constants.EMAIL, "user@example.com");
        when(attributeStore.save(any(TeamsApisAttributes.class), eq(INVOKER_ID))).thenReturn(true);

        String json = resource.saveCredentials(payload);

        assertTrue(JsonParser.parseString(json).getAsJsonObject().get("isSuccess").getAsBoolean());
    }

    @Test
    void saveCredentialsWithJsonNullClientSecretIsSkippedByUnmaskButRejectedDownstream() {
        // unmaskClientSecretIfNeeded treats a JsonNull clientSecret as "nothing to unmask" and
        // leaves it in place; TeamsApisAttributes.create() then still sees the key present
        // (has(CLIENT_SECRET) == true) and calls getAsString() on the JsonNull, which Gson rejects.
        JsonObject payload = new JsonObject();
        payload.addProperty(Constants.AUTH_TYPE, Constants.PRIVATE);
        payload.addProperty(Constants.TENANT_ID, "tid");
        payload.addProperty(Constants.EMAIL, "user@example.com");
        payload.add(Constants.CLIENT_SECRET, com.google.gson.JsonNull.INSTANCE);

        assertThrows(UnsupportedOperationException.class, () -> resource.saveCredentials(payload));
        verify(auditService).record(eq(INVOKER_ID), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("Save Credentials"), anyString(), eq("UnsupportedOperationException"), anyMap());
    }

    // ---------------- getCredentials ----------------

    @Test
    void getCredentialsReturnsEmptyWhenNothingStored() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        assertEquals("", resource.getCredentials(Constants.PUBLIC, null));
    }

    @Test
    void getCredentialsReturnsEmptyWhenAuthTypeParamNull() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PUBLIC, "krista", null));
        assertEquals("", resource.getCredentials(null, null));
    }

    @Test
    void getCredentialsReturnsEmptyWhenAuthTypeMismatches() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PRIVATE, null, "s2s"));
        assertEquals("", resource.getCredentials(Constants.PUBLIC, null));
    }

    @Test
    void getCredentialsReturnsEmptyOnPublicAuthFlowModeMismatch() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PUBLIC, "krista", null));
        assertEquals("", resource.getCredentials(Constants.PUBLIC, "AUTH_CODE_PRIVATE"));
    }

    @Test
    void getCredentialsReturnsAttributesOnPublicAuthFlowModeMatch() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PUBLIC, "own", null));
        String json = resource.getCredentials(Constants.PUBLIC, "AUTH_CODE_PRIVATE");
        assertFalse(json.isEmpty());
    }

    @Test
    void getCredentialsHidesClientIdAndSecretForKristaManagedPublic() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PUBLIC, "krista", null));
        JsonObject r = JsonParser.parseString(resource.getCredentials(Constants.PUBLIC, null)).getAsJsonObject();
        assertFalse(r.has(Constants.CLIENT_ID));
        assertFalse(r.has(Constants.CLIENT_SECRET));
    }

    @Test
    void getCredentialsHidesClientIdAndSecretForKristaManagedPrivate() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PRIVATE, null, "krista"));
        JsonObject r = JsonParser.parseString(resource.getCredentials(Constants.PRIVATE, null)).getAsJsonObject();
        assertFalse(r.has(Constants.CLIENT_ID));
        assertFalse(r.has(Constants.CLIENT_SECRET));
    }

    @Test
    void getCredentialsMasksSecretForOwnCredentials() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "real-secret", "u@e.com", Constants.PUBLIC, "own", null));
        JsonObject r = JsonParser.parseString(resource.getCredentials(Constants.PUBLIC, null)).getAsJsonObject();
        assertEquals(AuthenticationResource.MASKED_SECRET, r.get(Constants.CLIENT_SECRET).getAsString());
    }

    @Test
    void getCredentialsOmitsSecretWhenNoneStoredForOwnCredentials() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "", "u@e.com", Constants.PRIVATE, null, "s2s"));
        JsonObject r = JsonParser.parseString(resource.getCredentials(Constants.PRIVATE, null)).getAsJsonObject();
        assertFalse(r.has(Constants.CLIENT_SECRET));
    }

    // ---------------- getAuthKey ----------------

    @Test
    void getAuthKeyReturnsEmptyWhenNothingStored() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(null);
        assertEquals("", resource.getAuthKey());
    }

    @Test
    void getAuthKeyReturnsStoredAuthType() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", Constants.PRIVATE, null, "s2s"));
        assertEquals("\"Private\"", resource.getAuthKey());
    }

    @Test
    void getAuthKeyDefaultsToPublicWhenAuthTypeMissing() {
        when(attributeStore.load(INVOKER_ID)).thenReturn(attrs("cid", "s", "u@e.com", null, null, null));
        assertEquals("\"" + Constants.PUBLIC + "\"", resource.getAuthKey());
    }
}
