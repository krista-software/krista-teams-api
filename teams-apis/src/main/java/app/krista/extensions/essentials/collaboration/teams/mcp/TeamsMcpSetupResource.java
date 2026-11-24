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
import app.krista.extensions.essentials.collaboration.teams.impl.stores.TeamsApisAttributeStore;
import app.krista.extensions.essentials.collaboration.teams.impl.stores.RefreshTokenStore;
import app.krista.model.base.FreeForm;
import app.krista.sdk.msauth.extension.mcp.AbstractMcpSetupResource;
import app.krista.sdk.msauth.extension.mcp.DefaultMsAuthBridge;
import app.krista.sdk.msauth.extension.mcp.McpProductConfig;
import app.krista.sdk.msauth.token.ITokenStore;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete MCP setup resource for the Teams extension.
 *
 * <p>Registers as a JAX-RS resource at {@code /teams/mcp-setup} and
 * inherits all endpoints from {@link AbstractMcpSetupResource}.
 * The Authentication tab's credentials are bridged via
 * {@link DefaultMsAuthBridge}.</p>
 *
 * <p>Overrides {@link #interceptNlQuery(String)} to delegate NL queries to
 * {@link TeamsMcpQueryService#query(String)}, which plans and executes the
 * appropriate Microsoft Graph API call over Teams messages.</p>
 */
@Path("/teams/mcp-setup")
public class TeamsMcpSetupResource extends AbstractMcpSetupResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamsMcpSetupResource.class);
    private static final Gson GSON = new Gson();

    private final TeamsMcpQueryService queryService;

    @Inject
    public TeamsMcpSetupResource(RefreshTokenStore refreshTokenStore,
                                  Invoker invoker,
                                  TeamsApisAttributeStore attributeStore,
                                  TeamsMcpQueryService queryService) {
        super(adaptTokenStore(refreshTokenStore),
              invoker,
              new DefaultMsAuthBridge(
                      attributeStore.load(invoker.getInvokerId()),
                      adaptTokenStore(refreshTokenStore)));
        this.queryService = queryService;
    }

    /**
     * Serves the AI Assistant tab HTML. Overrides the SDK's {@code getTab()} to patch a
     * rendering bug in the shipped {@code mcp-setup/index.html}: its {@code renderResults}
     * draws table columns solely from {@code productConfig.resultColumns} (a fixed
     * message-only schema), so queries returning teams/channels/chats — whose row keys don't
     * match those columns — render every cell blank. The backend already returns a correct
     * dynamic {@code columns} array; the patch makes the tab prefer it.
     */
    @GET
    @Produces("text/html")
    public Response getTab() {
        String html = patchedMcpHtml();
        if (html == null) {
            return Response.status(404).entity("AI Assistant tab not found.").build();
        }
        return Response.ok(html).type("text/html").build();
    }

    static String patchedMcpHtml() {
        try (InputStream in = TeamsMcpSetupResource.class.getClassLoader()
                .getResourceAsStream("mcp-setup/index.html")) {
            if (in == null) {
                return null;
            }
            // Prefer the backend's dynamic columns (present for every query shape) over the
            // fixed message-only productConfig.resultColumns, which leaves teams/channels/chats
            // rows rendering blank.
            return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace(
                            "var cols = (productConfig && productConfig.resultColumns) ? productConfig.resultColumns : [];",
                            "var cols = (d.columns && d.columns.length) ? d.columns "
                                    + ": (productConfig && productConfig.resultColumns) ? productConfig.resultColumns : [];");
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
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
     * Intercepts NL queries and delegates to
     * {@link TeamsMcpQueryService#query(String)}, which asks the AI
     * to plan the appropriate Microsoft Graph API call over Teams messages
     * and executes it generically — no hardcoded intents.
     */
    @Override
    protected Response interceptNlQuery(String question) {
        LOG.debug("[MCP-TM-NLQ] interceptNlQuery — question: '{}'", question);

        try {
            ExtensionResponse extResponse = queryService.query(question);
            LOG.debug("[MCP-TM-NLQ] queryService.query() returned — result: {}", extResponse.getResult());

            if (extResponse.getResult() == ExtensionResponse.Result.FAILURE) {
                String errorMsg = "Query failed.";
                if (extResponse.getError() != null) {
                    errorMsg = String.valueOf(extResponse.getError());
                }
                LOG.debug("[MCP-TM-NLQ] FAILURE — {}", errorMsg);
                return Response.status(400)
                        .entity(GSON.toJson(Map.of("error", errorMsg)))
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> result = convertToNlqResponse(extResponse, question);
            LOG.debug("[MCP-TM-NLQ] returning result — total: {}, shown: {}", result.get("total"), result.get("shown"));
            return Response.ok(GSON.toJson(result)).type(MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            LOG.error("[MCP-TM-NLQ] interceptNlQuery FAILED — {}", e.getMessage(), e);
            return Response.status(400)
                    .entity(GSON.toJson(Map.of("error", "Query failed: " + e.getMessage())))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Converts an {@link ExtensionResponse} with FreeForm list into the
     * JSON structure the NQL tab UI expects.
     *
     * <p>Columns are derived dynamically from the keys present in the response
     * rows at runtime — no hardcoded column list. Whatever fields the Graph API
     * returns (messages, teams, channels, chats, etc.) will automatically
     * produce the correct columns without any code changes.</p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToNlqResponse(ExtensionResponse extResponse, String question) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> rows = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>(); // preserves insertion order

        Object responseValue = extResponse.getResponseValue();
        if (responseValue instanceof Map) {
            Map<String, Object> payload = (Map<String, Object>) responseValue;
            Object responseList = payload.get("Response");
            if (responseList instanceof List) {
                List<?> freeFormRows = (List<?>) responseList;
                for (Object row : freeFormRows) {
                    if (row instanceof FreeForm) {
                        FreeForm ff = (FreeForm) row;
                        Map<String, String> item = new LinkedHashMap<>();
                        for (Object key : ff.keySet()) {
                            String value = extractFreeFormValue(ff, key);
                            if (value != null) {
                                String colKey = toColumnKey(String.valueOf(key));
                                item.put(colKey, value);
                                seenKeys.add(colKey);
                            }
                        }
                        rows.add(item);
                    }
                }
            }
        }

        // Build column definitions at runtime from the keys that actually appeared
        List<Map<String, String>> columns = new ArrayList<>();
        for (String key : seenKeys) {
            Map<String, String> col = new LinkedHashMap<>();
            col.put("key", key);
            col.put("label", toColumnLabel(key));
            col.put("type", isLinkColumn(key) ? "link" : "text");
            columns.add(col);
        }

        LOG.debug("[MCP-TM-NLQ] convertToNlqResponse — rows: {}, columns: {}", rows.size(), seenKeys);

        result.put("total", rows.size());
        result.put("shown", rows.size());
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("generatedQuery", question);
        result.put("specific", false);
        return result;
    }

    /** Extracts the string value from a FreeForm entry (handles [type, value] lists). */
    private static String extractFreeFormValue(FreeForm ff, Object key) {
        Object val = ff.get(key);
        if (val instanceof List) {
            List<?> valList = (List<?>) val;
            if (valList.size() >= 2 && valList.get(1) != null) {
                String s = String.valueOf(valList.get(1));
                return s.isEmpty() ? null : s;
            }
        } else if (val != null) {
            String s = String.valueOf(val);
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    /**
     * Normalises a FreeForm key (PascalCase) to a camelCase column key.
     * No hardcoded names — works for any field the Graph API returns.
     *
     * <p>Examples: {@code "Subject"} → {@code "subject"},
     * {@code "MessageId"} → {@code "messageId"},
     * {@code "ChatOrChannel"} → {@code "chatOrChannel"}.</p>
     */
    private static String toColumnKey(String freeFormKey) {
        if (freeFormKey == null || freeFormKey.isEmpty()) return freeFormKey;
        return Character.toLowerCase(freeFormKey.charAt(0)) + freeFormKey.substring(1);
    }

    /**
     * Converts a camelCase column key to a human-readable label.
     *
     * <p>Examples: {@code "messageId"} → {@code "Message Id"},
     * {@code "chatOrChannel"} → {@code "Chat Or Channel"}.</p>
     */
    private static String toColumnLabel(String columnKey) {
        if (columnKey == null || columnKey.isEmpty()) return columnKey;
        String spaced = columnKey.replaceAll("([a-z])([A-Z])", "$1 $2");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /**
     * Returns {@code true} for column keys whose values are URLs and should
     * be rendered as clickable links in the UI.
     */
    private static boolean isLinkColumn(String key) {
        return "subject".equals(key) || "link".equals(key) || "url".equals(key);
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
