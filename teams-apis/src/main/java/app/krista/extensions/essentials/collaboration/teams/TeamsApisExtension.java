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
import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extension.util.InvokerAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.sdk.msauth.MicrosoftAuthSdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

@Java(version = Java.Version.JAVA_21)
@Field.Text(value = Constants.CLIENT_ID)
@Field.Text(value = Constants.CLIENT_SECRET, isSecured = true)
@Field.Text(value = Constants.TENANT_ID, isSecured = true)
@Field.Text(value = Constants.EMAIL)
@StaticResource(path = "docs", file = "docs")
@ChangeLog(file = "docs/pages/release-notes.md")
@Extension(version = "4.0.2", jaxrsId = Constants.TEAMS_JAXRS_ID, name = "Teams APIs",
        description = "Krista integration with Microsoft Teams via Microsoft Graph API",
        implementationModel = Extension.ImplementationModel.Krista_4_O)
@Containerize(baseImageVersion = "3.6.2-sp3")
public class TeamsApisExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamsApisExtension.class);

    private final String routingUrl;
    private final Invoker invoker;
    private final TeamsApisAttributeStore attributeStore;
    private final TeamsApisRequestAuthenticator requestAuthenticator;
    private final GraphServiceClientProviderFactory providerFactory;
    private final RefreshTokenStore refreshTokenStore;

    @Inject
    public TeamsApisExtension(Invoker invoker, TeamsApisAttributeStore attributeStore,
                              AuthorizationContext authorizationContext,
                              GraphServiceClientProviderFactory providerFactory,
                              RefreshTokenStore refreshTokenStore) {
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.invoker = invoker;
        this.attributeStore = attributeStore;
        this.providerFactory = providerFactory;
        this.refreshTokenStore = refreshTokenStore;
        this.requestAuthenticator = new TeamsApisRequestAuthenticator(attributeStore, invoker.getInvokerId(),
                authorizationContext, routingUrl);
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of(
                "Authentication", "teams-apis/teams/authentication/",
                "Documentation", "static/docs",
                "Teams Audit Dashboard", "teams-apis/teams/audit/",
                "AI Assistant", "teams-apis/teams/mcp-setup");
    }

    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        String invokerId = invoker.getInvokerId();
        TeamsApisAttributes attrs = attributeStore.load(invokerId);
        if (attrs == null || attrs.getClientId() == null) {
            throw new IllegalArgumentException(
                    "Teams is not configured yet. Open the 'Authentication' tab, enter your credentials, and click 'Authorize'.");
        }
        try {
            // Uses the stored attributes + refresh token; MsAuthGraphClientProvider throws
            // MustAuthorizeException when no valid refresh token exists for the composite key.
            var graphClient = providerFactory.create(invokerId).getGraphServiceClientForAdmin();
            if (Constants.PRIVATE.equals(attrs.getAuthType())) {
                // App-only (client credentials) has no user context — verify via /users/{email}
                graphClient.users(attrs.getEmail()).joinedTeams().buildRequest().get();
            } else {
                // Delegated (Public) — verify via /me, which requires a saved refresh token
                graphClient.me().joinedTeams().buildRequest().get();
            }
            LOGGER.info("TestConnection(invoker): success, invokerId={}, authType={}", invokerId, attrs.getAuthType());
        } catch (MustAuthorizeException e) {
            LOGGER.info("TestConnection(invoker): not authorized, invokerId={}", invokerId);
            throw new IllegalArgumentException(
                    "Not authorized yet. Open the 'Authentication' tab and click 'Authorize' to sign in with your "
                            + "Microsoft account, then test again.");
        } catch (Exception e) {
            LOGGER.error("TestConnection(invoker): failed, invokerId={}, error={}", invokerId, e.getMessage(), e);
            throw new IllegalArgumentException("Test connection failed: " + e.getMessage());
        }
    }

    /**
     * Migrates pre-3.0 invoker attributes (raw clientId/clientSecret/tenantId/email extension
     * fields) into the SDK attribute store so already-configured invokers keep working.
     */
    @InvokerRequest(InvokerRequest.Type.INVOKER_LOADED)
    public void load() {
        String invokerId = invoker.getInvokerId();
        if (attributeStore.load(invokerId) != null) {
            return; // already on the new store
        }
        Map<String, Object> legacy = invoker.getAttributes();
        String clientId = InvokerAttributes.getStringOrNull(legacy, Constants.CLIENT_ID);
        String clientSecret = InvokerAttributes.getStringOrNull(legacy, Constants.CLIENT_SECRET);
        String tenantId = InvokerAttributes.getStringOrNull(legacy, Constants.TENANT_ID);
        String email = InvokerAttributes.getStringOrNull(legacy, Constants.EMAIL);
        if (clientId == null || clientSecret == null || tenantId == null) {
            LOGGER.info("No legacy credentials to migrate, invokerId={}", invokerId);
            return;
        }
        // The old 2.0.9 extension used a delegated auth-code flow with the customer's own
        // Azure app (user sign-in), not app-only client credentials. Map legacy credentials
        // to Public+own so call semantics are preserved, rather than silently switching to
        // Private (client-credentials/app-only).
        TeamsApisAttributes migrated = new TeamsApisAttributes(clientId, clientSecret, tenantId, email,
                Constants.PUBLIC, routingUrl, "own", null);
        attributeStore.save(migrated, invokerId);

        // Re-key the legacy refresh token so the migrated connection works WITHOUT re-authorizing.
        // The old 2.0.9 delegated flow stored the raw refresh token under the bare email; the SDK
        // looks it up under buildTokenKey(email, clientId, authType). Copy it across (same pattern
        // as SharePoint). The migrated attrs' effective clientId is used so the key matches lookup.
        if (email != null) {
            String legacyToken = refreshTokenStore.get(email);
            if (legacyToken != null && !legacyToken.isBlank()) {
                String newKey = MicrosoftAuthSdk.buildTokenKey(email, migrated.getClientId(), migrated.getAuthType());
                refreshTokenStore.put(newKey, legacyToken);
                LOGGER.info("Migrated legacy refresh token from email key to SDK key, invokerId={}. "
                        + "Connection should work without re-authorizing.", invokerId);
            } else {
                LOGGER.info("No legacy refresh token found under email key, invokerId={}. "
                        + "User will authorize once via the Authentication tab.", invokerId);
            }
        }
        LOGGER.info("Migrated legacy Teams credentials to SDK attribute store as Public+own, invokerId={}.", invokerId);
    }

}
