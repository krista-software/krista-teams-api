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
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers {@link TeamsMcpSetupResource}: the JAX-RS wiring, the {@code getTab}/config overrides,
 * and — most importantly — {@code interceptNlQuery}'s dynamic FreeForm-to-JSON conversion, which
 * has no hardcoded column list and must handle every shape the mocked {@link TeamsMcpQueryService}
 * can hand back.
 */
class TeamsMcpSetupResourceTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";
    private static final Gson GSON = new Gson();

    private RefreshTokenStore refreshTokenStore;
    private Invoker invoker;
    private TeamsApisAttributeStore attributeStore;
    private TeamsMcpQueryService queryService;
    private TeamsMcpSetupResource resource;

    @BeforeEach
    void setUp() {
        refreshTokenStore = mock(RefreshTokenStore.class);
        invoker = mock(Invoker.class);
        when(invoker.getInvokerId()).thenReturn("inv-1");
        attributeStore = mock(TeamsApisAttributeStore.class);
        when(attributeStore.load("inv-1")).thenReturn(
                new TeamsApisAttributes("cid", "secret", "tenant", "user@example.com", Constants.PUBLIC, ROUTING));
        queryService = mock(TeamsMcpQueryService.class);
        resource = new TeamsMcpSetupResource(refreshTokenStore, invoker, attributeStore, queryService);
    }

    private static Object invokePrivate(Object target, String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = TeamsMcpSetupResource.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        try {
            return m.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw e;
        }
    }

    // ── basic wiring ─────────────────────────────────────────────────────────

    @Test
    void getTabServesThePatchedHtml() {
        Response response = resource.getTab();
        assertEquals(200, response.getStatus());
        String body = String.valueOf(response.getEntity());
        assertTrue(body.contains("d.columns && d.columns.length"));
    }

    @Test
    void productConfigAndJaxrsIdAreTeamsSpecific() throws Exception {
        Object config = invokePrivate(resource, "getProductConfig", new Class<?>[]{});
        assertTrue(config instanceof TeamsMcpProductConfig);
        assertEquals("teams", invokePrivate(resource, "getJaxrsId", new Class<?>[]{}));
    }

    // ── interceptNlQuery ─────────────────────────────────────────────────────

    @Test
    void interceptNlQueryReturns400WithDefaultMessageWhenErrorIsNull() {
        ExtensionResponse failure = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, null, null, null);
        when(queryService.query("q")).thenReturn(failure);

        Response response = resource.interceptNlQuery("q");
        assertEquals(400, response.getStatus());
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertEquals("Query failed.", body.get("error").getAsString());
    }

    @Test
    void interceptNlQueryReturns400WithTheErrorMessageWhenPresent() {
        ExtensionResponse.Error error = new ExtensionResponse.Error(
                "boom", 0L, ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, "");
        ExtensionResponse failure = new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, null, null);
        when(queryService.query("q")).thenReturn(failure);

        Response response = resource.interceptNlQuery("q");
        assertEquals(400, response.getStatus());
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertTrue(body.get("error").getAsString().contains("boom"));
    }

    @Test
    void interceptNlQueryReturns400WhenQueryServiceThrows() {
        when(queryService.query("q")).thenThrow(new RuntimeException("network down"));

        Response response = resource.interceptNlQuery("q");
        assertEquals(400, response.getStatus());
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertTrue(body.get("error").getAsString().contains("network down"));
    }

    @Test
    void interceptNlQueryHandlesSuccessWithNoResponseValue() {
        ExtensionResponse success = new ExtensionResponse(ExtensionResponse.Result.SUCCESS, null, null, null, null);
        when(queryService.query("q")).thenReturn(success);

        Response response = resource.interceptNlQuery("q");
        assertEquals(200, response.getStatus());
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertEquals(0, body.get("total").getAsInt());
        assertTrue(body.getAsJsonArray("rows").isEmpty());
        assertTrue(body.getAsJsonArray("columns").isEmpty());
    }

    @Test
    void interceptNlQueryHandlesResponseValueWithoutAResponseList() {
        ExtensionResponse success = new ExtensionResponse(
                ExtensionResponse.Result.SUCCESS, Map.of("other", "value"), null, null, null);
        when(queryService.query("q")).thenReturn(success);

        Response response = resource.interceptNlQuery("q");
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertEquals(0, body.get("total").getAsInt());
    }

    @Test
    void interceptNlQueryBuildsDynamicColumnsFromFreeFormRowsAndDetectsLinkColumns() {
        FreeForm row1 = new FreeForm();
        row1.put("From", " Text ", "Priya");
        row1.put("Link", " Text ", "https://teams.microsoft.com/l/1");
        FreeForm row2 = new FreeForm();
        row2.put("From", " Text ", "Sam");
        row2.put("Subject", " Text ", "https://teams.microsoft.com/l/2");
        row2.put("Url", " Text ", "https://example.com");

        ExtensionResponse success = new ExtensionResponse(
                ExtensionResponse.Result.SUCCESS, Map.of("Response", List.of(row1, row2)), null, null, null);
        when(queryService.query("q")).thenReturn(success);

        Response response = resource.interceptNlQuery("q");
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertEquals(2, body.get("total").getAsInt());
        assertEquals(2, body.get("shown").getAsInt());
        assertEquals("q", body.get("generatedQuery").getAsString());
        assertFalse(body.get("specific").getAsBoolean());

        Map<String, String> colTypes = new java.util.HashMap<>();
        for (var colEl : body.getAsJsonArray("columns")) {
            JsonObject col = colEl.getAsJsonObject();
            colTypes.put(col.get("key").getAsString(), col.get("type").getAsString());
        }
        assertEquals("text", colTypes.get("from"));
        assertEquals("link", colTypes.get("link"));
        assertEquals("link", colTypes.get("subject"));
        assertEquals("link", colTypes.get("url"));

        JsonObject firstRow = body.getAsJsonArray("rows").get(0).getAsJsonObject();
        assertEquals("Priya", firstRow.get("from").getAsString());
    }

    @Test
    void interceptNlQuerySkipsNonFreeFormRowsAndEmptyOrListTypedValues() {
        FreeForm withListValue = new FreeForm();
        // Directly stash a List value to exercise extractFreeFormValue's List-handling branch —
        // production code never stores a List, but the method defensively supports it.
        withListValue.put("Multi", " Text ", java.util.List.of("type", "actual-value"));
        withListValue.put("Empty", " Text ", "");
        withListValue.put("ShortList", " Text ", java.util.List.of("onlyOne"));
        withListValue.put("NullSecond", " Text ", java.util.Arrays.asList("type", null));
        withListValue.put("EmptySecond", " Text ", java.util.List.of("type", ""));
        withListValue.put("NullValue", " Text ", null);

        ExtensionResponse success = new ExtensionResponse(ExtensionResponse.Result.SUCCESS,
                Map.of("Response", List.of("not-a-freeform", withListValue)), null, null, null);
        when(queryService.query("q")).thenReturn(success);

        Response response = resource.interceptNlQuery("q");
        JsonObject body = GSON.fromJson(String.valueOf(response.getEntity()), JsonObject.class);
        assertEquals(1, body.get("total").getAsInt());
        JsonObject row = body.getAsJsonArray("rows").get(0).getAsJsonObject();
        assertEquals("actual-value", row.get("multi").getAsString());
        assertFalse(row.has("empty"));
        assertFalse(row.has("shortList"));
        assertFalse(row.has("nullSecond"));
        assertFalse(row.has("emptySecond"));
        assertFalse(row.has("nullValue"));
    }

    // ── private helpers via reflection (pure-function edge cases) ───────────

    @Test
    void toColumnKeyLowercasesFirstCharacterAndPassesThroughBlankInput() throws Exception {
        assertEquals("messageId", invokePrivate(resource, "toColumnKey", new Class<?>[]{String.class}, "MessageId"));
        assertNull(invokePrivate(resource, "toColumnKey", new Class<?>[]{String.class}, (String) null));
        assertEquals("", invokePrivate(resource, "toColumnKey", new Class<?>[]{String.class}, ""));
    }

    @Test
    void toColumnLabelSpacesCamelCaseAndPassesThroughBlankInput() throws Exception {
        assertEquals("Chat Or Channel", invokePrivate(resource, "toColumnLabel", new Class<?>[]{String.class}, "chatOrChannel"));
        assertNull(invokePrivate(resource, "toColumnLabel", new Class<?>[]{String.class}, (String) null));
        assertEquals("", invokePrivate(resource, "toColumnLabel", new Class<?>[]{String.class}, ""));
    }

    @Test
    void isLinkColumnRecognizesOnlyKnownLinkKeys() throws Exception {
        assertEquals(true, invokePrivate(resource, "isLinkColumn", new Class<?>[]{String.class}, "link"));
        assertEquals(true, invokePrivate(resource, "isLinkColumn", new Class<?>[]{String.class}, "subject"));
        assertEquals(true, invokePrivate(resource, "isLinkColumn", new Class<?>[]{String.class}, "url"));
        assertEquals(false, invokePrivate(resource, "isLinkColumn", new Class<?>[]{String.class}, "from"));
    }

    @Test
    void adaptTokenStoreForwardsToTheRefreshTokenStore() throws Exception {
        RefreshTokenStore store = mock(RefreshTokenStore.class);
        when(store.get("k")).thenReturn("v");
        Object adapted = invokePrivate(TeamsMcpSetupResource.class, "adaptTokenStore",
                new Class<?>[]{RefreshTokenStore.class}, store);
        var tokenStore = (app.krista.sdk.msauth.token.ITokenStore) adapted;
        tokenStore.save("k", "v");
        tokenStore.get("k");
        tokenStore.delete("k");
        org.mockito.Mockito.verify(store).put("k", "v");
        org.mockito.Mockito.verify(store).remove("k");
    }

    private static Object invokePrivate(Class<?> owner, String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = owner.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }
}
