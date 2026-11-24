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
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.ChannelMessage;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.GroupChat;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Team;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.stores.Replies;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProvider;
import app.krista.extensions.essentials.collaboration.teams.impl.connectors.GraphServiceClientProviderFactory;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for working with Microsoft Teams chats and messages.
 */
@Service
public class ChatService {

    private final GraphServiceClientProviderFactory providerFactory;
    private final TeamService teamService;
    private final ChannelService channelService;
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    // In-memory storage for chat IDs by user email (key-value pair)
    private final Map<String, String> userChatStorage = new ConcurrentHashMap<>();

    // Add constants for pagination limits
    private static final int MAX_MESSAGES_TO_PROCESS = 2000;
    private static final int MAX_SKIP_COUNT = 1000;
    private static final int DEFAULT_PAGE_SIZE = 50;

    @Inject
    public ChatService(GraphServiceClientProviderFactory providerFactory, TeamService teamService, ChannelService channelService) {
        this.providerFactory = providerFactory;
        this.teamService = teamService;
        this.channelService = channelService;
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
     * Send a message to a specific channel within a team.
     *
     * @param teamName    The name of the team.
     * @param channelName The name of the channel.
     * @param message     The message content.
     * @return True if the message was sent successfully, false otherwise.
     */
    public boolean sendMessageToChannel(String teamName, String channelName, String message) {
        return sendMessageToChannel(teamName, channelName, message, false);
    }

    /**
     * Send a message to a specific channel within a team with HTML support.
     *
     * @param teamName    The name of the team.
     * @param channelName The name of the channel.
     * @param message     The message content.
     * @param isHtml      Whether the message content is HTML.
     * @return True if the message was sent successfully, false otherwise.
     */
    public boolean sendMessageToChannel(String teamName, String channelName, String message, boolean isHtml) {

        if (teamName == null || teamName.isBlank() || channelName == null || channelName.isBlank() || message == null || message.isBlank()) {
            logger.error("Invalid input parameters. Returning false.");
            return false;
        }

        // Get the joined teams (prefer per-user credentials so we find the correct user's team)
        List<Team> joinedTeams = teamService.getJoinedTeamsByNamePreferringPerUser(teamName);

        // Find the current team by name
        Team currentTeam = joinedTeams.stream()
                .findFirst()
                .orElse(null);

        if (currentTeam == null) {
            logger.error("Team not found. Returning false.");
            return false;
        }

        // Get the channels of the current team
        List<Channel> channelsOfTeam = channelService.getChannelsOfTeam(currentTeam.iD, channelName);

        // Find the current channel by name
        Channel currentChannel = channelsOfTeam.stream()
                .findFirst()
                .orElse(null);

        if (currentChannel == null) {
            logger.error("Channel not found. Returning false.");
            return false;
        }

        // Send the message to the channel (prefer per-user credentials)
        GraphServiceClient<Request> graphServiceClientForUser = initializeGroupChatClient();
        ChatMessage chatMessage = new ChatMessage();
        ItemBody body = new ItemBody();
        body.content = message;
        if (isHtml) {
            body.contentType = BodyType.HTML;
        }
        chatMessage.body = body;

        graphServiceClientForUser.teams(currentTeam.iD)
                .channels(currentChannel.iD)
                .messages()
                .buildRequest()
                .post(chatMessage);

        logger.info("Message sent successfully.");
        return true;
    }

    /**
     * Send a message to a chat group.
     *
     * @param groupName The name of the chat group.
     * @param message   The message content.
     * @return True if the message was sent successfully, false otherwise.
     * @throws IllegalStateException If the provided group name is invalid or if there are multiple groups with the same name.
     */
    public boolean sendMessageToChatGroup(String groupName, String message, boolean isHtml) {

        if (groupName == null || groupName.isBlank() || message == null || message.isBlank()) {
            logger.error("Invalid input parameters for sendMessageToChatGroup. Returning false.");
            return false;
        }

        GraphServiceClient<Request> graphServiceClientForUser = getUserClient();
        // Build the filter query with case-insensitive comparison
        String filterQuery = String.format("chatType eq '%s' and tolower(topic) eq tolower('%s')", "group", groupName);
        // select variables
        String selectFields = "id,topic,chatType"; // Replace with the fields you want to select

        ChatCollectionPage chats = graphServiceClientForUser.me().chats()
                .buildRequest()
                .select(selectFields)
                .filter(filterQuery)
                .get();

        List<Chat> allChats = new ArrayList<>();

        // Iterate over all pages of results
        while (chats != null) {
            // Add all chat to the ArrayList
            allChats.addAll(chats.getCurrentPage());

            // Check if there are more pages to retrieve
            ChatCollectionRequestBuilder nextPage = chats.getNextPage();
            if (nextPage != null) {
                chats = nextPage.buildRequest().get();
            } else {
                break; // No more pages
            }
        }

        if (allChats.isEmpty()) {
            logger.error("No chat group found with the provided name. Returning false.");
            return false;
        }

        if (allChats.size() > 1) {
            logger.error("Multiple chat groups found with the same name. Returning false.");
            return false;
        }

        logger.info("Sending a message to the chat group.");
        boolean result = sendMessageToChat(allChats.get(0).id, message, isHtml);

        return result;
    }

    /**
     * Send a message directly to a user with detailed response information.
     * This method implements the Microsoft Graph API approach for creating 1:1 chats and sending messages.
     *
     * @param userEmail       The email address of the user to send the message to.
     * @param messageContent  The message content.
     * @param reuseChatId     Whether to reuse existing chat if found.
     * @param logConversation Whether to store chat ID in memory for reuse.
     * @return Map containing Status Code, Message Id, Chat Id, and Response Message.
     */
    public Map<String, String> sendMessageToUser(String userEmail, String messageContent,
                                                 boolean reuseChatId, boolean logConversation) {
        return sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, false);
    }

    /**
     * Send a message directly to a user with detailed response information and HTML support.
     * This method implements the Microsoft Graph API approach for creating 1:1 chats and sending messages.
     *
     * @param userEmail       The email address of the user to send the message to.
     * @param messageContent  The message content.
     * @param reuseChatId     Whether to reuse existing chat if found.
     * @param logConversation Whether to store chat ID in memory for reuse.
     * @param isHtml          Whether the message content is HTML.
     * @return Map containing Status Code, Message Id, Chat Id, and Response Message.
     */
    public Map<String, String> sendMessageToUser(String userEmail, String messageContent,
                                                 boolean reuseChatId, boolean logConversation, boolean isHtml) {
        logger.info("ChatService.sendMessageToUserWithDetails() called with userEmail: {}, messageContent length: {}, reuseChatId: {}, logConversation: {}",
                userEmail, messageContent != null ? messageContent.length() : 0, reuseChatId, logConversation);

        Map<String, String> result = new HashMap<>();
        GraphServiceClient<Request> graphServiceClient = getUserClient();

        // Step 1: Get or create chat ID
        String chatId = null;
        if (reuseChatId) {
            // First check if we have a stored chat ID for this user
            chatId = getStoredChatIdForUser(userEmail);
            if (chatId != null) {
                logger.debug("Found stored chat ID for user: {} - chatId: {}", userEmail, chatId);
            } else {
                // If no stored chat ID, search for existing direct chat
                chatId = findExistingDirectChat(graphServiceClient, userEmail);
                logger.debug("Existing chat search result for {}: {}", userEmail, chatId != null ? chatId : "not found");
            }
        }

        if (chatId == null) {
            // Create new 1:1 chat
            logger.info("Creating new 1:1 chat with user: {}", userEmail);
            chatId = createOneOnOneChat(graphServiceClient, userEmail);
        }

        // Step 2: Send message to the chat using the enhanced approach
        Map<String, String> messageResult = sendMessageToChatWithDetails(graphServiceClient, chatId, messageContent, isHtml);

        if ("200".equals(messageResult.get("Status Code")) || "201".equals(messageResult.get("Status Code"))) {
            result.put("Status Code", messageResult.get("Status Code"));
            result.put("Message Id", messageResult.get("Message Id"));
            result.put("Chat Id", chatId);

            // Create appropriate success message based on message type
            String messageType = isHtml ? "HTML message" : "Message";
            result.put("Response Message", messageType + " sent successfully to user " + userEmail);

            // Log conversation if requested
            if (logConversation) {
                storeChatIdForUser(userEmail, chatId);
                logger.debug("Conversation logging completed for user: {}, chatId: {}", userEmail, chatId);
            }

            logger.info("Successfully sent {} to user: {}, messageId: {}, chatId: {}",
                    isHtml ? "HTML message" : "message", userEmail, messageResult.get("Message Id"), chatId);
        } else {
            result.put("Status Code", messageResult.get("Status Code"));
            result.put("Message Id", "");
            result.put("Chat Id", chatId);

            // Create appropriate error message based on message type
            String messageType = isHtml ? "HTML message" : "Message";
            result.put("Response Message", "Error sending " + messageType.toLowerCase() + " to user " + userEmail + ": " + messageResult.get("Response Message"));
        }

        return result;
    }

    /**
     * Send a message to a specific chat.
     *
     * @param chatId  The ID of the chat.
     * @param message The message content.
     * @return True if the message was sent successfully, false otherwise.
     */
    boolean sendMessageToChat(String chatId, String message, boolean isHtml) {

        if (chatId == null || chatId.isBlank()) {
            logger.error("Invalid chat ID. Returning false.");
            return false;
        }
        ChatMessage chatMessage = new ChatMessage();
        ItemBody body = new ItemBody();
        body.content = message;
        if (isHtml) {
            body.contentType = BodyType.HTML;
        }
        chatMessage.body = body;

        GraphServiceClient<Request> graphServiceClientForUser = getUserClient();

        graphServiceClientForUser.chats(chatId).messages()
                .buildRequest()
                .post(chatMessage);

        logger.info("Message sent successfully for sendMessageToChat.");
        return true;
    }

    /**
     * Create a new 1:1 chat with a specific user using Microsoft Graph API.
     * Based on the reference implementation provided with comprehensive logging.
     *
     * @param graphClient The Graph service client.
     * @param userEmail   The email address of the user.
     * @return The chat ID if created successfully, null otherwise.
     */
    private String createOneOnOneChat(GraphServiceClient<Request> graphClient, String userEmail) {
        logger.info("ChatService.createOneOnOneChat() called for user: {}", userEmail);
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Starting 1:1 chat creation process with user: {}", userEmail);

            // Step 1: Get the recipient user ID by email
            String recipientUserId = getUserIdByEmail(graphClient, userEmail);
            logger.debug("Step 1 SUCCESS: Found recipient user ID: {} for email: {}", recipientUserId, userEmail);

            // Step 2: Get current user ID (sender)
            String senderUserId = getCurrentUserId(graphClient);
            logger.debug("Step 2 SUCCESS: Found sender user ID: {}", senderUserId);

            // Step 3: Create sender member
            AadUserConversationMember senderMember = new AadUserConversationMember();
            senderMember.oDataType = "#microsoft.graph.aadUserConversationMember";
            senderMember.roles = Arrays.asList("owner");
            String senderBindUrl = "https://graph.microsoft.com/v1.0/users('" + senderUserId + "')";
            senderMember.additionalDataManager().put("user@odata.bind",
                    new com.google.gson.JsonPrimitive(senderBindUrl));
            logger.debug("Step 3 SUCCESS: Sender member created with bind URL: {}", senderBindUrl);

            // Step 4: Create recipient member
            logger.debug("Step 4: Creating recipient member with user ID: {}", recipientUserId);
            AadUserConversationMember recipientMember = new AadUserConversationMember();
            recipientMember.oDataType = "#microsoft.graph.aadUserConversationMember";
            recipientMember.roles = Arrays.asList("owner");
            String recipientBindUrl = "https://graph.microsoft.com/v1.0/users('" + recipientUserId + "')";
            recipientMember.additionalDataManager().put("user@odata.bind",
                    new com.google.gson.JsonPrimitive(recipientBindUrl));
            logger.debug("Step 4 SUCCESS: Recipient member created with bind URL: {}", recipientBindUrl);

            // Step 5: Create chat object with members
            logger.debug("Step 5: Creating chat object with chatType: ONE_ON_ONE");
            Chat chat = new Chat();
            chat.chatType = ChatType.ONE_ON_ONE;
            logger.debug("Step 5: Chat object created with chatType: {}", chat.chatType);

            // Add members to chat using LinkedList as per reference implementation
            logger.debug("Step 5: Adding members to chat (sender + recipient)");
            List<ConversationMember> members = new LinkedList<>();
            members.add(senderMember);
            members.add(recipientMember);
            logger.debug("Step 5: Added {} members to chat", members.size());

            // Set members using collection page
            ConversationMemberCollectionPage membersPage = new ConversationMemberCollectionPage(members, null);
            chat.members = membersPage;
            logger.debug("Step 5 SUCCESS: Chat object configured with members collection");

            // Step 6: Create the chat using POST request to Microsoft Graph API
            Chat createdChat = graphClient.chats()
                    .buildRequest()
                    .post(chat);

            if (createdChat != null && createdChat.id != null) {
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Step 6 SUCCESS: 1:1 chat created successfully with user: {}, chatId: {}, duration: {}ms",
                        userEmail, createdChat.id, duration);
                logger.debug("Chat creation completed - chatId: {}, chatType: {}", createdChat.id, createdChat.chatType);
                return createdChat.id;
            } else {
                logger.error("Step 6 FAILED: Chat creation returned null or missing chat ID for user: {}", userEmail);
                return null;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("ERROR: Exception occurred while creating 1:1 chat with user: {}, duration: {}ms",
                    userEmail, duration, e);
            return null;
        }
    }

    /**
     * Send a message to a specific chat and return detailed response information.
     * Based on the reference implementation provided with comprehensive logging.
     *
     * @param graphClient    The Graph service client.
     * @param chatId         The ID of the chat.
     * @param messageContent The message content.
     * @return Map containing Status Code, Message Id, and Response Message.
     */
    private Map<String, String> sendMessageToChatWithDetails(GraphServiceClient<Request> graphClient, String chatId, String messageContent) {
        return sendMessageToChatWithDetails(graphClient, chatId, messageContent, false);
    }

    /**
     * Send a message to a specific chat and return detailed response information with HTML support.
     * Based on the reference implementation provided with comprehensive logging.
     *
     * @param graphClient    The Graph service client.
     * @param chatId         The ID of the chat.
     * @param messageContent The message content.
     * @param isHtml         Whether the message content is HTML.
     * @return Map containing Status Code, Message Id, and Response Message.
     */
    private Map<String, String> sendMessageToChatWithDetails(GraphServiceClient<Request> graphClient, String chatId, String messageContent, boolean isHtml) {
        logger.info("ChatService.sendMessageToChatWithDetails() called for chatId: {}, content length: {}, isHtml: {}",
                chatId, messageContent != null ? messageContent.length() : 0, isHtml);
        long startTime = System.currentTimeMillis();
        Map<String, String> result = new HashMap<>();

        try {
            logger.debug("Starting message sending process to chatId: {}", chatId);

            // Step 1: Validate chat ID
            logger.debug("Step 1: Validating chat ID");
            if (chatId == null || chatId.isBlank()) {
                logger.error("Step 1 FAILED: Invalid chat ID for sendMessageToChatWithDetails - chatId is null or blank");
                result.put("Status Code", "400");
                result.put("Message Id", "");
                result.put("Response Message", "Invalid chat ID provided");
                return result;
            }
            logger.debug("Step 1 SUCCESS: Chat ID validation passed - chatId: {}", chatId);

            // Step 2: Validate message content
            logger.debug("Step 2: Validating message content");
            if (messageContent == null || messageContent.isBlank()) {
                logger.error("Step 2 FAILED: Invalid message content - content is null or blank");
                result.put("Status Code", "400");
                result.put("Message Id", "");
                result.put("Response Message", "Invalid message content provided");
                return result;
            }
            logger.debug("Step 2 SUCCESS: Message content validation passed - length: {}", messageContent.length());

            // Step 3: Create chat message following the reference implementation
            String contentType = isHtml ? "HTML" : "TEXT";
            logger.debug("Step 3: Creating ChatMessage object with content type: {}", contentType);
            ChatMessage chatMessage = new ChatMessage();
            ItemBody body = new ItemBody();
            body.contentType = isHtml ? BodyType.HTML : BodyType.TEXT;
            body.content = messageContent;
            chatMessage.body = body;
            logger.debug("Step 3 SUCCESS: ChatMessage created - contentType: {}, content length: {}",
                    body.contentType, messageContent.length());

            // Step 4: Send message to the chat using Microsoft Graph API
            logger.debug("Step 4: Sending message to chat via Graph API");

            ChatMessage sentMessage = graphClient.chats(chatId)
                    .messages()
                    .buildRequest()
                    .post(chatMessage);

            if (sentMessage != null && sentMessage.id != null) {
                result.put("Status Code", "201");
                result.put("Message Id", sentMessage.id);
                result.put("Response Message", "Message sent successfully");
                return result;
            } else {
                logger.error("Step 4 FAILED: Message sending returned null or missing message ID for chatId: {}", chatId);
                result.put("Status Code", "500");
                result.put("Message Id", "");
                result.put("Response Message", "Failed to send message - no message ID returned");
                return result;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("ERROR: Exception occurred while sending message to chatId: {}, duration: {}ms",
                    chatId, duration, e);
            result.put("Status Code", "500");
            result.put("Message Id", "");
            result.put("Response Message", "Error sending message: " + e.getMessage());
            return result;
        }
    }

    /**
     * Find existing direct (1:1) chat with a specific user.
     *
     * @param graphClient The Graph service client.
     * @param userEmail   The email address of the user.
     * @return The chat ID if found, null otherwise.
     */
    private String findExistingDirectChat(GraphServiceClient<Request> graphClient, String userEmail) {
        try {
            logger.debug("Searching for existing direct chat with user: {}", userEmail);

            // Get user ID first
            String userId = getUserIdByEmail(graphClient, userEmail);
            if (userId == null) {
                logger.warn("Could not find user ID for email: {}", userEmail);
                return null;
            }

            // Get all chats of type 'oneOnOne'
            ChatCollectionPage chats = graphClient.me().chats()
                    .buildRequest()
                    .filter("chatType eq 'oneOnOne'")
                    .select("id,chatType")
                    .get();

            List<Chat> allChats = new ArrayList<>();
            while (chats != null) {
                allChats.addAll(chats.getCurrentPage());
                if (chats.getNextPage() == null) {
                    break;
                }
                chats = chats.getNextPage().buildRequest().get();
            }

            logger.debug("Found {} direct chats to search through", allChats.size());

            // For each chat, check if it contains the target user
            for (Chat chat : allChats) {
                try {
                    // Get members of this chat
                    ConversationMemberCollectionPage members = graphClient.chats(chat.id).members()
                            .buildRequest()
                            .get();

                    if (members != null) {
                        for (ConversationMember member : members.getCurrentPage()) {
                            if (member instanceof AadUserConversationMember) {
                                AadUserConversationMember userMember = (AadUserConversationMember) member;
                                if (userId.equals(userMember.userId)) {
                                    logger.debug("Found existing direct chat with user: {}, chatId: {}", userEmail, chat.id);
                                    return chat.id;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Error checking members for chat {}: {}", chat.id, e.getMessage());
                    // Continue to next chat
                }
            }

            logger.debug("No existing direct chat found with user: {}", userEmail);
            return null;
        } catch (Exception e) {
            logger.error("Error searching for existing direct chat with user: {}", userEmail, e);
            return null;
        }
    }

    /**
     * Get the current authenticated user's ID.
     *
     * @param graphClient The Graph service client.
     * @return The current user's ID if found, null otherwise.
     */
    private String getCurrentUserId(GraphServiceClient<Request> graphClient) {
        logger.debug("ChatService.getCurrentUserId() called");
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Getting current authenticated user ID via Graph API");
            logger.debug("API Call - GET /me with select: id");

            User currentUser = graphClient.me()
                    .buildRequest()
                    .select("id")
                    .get();

            if (currentUser != null && currentUser.id != null) {
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("SUCCESS: Found current user ID: {}, duration: {}ms", currentUser.id, duration);
                return currentUser.id;
            } else {
                logger.error("FAILED: Could not get current user ID - response was null or missing ID");
                logger.debug("Current user response: user={}, userId={}",
                        currentUser != null ? "not null" : "null",
                        currentUser != null ? currentUser.id : "null");
                return null;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("ERROR: Exception occurred while getting current user ID, duration: {}ms", duration, e);
            logger.debug("Exception details: type={}, message={}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Get user ID by email address.
     *
     * @param graphClient The Graph service client.
     * @param userEmail   The email address.
     * @return The user ID if found, null otherwise.
     * @throws RuntimeException if the error is due to missing permissions
     */
    private String getUserIdByEmail(GraphServiceClient<Request> graphClient, String userEmail) {
        logger.debug("ChatService.getUserIdByEmail() called for email: {}", userEmail);
        long startTime = System.currentTimeMillis();

        try {
            logger.debug("Looking up user ID for email: {} via Graph API", userEmail);

            // Build filter query for both mail and userPrincipalName
            String filterQuery = "mail eq '" + userEmail + "' or userPrincipalName eq '" + userEmail + "'";
            logger.debug("API Call - GET /users with filter: {}, select: id,mail,userPrincipalName", filterQuery);

            // Use the users endpoint to find the user by email
            UserCollectionPage users = graphClient.users()
                    .buildRequest()
                    .filter(filterQuery)
                    .select("id,mail,userPrincipalName")
                    .get();

            if (users != null && !users.getCurrentPage().isEmpty()) {
                User foundUser = users.getCurrentPage().get(0);
                String userId = foundUser.id;
                long duration = System.currentTimeMillis() - startTime;

                logger.debug("SUCCESS: Found user ID: {} for email: {}, duration: {}ms", userId, userEmail, duration);
                logger.debug("User details - id: {}, mail: {}, userPrincipalName: {}",
                        foundUser.id, foundUser.mail, foundUser.userPrincipalName);
                return userId;
            } else {
                long duration = System.currentTimeMillis() - startTime;
                logger.warn("FAILED: No user found for email: {}, duration: {}ms", userEmail, duration);
                logger.debug("User search returned: users={}, currentPage size={}",
                        users != null ? "not null" : "null",
                        users != null ? users.getCurrentPage().size() : 0);
                return null;
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("ERROR: Exception occurred while looking up user ID for email: {}, duration: {}ms",
                    userEmail, duration, e);
            logger.debug("Exception details: type={}, message={}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Validate input parameters for channel operations.
     *
     * @param channelID The channel ID to validate.
     * @param teamID    The team ID to validate.
     * @throws IllegalArgumentException If any input is invalid.
     */
    private void validateInputs(String channelID, String teamID) {
        if (channelID == null || channelID.isBlank()) {
            throw new IllegalArgumentException("Channel ID cannot be null or empty");
        }
        if (teamID == null || teamID.isBlank()) {
            throw new IllegalArgumentException("Team ID cannot be null or empty");
        }
    }

    private GraphServiceClient<Request> initializeGraphClient() {
        logger.info("Initializing GraphServiceClient...");
        GraphServiceClient<Request> graphServiceClientForUser = getUserClient();
        if (graphServiceClientForUser == null) {
            logger.error("GraphServiceClient initialization failed.");
            throw new IllegalStateException("GraphServiceClient is null. Initialization failed.");
        }
        logger.info("GraphServiceClient initialized successfully.");
        return graphServiceClientForUser;
    }

    /**
     * Initializes a Graph client for group chat operations, preferring the current user's
     * per-user credentials (wsContact) over the operator account. Falls back to the operator
     * if no per-user credentials are stored. This is needed because GET /chats/{id}/messages
     * requires the caller to be a member of the chat — the operator service account is not.
     */
    private GraphServiceClient<Request> initializeGroupChatClient() {
        logger.info("Initializing GraphServiceClient for group chat (preferring per-user credentials)...");
        GraphServiceClient<Request> client = providerFactory.getPreferringPerUserGraphClient();
        logger.info("GraphServiceClient initialized successfully (group chat path).");
        return client;
    }

    private ChannelMessage processMessage(ChatMessage message) {
        logger.debug("Processing message with ID: {}", message.id);
        ChannelMessage channelMessage = new ChannelMessage();

        // Set basic message details
        channelMessage.senderName = (message.from != null && message.from.user != null)
                ? message.from.user.displayName : "Unknown Sender";
        channelMessage.subject = message.subject;
        channelMessage.message = message.body != null ? sanitizeString(message.body.content) : "No content";
        channelMessage.messageId = message.id;

        // Fix date conversion
        if (message.createdDateTime != null) {
            try {
                // Convert to milliseconds since epoch
                channelMessage.createdDateTime = message.createdDateTime.toInstant().toEpochMilli();
                logger.debug("Converted date: {}", channelMessage.createdDateTime);
            } catch (Exception e) {
                logger.warn("Error converting date: {}", e.getMessage());
                channelMessage.createdDateTime = null;
            }
        } else {
            channelMessage.createdDateTime = null;
        }

        // Add attachments as a comma-separated string
        if (message.attachments != null && !message.attachments.isEmpty()) {
            channelMessage.attachmentLinks = message.attachments.stream()
                    .map(attachment -> attachment.contentUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        } else {
            channelMessage.attachmentLinks = "";
        }

        return channelMessage;
    }

    private String sanitizeString(String input) {
        if (input == null) {
            return "";
        }
        // Basic HTML tag removal (not full sanitizer)
        return input.replaceAll("<[^>]*>", "").trim();
    }


    /**
     * Get channel messages with date range filtering and a limit on the number of messages.
     *
     * @param channelID The ID of the channel.
     * @param teamID    The ID of the team.
     * @param fromDate  The start date from when to retrieve messages.
     * @param toDate    The end date to when to retrieve messages (optional).
     * @param limit     Maximum number of messages to return.
     * @return A list of channel messages within the specified date range, limited to the specified number.
     */
    public List<ChannelMessage> getChannelMessagesWithDateRangeLimit(String channelID, String teamID, Long fromDate, Long toDate, int limit) {
        logger.info("getChannelMessagesWithDateRangeLimit() invoked with channelID: {}, teamID: {}, fromDate: {}, toDate: {}, limit: {}",
                channelID, teamID, fromDate, toDate, limit);
        // If toDate is null, set it to current date for better user experience
        Long effectiveToDate = toDate != null ? toDate : System.currentTimeMillis();
        if (toDate == null) {
            logger.info("No toDate provided, using current date: {}", effectiveToDate);
        }

        // Validate and initialize
        validateInputs(channelID, teamID);
        int actualLimit = validateAndAdjustLimit(limit);
        GraphServiceClient<Request> graphClient = initializeGraphClient();

        // Setup pagination parameters
        PaginationContext paginationContext = initializePaginationContext(actualLimit);

        // Fetch and process messages
        List<ChannelMessage> matchingMessages = fetchAndFilterMessages(
                channelID, teamID, fromDate, effectiveToDate,
                graphClient, paginationContext, actualLimit);

        logger.info("Total messages processed: {}", paginationContext.totalProcessed);
        logger.info("Total matching messages found: {}", matchingMessages.size());
        return matchingMessages;
    }

    /**
     * Validates and adjusts the limit parameter.
     *
     * @param limit The requested limit
     * @return The adjusted limit
     */
    private int validateAndAdjustLimit(int limit) {
        if (limit <= 0) {
            logger.info("Invalid limit ({}), using default page size: {}", limit, DEFAULT_PAGE_SIZE);
            return DEFAULT_PAGE_SIZE;
        }
        return limit;
    }

    /**
     * Initializes pagination context with default values.
     *
     * @param limit The maximum number of messages to return
     * @return A new PaginationContext object
     */
    private PaginationContext initializePaginationContext(int limit) {
        PaginationContext context = new PaginationContext();
        context.skipCount = null;
        context.hasMoreMessages = true;
        context.maxSkipCount = MAX_SKIP_COUNT;
        context.pageSize = DEFAULT_PAGE_SIZE;
        context.totalProcessed = 0;
        context.maxToProcess = MAX_MESSAGES_TO_PROCESS;
        context.processedIds = new HashSet<>();
        return context;
    }

    /**
     * Fetches and filters messages based on date range.
     *
     * @param channelID   The channel ID
     * @param teamID      The team ID
     * @param fromDate    The start date
     * @param toDate      The end date
     * @param graphClient The Graph client
     * @param context     The pagination context
     * @param limit       The maximum number of messages to return
     * @return A list of matching messages
     */
    private List<ChannelMessage> fetchAndFilterMessages(
            String channelID, String teamID, Long fromDate, Long toDate,
            GraphServiceClient<Request> graphClient, PaginationContext context, int limit) {

        List<ChannelMessage> matchingMessages = new ArrayList<>();

        while (shouldContinueFetching(context, matchingMessages.size(), limit)) {
            try {
                if (isSkipCountExcessive(context)) {
                    break;
                }

                // Fetch a page of messages
                ChatMessageCollectionPage messagesPage = fetchMessagesPage(
                        channelID, teamID, graphClient, context.skipCount, context.pageSize);
                List<ChatMessage> currentPage = messagesPage.getCurrentPage();

                if (currentPage.isEmpty()) {
                    logger.info("Received empty page. Stopping pagination.");
                    context.hasMoreMessages = false;
                    continue;
                }

                logger.info("Received page with {} messages", currentPage.size());

                // Process the current page
                processCurrentPage(currentPage, matchingMessages, context, fromDate, toDate, limit);

                // Check if we've reached our limits
                if (haveReachedProcessingLimits(matchingMessages.size(), context.totalProcessed, limit)) {
                    logger.info("Reached processing limits. Stopping pagination.");
                    context.hasMoreMessages = false;
                    continue;
                }

                // Update pagination state
                updatePaginationState(messagesPage, currentPage, context);

            } catch (GraphServiceException gse) {
                throw gse;

            } catch (Exception e) {
                logger.error("Error fetching messages: {}", e.getMessage(), e);
                context.hasMoreMessages = false; // Stop on error
            }
        }

        return matchingMessages;
    }

    /**
     * Determines if we should continue fetching more pages.
     */
    private boolean shouldContinueFetching(PaginationContext context, int currentMatchCount, int limit) {
        return context.hasMoreMessages &&
                currentMatchCount < limit &&
                context.totalProcessed < context.maxToProcess;
    }

    /**
     * Checks if the skip count has exceeded the maximum allowed.
     */
    private boolean isSkipCountExcessive(PaginationContext context) {
        if (context.skipCount != null && context.skipCount > context.maxSkipCount) {
            logger.info("Reached maximum skip count ({}). Stopping pagination to avoid timeouts.", context.maxSkipCount);
            return true;
        }
        return false;
    }

    /**
     * Processes the current page of messages, filtering by date range.
     */
    private void processCurrentPage(
            List<ChatMessage> currentPage,
            List<ChannelMessage> matchingMessages,
            PaginationContext context,
            Long fromDate, Long toDate, int limit) {

        for (ChatMessage message : currentPage) {
            context.totalProcessed++;

            // Skip already processed messages
            if (context.processedIds.contains(message.id)) {
                logger.info("Skipping already processed message: {}", message.id);
                continue;
            }

            // Add to processed set
            context.processedIds.add(message.id);

            if (matchingMessages.size() >= limit) {
                break; // We have enough messages
            }

            ChannelMessage channelMessage = processMessage(message);

            // Apply date filter
            if (isMessageInDateRange(channelMessage, fromDate, toDate)) {
                matchingMessages.add(channelMessage);
                logger.info("Added matching message: {} (total: {})", message.id, matchingMessages.size());
            }
        }
    }

    /**
     * Checks if a message falls within the specified date range.
     */
    private boolean isMessageInDateRange(ChannelMessage message, Long fromDate, Long toDate) {
        if (message.createdDateTime == null) {
            return false;
        }

        boolean afterFromDate = fromDate == null || message.createdDateTime >= fromDate;
        // toDate is now always provided (current date if not specified by user)
        boolean beforeToDate = message.createdDateTime <= toDate;

        return afterFromDate && beforeToDate;
    }

    /**
     * Checks if we've reached the processing limits.
     */
    private boolean haveReachedProcessingLimits(int matchCount, int totalProcessed, int limit) {
        return matchCount >= limit || totalProcessed >= MAX_MESSAGES_TO_PROCESS;
    }

    /**
     * Updates the pagination state based on the current results.
     */
    private void updatePaginationState(
            ChatMessageCollectionPage messagesPage,
            List<ChatMessage> currentPage,
            PaginationContext context) {

        if (messagesPage.getNextPage() == null) {
            logger.info("No more pages available. Stopping pagination.");
            context.hasMoreMessages = false;
        } else {
            // Increment skip count for next page
            context.skipCount = (context.skipCount == null) ? currentPage.size() : context.skipCount + currentPage.size();
            logger.info("Fetching next page with skip count: {}", context.skipCount);
        }
    }

    /**
     * Inner class to hold pagination state.
     */
    private static class PaginationContext {
        Integer skipCount;
        boolean hasMoreMessages;
        int maxSkipCount;
        int pageSize;
        int totalProcessed;
        int maxToProcess;
        Set<String> processedIds;
    }

    private ChatMessageCollectionPage fetchMessagesPage(String channelID, String teamID, GraphServiceClient<Request> graphServiceClientForUser, Integer skip, int pageSize) {
        logger.info("Fetching messages page with skip: {}, pageSize: {}", skip, pageSize);

        // Create a request builder with timeout settings
        var requestBuilder = graphServiceClientForUser
                .teams(teamID)
                .channels(channelID)
                .messages()
                .buildRequest()
                .top(pageSize);  // Always request the specified page size

        // Only apply skip if it's not null
        if (skip != null) {
            requestBuilder = requestBuilder.skip(skip);
            logger.info("Applied skip: {}", skip);
        }

        try {
            logger.info("Fetching messages page with skip: {}, pageSize: {}", skip, pageSize);
            return requestBuilder.get();
        } catch (Exception e) {
            logger.error("Error in fetchMessagesPage: {}", e.getMessage(), e);
            // Return an empty page instead of throwing
            return new ChatMessageCollectionPage(new ArrayList<>(), null);
        }
    }

    /**
     * Get group chats with date range filtering.
     *
     * @param chatID   The ID of the chat.
     * @param fromDate The start date from when to retrieve messages.
     * @param toDate   The end date to when to retrieve messages (optional).
     * @return A list of group chat messages within the specified date range.
     */
    public List<ChannelMessage> getGroupChats(String chatID, Long fromDate, Long toDate) {
        logger.info("getGroupChats() invoked with chatID: {}, fromDate: {}, toDate: {}", chatID, fromDate, toDate);

        try {
            // If toDate is null, set it to current date for better user experience
            Long effectiveToDate = toDate != null ? toDate : System.currentTimeMillis();
            if (toDate == null) {
                logger.info("No toDate provided, using current date: {}", effectiveToDate);
            }

            // Validate inputs
            validateGroupChatInputs(chatID, fromDate);

            // Initialize Graph client — uses per-user credentials if available (needed for chat member access)
            GraphServiceClient<Request> graphServiceClientForUser = initializeGroupChatClient();

            // Initialize pagination context
            GroupChatPaginationContext context = initializeGroupChatPaginationContext();

            // Fetch and filter messages
            List<ChannelMessage> matchingMessages = fetchAndFilterGroupChatMessages(
                    chatID, fromDate, effectiveToDate, graphServiceClientForUser, context);

            logger.info("Total messages processed: {}", context.totalProcessed);
            logger.info("Total matching messages found: {}", matchingMessages.size());
            return matchingMessages;

        } catch (IllegalStateException ise) {
            // Handle authentication errors specifically
            String errorMessage = ise.getMessage();
            if (errorMessage != null && errorMessage.contains("Failed to get account")) {
                logger.error("Authentication error in getGroupChats for chatID: {}", chatID, ise);

                String friendlyMessage = "Authentication Error: Unable to retrieve account information.\n\n" +
                        "This error typically occurs when:\n" +
                        "  • The Teams API extension is not properly authenticated\n" +
                        "  • The authentication token has expired\n" +
                        "  • The authorized account is not available\n\n" +
                        "To resolve this issue:\n" +
                        "1. Go to the Teams API extension configuration in Krista\n" +
                        "2. Re-authenticate the extension with your Microsoft account\n" +
                        "3. Ensure you have the necessary permissions\n" +
                        "4. Retry the operation\n\n" +
                        "If the problem persists, contact your administrator.";

                throw new RuntimeException(friendlyMessage, ise);
            }

            // For other IllegalStateExceptions, re-throw
            throw ise;
        }
    }

    /**
     * Validates the input parameters for group chat retrieval.
     *
     * @param chatID   The chat ID to validate
     * @param fromDate The from date to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateGroupChatInputs(String chatID, Long fromDate) {
        if (chatID == null || chatID.isBlank()) {
            throw new IllegalArgumentException("Chat ID cannot be null or empty");
        }
        if (fromDate == null) {
            throw new IllegalArgumentException("From date cannot be null");
        }
    }

    /**
     * Initializes the pagination context for group chat messages.
     *
     * @return A new GroupChatPaginationContext object
     */
    private GroupChatPaginationContext initializeGroupChatPaginationContext() {
        GroupChatPaginationContext context = new GroupChatPaginationContext();
        context.skipCount = null;
        context.hasMoreMessages = true;
        context.maxSkipCount = MAX_SKIP_COUNT;
        context.pageSize = DEFAULT_PAGE_SIZE;
        context.totalProcessed = 0;
        context.maxToProcess = MAX_MESSAGES_TO_PROCESS;
        context.processedIds = new HashSet<>();
        return context;
    }

    /**
     * Fetches and filters group chat messages based on date range.
     *
     * @param chatID      The chat ID
     * @param fromDate    The start date
     * @param toDate      The end date
     * @param graphClient The Graph client
     * @param context     The pagination context
     * @return A list of matching group chat messages
     */
    private List<ChannelMessage> fetchAndFilterGroupChatMessages(
            String chatID, Long fromDate, Long toDate,
            GraphServiceClient<Request> graphClient, GroupChatPaginationContext context) {

        List<ChannelMessage> matchingMessages = new ArrayList<>();

        while (shouldContinueGroupChatFetching(context)) {
            try {
                if (isGroupChatSkipCountExcessive(context)) {
                    break;
                }

                // Fetch a page of messages
                ChatMessageCollectionPage messagesPage = fetchGroupChatMessagesPage(
                        chatID, graphClient, context.skipCount, context.pageSize);
                List<ChatMessage> currentPage = messagesPage.getCurrentPage();

                if (currentPage.isEmpty()) {
                    logger.info("Received empty page. Stopping pagination.");
                    context.hasMoreMessages = false;
                    continue;
                }

                logger.info("Received page with {} messages", currentPage.size());

                // Process the current page
                processGroupChatCurrentPage(currentPage, matchingMessages, context, fromDate, toDate);

                // Check if we've reached our limits
                if (haveReachedGroupChatProcessingLimits(context)) {
                    logger.info("Reached processing limits. Stopping pagination.");
                    context.hasMoreMessages = false;
                    continue;
                }

                // Update pagination state
                updateGroupChatPaginationState(messagesPage, currentPage, context);

            } catch (GraphServiceException gse) {
                // Handle permission errors specifically
                throw gse;

            } catch (Exception e) {
                logger.error("Error fetching messages for chatID: {}: {}", chatID, e.getMessage(), e);
                context.hasMoreMessages = false; // Stop on error
            }
        }

        return matchingMessages;
    }

    /**
     * Determines if we should continue fetching more pages for group chats.
     */
    private boolean shouldContinueGroupChatFetching(GroupChatPaginationContext context) {
        return context.hasMoreMessages && context.totalProcessed < context.maxToProcess;
    }

    /**
     * Checks if the skip count has exceeded the maximum allowed for group chats.
     */
    private boolean isGroupChatSkipCountExcessive(GroupChatPaginationContext context) {
        if (context.skipCount != null && context.skipCount > context.maxSkipCount) {
            logger.info("Reached maximum skip count ({}). Stopping pagination to avoid timeouts.", context.maxSkipCount);
            return true;
        }
        return false;
    }

    /**
     * Processes the current page of group chat messages, filtering by date range.
     */
    private void processGroupChatCurrentPage(
            List<ChatMessage> currentPage,
            List<ChannelMessage> matchingMessages,
            GroupChatPaginationContext context,
            Long fromDate, Long toDate) {

        for (ChatMessage message : currentPage) {
            context.totalProcessed++;

            // Skip already processed messages
            if (context.processedIds.contains(message.id)) {
                logger.info("Skipping already processed message: {}", message.id);
                continue;
            }

            // Add to processed set
            context.processedIds.add(message.id);

            ChannelMessage channelMessage = processGroupChatMessage(message);

            // Apply date filter
            if (isMessageInDateRange(channelMessage, fromDate, toDate)) {
                matchingMessages.add(channelMessage);
                logger.info("Added matching message: {} (total: {})", message.id, matchingMessages.size());
            }
        }
    }

    /**
     * Checks if a group chat message falls within the specified date range.
     */
    private boolean isGroupChatMessageInDateRange(GroupChat groupChat, Long fromDate, Long toDate) {
        if (groupChat.createdDateTime == null) {
            return false;
        }

        boolean afterFromDate = fromDate == null || groupChat.createdDateTime >= fromDate;
        // toDate is now always provided (current date if not specified by user)
        boolean beforeToDate = groupChat.createdDateTime <= toDate;

        return afterFromDate && beforeToDate;
    }

    /**
     * Checks if we've reached the processing limits for group chats.
     */
    private boolean haveReachedGroupChatProcessingLimits(GroupChatPaginationContext context) {
        return context.totalProcessed >= context.maxToProcess;
    }

    /**
     * Updates the pagination state for group chats based on the current results.
     */
    private void updateGroupChatPaginationState(
            ChatMessageCollectionPage messagesPage,
            List<ChatMessage> currentPage,
            GroupChatPaginationContext context) {

        if (messagesPage.getNextPage() == null) {
            logger.info("No more pages available. Stopping pagination.");
            context.hasMoreMessages = false;
        } else {
            // Increment skip count for next page
            context.skipCount = (context.skipCount == null) ? currentPage.size() : context.skipCount + currentPage.size();
            logger.info("Fetching next page with skip count: {}", context.skipCount);
        }
    }

    /**
     * Inner class to hold pagination state for group chat messages.
     */
    private static class GroupChatPaginationContext {
        Integer skipCount;
        boolean hasMoreMessages;
        int maxSkipCount;
        int pageSize;
        int totalProcessed;
        int maxToProcess;
        Set<String> processedIds;
    }

    private ChatMessageCollectionPage fetchGroupChatMessagesPage(String chatID, GraphServiceClient<Request> graphServiceClientForUser, Integer skip, int pageSize) {
        // Create a request builder
        var requestBuilder = graphServiceClientForUser
                .chats(chatID)
                .messages()
                .buildRequest()
                .top(pageSize);  // Always request the specified page size

        // Only apply skip if it's not null
        if (skip != null) {
            requestBuilder = requestBuilder.skip(skip);
        }

        try {
            logger.info("Fetching group chat messages page with skip: {}, pageSize: {}", skip, pageSize);
            return requestBuilder.get();
        } catch (Exception e) {
            logger.error("Error in fetchGroupChatMessagesPage: {}", e.getMessage(), e);
            // Return an empty page instead of throwing
            return new ChatMessageCollectionPage(new ArrayList<>(), null);
        }
    }

    /**
     * Process a chat message into a ChannelMessage entity.
     * Note: This processes individual group chat messages as ChannelMessage entities.
     *
     * @param message The chat message to process.
     * @return A ChannelMessage entity representing the message.
     */
    private ChannelMessage processGroupChatMessage(ChatMessage message) {
        ChannelMessage channelMessage = new ChannelMessage();

        // Set basic message details
        channelMessage.senderName = (message.from != null && message.from.user != null)
                ? message.from.user.displayName : "Unknown Sender";
        channelMessage.subject = message.subject;
        channelMessage.message = message.body != null ? sanitizeString(message.body.content) : "No content";
        channelMessage.messageId = message.id;

        // Handle attachments
        if (message.attachments != null && !message.attachments.isEmpty()) {
            channelMessage.attachmentLinks = message.attachments.stream()
                    .map(attachment -> attachment.contentUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
        } else {
            channelMessage.attachmentLinks = "";
        }

        // Set creation date
        channelMessage.createdDateTime = message.createdDateTime != null ?
                message.createdDateTime.toInstant().toEpochMilli() : System.currentTimeMillis();

        return channelMessage;
    }

    /**
     * Get details of a group chat by name.
     *
     * @param groupName The name of the group chat.
     * @return A Groups entity with details of the group chat, or null if not found.
     */
    public GroupChat getGroupChatByName(String groupName) {
        logger.info("getGroupChatByName() invoked with groupName: {}", groupName);

        // Validate input
        if (groupName == null || groupName.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }

        GraphServiceClient<Request> graphServiceClientForUser = initializeGraphClient();

        // Build the filter query with case-insensitive comparison
        String filterQuery = String.format("chatType eq '%s' and tolower(topic) eq tolower('%s')", "group", groupName);
        // Select variables
        String selectFields = "id,topic,chatType,createdDateTime"; // Fields to select

        ChatCollectionPage chats = graphServiceClientForUser.me().chats()
                .buildRequest()
                .select(selectFields)
                .filter(filterQuery)
                .get();

        List<Chat> allChats = new ArrayList<>();

        // Iterate over all pages of results
        while (chats != null) {
            // Add all chats to the ArrayList
            allChats.addAll(chats.getCurrentPage());

            // Check if there are more pages to retrieve
            ChatCollectionRequestBuilder nextPage = chats.getNextPage();
            if (nextPage != null) {
                chats = nextPage.buildRequest().get();
            } else {
                break; // No more pages
            }
        }

        if (allChats.isEmpty()) {
            logger.warn("No chat group found with the provided name: {}", groupName);
            return null;
        }

        if (allChats.size() > 1) {
            logger.warn("Multiple chat groups found with the same name: {}", groupName);
            logger.info("Returning the first match.");
        }

        // Convert the first matching Chat to a GroupChat entity
        Chat chat = allChats.get(0);
        GroupChat groupChat = new GroupChat();
        groupChat.groupId = chat.id;
        groupChat.topic = chat.topic;
        groupChat.type = chat.chatType != null ? chat.chatType.toString() : "Unknown";

        // Convert createdDateTime if available
        if (chat.createdDateTime != null) {
            try {
                groupChat.createdDateTime = chat.createdDateTime.toInstant().toEpochMilli();
            } catch (Exception e) {
                logger.warn("Error converting date: {}", e.getMessage());
                groupChat.createdDateTime = null;
            }
        }

        // Retrieve and populate chat members
        GraphServiceClient<Request> graphClient = getUserClient();
        groupChat.members = getChatMembers(graphClient, chat.id);

        // Log the members information
        logger.info("Group chat '{}' has {} members", groupChat.topic,
                groupChat.members != null ? groupChat.members.size() : 0);
        if (groupChat.members != null && !groupChat.members.isEmpty()) {
            logger.info("Members for group chat '{}': {}", groupChat.topic, groupChat.members);
            for (int i = 0; i < groupChat.members.size(); i++) {
                Map<String, Object> member = groupChat.members.get(i);
                logger.info("Member {}: Name='{}', Email='{}'",
                        i + 1, member.get("Name"), member.get("Email"));
            }
        } else {
            logger.warn("No members found for group chat '{}' - this may be due to insufficient permissions",
                    groupChat.topic);
        }

        return groupChat;
    }

    /**
     * Get all group chats that the user has access to.
     *
     * @return A list of Groups entities representing all group chats.
     */
    public List<GroupChat> getAllGroupChats() {
        logger.info("getAllGroupChats() invoked");

        // Create a cache for user details to avoid duplicate lookups
        ConcurrentHashMap<String, Map<String, Object>> userCache = new ConcurrentHashMap<>();

        GraphServiceClient<Request> graphServiceClientForUser = initializeGroupChatClient();

        // Build the filter query to get group chats as well as meeting chats
        String filterQuery = "chatType eq 'group' or chatType eq 'meeting'";
        // Select only the fields we need
        String selectFields = "id,topic,chatType,createdDateTime";

        try {
            ChatCollectionPage chats = graphServiceClientForUser.me().chats()
                    .buildRequest()
                    .select(selectFields)
                    .filter(filterQuery)
                    .get();

            List<GroupChat> basicGroupChats = new ArrayList<>();

            // Process all pages of results to get basic chat info first
            while (chats != null) {
                for (Chat chat : chats.getCurrentPage()) {
                    GroupChat groupChat = convertChatToGroupChatBasic(chat);
                    if (groupChat != null) {
                        basicGroupChats.add(groupChat);
                    }
                }

                // Check if there are more pages
                ChatCollectionRequestBuilder nextPage = chats.getNextPage();
                if (nextPage != null) {
                    chats = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }

            logger.info("Found {} basic group chats, now fetching members in parallel", basicGroupChats.size());

            // Use parallel processing to fetch members for all chats
            int maxThreads = Math.min(8, Runtime.getRuntime().availableProcessors());
            ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

            try {
                // Process all chats in parallel
                List<GroupChat> enrichedGroupChats = basicGroupChats.parallelStream()
                        .map(basicChat -> {
                            try {
                                return enrichGroupChatWithMembers(basicChat, graphServiceClientForUser, userCache);
                            } catch (Exception e) {
                                logger.error("Error enriching chat {}: {}", basicChat.groupId, e.getMessage(), e);
                                return basicChat; // Return basic chat if enrichment fails
                            }
                        })
                        .collect(Collectors.toList());

                logger.info("Successfully enriched {} group chats with member details", enrichedGroupChats.size());
                logger.info("User cache contains {} unique users", userCache.size());

                return enrichedGroupChats;
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving group chats: {}", e.getMessage(), e);
            // Return empty list instead of throwing exception
            return Collections.emptyList();
        }
    }

    public List<GroupChat> getFilteredGroupChats(int limitValue, String topic, String chatType) {
        logger.info("getFilteredGroupChats() invoked with limit={}, topic={}, chatType={}", limitValue, topic, chatType);

        ConcurrentHashMap<String, Map<String, Object>> userCache = new ConcurrentHashMap<>();
        GraphServiceClient<Request> graphServiceClientForUser = initializeGroupChatClient();

        String filterQuery;
        if (chatType != null && !chatType.isBlank()) {
            filterQuery = "chatType eq '" + chatType + "'";
        } else {
            filterQuery = "chatType eq 'group' or chatType eq 'meeting'";
        }
        String selectFields = "id,topic,chatType,createdDateTime";

        try {
            ChatCollectionPage chats = graphServiceClientForUser.me().chats()
                    .buildRequest()
                    .select(selectFields)
                    .filter(filterQuery)
                    .get();

            List<GroupChat> basicGroupChats = new ArrayList<>();

            while (chats != null) {
                for (Chat chat : chats.getCurrentPage()) {
                    GroupChat groupChat = convertChatToGroupChatBasic(chat);
                    if (groupChat != null) {
                        if (topic == null || topic.isBlank() || (groupChat.topic != null && groupChat.topic.toLowerCase().contains(topic.toLowerCase()))) {
                            basicGroupChats.add(groupChat);
                        }
                    }
                    if (limitValue > 0 && basicGroupChats.size() >= limitValue) break;
                }
                if (limitValue > 0 && basicGroupChats.size() >= limitValue) break;

                ChatCollectionRequestBuilder nextPage = chats.getNextPage();
                if (nextPage != null) {
                    chats = nextPage.buildRequest().get();
                } else {
                    break;
                }
            }

            logger.info("Found {} filtered group chats, now fetching members in parallel", basicGroupChats.size());

            int maxThreads = Math.min(8, Runtime.getRuntime().availableProcessors());
            ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
            try {
                List<GroupChat> enrichedGroupChats = basicGroupChats.parallelStream()
                        .map(basicChat -> {
                            try {
                                return enrichGroupChatWithMembers(basicChat, graphServiceClientForUser, userCache);
                            } catch (Exception e) {
                                logger.error("Error enriching chat {}: {}", basicChat.groupId, e.getMessage(), e);
                                return basicChat;
                            }
                        })
                        .collect(Collectors.toList());
                return enrichedGroupChats;
            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            logger.error("Error retrieving filtered group chats: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Enrich a basic group chat with member details.
     *
     * @param basicChat The basic group chat to enrich
     * @param client    The Graph client
     * @param userCache Cache of user details to avoid duplicate lookups
     * @return The enriched group chat
     */
    private GroupChat enrichGroupChatWithMembers(GroupChat basicChat,
                                                 GraphServiceClient<Request> client,
                                                 ConcurrentHashMap<String, Map<String, Object>> userCache) {
        logger.info("Enriching group chat: {} ({})", basicChat.topic, basicChat.groupId);

        try {
            // Get members with caching
            List<Map<String, Object>> members = getChatMembersWithCache(client, basicChat.groupId, userCache);
            basicChat.members = members;

            // Log the members information
            logger.info("Group chat '{}' has {} members", basicChat.topic, members.size());

            return basicChat;
        } catch (Exception e) {
            logger.warn("Failed to enrich chat {}: {}", basicChat.groupId, e.getMessage());
            // Return the basic chat even if member enrichment fails
            basicChat.members = new ArrayList<>();
            return basicChat;
        }
    }

    /**
     * Get chat members with caching to avoid duplicate user lookups.
     *
     * @param client    The Graph client
     * @param chatId    The chat ID
     * @param userCache Cache of user details
     * @return List of member information
     */
    private List<Map<String, Object>> getChatMembersWithCache(
            GraphServiceClient<Request> client,
            String chatId,
            ConcurrentHashMap<String, Map<String, Object>> userCache) {

        List<Map<String, Object>> membersList = new ArrayList<>();

        try {
            logger.info("Retrieving members for chat: {}", chatId);

            ConversationMemberCollectionPage members = client.me()
                    .chats(chatId)
                    .members()
                    .buildRequest()
                    .get();

            while (members != null) {
                for (ConversationMember member : members.getCurrentPage()) {
                    if (member instanceof AadUserConversationMember) {
                        AadUserConversationMember aadMember = (AadUserConversationMember) member;

                        // Use cached user info if available
                        String userId = aadMember.id;
                        Map<String, Object> memberInfo = userCache.computeIfAbsent(userId, id -> {
                            Map<String, Object> info = new HashMap<>();
                            info.put("Name", aadMember.displayName != null ? aadMember.displayName : "Unknown");
                            info.put("Email", aadMember.email != null ? aadMember.email : "");
                            return info;
                        });

                        membersList.add(memberInfo);
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

            return membersList;
        } catch (Exception e) {
            logger.error("Error getting chat members: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Convert a Chat object to a basic GroupChat entity (without members).
     *
     * @param chat The Chat object to convert
     * @return A basic GroupChat entity
     */
    private GroupChat convertChatToGroupChatBasic(Chat chat) {
        if (chat == null) {
            return null;
        }

        GroupChat groupChat = new GroupChat();
        groupChat.groupId = chat.id;
        groupChat.topic = chat.topic;
        groupChat.type = chat.chatType != null ? chat.chatType.toString() : "Unknown";

        // Convert createdDateTime if available
        if (chat.createdDateTime != null) {
            try {
                groupChat.createdDateTime = chat.createdDateTime.toInstant().toEpochMilli();
            } catch (Exception e) {
                logger.warn("Error converting date: {}", e.getMessage());
                groupChat.createdDateTime = null;
            }
        }

        // Don't fetch members for basic conversion
        groupChat.members = new ArrayList<>();

        return groupChat;
    }

    /**
     * Get replies for a specific channel message.
     *
     * @param channelMessageId The ID of the channel message.
     * @param channelID        The ID of the channel.
     * @param teamID           The ID of the team.
     * @return A list of replies for the message.
     */
    public List<Replies> getChannelMessageReplies(String channelMessageId, String channelID, String teamID) {
        logger.info("getChannelMessageReplies() invoked for message ID: {}", channelMessageId);
        GraphServiceClient<Request> graphServiceClientForUser = initializeGraphClient();

        List<Replies> replies = new ArrayList<>();
        String nextLink = null;

        try {
            do {
                logger.debug("Fetching replies page with nextLink: {}", nextLink);
                ChatMessageCollectionPage repliesPage = fetchRepliesPage(channelMessageId, channelID, teamID, graphServiceClientForUser, nextLink);

                if (repliesPage == null || repliesPage.getCurrentPage().isEmpty()) {
                    logger.debug("No more replies found.");
                    break;
                }

                for (ChatMessage reply : repliesPage.getCurrentPage()) {
                    Replies replyEntity = processReply(reply, channelMessageId);
                    replies.add(replyEntity);
                }

                nextLink = getNextLink(repliesPage);

            } while (nextLink != null);

            logger.info("All replies fetched successfully for message ID: {}. Total replies: {}", channelMessageId, replies.size());

        } catch (Exception e) {
            logger.error("Error fetching replies for message ID {}: {}", channelMessageId, e.getMessage(), e);
        }

        return replies;
    }

    /**
     * Fetch a page of replies for a channel message.
     */
    private ChatMessageCollectionPage fetchRepliesPage(String channelMessageId, String channelID, String teamID, GraphServiceClient<Request> graphServiceClientForUser, String nextLink) {
        try {
            if (nextLink == null) {
                logger.debug("Fetching first page of replies for message ID: {}", channelMessageId);
                return graphServiceClientForUser
                        .teams(teamID)
                        .channels(channelID)
                        .messages(channelMessageId)
                        .replies()
                        .buildRequest()
                        .get();
            } else {
                if (nextLink.startsWith("https://graph.microsoft.com/v1.0")) {
                    nextLink = nextLink.replace("https://graph.microsoft.com/v1.0", "");
                }
                logger.debug("Fetching next page of replies from: {}", nextLink);
                return graphServiceClientForUser
                        .customRequest(nextLink, ChatMessageCollectionPage.class)
                        .buildRequest()
                        .get();
            }
        } catch (Exception e) {
            logger.error("Error fetching replies page: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Process a reply message into a Replies entity.
     */
    private Replies processReply(ChatMessage reply, String channelMessageId) {
        logger.debug("Processing reply with ID: {}", reply.id);

        Replies replyEntity = new Replies();

        // Set reply details
        replyEntity.replyId = reply.id;
        replyEntity.senderName = (reply.from != null && reply.from.user != null)
                ? reply.from.user.displayName : "Unknown Sender";
        replyEntity.replyMessage = reply.body != null ? sanitizeString(reply.body.content) : "No content";
        replyEntity.messageId = channelMessageId;
        replyEntity.createdDateTime = reply.createdDateTime != null
                ? reply.createdDateTime.toInstant().toEpochMilli() : null;

        // Handle attachments
        if (reply.attachments != null && !reply.attachments.isEmpty()) {
            String attachments = reply.attachments.stream()
                    .map(attachment -> attachment.contentUrl)
                    .collect(Collectors.joining(","));
            replyEntity.attachmentLinks = attachments;
        } else {
            replyEntity.attachmentLinks = "";
        }

        return replyEntity;
    }

    /**
     * Extract the next link from a ChatMessageCollectionPage for pagination.
     */
    private String getNextLink(ChatMessageCollectionPage page) {
        try {
            if (page != null && page.getNextPage() != null) {
                // Get the next page request URL
                String nextPageUrl = page.getNextPage().getRequestUrl().toString();
                logger.debug("Next link found: {}", nextPageUrl);
                return nextPageUrl;
            }
        } catch (Exception e) {
            logger.error("Error extracting next link: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retrieve chat members from Microsoft Graph API.
     *
     * @param client The Graph API client.
     * @param chatId The ID of the chat.
     * @return A list of maps containing member information (Name and Email).
     */
    private List<Map<String, Object>> getChatMembers(GraphServiceClient<Request> client, String chatId) {
        List<Map<String, Object>> membersList = new ArrayList<>();

        try {
            logger.info("Retrieving members for chat: {}", chatId);
            logger.info("Making API call to: /chats/{}/members", chatId);

            ConversationMemberCollectionPage members = client.me()
                    .chats(chatId)
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

            logger.info("Successfully retrieved {} members for chat: {}", membersList.size(), chatId);

            // Print all members for debugging
            if (!membersList.isEmpty()) {
                logger.info("=== MEMBERS LIST FOR CHAT {} ===", chatId);
                for (int i = 0; i < membersList.size(); i++) {
                    Map<String, Object> member = membersList.get(i);
                    logger.info("Member {}: {}", i + 1, member);
                }
                logger.info("=== END MEMBERS LIST ===");
            } else {
                logger.warn("No members were added to the list for chat: {}", chatId);
            }

        } catch (Exception e) {
            logger.error("Error retrieving chat members for chat: {}. Error: {}", chatId, e.getMessage());
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            // Return empty list on error to avoid breaking the chat retrieval
        }

        logger.info("Returning members list with {} items for chat: {}", membersList.size(), chatId);
        return membersList;
    }

    /**
     * Store chat ID for a user email in memory for future reuse.
     * This implements the logConversation functionality using key-value pairs.
     *
     * @param userEmail The email address of the user (key).
     * @param chatId    The chat ID to store (value).
     */
    private void storeChatIdForUser(String userEmail, String chatId) {
        if (userEmail == null || userEmail.isBlank() || chatId == null || chatId.isBlank()) {
            logger.warn("Cannot store chat ID: invalid userEmail or chatId - userEmail: {}, chatId: {}", userEmail, chatId);
            return;
        }

        try {
            // Store the chat ID using email as key
            String previousChatId = userChatStorage.put(userEmail, chatId);
            logger.debug("Previous chat ID for user: {} - {}", userEmail, previousChatId);

            if (previousChatId != null) {
                logger.info("Updated stored chat ID for user: {} - previous: {}, new: {}", userEmail, previousChatId, chatId);
            } else {
                logger.info("Stored new chat ID for user: {} - chatId: {}", userEmail, chatId);
            }

            logger.info("Current storage size: {} entries", userChatStorage.size());

        } catch (Exception e) {
            logger.error("Error storing chat ID for user: {}, chatId: {}", userEmail, chatId, e);
        }
    }

    /**
     * Retrieve stored chat ID for a user email.
     * This allows reusing existing chat IDs when logConversation was enabled.
     *
     * @param userEmail The email address of the user (key).
     * @return The stored chat ID if found, null otherwise.
     */
    public String getStoredChatIdForUser(String userEmail) {
        try {
            String chatId = userChatStorage.get(userEmail);
            logger.debug("Current storage size: {} entries", userChatStorage.size());

            if (chatId != null) {
                logger.debug("Retrieved stored chat ID for user: {} - chatId: {}", userEmail, chatId);
            } else {
                logger.debug("No stored chat ID found for user: {}", userEmail);
            }

            return chatId;

        } catch (Exception e) {
            logger.error("Error retrieving chat ID for user: {}", userEmail, e);
            return null;
        }
    }

    /**
     * Validates if a user exists in Microsoft Teams by checking if the email
     * corresponds to a valid user in the organization.
     * This method is used by validators to check user existence without creating chats.
     *
     * @param userEmail The email address to validate
     * @return true if user exists and is accessible, false otherwise
     * @throws RuntimeException if the error is due to missing permissions or authentication issues
     */
    public boolean validateUserExists(String userEmail) {
        logger.debug("ChatService.validateUserExists() called for email: {}", userEmail);
        long startTime = System.currentTimeMillis();
        // Use admin client to avoid account ID issues during validation
        GraphServiceClient<Request> graphClient = clientProvider().getGraphServiceClientForAdmin();
        String userId = getUserIdByEmail(graphClient, userEmail);

        long duration = System.currentTimeMillis() - startTime;

        if (userId != null) {
            logger.debug("User validation SUCCESS: User exists for email: {}, userId: {}, duration: {}ms",
                    userEmail, userId, duration);
            return true;
        } else {
            logger.warn("User validation FAILED: No user found for email: {}, duration: {}ms",
                    userEmail, duration);
            return false;
        }
    }

}
