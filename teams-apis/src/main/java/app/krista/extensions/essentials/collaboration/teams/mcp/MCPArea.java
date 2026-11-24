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
import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.CatalogRequest;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Field;

import javax.inject.Inject;

/**
 * Catalog entry point for the MCP "Ask Agent" natural language query.
 */
@Domain(id = "catEntryDomain_5fa2fc97-4b17-44cf-b98f-aa91a459a091",
        name = "Collaboration",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "a0d44dfa-e749-4bbf-9d86-db8b5991b153")
public class MCPArea {

    private final TeamsMcpQueryService mcpQueryService;

    @Inject
    public MCPArea(TeamsMcpQueryService mcpQueryService) {
        this.mcpQueryService = mcpQueryService;
    }

    @CatalogRequest(
            id = "localDomainRequest_c4f8a2e1-7b3d-4e9a-a5c6-2d8f91b04e7a",
            name = "Ask Agent",
            description = "Accepts natural language requests, enables the AI agent to reason and invoke available tools as needed, and returns the generated response.",
            area = "MCP",
            type = CatalogRequest.Type.QUERY_SYSTEM, tool = true)
    @Field.Desc(name = "Response", type = "[ FreeForm ]", required = false, description = "List of Teams messages matching the query")
    public ExtensionResponse askAgent(
            @Field(name = "Query", type = "Paragraph", required = true, description = "",
                    attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {}) String query) {
        return mcpQueryService.query(query);
    }
}
