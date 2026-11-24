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

import app.krista.extensions.util.KeyValueStore;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.ksdk.context.RequestContext;
import app.krista.sdk.msauth.extension.base.MsAuthAttributes;
import app.krista.sdk.msauth.extension.service.MsAuthGraphClientProvider;

/**
 * Thin type alias so existing business logic (TeamService/ChannelService/ChatService,
 * helpers, validators) keeps its imports. All auth logic lives in the SDK's
 * {@link MsAuthGraphClientProvider}.
 */
public class GraphServiceClientProvider extends MsAuthGraphClientProvider {

    public GraphServiceClientProvider(KeyValueStore keyValueStore,
                                      MsAuthAttributes attributes,
                                      RequestContext requestContext,
                                      AuthorizationContext authorizationContext,
                                      String authContextId) {
        super(keyValueStore, attributes, requestContext, authorizationContext, authContextId);
    }

    /** Typed accessor for Teams business logic. */
    public app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes getTeamsAttributes() {
        return getAttributes();
    }
}
