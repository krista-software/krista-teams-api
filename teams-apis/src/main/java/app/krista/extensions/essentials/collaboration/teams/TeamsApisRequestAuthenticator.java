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

import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.ksdk.context.AuthorizationContext;
import app.krista.sdk.msauth.extension.base.AbstractRequestAuthenticator;
import app.krista.sdk.msauth.extension.base.MsAuthAttributes;
import app.krista.sdk.msauth.token.ITokenStore;
import app.krista.sdk.msauth.token.InMemoryTokenStore;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

@Service
public class TeamsApisRequestAuthenticator extends AbstractRequestAuthenticator {

    private final AuthorizationContext authorizationContext;
    private final TeamsApisAttributeStore attributeStore;

    @Inject
    public TeamsApisRequestAuthenticator(TeamsApisAttributeStore attributeStore, String invokerId,
                                         AuthorizationContext authorizationContext, String routingUrl) {
        super(invokerId, routingUrl, "/teams");
        this.attributeStore = attributeStore;
        this.authorizationContext = authorizationContext;
    }

    @Override
    protected MsAuthAttributes loadAttributes(String key) {
        return attributeStore.load(key);
    }

    /** Visible-for-testing accessor (protected method not reachable from test package otherwise). */
    MsAuthAttributes loadAttributesForTest(String key) {
        return loadAttributes(key);
    }

    @Override
    protected ITokenStore getTokenStore() {
        return new InMemoryTokenStore();
    }

    @Override
    protected MsAuthAttributes createDefaultAttributes(String routingUrl) {
        return new TeamsApisAttributes(null, null, null, null, Constants.PUBLIC, routingUrl);
    }

    @Override
    protected String getNotificationPath() {
        // Teams has no notification endpoint. "/teams" would collide with the
        // @ApplicationPath("teams") base and misattribute unrelated requests to
        // an arbitrary account. AbstractRequestAuthenticator#getAuthenticatedAccountId
        // (see microsoft-auth-sdk) only compares this value when non-null
        // (`notifPath != null && notifPath.equals(path)`), so returning null here is
        // safe and simply disables notification-based account resolution.
        return null;
    }

    @Override
    protected String resolveNotificationAccountId() {
        return authorizationContext.getAuthorizedAccount().getAccountId();
    }
}
