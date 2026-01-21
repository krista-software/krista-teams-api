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

package app.krista.extensions.essentials.collaboration.teams.errorhandling.utils;

import app.krista.extensions.essentials.collaboration.teams.service.ChatService;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper class for Send HTML Message to Teams Channel functionality.
 * Contains all the logic for sending HTML messages to Teams channels with comprehensive error handling.
 */
@Singleton
@Service
public class SendHTMLMessageToChannelUtils {

    private static final Logger logger = LoggerFactory.getLogger(SendHTMLMessageToChannelUtils.class);
    private final ChatService chatService;

    @Inject
    public SendHTMLMessageToChannelUtils(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Send HTML message to Teams channel with comprehensive error handling and logging.
     * This method contains all the logic for sending HTML messages to channels, including
     * permission error handling and fallback mechanisms.
     *
     * @param teamName    The name of the team.
     * @param channelName The name of the channel.
     * @param message     The HTML message content.
     * @return Boolean indicating success or failure.
     */
    public Boolean sendHTMLMessageToChannelWithDetails(String teamName, String channelName, String message) {
        try {
            // Call ChatService with isHtml=true
            return chatService.sendMessageToChannel(teamName, channelName, message, true);

        } catch (Exception serviceException) {
            logger.error("ChatService.sendMessageToChannel() failed for team: {}, channel: {}", 
                    teamName, channelName, serviceException);

            // Check if it's a permission error
            if (serviceException.getMessage() != null &&
                    (serviceException.getMessage().contains("Authorization_RequestDenied") ||
                            serviceException.getMessage().contains("Insufficient privileges") ||
                            serviceException.getMessage().contains("403"))) {

                logger.warn("Permission error detected for team: {}, channel: {}, providing detailed error message", 
                        teamName, channelName);
                throw new RuntimeException("Error: Insufficient permissions to send HTML message to channel. " +
                        "The application needs appropriate permissions in Microsoft Graph API. " +
                        "Please contact your administrator to grant these permissions.", serviceException);
            } else {
                // Generic service error
                throw new RuntimeException("Error: Failed to send HTML message to channel - " + 
                        serviceException.getMessage(), serviceException);
            }
        }
    }
}

