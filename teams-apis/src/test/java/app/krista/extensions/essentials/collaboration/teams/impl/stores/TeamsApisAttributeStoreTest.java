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

package app.krista.extensions.essentials.collaboration.teams.impl.stores;

import app.krista.extension.executor.Invoker;
import app.krista.extension.request.RoutingInfo;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TeamsApisAttributeStoreTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/x";

    private final Map<String, Object> backing = new HashMap<>();
    private KeyValueStore kv;
    private TeamsApisAttributeStore store;

    @BeforeEach
    void setUp() {
        kv = mock(KeyValueStore.class);
        when(kv.get(anyString())).thenAnswer(inv -> backing.get(inv.getArgument(0)));
        doAnswer(inv -> backing.put(inv.getArgument(0), inv.getArgument(1)))
                .when(kv).put(anyString(), any());
        doAnswer(inv -> backing.remove(inv.getArgument(0))).when(kv).remove(anyString());

        Invoker invoker = mock(Invoker.class, RETURNS_DEEP_STUBS);
        when(invoker.getRoutingInfo().getRoutingURL(anyString(), any(RoutingInfo.Type.class))).thenReturn(ROUTING);

        store = new TeamsApisAttributeStore(invoker, kv);
    }

    // ---------------- load ----------------

    @Test
    void loadReturnsNullWhenKeyMissing() {
        assertNull(store.load("missing-key"));
    }

    @Test
    void loadReturnsNullWhenStoredValueIsBlank() {
        backing.put("blank-key", "   ");
        assertNull(store.load("blank-key"));
    }

    @Test
    void loadReturnsNullWhenStoredValueIsInvalidJson() {
        backing.put("bad-json-key", "{not valid json");
        assertNull(store.load("bad-json-key"));
    }

    @Test
    void loadRoundTripsPublicAttributesWithBothAuthModesPresent() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid", "a@b.com",
                Constants.PUBLIC, ROUTING, "own", "s2s");
        store.save(attrs, "invoker-1");

        TeamsApisAttributes loaded = store.load("invoker-1");

        assertNotNull(loaded);
        assertEquals("cid", loaded.getClientId());
        assertEquals("cs", loaded.getClientSecret());
        assertEquals("tid", loaded.getTenantId());
        assertEquals("a@b.com", loaded.getEmail());
        assertEquals(Constants.PUBLIC, loaded.getAuthType());
        assertEquals("own", loaded.getPublicAuthMode());
        assertEquals("s2s", loaded.getPrivateAuthMode());
    }

    @Test
    void loadOmitsAuthModesWhenAbsentFromStoredJson() {
        // TeamsApisAttributes.toMap() never omits "publicAuthMode" (getPublicAuthMode() always
        // falls back to "krista"), so a normal save()->load() round trip can never exercise the
        // containsKey("publicAuthMode") == false branch. Write the raw JSON directly instead, to
        // simulate an older/hand-crafted record that never had the key at all.
        Map<String, Object> raw = new HashMap<>();
        raw.put(Constants.CLIENT_ID, "cid");
        raw.put(Constants.CLIENT_SECRET, "cs");
        raw.put(Constants.TENANT_ID, "tid");
        raw.put(Constants.EMAIL, "a@b.com");
        raw.put(Constants.AUTH_TYPE, Constants.PRIVATE);
        backing.put("raw-key", Constants.GSON.toJson(raw));

        TeamsApisAttributes loaded = store.load("raw-key");

        assertNotNull(loaded);
        assertEquals(Constants.PRIVATE, loaded.getAuthType());
        assertEquals("krista", loaded.getPublicAuthMode(), "falls back inside TeamsApisAttributes itself");
        assertNull(loaded.getPrivateAuthMode());
    }

    // ---------------- save(attributes) [1-arg, generates its own key] ----------------

    @Test
    void saveWithGeneratedKeyThrowsOnNullAttributes() {
        assertThrows(IllegalArgumentException.class, () -> store.save(null));
    }

    @Test
    void saveWithGeneratedKeyReturnsAuthContextIdAndPersists() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid",
                "a@b.com", Constants.PUBLIC, ROUTING);

        String authContextId = store.save(attrs);

        assertTrue(authContextId.startsWith(Constants.AUTH_CONTEXT_ID + Constants.UNDER_SCORE),
                "unexpected authContextId: " + authContextId);
        verify(kv).put(eq(authContextId), anyString());
        assertNotNull(store.load(authContextId));
    }

    // ---------------- save(attributes, invokerId) [2-arg] ----------------

    @Test
    void saveWithInvokerIdThrowsOnNullAttributes() {
        assertThrows(IllegalArgumentException.class, () -> store.save(null, "invoker-1"));
    }

    @Test
    void saveWithInvokerIdReturnsTrueAndPersistsUnderInvokerId() {
        TeamsApisAttributes attrs = new TeamsApisAttributes("cid", "cs", "tid",
                "a@b.com", Constants.PRIVATE, ROUTING);

        boolean result = store.save(attrs, "invoker-3");

        assertTrue(result);
        verify(kv).put(eq("invoker-3"), anyString());
        assertNotNull(store.load("invoker-3"));
    }

    // ---------------- remove ----------------

    @Test
    void removeDelegatesToKeyValueStore() {
        backing.put("some-key", "irrelevant");

        store.remove("some-key");

        verify(kv).remove("some-key");
        assertNull(backing.get("some-key"));
    }
}
