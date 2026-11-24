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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Channel entity.
 */
@DisplayName("Channel Entity Test Suite")
class ChannelTest {

    @Test
    @DisplayName("Should create Channel with all fields")
    void testCreateChannelWithAllFields() {
        Channel channel = new Channel();
        channel.iD = "channel-123";
        channel.name = "General";
        channel.description = "General discussion channel";
        channel.email = "general@team.example.com";
        channel.teamID = "team-456";
        
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> member = new HashMap<>();
        member.put("Name", "Jane Doe");
        member.put("Email", "jane@example.com");
        members.add(member);
        channel.members = members;

        assertEquals("channel-123", channel.iD);
        assertEquals("General", channel.name);
        assertEquals("General discussion channel", channel.description);
        assertEquals("general@team.example.com", channel.email);
        assertEquals("team-456", channel.teamID);
        assertNotNull(channel.members);
        assertEquals(1, channel.members.size());
    }

    @Test
    @DisplayName("Should create Channel with minimal fields")
    void testCreateChannelWithMinimalFields() {
        Channel channel = new Channel();
        channel.name = "Test Channel";
        channel.members = new ArrayList<>();

        assertNull(channel.iD);
        assertEquals("Test Channel", channel.name);
        assertNull(channel.description);
        assertNull(channel.email);
        assertNull(channel.teamID);
        assertNotNull(channel.members);
        assertTrue(channel.members.isEmpty());
    }

    @Test
    @DisplayName("Should allow null values for optional fields")
    void testNullValues() {
        Channel channel = new Channel();

        assertNull(channel.iD);
        assertNull(channel.name);
        assertNull(channel.description);
        assertNull(channel.email);
        assertNull(channel.teamID);
        assertNull(channel.members);
    }

    @Test
    @DisplayName("Should allow updating fields")
    void testUpdateFields() {
        Channel channel = new Channel();
        channel.iD = "channel-old";
        channel.name = "Old Name";

        channel.iD = "channel-new";
        channel.name = "New Name";
        channel.description = "Updated description";
        channel.email = "new@example.com";

        assertEquals("channel-new", channel.iD);
        assertEquals("New Name", channel.name);
        assertEquals("Updated description", channel.description);
        assertEquals("new@example.com", channel.email);
    }
}

