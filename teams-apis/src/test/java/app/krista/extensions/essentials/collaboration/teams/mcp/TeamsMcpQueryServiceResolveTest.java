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

package app.krista.extensions.essentials.collaboration.teams.mcp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers {@link TeamsMcpQueryService#findIdByDisplayName} — the pure matcher used to resolve a
 * team/channel referred to by name into the id Graph requires (so a user can say "channel =
 * SimRamJalTest" instead of pasting a GUID).
 */
class TeamsMcpQueryServiceResolveTest {

    private static final Gson GSON = new Gson();

    @Test
    void findsIdByDisplayNameCaseInsensitive() {
        JsonObject list = GSON.fromJson(
                "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"},"
                        + "{\"id\":\"t2\",\"displayName\":\"SimRamJalTest\"}]}", JsonObject.class);
        assertEquals("t2", TeamsMcpQueryService.findIdByDisplayName(list, "simramjaltest"));
        assertEquals("t1", TeamsMcpQueryService.findIdByDisplayName(list, "General"));
    }

    @Test
    void returnsNullWhenNoMatch() {
        JsonObject list = GSON.fromJson(
                "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}", JsonObject.class);
        assertNull(TeamsMcpQueryService.findIdByDisplayName(list, "Nope"));
    }

    @Test
    void returnsNullForBlankNameOrEmptyList() {
        JsonObject list = GSON.fromJson(
                "{\"value\":[{\"id\":\"t1\",\"displayName\":\"General\"}]}", JsonObject.class);
        assertNull(TeamsMcpQueryService.findIdByDisplayName(list, null));
        assertNull(TeamsMcpQueryService.findIdByDisplayName(list, "  "));
        assertNull(TeamsMcpQueryService.findIdByDisplayName(GSON.fromJson("{}", JsonObject.class), "General"));
    }

    @Test
    void returnsNullWhenListResponseIsNullOrValueIsNotAnArray() {
        assertNull(TeamsMcpQueryService.findIdByDisplayName(null, "General"));
        JsonObject notArray = GSON.fromJson("{\"value\":\"oops\"}", JsonObject.class);
        assertNull(TeamsMcpQueryService.findIdByDisplayName(notArray, "General"));
    }

    @Test
    void skipsNonObjectElementsInTheValueArray() {
        JsonObject withPrimitiveElement = GSON.fromJson(
                "{\"value\":[\"not-an-object\",{\"id\":\"t1\",\"displayName\":\"General\"}]}", JsonObject.class);
        assertEquals("t1", TeamsMcpQueryService.findIdByDisplayName(withPrimitiveElement, "General"));
    }

    @Test
    void extractsSendMessageContent() {
        assertEquals("Standup at 10", TeamsMcpQueryService.extractSendContent(
                "{\"body\":{\"content\":\"Standup at 10\"}}"));
    }

    @Test
    void extractSendContentIsNullWhenMissingOrUnparseable() {
        assertNull(TeamsMcpQueryService.extractSendContent(null));
        assertNull(TeamsMcpQueryService.extractSendContent(""));
        assertNull(TeamsMcpQueryService.extractSendContent("{}"));
        assertNull(TeamsMcpQueryService.extractSendContent("{\"body\":{}}"));
        assertNull(TeamsMcpQueryService.extractSendContent("not json"));
    }

    @Test
    void extractSendContentIsNullWhenBodyFieldIsNotAnObjectOrContentIsNull() {
        assertNull(TeamsMcpQueryService.extractSendContent("{\"body\":\"not-an-object\"}"));
        assertNull(TeamsMcpQueryService.extractSendContent("{\"body\":{\"content\":null}}"));
    }
}
