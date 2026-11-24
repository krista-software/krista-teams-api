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
import org.junit.jupiter.api.Test;

import javax.ws.rs.ApplicationPath;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamsApisApplicationTest {

    @Test
    void getClassesReturnsAllThreeResources() {
        Set<Class<?>> classes = new TeamsApisApplication().getClasses();

        assertEquals(3, classes.size());
        assertTrue(classes.contains(AuthenticationResource.class));
        assertTrue(classes.contains(TeamsAuditResource.class));
        assertTrue(classes.contains(TeamsMcpSetupResource.class));
    }

    @Test
    void applicationPathIsRoot() {
        ApplicationPath path = TeamsApisApplication.class.getAnnotation(ApplicationPath.class);

        assertEquals("/", path.value());
    }
}
