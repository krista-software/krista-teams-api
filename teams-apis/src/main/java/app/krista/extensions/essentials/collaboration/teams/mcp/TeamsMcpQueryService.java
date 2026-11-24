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

import app.krista.extension.executor.ExtensionResponse;
import app.krista.extension.executor.Invoker;
import app.krista.extension.executor.RemediationAction;
import app.krista.extension.executor.RemediationActions;
import app.krista.extension.executor.impl.InformAPersonAction;
import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.model.base.FreeForm;
import app.krista.sdk.msauth.extension.mcp.AbstractMcpQueryService;
import app.krista.sdk.msauth.extension.mcp.AnthropicApiClient;
import app.krista.sdk.msauth.extension.mcp.DefaultMsAuthBridge;
import app.krista.sdk.msauth.extension.mcp.GeminiApiClient;
import app.krista.sdk.msauth.extension.mcp.McpModelConfig;
import app.krista.sdk.msauth.extension.mcp.McpProductConfig;
import app.krista.sdk.msauth.extension.mcp.McpSearchContext;
import app.krista.sdk.msauth.extension.mcp.McpSessionKeys;
import app.krista.sdk.msauth.extension.mcp.OpenAiApiClient;
import app.krista.sdk.msauth.token.ITokenStore;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Teams-specific MCP query service — extends the SDK's
 * {@link AbstractMcpQueryService} with Teams chat/channel message search
 * via Microsoft Graph.
 *
 * <p>The {@link #query(String)} entry point asks the AI to plan the exact
 * Microsoft Graph API call needed to answer the question, then executes it
 * generically. Reads (GET) are unrestricted; the only writes permitted are
 * sending a channel or chat message — enforced by {@link #isAllowedGraphCall}.
 * Teams/channels referred to by name are resolved to ids automatically.</p>
 *
 * <p>Used by {@link MCPArea} to execute natural language queries via
 * the "Ask Agent" catalog request, and by
 * {@link TeamsMcpSetupResource#interceptNlQuery(String)} for the
 * AI Assistant tab.</p>
 */
@Service
public class TeamsMcpQueryService extends AbstractMcpQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(TeamsMcpQueryService.class);
    private static final Gson GSON = new Gson();
    private static final String GRAPH_SEARCH_QUERY_URL = "https://graph.microsoft.com/v1.0/search/query";

    private final ITokenStore tokenStore;
    private final Invoker invoker;
    private final TeamsApisAttributeStore attributeStore;

    @Inject
    public TeamsMcpQueryService(RefreshTokenStore refreshTokenStore,
                                Invoker invoker,
                                TeamsApisAttributeStore attributeStore) {
        super(adaptTokenStore(refreshTokenStore),
              invoker,
              new DefaultMsAuthBridge(
                      attributeStore.load(invoker.getInvokerId()),
                      adaptTokenStore(refreshTokenStore)));
        this.tokenStore = adaptTokenStore(refreshTokenStore);
        this.invoker = invoker;
        this.attributeStore = attributeStore;
    }

    /**
     * Loads current attributes and builds a fresh {@link DefaultMsAuthBridge} on
     * every call so that credential changes in the Authentication tab are
     * picked up without restarting the server.
     */
    private DefaultMsAuthBridge currentAuthBridge() {
        TeamsApisAttributes attrs = attributeStore.load(invoker.getInvokerId());
        return new DefaultMsAuthBridge(attrs, tokenStore);
    }

    /**
     * Returns the current {@link TeamsApisAttributes}, reloaded from the store on
     * every call so that credential changes are always reflected.
     */
    private TeamsApisAttributes currentAttributes() {
        return attributeStore.load(invoker.getInvokerId());
    }

    @Override
    protected McpProductConfig getProductConfig() {
        return new TeamsMcpProductConfig();
    }

    @Override
    protected String getJaxrsId() {
        return "teams";
    }

    /**
     * Single entry point for both catalog requests and the NQL tab.
     *
     * <p>Asks the AI to plan the exact Microsoft Graph API call ({@code method},
     * {@code path}, {@code body}) needed to answer the user's question, then
     * executes it generically via {@link #executeGraphCall}. No hardcoded
     * intents — any valid Graph endpoint works without code changes.</p>
     *
     * <p>Falls back to the parent KQL search pipeline if the AI returns an
     * unparseable response.</p>
     */
    @Override
    public ExtensionResponse query(String question) {
        long startTime = System.currentTimeMillis();
        String id = invoker.getInvokerId();
        LOG.info("[MCP-TM] query START — invokerId: {}, question: '{}'", id, question);

        try {
            // Resolve AI provider + API key
            String provider = tokenStore.get(McpSessionKeys.aiProvider(id));
            String aiKey = blank(provider) ? null : tokenStore.get(McpSessionKeys.aiKey(id, provider));

            if (blank(aiKey)) {
                aiKey = tokenStore.get(McpSessionKeys.claudeApiKey(id));
                if (!blank(aiKey)) {
                    provider = "anthropic";
                }
            }

            if (blank(aiKey)) {
                LOG.warn("[MCP-TM] query — No AI key configured");
                return failure("AI API key is not configured. "
                        + "Please set it in the AI Assistant tab → AI Settings.");
            }

            // Reload credentials on every call so changes in the Authentication tab take effect
            DefaultMsAuthBridge authBridge = currentAuthBridge();
            if (!authBridge.isAuthenticated()) {
                return failure("Not connected. Configure credentials in the Authentication tab.");
            }

            // Ask AI to plan the Graph API call
            TeamsMcpProductConfig config = (TeamsMcpProductConfig) getProductConfig();
            String planPrompt = config.graphApiPlanningPrompt(question);
            LOG.info("[MCP-TM] query — planning Graph API call via AI...");
            String raw = callAi(aiKey, provider, planPrompt);
            LOG.info("[MCP-TM] query — AI plan: '{}'", raw);

            if (raw == null || raw.isBlank()) {
                LOG.warn("[MCP-TM] query — AI returned blank response");
                return failure("Could not determine a valid Graph API call from the AI response. "
                        + "Please try rephrasing your question.");
            }

            if (raw.contains("{")) {
                raw = raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1);
            }

            JsonObject plan = GSON.fromJson(raw, JsonObject.class);
            String method = getJsonString(plan, "method");
            String path   = getJsonString(plan, "path");
            String body   = plan != null && plan.has("body") && !plan.get("body").isJsonNull()
                    ? (plan.get("body").isJsonPrimitive()
                            ? plan.get("body").getAsString()
                            : plan.get("body").toString())
                    : null;

            if (blank(method) || blank(path)) {
                LOG.warn("[MCP-TM] query — AI returned invalid plan (missing method or path)");
                return failure("Could not parse a valid Graph API call from the AI response. "
                        + "Please try rephrasing your question.");
            }

            if ("unsupported".equalsIgnoreCase(method)) {
                LOG.info("[MCP-TM] query — question not related to Teams, returning guidance");
                FreeForm ff = new FreeForm();
                ff.put("Answer", " Text ",
                        "I can only answer questions related to Teams messages, chats, channels, "
                        + "teams, or your Microsoft 365 profile. "
                        + "Please ask something like: 'Show messages about the quarterly report', "
                        + "'Find messages from Priya last week', or 'List my joined teams'.");
                return success(Map.of("Response", List.of(ff)));
            }

            LOG.info("[MCP-TM] query — executing {} {}", method, path);

            String teamName = getJsonString(plan, "teamName");
            String channelName = getJsonString(plan, "channelName");

            // A send needs real message text. If the user didn't provide any, the AI copies the
            // "<message text>" example placeholder — don't post that. Fail fast (before the slower
            // name resolution) with a helpful prompt.
            if ("POST".equalsIgnoreCase(method) && stripQuery(path).endsWith("/messages")) {
                String content = extractSendContent(body);
                if (content == null || content.isBlank() || content.contains("<message text>")) {
                    return failure("Please include the message to send — for example: "
                            + "\"send 'Standup at 10am' to channel SimRamJalTest\".");
                }
            }

            McpSearchContext ctx = authBridge.buildSearchContext(config);
            String authHeader = ctx.authHeader();
            String resolvedPathStr = resolvedPath(path);
            // Resolve {team-id}/{channel-id} placeholders from names the user gave (e.g.
            // "channel = SimRamJalTest") into the GUIDs Graph requires.
            resolvedPathStr = resolveNamedPlaceholders(resolvedPathStr, teamName, channelName, authHeader);
            String fullUrl = "https://graph.microsoft.com/v1.0" + resolvedPathStr;
            String generatedPlan = method.toUpperCase() + " " + resolvedPathStr;

            ExtensionResponse result = executeGraphCall(method, fullUrl, body, authHeader, generatedPlan);
            LOG.info("[MCP-TM] query END — elapsed: {}ms",
                    System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            LOG.error("[MCP-TM] query FAILED — elapsed: {}ms",
                    System.currentTimeMillis() - startTime, e);
            return failure("Query failed: " + e.getMessage());
        }
    }

    /**
     * Resolves {@code {team-id}} / {@code {channel-id}} placeholders in a planned Graph path from
     * the team/channel display names the user supplied, so a question like "channel = SimRamJalTest"
     * works without the user knowing the GUIDs. Lists joined teams (and, for a channel, that team's
     * channels) and matches by display name. Throws {@link IllegalStateException} with an
     * actionable message when the named team/channel can't be found.
     */
    private String resolveNamedPlaceholders(String path, String teamName, String channelName, String authHeader) {
        boolean needsTeam = path.contains("{team-id}");
        boolean needsChannel = path.contains("{channel-id}");
        if (!needsTeam && !needsChannel) {
            return path;
        }
        JsonObject teams = graphGetJson("https://graph.microsoft.com/v1.0/me/joinedTeams", authHeader);
        JsonArray teamArr = teams != null && teams.has("value") && teams.get("value").isJsonArray()
                ? teams.getAsJsonArray("value") : new JsonArray();

        String teamId = null;
        String channelId = null;
        boolean teamNamed = teamName != null && !teamName.isBlank();
        for (JsonElement el : teamArr) {
            if (!el.isJsonObject()) continue;
            JsonObject team = el.getAsJsonObject();
            String tName = getStr(team, "displayName");
            String tId = getStr(team, "id");
            if (teamNamed && !teamName.equalsIgnoreCase(tName)) {
                continue;
            }
            if (needsChannel) {
                JsonObject channels = graphGetJson(
                        "https://graph.microsoft.com/v1.0/teams/" + tId + "/channels", authHeader);
                String chId = findIdByDisplayName(channels, channelName);
                if (chId != null) {
                    teamId = tId;
                    channelId = chId;
                    break;
                }
                if (teamNamed) {
                    break; // team was named explicitly; don't scan other teams
                }
            } else {
                teamId = tId;
                break;
            }
        }

        if (needsTeam && teamId == null) {
            throw new IllegalStateException("Could not find a team"
                    + (teamNamed ? " named '" + teamName + "'"
                        : channelName != null && !channelName.isBlank()
                            ? " containing a channel named '" + channelName + "'" : "")
                    + " among your joined teams.");
        }
        if (needsChannel && channelId == null) {
            throw new IllegalStateException("Could not find a channel named '"
                    + (channelName == null ? "" : channelName) + "'"
                    + (teamNamed ? " in team '" + teamName + "'" : "") + ".");
        }
        String resolved = path;
        if (teamId != null) resolved = resolved.replace("{team-id}", teamId);
        if (channelId != null) resolved = resolved.replace("{channel-id}", channelId);
        return resolved;
    }

    private static String stripQuery(String path) {
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    /**
     * Extracts {@code body.content} from a send-message request body JSON, or {@code null} if the
     * body is missing, unparseable, or has no content.
     */
    static String extractSendContent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonObject b = GSON.fromJson(body, JsonObject.class);
            if (b != null && b.has("body") && b.get("body").isJsonObject()) {
                JsonObject inner = b.getAsJsonObject("body");
                if (inner.has("content") && !inner.get("content").isJsonNull()) {
                    return inner.get("content").getAsString();
                }
            }
        } catch (RuntimeException ignore) {
            // unparseable body → treat as no content
        }
        return null;
    }

    /** GET a Graph endpoint and parse the JSON body; throws on a non-2xx response. */
    private JsonObject graphGetJson(String url, String authHeader) {
        Client client = ClientBuilder.newClient();
        try {
            javax.ws.rs.core.Response resp = client.target(url.replace(" ", "%20"))
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", authHeader)
                    .get();
            String bodyStr = resp.readEntity(String.class);
            if (resp.getStatus() / 100 != 2) {
                throw new IllegalStateException("Graph lookup failed (HTTP " + resp.getStatus() + "): " + url);
            }
            return GSON.fromJson(bodyStr, JsonObject.class);
        } finally {
            client.close();
        }
    }

    /**
     * Returns the {@code id} of the first element in a Graph list response ({@code value[]}) whose
     * {@code displayName} matches {@code name} case-insensitively, or {@code null} if none match
     * (including a blank name or an empty/missing list).
     */
    static String findIdByDisplayName(JsonObject listResponse, String name) {
        if (name == null || name.isBlank() || listResponse == null
                || !listResponse.has("value") || !listResponse.get("value").isJsonArray()) {
            return null;
        }
        for (JsonElement el : listResponse.getAsJsonArray("value")) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (name.equalsIgnoreCase(getStr(obj, "displayName"))) {
                return getStr(obj, "id");
            }
        }
        return null;
    }

    /**
     * Replaces {@code /me/} with {@code /users/{email}/} when private auth is
     * configured, so the same AI-generated paths work for both auth modes.
     */
    private String resolvedPath(String path) {
        TeamsApisAttributes attrs = currentAttributes();
        if (attrs != null && attrs.isPrivateAuth()) {
            String email = attrs.getEmail();
            return path.replaceFirst("^/me(/|$)", "/users/" + email + "$1");
        }
        return path;
    }

    // Send targets (Teams MCP now supports posting messages). Anchored patterns so ONLY a
    // top-level channel/chat message send is permitted — never replies, members, or any other
    // sub-resource, and never a different method.
    private static final java.util.regex.Pattern SEND_CHANNEL_MESSAGE = java.util.regex.Pattern.compile(
            "^https://graph\\.microsoft\\.com/v1\\.0/teams/[^/]+/channels/[^/]+/messages$");
    private static final java.util.regex.Pattern SEND_CHAT_MESSAGE = java.util.regex.Pattern.compile(
            "^https://graph\\.microsoft\\.com/v1\\.0/chats/[^/]+/messages$");

    /**
     * Restricts which planned Graph API calls are allowed to execute.
     *
     * <p>The LLM plans arbitrary Graph API calls from natural language, so the executable set is
     * constrained to a strict allowlist:
     * <ul>
     *   <li>GET — always allowed (read-only).</li>
     *   <li>POST to the Graph Search endpoint ({@code /search/query}) — a read-only search call.</li>
     *   <li>POST to send a channel message ({@code /teams/{id}/channels/{id}/messages}) or a chat
     *       message ({@code /chats/{id}/messages}) — the only write operations Teams MCP allows.</li>
     * </ul>
     * Every other method/URL combination (any other POST target, replies or sub-resources, and any
     * other HTTP method such as DELETE/PATCH/PUT) is rejected before any HTTP call is made.</p>
     */
    static boolean isAllowedGraphCall(String method, String url) {
        if (method == null || url == null) {
            return false;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method)) {
            return url.equals(GRAPH_SEARCH_QUERY_URL)
                    || url.startsWith(GRAPH_SEARCH_QUERY_URL)
                    || SEND_CHANNEL_MESSAGE.matcher(url).matches()
                    || SEND_CHAT_MESSAGE.matcher(url).matches();
        }
        return false;
    }

    /**
     * Executes any Microsoft Graph API call and converts the response into
     * a list of FreeForm rows for the NQL tab.
     *
     * <p>Handles GET and POST. HTTP 202/204 (no body) is treated as a
     * successful action. HTTP 200/201 responses are parsed by
     * {@link #parseGenericResponse}.</p>
     */
    private ExtensionResponse executeGraphCall(String method, String fullUrl,
                                               String body, String authHeader,
                                               String generatedPlan) {
        if (!isAllowedGraphCall(method, fullUrl)) {
            LOG.warn("[MCP-TM] executeGraphCall — rejected disallowed call: {} {}", method, fullUrl);
            return failure("This request cannot be executed. Allowed: any GET (read), and POST only "
                    + "to Microsoft Graph Search or to send a channel/chat message. Requested: "
                    + method + " " + fullUrl);
        }

        // Encode spaces so JAX-RS client.target() can construct a valid URI.
        // OData $filter values like "isRead eq true" contain spaces that are
        // valid OData but illegal in an unencoded URI string.
        String safeUrl = fullUrl.replace(" ", "%20");
        Client client = ClientBuilder.newClient();
        try {
            javax.ws.rs.core.Response httpResp;
            if ("POST".equalsIgnoreCase(method)) {
                httpResp = client.target(safeUrl)
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .post(Entity.entity(body != null ? body : "{}",
                                MediaType.APPLICATION_JSON));
            } else {
                httpResp = client.target(safeUrl)
                        .request(MediaType.APPLICATION_JSON)
                        .header("Authorization", authHeader)
                        .get();
            }

            int status = httpResp.getStatus();
            LOG.info("[MCP-TM] executeGraphCall — {} {} → HTTP {}",
                    method, fullUrl, status);

            if (status == 202 || status == 204) {
                // No-body success
                FreeForm ff = new FreeForm();
                ff.put("Answer", " Text ", "Action completed successfully.");
                Map<String, Object> respMap = new HashMap<>();
                respMap.put("Response", List.of(ff));
                if (generatedPlan != null) respMap.put("generatedPlan", generatedPlan);
                return success(respMap);
            }

            if (status == 200 || status == 201) {
                // 200 = GET responses; 201 = created resource
                String responseBody = httpResp.readEntity(String.class);
                List<FreeForm> rows = parseGenericResponse(responseBody);
                LOG.info("[MCP-TM] executeGraphCall — parsed {} row(s)", rows.size());
                Map<String, Object> respMap = new HashMap<>();
                respMap.put("Response", rows);
                if (generatedPlan != null) respMap.put("generatedPlan", generatedPlan);
                return success(respMap);
            }

            String errorBody = httpResp.readEntity(String.class);
            LOG.error("[MCP-TM] executeGraphCall FAILED — HTTP {}: {}", status, errorBody);
            return failure("Graph API call failed (HTTP " + status + "): " + errorBody);

        } catch (Exception e) {
            LOG.error("[MCP-TM] executeGraphCall FAILED — {}", e.getMessage(), e);
            return failure("Graph API call failed: " + e.getMessage());
        } finally {
            client.close();
        }
    }

    /**
     * Converts any Graph API JSON response into FreeForm rows.
     *
     * <p>Handles three shapes:
     * <ul>
     *   <li>Search response — {@code value[].hitsContainers[].hits[].resource}</li>
     *   <li>List response  — flat {@code value[]} array (messages, teams, channels, chats…)</li>
     *   <li>Single object  — no {@code value} key (e.g. GET /chats/{chatId}/messages/{id})</li>
     * </ul>
     */
    private static List<FreeForm> parseGenericResponse(String json) {
        List<FreeForm> rows = new ArrayList<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        if (!root.has("value") || !root.get("value").isJsonArray()) {
            // Single-object response
            rows.add(graphObjectToFreeForm(root));
            return rows;
        }

        JsonArray value = root.getAsJsonArray("value");
        if (value.size() == 0) return rows;

        // Detect search response by presence of "hitsContainers" in first element
        JsonElement first = value.get(0);
        if (first.isJsonObject() && first.getAsJsonObject().has("hitsContainers")) {
            for (JsonElement valEl : value) {
                JsonArray hitsContainers = valEl.getAsJsonObject().has("hitsContainers")
                        ? valEl.getAsJsonObject().getAsJsonArray("hitsContainers")
                        : new JsonArray();
                for (JsonElement hcEl : hitsContainers) {
                    JsonArray hits = hcEl.getAsJsonObject().has("hits")
                            ? hcEl.getAsJsonObject().getAsJsonArray("hits")
                            : new JsonArray();
                    for (JsonElement hitEl : hits) {
                        JsonObject hit = hitEl.getAsJsonObject();
                        JsonObject resource = hit.has("resource")
                                ? hit.getAsJsonObject("resource")
                                : new JsonObject();
                        FreeForm ff = graphObjectToFreeForm(resource);
                        // Graph Search API puts the message ID in "hitId" on the hit
                        // object, not inside resource — inject it into the FreeForm row.
                        if (hit.has("hitId") && !hit.get("hitId").isJsonNull()) {
                            String hitId = hit.get("hitId").getAsString();
                            if (!hitId.isEmpty()) {
                                ff.put("Id", " Text ", hitId);
                            }
                        }
                        rows.add(ff);
                    }
                }
            }
            return rows;
        }

        // Flat list response (messages, teams, channels, chats…)
        for (JsonElement el : value) {
            if (el.isJsonObject()) {
                rows.add(graphObjectToFreeForm(el.getAsJsonObject()));
            }
        }
        return rows;
    }

    /**
     * Converts a single Graph JSON object into a FreeForm row by iterating
     * its fields and mapping known field names to display-friendly FreeForm keys.
     */
    private static FreeForm graphObjectToFreeForm(JsonObject obj) {
        FreeForm ff = new FreeForm();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String graphKey = entry.getKey();
            String strVal = toDisplayString(graphKey, entry.getValue(), obj);
            if (strVal != null && !strVal.isEmpty()) {
                ff.put(toFreeFormKey(graphKey), " Text ", strVal);
            }
        }
        return ff;
    }

    /**
     * Converts a Graph JSON field value to a display string.
     * Handles primitives, date truncation, nested {@code from.user},
     * {@code body.content}; skips other complex types.
     */
    private static String toDisplayString(String key, JsonElement val, JsonObject parent) {
        if (val == null || val.isJsonNull()) return null;
        if (val.isJsonPrimitive()) {
            String s = val.getAsString();
            // Truncate ISO-8601 timestamps to date portion
            if ((key.contains("DateTime") || key.contains("date")) && s.contains("T")) {
                return s.substring(0, s.indexOf('T'));
            }
            return s;
        }
        if (val.isJsonObject()) {
            // "from" → "Display Name"
            if ("from".equals(key)) return extractSender(parent);
            // "body" → content text
            if ("body".equals(key)) return getStr(val.getAsJsonObject(), "content");
        }
        return null; // skip other complex types
    }

    /**
     * Maps a Graph API field name to a FreeForm key.
     *
     * <p>The FreeForm key is then lowercased by {@code toColumnKey()} in
     * {@link TeamsMcpSetupResource} to produce the final column key that
     * must match an entry in {@link TeamsMcpProductConfig#resultColumns()}.
     *
     * <p>Teams message fields are mapped to PascalCase FreeForm keys.
     * Team/channel fields use natural names so they appear as their own columns.
     * Unknown fields pass through unchanged and appear as extra dynamic columns.</p>
     */
    private static String toFreeFormKey(String graphKey) {
        switch (graphKey) {
            // ── message fields ───────────────────────────────────
            case "from":                 return "From";
            case "createdDateTime":      return "CreatedDateTime";
            case "lastModifiedDateTime": return "LastModifiedDateTime";
            case "summary":              return "Summary";
            case "webUrl":               return "Link";
            case "id":                   return "Id";
            case "body":                 return "Body";
            case "importance":           return "Importance";
            case "chatId":               return "ChatId";
            case "channelIdentity":      return "ChannelIdentity";
            // ── team / channel fields ──
            case "displayName":          return "Name";
            case "description":          return "Description";
            default:                     return graphKey;        // pass-through for unknown fields
        }
    }

    // ── Shared field helpers ─────────────────────────────────────────────

    private static String getStr(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private static String extractSender(JsonObject resource) {
        if (!resource.has("from") || resource.get("from").isJsonNull()) return "";
        JsonObject from = resource.getAsJsonObject("from");
        if (!from.has("user") || from.get("user").isJsonNull()) return "";
        JsonObject user = from.getAsJsonObject("user");
        return getStr(user, "displayName");
    }

    // ── AI dispatch ──────────────────────────────────────────────────────

    private static String callAi(String apiKey, String provider, String prompt) throws Exception {
        if ("openai".equals(provider)) {
            return OpenAiApiClient.call(apiKey, prompt, McpModelConfig.openAiModel(), 0);
        } else if ("gemini".equals(provider)) {
            return GeminiApiClient.call(apiKey, prompt, McpModelConfig.geminiModel(), 0);
        } else {
            return AnthropicApiClient.call(apiKey, prompt, McpModelConfig.anthropicModel(), 0);
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────

    private static ExtensionResponse success(Map<String, Object> values) {
        return new ExtensionResponse(ExtensionResponse.Result.SUCCESS, values, null, null, null);
    }

    private static ExtensionResponse failure(String message) {
        ExtensionResponse.Error error = new ExtensionResponse.Error(
                message, System.currentTimeMillis(),
                ExtensionResponse.Error.ExceptionType.LOGIC_ERROR, "");
        RemediationActions remediationActions = new RemediationActions(
                List.of(InformAPersonAction.create(message,
                        RemediationAction.RecipientType.ACTIVE_USER, List.of())), null);
        return new ExtensionResponse(ExtensionResponse.Result.FAILURE, null, error, remediationActions, Map.of());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static String getJsonString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    /**
     * Adapts the Teams {@link RefreshTokenStore} (put/get/remove) to
     * the SDK's {@link ITokenStore} interface (save/get/delete).
     */
    private static ITokenStore adaptTokenStore(RefreshTokenStore store) {
        return new ITokenStore() {
            @Override
            public void save(String key, String value) {
                store.put(key, value);
            }

            @Override
            public String get(String key) {
                return store.get(key);
            }

            @Override
            public void delete(String key) {
                store.remove(key);
            }
        };
    }
}
