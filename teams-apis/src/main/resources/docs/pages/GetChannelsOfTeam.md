# Get Channels of Team

## Overview

The **Get Channels of Team** catalog request retrieves all available channels for a specific Microsoft Teams team identified by its **team name**. This operation provides access to channel information including channel IDs, names, descriptions, and member lists, making it ideal for channel discovery, selection interfaces, and team navigation.

This request accepts a team name as input and returns all channels within that team that the user has access to.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Team.ReadBasic.All` | Access team information and list joined teams | **REQUIRED** |
| `Channel.ReadBasic.All` | Access channel information | **REQUIRED** |
| `TeamMember.Read.All` | Read team member information | **OPTIONAL** |
| `ChannelMember.Read.All` | Read channel member information | **OPTIONAL** |

> **Important**: The `Team.ReadBasic.All` and `Channel.ReadBasic.All` permissions are required and allow retrieving team and channel information. The `TeamMember.Read.All` and `ChannelMember.Read.All` permissions are optional - if not configured, member lists will be returned as empty arrays. The operation will still succeed and return channel information, but without member details.

> **Current Behavior**: With only required permissions:
> - ✅ Team IDs, names, and descriptions are retrieved successfully
> - ✅ Channel IDs, names, descriptions, and emails are retrieved successfully
> - ⚠️ Team member lists are returned as empty arrays `[]`
> - ⚠️ Channel member lists are returned as empty arrays `[]`
> - ✅ The operation completes without errors (graceful degradation)

> **To Get Full Member Information**: Add the `TeamMember.Read.All` and `ChannelMember.Read.All` permissions to your Azure AD app registration and grant admin consent. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **API Endpoints Used**:
> - `GET /me/joinedTeams` - Requires `Team.ReadBasic.All` (required)
> - `GET /teams/{teamId}/channels` - Requires `Channel.ReadBasic.All` (required)
> - `GET /teams/{teamId}/members` - Requires `TeamMember.Read.All` (optional)
> - `GET /teams/{teamId}/channels/{channelId}/members` - Requires `ChannelMember.Read.All` (optional)

> **Documentation**: [Microsoft Graph - List channels](https://learn.microsoft.com/en-us/graph/api/channel-list) | [Microsoft Graph - List team members](https://learn.microsoft.com/en-us/graph/api/team-list-members) | [Microsoft Graph - List channel members](https://learn.microsoft.com/en-us/graph/api/channel-list-members)

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes (automatic retry on transient failures) |

---

## Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| **Team Name** | Text | Yes | The display name of the Microsoft Teams team |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

### Parameter Details

- **Team Name**: Case-insensitive partial match supported
  - Example: "Engineering" will match "Engineering Team"
  - Must match at least one team the user has joined
  - Returns channels for the first matching team if multiple matches exist

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Channels** | List of Entity (Channel) | A list of Channel entities containing channel details |

### Channel Entity Structure

Each Channel entity in the returned list contains:

| Field | Type | Description |
|-------|------|-------------|
| **iD** | Text | Unique identifier for the channel |
| **name** | Text | Display name of the channel |
| **description** | Text | Channel description (may be null) |
| **email** | Text | Email address for the channel (may be null) |
| **teamID** | Text | ID of the parent team |
| **members** | List of Entity (Member) | List of channel members with their details |

### Member Entity Structure

Each Member entity contains:

| Field | Type | Description |
|-------|------|-------------|
| **iD** | Text | Unique identifier for the member |
| **displayName** | Text | Display name of the member |
| **email** | Text | Email address of the member |
| **roles** | List of Text | Member roles (e.g., "owner", "member") |

---

## Validation Rules

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Team Name Required** | Team Name parameter must be provided | "Team Name is required" |
| **Team Exists** | Team with the specified name must exist | "No team found with name: {teamName}" |
| **User Access** | User must have access to the team | "No team found with name: {teamName}" |

---

## Error Handling

### 1. Team Not Found

**Scenario**: No team exists with the specified name, or user doesn't have access.

**Error Type**: Success with empty result

**Response**:
```json
{
  "Channels": []
}
```

**Resolution**: Verify team name spelling and user's team membership.

---

### 2. Authentication Error

**Scenario**: OAuth token is invalid, expired, or missing.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Authentication failed. Please verify your OAuth credentials.
```

**Resolution**: Verify extension configuration and re-authenticate.

---

### 3. Authorization Error

**Scenario**: User lacks permission to access team channels.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Insufficient permissions to access team channels.
```

**Resolution**: Ensure user is a member of the team and has appropriate permissions.

---

### 4. Microsoft Graph API Error

**Scenario**: Microsoft Graph API returns an error or is unavailable.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Failed to retrieve channels from Microsoft Graph API.
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

## Usage Examples

### Example 1: Retrieve Channels by Team Name

**Request**:
```
Team Name: "Engineering Team"
```

**Response**:
```json
{
  "Channels": [
    {
      "iD": "19:abc123def456@thread.tacv2",
      "name": "General",
      "description": "General discussion for the team",
      "email": "engineering.general@company.com",
      "teamID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "members": [
        {
          "iD": "user1-id",
          "displayName": "John Doe",
          "email": "john.doe@company.com",
          "roles": ["owner"]
        }
      ]
    },
    {
      "iD": "19:def456ghi789@thread.tacv2",
      "name": "Development",
      "description": "Development discussions and updates",
      "email": "engineering.dev@company.com",
      "teamID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "members": [...]
    }
  ]
}
```

---

### Example 2: Partial Team Name Match

**Request**:
```
Team Name: "Eng"
```

**Response**:
```json
{
  "Channels": [
    {
      "iD": "19:abc123def456@thread.tacv2",
      "name": "General",
      "description": "General discussion for the team",
      "teamID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "members": [...]
    }
  ]
}
```

**Note**: Returns channels for "Engineering Team" (first match).

---

### Example 3: Team Not Found

**Request**:
```
Team Name: "Nonexistent Team"
```

**Response**:
```json
{
  "Channels": []
}
```

---

### Example 4: Channel Selection Interface

**Use Case**: Display channels for user selection.

**Request**:
```
Team Name: "Marketing Team"
```

**Processing**:
```
For each channel in Channels:
  Display channel.name in dropdown
  Store channel.iD for messaging operations
```

---

## Business Rules

### 1. Team Name Matching

- **Case-insensitive**: "engineering" matches "Engineering Team"
- **Partial match**: "Eng" matches "Engineering Team"
- **First match**: If multiple teams match, returns channels for the first team found
- **Exact match preferred**: Exact matches take precedence over partial matches

### 2. Channel Access

- Only returns channels the user has access to
- Private channels may be excluded based on membership
- Deleted or archived channels are not included

### 3. Channel Ordering

- Channels are returned in the order provided by Microsoft Graph API
- "General" channel is typically first (default channel)
- No guaranteed sort order

### 4. Member Information

- Includes all channel members the user has permission to see
- Member details may be limited based on privacy settings
- Guest users may have restricted member visibility

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **First Match Only** | Returns channels for first matching team if multiple teams match |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **No Multi-Team Support** | Cannot retrieve channels from multiple teams in one request |
| **No Channel Filtering** | Cannot filter channels by name or type |
| **No Sorting** | Channels returned in API-provided order |
| **Private Channel Visibility** | Private channels only visible to members |

---

## Best Practices

### Do

- **Use exact team names** when possible for predictable results
- **Handle empty results** gracefully for non-existent teams
- **Cache channel lists** if referencing multiple times
- **Use channel IDs** for subsequent operations (e.g., sending messages)
- **Validate team name** before calling if possible

### Don't

- **Don't assume channel order** - sort by name if order matters
- **Don't rely on partial matching** for critical operations - use exact names
- **Don't hardcode channel IDs** - retrieve them dynamically
- **Don't expose channel IDs** to end users - use channel names for display
- **Don't call repeatedly** in tight loops - cache results when possible

---

## Common Use Cases

### 1. Channel Selection for Messaging

**Scenario**: Allow users to select a channel to send a message.

**Implementation**:
1. Call Get Channels of Team with team name
2. Display channel names in dropdown
3. Store selected channel ID
4. Use channel ID for Send Message to Teams Channel request

---

### 2. Team Navigation Interface

**Scenario**: Build a navigation menu showing all channels in a team.

**Implementation**:
1. Call Get Channels of Team
2. Display channels in sidebar or menu
3. Provide links to channel conversations
4. Highlight active channel

---

### 3. Channel Discovery

**Scenario**: Help users discover available channels in a team.

**Implementation**:
1. Call Get Channels of Team
2. Display channel names and descriptions
3. Show member counts
4. Provide "Join Channel" option for private channels

---

### 4. Automated Channel Reporting

**Scenario**: Generate reports on team channel structure.

**Implementation**:
1. Call Get All Teams to get team list
2. For each team, call Get Channels of Team
3. Extract channel and member information
4. Generate CSV or Excel report

---

## Troubleshooting

### Issue 1: Empty Channels List

**Symptoms**: Request returns empty list `[]`

**Possible Causes**:
- Team name doesn't match any teams
- User is not a member of the team
- Team has no channels (unlikely - General channel always exists)

**Resolution**:
1. Verify team name spelling (check with Get All Teams)
2. Confirm user is a member of the team
3. Try exact team name instead of partial match

---

### Issue 2: Missing Private Channels

**Symptoms**: Some channels are not returned

**Possible Causes**:
- User is not a member of private channels
- Private channels have restricted visibility

**Resolution**:
1. Verify user membership in private channels
2. Check channel privacy settings
3. Confirm user has appropriate permissions

---

### Issue 3: Wrong Team Channels Returned

**Symptoms**: Channels from unexpected team are returned

**Possible Causes**:
- Partial team name matches multiple teams
- First matching team is not the intended team

**Resolution**:
1. Use exact team name instead of partial match
2. Call Get All Teams to verify exact team names
3. Consider using Get Channels of Team by Team ID for precise control

---

### Issue 4: Incomplete Channel Data

**Symptoms**: Channels returned but missing descriptions or members

**Possible Causes**:
- Channels have no description set
- Member information restricted by admin policies
- Insufficient permissions

**Resolution**:
1. Verify user has appropriate permissions
2. Check Microsoft 365 admin policies
3. Confirm required OAuth scopes are granted

---

## Related Catalog Requests

- [Get All Teams](GetAllTeams.md) - Retrieve all teams to find exact team names
- [Get Channels of Team by Team ID](GetChannelsOfTeamByTeamID.md) - Retrieve channels using team ID (more precise)
- [Send Message to Teams Channel](SendMessageToTeamsChannel.md) - Send messages to retrieved channels
- [Get Channel Messages](GetChannelMessages.md) - Retrieve messages from channels

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

