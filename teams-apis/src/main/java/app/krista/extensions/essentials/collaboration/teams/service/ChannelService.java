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
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.requests.ChannelCollectionPage;
import com.microsoft.graph.requests.ChannelCollectionRequestBuilder;
import com.microsoft.graph.requests.ConversationMemberCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.models.ConversationMember;
import com.microsoft.graph.models.AadUserConversationMember;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for working with Microsoft Teams channels.
 */
@Service
public class ChannelService {
    private final GraphServiceClientProvider clientProvider;
    private static final Logger logger = LoggerFactory.getLogger(ChannelService.class);

    @Inject
    public ChannelService(GraphServiceClientProviderFactory providerFactory) {
        this.clientProvider = providerFactory.create();
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

        GraphServiceClient<Request> graphServiceClientForUser = clientProvider.getGraphServiceClientForUser(null, null);

        // Retrieve channels from the Graph API based on the provided parameters
        ChannelCollectionPage channels = getChannelsFromGraphAPI(graphServiceClientForUser, teamId);

        // Process and return the list of channels
        return retrieveChannelsList(channels, teamId, channelName);
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

                        // Retrieve and populate channel members
                        GraphServiceClient<Request> graphClient = clientProvider.getGraphServiceClientForUser(null, null);
                        channelEntity.members = getChannelMembers(graphClient, teamId, channel.id);

                        if (channelEntity.members != null && !channelEntity.members.isEmpty()) {
                            for (int i = 0; i < channelEntity.members.size(); i++) {
                                Map<String, Object> member = channelEntity.members.get(i);
                            }
                        }
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
        GraphServiceClient<Request> graphClient = clientProvider.getGraphServiceClientForUser(null, null);

        while (channels != null) {
            for (com.microsoft.graph.models.Channel channel : channels.getCurrentPage()) {
                Channel myChannel = new Channel();
                myChannel.iD = channel.id;
                myChannel.name = channel.displayName;
                myChannel.description = channel.description;
                myChannel.email = channel.email;
                myChannel.teamID = teamId;

                // Retrieve and populate channel members
                myChannel.members = getChannelMembers(graphClient, teamId, channel.id);

                // Log the members information for each channel
                logger.info("Channel '{}' has {} members", myChannel.name,
                        myChannel.members != null ? myChannel.members.size() : 0);
                if (myChannel.members != null && !myChannel.members.isEmpty()) {
                    logger.info("Members for channel '{}': {}", myChannel.name, myChannel.members);
                    for (int i = 0; i < myChannel.members.size(); i++) {
                        Map<String, Object> member = myChannel.members.get(i);
                        logger.info("Member {}: Name='{}', Email='{}'",
                                i + 1, member.get("Name"), member.get("Email"));
                    }
                } else {
                    logger.warn("No members found for channel '{}' - this may be due to insufficient permissions",
                            myChannel.name);
                }

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
            GraphServiceClient<Request> graphServiceClientForUser = clientProvider.getGraphServiceClientForUser(null, null);

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

    /**
     * Retrieve channel members from Microsoft Graph API.
     *
     * @param client    The Graph API client.
     * @param teamId    The ID of the team.
     * @param channelId The ID of the channel.
     * @return A list of maps containing member information (Name and Email).
     */
    private List<Map<String, Object>> getChannelMembers(GraphServiceClient<Request> client, String teamId, String channelId) {
        List<Map<String, Object>> membersList = new ArrayList<>();

        try {
            logger.info("Retrieving members for channel: {} in team: {}", channelId, teamId);
            logger.info("Making API call to: /teams/{}/channels/{}/members", teamId, channelId);

            ConversationMemberCollectionPage members = client.teams(teamId)
                    .channels(channelId)
                    .members()
                    .buildRequest()
                    .get();

            logger.info("Successfully received member collection page from API");
            int pageCount = 0;

            while (members != null) {
                pageCount++;
                logger.info("Processing page {} of members, current page has {} members",
                        pageCount, members.getCurrentPage().size());

                for (ConversationMember member : members.getCurrentPage()) {
                    logger.info("Processing member of type: {}", member.getClass().getSimpleName());

                    if (member instanceof AadUserConversationMember) {
                        AadUserConversationMember aadMember = (AadUserConversationMember) member;

                        logger.info("Found AAD User Member - ID: {}, DisplayName: {}, Email: {}",
                                aadMember.id, aadMember.displayName, aadMember.email);

                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("Name", aadMember.displayName != null ? aadMember.displayName : "Unknown");
                        memberInfo.put("Email", aadMember.email != null ? aadMember.email : "");

                        membersList.add(memberInfo);
                        logger.info("Added member to list: Name='{}', Email='{}'",
                                memberInfo.get("Name"), memberInfo.get("Email"));
                    } else {
                        logger.warn("Skipping member of unsupported type: {}", member.getClass().getSimpleName());
                    }
                }

                // Check if there are more pages
                var nextPage = members.getNextPage();
                if (nextPage != null) {
                    members = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }

            logger.info("Successfully retrieved {} members for channel: {}", membersList.size(), channelId);

            // Print all members for debugging
            if (!membersList.isEmpty()) {
                logger.info("=== MEMBERS LIST FOR CHANNEL {} ===", channelId);
                for (int i = 0; i < membersList.size(); i++) {
                    Map<String, Object> member = membersList.get(i);
                    logger.info("Member {}: {}", i + 1, member);
                }
                logger.info("=== END MEMBERS LIST ===");
            } else {
                logger.warn("No members were added to the list for channel: {}", channelId);
            }

        } catch (Exception e) {
            logger.error("Error retrieving channel members for channel: {} in team: {}. Error: {}",
                    channelId, teamId, e.getMessage());
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            // Return empty list on error to avoid breaking the channel retrieval
        }

        logger.info("Returning members list with {} items for channel: {}", membersList.size(), channelId);
        return membersList;
    }
}

