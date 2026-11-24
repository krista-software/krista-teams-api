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
 * Constants for sub-catalog requests.
 */
public class SubCatalogConstants {
    private SubCatalogConstants() {}

    // Common constants
    public static final String VALIDATION_RESULTS = "validationResults";
    public static final String REENTER = "Reenter";

    // Get Channel Messages related constants
    public static final String CONFIRM_REENTER_GET_CHANNEL_MESSAGES = "confirmReenterGetChannelMessages";
    public static final String HANDLE_REENTER_GET_CHANNEL_MESSAGES = "handleReenterGetChannelMessages";

    // Date validation constants
    public static final String CONFIRM_DATE_VALIDATION_GET_CHANNEL_MESSAGES = "confirmDateValidationGetChannelMessages";
    public static final String HANDLE_DATE_VALIDATION_GET_CHANNEL_MESSAGES = "handleDateValidationGetChannelMessages";

    // Get Group Chat Messages related constants
    public static final String CONFIRM_REENTER_GET_GROUP_CHAT_MESSAGES = "confirmReenterGetGroupChatMessages";
    public static final String HANDLE_REENTER_GET_GROUP_CHAT_MESSAGES = "handleReenterGetGroupChatMessages";

    // Date validation constants for Group Chat Messages
    public static final String CONFIRM_DATE_VALIDATION_GET_GROUP_CHAT_MESSAGES = "confirmDateValidationGetGroupChatMessages";
    public static final String HANDLE_DATE_VALIDATION_GET_GROUP_CHAT_MESSAGES = "handleDateValidationGetGroupChatMessages";

    // Get Group Chat by Name related constants
    public static final String CONFIRM_GROUP_NAME_VALIDATION_GET_GROUP_CHAT_BY_NAME = "confirmGroupNameValidationGetGroupChatByName";
    public static final String HANDLE_GROUP_NAME_VALIDATION_GET_GROUP_CHAT_BY_NAME = "handleGroupNameValidationGetGroupChatByName";

    // Get Channel Messages - Channel Team Link validation constants
    public static final String CONFIRM_CHANNEL_TEAM_LINK_VALIDATION_GET_CHANNEL_MESSAGES = "confirmChannelTeamLinkValidationGetChannelMessages";
    public static final String HANDLE_CHANNEL_TEAM_LINK_VALIDATION_GET_CHANNEL_MESSAGES = "handleChannelTeamLinkValidationGetChannelMessages";

    // Get Channel Message Replies - Channel ID validation constants
    public static final String CONFIRM_CHANNEL_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES = "confirmChannelIdValidationGetChannelMessageReplies";
    public static final String HANDLE_CHANNEL_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES = "handleChannelIdValidationGetChannelMessageReplies";

    // Get Channel Message Replies - Team ID validation constants
    public static final String CONFIRM_TEAM_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES = "confirmTeamIdValidationGetChannelMessageReplies";
    public static final String HANDLE_TEAM_ID_VALIDATION_GET_CHANNEL_MESSAGE_REPLIES = "handleTeamIdValidationGetChannelMessageReplies";

    // Get Channels of Team by Team ID - Team ID validation constants
    public static final String CONFIRM_TEAM_ID_VALIDATION_GET_CHANNELS_OF_TEAM = "confirmTeamIdValidationGetChannelsOfTeam";
    public static final String HANDLE_TEAM_ID_VALIDATION_GET_CHANNELS_OF_TEAM = "handleTeamIdValidationGetChannelsOfTeam";

    // Send Message to User - Email and Message Content validation constants
    public static final String CONFIRM_SEND_MESSAGE_TO_USER = "confirmSendMessageToUser";
    public static final String HANDLE_SEND_MESSAGE_TO_USER = "handleSendMessageToUser";

    // Send HTML Message to User - Email and HTML Message Content validation constants
    public static final String CONFIRM_SEND_HTML_MESSAGE_TO_USER = "confirmSendHTMLMessageToUser";
    public static final String HANDLE_SEND_HTML_MESSAGE_TO_USER = "handleSendHTMLMessageToUser";

    // Send HTML Message to Teams Channel - Team Name, Channel Name, and HTML Message Content validation constants
    public static final String CONFIRM_SEND_HTML_MESSAGE_TO_CHANNEL = "confirmSendHTMLMessageToChannel";
    public static final String HANDLE_SEND_HTML_MESSAGE_TO_CHANNEL = "handleSendHTMLMessageToChannel";
}
