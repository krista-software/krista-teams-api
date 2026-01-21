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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SendMessageToUserUtils.
 * Tests message sending functionality with error handling and permission management.
 */
@DisplayName("SendMessageToUserUtils Test Suite")
class SendMessageToUserUtilsTest {

    @Mock
    private ChatService chatService;

    private SendMessageToUserUtils utils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        utils = new SendMessageToUserUtils(chatService);
    }

    @Nested
    @DisplayName("Successful Message Sending Tests")
    class SuccessfulMessageSendingTests {

        @Test
        @DisplayName("Should successfully send plain text message")
        void testSendPlainTextMessage() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Hello, this is a test message.";
            boolean reuseChatId = true;
            boolean logConversation = true;

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "Message sent successfully");

            when(chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, false))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(
                    userEmail, messageContent, reuseChatId, logConversation);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
            assertEquals("msg-123", result.get("Message Id"));
            assertEquals("chat-456", result.get("Chat Id"));
            assertEquals("Message sent successfully", result.get("Response Message"));

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, reuseChatId, logConversation, false);
        }

        @Test
        @DisplayName("Should successfully send HTML message")
        void testSendHTMLMessage() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Hello, this is a <strong>test</strong> message.</p>";
            boolean reuseChatId = true;
            boolean logConversation = true;
            boolean isHtml = true;

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "HTML message sent successfully");

            when(chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, isHtml))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(
                    userEmail, messageContent, reuseChatId, logConversation, isHtml);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
            assertEquals("msg-123", result.get("Message Id"));
            assertEquals("chat-456", result.get("Chat Id"));
            assertEquals("HTML message sent successfully", result.get("Response Message"));

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, reuseChatId, logConversation, isHtml);
        }

        @Test
        @DisplayName("Should send message without reusing chat ID")
        void testSendMessageWithoutReusingChatId() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";
            boolean reuseChatId = false;
            boolean logConversation = false;

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-789");
            expectedResult.put("Chat Id", "chat-new");
            expectedResult.put("Response Message", "New chat created and message sent");

            when(chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, false))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(
                    userEmail, messageContent, reuseChatId, logConversation);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
            assertEquals("msg-789", result.get("Message Id"));
            assertEquals("chat-new", result.get("Chat Id"));

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, reuseChatId, logConversation, false);
        }
    }

    @Nested
    @DisplayName("Permission Error Handling Tests")
    class PermissionErrorHandlingTests {

        @Test
        @DisplayName("Should handle Authorization_RequestDenied error")
        void testAuthorizationRequestDeniedError() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenThrow(new RuntimeException("Authorization_RequestDenied: Insufficient privileges"));

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(
                    userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("403", result.get("Status Code"));
            assertEquals("", result.get("Message Id"));
            assertEquals("", result.get("Chat Id"));
            assertTrue(result.get("Response Message").contains("Insufficient permissions"));
            assertTrue(result.get("Response Message").contains("User.Read.All"));
            assertTrue(result.get("Response Message").contains("Chat.Create"));
        }

        @Test
        @DisplayName("Should handle 403 Forbidden error")
        void testForbiddenError() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenThrow(new RuntimeException("403 Forbidden: Access denied"));

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(
                    userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("403", result.get("Status Code"));
            assertTrue(result.get("Response Message").contains("Insufficient permissions"));
        }

        @Test
        @DisplayName("Should handle Insufficient privileges error")
        void testInsufficientPrivilegesError() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenThrow(new RuntimeException("Insufficient privileges to complete the operation"));

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(
                    userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("403", result.get("Status Code"));
            assertTrue(result.get("Response Message").contains("Insufficient permissions"));
            assertTrue(result.get("Response Message").contains("administrator"));
        }
    }

    @Nested
    @DisplayName("Generic Error Handling Tests")
    class GenericErrorHandlingTests {

        @Test
        @DisplayName("Should handle generic service exception")
        void testGenericServiceException() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenThrow(new RuntimeException("Network error occurred"));

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("500", result.get("Status Code"));
            assertEquals("", result.get("Message Id"));
            assertEquals("", result.get("Chat Id"));
            assertTrue(result.get("Response Message").contains("Failed to send message"));
            assertTrue(result.get("Response Message").contains("Network error occurred"));
        }

        @Test
        @DisplayName("Should handle null pointer exception")
        void testNullPointerException() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenThrow(new NullPointerException("Null value encountered"));

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("500", result.get("Status Code"));
            assertTrue(result.get("Response Message").contains("Failed to send message"));
        }

        @Test
        @DisplayName("Should handle exception with null message")
        void testExceptionWithNullMessage() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "Test message";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenThrow(new RuntimeException((String) null));

            // Act
            Map<String, String> result = utils.sendMessageToUserWithDetails(userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("500", result.get("Status Code"));
            assertTrue(result.get("Response Message").contains("Failed to send message"));
        }
    }

    @Nested
    @DisplayName("Parameter Variation Tests")
    class ParameterVariationTests {

        @Test
        @DisplayName("Should handle different email formats")
        void testDifferentEmailFormats() {
            // Arrange
            String[] emails = {
                    "user@example.com",
                    "test.user@example.com",
                    "user+tag@example.co.uk"
            };

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "Success");

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedResult);

            // Act & Assert
            for (String email : emails) {
                Map<String, String> result = utils.sendMessageToUserWithDetails(
                        email, "Test message", true, true);

                assertNotNull(result);
                assertEquals("200", result.get("Status Code"));
            }

            verify(chatService, times(emails.length)).sendMessageToUser(
                    anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());
        }

        @Test
        @DisplayName("Should handle different message content types")
        void testDifferentMessageContentTypes() {
            // Arrange
            String[] messages = {
                    "Simple text",
                    "Message with numbers: 123",
                    "Message with special chars: @#$%",
                    "Multi\nline\nmessage"
            };

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "Success");

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean()))
                    .thenReturn(expectedResult);

            // Act & Assert
            for (String message : messages) {
                Map<String, String> result = utils.sendMessageToUserWithDetails(
                        "user@example.com", message, true, true);

                assertNotNull(result);
                assertEquals("200", result.get("Status Code"));
            }

            verify(chatService, times(messages.length)).sendMessageToUser(
                    anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());
        }
    }
}

