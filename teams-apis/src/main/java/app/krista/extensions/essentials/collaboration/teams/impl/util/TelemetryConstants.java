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

package app.krista.extensions.essentials.collaboration.teams.impl.util;

/**
 * Constants for telemetry metrics in Teams APIs.
 * These constants are used to track various operations and their outcomes.
 */
public class TelemetryConstants {

    private TelemetryConstants() {
        // Private constructor to prevent instantiation
    }

    // Get Channels of Team metrics
    public static final String GET_CHANNELS_OF_TEAM_REQUEST_SUCCESS = "teams.get_channels_of_team.request.success";
    public static final String GET_CHANNELS_OF_TEAM_REQUEST_FAILURE = "teams.get_channels_of_team.request.failure";

    // Get Channels of Team by Team ID metrics
    public static final String GET_CHANNELS_BY_TEAM_ID_REQUEST_SUCCESS = "teams.get_channels_by_team_id.request.success";
    public static final String GET_CHANNELS_BY_TEAM_ID_REQUEST_FAILURE = "teams.get_channels_by_team_id.request.failure";

    // Get Channel Messages metrics
    public static final String GET_CHANNEL_MESSAGES_REQUEST_SUCCESS = "teams.get_channel_messages.request.success";
    public static final String GET_CHANNEL_MESSAGES_REQUEST_FAILURE = "teams.get_channel_messages.request.failure";

    // Get Group Chat Messages metrics
    public static final String GET_GROUP_CHAT_MESSAGES_REQUEST_SUCCESS = "teams.get_group_chat_messages.request.success";
    public static final String GET_GROUP_CHAT_MESSAGES_REQUEST_FAILURE = "teams.get_group_chat_messages.request.failure";

    // Get Group Chat by Name metrics
    public static final String GET_GROUP_CHAT_BY_NAME_REQUEST_SUCCESS = "teams.get_group_chat_by_name.request.success";
    public static final String GET_GROUP_CHAT_BY_NAME_REQUEST_FAILURE = "teams.get_group_chat_by_name.request.failure";

    // Get All Teams metrics
    public static final String GET_ALL_TEAMS_REQUEST_SUCCESS = "teams.get_all_teams.request.success";
    public static final String GET_ALL_TEAMS_REQUEST_FAILURE = "teams.get_all_teams.request.failure";

    // Get All Group Chats metrics
    public static final String GET_ALL_GROUP_CHATS_REQUEST_SUCCESS = "teams.get_all_group_chats.request.success";
    public static final String GET_ALL_GROUP_CHATS_REQUEST_FAILURE = "teams.get_all_group_chats.request.failure";

    // Get Channel Message Replies metrics
    public static final String GET_CHANNEL_MESSAGE_REPLIES_REQUEST_SUCCESS = "teams.get_channel_message_replies.request.success";
    public static final String GET_CHANNEL_MESSAGE_REPLIES_REQUEST_FAILURE = "teams.get_channel_message_replies.request.failure";

    // Send Message to User metrics
    public static final String SEND_MESSAGE_TO_USER_REQUEST_SUCCESS = "teams.send_message_to_user.request.success";
    public static final String SEND_MESSAGE_TO_USER_REQUEST_FAILURE = "teams.send_message_to_user.request.failure";

    // Send Message to Teams Group metrics
    public static final String SEND_MESSAGE_TEAMS_GROUP_REQUEST_SUCCESS = "teams.send_message_teams_group.request.success";
    public static final String SEND_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE = "teams.send_message_teams_group.request.failure";

    // Send Message to Channel metrics
    public static final String SEND_MESSAGE_TO_CHANNEL_REQUEST_SUCCESS = "teams.send_message_to_channel.request.success";
    public static final String SEND_MESSAGE_TO_CHANNEL_REQUEST_FAILURE = "teams.send_message_to_channel.request.failure";

    // Send HTML Message to Teams Group metrics
    public static final String SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_SUCCESS = "teams.send_html_message_teams_group.request.success";
    public static final String SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE = "teams.send_html_message_teams_group.request.failure";

    // Send HTML Message to User metrics
    public static final String SEND_HTML_MESSAGE_TO_USER_REQUEST_SUCCESS = "teams.send_html_message_to_user.request.success";
    public static final String SEND_HTML_MESSAGE_TO_USER_REQUEST_FAILURE = "teams.send_html_message_to_user.request.failure";

    // Send HTML Message to Channel metrics
    public static final String SEND_HTML_MESSAGE_TO_CHANNEL_REQUEST_SUCCESS = "teams.send_html_message_to_channel.request.success";
    public static final String SEND_HTML_MESSAGE_TO_CHANNEL_REQUEST_FAILURE = "teams.send_html_message_to_channel.request.failure";
}

