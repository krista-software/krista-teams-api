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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.constants;

/**
 * Constants for Teams API resource field names.
 */
public class TeamsResources {
    private TeamsResources() {
    }

    // Existing constants
    public static final String CHANNEL_ID = "Channel ID";
    public static final String TEAM_ID = "Team ID";
    public static final String CHAT_ID = "Chat ID";
    public static final String GROUP_NAME = "Group Name";
    public static final String CHANNEL_MESSAGE_ID = "Channel Message ID";//New

    // Date-related constants
    public static final String FROM_DATE = "From Date";
    public static final String TO_DATE = "To Date";
    public static final String STATE_ID = "stateId";

    // Send Message to User constants
    public static final String USER_EMAIL = "User Email";
    public static final String MESSAGE_CONTENT = "Message Content";

    // Send HTML Message to User constants
    public static final String HTML_MESSAGE_CONTENT = "HTML Message Content";

    // Send HTML Message to Teams Channel constants
    public static final String TEAM_NAME = "Team Name";
    public static final String CHANNEL_NAME = "Channel Name";
    public static final String HTML_MESSAGE_CONTENT_FOR_CHANNEL = "HTML Message Content";
}
