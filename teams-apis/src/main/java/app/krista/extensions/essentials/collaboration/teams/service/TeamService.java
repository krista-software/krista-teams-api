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
import com.google.gson.JsonPrimitive;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.AadUserConversationMember;
import com.microsoft.graph.models.ConversationMember;
import com.microsoft.graph.requests.ConversationMemberCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.TeamCollectionPage;
import com.microsoft.graph.requests.TeamCollectionRequestBuilder;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for interacting with Microsoft Teams through the Microsoft Graph API.
 */
@Service
public class TeamService {

    private final GraphServiceClientProviderFactory providerFactory;
    private static final Logger logger = LoggerFactory.getLogger(TeamService.class);

    /**
     * Constructs a new TeamService with the provided GraphServiceClientProviderFactory.
     *
     * @param providerFactory The factory for creating GraphServiceClientProvider instances.
     */
    @Inject
    public TeamService(GraphServiceClientProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    /**
     * Resolves the provider on every call — never cached. This service is an HK2 singleton, and
     * {@code providerFactory.create()} captures the current request's context (invokeAsUser /
     * wsContact) at call time; caching would freeze the first caller's context (typically the
     * operator) and reuse it for every user, breaking per-user auth. Creating it here (in a
     * method, not the constructor) also lets a per-user {@code MustAuthorizeException} surface
     * from the catalog method so the platform renders the per-user authorization link.
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
     * Get a list of teams that the user has joined.
     * If teamName is provided, filter the results by name.
     *
     * @param teamName Optional team name to filter by.
     * @return A list of teams that the user has joined.
     */
    public List<Team> getJoinedTeams(String teamName) {
        logger.info("getJoinedTeams() invoked with teamName: {}", teamName);
        GraphServiceClient<Request> graphServiceClientForUser = getUserClient();

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
    }

    /**
     * Returns all joined teams using per-user credentials (preferring wsContact over operator).
     * Used by validators that need to check team membership without a name filter.
     */
    public List<Team> getJoinedTeamsPreferringPerUser() {
        logger.info("getJoinedTeamsPreferringPerUser() invoked");
        GraphServiceClient<Request> client = providerFactory.getPreferringPerUserGraphClient();

        TeamCollectionPage joinedTeams = client.me().joinedTeams().buildRequest().get();
        List<Team> teamList = new ArrayList<>();
        while (joinedTeams != null) {
            for (com.microsoft.graph.models.Team team : joinedTeams.getCurrentPage()) {
                Team teamEntity = new Team();
                teamEntity.iD = team.id;
                teamEntity.name = team.displayName;
                teamList.add(teamEntity);
            }
            TeamCollectionRequestBuilder nextPage = joinedTeams.getNextPage();
            if (nextPage != null) {
                joinedTeams = nextPage.buildRequest().get();
            } else {
                break;
            }
        }
        logger.info("getJoinedTeamsPreferringPerUser() found {} teams", teamList.size());
        return teamList;
    }

    /**
     * Finds joined teams by name using per-user credentials (preferring wsContact over operator).
     * Used by channel send operations so the correct user's team is found, not the operator's.
     * Returns lightweight Team objects (no members) sufficient for routing a send.
     */
    public List<Team> getJoinedTeamsByNamePreferringPerUser(String teamName) {
        logger.info("getJoinedTeamsByNamePreferringPerUser() invoked with teamName: {}", teamName);
        GraphServiceClient<Request> client = providerFactory.getPreferringPerUserGraphClient();

        TeamCollectionPage joinedTeams = client.me().joinedTeams().buildRequest().get();
        List<Team> teamList = new ArrayList<>();
        while (joinedTeams != null) {
            for (com.microsoft.graph.models.Team team : joinedTeams.getCurrentPage()) {
                if (teamName != null && !teamName.isBlank()) {
                    if (team.displayName != null && team.displayName.toLowerCase().contains(teamName.toLowerCase())) {
                        Team teamEntity = new Team();
                        teamEntity.iD = team.id;
                        teamEntity.name = team.displayName;
                        teamList.add(teamEntity);
                    }
                }
            }
            TeamCollectionRequestBuilder nextPage = joinedTeams.getNextPage();
            if (nextPage != null) {
                joinedTeams = nextPage.buildRequest().get();
            } else {
                break;
            }
        }
        logger.info("getJoinedTeamsByNamePreferringPerUser() found {} teams for name: {}", teamList.size(), teamName);
        return teamList;
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

        GraphServiceClient<Request> graphServiceClient = getUserClient();

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
        logger.info("Retrieving members for team: {}", teamId);
        List<Map<String, Object>> membersList = new ArrayList<>();

        try {
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
        } catch (com.microsoft.graph.http.GraphServiceException e) {
            logger.error("Error retrieving team members for team: {}. Error: {}", teamId, e.getMessage());
            logger.error("Exception type: GraphServiceException");
            logger.warn("Returning members list with 0 items for team: {} - this may be due to insufficient permissions.", teamId);
        } catch (Exception e) {
            logger.error("Unexpected error retrieving team members for team: {}. Error: {}", teamId, e.getMessage(), e);
            logger.warn("Returning members list with 0 items for team: {}", teamId);
        }

        logger.info("Returning members list with {} items for team: {}", membersList.size(), teamId);
        return membersList;
    }

    /**
     * Creates a new Microsoft Teams team and returns its ID once provisioned.
     * Polls for up to 30 seconds for the team to become available.
     *
     * @param teamName The display name for the new team.
     * @return The team ID, or null if creation/provisioning failed.
     */
    public String createTeamAndGetId(String teamName) {
        GraphServiceClient<Request> graphClient = getUserClient();

        com.microsoft.graph.models.Team newTeam = new com.microsoft.graph.models.Team();
        newTeam.displayName = teamName;
        newTeam.description = teamName;
        newTeam.additionalDataManager().put("template@odata.bind",
                new JsonPrimitive("https://graph.microsoft.com/v1.0/teamsTemplates('standard')"));

        try {
            com.microsoft.graph.models.Team created = graphClient.teams().buildRequest().post(newTeam);
            if (created != null && created.id != null) {
                logger.info("Team '{}' created with ID: {}", teamName, created.id);
                return created.id;
            }
        } catch (Exception e) {
            // 202 Accepted — team is being provisioned asynchronously
            logger.info("Team creation initiated (async) for '{}': {}", teamName, e.getMessage());
        }

        // Poll for up to 30 seconds for the team to appear in joinedTeams
        for (int i = 0; i < 15; i++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            List<Team> found = getJoinedTeams(teamName);
            for (Team t : found) {
                if (teamName.equalsIgnoreCase(t.name)) {
                    logger.info("Team '{}' found after polling with ID: {}", teamName, t.iD);
                    return t.iD;
                }
            }
        }

        logger.warn("Team '{}' not found after provisioning timeout", teamName);
        return null;
    }
}
