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

import app.krista.sdk.msauth.config.AuthFlow;
import app.krista.sdk.msauth.extension.annotations.*;

// ── Auth SDK Core ──
@MsAuthExtension(
        extensionType = "teams",
        callbackPathPrefix = "/teams",
        supportedFlows = {AuthFlow.AUTH_CODE_PUBLIC, AuthFlow.AUTH_CODE_PRIVATE, AuthFlow.CLIENT_CREDENTIALS_SECRET},
        defaultScopes = {"openid", "offline_access", "User.Read", "User.Read.All", "Team.ReadBasic.All",
                "Channel.ReadBasic.All", "Channel.Create", "Group.ReadWrite.All", "ChannelMessage.Send",
                "Chat.ReadWrite", "ChatMessage.Send"},
        // ── Auth Tab UI ──
        headerTitle = "Teams Authentication",
        headerSubtitle = "Configure how Krista connects to Microsoft Teams",
        showScopePanel = true,
        supportsOwnCredentials = true,
        enableAuditDashboard = true,
        setupGuideUrl = "../../../static/docs/#/pages/CreatingTeamsApp"
)
// ── Scopes ──
@AuthScope(value = "Team.ReadBasic.All", description = "Read the names and descriptions of teams", required = true,
        catalogRequests = {"Get All Teams", "Get channels of team", "Get Channels of Team by Team ID"})
@AuthScope(value = "Channel.ReadBasic.All", description = "Read channel names and descriptions", required = true,
        catalogRequests = {"Get channels of team", "Get Channels of Team by Team ID", "Get Channel Messages",
                "Get Channel Message Replies"})
@AuthScope(value = "Channel.Create", description = "Create channels", required = true,
        catalogRequests = {"Create Channel"})
@AuthScope(value = "Group.ReadWrite.All", description = "Create teams (auto-create when target team is missing)",
        catalogRequests = {"Create Channel"})
@AuthScope(value = "User.Read.All", description = "Look up users by email to add as private channel members",
        catalogRequests = {"Create Channel"})
@AuthScope(value = "ChannelMessage.Send", description = "Send messages to channels", required = true,
        catalogRequests = {"Send message to teams channel", "Send HTML message to teams channel"})
@AuthScope(value = "Chat.ReadWrite", description = "Read and write user chats", required = true,
        catalogRequests = {"Get All Group Chats", "Get Recent Group Chats", "Get Group Chat by Name",
                "Get Group Chat Messages", "Send message teams group", "Send HTML message teams group"})
@AuthScope(value = "ChatMessage.Send", description = "Send messages in chats", required = true,
        catalogRequests = {"Send Message to User", "Send HTML Message to User"})
@AuthScope(value = "User.Read", description = "Read user profile",
        catalogRequests = {"Test Connection"})
// ── Audit Dashboard ──
@AuditConfig(
        dashboardTitle = "Teams Audit Dashboard",
        enableCredentialHealth = true,
        enableCharts = true,
        csvExportPrefix = "teams-audit",
        subscriptionPanelTitle = ""
)
@AuditEventGroup(name = "Catalog Requests", events = {
        @AuditEvent(type = "CATALOG_REQUEST_TRIGGERED", description = "Catalog request execution started"),
        @AuditEvent(type = "CATALOG_REQUEST_SUCCESS", description = "Catalog request completed successfully"),
        @AuditEvent(type = "CATALOG_REQUEST_FAILURE", description = "Catalog request failed")
})
public class TeamsAuthConfig {
}
