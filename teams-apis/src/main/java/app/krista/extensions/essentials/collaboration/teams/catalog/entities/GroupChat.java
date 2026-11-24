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
        ecosystemVersion = "57effcfd-5aa2-44f0-8bc7-5c2585732b64")
@Entity(name = "Group Chat", id = "localDomainEntity_13034efa-26ab-463a-a79c-79958b3d4c5c", primaryKey = "Group Id", supportStore = false, options = {})
public class GroupChat {

    @Searchable
    @ToString
    @Field.Text(name = "Group Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String groupId;

    @Searchable
    @ToString
    @Field.Text(name = "Topic", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String topic;

    @Searchable
    @ToString
    @Field.Text(name = "Type", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String type;

    @Searchable
    @ToString
    @Field.Date(name = "Created Date Time", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long createdDateTime;

    @ToString
    @Field.Desc(name = "Members", type = "[ { Name: Text, Email: Text } ]", required = true)
    public List<Map<String, Object>> members;

}