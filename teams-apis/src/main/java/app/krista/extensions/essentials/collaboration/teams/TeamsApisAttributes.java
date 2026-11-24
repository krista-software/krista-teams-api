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

import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.sdk.msauth.extension.base.MsAuthAttributes;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static app.krista.extensions.essentials.collaboration.teams.impl.util.Constants.*;

public final class TeamsApisAttributes implements MsAuthAttributes {

    @SerializedName(Constants.EMAIL)
    private final String email;
    @SerializedName(Constants.CLIENT_ID)
    private final String clientId;
    @SerializedName(Constants.CLIENT_SECRET)
    private final String clientSecret;
    @SerializedName(Constants.AUTH_TYPE)
    private final String authType;
    @SerializedName(Constants.TENANT_ID)
    private final String tenantId;
    @SerializedName("publicAuthMode")
    private final String publicAuthMode;
    @SerializedName("privateAuthMode")
    private final String privateAuthMode;

    private final String routingUrl;
    private String publicClientId;
    private String publicClientSecret;

    public TeamsApisAttributes(String clientId, String clientSecret, String tenantId, String email,
                               String authType, String routingUrl) {
        this(clientId, clientSecret, tenantId, email, authType, routingUrl, null, null);
    }

    public TeamsApisAttributes(String clientId, String clientSecret, String tenantId, String email,
                               String authType, String routingUrl, String publicAuthMode, String privateAuthMode) {
        loadPublicConfig();
        this.clientId = clientId == null ? publicClientId : clientId;
        this.clientSecret = clientSecret == null ? publicClientSecret : clientSecret;
        this.tenantId = tenantId;
        this.email = email;
        this.authType = authType;
        this.publicAuthMode = publicAuthMode != null ? publicAuthMode : "krista";
        this.privateAuthMode = privateAuthMode;
        this.routingUrl = routingUrl.replace(LOCAL_EXTN_URL, LOCAL_EXTN_REPLACE_URL);
    }

    public static TeamsApisAttributes create(JsonObject authPayload, String baseurl) {
        String authType = authPayload.get(Constants.AUTH_TYPE).getAsString();
        if (Constants.PUBLIC.equals(authType)) {
            String publicAuthMode = authPayload.has("publicAuthMode") && !authPayload.get("publicAuthMode").isJsonNull()
                    ? authPayload.get("publicAuthMode").getAsString() : "krista";
            String clientId = null;
            String clientSecret = null;
            String tenantId = null;
            if ("own".equals(publicAuthMode)) {
                clientId = authPayload.has(Constants.CLIENT_ID) ? authPayload.get(Constants.CLIENT_ID).getAsString() : null;
                clientSecret = authPayload.has(Constants.CLIENT_SECRET) ? authPayload.get(Constants.CLIENT_SECRET).getAsString() : null;
                tenantId = authPayload.has(Constants.TENANT_ID) && !authPayload.get(Constants.TENANT_ID).isJsonNull()
                        ? authPayload.get(Constants.TENANT_ID).getAsString() : null;
            }
            return new TeamsApisAttributes(clientId, clientSecret, tenantId,
                    authPayload.get(Constants.EMAIL).getAsString(), authType, baseurl, publicAuthMode, null);
        } else if (Constants.PRIVATE.equals(authType)) {
            String privateAuthMode = authPayload.has("privateAuthMode") && !authPayload.get("privateAuthMode").isJsonNull()
                    ? authPayload.get("privateAuthMode").getAsString() : "s2s";
            String clientId = authPayload.has(Constants.CLIENT_ID) ? authPayload.get(Constants.CLIENT_ID).getAsString() : null;
            String clientSecret = authPayload.has(Constants.CLIENT_SECRET) ? authPayload.get(Constants.CLIENT_SECRET).getAsString() : null;
            return new TeamsApisAttributes(clientId, clientSecret,
                    authPayload.get(Constants.TENANT_ID).getAsString(),
                    authPayload.get(Constants.EMAIL).getAsString(), authType, baseurl, null, privateAuthMode);
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_AUTH);
        }
    }

    private void loadPublicConfig() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(inputStream);
            this.publicClientId = properties.getProperty("public.clientId");
            this.publicClientSecret = properties.getProperty("public.clientSecret");
        } catch (IOException cause) {
            throw new RuntimeException("We couldn't load the configuration file required for public authentication. "
                    + "Please verify that the configuration file exists and is accessible, or contact your administrator for assistance.", cause);
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getAuthType() {
        return authType;
    }

    public String getPublicAuthMode() {
        return publicAuthMode != null ? publicAuthMode : "krista";
    }

    public String getPrivateAuthMode() {
        return privateAuthMode;
    }

    public String getCallbackEndPoint() {
        if (Constants.PUBLIC.equals(authType)) {
            if ("own".equals(publicAuthMode)) {
                // Legacy 2.0.9 redirect URI — kept at the jaxrsId root (no "teams" segment) so
                // existing customer Azure app registrations survive the migration. Tabs live at
                // teams-apis/teams/...; only the callback resource is mounted at the root.
                return routingUrl + "/teams-apis/callback";
            }
            return routingUrl.replaceAll("/extension/api/.*", DEFAULT_CALLBACK_PATH);
        } else if (Constants.PRIVATE.equals(authType)) {
            return routingUrl + "/teams-apis/callback";
        } else {
            return EMPTY_STRING;
        }
    }

    @Override
    public String getForwardPath() {
        if (Constants.PUBLIC.equals(authType)) {
            return routingUrl + EXTENSION_FORWARD_PATH;
        } else {
            return EMPTY_STRING;
        }
    }

    @Override
    public String getScopeString() {
        return Constants.REQUIRED_SCOPE;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> attributesMap = new HashMap<>();
        putIfNotNull(attributesMap, Constants.CLIENT_ID, getClientId());
        putIfNotNull(attributesMap, Constants.CLIENT_SECRET, getClientSecret());
        putIfNotNull(attributesMap, Constants.TENANT_ID, getTenantId());
        putIfNotNull(attributesMap, Constants.EMAIL, getEmail());
        putIfNotNull(attributesMap, Constants.AUTH_TYPE, getAuthType());
        putIfNotNull(attributesMap, "publicAuthMode", getPublicAuthMode());
        putIfNotNull(attributesMap, "privateAuthMode", getPrivateAuthMode());
        return attributesMap;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
