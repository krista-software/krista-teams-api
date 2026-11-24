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

import app.krista.model.base.FreeForm;
import app.krista.sdk.msauth.extension.mcp.McpSearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers every pure JSON-shaping branch of {@link TeamsMcpProductConfig}: it has no network or
 * static dependencies, so every method is reachable directly.
 */
class TeamsMcpProductConfigTest {

    private final TeamsMcpProductConfig config = new TeamsMcpProductConfig();

    @Test
    void searchTargetsGraphSearchWithKql() {
        assertEquals("https://graph.microsoft.com/v1.0/search/query", config.searchUrl());
        assertEquals("POST", config.searchHttpMethod());
        assertEquals("KQL", config.queryLanguage());
        assertEquals("KQL (Keyword Query Language)", config.queryLanguageFullName());
        assertEquals("Microsoft Teams", config.productName());
    }

    @Test
    void standardFieldsAndColumnsAndExamplesAreFixed() {
        assertEquals(List.of("From", "CreatedDateTime", "Summary", "ChatOrChannel", "Link"), config.standardFields());
        assertEquals(5, config.resultColumns().size());
        assertEquals("link", config.resultColumns().get(4).type());
        assertFalse(config.nlExamples().isEmpty());
        assertFalse(config.kqlExamples().isEmpty());
    }

    @Test
    void searchRequestBodyUsesChatMessageEntityType() {
        String body = config.buildSearchRequestBody("project update", 10, false);
        assertTrue(body.contains("\"chatMessage\""), "Graph Search over Teams uses the chatMessage entity type");
        assertTrue(body.contains("project update"));
        assertTrue(body.contains("\"size\":10"));
    }

    @Test
    void promptsEmbedTheirInputs() {
        assertTrue(config.queryGenerationPrompt("who posted this?").contains("who posted this?"));
        assertTrue(config.answerExtractionPrompt("q1", "data-blob").contains("q1"));
        assertTrue(config.answerExtractionPrompt("q1", "data-blob").contains("data-blob"));
        assertTrue(config.graphApiPlanningPrompt("send hi to sales").contains("send hi to sales"));
    }

    // ── parseSearchResponse ────────────────────────────────────────────────

    @Test
    void parseSearchResponseHandlesMissingValueArray() {
        McpSearchResult result = config.parseSearchResponse("{}", "https://graph.microsoft.com");
        assertEquals("kql", result.type());
        assertEquals(0, result.total());
        assertEquals(0, result.shown());
        assertTrue(result.rows().isEmpty());
    }

    @Test
    void parseSearchResponseExtractsFromDateSummaryAndChatOrChannelViaChannelIdentity() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"total\":5,\"hits\":[{"
                + "\"hitId\":\"H1\","
                + "\"resource\":{"
                + "\"id\":\"M1\","
                + "\"createdDateTime\":\"2026-07-20T10:00:00Z\","
                + "\"from\":{\"user\":{\"displayName\":\"Priya\"}},"
                + "\"summary\":\"hello there\","
                + "\"webUrl\":\"https://teams.microsoft.com/l/message/1\","
                + "\"channelIdentity\":{\"channelId\":\"C1\"}"
                + "}}]}]}]}";
        McpSearchResult result = config.parseSearchResponse(json, "https://graph.microsoft.com");
        assertEquals(5, result.total());
        assertEquals(1, result.shown());
        Map<String, Object> row = result.rows().get(0);
        assertEquals("Priya", row.get("from"));
        assertEquals("2026-07-20", row.get("createdDateTime"));
        assertEquals("hello there", row.get("summary"));
        assertEquals("https://teams.microsoft.com/l/message/1", row.get("link"));
        assertEquals("C1", row.get("chatOrChannel"));
        assertEquals("M1", row.get("id"));
        assertEquals("H1", row.get("messageId"));
    }

    @Test
    void parseSearchResponseSkipsNullAndEmptyStringFieldsAndHandlesArrayAndDateSuffixValues() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{"
                + "\"nullField\":null,"
                + "\"emptyField\":\"\","
                + "\"tags\":[1,2,3],"
                + "\"customDate\":\"2026-07-20\","
                + "\"summary\":\"x\""
                + "}}]}]}]}";
        McpSearchResult result = config.parseSearchResponse(json, "b");
        Map<String, Object> row = result.rows().get(0);
        assertFalse(row.containsKey("nullField"));
        assertFalse(row.containsKey("emptyField"));
        assertFalse(row.containsKey("tags"), "arrays have no single-value representation and are skipped");
        assertEquals("2026-07-20", row.get("customDate"), "a *Date-suffixed key without a time part passes through unchanged");
    }

    @Test
    void parseSearchResponseDefaultsMissingHitsContainersHitsAndResourceToEmpty() {
        assertTrue(config.parseSearchResponse("{\"value\":[{}]}", "b").rows().isEmpty());
        assertTrue(config.parseSearchResponse("{\"value\":[{\"hitsContainers\":[{}]}]}", "b").rows().isEmpty());
        // A hit with no "resource" still produces a row (defaulted to an empty JsonObject).
        assertEquals(1, config.parseSearchResponse(
                "{\"value\":[{\"hitsContainers\":[{\"hits\":[{}]}]}]}", "b").rows().size());
    }

    @Test
    void parseSearchResponseFallsBackToChatIdAndBodyContentWhenSummaryAndChannelIdentityMissing() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{"
                + "\"chatId\":\"CHAT1\","
                + "\"body\":{\"content\":\"<p>plain <b>text</b></p>\"}"
                + "}}]}]}]}";
        McpSearchResult result = config.parseSearchResponse(json, "https://graph.microsoft.com");
        Map<String, Object> row = result.rows().get(0);
        assertEquals("CHAT1", row.get("chatOrChannel"));
        assertEquals("plain text", row.get("summary"));
        assertFalse(row.containsKey("messageId"), "no hitId present on the hit");
    }

    @Test
    void parseSearchResponseSkipsComplexFieldsWithNoDisplayRepresentation() {
        // channelIdentity is a nested object handled separately by extractChatOrChannel — inside the
        // generic per-field loop it must be skipped (toDisplayValue returns null for it).
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"channelIdentity\":{\"channelId\":\"\"}}}]}]}]}";
        McpSearchResult result = config.parseSearchResponse(json, "https://graph.microsoft.com");
        Map<String, Object> row = result.rows().get(0);
        assertFalse(row.containsKey("channelIdentity"));
        assertFalse(row.containsKey("chatOrChannel"), "blank channelId and no chatId means no chat/channel");
    }

    // ── parseStandardResponse ───────────────────────────────────────────────

    @Test
    void parseStandardResponseMapsKnownFieldsToPascalCaseFreeFormKeys() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"hitId\":\"H2\","
                + "\"resource\":{"
                + "\"from\":{\"user\":{\"displayName\":\"Sam\"}},"
                + "\"createdDateTime\":\"2026-07-20T10:00:00Z\","
                + "\"lastModifiedDateTime\":\"2026-07-21T10:00:00Z\","
                + "\"summary\":\"the summary\","
                + "\"webUrl\":\"https://link\","
                + "\"importance\":\"high\","
                + "\"chatId\":\"CHAT2\","
                + "\"unknownField\":\"passthrough\""
                + "}}]}]}]}";
        List<FreeForm> rows = config.parseStandardResponse(json, "https://graph.microsoft.com");
        FreeForm ff = rows.get(0);
        assertEquals("Sam", ff.get("From"));
        assertEquals("2026-07-20", ff.get("CreatedDateTime"));
        assertEquals("2026-07-21", ff.get("LastModifiedDateTime"));
        assertEquals("the summary", ff.get("Summary"));
        assertEquals("https://link", ff.get("Link"));
        assertEquals("high", ff.get("Importance"));
        assertEquals("CHAT2", ff.get("ChatId"));
        assertEquals("passthrough", ff.get("unknownField"));
        assertEquals("CHAT2", ff.get("ChatOrChannel"));
        assertEquals("H2", ff.get("MessageId"));
    }

    @Test
    void parseStandardResponseHandlesMissingValueArray() {
        assertTrue(config.parseStandardResponse("{}", "b").isEmpty());
    }

    @Test
    void parseStandardResponseSkipsNullAndEmptyStringFieldsAndSkipsArrayValuedFields() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"nullField\":null,\"emptyField\":\"\",\"tags\":[1,2,3],\"summary\":\"x\"}"
                + "}]}]}]}";
        FreeForm ff = config.parseStandardResponse(json, "b").get(0);
        assertFalse(ff.containsKey("nullField"));
        assertFalse(ff.containsKey("emptyField"));
        assertFalse(ff.containsKey("tags"));
    }

    @Test
    void parseStandardResponseDefaultsMissingHitsContainersHitsAndResourceToEmpty() {
        assertTrue(config.parseStandardResponse("{\"value\":[{}]}", "b").isEmpty());
        assertTrue(config.parseStandardResponse("{\"value\":[{\"hitsContainers\":[{}]}]}", "b").isEmpty());
        assertEquals(1, config.parseStandardResponse(
                "{\"value\":[{\"hitsContainers\":[{\"hits\":[{}]}]}]}", "b").size());
    }

    @Test
    void parseStandardResponseFallsBackToBodyContentWhenNoSummary() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"body\":{\"content\":\"raw text\"}}}]}]}]}";
        List<FreeForm> rows = config.parseStandardResponse(json, "https://graph.microsoft.com");
        assertEquals("raw text", rows.get(0).get("Summary"));
    }

    @Test
    void parseStandardResponseOmitsSummaryFallbackWhenBodyEmpty() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"id\":\"M9\"}}]}]}]}";
        List<FreeForm> rows = config.parseStandardResponse(json, "https://graph.microsoft.com");
        assertFalse(rows.get(0).containsKey("Summary"));
    }

    // ── parseAllFieldsResponse ──────────────────────────────────────────────

    @Test
    void parseAllFieldsResponsePrefersHitLevelSummaryAndAddsTitleKeyAliases() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"hitId\":\"H3\","
                + "\"summary\":\"hit-level summary\","
                + "\"resource\":{\"from\":{\"user\":{\"displayName\":\"Alex\"}}}"
                + "}]}]}]}";
        List<Map<String, String>> rows = config.parseAllFieldsResponse(json, "https://graph.microsoft.com");
        Map<String, String> row = rows.get(0);
        assertEquals("hit-level summary", row.get("summary"));
        assertEquals("hit-level summary", row.get("title"));
        assertEquals("hit-level summary", row.get("key"));
        assertEquals("H3", row.get("id"));
        assertEquals("Alex", row.get("from"));
    }

    @Test
    void parseAllFieldsResponseHandlesMissingValueArray() {
        assertTrue(config.parseAllFieldsResponse("{}", "b").isEmpty());
    }

    @Test
    void parseAllFieldsResponseSkipsNullAndEmptyStringFieldsAndSkipsArrayValuedFields() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"nullField\":null,\"emptyField\":\"\",\"tags\":[1,2,3],\"summary\":\"x\"}"
                + "}]}]}]}";
        Map<String, String> row = config.parseAllFieldsResponse(json, "b").get(0);
        assertFalse(row.containsKey("nullField"));
        assertFalse(row.containsKey("emptyField"));
        assertFalse(row.containsKey("tags"));
    }

    @Test
    void parseAllFieldsResponseDefaultsMissingHitsContainersHitsAndResourceToEmpty() {
        assertTrue(config.parseAllFieldsResponse("{\"value\":[{}]}", "b").isEmpty());
        assertTrue(config.parseAllFieldsResponse("{\"value\":[{\"hitsContainers\":[{}]}]}", "b").isEmpty());
        assertEquals(1, config.parseAllFieldsResponse(
                "{\"value\":[{\"hitsContainers\":[{\"hits\":[{}]}]}]}", "b").size());
    }

    @Test
    void parseAllFieldsResponseFallsBackToResourceSummaryThenBodyContent() {
        String withResourceSummary = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"summary\":\"resource summary\"}}]}]}]}";
        Map<String, String> row1 = config.parseAllFieldsResponse(withResourceSummary, "b").get(0);
        assertEquals("resource summary", row1.get("summary"));

        String withBodyOnly = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"body\":{\"content\":\"<i>body text</i>\"}}}]}]}]}";
        Map<String, String> row2 = config.parseAllFieldsResponse(withBodyOnly, "b").get(0);
        assertEquals("body text", row2.get("summary"));

        String withNothing = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"id\":\"none\"}}]}]}]}";
        Map<String, String> row3 = config.parseAllFieldsResponse(withNothing, "b").get(0);
        assertFalse(row3.containsKey("summary"));
        assertFalse(row3.containsKey("title"));
    }

    @Test
    void parseAllFieldsResponseAddsChatOrChannelWhenPresent() {
        String json = "{\"value\":[{\"hitsContainers\":[{\"hits\":[{"
                + "\"resource\":{\"chatId\":\"CHAT3\"}}]}]}]}";
        Map<String, String> row = config.parseAllFieldsResponse(json, "b").get(0);
        assertEquals("CHAT3", row.get("chatOrChannel"));
    }
}
