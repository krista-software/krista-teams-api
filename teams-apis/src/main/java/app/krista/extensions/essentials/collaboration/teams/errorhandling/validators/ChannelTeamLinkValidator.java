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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.validators;

import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Channel;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.FieldTypes;
import app.krista.extensions.essentials.collaboration.teams.errorhandling.constants.TeamsResources;
import app.krista.extensions.essentials.collaboration.teams.service.ChannelService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Validator for Channel ID and Team ID relationship.
 * Validates that the provided Channel ID is linked with the provided Team ID.
 */
@Service
public class ChannelTeamLinkValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelTeamLinkValidator.class);

    private final ChannelService channelService;

    @Inject
    public ChannelTeamLinkValidator(ChannelService channelService) {
        this.channelService = channelService;
        LOGGER.info("ChannelTeamLinkValidator constructor called");
        System.out.println("ChannelTeamLinkValidator constructor - Initializing with ChannelService dependency");
    }

    @Override
    public Boolean validate(String resourceId, Map<ValidationResource, String> context) {
        System.out.println("ChannelTeamLinkValidator.validate() - Starting validation");
        LOGGER.debug("ChannelTeamLinkValidator.validate() called with context: {}", context);

        try {
            // Extract Channel ID and Team ID from context
            String channelID = context.get(ValidationResource.CHANNEL_ID);
            String teamID = context.get(ValidationResource.TEAM_ID);

            // Check if both IDs are provided
            if (channelID == null || channelID.trim().isEmpty() || teamID == null || teamID.trim().isEmpty()) {
                LOGGER.warn("Channel ID or Team ID is null or empty - channelID: {}, teamID: {}", channelID, teamID);
                return false;
            }

            // Get all channels for the team
            List<Channel> channels = channelService.getChannelsOfTeam(teamID.trim(), null);

            if (channels == null || channels.isEmpty()) {
                LOGGER.warn("No channels found for teamID: {}", teamID);
                return false;
            }

            // Check if the channel ID exists in the team's channels
            boolean isLinked = channels.stream()
                    .anyMatch(channel -> channelID.trim().equals(channel.iD));

            if (isLinked) {
                LOGGER.info("Channel ID {} is linked with Team ID {}", channelID, teamID);
                return true;
            } else {
                LOGGER.warn("Channel ID {} is NOT linked with Team ID {}", channelID, teamID);
                return false;
            }

        } catch (Exception e) {
            LOGGER.error("Error during channel-team link validation: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getFetchFieldName() {
        return "Channel ID and Team ID";
    }

    @Override
    public String getFieldType() {
        return FieldTypes.TEXT_FIELD;
    }

    @Override
    public String getFetchStepMessage() {
        return "Please enter a valid Channel ID that is linked with the provided Team ID.";
    }

    @Override
    public String getConfirmationStepMessage(String resourceId, Map<ValidationResource, String> context) {
        String channelID = context.get(ValidationResource.CHANNEL_ID);
        String teamID = context.get(ValidationResource.TEAM_ID);
        return String.format("The provided Channel ID is not Linked with Team ID. Please Enter the correct Data. Channel ID '%s' is not associated with Team ID '%s'.", channelID, teamID);
    }

    @Override
    public String getErrMessage(String resourceId) {
        return "The provided Channel ID is not linked with the Team ID. Please provide a valid Channel ID that belongs to the specified Team.";
    }
}
