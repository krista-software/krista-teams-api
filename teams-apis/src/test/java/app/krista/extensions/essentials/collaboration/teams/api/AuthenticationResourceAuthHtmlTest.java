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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SDK 1.0.5 pre-built auth/index.html detects its extension id with
 * {@code location.pathname.match(/\/rest\/(\w+)\//)} and falls back to 'outlook'. Teams keeps
 * jaxrsId "teams-apis", so the page URL never contains "/rest/" and the auth tab bootstraps as
 * Outlook, fetches metadata for an unknown extension, and dies ("Extension not found."). The
 * served HTML must therefore carry a 'teams' fallback instead of 'outlook'.
 */
class AuthenticationResourceAuthHtmlTest {

    @Test
    void servedAuthHtmlFallsBackToTeamsExtensionId() {
        String html = AuthenticationResource.patchedAuthHtml();
        assertNotNull(html, "auth/index.html must be on the classpath (sdkUi extraction)");
        assertTrue(html.contains("return m?m[1]:'teams'"),
                "served auth HTML must fall back to extension id 'teams' when the URL has no /rest/ segment");
        assertFalse(html.contains(":'outlook'"),
                "served auth HTML must not retain the SDK's hard-coded 'outlook' fallback");
    }
}
