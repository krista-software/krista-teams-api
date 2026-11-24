# Get All Teams

## Overview

The **Get All Teams** catalog request retrieves a comprehensive list of all Microsoft Teams that the authenticated user has joined. This operation provides access to team information including team IDs, names, descriptions, and member lists, making it ideal for team discovery, selection interfaces, and administrative operations.

This is a simple query operation with no input parameters required. It returns all teams the user has access to through their Microsoft 365 account.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Team.ReadBasic.All` | Access team information and list joined teams | **REQUIRED** |
| `TeamMember.Read.All` | Read team member information | **OPTIONAL** |

> **Important**: The `Team.ReadBasic.All` permission is required and allows retrieving team information. The `TeamMember.Read.All` permission is optional - if not configured, team member lists will be returned as empty arrays. The operation will still succeed and return team information, but without member details.

> **Current Behavior**: With only `Team.ReadBasic.All` permission:
> - ✅ Team IDs, names, and descriptions are retrieved successfully
> - ⚠️ Team member lists are returned as empty arrays `[]`
> - ✅ The operation completes without errors (graceful degradation)

> **To Get Full Team Member Information**: Add the `TeamMember.Read.All` permission to your Azure AD app registration and grant admin consent. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **API Endpoints Used**:
> - `GET /me/joinedTeams` - Requires `Team.ReadBasic.All` (required)
> - `GET /teams/{teamId}/members` - Requires `TeamMember.Read.All` (optional)

> **Documentation**: [Microsoft Graph - List joinedTeams](https://learn.microsoft.com/en-us/graph/api/user-list-joinedteams) | [Microsoft Graph - List team members](https://learn.microsoft.com/en-us/graph/api/team-list-members)

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | QUERY_SYSTEM |
| **Retry Support** | Yes (automatic retry on transient failures) |

---

## Input Parameters

This catalog request requires **no input parameters**. It automatically retrieves all teams for the authenticated user.

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Teams** | List of Entity (Team) | A list of Team entities containing team details |

### Team Entity Structure

Each Team entity in the returned list contains:

| Field | Type | Description |
|-------|------|-------------|
| **iD** | Text | Unique identifier for the team |
| **name** | Text | Display name of the team |
| **description** | Text | Team description (may be null) |
| **members** | List of Entity (Member) | List of team members with their details |

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

**Scenario**: User lacks permission to access Teams data.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Insufficient permissions to access Teams data.
```

**Resolution**: Ensure the user has appropriate Microsoft 365 licenses and Teams access.

---

### 3. Microsoft Graph API Error

**Scenario**: Microsoft Graph API returns an error or is unavailable.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Failed to retrieve teams from Microsoft Graph API.
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

### 4. No Teams Found

**Scenario**: User has not joined any teams.

**Response**: Empty list `[]`

**Behavior**: Returns successfully with an empty list (not an error).

---

## Usage Examples

### Example 1: Retrieve All Teams

**Request**:
```
Get All Teams
```

**Response**:
```json
{
  "Teams": [
    {
      "iD": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "name": "Engineering Team",
      "description": "Software development and engineering",
      "members": [
        {
          "iD": "user1-id",
          "displayName": "John Doe",
          "email": "john.doe@company.com",
          "roles": ["owner"]
        },
        {
          "iD": "user2-id",
          "displayName": "Jane Smith",
          "email": "jane.smith@company.com",
          "roles": ["member"]
        }
      ]
    },
    {
      "iD": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "name": "Marketing Team",
      "description": "Marketing and communications",
      "members": [...]
    }
  ]
}
```

---

### Example 2: User with No Teams

**Request**:
```
Get All Teams
```

**Response**:
```json
{
  "Teams": []
}
```

---

### Example 3: Using Teams for Selection

**Use Case**: Display a dropdown list of teams for user selection.

**Request**:
```
Get All Teams
```

**Processing**:
```
For each team in Teams:
  Display team.name in dropdown
  Store team.iD as value
```

---

## Business Rules

### 1. User Access Scope

- Only returns teams the authenticated user has **joined**
- Does not include teams the user can discover but hasn't joined
- Respects Microsoft 365 tenant boundaries

### 2. Team Membership

- Includes all team members regardless of role (owner, member, guest)
- Member information reflects current team membership at query time
- Deleted or archived teams are not included

### 3. Pagination

- Microsoft Graph API handles pagination automatically
- All teams are retrieved across multiple pages if necessary
- No manual pagination required by the caller

### 4. Data Freshness

- Returns real-time data from Microsoft Graph API
- No caching applied
- Reflects current team state at the time of the request

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **Response Size** | Large organizations with many teams may experience slower response times |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **Joined Teams Only** | Only returns teams the user has joined, not all discoverable teams |
| **No Filtering** | Cannot filter by team name, description, or other criteria |
| **No Sorting** | Teams are returned in the order provided by Microsoft Graph API |
| **Member Details** | Member information may be limited based on user permissions |

---

## Best Practices

### Do

- **Cache results locally** if you need to reference teams multiple times in a short period
- **Handle empty results gracefully** for users who haven't joined any teams
- **Use team IDs** for subsequent operations (e.g., getting channels, sending messages)
- **Implement error handling** for authentication and API failures
- **Display team names** to users for selection, but store team IDs for operations

### Don't

- **Don't assume teams exist** - always check for empty results
- **Don't hardcode team IDs** - retrieve them dynamically using this request
- **Don't call repeatedly** in tight loops - cache results when possible
- **Don't expose team IDs** to end users - use team names for display
- **Don't rely on team order** - sort teams by name if order matters

---

## Common Use Cases

### 1. Team Selection Interface

**Scenario**: Allow users to select a team from a dropdown list.

**Implementation**:
1. Call Get All Teams
2. Extract team names and IDs
3. Display team names in UI
4. Store selected team ID for subsequent operations

---

### 2. Team Discovery Dashboard

**Scenario**: Display all teams the user belongs to with member counts.

**Implementation**:
1. Call Get All Teams
2. For each team, count members
3. Display team name, description, and member count
4. Provide links to team channels or conversations

---

### 3. Team-Based Routing

**Scenario**: Route notifications or messages based on team membership.

**Implementation**:
1. Call Get All Teams
2. Check if user is member of specific team
3. Route message to appropriate team channel
4. Log team membership for audit purposes

---

### 4. Administrative Reporting

**Scenario**: Generate reports on team membership and structure.

**Implementation**:
1. Call Get All Teams
2. Extract team and member information
3. Generate CSV or Excel report
4. Include team names, member counts, and owner information

---

## Troubleshooting

### Issue 1: Empty Teams List

**Symptoms**: Request returns empty list `[]`

**Possible Causes**:
- User has not joined any teams
- User account is newly created
- Teams membership has been revoked

**Resolution**:
1. Verify user has joined teams in Microsoft Teams application
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
3. Ensure `Team.ReadBasic.All` scope is granted
4. Re-authenticate if necessary

---

### Issue 3: Incomplete Team Data

**Symptoms**: Teams returned but missing descriptions or members

**Possible Causes**:
- Insufficient permissions
- Teams have no description set
- Member information restricted by admin policies

**Resolution**:
1. Verify user has appropriate permissions
2. Check Microsoft 365 admin policies
3. Confirm required OAuth scopes are granted

---

### Issue 4: Slow Response Times

**Symptoms**: Request takes longer than expected to complete

**Possible Causes**:
- Large number of teams (100+)
- Network latency
- Microsoft Graph API throttling

**Resolution**:
1. Implement caching for frequently accessed data
2. Check network connectivity
3. Monitor Microsoft 365 service health
4. Consider pagination if available in future versions

---

## Related Catalog Requests

- [Get Channels of Team](GetChannelsOfTeam.md) - Retrieve channels for a specific team by name
- [Get Channels of Team by Team ID](GetChannelsOfTeamByTeamID.md) - Retrieve channels using team ID
- [Send Message to Teams Channel](SendMessageToTeamsChannel.md) - Send messages to team channels
- [Get Channel Messages](GetChannelMessages.md) - Retrieve messages from team channels

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

