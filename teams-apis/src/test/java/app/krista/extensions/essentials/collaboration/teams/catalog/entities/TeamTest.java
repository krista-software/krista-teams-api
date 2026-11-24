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
 * Test suite for Team entity.
 */
@DisplayName("Team Entity Test Suite")
class TeamTest {

    @Test
    @DisplayName("Should create Team with all fields")
    void testCreateTeamWithAllFields() {
        Team team = new Team();
        team.iD = "team-123";
        team.name = "Development Team";
        team.description = "Team for development";
        
        List<Map<String, Object>> members = new ArrayList<>();
        Map<String, Object> member = new HashMap<>();
        member.put("Name", "John Doe");
        member.put("Email", "john@example.com");
        members.add(member);
        team.members = members;

        assertEquals("team-123", team.iD);
        assertEquals("Development Team", team.name);
        assertEquals("Team for development", team.description);
        assertNotNull(team.members);
        assertEquals(1, team.members.size());
        assertEquals("John Doe", team.members.get(0).get("Name"));
        assertEquals("john@example.com", team.members.get(0).get("Email"));
    }

    @Test
    @DisplayName("Should create Team with minimal fields")
    void testCreateTeamWithMinimalFields() {
        Team team = new Team();
        team.name = "Test Team";

        assertNull(team.iD);
        assertEquals("Test Team", team.name);
        assertNull(team.description);
        assertNull(team.members);
    }

    @Test
    @DisplayName("Should create Team with empty members list")
    void testCreateTeamWithEmptyMembers() {
        Team team = new Team();
        team.iD = "team-456";
        team.name = "Empty Team";
        team.members = new ArrayList<>();

        assertEquals("team-456", team.iD);
        assertEquals("Empty Team", team.name);
        assertNotNull(team.members);
        assertTrue(team.members.isEmpty());
    }

    @Test
    @DisplayName("Should create Team with multiple members")
    void testCreateTeamWithMultipleMembers() {
        Team team = new Team();
        team.iD = "team-789";
        team.name = "Large Team";
        
        List<Map<String, Object>> members = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> member = new HashMap<>();
            member.put("Name", "User " + i);
            member.put("Email", "user" + i + "@example.com");
            members.add(member);
        }
        team.members = members;

        assertEquals(5, team.members.size());
        assertEquals("User 1", team.members.get(0).get("Name"));
        assertEquals("user5@example.com", team.members.get(4).get("Email"));
    }

    @Test
    @DisplayName("Should allow null values for all fields")
    void testNullValues() {
        Team team = new Team();

        assertNull(team.iD);
        assertNull(team.name);
        assertNull(team.description);
        assertNull(team.members);
    }

    @Test
    @DisplayName("Should allow updating fields")
    void testUpdateFields() {
        Team team = new Team();
        team.iD = "team-old";
        team.name = "Old Name";

        team.iD = "team-new";
        team.name = "New Name";
        team.description = "Updated description";

        assertEquals("team-new", team.iD);
        assertEquals("New Name", team.name);
        assertEquals("Updated description", team.description);
    }
}

