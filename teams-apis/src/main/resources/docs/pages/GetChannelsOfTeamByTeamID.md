# Get Channels of Team by Team ID

## Overview

The **Get Channels of Team by Team ID** catalog request retrieves all available channels for a specific Microsoft Teams team identified by its **team ID**. This operation provides precise channel retrieval using the team's unique identifier, making it ideal for programmatic access, automation workflows, and scenarios where exact team identification is required.

Unlike the team name-based request, this operation uses the team ID for exact matching, eliminating ambiguity when multiple teams have similar names.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Team.ReadBasic.All` | Access team information | **REQUIRED** |
| `Channel.ReadBasic.All` | Access channel information | **REQUIRED** |
| `ChannelMember.Read.All` | Read channel member information | **OPTIONAL** |

> **Important**: The `Team.ReadBasic.All` and `Channel.ReadBasic.All` permissions are required and allow retrieving channel information. The `ChannelMember.Read.All` permission is optional - if not configured, channel member lists will be returned as empty arrays. The operation will still succeed and return channel information, but without member details.

> **Current Behavior**: With only required permissions:
> - ✅ Channel IDs, names, descriptions, and emails are retrieved successfully
> - ⚠️ Channel member lists are returned as empty arrays `[]`
> - ✅ The operation completes without errors (graceful degradation)

> **To Get Full Channel Member Information**: Add the `ChannelMember.Read.All` permission to your Azure AD app registration and grant admin consent. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **API Endpoints Used**:
> - `GET /teams/{teamId}` - Requires `Team.ReadBasic.All` (required)
> - `GET /teams/{teamId}/channels` - Requires `Channel.ReadBasic.All` (required)
> - `GET /teams/{teamId}/channels/{channelId}/members` - Requires `ChannelMember.Read.All` (optional)

> **Documentation**: [Microsoft Graph - List channels](https://learn.microsoft.com/en-us/graph/api/channel-list) | [Microsoft Graph - List channel members](https://learn.microsoft.com/en-us/graph/api/channel-list-members)

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | QUERY_SYSTEM |
| **Retry Support** | Yes (automatic retry on transient failures) |

---

## Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| **Team ID** | Text | Yes | The unique identifier of the Microsoft Teams team |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

### Parameter Details

- **Team ID**: Microsoft Graph team identifier
  - Format: UUID (e.g., `a1b2c3d4-e5f6-7890-abcd-ef1234567890`)
  - Must be a valid team ID the user has access to
  - Obtain from Get All Teams or Get Channels of Team requests

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
| **teamID** | Text | ID of the parent team (matches input Team ID) |
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
| **Team ID Required** | Team ID parameter must not be null or empty | "Team ID is required" |
| **Team ID Format** | Team ID must be a valid UUID format | "Invalid Team ID format" |
| **Team Exists** | Team with the specified ID must exist | "The provided Team ID '{teamId}' is invalid or not accessible" |
| **User Access** | User must have access to the team | "The provided Team ID '{teamId}' is invalid or not accessible" |

---

## Validation Re-Entry Flow

When Team ID validation fails, the extension provides an opportunity to correct the input.

### Step 1: Validation Failure

If the Team ID is invalid or inaccessible, the system returns a validation error with details.

**Example Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Team ID 'invalid-id' is invalid or not accessible. Please enter a valid team ID that exists and you have permission to access."
  },
  "confirmationRequest": "confirmTeamIdValidationGetChannelsOfTeam"
}
```

### Step 2: Confirmation

The user is prompted to confirm whether they want to re-enter the Team ID.

**Prompt**: "Would you like to re-enter the Team ID?"

**Options**:
- **Yes**: Proceed to re-entry
- **No**: Cancel the operation

### Step 3: Re-Entry

If the user chooses to re-enter, they are prompted to provide a corrected Team ID.

**Prompt**: "Please enter a valid Team ID that you have access to."

### Step 4: Re-Validation

The corrected Team ID is validated again. If validation passes, channels are retrieved. If validation fails again, a permanent error is returned.

**Permanent Error Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Team ID is still invalid. Please try again with a new request."
  }
}
```

---

## Error Handling

### 1. Team ID Validation Error (INPUT_ERROR)

**Scenario**: Team ID is null, empty, invalid format, or team doesn't exist.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Team ID 'abc123' is invalid or not accessible. Please enter a valid team ID that exists and you have permission to access."
  }
}
```

**Resolution**: 
1. Verify Team ID format (should be UUID)
2. Use Get All Teams to retrieve valid team IDs
3. Confirm user has access to the team

---

### 2. Team Not Accessible (INPUT_ERROR)

**Scenario**: Team ID is valid but user doesn't have access.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Team ID is invalid or not accessible. Please enter a valid team ID that exists and you have permission to access."
  }
}
```

**Resolution**: Verify user is a member of the team.

---

### 3. Authentication Error (SYSTEM_ERROR)

**Scenario**: OAuth token is invalid, expired, or missing.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Authentication failed. Please verify your OAuth credentials.
```

**Resolution**: Verify extension configuration and re-authenticate.

---

### 4. Microsoft Graph API Error (LOGIC_ERROR)

**Scenario**: Microsoft Graph API returns an error or is unavailable.

**Error Type**: `LOGIC_ERROR`

**Response**:
```json
{
  "Channels": "Failed to retrieve channels: [error details]"
}
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

### 5. No Channels Found (Success)

**Scenario**: Team exists but has no channels (rare - General channel usually exists).

**Response**:
```json
{
  "Channels": []
}
```

**Behavior**: Returns successfully with an empty list (not an error).

---

## Usage Examples

### Example 1: Retrieve Channels by Team ID

**Request**:
```
Team ID: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
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

### Example 2: Invalid Team ID Format

**Request**:
```
Team ID: "invalid-id"
```

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Team ID 'invalid-id' is invalid or not accessible. Please enter a valid team ID that exists and you have permission to access."
  },
  "confirmationRequest": "confirmTeamIdValidationGetChannelsOfTeam"
}
```

---

### Example 3: Team ID Not Accessible

**Request**:
```
Team ID: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
```

**Response** (if user doesn't have access):
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Team ID is invalid or not accessible. Please enter a valid team ID that exists and you have permission to access."
  },
  "confirmationRequest": "confirmTeamIdValidationGetChannelsOfTeam"
}
```

---

### Example 4: Validation Re-Entry Success

**Initial Request**:
```
Team ID: "wrong-id"
```

**Validation Error**: Team ID invalid

**User Action**: Choose to re-enter

**Corrected Input**:
```
Team ID: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
```

**Final Response**:
```json
{
  "Channels": [
    {
      "iD": "19:abc123def456@thread.tacv2",
      "name": "General",
      "teamID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "members": [...]
    }
  ]
}
```

---

## Business Rules

### 1. Team ID Validation

- Team ID must be a valid UUID format
- Team must exist in Microsoft 365 tenant
- User must be a member of the team
- Validation performed against user's joined teams list

### 2. Channel Access

- Only returns channels the user has access to
- Private channels may be excluded based on membership
- Deleted or archived channels are not included

### 3. Exact Matching

- Team ID must match exactly (no partial matching)
- Case-sensitive UUID comparison
- No ambiguity - one team ID maps to one team

### 4. Validation Re-Entry

- One retry opportunity provided for invalid Team IDs
- After retry failure, permanent error returned
- User must initiate new request for additional attempts

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **One Retry Only** | Validation re-entry allowed only once per request |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **Single Team Only** | Cannot retrieve channels from multiple teams in one request |
| **No Channel Filtering** | Cannot filter channels by name or type |
| **No Sorting** | Channels returned in API-provided order |
| **Private Channel Visibility** | Private channels only visible to members |

---

## Best Practices

### Do

- **Obtain Team IDs** from Get All Teams request for accuracy
- **Validate Team ID format** before calling (UUID pattern)
- **Handle validation errors** gracefully with re-entry flow
- **Cache channel lists** if referencing multiple times
- **Use channel IDs** from response for subsequent operations

### Don't

- **Don't hardcode Team IDs** - retrieve them dynamically
- **Don't assume Team ID validity** - always handle validation errors
- **Don't expose Team IDs** to end users - use team names for display
- **Don't call repeatedly** in tight loops - cache results when possible
- **Don't skip error handling** - validation failures are common

---

## Common Use Cases

### 1. Programmatic Channel Access

**Scenario**: Automated workflow needs to access specific team channels.

**Implementation**:
1. Store team ID from previous Get All Teams call
2. Call Get Channels of Team by Team ID
3. Extract channel IDs for messaging operations
4. Use channel IDs in Send Message to Teams Channel

---

### 2. Multi-Team Channel Discovery

**Scenario**: Retrieve channels from multiple specific teams.

**Implementation**:
1. Call Get All Teams to get team IDs
2. For each team ID, call Get Channels of Team by Team ID
3. Aggregate channel information
4. Display organized channel list

---

### 3. Team-Specific Reporting

**Scenario**: Generate channel reports for specific teams.

**Implementation**:
1. Identify target team IDs
2. Call Get Channels of Team by Team ID for each
3. Extract channel and member counts
4. Generate CSV or Excel report

---

### 4. Channel Validation

**Scenario**: Verify channel exists before sending message.

**Implementation**:
1. Call Get Channels of Team by Team ID
2. Search for specific channel name in results
3. Validate channel ID
4. Proceed with messaging operation

---

## Troubleshooting

### Issue 1: Team ID Validation Fails

**Symptoms**: `INPUT_ERROR` with "invalid or not accessible" message

**Possible Causes**:
- Team ID format is incorrect (not UUID)
- Team doesn't exist
- User is not a member of the team

**Resolution**:
1. Verify Team ID format (should be UUID like `a1b2c3d4-e5f6-7890-abcd-ef1234567890`)
2. Call Get All Teams to retrieve valid team IDs
3. Confirm user is a member of the team
4. Use validation re-entry flow to correct Team ID

---

### Issue 2: Empty Channels List

**Symptoms**: Request succeeds but returns empty list `[]`

**Possible Causes**:
- Team has no channels (very rare)
- All channels are private and user is not a member

**Resolution**:
1. Verify team has channels in Microsoft Teams application
2. Check user's channel membership
3. Confirm user has appropriate permissions

---

### Issue 3: Missing Private Channels

**Symptoms**: Some channels are not returned

**Possible Causes**:
- User is not a member of private channels
- Private channels have restricted visibility

**Resolution**:
1. Verify user membership in private channels
2. Check channel privacy settings
3. Confirm user has appropriate permissions

---

### Issue 4: Validation Re-Entry Fails

**Symptoms**: Corrected Team ID still fails validation

**Possible Causes**:
- Team ID still incorrect
- User lost access to team between attempts
- Network or API issues

**Resolution**:
1. Double-check Team ID from Get All Teams
2. Verify user still has team access
3. Initiate new request instead of retrying
4. Check Microsoft 365 service health

---

## Related Catalog Requests

- [Get All Teams](GetAllTeams.md) - Retrieve all teams to obtain team IDs
- [Get Channels of Team](GetChannelsOfTeam.md) - Retrieve channels by team name (alternative)
- [Send Message to Teams Channel](SendMessageToTeamsChannel.md) - Send messages to retrieved channels
- [Get Channel Messages](GetChannelMessages.md) - Retrieve messages from channels

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

