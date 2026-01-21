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

import app.krista.extension.impl.anno.*;

@Domain(id = "catEntryDomain_5fa2fc97-4b17-44cf-b98f-aa91a459a091",
        name = "Collaboration",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "889b4388-b298-48ca-88e7-2f588b31f015")
@Entity(name = "Replies", id = "localDomainEntity_2d101ac0-3ae1-477c-9a66-c46b1d6c8d18", primaryKey = "Reply Id", supportStore = false, options = {})
public class Replies {

    @Field.Text(name = "Reply Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String replyId;

    @Field(name = "Reply Message", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String replyMessage;

    @Field.Text(name = "Sender Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String senderName;

    @Field.Text(name = "Message Id", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String messageId;

    @Field(name = "Attachment Links", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String attachmentLinks;

    @Field.Date(name = "Created Date Time", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long createdDateTime;

}
