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

package app.krista.extensions.essentials.collaboration.teams.service;

import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Channel;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Team;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for working with Microsoft Teams channels.
 */
@Service
public class ChannelService {
    private final GraphServiceClientProviderFactory providerFactory;
    private final TeamService teamService;
    private static final Logger logger = LoggerFactory.getLogger(ChannelService.class);

    @Inject
    public ChannelService(GraphServiceClientProviderFactory providerFactory, TeamService teamService) {
        this.providerFactory = providerFactory;
        this.teamService = teamService;
    }

    /**
     * Resolves the provider on every call — never cached. This service is an HK2 singleton, so
     * caching would freeze the first caller's context (typically the operator) and reuse it for
     * every user, breaking per-user auth. {@code providerFactory.create()} captures the current
     * request's context (invokeAsUser / wsContact) at call time, and creating it here (not in the
     * constructor) lets a per-user MustAuthorizeException surface from the catalog method.
     */
    private GraphServiceClientProvider clientProvider() {
        return providerFactory.create();
    }

    /**
     * Acquires the delegated Graph client for the current user, translating the SDK's
     * checked {@link IOException} (token acquisition failure) into a RuntimeException.
     */
    private GraphServiceClient<Request> getUserClient() {
        try {
            return clientProvider().getGraphServiceClientForUser(null, null);
        } catch (IOException cause) {
            throw new RuntimeException("We couldn't acquire a Microsoft Graph client for the current user. Please try again.", cause);
        }
    }

    /**
     * Acquires a Graph client preferring per-user (wsContact) credentials over operator,
     * falling back to operator silently if no per-user credentials exist.
     */
    private GraphServiceClient<Request> getPreferringPerUserClient() {
        return providerFactory.getPreferringPerUserGraphClient();
    }

    /**
     * Get a list of channels for a given team. Optionally, filter by channel name.
     *
     * @param teamId      The ID of the team.
     * @param channelName The name of the channel to filter by (optional).
     * @return A list of channels in the team.
     */
    public List<Channel> getChannelsOfTeam(String teamId, String channelName) {

        if (teamId == null || teamId.isBlank()) {
            logger.warn("Team ID is null or blank. Returning an empty list.");
            return Collections.emptyList();
        }

        GraphServiceClient<Request> graphServiceClientForUser = getPreferringPerUserClient();

        // Retrieve channels from the Graph API based on the provided parameters
        ChannelCollectionPage channels = getChannelsFromGraphAPI(graphServiceClientForUser, teamId);

        // Process and return the list of channels
        return retrieveChannelsList(channels, teamId, channelName);
    }

    /**
     * Helper method to retrieve channels from the team name.
     *
     * @param teamName The name of the team
     * @return List of channels for the team
     */
    public List<Channel> getChannelsFromTeamName(String teamName) {
        logger.debug("Calling Teams API: teamService.getJoinedTeamsByNamePreferringPerUser with teamName={}", teamName);
        List<Team> joinedTeams = teamService.getJoinedTeamsByNamePreferringPerUser(teamName);
        logger.debug("Teams API response: received {} joined teams", joinedTeams != null ? joinedTeams.size() : 0);

        if (joinedTeams != null) {
            logger.debug("Calling Teams API: channelService.getChannelsOfTeam with teamId={}", joinedTeams.getFirst().iD);

            List<Channel> channels = getChannelsOfTeam(joinedTeams.getFirst().iD, null);
            logger.debug("Teams API response: received {} channels for team {}",
                    channels != null ? channels.size() : 0, teamName);

            return channels;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get a collection of channels from the Microsoft Graph API.
     *
     * @param client The Graph API client.
     * @param teamId The ID of the team.
     * @return A collection of channels from the Graph API.
     */
    private ChannelCollectionPage getChannelsFromGraphAPI(GraphServiceClient<Request> client, String teamId) {

        // Retrieve all channels for the team
        return client.teams(teamId).channels()
                .buildRequest()
                .get();
    }

    /**
     * Retrieve a list of channels based on the provided parameters.
     *
     * @param channels    The collection of channels from the Graph API.
     * @param teamId      The ID of the team.
     * @param channelName The name of the channel to filter by (optional).
     * @return A list of channels based on the provided parameters.
     */
    private List<Channel> retrieveChannelsList(ChannelCollectionPage channels, String teamId, String channelName) {

        List<Channel> channelList = new ArrayList<>();

        if (channelName != null && !channelName.isBlank()) {
            Channel matchingChannel = findMatchingChannel(channels, teamId, channelName);
            if (matchingChannel != null) {
                channelList.add(matchingChannel);
            }
        } else {
            channelList.addAll(getAllChannels(channels, teamId));
        }

        return channelList;
    }

    /**
     * Find a matching channel in the provided collection of channels.
     *
     * @param channels    The collection of channels from the Graph API.
     * @param teamId      The ID of the team.
     * @param channelName The name of the channel to match.
     * @return The matching channel or null if no match is found.
     */
    private Channel findMatchingChannel(ChannelCollectionPage channels, String teamId, String channelName) {

        if (channelName != null && !channelName.isBlank()) {
            while (channels != null) {
                for (com.microsoft.graph.models.Channel channel : channels.getCurrentPage()) {
                    if (channelName.equalsIgnoreCase(channel.displayName)) {
                        logger.info("Matching channel found: {}", channel.displayName);
                        Channel channelEntity = new Channel();
                        channelEntity.iD = channel.id;
                        channelEntity.name = channel.displayName;
                        channelEntity.description = channel.description;
                        channelEntity.email = channel.email;
                        channelEntity.teamID = teamId;
                        return channelEntity;
                    }
                }

                ChannelCollectionRequestBuilder nextPage = channels.getNextPage();
                if (nextPage != null) {
                    channels = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }
        }

        return null; // No matching channel found
    }

    /**
     * Retrieve all channels from the provided collection of channels.
     *
     * @param channels The collection of channels from the Graph API.
     * @param teamId   The ID of the team.
     * @return A list of all channels from the provided collection.
     */
    private List<Channel> getAllChannels(ChannelCollectionPage channels, String teamId) {

        List<Channel> channelList = new ArrayList<>();
        GraphServiceClient<Request> graphClient = getUserClient();

        while (channels != null) {
            for (com.microsoft.graph.models.Channel channel : channels.getCurrentPage()) {
                Channel myChannel = new Channel();
                myChannel.iD = channel.id;
                myChannel.name = channel.displayName;
                myChannel.description = channel.description;
                myChannel.email = channel.email;
                myChannel.teamID = teamId;
                channelList.add(myChannel);
            }

            ChannelCollectionRequestBuilder nextPage = channels.getNextPage();
            if (nextPage != null) {
                channels = nextPage.buildRequest().get();
            } else {
                break;
            }
        }

        return channelList;
    }

    /**
     * Create a new channel in a team.
     *
     * @param teamName    The name of the team.
     * @param channelName The name of the channel to create.
     * @param channelType "Standard" or "Private".
     * @param members     Comma-separated member emails (used for Private channels).
     * @return Map with "isCreated" (Boolean) and "message" (String).
     */
    public Map<String, Object> createChannel(String teamName, String channelName, String channelType, String members) {
        // Validate channel name against Microsoft Teams character restrictions
        if (channelName != null && channelName.chars().anyMatch(c -> "&~#%*{}+/\\:<>?|'\"\\,".indexOf(c) >= 0)) {
            return Map.of("isCreated", false,
                    "message", "Channel name contains invalid characters. These are not allowed in Teams channel names: & ~ # % * { } + / \\ : < > ? | ' \" ,");
        }

        List<Team> teams = teamService.getJoinedTeams(teamName);

        String teamId;
        if (teams == null || teams.isEmpty()) {
            if ("Private".equalsIgnoreCase(channelType)) {
                return Map.of("isCreated", false,
                        "message", "Team '" + teamName + "' not found. Please create the team in Microsoft Teams, add the configured account as a member/owner, then run this request again.");
            }
            // Standard channel: auto-create the team
            logger.info("Team '{}' not found. Creating it automatically for standard channel.", teamName);
            teamId = teamService.createTeamAndGetId(teamName);
            if (teamId == null) {
                return Map.of("isCreated", false,
                        "message", "Team '" + teamName + "' could not be created. Please create it manually in Microsoft Teams and run this request again.");
            }
        } else {
            teamId = teams.getFirst().iD;
        }

        List<Channel> existing = getChannelsOfTeam(teamId, channelName);
        if (!existing.isEmpty()) {
            return Map.of("isCreated", false, "message", "Channel '" + channelName + "' already exists in team '" + teamName + "'.");
        }

        boolean isPrivate = "Private".equalsIgnoreCase(channelType);

        GraphServiceClient<Request> graphClient = getUserClient();

        com.microsoft.graph.models.Channel newChannel = new com.microsoft.graph.models.Channel();
        newChannel.displayName = channelName;
        newChannel.membershipType = isPrivate ? ChannelMembershipType.PRIVATE : ChannelMembershipType.STANDARD;

        if (isPrivate) {
            List<ConversationMember> memberList = new ArrayList<>();

            // Add current authenticated user as owner (required by Graph API for private channels)
            User currentUser = graphClient.me().buildRequest().select("id").get();
            if (currentUser != null && currentUser.id != null) {
                AadUserConversationMember owner = new AadUserConversationMember();
                owner.oDataType = "#microsoft.graph.aadUserConversationMember";
                owner.roles = new ArrayList<>(List.of("owner"));
                owner.additionalDataManager().put("user@odata.bind",
                        new com.google.gson.JsonPrimitive("https://graph.microsoft.com/v1.0/users('" + currentUser.id + "')"));
                memberList.add(owner);
            }

            // Add extra members from comma-separated email list
            if (members != null && !members.isBlank()) {
                for (String email : members.split(",")) {
                    String trimmed = email.trim();
                    if (!trimmed.isEmpty()) {
                        String userId = getUserIdByEmail(graphClient, trimmed);
                        if (userId != null) {
                            AadUserConversationMember m = new AadUserConversationMember();
                            m.oDataType = "#microsoft.graph.aadUserConversationMember";
                            m.roles = new ArrayList<>(List.of("member"));
                            m.additionalDataManager().put("user@odata.bind",
                                    new com.google.gson.JsonPrimitive("https://graph.microsoft.com/v1.0/users('" + userId + "')"));
                            memberList.add(m);
                        } else {
                            logger.warn("Could not resolve user ID for email: {}", trimmed);
                        }
                    }
                }
            }

            if (!memberList.isEmpty()) {
                newChannel.members = new ConversationMemberCollectionPage(memberList, null);
            }
        }

        try {
            graphClient.teams(teamId).channels().buildRequest().post(newChannel);
        } catch (Exception e) {
            String errMsg = e.getMessage() != null ? e.getMessage() : "";
            if (errMsg.contains("not part of the parent roster") || errMsg.contains("parent roster")) {
                return Map.of("isCreated", false,
                        "message", "Cannot create private channel '" + channelName + "' in team '" + teamName +
                                "'. The configured account is not a member of this team. " +
                                "Please open the team in Microsoft Teams, add the configured account as a member/owner, and run this request again.");
            }
            throw e;
        }

        return Map.of("isCreated", true, "message", "Channel '" + channelName + "' created successfully in team '" + teamName + "'.");
    }

    private String getUserIdByEmail(GraphServiceClient<Request> graphClient, String userEmail) {
        try {
            String filterQuery = "mail eq '" + userEmail + "' or userPrincipalName eq '" + userEmail + "'";
            UserCollectionPage users = graphClient.users()
                    .buildRequest()
                    .filter(filterQuery)
                    .select("id,mail,userPrincipalName")
                    .get();
            if (users != null && !users.getCurrentPage().isEmpty()) {
                return users.getCurrentPage().get(0).id;
            }
        } catch (Exception e) {
            logger.warn("Failed to resolve user ID for email: {}", userEmail, e);
        }
        return null;
    }

    /**
     * Validate if a channel exists and is accessible by attempting to retrieve it directly.
     * This method makes an API call to check if the channel exists and the user has access to it.
     *
     * @param channelId The ID of the channel to validate.
     * @param teamId    The ID of the team (optional, used for better error context).
     * @return true if the channel exists and is accessible, false otherwise.
     */
    public boolean validateChannelExists(String channelId, String teamId) {
        if (channelId == null || channelId.isBlank()) {
            logger.warn("Channel ID is null or blank. Cannot validate.");
            return false;
        }

        try {
            GraphServiceClient<Request> graphServiceClientForUser = getPreferringPerUserClient();

            if (teamId != null && !teamId.isBlank()) {
                // If team ID is provided, try to get the specific channel from the team
                try {
                    var channel = graphServiceClientForUser.teams(teamId.trim())
                            .channels(channelId.trim())
                            .buildRequest()
                            .get();

                    if (channel != null) {
                        logger.info("Channel validation successful: channel {} exists in team {}", channelId, teamId);
                        return true;
                    }
                } catch (Exception e) {
                    logger.warn("Channel validation failed for channel {} in team {}: {}", channelId, teamId, e.getMessage());
                    return false;
                }
            } else {
                // If no team ID provided, we need to search through all accessible teams
                // This is less efficient but necessary when team ID is not available
                logger.info("No team ID provided, searching for channel {} across all accessible teams", channelId);

                // Get all joined teams and search for the channel
                try {
                    // This would require TeamService, but to avoid circular dependency,
                    // we'll return false and recommend providing team ID
                    logger.warn("Channel validation requires team ID for efficient validation. Channel ID: {}", channelId);
                    return false;
                } catch (Exception e) {
                    logger.error("Error during channel validation: {}", e.getMessage());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error validating channel {}: {}", channelId, e.getMessage());
            return false;
        }

        return false;
    }
}

