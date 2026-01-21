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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for SendHTMLMessageToChannelUtils.
 * Tests HTML message sending to Teams channels with error handling and permission management.
 */
@DisplayName("SendHTMLMessageToChannelUtils Test Suite")
class SendHTMLMessageToChannelUtilsTest {

    @Mock
    private ChatService chatService;

    private SendHTMLMessageToChannelUtils utils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        utils = new SendHTMLMessageToChannelUtils(chatService);
    }

    @Nested
    @DisplayName("Successful HTML Message Sending Tests")
    class SuccessfulHTMLMessageSendingTests {

        @Test
        @DisplayName("Should successfully send simple HTML message to channel")
        void testSendSimpleHTMLMessageToChannel() {
            // Arrange
            String teamName = "Engineering Team";
            String channelName = "General";
            String messageContent = "<p>Hello, this is a test message.</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);

            verify(chatService, times(1)).sendMessageToChannel(teamName, channelName, messageContent, true);
        }

        @Test
        @DisplayName("Should successfully send complex HTML message to channel")
        void testSendComplexHTMLMessageToChannel() {
            // Arrange
            String teamName = "Marketing Team";
            String channelName = "Announcements";
            String messageContent = "<div><h2>Important Update</h2><p>Please review the <strong>new guidelines</strong>.</p><ul><li>Item 1</li><li>Item 2</li></ul></div>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);

            verify(chatService, times(1)).sendMessageToChannel(teamName, channelName, messageContent, true);
        }

        @Test
        @DisplayName("Should handle successful message sending to different channels")
        void testSendToMultipleChannels() {
            // Arrange
            String teamName = "Sales Team";
            String[] channels = {"General", "Announcements", "Random"};
            String messageContent = "<p>Test message</p>";

            when(chatService.sendMessageToChannel(eq(teamName), anyString(), eq(messageContent), eq(true)))
                    .thenReturn(true);

            // Act & Assert
            for (String channel : channels) {
                Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channel, messageContent);
                assertNotNull(result);
                assertTrue(result);
            }

            verify(chatService, times(channels.length)).sendMessageToChannel(
                    eq(teamName), anyString(), eq(messageContent), eq(true));
        }
    }

    @Nested
    @DisplayName("Permission Error Handling Tests")
    class PermissionErrorHandlingTests {

        @Test
        @DisplayName("Should handle Authorization_RequestDenied error")
        void testAuthorizationRequestDeniedError() {
            // Arrange
            String teamName = "Engineering Team";
            String channelName = "General";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenThrow(new RuntimeException("Authorization_RequestDenied: Insufficient privileges"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);
            });

            assertTrue(exception.getMessage().contains("Insufficient permissions"));
            assertTrue(exception.getMessage().contains("HTML message to channel"));
            assertTrue(exception.getMessage().contains("administrator"));
        }

        @Test
        @DisplayName("Should handle 403 Forbidden error")
        void testForbiddenError() {
            // Arrange
            String teamName = "Marketing Team";
            String channelName = "Announcements";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenThrow(new RuntimeException("403 Forbidden: Access denied"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);
            });

            assertTrue(exception.getMessage().contains("Insufficient permissions"));
            assertTrue(exception.getMessage().contains("HTML message to channel"));
        }

        @Test
        @DisplayName("Should handle Insufficient privileges error")
        void testInsufficientPrivilegesError() {
            // Arrange
            String teamName = "Sales Team";
            String channelName = "General";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenThrow(new RuntimeException("Insufficient privileges to complete the operation"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);
            });

            assertTrue(exception.getMessage().contains("Insufficient permissions"));
            assertTrue(exception.getMessage().contains("administrator"));
        }
    }

    @Nested
    @DisplayName("Generic Error Handling Tests")
    class GenericErrorHandlingTests {

        @Test
        @DisplayName("Should handle generic service exception")
        void testGenericServiceException() {
            // Arrange
            String teamName = "Engineering Team";
            String channelName = "General";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenThrow(new RuntimeException("Network error occurred"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);
            });

            assertTrue(exception.getMessage().contains("Failed to send HTML message to channel"));
            assertTrue(exception.getMessage().contains("Network error occurred"));
        }

        @Test
        @DisplayName("Should handle null pointer exception")
        void testNullPointerException() {
            // Arrange
            String teamName = "Marketing Team";
            String channelName = "Announcements";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenThrow(new NullPointerException("Null value encountered"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);
            });

            assertTrue(exception.getMessage().contains("Failed to send HTML message to channel"));
        }

        @Test
        @DisplayName("Should handle exception with null message")
        void testExceptionWithNullMessage() {
            // Arrange
            String teamName = "Sales Team";
            String channelName = "General";
            String messageContent = "<p>Test HTML message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenThrow(new RuntimeException((String) null));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);
            });

            assertTrue(exception.getMessage().contains("Failed to send HTML message to channel"));
        }
    }

    @Nested
    @DisplayName("HTML Content Variation Tests")
    class HTMLContentVariationTests {

        @Test
        @DisplayName("Should handle HTML with links")
        void testHTMLWithLinks() {
            // Arrange
            String teamName = "Engineering Team";
            String channelName = "General";
            String messageContent = "<p>Visit <a href='https://example.com'>our website</a> for more info.</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle HTML with images")
        void testHTMLWithImages() {
            // Arrange
            String teamName = "Marketing Team";
            String channelName = "Announcements";
            String messageContent = "<p>Check out: <img src='https://example.com/image.png' alt='Example' /></p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle HTML with tables")
        void testHTMLWithTables() {
            // Arrange
            String teamName = "Sales Team";
            String channelName = "Reports";
            String messageContent = "<table><tr><th>Header</th></tr><tr><td>Data</td></tr></table>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle HTML with lists")
        void testHTMLWithLists() {
            // Arrange
            String teamName = "Engineering Team";
            String channelName = "General";
            String messageContent = "<ul><li>First item</li><li>Second item</li><li>Third item</li></ul>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("Team and Channel Name Variation Tests")
    class TeamAndChannelNameVariationTests {

        @Test
        @DisplayName("Should handle team names with spaces")
        void testTeamNamesWithSpaces() {
            // Arrange
            String teamName = "Engineering Team 2024";
            String channelName = "General";
            String messageContent = "<p>Test message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);
        }

        @Test
        @DisplayName("Should handle channel names with special characters")
        void testChannelNamesWithSpecialCharacters() {
            // Arrange
            String teamName = "Marketing Team";
            String channelName = "Q&A - Support";
            String messageContent = "<p>Test message</p>";

            when(chatService.sendMessageToChannel(teamName, channelName, messageContent, true))
                    .thenReturn(true);

            // Act
            Boolean result = utils.sendHTMLMessageToChannelWithDetails(teamName, channelName, messageContent);

            // Assert
            assertNotNull(result);
            assertTrue(result);
        }
    }
}

