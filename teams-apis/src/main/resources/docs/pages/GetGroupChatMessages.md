# Get Group Chat Messages

## Overview

The **Get Group Chat Messages** catalog request retrieves messages from a specific Microsoft Teams group chat within a specified date range. This operation provides access to group chat conversation history, making it ideal for message archival, compliance reporting, conversation analysis, and integration with external systems.

This request requires a Chat ID to identify the group chat, along with a required From Date and optional To Date to filter messages by time period.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Chat.ReadBasic` or `Chat.Read` or `Chat.ReadWrite` | Access chat information and read messages | **REQUIRED** |

> **Important**: The `Chat.ReadBasic` (or `Chat.Read` or `Chat.ReadWrite`) permission is required and allows reading chat messages. The `Chat.ReadWrite` permission includes read capabilities.

> **Current Behavior**: With required permissions:
> - ✅ Chat messages are retrieved successfully
> - ✅ Message content, sender information, and timestamps are available
> - ✅ The operation completes without errors

> **API Endpoints Used**:
> - `GET /chats/{chatId}/messages` - Requires `Chat.ReadBasic` (least privileged) or `Chat.Read` or `Chat.ReadWrite` (required)

> **Documentation**: [Microsoft Graph - List chat messages](https://learn.microsoft.com/en-us/graph/api/chat-list-messages)

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
| **Chat ID** | Text | Yes | The unique identifier of the Teams group chat |
| **From Date** | Date | Yes | Start date for message retrieval (includes time) |
| **To Date** | Date | No | End date for message retrieval (includes time) |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

### Parameter Details

- **Chat ID**: Microsoft Graph chat identifier
  - Format: `19:abc123def456@thread.v2` or similar
  - Obtain from Get All Group Chats or Get Group Chat by Name
  
- **From Date**: Required start date (timestamp in milliseconds)
  - Must not be in the future
  - Must be before or equal to To Date
  - Defines the beginning of the message retrieval window

- **To Date**: Optional end date (timestamp in milliseconds)
  - If not provided, defaults to current date/time
  - Must not be before From Date
  - Defines the end of the message retrieval window

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Group Chat Messages** | List of Entity (Channel Message) | A list of Channel Message entities from the group chat |

### Channel Message Entity Structure

Each Channel Message entity contains:

| Field | Type | Description |
|-------|------|-------------|
| **iD** | Text | Unique identifier for the message |
| **content** | Text | Message content (plain text or HTML) |
| **createdDateTime** | Date | Timestamp when message was created |
| **from** | Entity (User) | User who sent the message |
| **webUrl** | Text | URL to view message in Teams |
| **importance** | Text | Message importance (normal, high, urgent) |
| **messageType** | Text | Type of message (message, systemEventMessage, etc.) |

---

## Validation Rules

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Chat ID Required** | Chat ID must not be null or empty | "Chat ID is required" |
| **Chat ID Exists** | Chat with specified ID must exist | "The provided Chat ID is invalid or not accessible" |
| **User Access** | User must have access to the group chat | "The provided Chat ID is invalid or not accessible" |
| **From Date Required** | From Date must be provided | "From Date is required" |
| **From Date Not Future** | From Date must not be in the future | "From Date must not be in the future" |
| **Date Range Valid** | From Date must be before or equal to To Date | "From Date must be before or equal to To Date" |

---

## Validation Re-Entry Flow

This request has **two separate validation flows** for different validation failures:

### Flow 1: Chat ID Validation

**Trigger**: Chat ID is null, empty, invalid, or user doesn't have access.

**Steps**:
1. Validation error returned with details
2. User prompted: "Would you like to re-enter the Chat ID?"
3. If Yes: User provides corrected Chat ID
4. Re-validation performed
5. If still invalid: Permanent error returned

**Example Error Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Chat ID is invalid or not accessible."
  },
  "confirmationRequest": "confirmReenterGetGroupChatMessages"
}
```

---

### Flow 2: Date Range Validation

**Trigger**: From Date is in the future or From Date is after To Date.

**Steps**:
1. Validation error returned: "Date Range is not Valid. Please correct the date range."
2. User prompted: "Would you like to re-enter the date range?"
3. If Yes: User provides corrected From Date and To Date
4. Re-validation performed
5. If still invalid: Permanent error returned

**Example Error Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "Date Range is not Valid. Please correct the date range."
  },
  "confirmationRequest": "confirmDateValidationGetGroupChatMessages"
}
```

---

## Error Handling

### 1. Chat ID Validation Error (INPUT_ERROR)

**Scenario**: Chat ID is null, empty, invalid, or user doesn't have access.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Chat ID is invalid or not accessible."
  },
  "confirmationRequest": "confirmReenterGetGroupChatMessages"
}
```

**Resolution**: Use Get All Group Chats or Get Group Chat by Name to retrieve valid chat IDs.

---

### 2. Date Range Validation Error (INPUT_ERROR)

**Scenario**: From Date is in the future or after To Date.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "Date Range is not Valid. Please correct the date range."
  },
  "confirmationRequest": "confirmDateValidationGetGroupChatMessages"
}
```

**Resolution**: Provide valid date range where From Date ≤ To Date and From Date is not in the future.

---

### 3. No Messages Found (Success)

**Scenario**: No messages exist in the specified date range.

**Response**:
```json
{
  "Group Chats": "Group chat messages are not present in the provided date range"
}
```

**Behavior**: Returns successfully with a message (not an error).

---

### 4. Microsoft Graph API Error (LOGIC_ERROR)

**Scenario**: Failed to retrieve messages from Microsoft Graph API.

**Error Type**: `LOGIC_ERROR`

**Response**:
```json
{
  "error": {
    "type": "LOGIC_ERROR",
    "message": "Failed to fetch group chat messages: Please enter valid data with a new request"
  }
}
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

## Usage Examples

### Example 1: Retrieve Messages with Date Range

**Request**:
```
Chat ID: "19:abc123def456@thread.v2"
From Date: 2024-01-01 00:00:00 (1704067200000)
To Date: 2024-01-31 23:59:59 (1706745599000)
```

**Response**:
```json
{
  "Group Chat Messages": [
    {
      "iD": "1234567890123",
      "content": "Happy New Year everyone!",
      "createdDateTime": 1704067800000,
      "from": {
        "displayName": "John Doe",
        "email": "john.doe@company.com"
      },
      "webUrl": "https://teams.microsoft.com/...",
      "importance": "normal",
      "messageType": "message"
    },
    {
      "iD": "1234567890124",
      "content": "Let's plan our Q1 goals.",
      "createdDateTime": 1704154200000,
      "from": {
        "displayName": "Jane Smith",
        "email": "jane.smith@company.com"
      },
      "webUrl": "https://teams.microsoft.com/...",
      "importance": "high",
      "messageType": "message"
    }
  ]
}
```

---

### Example 2: Retrieve Messages from Specific Date to Now

**Request**:
```
Chat ID: "19:abc123def456@thread.v2"
From Date: 2024-03-01 00:00:00 (1709251200000)
To Date: (not provided - defaults to current date/time)
```

**Response**:
```json
{
  "Group Chat Messages": [
    {
      "iD": "1234567890125",
      "content": "March project update meeting scheduled.",
      "createdDateTime": 1709280000000,
      "from": {...},
      "webUrl": "...",
      "importance": "normal",
      "messageType": "message"
    }
  ]
}
```

---

### Example 3: Invalid Chat ID

**Request**:
```
Chat ID: "invalid-chat-id"
From Date: 2024-01-01 00:00:00
To Date: 2024-01-31 23:59:59
```

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided Chat ID is invalid or not accessible."
  },
  "confirmationRequest": "confirmReenterGetGroupChatMessages"
}
```

---

### Example 4: Invalid Date Range (Future Date)

**Request**:
```
Chat ID: "19:abc123def456@thread.v2"
From Date: 2025-12-31 23:59:59 (future date)
To Date: (not provided)
```

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "Date Range is not Valid. Please correct the date range."
  },
  "confirmationRequest": "confirmDateValidationGetGroupChatMessages"
}
```

---

### Example 5: No Messages in Date Range

**Request**:
```
Chat ID: "19:abc123def456@thread.v2"
From Date: 2020-01-01 00:00:00
To Date: 2020-01-31 23:59:59
```

**Response**:
```json
{
  "Group Chats": "Group chat messages are not present in the provided date range"
}
```

---

## Business Rules

### 1. Date Range Handling

- **From Date is required** - must be provided
- If **To Date** is not provided: Defaults to current date/time
- Date range is inclusive (includes messages on From Date and To Date)
- Timestamps are in milliseconds since epoch
- From Date must not be in the future

### 2. Chat Access

- User must be a member of the group chat
- Only accessible group chats are returned
- Deleted or archived chats may not be accessible

### 3. Message Content

- Returns both plain text and HTML messages
- System messages (e.g., member added) may be included
- Deleted messages are not included
- Message content may be truncated for very long messages

### 4. Validation Re-Entry

- One retry opportunity provided for invalid Chat IDs
- One retry opportunity provided for invalid date ranges
- After retry failure, permanent error returned
- User must initiate new request for additional attempts

---

## Limitations

### Technical Limitations

| Limitation | Description |
|------------|-------------|
| **API Rate Limits** | Subject to Microsoft Graph API throttling limits |
| **One Retry Only** | Validation re-entry allowed only once per validation type |
| **Network Dependency** | Requires active internet connection to Microsoft 365 services |
| **Message Limit** | May be subject to Microsoft Graph API message retrieval limits |

### Functional Limitations

| Limitation | Description |
|------------|-------------|
| **Single Chat Only** | Cannot retrieve messages from multiple chats in one request |
| **No Message Filtering** | Cannot filter by sender, content, or message type |
| **No Sorting Options** | Messages returned in chronological order only |
| **No Attachment Access** | Message attachments not included in response |
| **From Date Required** | Cannot retrieve all messages without specifying From Date |

---

## Best Practices

### Do

- **Use specific date ranges** to limit message retrieval scope
- **Validate Chat ID** before calling using Get All Group Chats
- **Handle empty results** gracefully for date ranges with no messages
- **Store message IDs** if you need to reference specific messages later
- **Implement error handling** for both validation flows (Chat ID and Date Range)
- **Provide From Date** - it's a required parameter

### Don't

- **Don't use future dates** for From Date - validation will fail
- **Don't omit From Date** - it's required
- **Don't assume messages exist** in every date range - handle empty results
- **Don't call repeatedly** in tight loops - cache results when possible
- **Don't ignore validation errors** - use re-entry flows to correct inputs

---

## Common Use Cases

### 1. Group Chat Archival

**Scenario**: Archive group chat messages for compliance or backup.

**Implementation**:
1. Call Get All Group Chats to get chat IDs
2. For each chat, call Get Group Chat Messages with date range
3. Store messages in external database or file system
4. Repeat with different date ranges to retrieve all messages

---

### 2. Conversation Analysis

**Scenario**: Analyze group chat conversations for sentiment or topics.

**Implementation**:
1. Retrieve messages for specific date range
2. Extract message content and sender information
3. Apply natural language processing or sentiment analysis
4. Generate insights or reports

---

### 3. Compliance Reporting

**Scenario**: Generate compliance reports for specific time periods.

**Implementation**:
1. Define reporting period (From Date to To Date)
2. Call Get Group Chat Messages for each group chat
3. Extract relevant message metadata
4. Generate CSV or PDF report

---

### 4. Message Search

**Scenario**: Search for messages containing specific keywords in group chats.

**Implementation**:
1. Retrieve messages for relevant date range
2. Filter messages by keyword in application logic
3. Display matching messages to user
4. Provide links to view in Teams

---

## Troubleshooting

### Issue 1: Chat ID Validation Fails

**Symptoms**: `INPUT_ERROR` with "Chat ID is invalid or not accessible" message

**Possible Causes**:
- Chat ID format is incorrect
- Chat doesn't exist
- User is not a member of the group chat

**Resolution**:
1. Call Get All Group Chats to retrieve valid chat IDs
2. Verify user is a member of the group chat
3. Use validation re-entry flow to correct Chat ID

---

### Issue 2: Date Range Validation Fails

**Symptoms**: `INPUT_ERROR` with "Date Range is not Valid" message

**Possible Causes**:
- From Date is in the future
- From Date is after To Date
- Date format is incorrect

**Resolution**:
1. Verify From Date is not in the future
2. Ensure From Date ≤ To Date
3. Use timestamp in milliseconds format
4. Use validation re-entry flow to correct dates

---

### Issue 3: No Messages Found

**Symptoms**: Response indicates "Group chat messages are not present in the provided date range"

**Possible Causes**:
- No messages exist in the specified date range
- Group chat was created after the specified date range
- All messages in range were deleted

**Resolution**:
1. Verify date range includes group chat activity period
2. Check group chat in Microsoft Teams to confirm messages exist
3. Adjust date range to include message timestamps

---

### Issue 4: Missing From Date

**Symptoms**: Request fails with validation error

**Possible Causes**:
- From Date parameter was not provided
- From Date is null or empty

**Resolution**:
1. Provide From Date parameter - it's required
2. Use valid timestamp in milliseconds format
3. Ensure From Date is not null or empty

---

## Related Catalog Requests

- [Get All Group Chats](GetAllGroupChats.md) - Retrieve all group chats to obtain chat IDs
- [Get Group Chat by Name](GetGroupChatByName.md) - Retrieve specific group chat by name
- [Send Message to Teams Group](SendMessageTeamsGroup.md) - Send messages to group chats
- [Get Channel Messages](GetChannelMessages.md) - Retrieve messages from team channels

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

