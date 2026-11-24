# Get All Group Chats

## Overview

The **Get All Group Chats** catalog request retrieves a comprehensive list of all Microsoft Teams group chats that the authenticated user has access to. This operation provides access to group chat information including chat IDs, topics, types, and creation timestamps, making it ideal for group chat discovery, selection interfaces, and administrative operations.

This is a simple query operation with no input parameters required. It returns all group chats the user has access to through their Microsoft 365 account.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Chat.ReadBasic` or `Chat.Read` or `Chat.ReadWrite` | Access chat information and list chats | **REQUIRED** |
| `ChatMember.Read` | Read chat member information | **OPTIONAL** |

> **Important**: The `Chat.ReadBasic` (or `Chat.Read` or `Chat.ReadWrite`) permission is required and allows retrieving group chat information. The `ChatMember.Read` permission is optional - if not configured, chat member lists will be returned as empty arrays. The operation will still succeed and return chat information, but without member details.

> **Current Behavior**: With only required permissions:
> - ✅ Chat IDs, topics, types, and creation timestamps are retrieved successfully
> - ⚠️ Chat member lists are returned as empty arrays `[]`
> - ✅ The operation completes without errors (graceful degradation)

> **To Get Full Chat Member Information**: Add the `ChatMember.Read` permission to your Azure AD app registration and grant admin consent. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **API Endpoints Used**:
> - `GET /me/chats` - Requires `Chat.ReadBasic` (least privileged) or `Chat.Read` or `Chat.ReadWrite` (required)
> - `GET /me/chats/{chatId}/members` - Requires `Chat.ReadBasic` or `ChatMember.Read` (optional)

> **Documentation**: [Microsoft Graph - List chats](https://learn.microsoft.com/en-us/graph/api/chat-list) | [Microsoft Graph - List chat members](https://learn.microsoft.com/en-us/graph/api/chat-list-members)

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | QUERY_SYSTEM |
| **Retry Support** | Yes (automatic retry on transient failures) |

---

## Input Parameters

This catalog request requires **no input parameters**. It automatically retrieves all group chats for the authenticated user.

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Group Chats** | List of Entity (Group Chat) | A list of Group Chat entities containing group chat details |

### Group Chat Entity Structure

Each Group Chat entity in the returned list contains:

| Field | Type | Description |
|-------|------|-------------|
| **groupId** | Text | Unique identifier for the group chat |
| **topic** | Text | Topic/name of the group chat |
| **type** | Text | Type of chat (typically "group") |
| **createdDateTime** | Date | Timestamp when the group chat was created (milliseconds since epoch) |

---

## Validation Rules

This catalog request has **no validation rules** as it requires no input parameters.

---

## Error Handling

### 1. Authentication Error

**Scenario**: OAuth token is invalid, expired, or missing.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Authentication failed. Please verify your OAuth credentials.
```

**Resolution**: Verify extension configuration and re-authenticate.

---

### 2. Authorization Error

**Scenario**: User lacks permission to access group chat data.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Insufficient permissions to access group chat data.
```

**Resolution**: Ensure the user has appropriate Microsoft 365 licenses and Teams access.

---

### 3. Microsoft Graph API Error

**Scenario**: Microsoft Graph API returns an error or is unavailable.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Failed to retrieve group chats from Microsoft Graph API.
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

### 4. No Group Chats Found

**Scenario**: User has not joined any group chats.

**Response**: Empty list `[]`

**Behavior**: Returns successfully with an empty list (not an error).

---

## Usage Examples

### Example 1: Retrieve All Group Chats

**Request**:
```
Get All Group Chats
```

**Response**:
```json
{
  "Group Chats": [
    {
      "groupId": "19:abc123def456@thread.v2",
      "topic": "Project Alpha Team",
      "type": "group",
      "createdDateTime": 1678901234000
    },
    {
      "groupId": "19:def456ghi789@thread.v2",
      "topic": "Marketing Discussion",
      "type": "group",
      "createdDateTime": 1678902345000
    },
    {
      "groupId": "19:ghi789jkl012@thread.v2",
      "topic": "Weekly Standup",
      "type": "group",
      "createdDateTime": 1678903456000
    }
  ]
}
```

---

### Example 2: User with No Group Chats

**Request**:
```
Get All Group Chats
```

**Response**:
```json
{
  "Group Chats": []
}
```

---

### Example 3: Using Group Chats for Selection

**Use Case**: Display a dropdown list of group chats for user selection.

**Request**:
```
Get All Group Chats
```

**Processing**:
```
For each groupChat in Group Chats:
  Display groupChat.topic in dropdown
  Store groupChat.groupId as value
```

---

### Example 4: Finding Specific Group Chat

**Use Case**: Search for a group chat by topic name.

**Request**:
```
Get All Group Chats
```

**Processing**:
```
For each groupChat in Group Chats:
  If groupChat.topic contains "Project Alpha":
    Use groupChat.groupId for messaging
```

---

## Business Rules

### 1. User Access Scope

- Only returns group chats the authenticated user is a **member** of
- Does not include 1:1 chats (only group chats)
- Respects Microsoft 365 tenant boundaries

### 2. Group Chat Types

- Returns only group chats (type = "group")
- Does not include meeting chats or channel conversations
- Includes both active and archived group chats user has access to

### 3. Pagination

- Microsoft Graph API handles pagination automatically
- All group chats are retrieved across multiple pages if necessary
- No manual pagination required by the caller

### 4. Data Freshness

- Returns real-time data from Microsoft Graph API
- No caching applied
- Reflects current group chat state at the time of the request

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **Response Size** | Large organizations with many group chats may experience slower response times |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **Group Chats Only** | Only returns group chats, not 1:1 chats or channel conversations |
| **No Filtering** | Cannot filter by topic, creation date, or other criteria |
| **No Sorting** | Group chats are returned in the order provided by Microsoft Graph API |
| **Limited Metadata** | Only basic group chat information is returned (no member lists) |

---

## Best Practices

### Do

- **Cache results locally** if you need to reference group chats multiple times in a short period
- **Handle empty results gracefully** for users who haven't joined any group chats
- **Use group chat IDs** for subsequent operations (e.g., sending messages, retrieving messages)
- **Implement error handling** for authentication and API failures
- **Display group chat topics** to users for selection, but store group chat IDs for operations

### Don't

- **Don't assume group chats exist** - always check for empty results
- **Don't hardcode group chat IDs** - retrieve them dynamically using this request
- **Don't call repeatedly** in tight loops - cache results when possible
- **Don't expose group chat IDs** to end users - use topics for display
- **Don't rely on group chat order** - sort by topic if order matters

---

## Common Use Cases

### 1. Group Chat Selection Interface

**Scenario**: Allow users to select a group chat from a dropdown list.

**Implementation**:
1. Call Get All Group Chats
2. Extract group chat topics and IDs
3. Display topics in UI
4. Store selected group chat ID for subsequent operations

---

### 2. Group Chat Discovery Dashboard

**Scenario**: Display all group chats the user belongs to.

**Implementation**:
1. Call Get All Group Chats
2. Display group chat topics and creation dates
3. Provide links to view messages or send new messages
4. Sort by creation date or topic name

---

### 3. Group Chat-Based Routing

**Scenario**: Route notifications or messages based on group chat membership.

**Implementation**:
1. Call Get All Group Chats
2. Check if user is member of specific group chat
3. Route message to appropriate group chat
4. Log group chat membership for audit purposes

---

### 4. Administrative Reporting

**Scenario**: Generate reports on group chat membership and activity.

**Implementation**:
1. Call Get All Group Chats
2. Extract group chat information
3. Generate CSV or Excel report
4. Include topics, creation dates, and chat IDs

---

## Troubleshooting

### Issue 1: Empty Group Chats List

**Symptoms**: Request returns empty list `[]`

**Possible Causes**:
- User has not joined any group chats
- User account is newly created
- Group chat membership has been revoked

**Resolution**:
1. Verify user has joined group chats in Microsoft Teams application
2. Check user's Microsoft 365 license status
3. Confirm user account is active and not disabled

---

### Issue 2: Authentication Failures

**Symptoms**: `SYSTEM_ERROR` with authentication message

**Possible Causes**:
- OAuth token expired
- Invalid client credentials
- Missing required scopes

**Resolution**:
1. Verify extension configuration parameters
2. Check OAuth token expiration
3. Ensure `Chat.ReadWrite` scope is granted
4. Re-authenticate if necessary

---

### Issue 3: Incomplete Group Chat Data

**Symptoms**: Group chats returned but missing topics or creation dates

**Possible Causes**:
- Group chats have no topic set
- Insufficient permissions
- Microsoft Graph API limitations

**Resolution**:
1. Verify user has appropriate permissions
2. Check Microsoft 365 admin policies
3. Confirm required OAuth scopes are granted

---

### Issue 4: Slow Response Times

**Symptoms**: Request takes longer than expected to complete

**Possible Causes**:
- Large number of group chats (100+)
- Network latency
- Microsoft Graph API throttling

**Resolution**:
1. Implement caching for frequently accessed data
2. Check network connectivity
3. Monitor Microsoft 365 service health
4. Consider pagination if available in future versions

---

## Related Catalog Requests

- [Get Group Chat by Name](GetGroupChatByName.md) - Retrieve specific group chat by topic name
- [Get Group Chat Messages](GetGroupChatMessages.md) - Retrieve messages from group chats
- [Send Message to Teams Group](SendMessageTeamsGroup.md) - Send messages to group chats
- [Send HTML Message to Teams Group](SendHTMLMessageTeamsGroup.md) - Send HTML messages to group chats

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

