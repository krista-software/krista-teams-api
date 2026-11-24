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

package app.krista.extensions.essentials.collaboration.teams.mcp;

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.model.base.FreeForm;
import app.krista.sdk.msauth.extension.mcp.AnthropicApiClient;
import app.krista.sdk.msauth.extension.mcp.DefaultMsAuthBridge;
import app.krista.sdk.msauth.extension.mcp.GeminiApiClient;
import app.krista.sdk.msauth.extension.mcp.OpenAiApiClient;
import app.krista.sdk.msauth.token.ITokenStore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises every private helper of {@link TeamsMcpQueryService} directly via reflection.
 *
 * <p>{@link TeamsMcpQueryService#query(String)} composes these helpers behind real Microsoft
 * Graph/AAD network calls (token acquisition, Graph HTTP calls), so driving every branch solely
 * through {@code query()} would require either live network access or mocking deep into the
 * microsoft-auth-sdk internals. Instead each private method is invoked directly with
 * {@code setAccessible(true)}, which JaCoCo counts identically to an in-process call — this is
 * the only network-free way to reach these branches.</p>
 */
class TeamsMcpQueryServiceInternalsTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";
    private static final Gson GSON = new Gson();

    private RefreshTokenStore refreshTokenStore;
    private Invoker invoker;
    private TeamsApisAttributeStore attributeStore;
    private TeamsMcpQueryService service;

    @BeforeEach
    void setUp() {
        refreshTokenStore = mock(RefreshTokenStore.class);
        invoker = mock(Invoker.class);
        when(invoker.getInvokerId()).thenReturn("inv-1");
        attributeStore = mock(TeamsApisAttributeStore.class);
        when(attributeStore.load("inv-1")).thenReturn(publicAttrs());
        service = new TeamsMcpQueryService(refreshTokenStore, invoker, attributeStore);
    }

    private static TeamsApisAttributes publicAttrs() {
        return new TeamsApisAttributes("cid", "secret", "tenant", "user@example.com", Constants.PUBLIC, ROUTING);
    }

    private static TeamsApisAttributes privateAttrs() {
        return new TeamsApisAttributes("cid", "secret", "tenant", "user@example.com", Constants.PRIVATE, ROUTING);
    }

    // ── reflection helpers ───────────────────────────────────────────────────

    private static Object invokeStatic(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        return invoke(null, name, paramTypes, args);
    }

    private Object invokeInstance(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        return invoke(service, name, paramTypes, args);
    }

    private static Object invoke(Object target, String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = TeamsMcpQueryService.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            if (e.getCause() instanceof Exception ex) throw ex;
            throw e;
        }
    }

    private static WebTarget stubbedTarget(Client client, int status, String body) {
        WebTarget target = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(target.request(anyString())).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.get()).thenReturn(response);
        when(builder.post(any())).thenReturn(response);
        when(response.getStatus()).thenReturn(status);
        when(response.readEntity(String.class)).thenReturn(body);
        when(client.target(anyString())).thenReturn(target);
        return target;
    }

    private static void routeUrlContaining(Client client, String urlSubstring, int status, String body) {
        WebTarget target = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(target.request(anyString())).thenReturn(builder);
        when(builder.header(anyString(), any())).thenReturn(builder);
        when(builder.get()).thenReturn(response);
        when(response.getStatus()).thenReturn(status);
        when(response.readEntity(String.class)).thenReturn(body);
        when(client.target(org.mockito.Mockito.<String>argThat(u -> u != null && u.contains(urlSubstring))))
                .thenReturn(target);
    }

    // ── protected overrides ──────────────────────────────────────────────────

    @Test
    void productConfigAndJaxrsIdAreTeamsSpecific() {
        assertTrue(service.getProductConfig() instanceof TeamsMcpProductConfig);
        assertEquals("teams", service.getJaxrsId());
    }

    // ── currentAttributes / currentAuthBridge ───────────────────────────────

    @Test
    void currentAttributesReloadsFromTheStoreEveryCall() throws Exception {
        TeamsApisAttributes attrs = publicAttrs();
        when(attributeStore.load("inv-1")).thenReturn(attrs);
        assertSame(attrs, invokeInstance("currentAttributes", new Class<?>[]{}));
    }

    @Test
    void currentAuthBridgeBuildsAWorkingBridgeFromCurrentAttributes() throws Exception {
        TeamsApisAttributes attrs = privateAttrs();
        when(attributeStore.load("inv-1")).thenReturn(attrs);
        Object bridge = invokeInstance("currentAuthBridge", new Class<?>[]{});
        assertTrue(bridge instanceof DefaultMsAuthBridge);
        // Private mode with non-blank creds is authenticated without any network call.
        assertTrue(((DefaultMsAuthBridge) bridge).isAuthenticated());
    }

    // ── resolvedPath ─────────────────────────────────────────────────────────

    @Test
    void resolvedPathPassesThroughWhenAttributesAreMissing() throws Exception {
        when(attributeStore.load("inv-1")).thenReturn(null);
        assertEquals("/me/joinedTeams", invokeInstance("resolvedPath", new Class<?>[]{String.class}, "/me/joinedTeams"));
    }

    @Test
    void resolvedPathPassesThroughForPublicAuth() throws Exception {
        when(attributeStore.load("inv-1")).thenReturn(publicAttrs());
        assertEquals("/me/joinedTeams", invokeInstance("resolvedPath", new Class<?>[]{String.class}, "/me/joinedTeams"));
    }

    @Test
    void resolvedPathRewritesMeToUsersEmailForPrivateAuth() throws Exception {
        when(attributeStore.load("inv-1")).thenReturn(privateAttrs());
        assertEquals("/users/user@example.com/joinedTeams",
                invokeInstance("resolvedPath", new Class<?>[]{String.class}, "/me/joinedTeams"));
        assertEquals("/users/user@example.com",
                invokeInstance("resolvedPath", new Class<?>[]{String.class}, "/me"));
    }

    @Test
    void resolvedPathLeavesNonMePathsUnchangedForPrivateAuth() throws Exception {
        when(attributeStore.load("inv-1")).thenReturn(privateAttrs());
        assertEquals("/teams/{team-id}/channels",
                invokeInstance("resolvedPath", new Class<?>[]{String.class}, "/teams/{team-id}/channels"));
    }

    // ── stripQuery ───────────────────────────────────────────────────────────

    @Test
    void stripQueryRemovesTrailingQueryString() throws Exception {
        assertEquals("/me/chats", invokeStatic("stripQuery", new Class<?>[]{String.class}, "/me/chats?$top=10"));
        assertEquals("/me/chats", invokeStatic("stripQuery", new Class<?>[]{String.class}, "/me/chats"));
    }

    // ── graphGetJson ─────────────────────────────────────────────────────────

    @Test
    void graphGetJsonParsesSuccessfulResponse() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 200, "{\"value\":[]}");

            Object result = invokeInstance("graphGetJson",
                    new Class<?>[]{String.class, String.class}, "https://graph.microsoft.com/v1.0/me/joinedTeams", "Bearer t");
            assertTrue(result instanceof JsonObject);
            assertTrue(((JsonObject) result).has("value"));
            verify(client).close();
        }
    }

    @Test
    void graphGetJsonThrowsOnNonSuccessStatus() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 404, "not found");

            assertThrows(IllegalStateException.class, () -> invokeInstance("graphGetJson",
                    new Class<?>[]{String.class, String.class}, "https://graph.microsoft.com/v1.0/me/joinedTeams", "Bearer t"));
            verify(client).close();
        }
    }

    // ── resolveNamedPlaceholders ─────────────────────────────────────────────

    @Test
    void resolveNamedPlaceholdersReturnsPathUnchangedWhenNoPlaceholders() throws Exception {
        String result = (String) invokeInstance("resolveNamedPlaceholders",
                new Class<?>[]{String.class, String.class, String.class, String.class},
                "/me/joinedTeams", null, null, "Bearer t");
        assertEquals("/me/joinedTeams", result);
    }

    @Test
    void resolveNamedPlaceholdersPicksFirstTeamWhenUnnamed() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}");

            String result = (String) invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", null, null, "Bearer t");
            assertEquals("/teams/t1/channels", result);
        }
    }

    @Test
    void resolveNamedPlaceholdersResolvesNamedTeamAndChannel() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"},"
                            + "{\"id\":\"t2\",\"displayName\":\"SimRamJalTest\"}]}");
            routeUrlContaining(client, "/channels", 200,
                    "{\"value\":[{\"id\":\"c9\",\"displayName\":\"Standup\"}]}");

            String result = (String) invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels/{channel-id}/messages", "SimRamJalTest", "Standup", "Bearer t");
            assertEquals("/teams/t2/channels/c9/messages", result);
        }
    }

    @Test
    void resolveNamedPlaceholdersSearchesAcrossTeamsForAnUnnamedChannel() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"},"
                            + "{\"id\":\"t2\",\"displayName\":\"Sales\"}]}");

            // t1's channels have no match; t2's channels have the match.
            WebTarget t1Channels = mock(WebTarget.class);
            Invocation.Builder t1Builder = mock(Invocation.Builder.class);
            Response t1Response = mock(Response.class);
            when(t1Channels.request(anyString())).thenReturn(t1Builder);
            when(t1Builder.header(anyString(), any())).thenReturn(t1Builder);
            when(t1Builder.get()).thenReturn(t1Response);
            when(t1Response.getStatus()).thenReturn(200);
            when(t1Response.readEntity(String.class)).thenReturn("{\"value\":[]}");

            WebTarget t2Channels = mock(WebTarget.class);
            Invocation.Builder t2Builder = mock(Invocation.Builder.class);
            Response t2Response = mock(Response.class);
            when(t2Channels.request(anyString())).thenReturn(t2Builder);
            when(t2Builder.header(anyString(), any())).thenReturn(t2Builder);
            when(t2Builder.get()).thenReturn(t2Response);
            when(t2Response.getStatus()).thenReturn(200);
            when(t2Response.readEntity(String.class)).thenReturn(
                    "{\"value\":[{\"id\":\"c5\",\"displayName\":\"Standup\"}]}");

            when(client.target(org.mockito.Mockito.<String>argThat(u -> u != null && u.contains("/teams/t1/channels"))))
                    .thenReturn(t1Channels);
            when(client.target(org.mockito.Mockito.<String>argThat(u -> u != null && u.contains("/teams/t2/channels"))))
                    .thenReturn(t2Channels);

            String result = (String) invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels/{channel-id}/messages", null, "Standup", "Bearer t");
            assertEquals("/teams/t2/channels/c5/messages", result);
        }
    }

    @Test
    void resolveNamedPlaceholdersThrowsWhenNamedTeamNotFound() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", "Ghost", null, "Bearer t"));
            assertTrue(ex.getMessage().contains("Could not find a team named 'Ghost'"));
        }
    }

    @Test
    void resolveNamedPlaceholdersThrowsWithChannelContextWhenNoTeamNamedAndChannelMissingEverywhere() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}");
            routeUrlContaining(client, "/channels", 200, "{\"value\":[]}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels/{channel-id}/messages", null, "Ghost", "Bearer t"));
            assertTrue(ex.getMessage().contains("Could not find a team containing a channel named 'Ghost'"));
        }
    }

    @Test
    void resolveNamedPlaceholdersThrowsGenericTeamMessageWhenNothingIsNamed() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200, "{\"value\":[]}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", null, null, "Bearer t"));
            assertEquals("Could not find a team among your joined teams.", ex.getMessage());
        }
    }

    @Test
    void resolveNamedPlaceholdersThrowsChannelInTeamMessageWhenPathHasNoTeamPlaceholder() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"Sales\"}]}");
            routeUrlContaining(client, "/channels", 200, "{\"value\":[]}");

            // Path only has {channel-id} — needsTeam is false, so the "team not found" branch is
            // skipped and the channel-specific message (with "in team") fires instead.
            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/channels/{channel-id}/messages", "Sales", "NoSuchChannel", "Bearer t"));
            assertEquals("Could not find a channel named 'NoSuchChannel' in team 'Sales'.", ex.getMessage());
        }
    }

    @Test
    void resolveNamedPlaceholdersThrowsPlainChannelMessageWhenNoTeamPlaceholderAndNoTeamNamed() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}");
            routeUrlContaining(client, "/channels", 200, "{\"value\":[]}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/channels/{channel-id}/messages", null, "Ghost", "Bearer t"));
            assertEquals("Could not find a channel named 'Ghost'.", ex.getMessage());
        }
    }

    @Test
    void resolveNamedPlaceholdersThrowsPlainChannelMessageWithEmptyNameWhenChannelNameIsNull() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}");
            routeUrlContaining(client, "/channels", 200, "{\"value\":[]}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/channels/{channel-id}/messages", null, null, "Bearer t"));
            assertEquals("Could not find a channel named ''.", ex.getMessage());
        }
    }

    @Test
    void resolveNamedPlaceholdersTreatsBlankTeamNameAsUnnamed() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}");

            String result = (String) invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", "   ", null, "Bearer t");
            assertEquals("/teams/t1/channels", result);
        }
    }

    @Test
    void resolveNamedPlaceholdersTreatsBlankChannelNameAsUnnamedInTheGenericTeamMessage() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200, "{\"value\":[]}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels/{channel-id}/messages", null, "   ", "Bearer t"));
            assertEquals("Could not find a team among your joined teams.", ex.getMessage());
        }
    }

    @Test
    void resolveNamedPlaceholdersSkipsNonObjectElementsAndHandlesMissingOrNonArrayValue() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);

            routeUrlContaining(client, "joinedTeams", 200,
                    "{\"value\":[\"not-an-object\",{\"id\":\"t1\",\"displayName\":\"General\"}]}");
            String result = (String) invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", null, null, "Bearer t");
            assertEquals("/teams/t1/channels", result);
        }

        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200, "{}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", null, null, "Bearer t"));
            assertEquals("Could not find a team among your joined teams.", ex.getMessage());
        }

        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            routeUrlContaining(client, "joinedTeams", 200, "{\"value\":\"oops\"}");

            Exception ex = assertThrows(IllegalStateException.class, () -> invokeInstance("resolveNamedPlaceholders",
                    new Class<?>[]{String.class, String.class, String.class, String.class},
                    "/teams/{team-id}/channels", null, null, "Bearer t"));
            assertEquals("Could not find a team among your joined teams.", ex.getMessage());
        }
    }

    // ── executeGraphCall ─────────────────────────────────────────────────────

    @Test
    void executeGraphCallRejectsDisallowedRequests() throws Exception {
        ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                "DELETE", "https://graph.microsoft.com/v1.0/me/messages/1", null, "Bearer t", "DELETE plan");
        assertEquals(ExtensionResponse.Result.FAILURE, resp.getResult());
        assertTrue(resp.getError().getErrorMessage().contains("This request cannot be executed"));
    }

    @Test
    void executeGraphCallReturnsRowsAndGeneratedPlanOnGetSuccess() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 200, "{\"id\":\"m1\",\"summary\":\"hi\"}");

            ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "GET", "https://graph.microsoft.com/v1.0/me/chats", null, "Bearer t", "GET /me/chats");
            assertEquals(ExtensionResponse.Result.SUCCESS, resp.getResult());
            assertEquals("GET /me/chats", resp.getResponseValue().get("generatedPlan"));
            List<?> rows = (List<?>) resp.getResponseValue().get("Response");
            assertEquals(1, rows.size());
            verify(client).close();
        }
    }

    @Test
    void executeGraphCallOmitsGeneratedPlanWhenNull() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 201, "{\"id\":\"created\"}");

            ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "POST", "https://graph.microsoft.com/v1.0/search/query", "{}", "Bearer t", null);
            assertEquals(ExtensionResponse.Result.SUCCESS, resp.getResult());
            assertFalse(resp.getResponseValue().containsKey("generatedPlan"));
        }
    }

    @Test
    void executeGraphCallTreats202And204AsNoBodySuccess() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 204, null);

            ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "POST", "https://graph.microsoft.com/v1.0/teams/t1/channels/c1/messages",
                    "{\"body\":{\"content\":\"hi\"}}", "Bearer t", "POST .../messages");
            assertEquals(ExtensionResponse.Result.SUCCESS, resp.getResult());
            @SuppressWarnings("unchecked")
            List<FreeForm> rows = (List<FreeForm>) resp.getResponseValue().get("Response");
            assertEquals("Action completed successfully.", rows.get(0).get("Answer"));
            assertEquals("POST .../messages", resp.getResponseValue().get("generatedPlan"));
        }
    }

    @Test
    void executeGraphCallOmitsGeneratedPlanFor202NoBodySuccessWhenNull() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 202, null);

            ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "POST", "https://graph.microsoft.com/v1.0/chats/c1/messages",
                    "{\"body\":{\"content\":\"hi\"}}", "Bearer t", null);
            assertEquals(ExtensionResponse.Result.SUCCESS, resp.getResult());
            assertFalse(resp.getResponseValue().containsKey("generatedPlan"));
        }
    }

    @Test
    void executeGraphCallReturnsFailureOnHttpErrorStatus() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            stubbedTarget(client, 400, "bad request body");

            ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "GET", "https://graph.microsoft.com/v1.0/me/chats", null, "Bearer t", "GET /me/chats");
            assertEquals(ExtensionResponse.Result.FAILURE, resp.getResult());
            assertTrue(resp.getError().getErrorMessage().contains("HTTP 400"));
            assertTrue(resp.getError().getErrorMessage().contains("bad request body"));
        }
    }

    @Test
    void executeGraphCallReturnsFailureWhenClientThrows() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            when(client.target(anyString())).thenThrow(new RuntimeException("network down"));

            ExtensionResponse resp = (ExtensionResponse) invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "GET", "https://graph.microsoft.com/v1.0/me/chats", null, "Bearer t", "GET /me/chats");
            assertEquals(ExtensionResponse.Result.FAILURE, resp.getResult());
            assertTrue(resp.getError().getErrorMessage().contains("network down"));
            verify(client).close();
        }
    }

    @Test
    void executeGraphCallDefaultsNullBodyToEmptyJsonObjectAndEncodesSpaces() throws Exception {
        try (MockedStatic<ClientBuilder> cb = mockStatic(ClientBuilder.class)) {
            Client client = mock(Client.class);
            cb.when(ClientBuilder::newClient).thenReturn(client);
            WebTarget target = stubbedTarget(client, 200, "{}");
            Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON).header("x", "y");

            invokeInstance("executeGraphCall",
                    new Class<?>[]{String.class, String.class, String.class, String.class, String.class},
                    "POST", "https://graph.microsoft.com/v1.0/search/query?$filter=isRead eq true",
                    null, "Bearer t", "plan");

            verify(client).target("https://graph.microsoft.com/v1.0/search/query?$filter=isRead%20eq%20true");
            ArgumentCaptor<Entity> captor = ArgumentCaptor.forClass(Entity.class);
            verify(builder, org.mockito.Mockito.atLeastOnce()).post(captor.capture());
            assertEquals("{}", captor.getValue().getEntity());
        }
    }

    // ── parseGenericResponse / graphObjectToFreeForm ────────────────────────

    @Test
    void parseGenericResponseTreatsMissingOrNonArrayValueAsSingleObject() throws Exception {
        @SuppressWarnings("unchecked")
        List<FreeForm> rows1 = (List<FreeForm>) invokeStatic("parseGenericResponse",
                new Class<?>[]{String.class}, "{\"id\":\"m1\",\"summary\":\"hi\"}");
        assertEquals(1, rows1.size());
        assertEquals("hi", rows1.get(0).get("Summary"));

        @SuppressWarnings("unchecked")
        List<FreeForm> rows2 = (List<FreeForm>) invokeStatic("parseGenericResponse",
                new Class<?>[]{String.class}, "{\"value\":\"oops\"}");
        assertEquals(1, rows2.size());
    }

    @Test
    void parseGenericResponseReturnsEmptyForEmptyValueArray() throws Exception {
        @SuppressWarnings("unchecked")
        List<FreeForm> rows = (List<FreeForm>) invokeStatic("parseGenericResponse",
                new Class<?>[]{String.class}, "{\"value\":[]}");
        assertTrue(rows.isEmpty());
    }

    @Test
    void parseGenericResponseHandlesSearchShapeWithAndWithoutHitId() throws Exception {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":["
                + "{\"hitId\":\"H1\",\"resource\":{\"summary\":\"first\"}},"
                + "{\"resource\":{\"summary\":\"second\"}},"
                + "{\"hitId\":null,\"resource\":{\"summary\":\"third\"}},"
                + "{\"hitId\":\"\",\"resource\":{\"summary\":\"fourth\"}}"
                + "]}]}]}";
        @SuppressWarnings("unchecked")
        List<FreeForm> rows = (List<FreeForm>) invokeStatic("parseGenericResponse", new Class<?>[]{String.class}, json);
        assertEquals(4, rows.size());
        assertEquals("H1", rows.get(0).get("Id"));
        assertFalse(rows.get(1).containsKey("Id"), "hitId key absent entirely");
        assertFalse(rows.get(2).containsKey("Id"), "hitId explicitly JSON null");
        assertFalse(rows.get(3).containsKey("Id"), "hitId present but empty");
    }

    @Test
    void parseGenericResponseDefaultsMissingHitsContainersHitsAndResourceToEmpty() throws Exception {
        @SuppressWarnings("unchecked")
        List<FreeForm> rows1 = (List<FreeForm>) invokeStatic("parseGenericResponse",
                new Class<?>[]{String.class}, "{\"value\":[{\"hitsContainers\":[{\"total\":1}]}]}");
        assertTrue(rows1.isEmpty(), "hitsContainers entry missing hits defaults to none");

        // A value[] entry whose FIRST hitsContainers entry has no "hits" — still detected as the
        // search shape (first value[] element has "hitsContainers"), so it must default cleanly.
        String missingHitsContainersOnSecondEntry = "{\"value\":["
                + "{\"hitsContainers\":[{\"hits\":[{\"resource\":{\"summary\":\"a\"}}]}]},"
                + "{}"
                + "]}";
        @SuppressWarnings("unchecked")
        List<FreeForm> rows2 = (List<FreeForm>) invokeStatic("parseGenericResponse",
                new Class<?>[]{String.class}, missingHitsContainersOnSecondEntry);
        assertEquals(1, rows2.size());

        // A hit with no "resource" key at all still produces a (blank) row.
        @SuppressWarnings("unchecked")
        List<FreeForm> rows3 = (List<FreeForm>) invokeStatic("parseGenericResponse",
                new Class<?>[]{String.class}, "{\"value\":[{\"hitsContainers\":[{\"hits\":[{}]}]}]}");
        assertEquals(1, rows3.size());
    }

    @Test
    void parseGenericResponseHandlesFlatListAndSkipsNonObjectElements() throws Exception {
        String json = "{\"value\":[\"not-an-object\",{\"displayName\":\"General\"}]}";
        @SuppressWarnings("unchecked")
        List<FreeForm> rows = (List<FreeForm>) invokeStatic("parseGenericResponse", new Class<?>[]{String.class}, json);
        assertEquals(1, rows.size());
        assertEquals("General", rows.get(0).get("Name"));
    }

    @Test
    void parseGenericResponseTreatsAPlainObjectFirstElementAsAFlatList() throws Exception {
        // First element is a JsonObject but has no "hitsContainers" key — must NOT be mistaken for
        // the search-response shape.
        String json = "{\"value\":[{\"displayName\":\"General\"},{\"displayName\":\"Sales\"}]}";
        @SuppressWarnings("unchecked")
        List<FreeForm> rows = (List<FreeForm>) invokeStatic("parseGenericResponse", new Class<?>[]{String.class}, json);
        assertEquals(2, rows.size());
        assertEquals("General", rows.get(0).get("Name"));
        assertEquals("Sales", rows.get(1).get("Name"));
    }

    @Test
    void graphObjectToFreeFormSkipsFieldsWithNoDisplayValueOrEmptyStringValue() throws Exception {
        JsonObject obj = GSON.fromJson(
                "{\"displayName\":\"Team A\",\"unknownArray\":[1,2,3],\"description\":\"\"}", JsonObject.class);
        Object result = invokeStatic("graphObjectToFreeForm", new Class<?>[]{JsonObject.class}, obj);
        FreeForm ff = (FreeForm) result;
        assertEquals("Team A", ff.get("Name"));
        assertFalse(ff.containsKey("unknownArray"));
        assertFalse(ff.containsKey("Description"), "an empty-string display value is skipped, not stored");
    }

    // ── toDisplayString / toFreeFormKey / getStr / extractSender ────────────

    @Test
    void toDisplayStringHandlesNullPrimitiveAndComplexBranches() throws Exception {
        JsonObject resourceWithFrom = GSON.fromJson(
                "{\"from\":{\"user\":{\"displayName\":\"Kim\"}},"
                        + "\"createdDateTime\":\"2026-07-20T10:00:00Z\","
                        + "\"duedate\":\"2026-07-20T10:00:00Z\","
                        + "\"plain\":\"value\","
                        + "\"body\":{\"content\":\"content text\"},"
                        + "\"other\":{\"nested\":true},"
                        + "\"nullField\":null}", JsonObject.class);

        assertNull(invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "nullField", resourceWithFrom.get("nullField"), resourceWithFrom));
        assertNull(invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "nullField", null, resourceWithFrom), "a real Java null element (not JsonNull) must also short-circuit");
        assertEquals("no-time-part", invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "plaindate", GSON.fromJson("\"no-time-part\"", com.google.gson.JsonElement.class), resourceWithFrom),
                "a *date* key whose value has no 'T' is passed through unchanged");
        assertEquals("2026-07-20", invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "createdDateTime", resourceWithFrom.get("createdDateTime"), resourceWithFrom));
        assertEquals("2026-07-20", invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "duedate", resourceWithFrom.get("duedate"), resourceWithFrom));
        assertEquals("value", invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "plain", resourceWithFrom.get("plain"), resourceWithFrom));
        assertEquals("Kim", invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "from", resourceWithFrom.get("from"), resourceWithFrom));
        assertEquals("content text", invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "body", resourceWithFrom.get("body"), resourceWithFrom));
        assertNull(invokeStatic("toDisplayString",
                new Class<?>[]{String.class, com.google.gson.JsonElement.class, JsonObject.class},
                "other", resourceWithFrom.get("other"), resourceWithFrom));
    }

    @Test
    void toFreeFormKeyMapsEveryKnownFieldAndPassesThroughUnknownOnes() throws Exception {
        assertEquals("From", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "from"));
        assertEquals("CreatedDateTime", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "createdDateTime"));
        assertEquals("LastModifiedDateTime", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "lastModifiedDateTime"));
        assertEquals("Summary", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "summary"));
        assertEquals("Link", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "webUrl"));
        assertEquals("Id", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "id"));
        assertEquals("Body", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "body"));
        assertEquals("Importance", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "importance"));
        assertEquals("ChatId", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "chatId"));
        assertEquals("ChannelIdentity", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "channelIdentity"));
        assertEquals("Name", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "displayName"));
        assertEquals("Description", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "description"));
        assertEquals("customField", invokeStatic("toFreeFormKey", new Class<?>[]{String.class}, "customField"));
    }

    @Test
    void getStrHandlesNullObjectMissingKeyAndJsonNull() throws Exception {
        assertEquals("", invokeStatic("getStr", new Class<?>[]{JsonObject.class, String.class}, (JsonObject) null, "x"));
        JsonObject obj = GSON.fromJson("{\"a\":null}", JsonObject.class);
        assertEquals("", invokeStatic("getStr", new Class<?>[]{JsonObject.class, String.class}, obj, "missing"));
        assertEquals("", invokeStatic("getStr", new Class<?>[]{JsonObject.class, String.class}, obj, "a"));
        JsonObject present = GSON.fromJson("{\"a\":\"value\"}", JsonObject.class);
        assertEquals("value", invokeStatic("getStr", new Class<?>[]{JsonObject.class, String.class}, present, "a"));
    }

    @Test
    void extractSenderHandlesMissingFromAndMissingUser() throws Exception {
        assertEquals("", invokeStatic("extractSender", new Class<?>[]{JsonObject.class},
                GSON.fromJson("{}", JsonObject.class)));
        assertEquals("", invokeStatic("extractSender", new Class<?>[]{JsonObject.class},
                GSON.fromJson("{\"from\":null}", JsonObject.class)));
        assertEquals("", invokeStatic("extractSender", new Class<?>[]{JsonObject.class},
                GSON.fromJson("{\"from\":{}}", JsonObject.class)));
        assertEquals("", invokeStatic("extractSender", new Class<?>[]{JsonObject.class},
                GSON.fromJson("{\"from\":{\"user\":null}}", JsonObject.class)));
        assertEquals("Kim", invokeStatic("extractSender", new Class<?>[]{JsonObject.class},
                GSON.fromJson("{\"from\":{\"user\":{\"displayName\":\"Kim\"}}}", JsonObject.class)));
    }

    // ── callAi ───────────────────────────────────────────────────────────────

    @Test
    void callAiDispatchesToOpenAiWhenProviderIsOpenai() throws Exception {
        try (MockedStatic<OpenAiApiClient> openAi = mockStatic(OpenAiApiClient.class)) {
            openAi.when(() -> OpenAiApiClient.call(eq("key"), eq("prompt"), any(), eq(0))).thenReturn("openai-answer");
            assertEquals("openai-answer", invokeStatic("callAi",
                    new Class<?>[]{String.class, String.class, String.class}, "key", "openai", "prompt"));
        }
    }

    @Test
    void callAiDispatchesToGeminiWhenProviderIsGemini() throws Exception {
        try (MockedStatic<GeminiApiClient> gemini = mockStatic(GeminiApiClient.class)) {
            gemini.when(() -> GeminiApiClient.call(eq("key"), eq("prompt"), any(), eq(0))).thenReturn("gemini-answer");
            assertEquals("gemini-answer", invokeStatic("callAi",
                    new Class<?>[]{String.class, String.class, String.class}, "key", "gemini", "prompt"));
        }
    }

    @Test
    void callAiDefaultsToAnthropicForAnyOtherProvider() throws Exception {
        try (MockedStatic<AnthropicApiClient> anthropic = mockStatic(AnthropicApiClient.class)) {
            anthropic.when(() -> AnthropicApiClient.call(eq("key"), eq("prompt"), any(), eq(0))).thenReturn("claude-answer");
            assertEquals("claude-answer", invokeStatic("callAi",
                    new Class<?>[]{String.class, String.class, String.class}, "key", "anthropic", "prompt"));
            assertEquals("claude-answer", invokeStatic("callAi",
                    new Class<?>[]{String.class, String.class, String.class}, "key", "unknown-provider", "prompt"));
        }
    }

    // ── success / failure / blank / getJsonString ───────────────────────────

    @Test
    void successBuildsASuccessResponseCarryingTheGivenValues() throws Exception {
        Map<String, Object> values = Map.of("Response", List.of());
        ExtensionResponse resp = (ExtensionResponse) invokeStatic("success", new Class<?>[]{Map.class}, values);
        assertEquals(ExtensionResponse.Result.SUCCESS, resp.getResult());
        assertSame(values, resp.getResponseValue());
        assertNull(resp.getError());
    }

    @Test
    void failureBuildsAFailureResponseWithARemediationAction() throws Exception {
        ExtensionResponse resp = (ExtensionResponse) invokeStatic("failure", new Class<?>[]{String.class}, "boom");
        assertEquals(ExtensionResponse.Result.FAILURE, resp.getResult());
        assertEquals("boom", resp.getError().getErrorMessage());
        assertEquals(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, resp.getError().getExceptionType());
        assertNotNull(resp.getRemediationActions());
    }

    @Test
    void blankRecognizesNullEmptyAndWhitespaceOnly() throws Exception {
        assertEquals(true, invokeStatic("blank", new Class<?>[]{String.class}, (String) null));
        assertEquals(true, invokeStatic("blank", new Class<?>[]{String.class}, ""));
        assertEquals(true, invokeStatic("blank", new Class<?>[]{String.class}, "   "));
        assertEquals(false, invokeStatic("blank", new Class<?>[]{String.class}, "x"));
    }

    @Test
    void getJsonStringHandlesNullObjectMissingKeyAndJsonNull() throws Exception {
        assertEquals("", invokeStatic("getJsonString", new Class<?>[]{JsonObject.class, String.class}, (JsonObject) null, "x"));
        JsonObject obj = GSON.fromJson("{\"a\":null}", JsonObject.class);
        assertEquals("", invokeStatic("getJsonString", new Class<?>[]{JsonObject.class, String.class}, obj, "missing"));
        assertEquals("", invokeStatic("getJsonString", new Class<?>[]{JsonObject.class, String.class}, obj, "a"));
        JsonObject present = GSON.fromJson("{\"a\":\"value\"}", JsonObject.class);
        assertEquals("value", invokeStatic("getJsonString", new Class<?>[]{JsonObject.class, String.class}, present, "a"));
    }

    // ── adaptTokenStore ──────────────────────────────────────────────────────

    @Test
    void adaptTokenStoreForwardsSaveGetAndDeleteToTheRefreshTokenStore() throws Exception {
        RefreshTokenStore store = mock(RefreshTokenStore.class);
        when(store.get("k")).thenReturn("v");
        Object adapted = invokeStatic("adaptTokenStore", new Class<?>[]{RefreshTokenStore.class}, store);
        ITokenStore tokenStore = (ITokenStore) adapted;

        tokenStore.save("k", "v");
        verify(store).put("k", "v");

        assertEquals("v", tokenStore.get("k"));

        tokenStore.delete("k");
        verify(store).remove("k");
    }
}
