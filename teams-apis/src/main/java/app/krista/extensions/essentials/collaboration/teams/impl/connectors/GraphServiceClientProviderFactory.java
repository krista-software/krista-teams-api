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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.audit.TeamsAuditService;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
public class GraphServiceClientProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphServiceClientProviderFactory.class);

    private final KeyValueStore keyValueStore;
    private final TeamsApisAttributeStore teamsApisAttributeStore;
    private final RequestContext requestContext;
    private final AuthorizationContext authorizationContext;
    private final String routingUrl;
    private final TeamsAuditService auditService;

    @Inject
    public GraphServiceClientProviderFactory(KeyValueStore keyValueStore,
                                             TeamsApisAttributeStore teamsApisAttributeStore,
                                             RequestContext requestContext,
                                             AuthorizationContext authorizationContext,
                                             Invoker invoker,
                                             TeamsAuditService auditService) {
        this.keyValueStore = keyValueStore;
        this.teamsApisAttributeStore = teamsApisAttributeStore;
        this.requestContext = requestContext;
        this.authorizationContext = authorizationContext;
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.auditService = auditService;
    }

    public String createAttributes(TeamsApisAttributes attributes) {
        LOGGER.info("ProviderFactory.createAttributes: authType={}, email={}, clientId={}",
                attributes.getAuthType(), attributes.getEmail(), attributes.getClientId());
        return teamsApisAttributeStore.save(attributes);
    }

    private GraphServiceClientProvider buildProvider(TeamsApisAttributes attrs, String authContextId) {
        long startTime = System.currentTimeMillis();
        LOGGER.info("ProviderFactory.buildProvider: authContextId={}, authType={}, email={}, clientId={}, tenantId={}",
                authContextId, attrs.getAuthType(), attrs.getEmail(), attrs.getClientId(), attrs.getTenantId());
        GraphServiceClientProvider provider = new GraphServiceClientProvider(
                keyValueStore, attrs, requestContext, authorizationContext, authContextId);
        provider.setAuditService(auditService);
        LOGGER.info("ProviderFactory.buildProvider: completed, authContextId={}, durationMs={}",
                authContextId, System.currentTimeMillis() - startTime);
        return provider;
    }

    /**
     * Creates a provider from explicit attributes — for OAuth callback forcing default Public
     * on wsContact per-user paths.
     */
    public GraphServiceClientProvider create(TeamsApisAttributes attributes) {
        return buildProvider(attributes, null);
    }

    public GraphServiceClientProvider create(String authContextId) {
        LOGGER.info("ProviderFactory.create(authContextId): authContextId={}", authContextId);
        TeamsApisAttributes attrs = teamsApisAttributeStore.load(authContextId);
        if (attrs == null) {
            LOGGER.info("ProviderFactory.create(authContextId): NO attributes found, throwing MustAuthorizeException, authContextId={}", authContextId);
            List<NamedValuedField> details = new ArrayList<>();
            details.add(new NamedValuedField("userId", "Text", authContextId, new HashMap<>(), new HashMap<>()));
            details.add(new NamedValuedField("authContextId", "Text", authContextId, new HashMap<>(), new HashMap<>()));
            throw new MustAuthorizeException("No credentials configured. Please authenticate via the Authentication tab.", details);
        }
        LOGGER.info("ProviderFactory.create(authContextId): attributes found, authContextId={}, authType={}, email={}",
                authContextId, attrs.getAuthType(), attrs.getEmail());
        return buildProvider(attrs, authContextId);
    }

    public GraphServiceClientProvider create() {
        return create(requestContext);
    }

    public GraphServiceClientProvider create(RequestContext context) {
        long startTime = System.currentTimeMillis();
        boolean isPerUser = context.invokeAsUser();
        LOGGER.info("ProviderFactory.create(context): invokeAsUser={}, invokerId={}", isPerUser, context.getInvokerId());

        // Rule 2: invokeAsUser check MUST happen FIRST — before any read of operator state.
        if (isPerUser) {
            return createPerUserProvider(context);
        }

        // Operator-shared path: keyed by invokerId
        String key = context.getInvokerId();
        TeamsApisAttributes attrs = teamsApisAttributeStore.load(key);
        if (attrs != null) {
            LOGGER.info("ProviderFactory.create(context): operator path, found attrs, invokerId={}, authType={}, email={}, durationMs={}",
                    key, attrs.getAuthType(), attrs.getEmail(), System.currentTimeMillis() - startTime);
            return buildProvider(attrs, null);
        }

        LOGGER.info("ProviderFactory.create(context): operator path, NO attrs found, throwing MustAuthorizeException, invokerId={}", key);
        List<NamedValuedField> details = new ArrayList<>();
        details.add(new NamedValuedField("userId", "Text", key, new HashMap<>(), new HashMap<>()));
        throw new MustAuthorizeException("No credentials configured. Please authenticate via the Authentication tab.", details);
    }

    /**
     * Per-user (MCP) auth path — completely isolated from operator state.
     * Looks up credentials by the user's wsContact accountId (Rule 1: hermetic keyspace).
     * If no per-user credentials exist, throws MustAuthorizeException to trigger consent (Rule 3).
     */
    private GraphServiceClientProvider createPerUserProvider(RequestContext context) {
        long startTime = System.currentTimeMillis();
        String wsContactId = authorizationContext.getAuthorizedAccount().getAccountId();
        String attrsKey = wsContactId + Constants.PER_USER_ATTRS_SUFFIX;
        LOGGER.info("ProviderFactory.createPerUserProvider: START, wsContactId={}, attrsKey={}, invokerId={}",
                wsContactId, attrsKey, context.getInvokerId());
        TeamsApisAttributes attrs = teamsApisAttributeStore.load(attrsKey);
        if (attrs != null) {
            LOGGER.info("ProviderFactory.createPerUserProvider: found per-user attrs, wsContactId={}, email={}, clientId={}, authType={}, durationMs={}",
                    wsContactId, attrs.getEmail(), attrs.getClientId(), attrs.getAuthType(), System.currentTimeMillis() - startTime);
            return buildProvider(attrs, null);
        }

        // No per-user credentials — trigger OAuth consent flow
        LOGGER.info("ProviderFactory.createPerUserProvider: NO per-user attrs, triggering OAuth consent, wsContactId={}", wsContactId);
        try {
            TeamsApisAttributes publicAttrs = new TeamsApisAttributes(
                    null, null, null, null, Constants.PUBLIC, routingUrl);
            String authContextId = teamsApisAttributeStore.save(publicAttrs);
            LOGGER.info("ProviderFactory.createPerUserProvider: saved temp public attrs, authContextId={}, wsContactId={}, durationMs={}",
                    authContextId, wsContactId, System.currentTimeMillis() - startTime);
            List<NamedValuedField> details = new ArrayList<>();
            details.add(new NamedValuedField("clientUserId", "Text", wsContactId, new HashMap<>(), new HashMap<>()));
            details.add(new NamedValuedField("userId", "Text", context.getInvokerId(), new HashMap<>(), new HashMap<>()));
            details.add(new NamedValuedField("authContextId", "Text", authContextId, new HashMap<>(), new HashMap<>()));
            throw new MustAuthorizeException("No credentials configured. Please authenticate via the Authentication tab.", details);
        } catch (MustAuthorizeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("ProviderFactory.createPerUserProvider: FAILED, wsContactId={}, error={}, durationMs={}",
                    wsContactId, e.getMessage(), System.currentTimeMillis() - startTime, e);
            throw new MustAuthorizeException("Per-user authentication setup failed. Please try again.", new ArrayList<>());
        }
    }

    /**
     * Creates a provider that prefers per-user (wsContact) credentials over operator credentials,
     * but falls back to operator silently if no per-user attrs exist or no wsContact is available.
     * Used exclusively for group chat read operations where the user must be a member of the chat.
     */
    public GraphServiceClientProvider createPreferringPerUser() {
        try {
            String wsContactId = authorizationContext.getAuthorizedAccount().getAccountId();
            String attrsKey = wsContactId + Constants.PER_USER_ATTRS_SUFFIX;
            TeamsApisAttributes perUserAttrs = teamsApisAttributeStore.load(attrsKey);
            if (perUserAttrs != null) {
                LOGGER.info("ProviderFactory.createPreferringPerUser: found per-user attrs for wsContact={}, using per-user credentials", wsContactId);
                return buildProvider(perUserAttrs, null);
            }
            LOGGER.info("ProviderFactory.createPreferringPerUser: no per-user attrs for wsContact={}, falling back to operator", wsContactId);
        } catch (Exception e) {
            LOGGER.info("ProviderFactory.createPreferringPerUser: no wsContact available ({}), falling back to operator", e.getMessage());
        }
        return create(requestContext);
    }

    /**
     * Convenience method that acquires a Graph client preferring per-user (wsContact) credentials,
     * falling back to operator silently. Centralises the repeated three-line acquisition pattern
     * used across ChannelService, TeamService, and ChatService.
     */
    public GraphServiceClient<Request> getPreferringPerUserGraphClient() {
        try {
            return createPreferringPerUser().getGraphServiceClientForUser(null, null);
        } catch (IOException cause) {
            throw new RuntimeException("We couldn't acquire a Microsoft Graph client. Please try again.", cause);
        }
    }

    public TeamsApisAttributeStore getAttributeStore() {
        return teamsApisAttributeStore;
    }
}
