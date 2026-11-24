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

import app.krista.extensions.util.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers every method on {@link RefreshTokenStore}: it's a thin delegate around
 * {@link KeyValueStore}, so each test just confirms the call and arguments pass through.
 */
class RefreshTokenStoreTest {

    private KeyValueStore keyValueStore;
    private RefreshTokenStore store;

    @BeforeEach
    void setUp() {
        keyValueStore = mock(KeyValueStore.class);
        store = new RefreshTokenStore(keyValueStore);
    }

    @Test
    void putDelegatesToKeyValueStore() {
        store.put("key-1", "token-1");

        verify(keyValueStore).put("key-1", "token-1");
    }

    @Test
    void getDelegatesToKeyValueStoreAndReturnsValue() {
        when(keyValueStore.get("key-1", String.class)).thenReturn("token-1");

        assertEquals("token-1", store.get("key-1"));
    }

    @Test
    void getReturnsNullWhenNothingStored() {
        when(keyValueStore.get("missing-key", String.class)).thenReturn(null);

        assertNull(store.get("missing-key"));
    }

    @Test
    void removeDelegatesToKeyValueStore() {
        store.remove("key-1");

        verify(keyValueStore).remove("key-1");
    }

    @Test
    void getKeyValueStoreReturnsTheInjectedInstance() {
        assertSame(keyValueStore, store.getKeyValueStore());
    }
}
