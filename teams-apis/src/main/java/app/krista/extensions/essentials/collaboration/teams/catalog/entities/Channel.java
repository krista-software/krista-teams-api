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
        ecosystemVersion = "635dfb5d-d9ed-4b25-bca3-ddc5f8549c99")
@Entity(name = "Channel", id = "localDomainEntity_478e917e-9c5f-44ac-b91d-9196ff3b4431", primaryKey = "ID", supportStore = false, options = {})
public class Channel {

    @Field.Text(name = "ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String iD;

    @ToString
    @Field.Text(name = "Name", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String name;

    @ToString
    @Field.Text(name = "Description", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String description;

    @ToString
    @Field(name = "Email", type = "Email", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String email;

    @Field.Text(name = "Team ID", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String teamID;

    @ToString
    @Field.Desc(name = "Members", type = "[ { Name: Text, Email: Text } ]", required = true)
    public List<Map<String, Object>> members;

}