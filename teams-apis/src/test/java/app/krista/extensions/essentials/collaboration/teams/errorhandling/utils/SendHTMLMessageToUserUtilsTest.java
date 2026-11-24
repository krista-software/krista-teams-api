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
 * Comprehensive test suite for SendHTMLMessageToUserUtils.
 * Tests HTML message sending functionality with error handling and permission management.
 */
@DisplayName("SendHTMLMessageToUserUtils Test Suite")
class SendHTMLMessageToUserUtilsTest {

    @Mock
    private ChatService chatService;

    private SendHTMLMessageToUserUtils utils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        utils = new SendHTMLMessageToUserUtils(chatService);
    }

    @Nested
    @DisplayName("Successful HTML Message Sending Tests")
    class SuccessfulHTMLMessageSendingTests {

        @Test
        @DisplayName("Should successfully send simple HTML message")
        void testSendSimpleHTMLMessage() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Hello, this is a test message.</p>";
            boolean reuseChatId = true;
            boolean logConversation = true;

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "HTML message sent successfully");

            when(chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, true))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendHTMLMessageToUserWithDetails(
                    userEmail, messageContent, reuseChatId, logConversation);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
            assertEquals("msg-123", result.get("Message Id"));
            assertEquals("chat-456", result.get("Chat Id"));
            assertEquals("HTML message sent successfully", result.get("Response Message"));

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, reuseChatId, logConversation, true);
        }

        @Test
        @DisplayName("Should successfully send complex HTML message")
        void testSendComplexHTMLMessage() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<div><h2>Title</h2><p>Paragraph with <strong>bold</strong> and <em>italic</em>.</p><ul><li>Item 1</li><li>Item 2</li></ul></div>";
            boolean reuseChatId = true;
            boolean logConversation = true;

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-456");
            expectedResult.put("Chat Id", "chat-789");
            expectedResult.put("Response Message", "Complex HTML message sent successfully");

            when(chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, true))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendHTMLMessageToUserWithDetails(
                    userEmail, messageContent, reuseChatId, logConversation);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
            assertEquals("msg-456", result.get("Message Id"));
            assertEquals("chat-789", result.get("Chat Id"));

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, reuseChatId, logConversation, true);
        }

        @Test
        @DisplayName("Should send HTML message without reusing chat ID")
        void testSendHTMLMessageWithoutReusingChatId() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";
            boolean reuseChatId = false;
            boolean logConversation = false;

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-new");
            expectedResult.put("Chat Id", "chat-new");
            expectedResult.put("Response Message", "New chat created and HTML message sent");

            when(chatService.sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, true))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendHTMLMessageToUserWithDetails(
                    userEmail, messageContent, reuseChatId, logConversation);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
            assertEquals("msg-new", result.get("Message Id"));
            assertEquals("chat-new", result.get("Chat Id"));

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, reuseChatId, logConversation, true);
        }
    }

    @Nested
    @DisplayName("Permission Error Handling Tests")
    class PermissionErrorHandlingTests {

        @Test
        @DisplayName("Should propagate Authorization_RequestDenied error from ChatService")
        void testAuthorizationRequestDeniedError() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenThrow(new RuntimeException("Authorization_RequestDenied: Insufficient privileges"));

            // Act & Assert
            // The utility class now propagates exceptions to be handled at a higher level
            // (in SendHTMLMessageToUserValidationHelper or ChatService)
            assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToUserWithDetails(userEmail, messageContent, true, true);
            });

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, true, true, true);
        }

        @Test
        @DisplayName("Should propagate 403 Forbidden error from ChatService")
        void testForbiddenError() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenThrow(new RuntimeException("403 Forbidden: Access denied"));

            // Act & Assert
            // The utility class now propagates exceptions to be handled at a higher level
            assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToUserWithDetails(userEmail, messageContent, true, true);
            });

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, true, true, true);
        }

        @Test
        @DisplayName("Should propagate Insufficient privileges error from ChatService")
        void testInsufficientPrivilegesError() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenThrow(new RuntimeException("Insufficient privileges to complete the operation"));

            // Act & Assert
            // The utility class now propagates exceptions to be handled at a higher level
            assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToUserWithDetails(userEmail, messageContent, true, true);
            });

            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, true, true, true);
        }
    }

    @Nested
    @DisplayName("Generic Error Handling Tests")
    class GenericErrorHandlingTests {

        @Test
        @DisplayName("Should propagate generic service exception from ChatService")
        void testGenericServiceException() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenThrow(new RuntimeException("Network error occurred"));

            // Act & Assert
            // The utility class now propagates exceptions to be handled at a higher level
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToUserWithDetails(userEmail, messageContent, true, true);
            });

            assertEquals("Network error occurred", exception.getMessage());
            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, true, true, true);
        }

        @Test
        @DisplayName("Should propagate null pointer exception from ChatService")
        void testNullPointerException() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenThrow(new NullPointerException("Null value encountered"));

            // Act & Assert
            // The utility class now propagates exceptions to be handled at a higher level
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                utils.sendHTMLMessageToUserWithDetails(userEmail, messageContent, true, true);
            });

            assertEquals("Null value encountered", exception.getMessage());
            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, true, true, true);
        }

        @Test
        @DisplayName("Should propagate exception with null message from ChatService")
        void testExceptionWithNullMessage() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenThrow(new RuntimeException((String) null));

            // Act & Assert
            // The utility class now propagates exceptions to be handled at a higher level
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToUserWithDetails(userEmail, messageContent, true, true);
            });

            assertNull(exception.getMessage());
            verify(chatService, times(1)).sendMessageToUser(
                    userEmail, messageContent, true, true, true);
        }
    }

    @Nested
    @DisplayName("HTML Content Variation Tests")
    class HTMLContentVariationTests {

        @Test
        @DisplayName("Should handle HTML with links")
        void testHTMLWithLinks() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Visit <a href='https://example.com'>our website</a> for more info.</p>";

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "Success");

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendHTMLMessageToUserWithDetails(
                    userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
        }

        @Test
        @DisplayName("Should handle HTML with images")
        void testHTMLWithImages() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<p>Check out: <img src='https://example.com/image.png' alt='Example' /></p>";

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "Success");

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendHTMLMessageToUserWithDetails(
                    userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
        }

        @Test
        @DisplayName("Should handle HTML with tables")
        void testHTMLWithTables() {
            // Arrange
            String userEmail = "user@example.com";
            String messageContent = "<table><tr><th>Header</th></tr><tr><td>Data</td></tr></table>";

            Map<String, String> expectedResult = new HashMap<>();
            expectedResult.put("Status Code", "200");
            expectedResult.put("Message Id", "msg-123");
            expectedResult.put("Chat Id", "chat-456");
            expectedResult.put("Response Message", "Success");

            when(chatService.sendMessageToUser(anyString(), anyString(), anyBoolean(), anyBoolean(), eq(true)))
                    .thenReturn(expectedResult);

            // Act
            Map<String, String> result = utils.sendHTMLMessageToUserWithDetails(
                    userEmail, messageContent, true, true);

            // Assert
            assertNotNull(result);
            assertEquals("200", result.get("Status Code"));
        }
    }
}

