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
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.audit.TeamsAuditService;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.AuthenticationResponse;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.authentication.AuthorizationListener;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.model.field.NamedValuedField;
import app.krista.sdk.msauth.MicrosoftAuthSdk;
import app.krista.sdk.msauth.config.AuthConfig;
import app.krista.sdk.msauth.token.ITokenStore;
import app.krista.sdk.msauth.token.InMemoryTokenStore;
import app.krista.sdk.msauth.token.TokenResult;
import com.google.gson.Gson;
import com.microsoft.graph.http.GraphServiceException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.*;

@Path("/")
public final class AuthenticationResource {

    public static final String USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST = "User Authenticated Successfully. Please proceed with the request.";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationResource.class);
    private static final Gson GSON = new Gson();

    /**
     * Placeholder returned by {@link #getCredentials} in place of a real stored clientSecret,
     * and recognized on save/testConnection as "the user did not change the secret".
     * Secrets must never round-trip to the UI in plaintext.
     */
    static final String MASKED_SECRET = "********";

    private final TeamsApisAttributeStore teamsApisAttributeStore;
    private final RefreshTokenStore refreshTokenStore;
    private final GraphServiceClientProviderFactory providerFactory;
    private final AuthorizationContext context;
    private final AuthorizationListener authorizationListener;
    private final String baseRoutingUrl;
    private final String invokerId;
    private final TeamsAuditService auditService;

    @Inject
    public AuthenticationResource(TeamsApisAttributeStore teamsApisAttributeStore, RefreshTokenStore refreshTokenStore,
                                  GraphServiceClientProviderFactory providerFactory, Invoker invoker,
                                  AuthorizationContext context, AuthorizationListener authorizationListener,
                                  TeamsAuditService auditService) {
        this.teamsApisAttributeStore = teamsApisAttributeStore;
        this.refreshTokenStore = refreshTokenStore;
        this.providerFactory = providerFactory;
        this.context = context;
        this.authorizationListener = authorizationListener;
        this.auditService = auditService;
        this.baseRoutingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.invokerId = invoker.getInvokerId();
    }

    @GET
    @Path("callback")
    public String getCallBackForV2Auth(@QueryParam("code") String code, @QueryParam("state") String state,
                                        @QueryParam("admin_consent") String adminConsent) {
        if ("True".equals(adminConsent)) {
            auditService.record(invokerId, "CATALOG_REQUEST_SUCCESS", "success", "OAuth Callback",
                    "Admin consent granted", null, Map.of("triggeredBy", "Admin"));
            return "Admin consent granted successfully! Click 'Test Connection' to verify, then 'Save' to apply.";
        }
        return getAuthenticationResponseMessage(code, state);
    }

    @GET
    @Path("teams/v3/oauth/callback")
    public String getCallbackForV3Auth(@QueryParam("code") String code, @QueryParam("state") String state,
                                        @QueryParam("admin_consent") String adminConsent) {
        if ("True".equals(adminConsent)) {
            auditService.record(invokerId, "CATALOG_REQUEST_SUCCESS", "success", "OAuth Callback",
                    "Admin consent granted", null, Map.of("triggeredBy", "Admin"));
            return "Admin consent granted successfully! Click 'Test Connection' to verify, then 'Save' to apply.";
        }
        return getAuthenticationResponseMessage(code, state);
    }

    @NotNull
    private String getAuthenticationResponseMessage(String code, String state) {
        long flowStartTime = System.currentTimeMillis();
        auditService.startRequest();
        LOGGER.info("OAuthCallback: START, codeLength={}, state={}", code != null ? code.length() : 0, state);

        if (code == null) {
            LOGGER.debug("Missing authorization code in callback. Rejecting authentication.");
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "OAuth Callback",
                    "No authorization code received — user may have cancelled", null,
                    Map.of("triggeredBy", "Unknown"));
            return "Authentication Failed. Please re-authorize.";
        }

        String[] parts = state != null ? state.split(Constants.HASH) : new String[]{""};
        if (parts[0].isBlank()) {
            LOGGER.error("State parameter is invalid or empty.");
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "OAuth Callback",
                    "Invalid state parameter — possible CSRF or corrupted redirect", null,
                    Map.of("triggeredBy", "Unknown"));
            throw new BadRequestException(Constants.INVALID_STATE_PARAMETERS);
        }

        String key = parts[0];
        String userEmail = key.contains(UNDER_SCORE) ? key.split(UNDER_SCORE)[0] : key;
        Map<String, String> callbackMeta = new HashMap<>();
        callbackMeta.put("triggeredBy", userEmail);

        // For MCP state (wsContact#INV_xxx#authContextId#forwardPath), parts[1] is the invokerId
        String clientKey = null;
        if (parts.length >= 3 && key.startsWith(WS_CONTACT)) {
            clientKey = parts[1];
        }
        String authContextId = getAuthContextId(state);

        LOGGER.info("OAuthCallback: parsed state, key={}, clientKey={}, authContextId={}, isMcpPath={}, partsCount={}",
                key, clientKey, authContextId, key.startsWith(WS_CONTACT), parts.length);

        try {
            // Per architectural rule: the token exchange MUST use the SAME OAuth client that
            // built the auth URL. For per-user callbacks (wsContact-prefixed key), the auth URL
            // was built with default Public, so token exchange uses default Public too —
            // regardless of stored Setup attrs. Otherwise AADSTS7000215.
            GraphServiceClientProvider clientProvider;
            if (key.startsWith(WS_CONTACT)) {
                // Per-user path — force default Public OAuth client
                TeamsApisAttributes publicAttrs = new TeamsApisAttributes(null, null, null, null, Constants.PUBLIC, baseRoutingUrl);
                clientProvider = providerFactory.create(publicAttrs);
            } else if (authContextId != null) {
                clientProvider = providerFactory.create(authContextId);
            } else {
                clientProvider = providerFactory.create();
            }

            TeamsApisAttributes teamsApisAttributes = clientProvider.getTeamsAttributes();
            LOGGER.info("OAuthCallback: provider created, authType={}, email={}, clientId={}, tenantId={}, callbackUrl={}, scopes={}",
                    teamsApisAttributes.getAuthType(), teamsApisAttributes.getEmail(), teamsApisAttributes.getClientId(),
                    teamsApisAttributes.getTenantId(), teamsApisAttributes.getCallbackEndPoint(), teamsApisAttributes.getScopeString());

            LOGGER.info("OAuthCallback: exchanging authorization code via MicrosoftAuthSdk, key={}", key);
            ITokenStore tokenStore = new ITokenStore() {
                @Override
                public void save(String k, String v) {
                    refreshTokenStore.put(k, v);
                }

                @Override
                public String get(String k) {
                    return refreshTokenStore.get(k);
                }

                @Override
                public void delete(String k) {
                    refreshTokenStore.remove(k);
                }
            };
            // Callback code exchange — user signed in via browser popup, so ALWAYS use
            // the Auth Code Public flow (multi-tenant /organizations/ authority). Both Krista
            // Managed and Own Credentials use the same authority — only the clientId differs.
            // Private (Client Credentials) never reaches this callback (no popup).
            MicrosoftAuthSdk sdk = MicrosoftAuthSdk.builder()
                    .config(toAuthConfig(teamsApisAttributes))
                    .tokenStore(tokenStore).build();
            // key is already the composite tokenKey (email_clientId_authType) from MustAuthorizeException
            // handleCallback stores refresh token under this key — matches provider lookup
            long tokenExchangeStart = System.currentTimeMillis();
            TokenResult tokenResult = sdk.handleCallback(code, key);
            LOGGER.info("OAuthCallback: token exchange completed, key={}, hasAccessToken={}, accessTokenLength={}, hasRefreshToken={}, refreshTokenLength={}, tokenExchangeDurationMs={}",
                    key,
                    tokenResult.getAccessToken() != null, tokenResult.getAccessToken() != null ? tokenResult.getAccessToken().length() : 0,
                    tokenResult.getRefreshToken() != null && !tokenResult.getRefreshToken().isEmpty(),
                    tokenResult.getRefreshToken() != null ? tokenResult.getRefreshToken().length() : 0,
                    System.currentTimeMillis() - tokenExchangeStart);

            // Verify the token was actually persisted in the store
            String verifyToken = refreshTokenStore.get(key);
            LOGGER.info("Callback token verification: key={}, stored={}", key,
                    verifyToken != null && !verifyToken.isEmpty() ? "YES" : "NO");

            // Extract actual email from token for accurate audit
            String authenticatedEmail = extractEmailFromAccessToken(tokenResult.getAccessToken());
            if (authenticatedEmail != null) callbackMeta.put("triggeredBy", authenticatedEmail);

            // MCP path: save permanent Public attrs under wsContactId + suffix.
            // The suffix avoids key collision with the refresh token, which the SDK
            // stores under bare wsContactId in the same KeyValueStore.
            if (clientKey != null && key.startsWith(WS_CONTACT)) {
                String attrsKey = key + Constants.PER_USER_ATTRS_SUFFIX;
                TeamsApisAttributes enrichedAttrs = new TeamsApisAttributes(
                        null, null, null, authenticatedEmail, Constants.PUBLIC, baseRoutingUrl);
                LOGGER.info("MCP callback: saving per-user attrs with email={} under attrsKey={} (token under {})",
                        authenticatedEmail, attrsKey, key);
                teamsApisAttributeStore.save(enrichedAttrs, attrsKey);

// handleCallback stores the refresh token under wsContactId, but the provider later
// looks it up via buildTokenKey(email, clientId, authType) — a different key.
// Copy the token to the composite key now so the next request finds it without
// triggering another re-auth round-trip.
                if (authenticatedEmail != null) {
                    String compositeKey = MicrosoftAuthSdk.buildTokenKey(
                            authenticatedEmail, enrichedAttrs.getClientId(), Constants.PUBLIC);
                    if (!compositeKey.equals(key)) {
                        String tokenJson = refreshTokenStore.get(key);
                        if (tokenJson != null) {
                            refreshTokenStore.put(compositeKey, tokenJson);
                            LOGGER.info("MCP callback: aliased per-user token from wsContactId='{}' to compositeKey='{}'",
                                    key, compositeKey);
                        }
                    }
                }
            }

            if (!key.startsWith(Constants.WS_CONTACT) && !hasUserAccess(clientProvider)) {
                LOGGER.debug("User access check failed. Authenticated user does not match setup user.");
                refreshTokenStore.remove(key);
                String configuredEmail = key.split(UNDER_SCORE)[0];
                auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "OAuth Callback",
                        "Sign-in rejected: '" + callbackMeta.get("triggeredBy") + "' signed in but setup is configured for '" + configuredEmail + "'",
                        null, callbackMeta);
                return Constants.UNAUTHORISED_USER + " User email '" + key.split(UNDER_SCORE)[0] + "' configured in the setup does not match with authenticated user.";
            }

            auditService.record(invokerId, "CATALOG_REQUEST_SUCCESS", "success", "OAuth Callback",
                    "User authenticated: " + callbackMeta.get("triggeredBy"), null, callbackMeta);

            if (context.isAuthenticated()) {
                LOGGER.info("OAuthCallback: SUCCESS, user authenticated and context active, key={}, email={}, totalDurationMs={}",
                        key, callbackMeta.get("triggeredBy"), System.currentTimeMillis() - flowStartTime);
                authorizationListener.authorized();
                return USER_AUTHENTICATED_SUCCESSFULLY_PLEASE_PROCEED_WITH_REQUEST;
            }

            LOGGER.info("OAuthCallback: SUCCESS, user authenticated (pending save), key={}, email={}, totalDurationMs={}",
                    key, callbackMeta.get("triggeredBy"), System.currentTimeMillis() - flowStartTime);
            return Constants.USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES;

        } catch (MustAuthorizeException cause) {
            // Rule 3: MustAuthorizeException must propagate — never wrap in IllegalStateException.
            throw cause;
        } catch (Exception cause) {
            LOGGER.error("OAuthCallback: FAILED, class={}, method=getAuthenticationResponseMessage, error={}, exceptionType={}, totalDurationMs={}",
                    getClass().getSimpleName(), cause.getMessage(), cause.getClass().getName(), System.currentTimeMillis() - flowStartTime, cause);
            if (cause instanceof InterruptedException) Thread.currentThread().interrupt();
            String rootMsg = cause.getMessage();
            if (cause.getCause() != null) rootMsg += " | Root cause: " + cause.getCause().getMessage();
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "OAuth Callback",
                    rootMsg, cause.getClass().getSimpleName(), callbackMeta);
            throw new IllegalStateException(rootMsg, cause);

        } finally {
            if (authContextId != null) {
                teamsApisAttributeStore.remove(authContextId);
            }
        }
    }

    private String extractEmailFromAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                int mod = parts[1].length() % 4;
                String padded = mod > 0 ? parts[1] + "=".repeat(4 - mod) : parts[1];
                byte[] decoded = java.util.Base64.getUrlDecoder().decode(padded);
                Map<?, ?> claims = Constants.GSON.fromJson(new String(decoded), Map.class);
                if (claims.containsKey("preferred_username")) return (String) claims.get("preferred_username");
                if (claims.containsKey("upn")) return (String) claims.get("upn");
                if (claims.containsKey("email")) return (String) claims.get("email");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to extract email from access token: {}", e.getMessage());
        }
        return null;
    }

    private boolean hasUserAccess(GraphServiceClientProvider clientProvider) {
        try {
            clientProvider.getUserRequestBuilder(null, null).joinedTeams().buildRequest().get();
            return true;
        } catch (GraphServiceException cause) {
            LOGGER.error("User access verification failed. User does not have permission to access Microsoft Graph or " +
                    "authentication is invalid : {}", cause.getMessage(), cause);
            return false;
        } catch (Exception cause) {
            // Operator's own token may be expired during a per-user re-auth callback.
            // Throwing here would 500/502 the user who just successfully authenticated.
            // Log and allow — the operator's credential state is independent of this user's auth.
            LOGGER.warn("Operator access check threw non-Graph exception during callback — treating as accessible: {}",
                    cause.getMessage());
            return true;
        }
    }

    /**
     * Extracts the authContextId embedded in the OAuth state parameter, if present.
     * State shapes: {@code key#authContextId}, {@code wsContactId#userId#authContextId(#forwardPath)}.
     */
    private static String getAuthContextId(String state) {
        int authContextIdIndex = state.indexOf(AUTH_CONTEXT_ID);
        if (authContextIdIndex == -1) {
            return null;
        }
        String authContextIdToEnd = state.substring(authContextIdIndex);
        int hashIndex = authContextIdToEnd.indexOf("#");
        if (hashIndex != -1) {
            return authContextIdToEnd.substring(0, hashIndex);
        }
        return authContextIdToEnd;
    }

    /**
     * Builds the SDK {@link AuthConfig} for the given attributes.
     * Private (this extension's meaning: Client Credentials) never reaches the OAuth
     * callback flow, but callback code exchange always uses the Auth Code Public builder
     * regardless of stored auth type — see rationale in {@link #getAuthenticationResponseMessage}.
     */
    /**
     * Returns the refresh-token key of the previously-saved configuration that should be removed
     * when saving {@code current}, or {@code null} to keep it. A token is removed only when the
     * mode/credentials actually changed (different composite key) — re-saving the same mode keeps
     * the freshly authorized token. This guarantees at most one active refresh token per invoker,
     * so switching auth modes and back always re-prompts for sign-in instead of silently reusing a
     * stale token.
     */
    static String staleTokenKeyToRemove(TeamsApisAttributes previous, TeamsApisAttributes current) {
        if (previous == null || previous.getEmail() == null || previous.getClientId() == null) {
            return null;
        }
        String prevKey = MicrosoftAuthSdk.buildTokenKey(
                previous.getEmail(), previous.getClientId(), previous.getAuthType());
        String newKey = (current != null && current.getEmail() != null && current.getClientId() != null)
                ? MicrosoftAuthSdk.buildTokenKey(current.getEmail(), current.getClientId(), current.getAuthType())
                : null;
        if (prevKey == null || prevKey.equals(newKey)) {
            return null;
        }
        return prevKey;
    }

    private static AuthConfig toAuthConfig(TeamsApisAttributes attributes) {
        return AuthConfig.publicAuth()
                .clientId(attributes.getClientId())
                .clientSecret(attributes.getClientSecret())
                .tenantId(attributes.getTenantId())
                .email(attributes.getEmail())
                .callbackUrl(attributes.getCallbackEndPoint())
                .scopes(List.of(attributes.getScopeString().split(Constants.SCOPE_SEPARATOR)))
                .build();
    }

    @GET
    @Path("/teams/authentication/{subPath:.*}")
    public InputStream customTabs(@PathParam("subPath") String subPath) {
        // Serve pre-built auth HTML from SDK JAR — no React build needed
        String html = patchedAuthHtml();
        return html == null ? null
                : new java.io.ByteArrayInputStream(html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static String patchedAuthHtml() {
        try (InputStream in = AuthenticationResource.class.getClassLoader().getResourceAsStream("auth/index.html")) {
            if (in == null) {
                return null;
            }
            // SDK 1.0.5's auth/index.html detects its extension id via /\/rest\/(\w+)\// and
            // falls back to 'outlook'. Our URLs use jaxrsId "teams-apis" (no /rest/ segment),
            // so the fallback must be 'teams' or the tab bootstraps as an unknown extension.
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("return m?m[1]:'outlook'", "return m?m[1]:'teams'");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @GET
    @Path("/teams/msauth/metadata/{extensionId}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getExtensionMetadata(@PathParam("extensionId") String extensionId) {
        // Initialize registry if not done yet and return metadata for this extension
        app.krista.sdk.msauth.extension.registry.ExtensionRegistry.initialize();
        return app.krista.sdk.msauth.extension.registry.ExtensionRegistry.get(extensionId)
                .map(m -> m.toUiJson())
                .orElse("{\"error\":\"Extension not found: " + extensionId + "\"}");
    }

    @POST
    @Path("/teams/saveCredentials")
    @Produces("text/plain")
    public String saveCredentials(com.google.gson.JsonObject authPayload) {
        auditService.startRequest();
        Map<String, String> meta = new HashMap<>();
        TeamsApisAttributes attributes;
        try {
            unmaskClientSecretIfNeeded(authPayload);
            attributes = TeamsApisAttributes.create(authPayload, baseRoutingUrl);
            if (attributes.getEmail() != null) meta.put("triggeredBy", attributes.getEmail());
        } catch (Exception cause) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "Save Credentials",
                    cause.getMessage(), cause.getClass().getSimpleName(), meta);
            throw cause;
        }
        try {
            // Mode switch (e.g. Quick Setup → own credentials): drop the previously-authorized
            // refresh token so only the current one is stored. Switching back later then requires
            // a fresh Authorize rather than silently reporting "connected" off a stale token.
            String staleKey = staleTokenKeyToRemove(teamsApisAttributeStore.load(invokerId), attributes);
            if (staleKey != null) {
                refreshTokenStore.remove(staleKey);
                LOGGER.info("saveCredentials: removed previous refresh token on mode switch, invokerId={}", invokerId);
            }
            boolean isSaved = teamsApisAttributeStore.save(attributes, invokerId);
            auditService.record(invokerId, isSaved ? "CATALOG_REQUEST_SUCCESS" : "CATALOG_REQUEST_FAILURE",
                    isSaved ? "success" : "failure", "Save Credentials",
                    isSaved ? "Configuration saved successfully" : "Failed to save configuration", null, meta);
            if (isSaved) {
                auditService.resetAuthCounters();
            }
            // The auth tab's doSave reads r.isSuccess — a bare "true"/"false" body parses as a
            // boolean and always lands in the failure branch.
            return GSON.toJson(new AuthenticationResponse(isSaved,
                    isSaved ? null : "Failed to save configuration", null));
        } catch (MustAuthorizeException e) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "Save Credentials",
                    "Authentication required — redirecting to sign-in", "MustAuthorizeException", meta);
            throw e;
        } catch (Exception e) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "Save Credentials",
                    e.getMessage(), e.getClass().getSimpleName(), meta);
            throw e;
        }
    }

    @POST
    @Path("/teams/testConnection")
    @Produces("text/plain")
    public String testConnection(com.google.gson.JsonObject authPayload) {
        long testStartTime = System.currentTimeMillis();
        auditService.startRequest();
        unmaskClientSecretIfNeeded(authPayload);
        TeamsApisAttributes teamsApisAttributes = TeamsApisAttributes.create(authPayload, baseRoutingUrl);
        LOGGER.info("TestConnection(API): START, email={}, authType={}, clientId={}, tenantId={}",
                teamsApisAttributes.getEmail(), teamsApisAttributes.getAuthType(), teamsApisAttributes.getClientId(), teamsApisAttributes.getTenantId());
        Map<String, String> meta = new HashMap<>();
        if (teamsApisAttributes.getEmail() != null) meta.put("triggeredBy", teamsApisAttributes.getEmail());
        String authContextId = null;
        String authUrl = null;
        try {
            // Save first so the OAuth callback can load the SAME attributes by authContextId —
            // the token exchange must use the exact OAuth client that built the auth URL.
            authContextId = providerFactory.createAttributes(teamsApisAttributes);
            GraphServiceClientProvider provider = providerFactory.create(authContextId);
            if (Constants.PRIVATE.equals(teamsApisAttributes.getAuthType())) {
                // Client Credentials (app-only): verify via /users/{email} — app-only tokens don't support /me
                provider.getGraphServiceClientForAdmin()
                        .users(teamsApisAttributes.getEmail())
                        .joinedTeams().buildRequest().get();
            } else {
                // Delegated (Auth Code Public/Private): verify via /me
                provider.getGraphServiceClientForAdmin()
                        .me().joinedTeams().buildRequest().get();
            }
            LOGGER.info("TestConnection(API): completed, success=true, email={}, durationMs={}",
                    teamsApisAttributes.getEmail(), System.currentTimeMillis() - testStartTime);
            auditService.record(invokerId, "CATALOG_REQUEST_SUCCESS", "success", "Test Connection",
                    "Connection verified for " + teamsApisAttributes.getEmail(), null, meta);
            return GSON.toJson(new AuthenticationResponse(true, null, null));
        } catch (MustAuthorizeException e) {
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "Test Connection",
                    "Authentication required — redirecting to sign-in", "MustAuthorizeException", meta);
            // The auth tab only opens the Microsoft popup when this response carries a url —
            // rethrowing reaches the page as an error body without one (no popup, dead end).
            String state = buildStateParameter(e, teamsApisAttributes);
            MicrosoftAuthSdk sdk = MicrosoftAuthSdk.builder()
                    .config(toAuthConfig(teamsApisAttributes))
                    .tokenStore(new InMemoryTokenStore()).build();
            authUrl = sdk.buildAuthorizationUrl(state);
            LOGGER.info("TestConnection(API): auth required, generated OAuth URL, state={}, durationMs={}",
                    state, System.currentTimeMillis() - testStartTime);
            return GSON.toJson(new AuthenticationResponse(false, e.getMessage(), authUrl));
        } catch (Exception e) {
            LOGGER.error("TestConnection(API): FAILED, email={}, error={}, durationMs={}",
                    teamsApisAttributes.getEmail(), e.getMessage(), System.currentTimeMillis() - testStartTime, e);
            auditService.record(invokerId, "CATALOG_REQUEST_FAILURE", "failure", "Test Connection",
                    e.getMessage(), e.getClass().getSimpleName(), meta);
            return GSON.toJson(new AuthenticationResponse(false, e.getMessage(), null));
        } finally {
            if (authUrl == null && authContextId != null) {
                // No OAuth round trip pending — test-connection attributes are temporary
                // probes; only Save persists configuration.
                teamsApisAttributeStore.remove(authContextId);
            }
        }
    }

    /**
     * Builds the OAuth state parameter from MustAuthorizeException details. Fields are looked up
     * by NAME, not index — the factory may emit them in any order, and MCP-shaped details put
     * CLIENT_USER_ID first. For Public auth the forward path is appended so the shared Krista
     * callback can route back to this extension.
     */
    static String buildStateParameter(MustAuthorizeException cause, TeamsApisAttributes attributes) {
        List<NamedValuedField> details = cause.getDetails();
        String userId = details.stream()
                .filter(f -> Objects.equals(f.getName(), Constants.USER_ID))
                .map(f -> (String) f.getValue())
                .findFirst()
                .orElseGet(() -> (String) details.get(0).getValue()); // legacy fallback
        Optional<String> authContextId = details.stream()
                .filter(f -> Objects.equals(f.getName(), Constants.AUTH_CONTEXT_ID))
                .map(f -> (String) f.getValue())
                .findFirst();
        Optional<String> clientUserId = details.stream()
                .filter(f -> Objects.equals(f.getName(), Constants.CLIENT_USER_ID))
                .map(f -> (String) f.getValue())
                .findFirst();

        String state;
        if (clientUserId.isPresent() && authContextId.isPresent()) {
            // 4-part MCP state: wsContactId#userId#authContextId(#forwardPath)
            state = clientUserId.get() + Constants.HASH + userId + Constants.HASH + authContextId.get();
        } else if (authContextId.isPresent()) {
            // 3-part: userId#authContextId(#forwardPath)
            state = userId + Constants.HASH + authContextId.get();
        } else {
            state = userId;
        }
        if (Constants.PUBLIC.equals(attributes.getAuthType())) {
            state += Constants.HASH + attributes.getForwardPath();
        }
        return state;
    }

    /**
     * If the incoming payload's clientSecret is the masked placeholder (or blank while stored
     * attributes already exist), replaces it in-place with the previously stored secret for this
     * invoker so saveCredentials/testConnection reuse the unchanged secret instead of persisting
     * the placeholder or wiping the secret out.
     */
    private void unmaskClientSecretIfNeeded(com.google.gson.JsonObject authPayload) {
        if (!authPayload.has(CLIENT_SECRET) || authPayload.get(CLIENT_SECRET).isJsonNull()) {
            return;
        }
        String incoming = authPayload.get(CLIENT_SECRET).getAsString();
        TeamsApisAttributes existing = teamsApisAttributeStore.load(invokerId);
        String resolved = resolveClientSecret(incoming, existing);
        if (resolved == null) {
            authPayload.remove(CLIENT_SECRET);
        } else {
            authPayload.addProperty(CLIENT_SECRET, resolved);
        }
    }

    /**
     * Pure decision logic for whether an incoming clientSecret from the UI represents a genuinely
     * new value or an "unchanged" round-trip of the masked placeholder / a blank field.
     * <p>
     * Behavior:
     * <ul>
     *   <li>incoming == {@link #MASKED_SECRET} and existing attrs have a stored secret → reuse stored secret.</li>
     *   <li>incoming is blank and existing attrs have a stored secret → reuse stored secret
     *       (the UI form re-submitted without touching the masked field).</li>
     *   <li>incoming is the masked placeholder but there is nothing stored to reuse → fail safe by
     *       NOT persisting the literal placeholder string as a real secret; return null instead.</li>
     *   <li>Any other non-blank incoming value → treated as a genuinely new secret, returned as-is.</li>
     * </ul>
     */
    static String resolveClientSecret(String incoming, TeamsApisAttributes existing) {
        String existingSecret = existing != null ? existing.getClientSecret() : null;
        boolean isPlaceholder = MASKED_SECRET.equals(incoming);
        boolean isBlank = incoming == null || incoming.isBlank();

        if (isPlaceholder || isBlank) {
            if (existingSecret != null && !existingSecret.isBlank()) {
                return existingSecret;
            }
            // Nothing stored to fall back to. Fail safe: never let the literal placeholder
            // (or a blank) be persisted/used as a real secret.
            return isPlaceholder ? null : incoming;
        }
        return incoming;
    }

    @GET
    @Path("/teams/getCredentials")
    @Produces("text/plain")
    public String getCredentials(@QueryParam(AUTH_TYPE) String authType,
                                 @QueryParam("authFlow") String authFlow) {
        LOGGER.info("GetCredentials: START, requestedAuthType={}, authFlow={}, invokerId={}", authType, authFlow, invokerId);
        final TeamsApisAttributes loadedAttributes = teamsApisAttributeStore.load(invokerId);
        if (loadedAttributes == null || authType == null) {
            LOGGER.info("GetCredentials: no stored attributes or authType null, invokerId={}, attrsFound={}, authType={}",
                    invokerId, loadedAttributes != null, authType);
            return "";
        }
        LOGGER.info("GetCredentials: loaded attrs, storedAuthType={}, email={}, clientId={}, publicAuthMode={}, privateAuthMode={}",
                loadedAttributes.getAuthType(), loadedAttributes.getEmail(), loadedAttributes.getClientId(),
                loadedAttributes.getPublicAuthMode(), loadedAttributes.getPrivateAuthMode());

        if (!authType.equals(loadedAttributes.getAuthType())) {
            return "";
        } else if (Constants.PUBLIC.equals(authType) && authFlow != null) {
            // For Public auth: match publicAuthMode to isolate Quick Setup (krista) from User Auth (own)
            String storedMode = loadedAttributes.getPublicAuthMode();
            boolean isOwnRequest = "AUTH_CODE_PRIVATE".equals(authFlow);
            boolean isOwnStored = "own".equals(storedMode);
            if (isOwnRequest != isOwnStored) return "";
        }

        Map<String, Object> attributesMap = loadedAttributes.toMap();
        // Determine whether to expose credentials based on stored auth mode
        boolean isKristaManaged = (Constants.PRIVATE.equals(loadedAttributes.getAuthType())
                && "krista".equals(loadedAttributes.getPrivateAuthMode()))
                || (Constants.PUBLIC.equals(loadedAttributes.getAuthType())
                && "krista".equals(loadedAttributes.getPublicAuthMode()));
        if (isKristaManaged) {
            // Krista Managed: don't expose internal credentials
            attributesMap.remove(CLIENT_ID);
            attributesMap.remove(CLIENT_SECRET);
        } else {
            // Private or Public+own credentials: never round-trip the plaintext secret to the UI.
            // Substitute a fixed placeholder when a secret is stored; leave absent otherwise.
            Object secret = attributesMap.get(CLIENT_SECRET);
            if (secret instanceof String && !((String) secret).isEmpty()) {
                attributesMap.put(CLIENT_SECRET, MASKED_SECRET);
            } else {
                attributesMap.remove(CLIENT_SECRET);
            }
        }
        return GSON.toJson(attributesMap);
    }

    @GET
    @Path("/teams/getAuthKey")
    @Produces("text/plain")
    public String getAuthKey() {
        LOGGER.debug("Loading stored authentication key for invokerId={}", invokerId);
        final TeamsApisAttributes attributes = teamsApisAttributeStore.load(invokerId);
        if (attributes != null) {
            final String authType = attributes.getAuthType();
            return GSON.toJson(java.util.Objects.requireNonNullElse(authType, Constants.PUBLIC));
        }
        return "";
    }
}
