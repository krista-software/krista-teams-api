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

import app.krista.extension.impl.anno.*;

@Domain(id = "catEntryDomain_5fa2fc97-4b17-44cf-b98f-aa91a459a091",
        name = "Collaboration",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "dfabb341-b47a-4c0d-872a-02fb564fa762")
@Entity(name = "Channel Message", id = "localDomainEntity_39dba2f1-bce5-4d60-9db6-3d4497674a01", primaryKey = "Message Id", supportStore = false, options = {})
public class ChannelMessage {

    @Field.Text(name = "Sender Name", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String senderName;

    @Field.Text(name = "Subject", required = false, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String subject;

    @Field(name = "Message", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String message;

    @Field.Text(name = "Message Id", required = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public String messageId;

    @Field(name = "Attachment Links", type = "Paragraph", required = false, attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String attachmentLinks;

    @Field.Date(name = "Created Date Time", required = false, includeTimeOfDay = true, allowPast = true, allowToday = true, allowFuture = true, attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Long createdDateTime;

}