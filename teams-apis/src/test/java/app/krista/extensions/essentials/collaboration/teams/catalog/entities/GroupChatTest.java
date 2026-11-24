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
 * Test suite for GroupChat entity.
 */
@DisplayName("GroupChat Entity Test Suite")
class GroupChatTest {

    @Test
    @DisplayName("Should create GroupChat with all fields")
    void testCreateGroupChatWithAllFields() {
        GroupChat groupChat = new GroupChat();
        groupChat.groupId = "group-123";
        groupChat.topic = "Project Discussion";
        groupChat.type = "group";
        groupChat.createdDateTime = System.currentTimeMillis();
        
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> member = new HashMap<>();
        member.put("Name", "Alice Smith");
        member.put("Email", "alice@example.com");
        members.add(member);
        groupChat.members = members;

        assertEquals("group-123", groupChat.groupId);
        assertEquals("Project Discussion", groupChat.topic);
        assertEquals("group", groupChat.type);
        assertNotNull(groupChat.createdDateTime);
        assertNotNull(groupChat.members);
        assertEquals(1, groupChat.members.size());
    }

    @Test
    @DisplayName("Should create GroupChat with minimal fields")
    void testCreateGroupChatWithMinimalFields() {
        GroupChat groupChat = new GroupChat();
        groupChat.members = new ArrayList<>();

        assertNull(groupChat.groupId);
        assertNull(groupChat.topic);
        assertNull(groupChat.type);
        assertNull(groupChat.createdDateTime);
        assertNotNull(groupChat.members);
        assertTrue(groupChat.members.isEmpty());
    }

    @Test
    @DisplayName("Should allow null values for all fields")
    void testNullValues() {
        GroupChat groupChat = new GroupChat();

        assertNull(groupChat.groupId);
        assertNull(groupChat.topic);
        assertNull(groupChat.type);
        assertNull(groupChat.createdDateTime);
        assertNull(groupChat.members);
    }

    @Test
    @DisplayName("Should allow updating fields")
    void testUpdateFields() {
        GroupChat groupChat = new GroupChat();
        groupChat.groupId = "group-old";
        groupChat.topic = "Old Topic";

        groupChat.groupId = "group-new";
        groupChat.topic = "New Topic";
        groupChat.type = "updated";
        groupChat.createdDateTime = 1234567890L;

        assertEquals("group-new", groupChat.groupId);
        assertEquals("New Topic", groupChat.topic);
        assertEquals("updated", groupChat.type);
        assertEquals(1234567890L, groupChat.createdDateTime);
    }

    @Test
    @DisplayName("Should handle different chat types")
    void testDifferentChatTypes() {
        GroupChat groupChat1 = new GroupChat();
        groupChat1.type = "group";

        GroupChat groupChat2 = new GroupChat();
        groupChat2.type = "meeting";

        assertEquals("group", groupChat1.type);
        assertEquals("meeting", groupChat2.type);
    }
}

