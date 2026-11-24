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

import java.util.Map;
import java.util.List;

import app.krista.extension.impl.anno.*;

@Domain(id = "catEntryDomain_5fa2fc97-4b17-44cf-b98f-aa91a459a091",
        name = "Collaboration",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "02e109f6-f771-4dbd-a6c3-aa9acfefbab4")
@Entity(name = "Team", id = "localDomainEntity_087dc99b-6f97-427c-9768-f6d4506cb9c2", primaryKey = "ID", supportStore = false, options = {})
public class Team {

    @Field.Text(name = "ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String iD;

    @Searchable
    @ToString
    @Field.Text(name = "Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String name;

    @ToString
    @Field.Text(name = "Description", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String description;

    @ToString
    @Field.Desc(name = "Members", type = "[ { Name: Text, Email: Text } ]", required = false)
    public List<Map<String, Object>> members;

}