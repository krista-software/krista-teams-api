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

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extension.executor.ErrorBuilder;
import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.ExtensionResponseBuilder;
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Channel;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.GroupChat;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Team;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.utils.GraphServiceExceptionHandler;
import app.krista.extensions.essentials.collaboration.teams.impl.util.*;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import app.krista.extensions.essentials.collaboration.teams.service.TeamService;
import app.krista.ksdk.telemetry.TelemetryMetrics;
import com.microsoft.graph.http.GraphServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    private final SendMessageToUserEmailValidationHelper sendMessageToUserEmailValidationHelper;
    private final TelemetryHelper telemetryHelper;
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
                         SendMessageToUserEmailValidationHelper sendMessageToUserEmailValidationHelper,
                         TelemetryHelper telemetryHelper,
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
        this.sendMessageToUserEmailValidationHelper = sendMessageToUserEmailValidationHelper;
        this.telemetryHelper = telemetryHelper;
        this.sendHTMLMessageToUserValidationHelper = sendHTMLMessageToUserValidationHelper;
        this.sendHTMLMessageToChannelValidationHelper = sendHTMLMessageToChannelValidationHelper;

        logger.trace("TeamsAPIsArea constructor completed");
    }

    @CatalogRequest(
            id = "localDomainRequest_17cfa54c-8e79-4f1b-837d-799d7dd4090a",
            name = "Get channels of team",
            description = "Retrieves all available channels for the given team name",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Desc(name = "Channels", type = "[ Entity(Channel) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getChannelsOfTeam(
            @Field.Text(name = "Team Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String teamName,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelsOfTeam");

        try {
            logger.info("getChannelsOfTeam() called with teamName: {}, allowRetry: {}", teamName, allowRetry);
            List<Channel> channels = channelService.getChannelsFromTeamName(teamName);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_OF_TEAM_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();

            String responseMessage = channels.isEmpty()
                    ? "No channels found for team '" + teamName + "'"
                    : "Successfully retrieved " + channels.size() + " channel(s) for team '" + teamName + "'";

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Channels", channels,
                            "Response Message", responseMessage))
                    .build();
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_OF_TEAM_REQUEST_FAILURE, 1L);
            logger.error("Error occurred while retrieving channels for team: {}", teamName, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to retrieve channels for team '" + teamName + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_2ba10aea-9dd9-4578-97b6-77cc65da202f",
            name = "Send message teams group",
            description = "Sending a message to the given group name",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse sendMessageTeamsGroup(
            @Field.Text(name = "Group Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String groupName,
            @Field.Text(name = "Message", attributes = {@Attribute(name = "visualWidth", value = "S")}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendMessageTeamsGroup");

        try {
            logger.info("sendMessageTeamsGroup() called with groupName: {}, allowRetry: {}", groupName, allowRetry);
            Boolean result = chatService.sendMessageToChatGroup(groupName, message, false);

            if (Boolean.TRUE.equals(result)) {
                telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TEAMS_GROUP_REQUEST_SUCCESS, 1L);
                operation.recordSuccess();

                return new ExtensionResponseBuilder()
                        .success(Map.of(
                                "Is Message Delivered", true,
                                "Response Message", "Message successfully sent to group '" + groupName + "'"))
                        .build();
            } else {
                telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, 1L);
                operation.recordFailure(new Exception("Failed to send message - service returned false"));

                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                        .errorMessage("Failed to send message to group '" + groupName + "'")
                        .build();

                return new ExtensionResponseBuilder()
                        .failure(error)
                        .build();
            }
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, 1L);
            logger.error("Error in sendMessageTeamsGroup for group: {}", groupName, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to send message to group '" + groupName + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_17b16e2e-aced-4c6d-9ce4-12c436251ece",
            name = "Send HTML message teams group",
            description = "Send HTML message teams group",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse sendHTMLMessageTeamsGroup(
            @Field.Text(name = "Group Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String groupName,
            @Field(name = "Message", type = "RichText", attributes = {@Attribute(name = "visualWidth", value = "L")}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendHTMLMessageTeamsGroup");

        try {
            int messageLength = message != null ? message.length() : 0;
            logger.info("sendHTMLMessageTeamsGroup() called with groupName: {}, allowRetry: {}, HTML message length: {} characters",
                    groupName, allowRetry, messageLength);
            Boolean result = chatService.sendMessageToChatGroup(groupName, message, true);

            if (Boolean.TRUE.equals(result)) {
                telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_SUCCESS, 1L);
                operation.recordSuccess();

                return new ExtensionResponseBuilder()
                        .success(Map.of(
                                "Is Message Delivered", true,
                                "Response Message", "HTML message successfully sent to group '" + groupName + "'"))
                        .build();
            } else {
                telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, 1L);
                operation.recordFailure(new Exception("Failed to send HTML message - service returned false"));

                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                        .errorMessage("Failed to send HTML message to group '" + groupName + "'")
                        .build();

                return new ExtensionResponseBuilder()
                        .failure(error)
                        .build();
            }
        } catch (GraphServiceException gse) {
            return GraphServiceExceptionHandler.handleMessageTooLargeError(gse, message, "group '" + groupName + "'",
                    TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, operation, telemetryMetrics);
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TEAMS_GROUP_REQUEST_FAILURE, 1L);
            int messageLength = message != null ? message.length() : 0;
            logger.error("Error in sendHTMLMessageTeamsGroup for group: {}, HTML message length: {} characters",
                    groupName, messageLength, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to send HTML message to group '" + groupName + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_8971f871-8dc3-4ab2-812f-6d5dccb1e779",
            name = "Send message to teams channel",
            description = "Sending a message to the provided team's channel",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse sendMessageToTeamsChannel(
            @Field.Text(name = "Team Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String teamName,
            @Field.Text(name = "Channel Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String channelName,
            @Field.Text(name = "Message", attributes = {@Attribute(name = "visualWidth", value = "S")}) String message,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendMessageToTeamsChannel");

        try {
            logger.info("sendMessageToTeamsChannel() called with teamName: {}, channelName: {}, allowRetry: {}", teamName, channelName, allowRetry);
            Boolean result = chatService.sendMessageToChannel(teamName, channelName, message);

            if (Boolean.TRUE.equals(result)) {
                telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_CHANNEL_REQUEST_SUCCESS, 1L);
                operation.recordSuccess();

                return new ExtensionResponseBuilder()
                        .success(Map.of(
                                "Is Message Delivered", true,
                                "Response Message", "Message successfully sent to channel '" + channelName + "' in team '" + teamName + "'"))
                        .build();
            } else {
                telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_CHANNEL_REQUEST_FAILURE, 1L);
                operation.recordFailure(new Exception("Failed to send message - service returned false"));

                ExtensionResponse.Error error = new ErrorBuilder()
                        .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                        .errorMessage("Failed to send message to channel '" + channelName + "' in team '" + teamName + "'")
                        .build();

                return new ExtensionResponseBuilder()
                        .failure(error)
                        .build();
            }
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_CHANNEL_REQUEST_FAILURE, 1L);
            logger.error("Error in sendMessageToTeamsChannel for team: {}, channel: {}", teamName, channelName, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to send message to channel '" + channelName + "' in team '" + teamName + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_5b8a9807-9a1a-4700-b252-4fab6c8e1da0",
            name = "Get Channel Messages",
            description = "Get channel messages by channel ID",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Channel Messages", type = "[ Entity(Channel Message) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getChannelMessages(
            @Field.Text(name = "Channel ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The channel ID of the Teams channel'")}) String channelID,
            @Field.Text(name = "Team ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The team ID of the Teams team'")}) String teamID,
            @Field.Date(name = "From Date", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The start date from when, to retrieve channel messages'")}) Long fromDate,
            @Field.Date(name = "To Date", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The end date to when, to retrieve channel messages'")}) Long toDate,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {
        logger.info("TeamsAPIsArea.getChannelMessages() called with channelID={}, teamID={}, fromDate={}, toDate={}, allowRetry={}", channelID, teamID, fromDate, toDate, allowRetry);
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelMessages");
        try {
            ExtensionResponse response = getChannelIDHelper.getChannelMessagesWithValidation(channelID, teamID, fromDate, toDate, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGES_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGES_REQUEST_FAILURE, 1L);
            logger.error("Error in getChannelMessages for channelID: {}, teamID: {}", channelID, teamID, cause);

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Channel Messages", Collections.emptyList(),
                            "Response Message", "Failed to retrieve channel messages: " + cause.getMessage()))
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_47abe7c5-2570-4516-826b-820c444ac485",
            name = "Get Group Chat Messages",
            description = "Get group chats by chat ID",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Group Chat Messages", type = "[ Entity(Channel Message) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getGroupChatMessages(
            @Field.Text(name = "Chat ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The chat ID of the Teams group chat'")}) String chatID,
            @Field.Date(name = "From Date", required = true, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The start date from when, to retrieve group chats'")}) Long fromDate,
            @Field.Date(name = "To Date", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The end date to when, to retrieve group chats'")}) Long toDate,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getGroupChatMessages");

        try {
            logger.info("TeamsAPIsArea.getGroupChatMessages() called with chatID={}, fromDate={}, toDate={}, allowRetry={}", chatID, fromDate, toDate, allowRetry);
            ExtensionResponse response = getChatIDHelper.getGroupChatMessagesWithValidation(chatID, fromDate, toDate, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_MESSAGES_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_MESSAGES_REQUEST_FAILURE, 1L);
            logger.error("Error in getGroupChatMessages for chatID: {}", chatID, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to retrieve group chat messages for chatID '" + chatID + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_4a335292-e6b3-4224-b1f0-9cd814ef8954",
            name = "Get Group Chat by Name",
            description = "Get details of a group chat by name",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Group", type = "Entity(Group Chat)", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getGroupChatByName(
            @Field.Text(name = "Group Name", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The name of the group chat'")}) String groupName,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getGroupChatByName");

        try {
            logger.info("TeamsAPIsArea.getGroupChatByName() called with groupName={}, allowRetry={}", groupName, allowRetry);
            ExtensionResponse response = getGroupChatByNameHelper.getGroupChatByNameWithValidation(groupName, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_BY_NAME_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_GROUP_CHAT_BY_NAME_REQUEST_FAILURE, 1L);
            logger.error("Error in getGroupChatByName for groupName: {}", groupName, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to retrieve group chat by name '" + groupName + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_2c951231-be29-408f-99b7-624321380a75",
            name = "Get All Teams",
            description = "Get a list of all Teams the user has access to",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Teams", type = "[ Entity(Team) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getAllTeams() {
        logger.debug("getTeams called");
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getAllTeams");

        try {
            List<Team> teams = teamService.getJoinedTeams(null);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_TEAMS_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();

            String responseMessage = teams.isEmpty()
                    ? "No teams found for the user"
                    : "Successfully retrieved " + teams.size() + " team(s)";

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Teams", teams,
                            "Response Message", responseMessage))
                    .build();
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_TEAMS_REQUEST_FAILURE, 1L);
            logger.error("Error in getAllTeams", cause);

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Teams", Collections.emptyList(),
                            "Response Message", "Failed to retrieve teams: " + cause.getMessage()))
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_0ad391d3-b11b-4c65-b5ab-548f297834d0",
            name = "Get All Group Chats",
            description = "Get details of all group chats the user has access to",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Group Chats", type = "[ Entity(Group Chat) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getAllGroupChats() {
        logger.debug("getAllGroupChats called");
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getAllGroupChats");
        try {
            List<GroupChat> groupChats = chatService.getAllGroupChats();
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_GROUP_CHATS_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();

            String responseMessage = groupChats.isEmpty()
                    ? "No group chats found for the user"
                    : "Successfully retrieved " + groupChats.size() + " group chat(s)";

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Group Chats", groupChats,
                            "Response Message", responseMessage))
                    .build();
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_ALL_GROUP_CHATS_REQUEST_FAILURE, 1L);
            logger.error("Error in getAllGroupChats", cause);

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Group Chats", Collections.emptyList(),
                            "Response Message", "Failed to retrieve group chats: " + cause.getMessage()))
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_c8f3a2b1-7e4d-4c9a-b2d3-8e7f6a5b4c3d",
            name = "Get Recent Group Chats",
            description = "Get recent group chats filtered by topic and chat type with an optional limit",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Group Chats", type = "[ Entity(Group Chat) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getRecentGroupChats(
            @Field.Text(name = "Limit", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Maximum number of chats to return (0 or blank = no limit)'")}) String limitStr,
            @Field.Text(name = "Topic", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Filter chats by topic (partial match)'")}) String topic,
            @Field.Text(name = "Chat Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Filter by chat type: group or meeting (leave blank for both)'")}) String chatType) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getRecentGroupChats");
        try {
            logger.info("getRecentGroupChats() called with limit={}, topic={}, chatType={}", limitStr, topic, chatType);
            int limitValue = 0;
            try { if (limitStr != null && !limitStr.isBlank()) limitValue = Integer.parseInt(limitStr.trim()); } catch (NumberFormatException ignored) {}
            List<GroupChat> groupChats = chatService.getFilteredGroupChats(limitValue, topic, chatType);
            operation.recordSuccess();

            String responseMessage = groupChats.isEmpty()
                    ? "No group chats found"
                    : "Successfully retrieved " + groupChats.size() + " group chat(s)";

            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Group Chats", groupChats,
                            "Response Message", responseMessage))
                    .build();
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error in getRecentGroupChats", cause);
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Group Chats", Collections.emptyList(),
                            "Response Message", "Failed to retrieve group chats: " + cause.getMessage()))
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_04ebedc3-5806-4e59-95ee-03cb61afe83c",
            name = "Get Channel Message Replies",
            description = "Get channel message replies (thread) by message Id",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Replies List", type = "[ Entity(Replies) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getChannelMessageReplies(
            @Field.Text(name = "Channel Message ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The message ID of the channel message'")}) String channelMessageID,
            @Field.Text(name = "Channel ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The channel ID of the Teams channel'")}) String channelID,
            @Field.Text(name = "Team ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The team ID of the Teams team'")}) String teamID,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelMessageReplies");

        try {
            logger.info("TeamsAPIsArea.getChannelMessageReplies() called with channelMessageId={}, channelId={}, teamId={}, allowRetry={}", channelMessageID, channelID, teamID, allowRetry);
            ExtensionResponse response = getChannelMessageRepliesHelper.getChannelMessageRepliesWithValidation(channelMessageID, channelID, teamID, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGE_REPLIES_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNEL_MESSAGE_REPLIES_REQUEST_FAILURE, 1L);
            logger.error("Error in getChannelMessageReplies for channelMessageID: {}, channelID: {}, teamID: {}", channelMessageID, channelID, teamID, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to retrieve channel message replies for messageID '" + channelMessageID + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_9af0bbb5-b9e9-4095-81eb-3d7bae65de30",
            name = "Get Channels of Team by Team ID",
            description = "Get a list of all channels for a given Team the user has access to",
            area = "Teams APIs",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Channels", type = "[ Entity(Channel) ]", required = false)
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse getChannelsOfTeamByTeamID(
            @Field.Text(name = "Team ID", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The team ID of the Teams team'")}) String teamID,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {
        logger.info("TeamsAPIsArea.getChannelsOfTeamByTeamID() called with teamID={}, allowRetry={}", teamID, allowRetry);
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("getChannelsOfTeamByTeamID");

        try {
            ExtensionResponse response = getChannelsOfTeamHelper.getChannelsOfTeamWithValidation(teamID, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_BY_TEAM_ID_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.GET_CHANNELS_BY_TEAM_ID_REQUEST_FAILURE, 1L);
            logger.error("Error in getChannelsOfTeamByTeamID for teamID: {}", teamID, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to retrieve channels for teamID '" + teamID + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_a9381a37-b98f-45c3-97e8-ec6fcc9fcb25",
            name = "Send Message to User",
            description = "Sends a 1:1 chat message to a specified user in Microsoft Teams via Graph API." +
                    "If the chat does not exist, it creates a new 1:1 chat thread with the user.",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Text(name = "Status Code", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'HTTP response code from the Graph API'")})
    @Field.Text(name = "Message Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'The unique ID of the message sent'")})
    @Field.Text(name = "Chat Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'ID of the 1:1 chat used or created'")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse sendMessageToUser(
            @Field.Text(name = "User Email", attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Email ID of the Teams user to whom message is to be sent'")}) String userEmail,
            @Field.Text(name = "Message Content", attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'The content of the message to be sent'")}) String messageContent,
            @Field.Boolean(name = "Reuse ChatId", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Reuse existing chat if found, else create a new one'")}) Boolean reuseChatId,
            @Field.Boolean(name = "Log Conversation", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Whether to store chat ID in Krista memory for reuse'")}) Boolean logConversation,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendMessageToUser");

        try {
            logger.info("sendMessageToUser() called with userEmail: {}, allowRetry: {}", userEmail, allowRetry);
            ExtensionResponse response = sendMessageToUserEmailValidationHelper.sendMessageToUserWithEmailValidation(
                    userEmail, messageContent, reuseChatId, logConversation, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_USER_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_MESSAGE_TO_USER_REQUEST_FAILURE, 1L);
            logger.error("Error in sendMessageToUser for user: {}", userEmail, cause);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to send message to user  '" + userEmail + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_0bf5b321-994b-442d-80a1-b736831354c1",
            name = "Send HTML Message to User",
            description = "Sends a 1:1 HTML chat message to a specified user in Microsoft Teams via Graph API.",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Text(name = "Status Code", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'HTTP response code from the Graph API'")})
    @Field.Text(name = "Message Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'The unique ID of the message sent'")})
    @Field.Text(name = "Chat Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'ID of the 1:1 chat used or created'")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse sendHTMLMessageToUser(
            @Field.Text(name = "User Email", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Email ID of the Teams user to whom message is to be sent'")}) String userEmail,
            @Field(name = "Message Content", type = "RichText", attributes = {@Attribute(name = "visualWidth", value = "L"), @Attribute(name = "toolTip", value = "'HTML message content to send'")}) String messageContent,
            @Field.Boolean(name = "Reuse ChatId", attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Reuse existing chat if found, else create a new one'")}) Boolean reuseChatId,
            @Field.Boolean(name = "Log Conversation", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"), @Attribute(name = "toolTip", value = "'Whether to store chat ID in Krista memory for reuse'")}) Boolean logConversation,
            @Field.Boolean(name = "Allow Retry", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}) Boolean allowRetry) {

        int messageLength = messageContent != null ? messageContent.length() : 0;
        logger.info("TeamsAPIsArea.sendHTMLMessageToUser() called with userEmail: {}, allowRetry: {}, HTML message length: {} characters",
                userEmail, allowRetry, messageLength);
        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("sendHTMLMessageToUser");

        try {
            ExtensionResponse response = sendHTMLMessageToUserValidationHelper.sendHTMLMessageToUserWithEmailValidation(
                    userEmail, messageContent, reuseChatId, logConversation, allowRetry);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_USER_REQUEST_SUCCESS, 1L);
            operation.recordSuccess();
            return response;
        } catch (GraphServiceException gse) {
            return GraphServiceExceptionHandler.handleMessageTooLargeError(gse, messageContent, "user '" + userEmail + "'",
                    TelemetryConstants.SEND_HTML_MESSAGE_TO_USER_REQUEST_FAILURE, operation, telemetryMetrics);
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_USER_REQUEST_FAILURE, 1L);
            logger.error("Error in sendHTMLMessageToUser for user: {}, HTML message length: {} characters",
                    userEmail, messageLength, cause);
            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to send html message to user  '" + userEmail + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_fe881b0c-b934-4c8c-9ecb-4a5095b80a02",
            name = "Send HTML message to teams channel",
            description = "Send HTML message to teams channel",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Boolean(name = "Is Message Delivered", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse sendHTMLMessageToTeamsChannel(
            @Field.Text(name = "Team Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String teamName,
            @Field.Text(name = "Channel Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String channelName,
            @Field(name = "Message", type = "RichText", attributes = {@Attribute(name = "visualWidth", value = "L")}) String message,
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
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            telemetryMetrics.incrementCounter(TelemetryConstants.SEND_HTML_MESSAGE_TO_CHANNEL_REQUEST_FAILURE, 1L);
            int messageLength = message != null ? message.length() : 0;
            logger.error("Error in sendHTMLMessageToTeamsChannel for team: {}, channel: {}, HTML message length: {} characters",
                    teamName, channelName, messageLength, cause);

            ExtensionResponse.Error error = new ErrorBuilder()
                    .exceptionType(ExtensionResponse.Error.ExceptionType.LOGIC_ERROR)
                    .errorMessage("Failed to send HTML message to channel '" + channelName + "' in team '" + teamName + "': " + cause.getMessage())
                    .build();

            return new ExtensionResponseBuilder()
                    .failure(error)
                    .build();
        }
    }

    @CatalogRequest(
            id = "localDomainRequest_b3e7c2a1-9f4d-4e8c-b1a2-7d6e5f4c3b2a",
            name = "Create Channel",
            description = "Creates a new channel in a Microsoft Teams team. Returns switch OFF if the team is not found or the channel already exists.",
            area = "Teams APIs",
            type = CatalogRequest.Type.CHANGE_SYSTEM, tool = true)
    @Field.Boolean(name = "Is Channel Created", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")})
    @Field.Text(name = "Response Message", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
            @Attribute(name = "toolTip", value = "'Message indicating success or details in case of failure'")})
    public ExtensionResponse createChannel(
            @Field.Text(name = "Team Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String teamName,
            @Field.Text(name = "Channel Name", attributes = {@Attribute(name = "visualWidth", value = "S")}) String channelName,
            @Field.PickOne(name = "Channel Type", values = {"Standard", "Private"}) String channelType,
            @Field.Text(name = "Members", required = false, attributes = {@Attribute(name = "visualWidth", value = "S"),
                    @Attribute(name = "toolTip", value = "'Comma-separated member emails (used for Private channels)'")}) String members) {

        TelemetryHelper.TelemetryOperation operation = telemetryHelper.startOperation("createChannel");

        try {
            logger.info("createChannel() called with teamName: {}, channelName: {}, channelType: {}", teamName, channelName, channelType);
            Map<String, Object> result = channelService.createChannel(teamName, channelName, channelType, members);
            boolean isCreated = Boolean.TRUE.equals(result.get("isCreated"));
            String responseMessage = (String) result.get("message");
            operation.recordSuccess();
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Is Channel Created", isCreated,
                            "Response Message", responseMessage))
                    .build();
        } catch (MustAuthorizeException cause) {
            throw cause;
        } catch (Exception cause) {
            operation.recordFailure(cause);
            logger.error("Error in createChannel for team: {}, channel: {}", teamName, channelName, cause);
            return new ExtensionResponseBuilder()
                    .success(Map.of(
                            "Is Channel Created", false,
                            "Response Message", "Failed to create channel '" + channelName + "': " + cause.getMessage()))
                    .build();
        }
    }
}
