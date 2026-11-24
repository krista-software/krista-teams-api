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

import static app.krista.extensions.essentials.collaboration.teams.mcp.TeamsMcpQueryService.isAllowedGraphCall;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the POST allowlist guard on {@link TeamsMcpQueryService#executeGraphCall}.
 *
 * <p>Teams MCP is query-only: the LLM plans arbitrary Graph API calls, so execution
 * must be restricted to GET requests plus POST only for the Graph Search endpoint
 * (Microsoft Graph exposes search as a read-only POST call). Any other
 * method/URL combination must be rejected rather than executed.</p>
 */
class TeamsMcpQueryServiceGraphAllowlistTest {

    @Test
    void allowsGet() {
        assertTrue(isAllowedGraphCall("GET", "https://graph.microsoft.com/v1.0/me/chats"));
        assertTrue(isAllowedGraphCall("get", "https://graph.microsoft.com/v1.0/me/joinedTeams"));
    }

    @Test
    void allowsPostToGraphSearchQuery() {
        assertTrue(isAllowedGraphCall("POST", "https://graph.microsoft.com/v1.0/search/query"));
        assertTrue(isAllowedGraphCall("post", "https://graph.microsoft.com/v1.0/search/query"));
    }

    @Test
    void allowsPostToGraphSearchQueryWithSuffix() {
        assertTrue(isAllowedGraphCall("POST", "https://graph.microsoft.com/v1.0/search/query?$select=id"));
    }

    @Test
    void rejectsPostToOtherEndpoints() {
        assertFalse(isAllowedGraphCall("POST", "https://graph.microsoft.com/v1.0/me/messages"));
        assertFalse(isAllowedGraphCall("POST", "https://graph.microsoft.com/v1.0/me/sendMail"));
        assertFalse(isAllowedGraphCall("POST", "https://graph.microsoft.com/v1.0/teams/T1/channels"));
    }

    @Test
    void allowsPostToSendChannelMessage() {
        assertTrue(isAllowedGraphCall("POST",
                "https://graph.microsoft.com/v1.0/teams/T1/channels/19:abc@thread.tacv2/messages"));
    }

    @Test
    void allowsPostToSendChatMessage() {
        assertTrue(isAllowedGraphCall("POST",
                "https://graph.microsoft.com/v1.0/chats/19:xyz@thread.v2/messages"));
    }

    @Test
    void rejectsPostToMessageRepliesAndSubResources() {
        // Only top-level message sends are permitted — replies and other sub-resources are not.
        assertFalse(isAllowedGraphCall("POST",
                "https://graph.microsoft.com/v1.0/teams/T1/channels/C1/messages/M1/replies"));
        assertFalse(isAllowedGraphCall("POST",
                "https://graph.microsoft.com/v1.0/teams/T1/channels/C1/members"));
    }

    @Test
    void rejectsOtherHttpMethods() {
        assertFalse(isAllowedGraphCall("DELETE", "https://graph.microsoft.com/v1.0/me/messages/123"));
        assertFalse(isAllowedGraphCall("PATCH", "https://graph.microsoft.com/v1.0/search/query"));
        assertFalse(isAllowedGraphCall("PUT", "https://graph.microsoft.com/v1.0/me/photo"));
    }

    @Test
    void rejectsNullMethodOrUrl() {
        assertFalse(isAllowedGraphCall(null, "https://graph.microsoft.com/v1.0/search/query"));
        assertFalse(isAllowedGraphCall("GET", null));
    }
}
