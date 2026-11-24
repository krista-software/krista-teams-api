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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers {@link MCPArea#askAgent}: it is a thin catalog-request wrapper that delegates the raw
 * query string to {@link TeamsMcpQueryService#query(String)} and returns its result unchanged.
 */
class MCPAreaTest {

    @Test
    void askAgentDelegatesToQueryServiceAndReturnsItsResponse() {
        TeamsMcpQueryService queryService = Mockito.mock(TeamsMcpQueryService.class);
        ExtensionResponse expected = new ExtensionResponse(
                ExtensionResponse.Result.SUCCESS, Map.of("Response", Map.of()), null, null, null);
        when(queryService.query("what did priya say?")).thenReturn(expected);

        MCPArea area = new MCPArea(queryService);
        ExtensionResponse actual = area.askAgent("what did priya say?");

        assertSame(expected, actual);
        verify(queryService).query("what did priya say?");
    }
}
