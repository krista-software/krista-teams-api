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
import app.krista.sdk.msauth.MicrosoftAuthSdk;
import app.krista.sdk.msauth.token.TokenResult;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.requests.TeamCollectionRequest;
import com.microsoft.graph.requests.TeamCollectionRequestBuilder;
import com.microsoft.graph.requests.UserRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import javax.ws.rs.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Covers the OAuth callback flow: {@link AuthenticationResource#getCallBackForV2Auth},
 * {@link AuthenticationResource#getCallbackForV3Auth} and the private
 * {@code getAuthenticationResponseMessage} they both delegate to (including the
 * extractEmailFromAccessToken/hasUserAccess/getAuthContextId helpers reached only through it).
 *
 * <p>{@code getAuthenticationResponseMessage} always constructs its own {@link MicrosoftAuthSdk}
 * internally and calls {@code handleCallback}, which would otherwise perform a real OAuth token
 * exchange over the network. {@link Mockito#mockConstruction} intercepts that construction so the
 * exchange is simulated in-process (requires the inline mock maker — see
 * src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker).
 */
class AuthenticationResourceCallbackTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";

    private TeamsApisAttributeStore attributeStore;
    private RefreshTokenStore refreshTokenStore;
    private GraphServiceClientProviderFactory providerFactory;
    private GraphServiceClientProvider provider;
    private AuthorizationContext context;
    private AuthorizationListener authorizationListener;
    private TeamsAuditService auditService;
    private AuthenticationResource resource;

    @BeforeEach
    void setUp() {
        attributeStore = mock(TeamsApisAttributeStore.class);
        refreshTokenStore = mock(RefreshTokenStore.class);
        providerFactory = mock(GraphServiceClientProviderFactory.class);
        provider = mock(GraphServiceClientProvider.class, RETURNS_DEEP_STUBS);
        context = mock(AuthorizationContext.class);
        authorizationListener = mock(AuthorizationListener.class);
        auditService = mock(TeamsAuditService.class);

        Invoker invoker = mock(Invoker.class, RETURNS_DEEP_STUBS);
        when(invoker.getRoutingInfo().getRoutingURL(any(), any())).thenReturn(ROUTING);
        when(invoker.getInvokerId()).thenReturn("INV_test");

        resource = new AuthenticationResource(attributeStore, refreshTokenStore, providerFactory,
                invoker, context, authorizationListener, auditService);

        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "secret", "tid", "user@example.com",
                Constants.PUBLIC, ROUTING);
        when(provider.getTeamsAttributes()).thenReturn(attrs);
    }

    private static String jwtWithClaim(String claimName, String claimValue) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"" + claimName + "\":\"" + claimValue + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".sig";
    }

    private MockedConstruction<MicrosoftAuthSdk> mockSdkReturning(TokenResult tokenResult) {
        return mockConstruction(MicrosoftAuthSdk.class,
                (mock, mockContext) -> when(mock.handleCallback(anyString(), anyString())).thenReturn(tokenResult));
    }

    /**
     * Stubs the joinedTeams().buildRequest().get() chain explicitly (rather than relying on
     * RETURNS_DEEP_STUBS beyond getUserRequestBuilder) — the Graph SDK's heavily-bounded generic
     * builder hierarchy (BaseCollectionRequestBuilder&lt;Team, TeamRequestBuilder, ...&gt;) confuses
     * Mockito's deep-stub type inference and produces a ClassCastException between the generic
     * erasure (BaseCollectionPage) and the real type (TeamCollectionPage).
     */
    private void stubHasUserAccessGet(RuntimeException toThrow) {
        UserRequestBuilder userRequestBuilder = mock(UserRequestBuilder.class);
        TeamCollectionRequestBuilder teamsBuilder = mock(TeamCollectionRequestBuilder.class);
        TeamCollectionRequest teamsRequest = mock(TeamCollectionRequest.class);
        when(provider.getUserRequestBuilder(null, null)).thenReturn(userRequestBuilder);
        when(userRequestBuilder.joinedTeams()).thenReturn(teamsBuilder);
        when(teamsBuilder.buildRequest()).thenReturn(teamsRequest);
        if (toThrow != null) {
            when(teamsRequest.get()).thenThrow(toThrow);
        }
    }

    // ---- getCallBackForV2Auth / getCallbackForV3Auth wrappers ----

    @Test
    void v2AdminConsentGrantedReturnsFixedMessageAndAudits() {
        String result = resource.getCallBackForV2Auth(null, null, "True");
        assertEquals("Admin consent granted successfully! Click 'Test Connection' to verify, then 'Save' to apply.", result);
        verify(auditService).record(eq("INV_test"), eq("CATALOG_REQUEST_SUCCESS"), eq("success"),
                eq("OAuth Callback"), eq("Admin consent granted"), isNull(), anyMap());
    }

    @Test
    void v3AdminConsentGrantedReturnsFixedMessageAndAudits() {
        String result = resource.getCallbackForV3Auth(null, null, "True");
        assertEquals("Admin consent granted successfully! Click 'Test Connection' to verify, then 'Save' to apply.", result);
    }

    @Test
    void v2DelegatesToCallbackMessageWhenNoAdminConsent() {
        String result = resource.getCallBackForV2Auth(null, "someState", null);
        assertEquals("Authentication Failed. Please re-authorize.", result);
    }

    @Test
    void v3DelegatesToCallbackMessageWhenNoAdminConsent() {
        String result = resource.getCallbackForV3Auth(null, "someState", null);
        assertEquals("Authentication Failed. Please re-authorize.", result);
    }

    // ---- code / state validation ----

    @Test
    void nullCodeReturnsAuthFailedAndAuditsFailure() {
        String result = resource.getCallBackForV2Auth(null, "user_cid_Public", null);
        assertEquals("Authentication Failed. Please re-authorize.", result);
        verify(auditService).record(eq("INV_test"), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("OAuth Callback"), contains("No authorization code"), isNull(), anyMap());
    }

    @Test
    void nullStateYieldsInvalidStateBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.getCallBackForV2Auth("code123", null, null));
    }

    @Test
    void blankKeyInStateYieldsInvalidStateBadRequest() {
        assertThrows(BadRequestException.class, () -> resource.getCallBackForV2Auth("code123", "#trailing", null));
    }

    // ---- success paths (non-WS_CONTACT) ----

    @Test
    void successWithAuthenticatedContextReturnsProceedMessage() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);
        when(refreshTokenStore.get("user@example.com_cid_Public")).thenReturn("stored-token-json");

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public#nope", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
        verify(authorizationListener).authorized();
        verify(attributeStore, never()).remove(anyString());
    }

    @Test
    void successWithoutAuthenticatedContextReturnsSaveChangesMessage() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(false);

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("upn", "user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
            assertEquals(Constants.USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES, result);
        }
        verify(authorizationListener, never()).authorized();
    }

    @Test
    void hasUserAccessGenericExceptionIsTreatedAsAccessible() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);
        stubHasUserAccessGet(new RuntimeException("operator token expired"));

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("email", "user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
    }

    // ---- authContextId present (else-if branch) + finally cleanup ----

    @Test
    void authContextIdInStateUsesProviderFactoryCreateByContextIdAndCleansUpInFinally() {
        String authContextId = "authContextId_AC2";
        when(providerFactory.create(authContextId)).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            resource.getCallBackForV2Auth("code123", "user_cid_Public#" + authContextId, null);
        }
        verify(attributeStore).remove(authContextId);
    }

    // ---- unauthorized user (hasUserAccess false via GraphServiceException) ----

    @Test
    void hasUserAccessGraphServiceExceptionRejectsSignIn() {
        when(providerFactory.create()).thenReturn(provider);
        stubHasUserAccessGet(mock(GraphServiceException.class));

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("email", "other@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "configured@example.com_cid_Public", null);
            assertTrue(result.startsWith(Constants.UNAUTHORISED_USER), "unexpected message: " + result);
            assertTrue(result.contains("configured@example.com"));
        }
        verify(refreshTokenStore).remove("configured@example.com_cid_Public");
        verify(auditService).record(eq("INV_test"), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("OAuth Callback"), contains("Sign-in rejected"), isNull(), anyMap());
    }

    // ---- WS_CONTACT / MCP path ----

    @Test
    void wsContactPathSavesPerUserAttrsAndAliasesToken() {
        String state = "wsContact-abc123#INV_test#authContextId_AC1#/forward/path";
        String key = "wsContact-abc123";
        when(providerFactory.create(any(TeamsApisAttributes.class))).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);
        when(refreshTokenStore.get(key)).thenReturn("existing-token-json");

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "mcp-user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", state, null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }

        verify(attributeStore).save(any(TeamsApisAttributes.class), eq(key + Constants.PER_USER_ATTRS_SUFFIX));
        // enrichedAttrs is built with a null clientId, which TeamsApisAttributes falls back to the
        // Krista-managed public.clientId from config.properties (loadPublicConfig()).
        String publicClientId = new TeamsApisAttributes(null, null, null, null, Constants.PUBLIC, ROUTING).getClientId();
        String compositeKey = MicrosoftAuthSdk.buildTokenKey("mcp-user@example.com", publicClientId, Constants.PUBLIC);
        verify(refreshTokenStore).put(eq(compositeKey), eq("existing-token-json"));
        verify(attributeStore).remove("authContextId_AC1");
    }

    @Test
    void wsContactPathWithNoExistingTokenSkipsAliasingPut() {
        // authenticatedEmail resolves fine (unlike wsContactPathWithUnresolvableEmailSkipsAliasing),
        // and compositeKey differs from key, but refreshTokenStore.get(key) returns null (default
        // stub) — the tokenJson-null branch must skip the put() alias entirely.
        String state = "wsContact-noToken#INV_test4#authContextId_AC6";
        when(providerFactory.create(any(TeamsApisAttributes.class))).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "another-mcp-user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            resource.getCallBackForV2Auth("code123", state, null);
        }
        verify(refreshTokenStore, never()).put(anyString(), anyString());
    }

    @Test
    void wsContactPathWithUnresolvableEmailSkipsAliasing() {
        // parts.length must be >= 3 for clientKey to be set (line: parts.length >= 3 && key.startsWith(WS_CONTACT)),
        // otherwise the MCP save block below is never entered at all.
        String state = "wsContact-xyz789#INV_test2#authContextId_AC5";
        when(providerFactory.create(any(TeamsApisAttributes.class))).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult("not-a-jwt", "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", state, null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
        verify(attributeStore).save(any(TeamsApisAttributes.class), eq("wsContact-xyz789" + Constants.PER_USER_ATTRS_SUFFIX));
        verify(refreshTokenStore, never()).put(anyString(), anyString());
    }

    // ---- parts.length >= 3 but key does not start with WS_CONTACT (line 133 TRUE/FALSE combo) ----

    @Test
    void threePartStateWithoutWsContactPrefixUsesDefaultProviderPath() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public#seg2#seg3", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
    }

    // ---- null accessToken/refreshToken (lines 192-194 ternaries) ----

    @Test
    void nullAccessTokenAndRefreshTokenAreHandledGracefully() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult(null, null, new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
    }

    @Test
    void nonNullButEmptyRefreshTokenIsTreatedAsAbsent() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "user@example.com"), "", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
    }

    // ---- refreshTokenStore.get(key) returns a non-null but empty string (line 200 ternary) ----

    @Test
    void emptyStoredVerificationTokenIsTreatedAsNotStored() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);
        when(refreshTokenStore.get("user@example.com_cid_Public")).thenReturn("");

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "user@example.com"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
        }
    }

    // ---- decodable JWT payload with none of the recognized claims (line 289 false branch) ----

    @Test
    void decodableTokenWithoutRecognizedClaimsYieldsNoEmail() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("some_other_claim", "irrelevant"), "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
    }

    // ---- ITokenStore adapter (AuthenticationResource$1): forwards save/get/delete to RefreshTokenStore ----

    @Test
    void tokenStoreAdapterForwardsToRefreshTokenStore() throws Exception {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);
        TokenResult tokenResult = new TokenResult(
                jwtWithClaim("preferred_username", "user@example.com"), "refresh", new Date());

        java.util.concurrent.atomic.AtomicReference<app.krista.sdk.msauth.token.ITokenStore> captured =
                new java.util.concurrent.atomic.AtomicReference<>();
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockConstruction(MicrosoftAuthSdk.class,
                (mock, mockContext) -> {
                    when(mock.handleCallback(anyString(), anyString())).thenReturn(tokenResult);
                    Object builder = mockContext.arguments().get(0);
                    java.lang.reflect.Field field = builder.getClass().getDeclaredField("tokenStore");
                    field.setAccessible(true);
                    captured.set((app.krista.sdk.msauth.token.ITokenStore) field.get(builder));
                })) {
            resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
        }

        app.krista.sdk.msauth.token.ITokenStore tokenStore = captured.get();
        assertNotNull(tokenStore, "the tokenStore passed to the builder must be reachable via reflection");

        tokenStore.save("k1", "v1");
        verify(refreshTokenStore).put("k1", "v1");

        when(refreshTokenStore.get("k1")).thenReturn("v1");
        assertEquals("v1", tokenStore.get("k1"));

        tokenStore.delete("k1");
        verify(refreshTokenStore).remove("k1");
    }

    // ---- extractEmailFromAccessToken: undecodable payload ----

    @Test
    void undecodableAccessTokenPayloadIsTreatedAsNoEmail() {
        when(providerFactory.create()).thenReturn(provider);
        when(context.isAuthenticated()).thenReturn(true);

        TokenResult tokenResult = new TokenResult("abc.!!!not-base64!!!.sig", "refresh", new Date());
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockSdkReturning(tokenResult)) {
            String result = resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null);
            assertEquals(AuthenticationResource.USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST, result);
        }
    }

    // ---- exception handling ----

    @Test
    void mustAuthorizeExceptionPropagatesUnwrapped() {
        when(providerFactory.create()).thenThrow(
                new MustAuthorizeException("please authorize", new java.util.ArrayList<>()));

        assertThrows(MustAuthorizeException.class,
                () -> resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null));
    }

    @Test
    void genericExceptionIsWrappedInIllegalStateExceptionWithRootCause() {
        when(providerFactory.create()).thenThrow(
                new RuntimeException("outer failure", new RuntimeException("inner cause")));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null));
        assertTrue(ex.getMessage().contains("outer failure"));
        assertTrue(ex.getMessage().contains("Root cause: inner cause"));
        verify(auditService).record(eq("INV_test"), eq("CATALOG_REQUEST_FAILURE"), eq("failure"),
                eq("OAuth Callback"), contains("outer failure"), eq("RuntimeException"), anyMap());
    }

    @Test
    void interruptedExceptionSetsInterruptFlagAndWraps() {
        when(providerFactory.create()).thenReturn(provider);
        Thread.interrupted(); // clear any stale flag before asserting
        try (MockedConstruction<MicrosoftAuthSdk> ignored = mockConstruction(MicrosoftAuthSdk.class,
                (mock, ctx) -> doAnswer(invocation -> {
                    throw new InterruptedException("simulated interrupt");
                }).when(mock).handleCallback(anyString(), anyString()))) {
            assertThrows(IllegalStateException.class,
                    () -> resource.getCallBackForV2Auth("code123", "user@example.com_cid_Public", null));
        }
        assertTrue(Thread.interrupted(), "Thread interrupt flag must be restored");
    }
}
