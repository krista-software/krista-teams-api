# Create Channel

## Overview

The **Create Channel** catalog request creates a new channel in a Microsoft Teams team. It supports both **Standard** and **Private** channel types.

For **Standard channels**: If the specified team does not exist, it is automatically created before the channel is added. If the team already exists, the channel is created directly.

For **Private channels**: The team must already exist and the configured account (Email set in extension settings) must already be a member or owner of that team. If the channel already exists in the team, the request returns switch OFF without creating a duplicate.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Team.ReadBasic.All` | List joined teams to look up the target team | **REQUIRED** |
| `Channel.Create` | Create channels in a team | **REQUIRED** |
| `User.Read` | Read the authenticated user's identity (required as private channel owner) | **REQUIRED** |
| `User.Read.All` | Resolve member emails to user IDs for private channel members | **REQUIRED for Private** |
| `Group.ReadWrite.All` | Create new teams when team does not exist (Standard channel only) | **REQUIRED for Standard auto-create** |

> **Important**: For **Private channels**, the configured account must be a member or owner of the target team. If it is not, the request returns switch OFF with a message to add the account in Microsoft Teams first.

> **Channel Name Restrictions**: Microsoft Teams does not allow the following characters in channel names: `& ~ # % * { } + / \ : < > ? | ' " ,`. Using any of these returns switch OFF with a validation message before calling the API.

> **API Endpoints Used**:
> - `GET /me/joinedTeams` — Requires `Team.ReadBasic.All`
> - `GET /teams/{teamId}/channels` — Requires `Channel.ReadBasic.All` (duplicate check)
> - `POST /teams/{teamId}/channels` — Requires `Channel.Create`
> - `GET /me` — Requires `User.Read` (private channel owner)
> - `GET /users?$filter=mail eq '...'` — Requires `User.Read.All` (resolve member emails)
> - `POST /teams` — Requires `Group.ReadWrite.All` (auto team creation, Standard only)

> **Documentation**: [Microsoft Graph - Create channel](https://learn.microsoft.com/en-us/graph/api/channel-post) | [Microsoft Graph - Create team](https://learn.microsoft.com/en-us/graph/api/team-post)

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | No |

---

## Input Parameters

| Parameter Name | Type | Required | Description |
|----------------|------|----------|-------------|
| **Team Name** | Text | Yes | The display name of the Microsoft Teams team |
| **Channel Name** | Text | Yes | The display name for the new channel |
| **Channel Type** | Pick One | Yes | Channel visibility: `Standard` or `Private` |
| **Members** | Text | No | Comma-separated member emails to add (Private channels only) |

### Parameter Details

- **Team Name**: Case-insensitive partial match against teams the configured account has joined.
  - Example: `"Engineering Team"` or `"Engineering"` (partial match supported)
  - If multiple teams match, the first result is used
  - For Standard: team is auto-created if not found
  - For Private: team must already exist and configured account must be a member

- **Channel Name**: The display name that will appear in Microsoft Teams.
  - Must not contain: `& ~ # % * { } + / \ : < > ? | ' " ,`
  - Maximum 50 characters
  - Example: `"General"`, `"Project Updates"`, `"Q4 Planning"`

- **Channel Type**:
  - `Standard` — Visible to all team members. Team is automatically created if not found.
  - `Private` — Visible only to added members. Team must exist and configured account must be a member/owner.

- **Members**: Applicable for Private channels only. Comma-separated list of email addresses to add as channel members. Each user must already be a member of the parent team.
  - Example: `user1@company.com, user2@company.com`
  - Leave blank to create a private channel with only the configured account as owner

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Is Channel Created** | Boolean | `true` if channel was created successfully, `false` otherwise |
| **Response Message** | Text | Success confirmation or detailed reason for failure |

---

## Validation Rules

| Rule | Description | Result |
|------|-------------|--------|
| **Channel Name Characters** | Channel name must not contain restricted characters (`& ~ # % * { } + / \ : < > ? \| ' " ,`) | Switch OFF with validation message |
| **Team Exists (Private)** | For Private channels, team must already exist | Switch OFF with setup instructions |
| **Team Auto-Create (Standard)** | For Standard channels, team is automatically created if not found | Proceeds to channel creation |
| **Channel Not Duplicate** | A channel with the same name must not already exist in the team | Switch OFF with duplicate message |
| **Roster Membership (Private)** | Configured account must be a member or owner of the team | Switch OFF with membership instructions |

---

## Error Handling

### 1. Invalid Channel Name

**Scenario**: Channel name contains characters not allowed by Microsoft Teams.

**Result**: Switch OFF

**Response Message**:
```
Channel name contains invalid characters. These are not allowed in Teams channel names: & ~ # % * { } + / \ : < > ? | ' " ,
```

**Resolution**: Remove or replace restricted characters in the channel name. For example, replace `&` with `and`.

---

### 2. Team Not Found (Private Channel)

**Scenario**: No team with the specified name exists, or the configured account is not a member.

**Result**: Switch OFF

**Response Message**:
```
Team 'X' not found. Please create the team in Microsoft Teams, add the configured account as a member/owner, then run this request again.
```

**Resolution**:
1. Create the team manually in Microsoft Teams
2. Add the configured account (the Email set in extension settings) as a member or owner
3. Re-run the request

---

### 3. Channel Already Exists

**Scenario**: A channel with the same name already exists in the team.

**Result**: Switch OFF

**Response Message**:
```
Channel 'X' already exists in team 'Y'.
```

**Resolution**: Use a different channel name, or verify the existing channel meets your needs and use it directly.

---

### 4. Configured Account Not in Team Roster (Private Channel)

**Scenario**: Team exists but the configured account is not a member, so a private channel cannot be created.

**Result**: Switch OFF

**Response Message**:
```
Cannot create private channel 'X' in team 'Y'. The configured account is not a member of this team. Please open the team in Microsoft Teams, add the configured account as a member/owner, and run this request again.
```

**Resolution**:
1. Open the team in Microsoft Teams
2. Go to team Members settings
3. Add the configured account (extension Email) as a member or owner
4. Re-run the request

---

### 5. Team Auto-Creation Failed (Standard Channel)

**Scenario**: Team was not found and automatic creation failed or timed out after 30 seconds.

**Result**: Switch OFF

**Response Message**:
```
Team 'X' could not be created. Please create it manually in Microsoft Teams and run this request again.
```

**Resolution**: Create the team manually in Microsoft Teams and re-run the request.

---

### 6. Authentication Error

**Scenario**: OAuth token is invalid, expired, or missing.

**Result**: `SYSTEM_ERROR`

**Resolution**: Verify extension configuration (Client ID, Client Secret, Tenant ID, Email) and re-authenticate.

---

## Usage Examples

### Example 1: Create Standard Channel in Existing Team

**Input**:
```
Team Name: Engineering Team
Channel Name: Releases
Channel Type: Standard
Members: (empty)
```

**Response**:
```json
{
  "Is Channel Created": true,
  "Response Message": "Channel 'Releases' created successfully in team 'Engineering Team'."
}
```

---

### Example 2: Create Standard Channel — Team Auto-Created

**Input**:
```
Team Name: New Project Team
Channel Name: Planning
Channel Type: Standard
Members: (empty)
```

**Response**:
```json
{
  "Is Channel Created": true,
  "Response Message": "Channel 'Planning' created successfully in team 'New Project Team'."
}
```

> "New Project Team" did not exist — it was automatically created, then the channel was added in one run.

---

### Example 3: Create Private Channel with Members

**Input**:
```
Team Name: Engineering Team
Channel Name: Leadership
Channel Type: Private
Members: alice@company.com, bob@company.com
```

**Response** (success):
```json
{
  "Is Channel Created": true,
  "Response Message": "Channel 'Leadership' created successfully in team 'Engineering Team'."
}
```

**Response** (if configured account is not a team member):
```json
{
  "Is Channel Created": false,
  "Response Message": "Cannot create private channel 'Leadership' in team 'Engineering Team'. The configured account is not a member of this team. Please open the team in Microsoft Teams, add the configured account as a member/owner, and run this request again."
}
```

---

### Example 4: Channel Already Exists

**Input**:
```
Team Name: Engineering Team
Channel Name: General
Channel Type: Standard
Members: (empty)
```

**Response**:
```json
{
  "Is Channel Created": false,
  "Response Message": "Channel 'General' already exists in team 'Engineering Team'."
}
```

---

### Example 5: Invalid Channel Name

**Input**:
```
Team Name: Engineering Team
Channel Name: Summary & Notes
Channel Type: Standard
Members: (empty)
```

**Response**:
```json
{
  "Is Channel Created": false,
  "Response Message": "Channel name contains invalid characters. These are not allowed in Teams channel names: & ~ # % * { } + / \\ : < > ? | ' \" ,"
}
```

---

## Business Rules

### 1. Team Name Matching

- **Case-insensitive**: `"engineering"` matches `"Engineering Team"`
- **Partial match**: `"Eng"` matches `"Engineering Team"`
- **First match**: If multiple teams match, returns the first team found
- For Standard channels: team is auto-created if no match found
- For Private channels: team must exist; switch OFF if not found

### 2. Channel Duplicate Check

- Before creating, all existing channels in the team are checked by name (case-insensitive)
- If a matching channel exists, the request returns switch OFF without creating a duplicate

### 3. Private Channel Ownership

- The configured account is automatically added as the owner of the private channel (required by Microsoft Graph API)
- Additional members from the **Members** field are added as regular members
- All specified members must already be part of the parent team — otherwise they are skipped with a warning in the logs

### 4. Standard Channel — Automatic Team Creation

- If team is not found for a Standard channel, `POST /teams` is called with a standard Microsoft Teams template
- Microsoft provisions teams asynchronously; the request polls `me/joinedTeams` for up to 30 seconds (15 attempts × 2 seconds)
- If the team becomes available within 30 seconds, channel creation proceeds immediately
- If provisioning times out, switch OFF is returned with a message to create the team manually

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **First Match Only** | Returns result for first matching team if multiple teams share a similar name |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |
| **Team Provisioning Time** | Auto-created teams are provisioned asynchronously; times out after 30 seconds |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **Private channel roster** | Private channel members must already be in the parent team |
| **Channel name characters** | `& ~ # % * { } + / \ : < > ? \| ' " ,` are not allowed by Microsoft Teams |
| **Channel name length** | Maximum 50 characters |
| **Private channels in API-created teams** | Private channels cannot be created in teams provisioned via API until the team owner opens the team in Microsoft Teams UI |
| **No channel update** | This request only creates new channels; updating existing channels is not supported |

---

## Best Practices

### Do

- **Use exact team names** when possible for predictable results
- **For Private channels**: ensure the configured account is already a member of the team before running
- **For Standard channels**: the request is fully self-contained — both team and channel are created in one run if needed
- **Check for existing channels** with Get Channels of Team before running if you want to avoid the duplicate message
- **Use simple channel names** without special characters

### Don't

- **Don't use special characters** (`&`, `#`, `%`, etc.) in channel names — replace `&` with `and`, `#` with `No.`, etc.
- **Don't add members** in the Members field who are not already part of the parent team (for Private channels)
- **Don't expect Private channels** to work immediately in teams provisioned via the API — the team must be opened in the Microsoft Teams UI first
- **Don't rely on partial name matching** for critical operations — use exact team names where possible

---

## Common Use Cases

### 1. Automated Project Channel Setup

**Scenario**: When a new project is created in a system, automatically provision a Teams channel for it.

**Implementation**:
1. Call Create Channel with the project name as Team Name and a relevant Channel Name
2. Use `Standard` channel type for full team visibility
3. If the team does not exist, it is auto-created
4. Switch ON confirms the channel is ready for messaging

---

### 2. Confidential Discussion Channel

**Scenario**: Create a private channel for a sensitive team discussion with specific members.

**Implementation**:
1. Ensure the team exists in Microsoft Teams and the configured account is a member
2. Call Create Channel with `Private` type and the relevant member emails in Members field
3. Switch ON confirms the private channel is created with the correct membership

---

### 3. Onboarding Channel per Department

**Scenario**: Create a dedicated onboarding channel for each new department team.

**Implementation**:
1. Call Create Channel for each department with `Channel Name: Onboarding`
2. Use `Standard` type for broad visibility
3. Check `Response Message` — confirms creation or reports if channel already exists

---

### 4. Conditional Channel Creation in a Flow

**Scenario**: In a Krista flow, create a channel only if it does not already exist, and branch based on result.

**Implementation**:
1. Call Create Channel
2. Check `Is Channel Created` output
   - `true` → channel was just created — continue with new channel logic
   - `false` + "already exists" → channel existed — use existing channel
   - `false` + other message → setup issue — alert and handle error path

---

## Troubleshooting

### Issue 1: Private Channel Not Created — "Not a member of this team"

**Symptoms**: Switch OFF with roster/membership message even though team exists.

**Possible Causes**:
- The configured account (extension Email) is not added to the team in Microsoft Teams
- The team was provisioned via API and the configured account was never formally added through the Teams UI

**Resolution**:
1. Open the team in Microsoft Teams
2. Go to team settings → Members
3. Add the configured account as a member or owner
4. Re-run the Create Channel request

---

### Issue 2: Standard Channel — Team Not Created After Timeout

**Symptoms**: Switch OFF with "Team could not be created" message.

**Possible Causes**:
- Microsoft team provisioning took longer than 30 seconds
- `Group.ReadWrite.All` permission is not granted in Azure AD

**Resolution**:
1. Verify `Group.ReadWrite.All` permission is granted with admin consent in Azure Portal
2. Create the team manually in Microsoft Teams
3. Re-run the request

---

### Issue 3: Channel Name Rejected

**Symptoms**: Switch OFF with "invalid characters" message.

**Possible Causes**:
- Channel name contains restricted characters such as `&`, `#`, `%`, `*`, or others

**Resolution**:
1. Remove or replace restricted characters
2. Example: `"Summary & Notes"` → `"Summary and Notes"`
3. Re-run the request with the corrected channel name

---

### Issue 4: Channel Already Exists But Expected to Be New

**Symptoms**: Switch OFF with "already exists" message.

**Possible Causes**:
- A channel with the same name was previously created in the team

**Resolution**:
1. Use Get Channels of Team to view existing channels
2. Use a different unique channel name, or reuse the existing channel for messaging operations

---

## Related Catalog Requests

- [Get All Teams](GetAllTeams.md) — Verify team names and confirm team existence
- [Get Channels of Team](GetChannelsOfTeam.md) — List existing channels before creating to avoid duplicates
- [Get Channels of Team by Team ID](GetChannelsOfTeamByTeamID.md) — Retrieve channels using team ID for precise lookup
- [Send Message to Teams Channel](SendMessageToTeamsChannel.md) — Send messages to the newly created channel

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) — Configure OAuth credentials and Email
- [Authentication](Authentication.md) — OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) — Azure AD app registration and permissions setup
