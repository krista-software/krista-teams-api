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

package app.krista.extensions.essentials.collaboration.teams.catalog.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ChannelMessage entity.
 */
@DisplayName("ChannelMessage Entity Test Suite")
class ChannelMessageTest {

    @Test
    @DisplayName("Should create ChannelMessage with all fields")
    void testCreateChannelMessageWithAllFields() {
        ChannelMessage message = new ChannelMessage();
        message.senderName = "John Doe";
        message.subject = "Meeting Notes";
        message.message = "Here are the notes from today's meeting...";
        message.messageId = "msg-123";
        message.attachmentLinks = "https://example.com/file1.pdf, https://example.com/file2.docx";
        message.createdDateTime = System.currentTimeMillis();

        assertEquals("John Doe", message.senderName);
        assertEquals("Meeting Notes", message.subject);
        assertEquals("Here are the notes from today's meeting...", message.message);
        assertEquals("msg-123", message.messageId);
        assertEquals("https://example.com/file1.pdf, https://example.com/file2.docx", message.attachmentLinks);
        assertNotNull(message.createdDateTime);
    }

    @Test
    @DisplayName("Should create ChannelMessage with minimal fields")
    void testCreateChannelMessageWithMinimalFields() {
        ChannelMessage message = new ChannelMessage();
        message.messageId = "msg-456";

        assertNull(message.senderName);
        assertNull(message.subject);
        assertNull(message.message);
        assertEquals("msg-456", message.messageId);
        assertNull(message.attachmentLinks);
        assertNull(message.createdDateTime);
    }

    @Test
    @DisplayName("Should allow null values for optional fields")
    void testNullValues() {
        ChannelMessage message = new ChannelMessage();

        assertNull(message.senderName);
        assertNull(message.subject);
        assertNull(message.message);
        assertNull(message.messageId);
        assertNull(message.attachmentLinks);
        assertNull(message.createdDateTime);
    }

    @Test
    @DisplayName("Should allow updating fields")
    void testUpdateFields() {
        ChannelMessage message = new ChannelMessage();
        message.messageId = "msg-old";
        message.subject = "Old Subject";

        message.messageId = "msg-new";
        message.subject = "New Subject";
        message.message = "Updated message content";
        message.senderName = "Jane Doe";

        assertEquals("msg-new", message.messageId);
        assertEquals("New Subject", message.subject);
        assertEquals("Updated message content", message.message);
        assertEquals("Jane Doe", message.senderName);
    }

    @Test
    @DisplayName("Should handle message without attachments")
    void testMessageWithoutAttachments() {
        ChannelMessage message = new ChannelMessage();
        message.messageId = "msg-789";
        message.message = "Simple message without attachments";

        assertEquals("msg-789", message.messageId);
        assertEquals("Simple message without attachments", message.message);
        assertNull(message.attachmentLinks);
    }

    @Test
    @DisplayName("Should handle message with multiple attachments")
    void testMessageWithMultipleAttachments() {
        ChannelMessage message = new ChannelMessage();
        message.messageId = "msg-multi";
        message.attachmentLinks = "https://example.com/file1.pdf, https://example.com/file2.docx, https://example.com/file3.xlsx";

        assertEquals("msg-multi", message.messageId);
        assertNotNull(message.attachmentLinks);
        assertTrue(message.attachmentLinks.contains("file1.pdf"));
        assertTrue(message.attachmentLinks.contains("file2.docx"));
        assertTrue(message.attachmentLinks.contains("file3.xlsx"));
    }

    @Test
    @DisplayName("Should handle long message content")
    void testLongMessageContent() {
        ChannelMessage message = new ChannelMessage();
        message.messageId = "msg-long";
        message.message = "This is a very long message content that spans multiple lines and contains a lot of information. ".repeat(10);

        assertEquals("msg-long", message.messageId);
        assertNotNull(message.message);
        assertTrue(message.message.length() > 500);
    }
}

