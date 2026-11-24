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

package app.krista.extensions.essentials.collaboration.teams.api;

import app.krista.extensions.essentials.collaboration.teams.mcp.TeamsMcpSetupResource;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@Service
// Root application path so the OAuth callback can be mounted at the jaxrsId root
// (teams-apis/callback — the legacy 2.0.9 redirect URI). Tab/API resources carry an explicit
// "teams/" prefix in their own @Path, so they still resolve at teams-apis/teams/... (which the
// SDK audit/auth tab HTML relies on to detect the extension id).
@ApplicationPath("/")
@ContractsProvided(Application.class)
public class TeamsApisApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(AuthenticationResource.class, TeamsAuditResource.class, TeamsMcpSetupResource.class);
    }
}
