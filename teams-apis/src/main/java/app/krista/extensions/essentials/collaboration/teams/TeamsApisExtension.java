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

import app.krista.extension.authorization.RequestAuthenticator;
import app.krista.extension.executor.Invoker;
import app.krista.extension.impl.anno.*;
import app.krista.extension.request.RoutingInfo;
import app.krista.extension.request.protos.http.HttpProtocol;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;

import javax.inject.Inject;
import java.util.Map;

@Java(version = Java.Version.JAVA_21)
@Field.Text(value = TeamsApisAttributes.CLIENT_ID)
@Field.Text(value = TeamsApisAttributes.CLIENT_SECRET, isSecured = true)
@Field.Text(value = TeamsApisAttributes.TENANT_ID, isSecured = true)
@Field.Text(value = TeamsApisAttributes.EMAIL)
@StaticResource(path = "docs", file = "docs")
@Extension(version = "2.0.8", jaxrsId = Constants.TEAMS_JAXRS_ID, name = "Teams APIs")
public class TeamsApisExtension {

    private final String routingUrl;
    private final TeamsApisAttributes teamsApisAttributes;
    private final GraphServiceClientProviderFactory clientProviderFactory;
    private final TeamsApisRequestAuthenticator requestAuthenticator;

    @Inject
    public TeamsApisExtension(Invoker invoker, TeamsApisAttributes teamsApisAttributes, GraphServiceClientProviderFactory clientProviderFactory, TeamsApisAttributeStore teamsApisAttributeStore) {
        this(invoker, teamsApisAttributes,
                clientProviderFactory,
                new TeamsApisRequestAuthenticator(teamsApisAttributeStore, teamsApisAttributes));
    }

    public TeamsApisExtension(Invoker invoker, TeamsApisAttributes teamsApisAttributes, GraphServiceClientProviderFactory clientProviderFactory, TeamsApisRequestAuthenticator requestAuthenticator) {
        this.routingUrl = invoker.getRoutingInfo().getRoutingURL(HttpProtocol.PROTOCOL_NAME, RoutingInfo.Type.APPLIANCE);
        this.teamsApisAttributes = teamsApisAttributes;
        this.clientProviderFactory = clientProviderFactory;
        this.requestAuthenticator = requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.AUTHENTICATOR)
    public RequestAuthenticator getRequestAuthenticator() {
        return requestAuthenticator;
    }

    @InvokerRequest(InvokerRequest.Type.CUSTOM_TABS)
    public Map<String, String> customTabs() {
        return Map.of("Documentation", "static/docs");
    }

    @InvokerRequest(InvokerRequest.Type.VALIDATE_ATTRIBUTES)
    public void validateConnection(Map<String, Object> connectionAttributes) {
        TeamsApisAttributes attributes = TeamsApisAttributes.create(routingUrl, connectionAttributes);
        if (attributes.getMailId() != null && !attributes.getMailId().isBlank()) {
            checkGraphApiConnection(attributes);
        }

    }

    @InvokerRequest(InvokerRequest.Type.TEST_CONNECTION)
    public void testConnection() {
        if (teamsApisAttributes.getMailId() != null && !teamsApisAttributes.getMailId().isBlank()) {
            checkGraphApiConnection(teamsApisAttributes);
        }
    }

    private void checkGraphApiConnection(TeamsApisAttributes attributes) {
        clientProviderFactory.create(attributes)
                .getGraphServiceClientForAdmin()
                .me().joinedTeams()
                .buildRequest()
                .get();
    }

    @InvokerRequest(InvokerRequest.Type.INVOKER_UPDATED)
    public void attributesUpdated(Map<String, Object> oldAttributes, Map<String, Object> newAttributes) {
        teamsApisAttributes.update(newAttributes);
    }

}
