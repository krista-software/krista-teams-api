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
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for Send HTML Message to User functionality.
 * Contains all the logic for sending HTML messages to users with comprehensive error handling.
 */
@Singleton
@Service
public class SendHTMLMessageToUserUtils {

    private static final Logger logger = LoggerFactory.getLogger(SendHTMLMessageToUserUtils.class);
    private final ChatService chatService;

    @Inject
    public SendHTMLMessageToUserUtils(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Send HTML message to user with comprehensive error handling and logging.
     * This method contains all the logic for sending HTML messages to users, including
     * permission error handling and fallback mechanisms.
     *
     * @param userEmail       The email address of the user to send the message to.
     * @param messageContent  The HTML message content.
     * @param reuseChatId     Whether to reuse existing chat if found.
     * @param logConversation Whether to store chat ID in memory for reuse.
     * @return Map containing Status Code, Message Id, Chat Id, and Response Message.
     */
    public Map<String, String> sendHTMLMessageToUserWithDetails(String userEmail, String messageContent,
                                                                boolean reuseChatId, boolean logConversation) {
        Map<String, String> result;

            // Call ChatService with isHtml=true
            result = chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, true);

            // Defensive check: ensure service didn't return null
            if (result == null) {
                logger.warn("ChatService returned null result for user: {}", userEmail);
                result = new HashMap<>();
                result.put("Status Code", result.get("Status code"));
                result.put("Message Id", "");
                result.put("Chat Id", "");
                result.put("Response Message", "Error: Service returned null response");
            }

            return result;

    }
}

