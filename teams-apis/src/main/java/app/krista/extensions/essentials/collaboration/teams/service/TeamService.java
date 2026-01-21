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

import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Team;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.TeamCollectionPage;
import com.microsoft.graph.requests.TeamCollectionRequestBuilder;
import com.microsoft.graph.requests.ConversationMemberCollectionPage;
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
 * Service class for interacting with Microsoft Teams through the Microsoft Graph API.
 */
@Service
public class TeamService {

    private final GraphServiceClientProvider clientProvider;
    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

    /**
     * Constructs a new TeamService with the provided GraphServiceClientProviderFactory.
     *
     * @param providerFactory The factory for creating GraphServiceClientProvider instances.
     */
    @Inject
    public TeamService(GraphServiceClientProviderFactory providerFactory) {
        this.clientProvider = providerFactory.create();
    }

    /**
     * Get a list of teams that the user has joined.
     * If teamName is provided, filter the results by name.
     *
     * @param teamName Optional team name to filter by.
     * @return A list of teams that the user has joined.
     */
    public List<Team> getJoinedTeams(String teamName) {
        logger.info("getJoinedTeams() invoked with teamName: {}", teamName);
        ;
        GraphServiceClient<Request> graphServiceClientForUser = clientProvider.getGraphServiceClientForUser(null, null);

        try {
            // Retrieve joined teams from the Graph API
            TeamCollectionPage joinedTeams = graphServiceClientForUser.me().joinedTeams()
                    .buildRequest()
                    .get();

            List<Team> teamList = new ArrayList<>();

            // Process all pages of results
            while (joinedTeams != null) {
                for (com.microsoft.graph.models.Team team : joinedTeams.getCurrentPage()) {
                    // If teamName is provided, filter by name (case-insensitive)
                    if (teamName != null && !teamName.isBlank()) {
                        if (team.displayName != null && team.displayName.toLowerCase().contains(teamName.toLowerCase())) {
                            Team teamEntity = new Team();
                            teamEntity.iD = team.id;
                            teamEntity.name = team.displayName;
                            teamEntity.description = team.description;

                            // Retrieve and populate team members
                            teamEntity.members = getTeamMembers(graphServiceClientForUser, team.id);

                            // Log the members information
                            logger.info("Team '{}' has {} members", teamEntity.name,
                                    teamEntity.members != null ? teamEntity.members.size() : 0);
                            if (teamEntity.members != null && !teamEntity.members.isEmpty()) {
                                logger.info("Members for team '{}': {}", teamEntity.name, teamEntity.members);
                                for (int i = 0; i < teamEntity.members.size(); i++) {
                                    Map<String, Object> member = teamEntity.members.get(i);
                                    logger.info("Member {}: Name='{}', Email='{}'",
                                            i + 1, member.get("Name"), member.get("Email"));
                                }
                            } else {
                                logger.warn("No members found for team '{}' - this may be due to insufficient permissions",
                                        teamEntity.name);
                            }

                            teamList.add(teamEntity);
                        }
                    } else {
                        // If no teamName is provided, add all teams
                        Team teamEntity = new Team();
                        teamEntity.iD = team.id;
                        teamEntity.name = team.displayName;
                        teamEntity.description = team.description;

                        // Retrieve and populate team members
                        teamEntity.members = getTeamMembers(graphServiceClientForUser, team.id);

                        // Log the members information
                        logger.info("Team '{}' has {} members", teamEntity.name,
                                teamEntity.members != null ? teamEntity.members.size() : 0);
                        if (teamEntity.members != null && !teamEntity.members.isEmpty()) {
                            logger.info("Members for team '{}': {}", teamEntity.name, teamEntity.members);
                            for (int i = 0; i < teamEntity.members.size(); i++) {
                                Map<String, Object> member = teamEntity.members.get(i);
                                logger.info("Member {}: Name='{}', Email='{}'",
                                        i + 1, member.get("Name"), member.get("Email"));
                            }
                        } else {
                            logger.warn("No members found for team '{}' - this may be due to insufficient permissions",
                                    teamEntity.name);
                        }

                        teamList.add(teamEntity);
                    }
                }

                // Check if there are more pages
                TeamCollectionRequestBuilder nextPage = joinedTeams.getNextPage();
                if (nextPage != null) {
                    joinedTeams = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }

            logger.info("Found {} teams", teamList.size());
            return teamList;
        } catch (Exception cause) {
            logger.error("Error retrieving joined teams: {}", cause.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves a Team based on the provided teamId.
     *
     * @param teamId The ID of the Team to retrieve.
     * @return The Team object corresponding to the provided teamId, or null if not found or an exception occurs.
     * @throws IllegalArgumentException if teamId is null or empty.
     */
    public Team getTeamById(String teamId) {
        if (teamId == null || teamId.isEmpty()) {
            return null;
        }

        GraphServiceClient<Request> graphServiceClient = clientProvider.getGraphServiceClientForUser(null, null);

        try {
            // Attempt to retrieve the Team from Microsoft Graph using the provided teamId.
            com.microsoft.graph.models.Team team = graphServiceClient.teams(teamId)
                    .buildRequest()
                    .get();
            if (team != null) {
                // If successful, create and return a new Team object with the retrieved data.
                Team teamEntity = new Team();
                teamEntity.iD = team.id;
                teamEntity.name = team.displayName;
                teamEntity.description = team.description;

                // Retrieve and populate team members
                teamEntity.members = getTeamMembers(graphServiceClient, team.id);

                // Log the members information
                logger.info("Team '{}' has {} members", teamEntity.name,
                        teamEntity.members != null ? teamEntity.members.size() : 0);
                if (teamEntity.members != null && !teamEntity.members.isEmpty()) {
                    logger.info("Members for team '{}': {}", teamEntity.name, teamEntity.members);
                    for (int i = 0; i < teamEntity.members.size(); i++) {
                        Map<String, Object> member = teamEntity.members.get(i);
                        logger.info("Member {}: Name='{}', Email='{}'",
                                i + 1, member.get("Name"), member.get("Email"));
                    }
                } else {
                    logger.warn("No members found for team '{}' - this may be due to insufficient permissions",
                            teamEntity.name);
                }

                return teamEntity;
            }
        } catch (GraphServiceException cause) {
            // Handle exceptions, log details, and return null in case of an exception.
            logger.error("Error retrieving team by ID: {}", cause.getMessage());
        }
        return null;
    }

    /**
     * Retrieve team members from Microsoft Graph API.
     *
     * @param client The Graph API client.
     * @param teamId The ID of the team.
     * @return A list of maps containing member information (Name and Email).
     */
    private List<Map<String, Object>> getTeamMembers(GraphServiceClient<Request> client, String teamId) {
        List<Map<String, Object>> membersList = new ArrayList<>();

        try {
            logger.info("Retrieving members for team: {}", teamId);
            ConversationMemberCollectionPage members = client.teams(teamId)
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

            logger.info("Successfully retrieved {} members for team: {}", membersList.size(), teamId);

            // Print all members for debugging
            if (!membersList.isEmpty()) {
                logger.info("=== MEMBERS LIST FOR TEAM {} ===", teamId);
                for (int i = 0; i < membersList.size(); i++) {
                    Map<String, Object> member = membersList.get(i);
                    logger.info("Member {}: {}", i + 1, member);
                }
                logger.info("=== END MEMBERS LIST ===");
            } else {
                logger.warn("No members were added to the list for team: {}", teamId);
            }

        } catch (Exception e) {
            logger.error("Error retrieving team members for team: {}. Error: {}", teamId, e.getMessage());
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
        }

        logger.info("Returning members list with {} items for team: {}", membersList.size(), teamId);
        return membersList;
    }
}
