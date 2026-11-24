# Get Recent Group Chats

## Overview

The **Get Recent Group Chats** catalog request retrieves group chats that the configured account has access to, with optional filtering by topic and chat type, and an optional result limit.

This request fetches chats from the Microsoft Graph API, applies topic filtering (case-insensitive partial match), respects the specified result limit, and enriches each result with member information. It is ideal for displaying a filtered list of recent group chats, building chat pickers, or monitoring specific chat topics.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Chat.ReadBasic` | Access chat metadata (id, topic, type, created date) | **REQUIRED** |
| `Chat.Read` | Access detailed chat information and members | **REQUIRED** |
| `ChatMember.Read` | Read chat member information | **REQUIRED** |
| `User.Read.All` | Resolve member user details (name, email) | **OPTIONAL** |

> **Important**: Without `User.Read.All`, member details may be limited. Chat list and topics will still be returned, but member names and emails may be incomplete.

> **Current Behavior**: With only required permissions:
> - ✅ Group chat IDs, topics, types, and created dates are retrieved successfully
> - ✅ Filtering by topic and chat type works correctly
> - ⚠️ Member details may be limited without `User.Read.All`
> - ✅ The operation completes without errors (graceful degradation)

> **API Endpoints Used**:
> - `GET /me/chats?$filter=chatType eq 'group'...` — Requires `Chat.ReadBasic`
> - `GET /me/chats/{chatId}/members` — Requires `ChatMember.Read`

> **Documentation**: [Microsoft Graph - List chats](https://learn.microsoft.com/en-us/graph/api/chat-list) | [Microsoft Graph - List chat members](https://learn.microsoft.com/en-us/graph/api/chat-list-members)

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | QUERY_SYSTEM |
| **Retry Support** | No |

---

## Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| **Limit** | Text | No | Maximum number of group chats to return. Enter `0` or leave blank to return all matching chats. |
| **Topic** | Text | No | Filter chats by topic name (partial match, case-insensitive). Leave blank to return all topics. |
| **Chat Type** | Text | No | Filter by chat type: `group` or `meeting`. Leave blank to return both types. |

### Parameter Details

- **Limit**: Parsed as an integer. Non-numeric values are ignored and treated as no limit.
  - `0` or blank = return all matching chats
  - `10` = return at most 10 chats
  - Applied during pagination — stops fetching once the limit is reached

- **Topic**: Case-insensitive partial match against the chat's topic field.
  - Example: `"project"` matches `"Q4 Project Discussion"` and `"Project Alpha"`
  - Chats without a topic set are excluded when a topic filter is provided
  - Leave blank to include all chats regardless of topic

- **Chat Type**: Filters chats by their Microsoft Graph `chatType` value.
  - `group` — Regular group chats
  - `meeting` — Chats associated with a Teams meeting
  - Leave blank — returns both `group` and `meeting` type chats

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Group Chats** | List of Entity (Group Chat) | A list of Group Chat entities matching the specified filters |
| **Response Message** | Text | Summary message indicating how many chats were retrieved or describing any failure |

### Group Chat Entity Structure

Each Group Chat entity in the returned list contains:

| Field | Type | Description |
|-------|------|-------------|
| **Group Id** | Text | Unique identifier for the group chat |
| **Topic** | Text | Display name/topic of the group chat (may be null if not set) |
| **Type** | Text | Chat type: `group` or `meeting` |
| **Created Date Time** | Date + Time | Date and time the chat was created |
| **Members** | List of `{ Name: Text, Email: Text }` | List of chat members with their display name and email |

---

## Validation Rules

| Rule | Description | Result |
|------|-------------|--------|
| **Limit Parsing** | If Limit is non-numeric, it is ignored and treated as no limit | No error, treats as unlimited |
| **Chat Type Value** | If Chat Type is blank, both `group` and `meeting` types are included | No error |
| **No Results** | If no chats match the filters, an empty list is returned | Switch ON with "No group chats found" message |

---

## Error Handling

### 1. No Chats Found

**Scenario**: No group chats match the provided filters.

**Result**: Switch ON (success with empty list)

**Response**:
```json
{
  "Group Chats": [],
  "Response Message": "No group chats found"
}
```

**Resolution**: Adjust topic or chat type filters, or verify the configured account has access to group chats.

---

### 2. Authentication Error

**Scenario**: OAuth token is invalid, expired, or missing.

**Result**: `SYSTEM_ERROR`

**Resolution**: Verify extension configuration (Client ID, Client Secret, Tenant ID, Email) and re-authenticate.

---

### 3. Authorization Error

**Scenario**: Configured account lacks permission to access chats.

**Result**: Switch ON with failure message

**Response Message**:
```
Failed to retrieve group chats: Error code: Forbidden ...
```

**Resolution**: Ensure `Chat.ReadBasic`, `Chat.Read`, and `ChatMember.Read` permissions are granted with admin consent in Azure Portal.

---

### 4. Microsoft Graph API Error

**Scenario**: Microsoft Graph API returns an error or is temporarily unavailable.

**Result**: Switch ON with failure message and empty list

**Response Message**:
```
Failed to retrieve group chats: [error details]
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

## Usage Examples

### Example 1: Retrieve All Group Chats (No Filters)

**Input**:
```
Limit: (empty)
Topic: (empty)
Chat Type: (empty)
```

**Response**:
```json
{
  "Group Chats": [
    {
      "Group Id": "19:abc123def456@thread.v2",
      "Topic": "Engineering Standup",
      "Type": "group",
      "Created Date Time": 1720000000000,
      "Members": [
        { "Name": "Alice Smith", "Email": "alice@company.com" },
        { "Name": "Bob Jones", "Email": "bob@company.com" }
      ]
    },
    {
      "Group Id": "19:xyz789@thread.v2",
      "Topic": "Q4 Planning",
      "Type": "group",
      "Created Date Time": 1719000000000,
      "Members": [...]
    }
  ],
  "Response Message": "Successfully retrieved 2 group chat(s)"
}
```

---

### Example 2: Filter by Topic

**Input**:
```
Limit: (empty)
Topic: planning
Chat Type: (empty)
```

**Response**:
```json
{
  "Group Chats": [
    {
      "Group Id": "19:xyz789@thread.v2",
      "Topic": "Q4 Planning",
      "Type": "group",
      "Created Date Time": 1719000000000,
      "Members": [...]
    }
  ],
  "Response Message": "Successfully retrieved 1 group chat(s)"
}
```

---

### Example 3: Get Recent 5 Group Chats

**Input**:
```
Limit: 5
Topic: (empty)
Chat Type: group
```

**Response**:
```json
{
  "Group Chats": [
    { "Group Id": "19:aaa@thread.v2", "Topic": "Engineering Standup", "Type": "group", ... },
    { "Group Id": "19:bbb@thread.v2", "Topic": "Design Review", "Type": "group", ... },
    { "Group Id": "19:ccc@thread.v2", "Topic": "Release Planning", "Type": "group", ... },
    { "Group Id": "19:ddd@thread.v2", "Topic": "Incident Response", "Type": "group", ... },
    { "Group Id": "19:eee@thread.v2", "Topic": "Client Feedback", "Type": "group", ... }
  ],
  "Response Message": "Successfully retrieved 5 group chat(s)"
}
```

---

### Example 4: Filter Meeting Chats

**Input**:
```
Limit: (empty)
Topic: (empty)
Chat Type: meeting
```

**Response**:
```json
{
  "Group Chats": [
    {
      "Group Id": "19:meeting123@thread.v2",
      "Topic": "Weekly Sync",
      "Type": "meeting",
      "Created Date Time": 1720100000000,
      "Members": [...]
    }
  ],
  "Response Message": "Successfully retrieved 1 group chat(s)"
}
```

---

### Example 5: No Chats Found

**Input**:
```
Limit: (empty)
Topic: nonexistenttopic
Chat Type: (empty)
```

**Response**:
```json
{
  "Group Chats": [],
  "Response Message": "No group chats found"
}
```

---

## Business Rules

### 1. Chat Type Filtering

- If **Chat Type** is blank, the request fetches chats with `chatType eq 'group' or chatType eq 'meeting'`
- If **Chat Type** is provided (e.g., `group`), the request fetches only `chatType eq 'group'`
- Invalid chat type values are passed to the API as-is; no results will be returned for unknown types

### 2. Topic Filtering

- Applied as a case-insensitive partial match after fetching from the API
- Chats with a null or empty topic are excluded when a topic filter is specified
- No topic filter = all chats are returned regardless of topic

### 3. Limit Behavior

- Limit is applied during pagination — fetching stops as soon as the specified count is reached
- `0` or blank = no limit, all matching chats are returned
- Non-numeric Limit values are silently ignored and treated as no limit

### 4. Member Enrichment

- After fetching the chat list, member details are fetched in parallel (up to 8 concurrent threads)
- Member details include display name and email
- If member lookup fails for a specific chat, the chat is still returned with an empty or partial members list
- Member results are cached per user to avoid duplicate API calls within a single request

### 5. Ordering

- Chats are returned in the order provided by the Microsoft Graph API
- No guaranteed sort order — results typically reflect the API's default ordering

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **Member Enrichment Time** | Large numbers of chats may take longer due to parallel member lookups |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **No Date Range Filter** | Cannot filter chats by creation date in this request — use Get Group Chat Messages for date-based filtering |
| **Topic Null Exclusion** | Chats without a topic set are excluded when a Topic filter is provided |
| **Chat Type Values** | Only `group` and `meeting` are valid chat type values |
| **No Sorting** | Chats are returned in API-provided order with no guaranteed sort |
| **First Matching Limit** | Limit stops pagination early — results may not be the most recent chats if API order is not chronological |

---

## Best Practices

### Do

- **Use the Limit parameter** to avoid fetching large numbers of chats when only a few are needed
- **Use specific topic filters** to narrow results for faster performance
- **Handle empty Group Chats list** gracefully — it is a valid successful response
- **Use Group Id** for subsequent operations like Get Group Chat Messages
- **Use Chat Type filter** when you only need group chats and not meeting chats

### Don't

- **Don't expect sorted results** — sort by Created Date Time in your flow if order matters
- **Don't hardcode Group IDs** — retrieve them dynamically using this request
- **Don't call repeatedly in tight loops** — cache results when referencing the same chat multiple times
- **Don't set Limit to 1** unless you only need one result — it stops fetching after the first match regardless of relevance

---

## Common Use Cases

### 1. Chat Picker for Sending Messages

**Scenario**: Allow a user to select a group chat to send a message to.

**Implementation**:
1. Call Get Recent Group Chats with no filters or a relevant topic
2. Display chat topics in a dropdown
3. Store the selected Group Id
4. Use Group Id in Send Message Teams Group or Send HTML Message Teams Group

---

### 2. Monitor Specific Project Chats

**Scenario**: Retrieve all group chats related to a specific project.

**Implementation**:
1. Call Get Recent Group Chats with `Topic: <project name>`
2. Retrieve messages from each returned chat using Get Group Chat Messages
3. Process or display the results as needed

---

### 3. Dashboard of Recent Team Conversations

**Scenario**: Show the 10 most recent group chats on a dashboard.

**Implementation**:
1. Call Get Recent Group Chats with `Limit: 10`, no other filters
2. Display topics, member counts, and creation dates
3. Provide links to open each chat

---

### 4. Automated Chat Audit

**Scenario**: List all meeting chats created for compliance review.

**Implementation**:
1. Call Get Recent Group Chats with `Chat Type: meeting`
2. Extract Group IDs, topics, and member lists
3. Generate report of all meeting-associated chats and their participants

---

## Troubleshooting

### Issue 1: Empty Group Chats List

**Symptoms**: Request returns empty list `[]` with "No group chats found"

**Possible Causes**:
- Topic filter does not match any chats
- Chat Type filter excludes all available chats
- Configured account has not participated in any group chats
- Insufficient permissions (`Chat.ReadBasic` not granted)

**Resolution**:
1. Remove all filters and re-run to verify data exists
2. Verify `Chat.ReadBasic` and `Chat.Read` permissions are granted with admin consent
3. Confirm the configured account is a member of at least one group chat

---

### Issue 2: Members List is Empty

**Symptoms**: Chats are returned but Members field is empty `[]`

**Possible Causes**:
- `ChatMember.Read` permission is not granted
- `User.Read.All` permission is not granted, limiting user detail resolution

**Resolution**:
1. Add `ChatMember.Read` and `User.Read.All` permissions in Azure Portal
2. Grant admin consent
3. Re-run the request

---

### Issue 3: Request Returns Error Message

**Symptoms**: Response Message contains "Failed to retrieve group chats: Error code: Forbidden"

**Possible Causes**:
- Missing `Chat.ReadBasic` or `Chat.Read` permissions
- Admin consent not granted

**Resolution**:
1. Go to Azure Portal → App registrations → API permissions
2. Add the required permissions listed in the Required Permissions section above
3. Grant admin consent for the organization
4. Re-run the request

---

### Issue 4: Large Number of Chats Slowing Response

**Symptoms**: Request takes a long time when no Limit is set

**Possible Causes**:
- Configured account has access to a very large number of group chats
- Member enrichment for many chats adds latency

**Resolution**:
1. Set a reasonable Limit (e.g., `20` or `50`) to cap results
2. Use a Topic or Chat Type filter to narrow down results before member enrichment

---

## Related Catalog Requests

- [Get All Group Chats](GetAllGroupChats.md) — Retrieve all group chats without filtering
- [Get Group Chat by Name](GetGroupChatByName.md) — Find a specific group chat by exact topic name
- [Get Group Chat Messages](GetGroupChatMessages.md) — Retrieve messages from a specific group chat
- [Send Message Teams Group](SendMessageTeamsGroup.md) — Send a message to a group chat
- [Send HTML Message Teams Group](SendHTMLMessageTeamsGroup.md) — Send an HTML message to a group chat

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) — Configure OAuth credentials and Email
- [Authentication](Authentication.md) — OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) — Azure AD app registration and permissions setup
