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
import app.krista.sdk.msauth.extension.mcp.McpProductConfig;
import app.krista.sdk.msauth.extension.mcp.McpSearchResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Teams-specific implementation of {@link McpProductConfig}.
 *
 * <p>Uses Microsoft Graph {@code /search/query} API with KQL
 * (Keyword Query Language) to search for Microsoft Teams chat and
 * channel messages.</p>
 */
public class TeamsMcpProductConfig implements McpProductConfig {

    private static final Gson GSON = new Gson();
    private static final String SEARCH_URL = "https://graph.microsoft.com/v1.0/search/query";

    @Override
    public String productName() {
        return "Microsoft Teams";
    }

    @Override
    public String queryLanguage() {
        return "KQL";
    }

    @Override
    public String queryLanguageFullName() {
        return "KQL (Keyword Query Language)";
    }

    @Override
    public String searchUrl() {
        return SEARCH_URL;
    }

    @Override
    public String searchHttpMethod() {
        return "POST";
    }

    @Override
    public List<String> standardFields() {
        return List.of("From", "CreatedDateTime", "Summary", "ChatOrChannel", "Link");
    }

    @Override
    public List<ColumnDef> resultColumns() {
        return List.of(
                new ColumnDef("from", "From", "text"),
                new ColumnDef("createdDateTime", "Created Date Time", "text"),
                new ColumnDef("summary", "Summary", "text"),
                new ColumnDef("chatOrChannel", "Chat Or Channel", "text"),
                new ColumnDef("link", "Link", "link")
        );
    }

    @Override
    public List<String> nlExamples() {
        return List.of(
                "Show messages about the quarterly report",
                "Find messages from Priya last week",
                "What did the platform team discuss yesterday?",
                "Find messages mentioning the release deadline"
        );
    }

    @Override
    public List<String> kqlExamples() {
        return List.of(
                "quarterly report",
                "from:priya sent>=2026-07-12",
                "release AND deadline"
        );
    }

    @Override
    public String buildSearchRequestBody(String query, int maxResults, boolean allFields) {
        JsonObject queryObj = new JsonObject();
        queryObj.addProperty("queryString", query);

        JsonArray entityTypes = new JsonArray();
        entityTypes.add("chatMessage");

        // Explicitly request message fields; without "fields" the Graph Search API
        // may return a minimal resource object with only @odata.type.
        JsonArray fields = new JsonArray();
        for (String f : new String[]{"id", "createdDateTime", "from", "body", "chatId",
                "channelIdentity", "webUrl", "summary"}) {
            fields.add(f);
        }

        JsonObject request = new JsonObject();
        request.add("entityTypes", entityTypes);
        request.add("query", queryObj);
        request.addProperty("from", 0);
        request.addProperty("size", maxResults);
        request.add("fields", fields);

        JsonArray requests = new JsonArray();
        requests.add(request);

        JsonObject body = new JsonObject();
        body.add("requests", requests);

        return body.toString();
    }

    @Override
    public McpSearchResult parseSearchResponse(String json, String baseUrl) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int total = 0;

        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonArray values = root.has("value") ? root.getAsJsonArray("value") : new JsonArray();

        for (JsonElement valEl : values) {
            JsonObject val = valEl.getAsJsonObject();
            JsonArray hitsContainers = val.has("hitsContainers")
                    ? val.getAsJsonArray("hitsContainers") : new JsonArray();

            for (JsonElement hcEl : hitsContainers) {
                JsonObject hc = hcEl.getAsJsonObject();
                total += hc.has("total") ? hc.get("total").getAsInt() : 0;
                JsonArray hits = hc.has("hits") ? hc.getAsJsonArray("hits") : new JsonArray();

                for (JsonElement hitEl : hits) {
                    JsonObject hit = hitEl.getAsJsonObject();
                    JsonObject resource = hit.has("resource")
                            ? hit.getAsJsonObject("resource") : new JsonObject();

                    Map<String, Object> row = new LinkedHashMap<>();
                    // Include every field the Graph API returns — no hardcoded picks
                    for (Map.Entry<String, JsonElement> entry : resource.entrySet()) {
                        String strVal = toDisplayValue(entry.getKey(), entry.getValue(), resource);
                        if (strVal != null && !strVal.isEmpty()) {
                            row.put(toSearchColumnKey(entry.getKey()), strVal);
                        }
                    }
                    // Fall back to a plain-text strip of body.content when summary is absent
                    if (!row.containsKey("summary")) {
                        String bodyText = extractBodyContent(resource);
                        if (bodyText != null && !bodyText.isEmpty()) {
                            row.put("summary", bodyText);
                        }
                    }
                    // Chat/channel identity — Teams messages use channelIdentity.channelId or chatId
                    String chatOrChannel = extractChatOrChannel(resource);
                    if (chatOrChannel != null && !chatOrChannel.isEmpty()) {
                        row.put("chatOrChannel", chatOrChannel);
                    }
                    // hitId is on the hit object, not inside resource
                    String hitId = getString(hit, "hitId");
                    if (!hitId.isEmpty()) {
                        row.put("messageId", hitId);
                    }
                    rows.add(row);
                }
            }
        }

        return new McpSearchResult("kql", total, rows.size(), rows);
    }

    @Override
    public List<FreeForm> parseStandardResponse(String json, String baseUrl) {
        List<FreeForm> rows = new ArrayList<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonArray values = root.has("value") ? root.getAsJsonArray("value") : new JsonArray();

        for (JsonElement valEl : values) {
            JsonObject val = valEl.getAsJsonObject();
            JsonArray hitsContainers = val.has("hitsContainers")
                    ? val.getAsJsonArray("hitsContainers") : new JsonArray();

            for (JsonElement hcEl : hitsContainers) {
                JsonObject hc = hcEl.getAsJsonObject();
                JsonArray hits = hc.has("hits") ? hc.getAsJsonArray("hits") : new JsonArray();

                for (JsonElement hitEl : hits) {
                    JsonObject hit = hitEl.getAsJsonObject();
                    JsonObject resource = hit.has("resource")
                            ? hit.getAsJsonObject("resource") : new JsonObject();

                    FreeForm ff = new FreeForm();
                    boolean hasSummary = false;
                    // Include every field the Graph API returns — no hardcoded picks
                    for (Map.Entry<String, JsonElement> entry : resource.entrySet()) {
                        String strVal = toDisplayValue(entry.getKey(), entry.getValue(), resource);
                        if (strVal != null && !strVal.isEmpty()) {
                            if ("summary".equals(entry.getKey())) {
                                hasSummary = true;
                            }
                            ff.put(toFreeFormKey(entry.getKey()), " Text ", strVal);
                        }
                    }
                    // Fall back to a plain-text strip of body.content when summary is absent
                    if (!hasSummary) {
                        String bodyText = extractBodyContent(resource);
                        if (bodyText != null && !bodyText.isEmpty()) {
                            ff.put("Summary", " Text ", bodyText);
                        }
                    }
                    // Chat/channel identity — Teams messages use channelIdentity.channelId or chatId
                    String chatOrChannel = extractChatOrChannel(resource);
                    if (chatOrChannel != null && !chatOrChannel.isEmpty()) {
                        ff.put("ChatOrChannel", " Text ", chatOrChannel);
                    }
                    // hitId is on the hit object, not inside resource
                    String hitId = getString(hit, "hitId");
                    if (!hitId.isEmpty()) {
                        ff.put("MessageId", " Text ", hitId);
                    }
                    rows.add(ff);
                }
            }
        }
        return rows;
    }

    @Override
    public List<Map<String, String>> parseAllFieldsResponse(String json, String baseUrl) {
        List<Map<String, String>> rows = new ArrayList<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        JsonArray values = root.has("value") ? root.getAsJsonArray("value") : new JsonArray();

        for (JsonElement valEl : values) {
            JsonObject val = valEl.getAsJsonObject();
            JsonArray hitsContainers = val.has("hitsContainers")
                    ? val.getAsJsonArray("hitsContainers") : new JsonArray();

            for (JsonElement hcEl : hitsContainers) {
                JsonObject hc = hcEl.getAsJsonObject();
                JsonArray hits = hc.has("hits") ? hc.getAsJsonArray("hits") : new JsonArray();

                for (JsonElement hitEl : hits) {
                    JsonObject hit = hitEl.getAsJsonObject();
                    JsonObject resource = hit.has("resource")
                            ? hit.getAsJsonObject("resource") : new JsonObject();

                    Map<String, String> row = new LinkedHashMap<>();
                    // Include every field the Graph API returns — no hardcoded picks
                    for (Map.Entry<String, JsonElement> entry : resource.entrySet()) {
                        String strVal = toDisplayValue(entry.getKey(), entry.getValue(), resource);
                        if (strVal != null && !strVal.isEmpty()) {
                            row.put(entry.getKey(), strVal);
                        }
                    }
                    // Chat/channel identity — Teams messages use channelIdentity.channelId or chatId
                    String chatOrChannel = extractChatOrChannel(resource);
                    if (chatOrChannel != null && !chatOrChannel.isEmpty()) {
                        row.put("chatOrChannel", chatOrChannel);
                    }
                    // hitId lives on the hit, not inside resource
                    String hitId = getString(hit, "hitId");
                    if (!hitId.isEmpty()) {
                        row.put("id", hitId);
                    }
                    // summary — prefer the hit-level summary, else resource summary,
                    // else a plain-text strip of body.content
                    String summary = getString(hit, "summary");
                    if (summary.isEmpty()) {
                        summary = row.getOrDefault("summary", "");
                    }
                    if (summary.isEmpty()) {
                        String bodyText = extractBodyContent(resource);
                        summary = bodyText != null ? bodyText : "";
                    }
                    if (!summary.isEmpty()) {
                        row.put("summary", summary);
                    }
                    // title/key aliases for SDK compatibility
                    if (!summary.isEmpty()) {
                        row.put("title", summary);
                        row.put("key", summary);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    @Override
    public String queryGenerationPrompt(String question) {
        return "You are a Microsoft Teams message search expert. Convert the following natural language question "
                + "into a KQL (Keyword Query Language) query for Microsoft Graph Search API targeting chat and "
                + "channel messages.\n\n"
                + "KQL syntax rules:\n"
                + "- Use property:value for field-specific searches (no spaces around the colon)\n"
                + "- Common properties: from, subject, body, sent, received, kind\n"
                + "- Use quotes for multi-word values: subject:\"Project Update\"\n"
                + "- Boolean operators: AND, OR, NOT (must be uppercase)\n"
                + "- Wildcards: * for prefix matching (e.g. budget*)\n"
                + "- Date filters: sent>=2024-01-01, sent<=2024-06-30\n"
                + "- kind:im for chat and channel messages\n\n"
                + "Examples:\n"
                + "- \"Messages from John\" → from:John\n"
                + "- \"Recent messages about budget\" → subject:budget OR body:budget\n"
                + "- \"Messages from last week about the release\" → release sent>=today-7\n"
                + "- \"Messages to the sales team\" → sales\n"
                + "- \"Important messages\" → importance:high\n"
                + "- \"Messages about project plan from Jane\" → from:Jane AND (subject:\"project plan\" OR body:\"project plan\")\n\n"
                + "Output a JSON object with exactly these fields:\n"
                + "  \"query\": the KQL query string\n"
                + "  \"specific\": true if the question asks about a specific attribute of a named message "
                + "(e.g. who sent a specific message, what channel it was posted in), "
                + "false if it asks for a list of messages\n"
                + "  \"field\": (only when specific=true) the field name being asked about "
                + "(e.g. \"from\", \"createdDateTime\", \"summary\")\n\n"
                + "Output ONLY raw JSON. No markdown, no backticks, no explanation.\n\n"
                + "Question: " + question;
    }

    @Override
    public String answerExtractionPrompt(String question, String contentData) {
        return "Given the following Microsoft Teams message search results, answer the user's question concisely.\n\n"
                + "Question: " + question + "\n\n"
                + "Search results:\n" + contentData + "\n"
                + "Instructions:\n"
                + "- Answer directly and concisely\n"
                + "- If the data doesn't contain the answer, say so\n"
                + "- Do not invent or hallucinate data\n"
                + "- Reference specific message senders, channels/chats, or dates when relevant";
    }

    /**
     * Builds a prompt that instructs the AI to determine the exact Microsoft
     * Graph API call needed to answer the user's question — without any
     * hardcoded intent classification.
     *
     * <p>The AI returns {@code {method, path, body}} which the caller executes
     * generically against the Graph API, so any valid Graph endpoint works
     * without code changes.</p>
     *
     * @param question the user's natural language question
     * @return the Graph API planning prompt
     */
    public String graphApiPlanningPrompt(String question) {
        String messageSelect = "$select=id,createdDateTime,lastModifiedDateTime,from,body,"
                + "summary,webUrl,importance,channelIdentity,chatId";
        return "You are a Microsoft Graph API expert for Microsoft Teams.\n"
                + "Given the user's question, determine the exact Microsoft Graph API call needed.\n\n"
                + "If the question is NOT related to Teams messages, chats, channels, teams, "
                + "or the user's Microsoft 365 profile/account, "
                + "output: {\"method\":\"unsupported\",\"path\":\"\"}\n\n"
                + "Output ONLY a raw JSON object (no markdown, no backticks, no explanation):\n"
                + "  \"method\": \"GET\", \"POST\", or \"unsupported\"\n"
                + "  \"path\":   relative path starting with /me/ or /teams/ or /chats/ "
                + "(e.g. \"/me/joinedTeams\", \"/me/chats\")\n"
                + "  \"body\":   full JSON string for POST requests, or null for GET\n"
                + "  \"teamName\":    (optional) the team's display name, when the user names a team "
                + "instead of giving its id\n"
                + "  \"channelName\": (optional) the channel's display name, when the user names a "
                + "channel instead of giving its id\n\n"
                + "IMPORTANT — resolving names: when the user refers to a team or channel by NAME "
                + "rather than an id, keep the literal {team-id} and/or {channel-id} placeholders in "
                + "the path and put the names in teamName/channelName. They are resolved to ids "
                + "automatically. Do NOT invent ids.\n\n"
                + "Common Graph API patterns for Teams:\n"
                + "- List joined teams:      GET /me/joinedTeams\n"
                + "- List channels of a team: GET /teams/{team-id}/channels\n"
                + "- List channel messages:  GET /teams/{team-id}/channels/{channel-id}/messages"
                + "?$top=10&" + messageSelect + "\n"
                + "- List my chats:          GET /me/chats\n"
                + "- List chat messages:     GET /chats/{chat-id}/messages"
                + "?$top=10&" + messageSelect + "\n"
                + "- Search messages (KQL):  POST /search/query\n"
                + "    body: {\"requests\":[{\"entityTypes\":[\"chatMessage\"],"
                + "\"query\":{\"queryString\":\"KQL\"},\"from\":0,\"size\":10,"
                + "\"fields\":[\"id\",\"createdDateTime\",\"from\",\"body\",\"chatId\","
                + "\"channelIdentity\",\"webUrl\",\"summary\"]}]}\n"
                + "- Send a channel message: POST /teams/{team-id}/channels/{channel-id}/messages\n"
                + "    body: {\"body\":{\"content\":\"THE ACTUAL MESSAGE TEXT FROM THE USER\"}}\n"
                + "- Send a chat message:     POST /chats/{chat-id}/messages\n"
                + "    body: {\"body\":{\"content\":\"THE ACTUAL MESSAGE TEXT FROM THE USER\"}}\n"
                + "  For a send, put the user's real message in body.content. If the user asked to "
                + "send but gave NO message text, output {\"method\":\"unsupported\",\"path\":\"\"} "
                + "rather than inventing or leaving a placeholder.\n"
                + "- Fetch channel message by ID: GET /teams/{team-id}/channels/{channel-id}"
                + "/messages/{message-id}?" + messageSelect + "\n"
                + "- Fetch chat message by ID: GET /chats/{chat-id}/messages/{message-id}?" + messageSelect + "\n\n"
                + "Example — \"send 'hi' to channel SimRamJalTest\": "
                + "{\"method\":\"POST\",\"path\":\"/teams/{team-id}/channels/{channel-id}/messages\","
                + "\"channelName\":\"SimRamJalTest\",\"body\":\"{\\\"body\\\":{\\\"content\\\":\\\"hi\\\"}}\"}\n\n"
                + "Question: " + question;
    }

    // ── Dynamic field conversion ──────────────────────────────────────────

    /**
     * Converts a single Graph API JSON field value to a displayable string.
     *
     * <p>Handles primitives (with ISO date truncation), the {@code from} nested
     * object, and skips arrays or other complex types that have no meaningful
     * single-value representation.</p>
     */
    private static String toDisplayValue(String key, JsonElement val, JsonObject resource) {
        if (val == null || val.isJsonNull()) return null;
        if (val.isJsonPrimitive()) {
            String s = val.getAsString();
            // Truncate ISO-8601 timestamps to the date portion
            if ((key.contains("DateTime") || key.endsWith("Date")) && s.contains("T")) {
                return s.substring(0, s.indexOf('T'));
            }
            return s;
        }
        if (val.isJsonObject()) {
            // from.user → "Display Name"
            if ("from".equals(key)) return extractFrom(resource);
            // body → extract content text
            if ("body".equals(key)) {
                return getString(val.getAsJsonObject(), "content");
            }
        }
        return null; // skip other complex nested types (e.g. channelIdentity handled separately)
    }

    /**
     * Maps a Graph API field name to a FreeForm key (PascalCase).
     * Used by {@link #parseStandardResponse}.
     *
     * <p>Teams message fields are mapped to PascalCase FreeForm keys.
     * Unknown fields pass through unchanged and appear as extra dynamic columns.</p>
     */
    private static String toFreeFormKey(String graphKey) {
        switch (graphKey) {
            case "from":               return "From";
            case "createdDateTime":    return "CreatedDateTime";
            case "lastModifiedDateTime": return "LastModifiedDateTime";
            case "summary":            return "Summary";
            case "webUrl":             return "Link";
            case "id":                 return "MessageId";
            case "body":               return "Body";
            case "importance":        return "Importance";
            case "chatId":             return "ChatId";
            default:                   return graphKey;        // pass-through for unknown fields
        }
    }

    /**
     * Maps a Graph API field name to a camelCase column key.
     * Used by {@link #parseSearchResponse}.
     * Only renames the fields whose Graph name differs from the expected column name;
     * everything else passes through so new fields appear automatically.
     */
    private static String toSearchColumnKey(String graphKey) {
        switch (graphKey) {
            case "createdDateTime": return "createdDateTime";
            case "webUrl":          return "link"; // "link" drives the link column type
            default:                return graphKey; // pass-through
        }
    }

    // ── Private helpers ──────────────────────────────────

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    /**
     * Extracts the sender display string from the {@code from.user} structure.
     * Teams chat messages use: {@code from: { user: { displayName, id } } }.
     */
    private static String extractFrom(JsonObject resource) {
        if (!resource.has("from") || resource.get("from").isJsonNull()) return "";
        JsonObject from = resource.getAsJsonObject("from");
        if (!from.has("user") || from.get("user").isJsonNull()) return "";
        JsonObject user = from.getAsJsonObject("user");
        return getString(user, "displayName");
    }

    /**
     * Extracts a plain-text strip of {@code body.content} for use as a summary
     * fallback when the Graph API does not return a {@code summary} field.
     */
    private static String extractBodyContent(JsonObject resource) {
        if (!resource.has("body") || resource.get("body").isJsonNull()) return null;
        JsonObject body = resource.getAsJsonObject("body");
        String content = getString(body, "content");
        if (content.isEmpty()) return null;
        // Strip HTML tags for a plain-text summary
        String stripped = content.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        return stripped.isEmpty() ? null : stripped;
    }

    /**
     * Extracts the chat-or-channel identifier a Teams message belongs to:
     * {@code channelIdentity.channelId} for channel messages, or {@code chatId}
     * for 1:1/group chat messages.
     */
    private static String extractChatOrChannel(JsonObject resource) {
        if (resource.has("channelIdentity") && !resource.get("channelIdentity").isJsonNull()) {
            JsonObject channelIdentity = resource.getAsJsonObject("channelIdentity");
            String channelId = getString(channelIdentity, "channelId");
            if (!channelId.isEmpty()) return channelId;
        }
        String chatId = getString(resource, "chatId");
        if (!chatId.isEmpty()) return chatId;
        return null;
    }

}
