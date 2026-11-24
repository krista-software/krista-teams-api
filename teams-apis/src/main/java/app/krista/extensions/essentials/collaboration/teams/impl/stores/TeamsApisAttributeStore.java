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
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.extensions.util.KeyValueStore;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;

import static app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.*;

@Service
public class TeamsApisAttributeStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamsApisAttributeStore.class);

    private final Invoker invoker;
    private final KeyValueStore store;

    @Inject
    public TeamsApisAttributeStore(Invoker invoker, KeyValueStore store) {
        this.invoker = invoker;
        this.store = store;
    }

    @SuppressWarnings("unchecked")
    public TeamsApisAttributes load(String key) {
        String loadedAttributes = null;
        if (store.get(key) != null) {
            loadedAttributes = (String) store.get(key);
        }
        if (loadedAttributes == null || loadedAttributes.isBlank()) {
            LOGGER.info("AttributeStore.load: cache miss, key={}", key);
            return null;
        }
        Map<String, Object> attributes;
        try {
            attributes = GSON.fromJson(loadedAttributes, Map.class);
        } catch (Exception e) {
            LOGGER.warn("AttributeStore.load: JSON parse failed, key={}, error={}", key, e.getMessage());
            return null;
        }
        String authType = attributes.get(AUTH_TYPE).toString();
        String baseUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        String publicAuthMode = attributes.containsKey("publicAuthMode") ? (String) attributes.get("publicAuthMode") : null;
        String privateAuthMode = attributes.containsKey("privateAuthMode") ? (String) attributes.get("privateAuthMode") : null;
        return new TeamsApisAttributes((String) attributes.get(CLIENT_ID), (String) attributes.get(CLIENT_SECRET),
                (String) attributes.get(TENANT_ID), (String) attributes.get(EMAIL), authType, baseUrl,
                publicAuthMode, privateAuthMode);
    }

    public String save(TeamsApisAttributes attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("Teams Attributes cannot be null");
        }
        String authContextId = Constants.AUTH_CONTEXT_ID + Constants.UNDER_SCORE + UUID.randomUUID();
        store.put(authContextId, GSON.toJson(attributes.toMap()));
        LOGGER.info("AttributeStore.save: authContextId={}, authType={}", authContextId, attributes.getAuthType());
        return authContextId;
    }

    public boolean save(TeamsApisAttributes attributes, String invokerId) {
        if (attributes == null) {
            throw new IllegalArgumentException("Teams Attributes cannot be null");
        }
        store.put(invokerId, GSON.toJson(attributes.toMap()));
        LOGGER.info("AttributeStore.save: invokerId={}, authType={}", invokerId, attributes.getAuthType());
        return true;
    }

    public void remove(String key) {
        LOGGER.info("AttributeStore.remove: key={}", key);
        store.remove(key);
    }
}
