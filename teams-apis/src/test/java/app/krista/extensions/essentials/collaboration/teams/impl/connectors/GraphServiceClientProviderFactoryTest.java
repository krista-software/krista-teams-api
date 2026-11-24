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
import app.krista.ksdk.accounts.Account;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.model.field.NamedValuedField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers every branch of {@link GraphServiceClientProviderFactory}: the operator-shared path,
 * the per-user (MCP) path with its own consent-triggering flow, and both {@code create} overloads.
 */
class GraphServiceClientProviderFactoryTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";

    private KeyValueStore keyValueStore;
    private TeamsApisAttributeStore attributeStore;
    private RequestContext requestContext;
    private AuthorizationContext authorizationContext;
    private Invoker invoker;
    private TeamsAuditService auditService;
    private GraphServiceClientProviderFactory factory;

    @BeforeEach
    void setUp() {
        keyValueStore = mock(KeyValueStore.class);
        attributeStore = mock(TeamsApisAttributeStore.class);
        requestContext = mock(RequestContext.class);
        authorizationContext = mock(AuthorizationContext.class);
        auditService = mock(TeamsAuditService.class);

        invoker = mock(Invoker.class);
        RoutingInfo routingInfo = mock(RoutingInfo.class);
        when(invoker.getRoutingInfo()).thenReturn(routingInfo);
        when(routingInfo.getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE)).thenReturn(ROUTING);

        factory = new GraphServiceClientProviderFactory(
                keyValueStore, attributeStore, requestContext, authorizationContext, invoker, auditService);
    }

    private static TeamsApisAttributes attrs(String clientId, String email) {
        return new TeamsApisAttributes(clientId, "secret", "tenant", email, Constants.PUBLIC, ROUTING);
    }

    private static NamedValuedField findDetail(List<NamedValuedField> details, String name) {
        return details.stream().filter(d -> d.getName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("missing detail: " + name));
    }

    // ── createAttributes ─────────────────────────────────────────────────────

    @Test
    void createAttributesDelegatesToTheAttributeStoreAndReturnsItsResult() {
        TeamsApisAttributes attributes = attrs("cid", "user@example.com");
        when(attributeStore.save(attributes)).thenReturn("auth-context-42");

        String result = factory.createAttributes(attributes);

        assertEquals("auth-context-42", result);
        verify(attributeStore).save(attributes);
    }

    // ── create(TeamsApisAttributes) ─────────────────────────────────────────

    @Test
    void createFromExplicitAttributesBuildsAProviderWithThoseAttributes() {
        TeamsApisAttributes attributes = attrs("cid", "user@example.com");

        GraphServiceClientProvider provider = factory.create(attributes);

        assertSame(attributes, provider.getTeamsAttributes());
    }

    // ── create(String authContextId) ────────────────────────────────────────

    @Test
    void createFromAuthContextIdBuildsAProviderWhenAttributesExist() {
        TeamsApisAttributes attributes = attrs("cid", "user@example.com");
        when(attributeStore.load("auth-context-1")).thenReturn(attributes);

        GraphServiceClientProvider provider = factory.create("auth-context-1");

        assertSame(attributes, provider.getTeamsAttributes());
    }

    @Test
    void createFromAuthContextIdThrowsWithBothUserIdAndAuthContextIdDetailsWhenMissing() {
        when(attributeStore.load("missing-context")).thenReturn(null);

        MustAuthorizeException ex = assertThrows(MustAuthorizeException.class,
                () -> factory.create("missing-context"));

        assertEquals("No credentials configured. Please authenticate via the Authentication tab.", ex.getMessage());
        assertEquals(2, ex.getDetails().size());
        assertEquals("missing-context", findDetail(ex.getDetails(), "userId").getValue());
        assertEquals("missing-context", findDetail(ex.getDetails(), "authContextId").getValue());
    }

    // ── create() — delegates to create(requestContext) ──────────────────────

    @Test
    void createWithNoArgsUsesTheInjectedRequestContext() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(requestContext.getInvokerId()).thenReturn("invoker-1");
        TeamsApisAttributes attributes = attrs("cid", "user@example.com");
        when(attributeStore.load("invoker-1")).thenReturn(attributes);

        GraphServiceClientProvider provider = factory.create();

        assertSame(attributes, provider.getTeamsAttributes());
    }

    // ── create(RequestContext) — operator-shared path ───────────────────────

    @Test
    void createForOperatorPathBuildsAProviderWhenAttributesExist() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(requestContext.getInvokerId()).thenReturn("invoker-2");
        TeamsApisAttributes attributes = attrs("cid", "user@example.com");
        when(attributeStore.load("invoker-2")).thenReturn(attributes);

        GraphServiceClientProvider provider = factory.create(requestContext);

        assertSame(attributes, provider.getTeamsAttributes());
    }

    @Test
    void createForOperatorPathThrowsWithOnlyUserIdDetailWhenAttributesAreMissing() {
        when(requestContext.invokeAsUser()).thenReturn(false);
        when(requestContext.getInvokerId()).thenReturn("invoker-3");
        when(attributeStore.load("invoker-3")).thenReturn(null);

        MustAuthorizeException ex = assertThrows(MustAuthorizeException.class,
                () -> factory.create(requestContext));

        assertEquals("No credentials configured. Please authenticate via the Authentication tab.", ex.getMessage());
        assertEquals(1, ex.getDetails().size());
        assertEquals("invoker-3", findDetail(ex.getDetails(), "userId").getValue());
    }

    // ── create(RequestContext) — per-user (MCP) path ─────────────────────────

    @Test
    void createForPerUserPathBuildsAProviderWhenPerUserAttributesExist() {
        when(requestContext.invokeAsUser()).thenReturn(true);
        when(requestContext.getInvokerId()).thenReturn("invoker-4");
        Account account = mock(Account.class);
        when(account.getAccountId()).thenReturn("acct-1");
        when(authorizationContext.getAuthorizedAccount()).thenReturn(account);
        TeamsApisAttributes attributes = attrs("cid", "user@example.com");
        when(attributeStore.load("acct-1" + Constants.PER_USER_ATTRS_SUFFIX)).thenReturn(attributes);

        GraphServiceClientProvider provider = factory.create(requestContext);

        assertSame(attributes, provider.getTeamsAttributes());
    }

    @Test
    void createForPerUserPathTriggersConsentWithAllThreeDetailsWhenNoPerUserAttributesExist() {
        when(requestContext.invokeAsUser()).thenReturn(true);
        when(requestContext.getInvokerId()).thenReturn("invoker-5");
        Account account = mock(Account.class);
        when(account.getAccountId()).thenReturn("acct-2");
        when(authorizationContext.getAuthorizedAccount()).thenReturn(account);
        when(attributeStore.load("acct-2" + Constants.PER_USER_ATTRS_SUFFIX)).thenReturn(null);
        when(attributeStore.save(any(TeamsApisAttributes.class))).thenReturn("saved-auth-context-9");

        MustAuthorizeException ex = assertThrows(MustAuthorizeException.class,
                () -> factory.create(requestContext));

        assertEquals("No credentials configured. Please authenticate via the Authentication tab.", ex.getMessage());
        assertEquals(3, ex.getDetails().size());
        assertEquals("acct-2", findDetail(ex.getDetails(), "clientUserId").getValue());
        assertEquals("invoker-5", findDetail(ex.getDetails(), "userId").getValue());
        assertEquals("saved-auth-context-9", findDetail(ex.getDetails(), "authContextId").getValue());

        org.mockito.ArgumentCaptor<TeamsApisAttributes> captor = org.mockito.ArgumentCaptor.forClass(TeamsApisAttributes.class);
        verify(attributeStore).save(captor.capture());
        assertEquals(Constants.PUBLIC, captor.getValue().getAuthType());
    }

    @Test
    void createForPerUserPathWrapsUnexpectedFailuresDuringConsentSetup() {
        when(requestContext.invokeAsUser()).thenReturn(true);
        when(requestContext.getInvokerId()).thenReturn("invoker-6");
        Account account = mock(Account.class);
        when(account.getAccountId()).thenReturn("acct-3");
        when(authorizationContext.getAuthorizedAccount()).thenReturn(account);
        when(attributeStore.load("acct-3" + Constants.PER_USER_ATTRS_SUFFIX)).thenReturn(null);
        when(attributeStore.save(any(TeamsApisAttributes.class))).thenThrow(new RuntimeException("store unavailable"));

        MustAuthorizeException ex = assertThrows(MustAuthorizeException.class,
                () -> factory.create(requestContext));

        assertEquals("Per-user authentication setup failed. Please try again.", ex.getMessage());
        assertTrue(ex.getDetails().isEmpty());
    }

    // ── getAttributeStore ─────────────────────────────────────────────────────

    @Test
    void getAttributeStoreReturnsTheInjectedStore() {
        assertSame(attributeStore, factory.getAttributeStore());
    }
}
