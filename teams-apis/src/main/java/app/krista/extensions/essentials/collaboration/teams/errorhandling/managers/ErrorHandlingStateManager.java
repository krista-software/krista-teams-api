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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.managers;

import com.google.gson.Gson;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages state for error handling between CatalogRequests and SubCatalogRequests.
 * This class provides a way to store and retrieve validation results and other state information.
 */
@Singleton
@Service
public class ErrorHandlingStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingStateManager.class);
    private final Map<String, String> internalState = new HashMap<>();
    private final Map<String, Object> metaInfoState = new HashMap<>();
    private final Gson gson = new Gson();

    /**
     * Stores a value in the internal state with the given key.
     *
     * @param key   The key to store the value under
     * @param value The value to store
     */
    public void put(String key, String value) {
        LOGGER.debug("Storing state with key: {}", key);
        internalState.put(key, value);
    }

    /**
     * Retrieves a value from the internal state as a Map.
     *
     * @param key The key to retrieve the value for
     * @return The value as a Map, or an empty Map if not found
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> get(String key) {
        LOGGER.debug("Retrieving state with key: {}", key);
        String value = internalState.get(key);
        if (value == null) {
            return new HashMap<>();
        }
        return gson.fromJson(value, Map.class);
    }

    /**
     * Retrieves the raw string value from the internal state.
     *
     * @param key The key to retrieve the value for
     * @return The raw string value, or null if not found
     */
    public String getRaw(String key) {
        LOGGER.debug("Retrieving raw state with key: {}", key);
        return internalState.get(key);
    }

    /**
     * Stores meta information that can't be serialized to JSON.
     *
     * @param key   The key to store the value under
     * @param value The value to store
     */
    public void putMetaInfo(String key, Object value) {
        LOGGER.debug("Storing meta info with key: {}", key);
        metaInfoState.put(key, value);
    }

    /**
     * Retrieves meta information.
     *
     * @param key The key to retrieve the value for
     * @return The value, or null if not found
     */
    public Object getMetaInfo(String key) {
        LOGGER.debug("Retrieving meta info with key: {}", key);
        return metaInfoState.get(key);
    }

    /**
     * Clears all state for a given key.
     *
     * @param key The key to clear
     */
    public void clear(String key) {
        LOGGER.debug("Clearing state with key: {}", key);
        internalState.remove(key);
        metaInfoState.remove(key);
    }
}
