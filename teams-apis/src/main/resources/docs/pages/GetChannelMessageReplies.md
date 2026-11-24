# Get Channel Message Replies

## Overview

The **Get Channel Message Replies** catalog request retrieves all replies (thread messages) for a specific message in a Microsoft Teams channel. This operation provides access to conversation threads, enabling you to view discussions, track responses, and analyze message interactions within Teams channels.

This request requires three parameters: the **Channel Message ID** of the parent message, the **Channel ID** where the message exists, and the **Team ID** that owns the channel.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Team.ReadBasic.All` | Access team information | **REQUIRED** |
| `Channel.ReadBasic.All` | Access channel information | **REQUIRED** |
| `ChannelMessage.Read.All` | Read channel messages and replies | **REQUIRED** |

> **Important**: All permissions listed above are required for this operation to work. Without the `ChannelMessage.Read.All` permission, this operation will fail with a 403 Forbidden error.

> **Current Behavior**: Without `ChannelMessage.Read.All` permission:
> - ❌ The operation will fail with a 403 Forbidden error
> - ❌ Channel message replies cannot be retrieved

> **To Enable This Operation**: Add the `ChannelMessage.Read.All` permission to your Azure AD app registration and grant admin consent. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **API Endpoints Used**:
> - `GET /teams/{teamId}/channels/{channelId}/messages/{messageId}/replies` - Requires `ChannelMessage.Read.All` (required)

> **Troubleshooting**: If you receive a 403 Forbidden error, verify that the `ChannelMessage.Read.All` permission is configured and admin consent has been granted.

> **Documentation**: [Microsoft Graph - List message replies](https://learn.microsoft.com/en-us/graph/api/chatmessage-list-replies)

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
| **Channel Message ID** | Text | Yes | The unique ID of the channel message (13-19 digit numeric format) |
| **Channel ID** | Text | Yes | The unique ID of the Teams channel containing the message |
| **Team ID** | Text | Yes | The unique ID of the Teams team that owns the channel |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

### Parameter Details

- **Channel Message ID**: The unique identifier of the parent message
  - Format: 13-19 digit numeric string (e.g., "1678901234567890")
  - Obtain from Get Channel Messages or Microsoft Teams application
  - Must exist in the specified channel

- **Channel ID**: The unique identifier of the Teams channel
  - Format: 19-character alphanumeric string with special characters
  - Example: `19:abc123def456@thread.tacv2`
  - Obtain from Get Channels of Team or Get Channels of Team by Team ID

- **Team ID**: The unique identifier of the Teams team
  - Format: GUID (e.g., `12345678-1234-1234-1234-123456789abc`)
  - Obtain from Get All Teams

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Replies List** | List of Entity (Replies) | A list of Replies entities containing reply message details |

### Replies Entity Structure

Each Replies entity in the returned list contains:

| Field | Type | Description |
|-------|------|-------------|
| **iD** | Text | Unique identifier for the reply message |
| **content** | Text | Content of the reply message |
| **createdDateTime** | Date | Timestamp when the reply was created (milliseconds since epoch) |
| **from** | Text | Display name of the user who sent the reply |
| **webUrl** | Text | URL to view the reply in Microsoft Teams |
| **importance** | Text | Importance level (normal, high, urgent) |
| **messageType** | Text | Type of message (message, systemEventMessage, etc.) |

---

## Validation Rules

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Channel Message ID Required** | Channel Message ID must not be null or empty | "Channel Message ID is required" |
| **Channel Message ID Format** | Channel Message ID must be 13-19 digit numeric string | "The provided Channel Message ID is invalid, not in the expected format, or does not exist in the specified channel." |
| **Channel Message ID Exists** | Message must exist in the specified channel | "The provided Channel Message ID is invalid, not in the expected format, or does not exist in the specified channel." |
| **Channel ID Required** | Channel ID must not be null or empty | "Channel ID is required" |
| **Team ID Required** | Team ID must not be null or empty | "Team ID is required" |

---

## Validation Re-Entry Flow

When Channel Message ID validation fails, the extension provides an opportunity to correct the input.

### Step 1: Validation Failure

If the Channel Message ID is invalid or doesn't exist, the system returns a validation error.

**Example Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Channel Message ID '123' is invalid, not in the expected format, or does not exist in the specified channel."
  },
  "confirmationRequest": "confirmChannelIdValidationGetChannelMessageReplies"
}
```

### Step 2: Confirmation

The user is prompted to confirm whether they want to re-enter the Channel Message ID.

**Prompt**: "Would you like to re-enter the Channel Message ID?"

**Options**:
- **Yes**: Proceed to re-entry
- **No**: Cancel the operation

### Step 3: Re-Entry

If the user chooses to re-enter, they are prompted to provide a corrected Channel Message ID.

**Prompt**: "Please enter a valid Channel Message ID."

### Step 4: Re-Validation

The corrected Channel Message ID is validated again. If validation passes, the replies are retrieved. If validation fails again, a permanent error is returned.

**Permanent Error Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided channel message ID is still invalid. Please try again with a new request."
  }
}
```

---

## Error Handling

### 1. Channel Message ID Validation Error (INPUT_ERROR)

**Scenario**: Channel Message ID is null, empty, invalid format, or doesn't exist.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Channel Message ID '123' is invalid, not in the expected format, or does not exist in the specified channel."
  },
  "confirmationRequest": "confirmChannelIdValidationGetChannelMessageReplies"
}
```

**Resolution**: 
1. Verify Channel Message ID format (13-19 digits)
2. Use Get Channel Messages to retrieve valid message IDs
3. Confirm message exists in the specified channel

---

### 2. Channel ID or Team ID Missing (INPUT_ERROR)

**Scenario**: Channel ID or Team ID is null or empty.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "Channel ID is required"
  }
}
```

**Resolution**: Provide valid Channel ID and Team ID parameters.

---

### 3. No Replies Found (Success with Empty List)

**Scenario**: Message exists but has no replies.

**Response**:
```json
{
  "Replies List": []
}
```

**Behavior**: Returns successfully with an empty list (not an error).

---

### 4. Authentication Error (SYSTEM_ERROR)

**Scenario**: OAuth token is invalid, expired, or missing.

**Error Type**: `SYSTEM_ERROR`

**Response**:
```
Error: Authentication failed. Please verify your OAuth credentials.
```

**Resolution**: Verify extension configuration and re-authenticate.

---

### 5. Microsoft Graph API Error (LOGIC_ERROR)

**Scenario**: Microsoft Graph API returns an error or is unavailable.

**Error Type**: `LOGIC_ERROR`

**Response**:
```json
{
  "error": {
    "type": "LOGIC_ERROR",
    "message": "Failed to retrieve channel message replies: [error details]"
  }
}
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

## Usage Examples

### Example 1: Retrieve Message Replies

**Request**:
```
Channel Message ID: "1678901234567890"
Channel ID: "19:abc123def456@thread.tacv2"
Team ID: "12345678-1234-1234-1234-123456789abc"
```

**Response**:
```json
{
  "Replies List": [
    {
      "iD": "1678901234567891",
      "content": "Great idea! I'll work on that.",
      "createdDateTime": 1678901300000,
      "from": "John Smith",
      "webUrl": "https://teams.microsoft.com/l/message/...",
      "importance": "normal",
      "messageType": "message"
    },
    {
      "iD": "1678901234567892",
      "content": "I can help with the implementation.",
      "createdDateTime": 1678901400000,
      "from": "Jane Doe",
      "webUrl": "https://teams.microsoft.com/l/message/...",
      "importance": "normal",
      "messageType": "message"
    }
  ]
}
```

---

### Example 2: Message with No Replies

**Request**:
```
Channel Message ID: "1678901234567890"
Channel ID: "19:abc123def456@thread.tacv2"
Team ID: "12345678-1234-1234-1234-123456789abc"
```

**Response**:
```json
{
  "Replies List": []
}
```

---

### Example 3: Invalid Channel Message ID Format

**Request**:
```
Channel Message ID: "123"
Channel ID: "19:abc123def456@thread.tacv2"
Team ID: "12345678-1234-1234-1234-123456789abc"
```

**Validation Error**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Channel Message ID '123' is invalid, not in the expected format, or does not exist in the specified channel."
  },
  "confirmationRequest": "confirmChannelIdValidationGetChannelMessageReplies"
}
```

**User Action**: Choose to re-enter

**Corrected Input**:
```
Channel Message ID: "1678901234567890"
```

**Final Response**: Replies retrieved successfully

---

### Example 4: Analyzing Message Thread

**Use Case**: Retrieve all replies to analyze discussion thread.

**Step 1 - Get Channel Messages**:
```
Channel ID: "19:abc123def456@thread.tacv2"
Team ID: "12345678-1234-1234-1234-123456789abc"
From Date: "2024-01-01"
To Date: "2024-01-31"
```

**Step 2 - Extract Message ID**: `1678901234567890`

**Step 3 - Get Replies**:
```
Channel Message ID: "1678901234567890"
Channel ID: "19:abc123def456@thread.tacv2"
Team ID: "12345678-1234-1234-1234-123456789abc"
```

**Step 4 - Analyze**: Count replies, identify participants, track response times

---

## Business Rules

### 1. Message ID Format

- Must be 13-19 digit numeric string
- No letters, spaces, or special characters allowed
- Leading zeros are significant
- Format validated before API call

### 2. Message Existence

- Message must exist in the specified channel
- Message must be accessible to the authenticated user
- Deleted messages cannot be retrieved

### 3. Reply Ordering

- Replies are returned in chronological order (oldest first)
- Order is determined by `createdDateTime` field
- No custom sorting options available

### 4. Validation Re-Entry

- One retry opportunity provided for invalid Channel Message IDs
- Channel ID and Team ID cannot be changed during re-entry
- After retry failure, permanent error returned

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **One Retry Only** | Validation re-entry allowed only once per request |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |
| **No Pagination** | All replies retrieved in single request (may be slow for large threads) |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **No Filtering** | Cannot filter replies by date, author, or content |
| **No Sorting** | Replies always returned in chronological order |
| **Read-Only** | Cannot modify or delete replies through this request |
| **No Attachments** | Attachment metadata may be limited |

---

## Best Practices

### Do

- **Validate Channel Message ID format** before calling (13-19 digits)
- **Use Get Channel Messages** to retrieve valid message IDs
- **Handle empty reply lists** gracefully (messages with no replies)
- **Store message IDs** for subsequent reply retrieval
- **Implement error handling** for validation failures
- **Use validation re-entry flow** to correct invalid message IDs

### Don't

- **Don't hardcode message IDs** - retrieve them dynamically
- **Don't assume all messages have replies** - check for empty lists
- **Don't skip Channel ID or Team ID** - all three parameters are required
- **Don't use invalid formats** - ensure 13-19 digit numeric format
- **Don't call repeatedly** for the same message - cache results when possible

---

## Common Use Cases

### 1. Thread Analysis

**Scenario**: Analyze discussion threads to track engagement.

**Implementation**:
1. Call Get Channel Messages to retrieve parent messages
2. For each message, call Get Channel Message Replies
3. Count replies per message
4. Identify most active discussions
5. Generate engagement reports

---

### 2. Conversation Tracking

**Scenario**: Track responses to specific announcements or questions.

**Implementation**:
1. Send message to channel
2. Store message ID
3. Periodically call Get Channel Message Replies
4. Monitor new replies
5. Notify stakeholders of responses

---

### 3. Support Ticket Resolution

**Scenario**: Track support ticket discussions in Teams channels.

**Implementation**:
1. Create support ticket as channel message
2. Retrieve replies to track resolution progress
3. Extract solution from replies
4. Mark ticket as resolved when appropriate reply found

---

### 4. Compliance and Audit

**Scenario**: Archive message threads for compliance purposes.

**Implementation**:
1. Retrieve channel messages
2. For each message, retrieve replies
3. Store complete thread in compliance database
4. Include timestamps, authors, and content
5. Generate audit reports

---

## Troubleshooting

### Issue 1: Invalid Channel Message ID Format

**Symptoms**: Validation error indicating invalid format

**Possible Causes**:
- Message ID contains non-numeric characters
- Message ID is too short (< 13 digits) or too long (> 19 digits)
- Message ID copied incorrectly

**Resolution**:
1. Verify message ID is 13-19 digit numeric string
2. Use Get Channel Messages to retrieve correct message ID
3. Copy message ID carefully without extra characters
4. Use validation re-entry flow to correct

---

### Issue 2: Message Not Found

**Symptoms**: Validation error indicating message doesn't exist

**Possible Causes**:
- Message ID is from different channel
- Message was deleted
- Channel ID or Team ID is incorrect
- User doesn't have access to message

**Resolution**:
1. Verify Channel ID and Team ID are correct
2. Confirm message exists in Microsoft Teams application
3. Check user has access to the channel
4. Use Get Channel Messages to verify message ID

---

### Issue 3: Empty Replies List

**Symptoms**: Request succeeds but returns empty list

**Possible Causes**:
- Message has no replies
- Replies were deleted
- User doesn't have permission to view replies

**Resolution**:
1. Verify message has replies in Microsoft Teams application
2. Check user permissions
3. Confirm replies weren't deleted
4. This is normal behavior for messages without replies

---

### Issue 4: Missing Channel ID or Team ID

**Symptoms**: Error indicating Channel ID or Team ID is required

**Possible Causes**:
- Parameter not provided
- Parameter is null or empty string
- Parameter name misspelled

**Resolution**:
1. Provide all three required parameters
2. Verify parameter names match exactly
3. Ensure parameters are not null or empty
4. Use Get All Teams and Get Channels of Team to retrieve IDs

---

## Related Catalog Requests

- [Get Channel Messages](GetChannelMessages.md) - Retrieve parent messages to get message IDs
- [Get Channels of Team](GetChannelsOfTeam.md) - Retrieve channel IDs
- [Get Channels of Team by Team ID](GetChannelsOfTeamByTeamID.md) - Retrieve channel IDs by team ID
- [Get All Teams](GetAllTeams.md) - Retrieve team IDs
- [Send Message to Teams Channel](SendMessageToTeamsChannel.md) - Send messages to channels
- [Send HTML Message to Teams Channel](SendHTMLMessageToTeamsChannel.md) - Send HTML messages to channels

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

