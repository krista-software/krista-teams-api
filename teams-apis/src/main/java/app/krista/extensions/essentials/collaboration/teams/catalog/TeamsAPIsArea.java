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

package app.krista.extensions.essentials.collaboration.teams.catalog;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.*;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.*;
import app.krista.extensions.essentials.collaboration.teams.impl.util.*;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.GraphServiceExceptionHandler;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.SendMessageToUserUtils;

import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import app.krista.extensions.essentials.collaboration.teams.service.TeamService;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import com.microsoft.graph.http.GraphServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@Domain(id = "catEntryDomain_5fa2fc97-4b17-44cf-b98f-aa91a459a091",
        name = "Collaboration",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "a0d44dfa-e749-4bbf-9d86-db8b5991b153")
public class TeamsAPIsArea {

    private final TeamService teamService;
    private final ChannelService channelService;
    private final ChatService chatService;
    private final GetChannelIDHelper getChannelIDHelper;
    private final GetChatIDHelper getChatIDHelper;
    private final GetGroupChatByNameHelper getGroupChatByNameHelper;
    private final GetChannelMessageRepliesHelper getChannelMessageRepliesHelper;
    private final GetChannelsOfTeamHelper getChannelsOfTeamHelper;
    private final SendMessageToUserUtils sendMessageToUserUtils;
    private final SendMessageToUserEmailValidationHelper sendMessageToUserEmailValidationHelper;
    private final TelemetryHelper telemetryHelper;
    private final SendMessageToUserResponseProcessingUtils sendMessageToUserResponseProcessingUtils;
    private final SendHTMLMessageToUserValidationHelper sendHTMLMessageToUserValidationHelper;
    private final SendHTMLMessageToChannelValidationHelper sendHTMLMessageToChannelValidationHelper;
    private final TelemetryMetrics telemetryMetrics;
    private static final Logger logger = LoggerFactory.getLogger(TeamsAPIsArea.class);

    @Inject
    public TeamsAPIsArea(TeamService teamService, ChannelService channelService, ChatService chatService,
                         GetChannelIDHelper getChannelIDHelper, GetChatIDHelper getChatIDHelper,
                         GetGroupChatByNameHelper getGroupChatByNameHelper,
                         GetChannelMessageRepliesHelper getChannelMessageRepliesHelper,
                         GetChannelsOfTeamHelper getChannelsOfTeamHelper,
                         SendMessageToUserUtils sendMessageToUserUtils,
                         SendMessageToUserEmailValidationHelper sendMessageToUserEmailValidationHelper,
                         TelemetryHelper telemetryHelper,
                         SendMessageToUserResponseProcessingUtils sendMessageToUserResponseProcessingUtils,
                         SendHTMLMessageToUserValidationHelper sendHTMLMessageToUserValidationHelper,
                         SendHTMLMessageToChannelValidationHelper sendHTMLMessageToChannelValidationHelper, TelemetryMetrics telemetryMetrics) {
        logger.trace("TeamsAPIsArea constructor called");

        this.telemetryMetrics = telemetryMetrics;
        this.teamService = teamService;
        this.channelService = channelService;
        this.chatService = chatService;
        this.getChannelIDHelper = getChannelIDHelper;
        this.getChatIDHelper = getChatIDHelper;
        this.getGroupChatByNameHelper = getGroupChatByNameHelper;
        this.getChannelMessageRepliesHelper = getChannelMessageRepliesHelper;
        this.getChannelsOfTeamHelper = getChannelsOfTeamHelper;
        this.sendMessageToUserUtils = sendMessageToUserUtils;
        this.sendMessageToUserEmailValidationHelper = sendMessageToUserEmailValidationHelper;
        this.telemetryHelper = telemetryHelper;
        this.sendMessageToUserResponseProcessingUtils = sendMessageToUserResponseProcessingUtils;
        this.sendHTMLMessageToUserValidationHelper = sendHTMLMessageToUserValidationHelper;
        this.sendHTMLMessageToChannelValidationHelper = sendHTMLMessageToChannelValidationHelper;

        logger.trace("TeamsAPIsArea constructor completed");
    }

    @CatalogRequest(
            id = "localDomainRequest_17cfa54c-8e79-4f1b-837d-799d7dd4090a",
            name = "Get channels of team",
            description = "Retrieves all available channels for the given team name",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Desc(name = "Channels", type = "[ Entity(Channel) ]", required = false)
    public List<Channel> getChannelsOfTeam(
            @Field.Text(name = "Team Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String teamName,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelsOfTeam");

        try {
            logger.info("getChannelsOfTeam() called with teamName: {}, allowRetry: {}", teamName, allowRetry);
            List<Channel> channels = getChannelsDirectly(teamName);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_OF_TEAM_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return channels;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_OF_TEAM_REQUEST_FAILURE, 1L);
            logger.error("Error occurred while retrieving channels for team: {}", teamName, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_2ba10aea-9dd9-4578-97b6-77cc65da202f",
            name = "Send message teams group",
            description = "Sending a message to the given group name",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean sendMessageTeamsGroup(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupName,
            @Field.Text(name = "Message", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendMessageTeamsGroup");

        try {
            logger.info("sendMessageTeamsGroup() called with groupName: {}, allowRetry: {}", groupName, allowRetry);
            Boolean result = chatService.sendMessageToChatGroup(groupName, message, false);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TEAMS_GROUP_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return result;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, 1L);
            logger.error("Error in sendMessageTeamsGroup for group: {}", groupName, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_17b16e2e-aced-4c6d-9ce4-12c436251ece",
            name = "Send HTML message teams group",
            description = "Send HTML message teams group",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean sendHTMLMessageTeamsGroup(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String groupName,
            @Field(name = "Message", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendHTMLMessageTeamsGroup");

        try {
            int messageLength = message != null ? message.length() : 0;
            logger.info("sendHTMLMessageTeamsGroup() called with groupName: {}, allowRetry: {}, HTML message length: {} characters",
                groupName, allowRetry, messageLength);
            Boolean result = chatService.sendMessageToChatGroup(groupName, message, true);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return result;
        } catch (GraphServiceException gse) {
            GraphServiceExceptionHandler.handleMessageTooLargeErrorAndThrow(gse, message, "group '" + groupName + "'",
                    TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, operation, telemetryMetrics);
            return false;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, 1L);
            int messageLength = message != null ? message.length() : 0;
            logger.error("Error in sendHTMLMessageTeamsGroup for group: {}, HTML message length: {} characters",
                groupName, messageLength, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_8971f871-8dc3-4ab2-812f-6d5dccb1e779",
            name = "Send message to teams channel",
            description = "Sending a message to the provided team's channel",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Boolean sendMessageToTeamsChannel(
            @Field.Text(name = "Team Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String teamName,
            @Field.Text(name = "Channel Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String channelName,
            @Field.Text(name = "Message", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendMessageToTeamsChannel");

        try {
            logger.info("sendMessageToTeamsChannel() called with teamName: {}, channelName: {}, allowRetry: {}", teamName, channelName, allowRetry);
            Boolean result = chatService.sendMessageToChannel(teamName, channelName, message);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_CHANNEL_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return result;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_CHANNEL_REQUEST_FAILURE, 1L);
            logger.error("Error in sendMessageToTeamsChannel for team: {}, channel: {}", teamName, channelName, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_5b8a9807-9a1a-4700-b252-4fab6c8e1da0",
            name = "Get Channel Messages",
            description = "Get channel messages by channel ID",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Channel Messages", type = "[ Entity(Channel Message) ]", required = false)
    public ExtensionResponse getChannelMessages(
            @Field.Text(name = "Channel ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The channel ID of the Teams channel'")}, options = {}) String channelID,
            @Field.Text(name = "Team ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The team ID of the Teams team'")}, options = {}) String teamID,
            @Field.Date(name = "From Date", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The start date from when, to retrieve channel messages'")}, options = {}) Long fromDate,
            @Field.Date(name = "To Date", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The end date to when, to retrieve channel messages'")}, options = {}) Long toDate,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelMessages");

        try {
            logger.info("TeamsAPIsArea.getChannelMessages() called with channelID={}, teamID={}, fromDate={}, toDate={}, allowRetry={}", channelID, teamID, fromDate, toDate, allowRetry);
            ExtensionResponse response = getChannelIDHelper.getChannelMessagesWithValidation(channelID, teamID, fromDate, toDate, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGES_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGES_REQUEST_FAILURE, 1L);
            logger.error("Error in getChannelMessages for channelID: {}, teamID: {}", channelID, teamID, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_47abe7c5-2570-4516-826b-820c444ac485",
            name = "Get Group Chat Messages",
            description = "Get group chats by chat ID",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Group Chat Messages", type = "[ Entity(Channel Message) ]", required = false)
    public ExtensionResponse getGroupChatMessages(
            @Field.Text(name = "Chat ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The chat ID of the Teams group chat'")}, options = {}) String chatID,
            @Field.Date(name = "From Date", required = true, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The start date from when, to retrieve group chats'")}, options = {}) Long fromDate,
            @Field.Date(name = "To Date", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The end date to when, to retrieve group chats'")}, options = {}) Long toDate,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getGroupChatMessages");

        try {
            logger.info("TeamsAPIsArea.getGroupChatMessages() called with chatID={}, fromDate={}, toDate={}, allowRetry={}", chatID, fromDate, toDate, allowRetry);
            ExtensionResponse response = getChatIDHelper.getGroupChatMessagesWithValidation(chatID, fromDate, toDate, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_MESSAGES_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_MESSAGES_REQUEST_FAILURE, 1L);
            logger.error("Error in getGroupChatMessages for chatID: {}", chatID, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_4a335292-e6b3-4224-b1f0-9cd814ef8954",
            name = "Get Group Chat by Name",
            description = "Get details of a group chat by name",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Group", type = "Entity(Group Chat)", required = false)
    public ExtensionResponse getGroupChatByName(
            @Field.Text(name = "Group Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The name of the group chat'")}, options = {}) String groupName,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getGroupChatByName");

        try {
            logger.info("TeamsAPIsArea.getGroupChatByName() called with groupName={}, allowRetry={}", groupName, allowRetry);
            ExtensionResponse response = getGroupChatByNameHelper.getGroupChatByNameWithValidation(groupName, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_BY_NAME_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_BY_NAME_REQUEST_FAILURE, 1L);
            logger.error("Error in getGroupChatByName for groupName: {}", groupName, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_2c951231-be29-408f-99b7-624321380a75",
            name = "Get All Teams",
            description = "Get a list of all Teams the user has access to",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Teams", type = "[ Entity(Team) ]", required = false)
    public List<Team> getAllTeams() {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getAllTeams");

        try {
            logger.debug("getTeams called");
            List<Team> teams = teamService.getJoinedTeams(null);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_TEAMS_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return teams;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_TEAMS_REQUEST_FAILURE, 1L);
            logger.error("Error in getAllTeams", cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_0ad391d3-b11b-4c65-b5ab-548f297834d0",
            name = "Get All Group Chats",
            description = "Get details of all group chats the user has access to",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Group Chats", type = "[ Entity(Group Chat) ]", required = false)
    public ExtensionResponse getAllGroupChats() {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getAllGroupChats");

        try {
            logger.debug("getAllGroupChats called");
            List<GroupChat> groupChats = chatService.getAllGroupChats();
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_GROUP_CHATS_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return new ExtensionResponseBuilder()
                    .success(Map.of("Group Chats", groupChats))
                    .build();
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_GROUP_CHATS_REQUEST_FAILURE, 1L);
            logger.error("Error in getAllGroupChats", cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_04ebedc3-5806-4e59-95ee-03cb61afe83c",
            name = "Get Channel Message Replies",
            description = "Get channel message replies (thread) by message Id",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Replies List", type = "[ Entity(Replies) ]", required = false)
    public ExtensionResponse getChannelMessageReplies(
            @Field.Text(name = "Channel Message ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The message ID of the channel message'")}, options = {}) String channelMessageID,
            @Field.Text(name = "Channel ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The channel ID of the Teams channel'")}, options = {}) String channelID,
            @Field.Text(name = "Team ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The team ID of the Teams team'")}, options = {}) String teamID,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelMessageReplies");

        try {
            logger.info("TeamsAPIsArea.getChannelMessageReplies() called with channelMessageId={}, channelId={}, teamId={}, allowRetry={}", channelMessageID, channelID, teamID, allowRetry);
            ExtensionResponse response = getChannelMessageRepliesHelper.getChannelMessageRepliesWithValidation(channelMessageID, channelID, teamID, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGE_REPLIES_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGE_REPLIES_REQUEST_FAILURE, 1L);
            logger.error("Error in getChannelMessageReplies for channelMessageID: {}, channelID: {}, teamID: {}", channelMessageID, channelID, teamID, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_9af0bbb5-b9e9-4095-81eb-3d7bae65de30",
            name = "Get Channels of Team by Team ID",
            description = "Get a list of all channels for a given Team the user has access to",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM)
    @Field.Desc(name = "Channels", type = "[ Entity(Channel) ]", required = false)
    public ExtensionResponse getChannelsOfTeamByTeamID(
            @Field.Text(name = "Team ID", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The team ID of the Teams team'")}, options = {}) String teamID,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelsOfTeamByTeamID");

        try {
            logger.info("TeamsAPIsArea.getChannelsOfTeamByTeamID() called with teamID={}, allowRetry={}", teamID, allowRetry);
            ExtensionResponse response = getChannelsOfTeamHelper.getChannelsOfTeamWithValidation(teamID, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_BY_TEAM_ID_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_BY_TEAM_ID_REQUEST_FAILURE, 1L);
            logger.error("Error in getChannelsOfTeamByTeamID for teamID: {}", teamID, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_a9381a37-b98f-45c3-97e8-ec6fcc9fcb25",
            name = "Send Message to User",
            description = "Sends a 1:1 chat message to a specified user in Microsoft Teams via Graph API." +
                    "If the chat does not exist, it creates a new 1:1 chat thread with the user.",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Status Code", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'HTTP response code from the Graph API'")}, options = {})
    @Field.Text(name = "Message Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'The unique ID of the message sent'")}, options = {})
    @Field.Text(name = "Chat Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'ID of the 1:1 chat used or created'")}, options = {})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")}, options = {})
    public ExtensionResponse sendMessageToUser(
            @Field.Text(name = "User Email", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Email ID of the Teams user to whom message is to be sent'")}, options = {}) String userEmail,
            @Field.Text(name = "Message Content", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'The content of the message to be sent'")}, options = {}) String messageContent,
            @Field.Boolean(name = "Reuse ChatId", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Reuse existing chat if found, else create a new one'")}, options = {}) Boolean reuseChatId,
            @Field.Boolean(name = "Log Conversation", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Whether to store chat ID in Krista memory for reuse'")}, options = {}) Boolean logConversation,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendMessageToUser");

        try {
            logger.info("sendMessageToUser() called with userEmail: {}, allowRetry: {}", userEmail, allowRetry);
            ExtensionResponse response = sendMessageToUserEmailValidationHelper.sendMessageToUserWithEmailValidation(
                    userEmail, messageContent, reuseChatId, logConversation, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_USER_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_USER_REQUEST_FAILURE, 1L);
            logger.error("Error in sendMessageToUser for user: {}", userEmail, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_0bf5b321-994b-442d-80a1-b736831354c1",
            name = "Send HTML Message to User",
            description = "Sends a 1:1 HTML chat message to a specified user in Microsoft Teams via Graph API.",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Text(name = "Status Code", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'HTTP response code from the Graph API'")}, options = {})
    @Field.Text(name = "Message Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The unique ID of the message sent'")}, options = {})
    @Field.Text(name = "Chat Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'ID of the 1:1 chat used or created'")}, options = {})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")}, options = {})
    public ExtensionResponse sendHTMLMessageToUser(
            @Field.Text(name = "User Email", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Email ID of the Teams user to whom message is to be sent'")}, options = {}) String userEmail,
            @Field(name = "Message Content", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L"), @Attribute(name = "toolTip", value = "'HTML message content to send'")}, options = {}) String messageContent,
            @Field.Boolean(name = "Reuse ChatId", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Reuse existing chat if found, else create a new one'")}, options = {}) Boolean reuseChatId,
            @Field.Boolean(name = "Log Conversation", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Whether to store chat ID in Krista memory for reuse'")}, options = {}) Boolean logConversation,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendHTMLMessageToUser");

        try {
            int messageLength = messageContent != null ? messageContent.length() : 0;
            logger.info("TeamsAPIsArea.sendHTMLMessageToUser() called with userEmail: {}, allowRetry: {}, HTML message length: {} characters",
                userEmail, allowRetry, messageLength);
            ExtensionResponse response = sendHTMLMessageToUserValidationHelper.sendHTMLMessageToUserWithEmailValidation(
                    userEmail, messageContent, reuseChatId, logConversation, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_USER_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (GraphServiceException gse) {
            return GraphServiceExceptionHandler.handleMessageTooLargeError(gse, messageContent, "user '" + userEmail + "'",
                    TelemetryConstants.SEND_HTML_MESSAGE_TO_USER_REQUEST_FAILURE, operation, telemetryMetrics);
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_USER_REQUEST_FAILURE, 1L);
            int messageLength = messageContent != null ? messageContent.length() : 0;
            logger.error("Error in sendHTMLMessageToUser for user: {}, HTML message length: {} characters",
                userEmail, messageLength, cause);
            throw cause;
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_fe881b0c-b934-4c8c-9ecb-4a5095b80a02",
            name = "Send HTML message to teams channel",
            description = "Send HTML message to teams channel",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public ExtensionResponse sendHTMLMessageToTeamsChannel(
            @Field.Text(name = "Team Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String teamName,
            @Field.Text(name = "Channel Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {}) String channelName,
            @Field(name = "Message", type = "RichText", required = true, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendHTMLMessageToTeamsChannel");

        try {
            int messageLength = message != null ? message.length() : 0;
            logger.info("TeamsAPIsArea.sendHTMLMessageToTeamsChannel() called with teamName: {}, channelName: {}, allowRetry: {}, HTML message length: {} characters",
                teamName, channelName, allowRetry, messageLength);
            ExtensionResponse response = sendHTMLMessageToChannelValidationHelper.sendHTMLMessageToChannelWithValidation(
                    teamName, channelName, message, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_CHANNEL_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (GraphServiceException gse) {
            return GraphServiceExceptionHandler.handleMessageTooLargeError(gse, message, "channel '" + channelName + "' in team '" + teamName + "'",
                    TelemetryConstants.SEND_HTML_MESSAGE_TO_CHANNEL_REQUEST_FAILURE, operation, telemetryMetrics);
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_CHANNEL_REQUEST_FAILURE, 1L);
            int messageLength = message != null ? message.length() : 0;
            logger.error("Error in sendHTMLMessageToTeamsChannel for team: {}, channel: {}, HTML message length: {} characters",
                teamName, channelName, messageLength, cause);
            throw cause;
        }
    }


    /**
     * Helper method to retrieve channels directly from the service.
     *
     * @param teamName The name of the team
     * @return List of channels for the team
     */
    private List<Channel> getChannelsDirectly(String teamName) {
        logger.debug("Calling Teams API: teamService.getJoinedTeams with teamName={}", teamName);
        List<Team> joinedTeams = teamService.getJoinedTeams(teamName);
        logger.debug("Teams API response: received {} joined teams", joinedTeams != null ? joinedTeams.size() : 0);

        if (joinedTeams.isEmpty()) {
            logger.info("No team found with name: {}", teamName);
            return Collections.emptyList();
        }

        logger.debug("Calling Teams API: channelService.getChannelsOfTeam with teamId={}", joinedTeams.get(0).iD);
        List<Channel> channels = channelService.getChannelsOfTeam(joinedTeams.get(0).iD, null);
        logger.debug("Teams API response: received {} channels for team {}",
                channels != null ? channels.size() : 0, teamName);

        return channels;
    }
}
