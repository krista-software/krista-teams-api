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

package app.krista.extensions.essentials.collaboration.teams.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The SDK 1.0.5 mcp-setup/index.html renders NLQ results using only
 * {@code productConfig.resultColumns} — a fixed message schema
 * (from/createdDateTime/summary/chatOrChannel/link). Any query returning a
 * different shape (teams, channels, chats) yields rows whose keys don't match
 * those columns, so every cell renders blank even though the backend returns a
 * correct dynamic {@code columns} array. The served HTML must prefer the
 * backend's {@code d.columns}.
 */
class TeamsMcpSetupResourceHtmlTest {

    @Test
    void servedMcpTabPrefersBackendColumns() {
        String html = TeamsMcpSetupResource.patchedMcpHtml();
        assertNotNull(html, "mcp-setup/index.html must be on the classpath (sdkUi extraction)");
        assertTrue(html.contains("d.columns && d.columns.length"),
                "MCP tab must render the dynamic columns the backend returns for non-message queries");
        assertFalse(html.contains("var cols = (productConfig && productConfig.resultColumns) ? productConfig.resultColumns : [];"),
                "the fixed message-only column schema must no longer be the sole column source");
    }
}
