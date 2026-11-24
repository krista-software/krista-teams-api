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

package app.krista.extensions.essentials.collaboration.teams.catalog.entities.stores;

import app.krista.extension.executor.SearchCondition;
import app.krista.extension.executor.SearchQuery;
import app.krista.extension.util.EntityStore;
import app.krista.extensions.essentials.collaboration.teams.catalog.entities.Team;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import app.krista.extensions.essentials.collaboration.teams.service.TeamService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeamStore implements EntityStore<Team> {

    private final TeamService teamService;

    @Inject
    public TeamStore(TeamService teamService) {
        this.teamService = teamService;
    }

    @Override
    public Team create(Map<String, Object> map) throws IOException {
        return null;
    }

    @Override
    public Team get(String teamId) throws IOException {
        return teamService.getTeamById(teamId);
    }

    @Override
    public Team update(Team team) throws IOException {
        return null;
    }

    @Override
    public void delete(String s) throws IOException {

    }

    @Override
    public boolean contains(String s) throws IOException {
        return false;
    }

    @Override
    @Deprecated
    public List<Team> search(List<SearchCondition> list, long l, int i) throws IOException {
        return Collections.emptyList();
    }

    @Override
    public List<Team> search(SearchQuery searchQuery, long pageIndex, int pageSize) throws IOException {
        List<SearchCondition> searchConditions = searchQuery.getSearchConditions();
        if (searchConditions == null || searchConditions.isEmpty()) {
            throw new IllegalArgumentException("Failed to get teams. " + Constants.CONDITIONS_NOT_FOUND);
        }

        String name = getSearchConditionValue(searchConditions, "Name");

        List<Team> joinedTeams = teamService.getJoinedTeams(name);
        return joinedTeams.stream().skip(Math.max((pageIndex - 1) * pageSize, 0)).limit(pageSize)
                .collect(Collectors.toList());
    }

    public static String getSearchConditionValue(List<SearchCondition> conditions, String fieldName) {
        return conditions.stream()
                .filter(condition -> fieldName.equalsIgnoreCase(condition.getFieldName()))
                .map(SearchCondition::getOperand)
                .map(operand -> Objects.toString(operand, "").trim())
                .findFirst()
                .orElse(null);
    }

    @Override
    public long count(List<SearchCondition> list) throws IOException {
        return 0;
    }

    @Override
    public List<String> lookup(Map<String, Object> map, List<String> list) throws IOException {
        return null;
    }
}
