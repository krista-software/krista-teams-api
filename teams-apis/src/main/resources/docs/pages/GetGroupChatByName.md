# Get Group Chat by Name

## Overview

The **Get Group Chat by Name** catalog request retrieves details of a specific Microsoft Teams group chat identified by its **topic name**. This operation provides access to group chat information including chat ID, topic, type, and creation timestamp, making it ideal for finding specific group chats by name for messaging or information retrieval operations.

This request accepts a group chat topic name as input and returns the matching group chat entity if found.

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
| **Group Name** | Text | Yes | The topic/name of the Microsoft Teams group chat |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

### Parameter Details

- **Group Name**: The topic name of the group chat
  - Case-sensitive exact match required
  - Must match an existing group chat the user has access to
  - Obtain from Get All Group Chats or Microsoft Teams application

---

## Output Parameters

| Parameter Name | Type | Description |
|----------------|------|-------------|
| **Group** | Entity (Group Chat) | A single Group Chat entity containing group chat details |

### Group Chat Entity Structure

The Group Chat entity contains:

| Field | Type | Description |
|-------|------|-------------|
| **groupId** | Text | Unique identifier for the group chat |
| **topic** | Text | Topic/name of the group chat |
| **type** | Text | Type of chat (typically "group") |
| **createdDateTime** | Date | Timestamp when the group chat was created (milliseconds since epoch) |

---

## Validation Rules

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Group Name Required** | Group Name parameter must not be null or empty | "Group Name is required" |
| **Group Name Length** | Group Name must be at least 1 character | "Group Name must be at least 1 character" |
| **Group Exists** | Group chat with the specified name must exist | "No group chat found with the specified name" |
| **User Access** | User must have access to the group chat | "No group chat found with the specified name" |

---

## Validation Re-Entry Flow

When Group Name validation fails, the extension provides an opportunity to correct the input.

### Step 1: Validation Failure

If the Group Name is invalid or no matching group chat is found, the system returns a validation error with details.

**Example Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "No group chat found with the specified name."
  },
  "confirmationRequest": "confirmGroupNameValidationGetGroupChatByName"
}
```

### Step 2: Confirmation

The user is prompted to confirm whether they want to re-enter the Group Name.

**Prompt**: "Would you like to re-enter the Group Name?"

**Options**:
- **Yes**: Proceed to re-entry
- **No**: Cancel the operation

### Step 3: Re-Entry

If the user chooses to re-enter, they are prompted to provide a corrected Group Name.

**Prompt**: "Please enter a valid Group Name."

### Step 4: Re-Validation

The corrected Group Name is validated again. If validation passes, the group chat is retrieved. If validation fails again, a permanent error is returned.

**Permanent Error Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "The provided group name is still invalid. Please try again with a new request."
  }
}
```

---

## Error Handling

### 1. Group Name Validation Error (INPUT_ERROR)

**Scenario**: Group Name is null, empty, or no matching group chat exists.

**Error Type**: `INPUT_ERROR`

**Response**:
```json
{
  "error": {
    "type": "INPUT_ERROR",
    "message": "No group chat found with the specified name."
  },
  "confirmationRequest": "confirmGroupNameValidationGetGroupChatByName"
}
```

**Resolution**: 
1. Verify Group Name spelling
2. Use Get All Group Chats to retrieve exact group chat names
3. Confirm user has access to the group chat

---

### 2. Group Chat Not Found (Success with Message)

**Scenario**: Group Name is valid format but no matching group chat exists.

**Response**:
```json
{
  "Group": "No group chat found with the specified name"
}
```

**Behavior**: Returns successfully with a message (not an error).

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
  "error": {
    "type": "LOGIC_ERROR",
    "message": "Failed to retrieve group chat: [error details]"
  }
}
```

**Resolution**: Check Microsoft 365 service status and network connectivity.

---

## Usage Examples

### Example 1: Retrieve Group Chat by Name

**Request**:
```
Group Name: "Project Alpha Team"
```

**Response**:
```json
{
  "Group": {
    "groupId": "19:abc123def456@thread.v2",
    "topic": "Project Alpha Team",
    "type": "group",
    "createdDateTime": 1678901234000
  }
}
```

---

### Example 2: Group Chat Not Found

**Request**:
```
Group Name: "Nonexistent Group"
```

**Response**:
```json
{
  "Group": "No group chat found with the specified name"
}
```

---

### Example 3: Validation Re-Entry Success

**Initial Request**:
```
Group Name: "Wrong Name"
```

**Validation Error**: Group chat not found

**User Action**: Choose to re-enter

**Corrected Input**:
```
Group Name: "Project Alpha Team"
```

**Final Response**:
```json
{
  "Group": {
    "groupId": "19:abc123def456@thread.v2",
    "topic": "Project Alpha Team",
    "type": "group",
    "createdDateTime": 1678901234000
  }
}
```

---

### Example 4: Using Group Chat ID for Messaging

**Use Case**: Find group chat and send a message.

**Step 1 - Request**:
```
Group Name: "Marketing Discussion"
```

**Step 1 - Response**:
```json
{
  "Group": {
    "groupId": "19:def456ghi789@thread.v2",
    "topic": "Marketing Discussion",
    "type": "group",
    "createdDateTime": 1678902345000
  }
}
```

**Step 2**: Use `groupId` in Send Message to Teams Group request

---

## Business Rules

### 1. Group Name Matching

- **Case-sensitive**: "project alpha" does NOT match "Project Alpha Team"
- **Exact match**: Full topic name must match exactly
- **No partial matching**: "Project" will not match "Project Alpha Team"
- **Whitespace sensitive**: Leading/trailing spaces matter

### 2. Group Chat Access

- User must be a member of the group chat
- Only accessible group chats are returned
- Deleted or archived group chats may not be accessible

### 3. Single Result

- Returns only one group chat (first match if multiple exist)
- If multiple group chats have the same topic, returns the first found
- No guarantee of which group chat is returned if duplicates exist

### 4. Validation Re-Entry

- One retry opportunity provided for invalid Group Names
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
| **Exact Match Only** | No partial or fuzzy matching supported |
| **Case-Sensitive** | Group Name must match exact case |
| **Single Result** | Returns only one group chat even if multiple matches exist |
| **No Wildcard Support** | Cannot use wildcards or regex patterns |

---

## Best Practices

### Do

- **Use exact group chat names** from Get All Group Chats for accuracy
- **Handle "not found" responses** gracefully
- **Store group chat IDs** for subsequent operations
- **Implement error handling** for validation failures
- **Use validation re-entry flow** to correct typos

### Don't

- **Don't assume case-insensitive matching** - use exact case
- **Don't use partial names** - provide full topic name
- **Don't hardcode group chat IDs** - retrieve them dynamically
- **Don't expose group chat IDs** to end users - use topics for display
- **Don't skip error handling** - validation failures are common

---

## Common Use Cases

### 1. Find Group Chat for Messaging

**Scenario**: Find a specific group chat to send a message.

**Implementation**:
1. Call Get Group Chat by Name with topic
2. Extract group chat ID from response
3. Use group chat ID in Send Message to Teams Group request
4. Handle "not found" case gracefully

---

### 2. Group Chat Lookup

**Scenario**: Verify a group chat exists before performing operations.

**Implementation**:
1. Call Get Group Chat by Name
2. Check if group chat was found
3. Proceed with operations if found
4. Display error message if not found

---

### 3. Group Chat ID Resolution

**Scenario**: Convert group chat topic to group chat ID for API calls.

**Implementation**:
1. User provides group chat topic
2. Call Get Group Chat by Name
3. Extract group chat ID
4. Use ID for subsequent API operations

---

### 4. Group Chat Validation

**Scenario**: Validate user has access to a specific group chat.

**Implementation**:
1. Call Get Group Chat by Name
2. If successful, user has access
3. If "not found", user doesn't have access or chat doesn't exist
4. Log validation result for audit

---

## Troubleshooting

### Issue 1: Group Chat Not Found

**Symptoms**: Response indicates "No group chat found with the specified name"

**Possible Causes**:
- Group Name spelling is incorrect
- Group Name case doesn't match
- User is not a member of the group chat
- Group chat doesn't exist

**Resolution**:
1. Call Get All Group Chats to see exact group chat names
2. Verify Group Name spelling and case
3. Confirm user is a member of the group chat
4. Use validation re-entry flow to correct name

---

### Issue 2: Case Sensitivity Issues

**Symptoms**: Group chat exists but not found due to case mismatch

**Possible Causes**:
- Group Name provided in different case than actual topic
- Leading/trailing spaces in Group Name

**Resolution**:
1. Use exact case from Get All Group Chats
2. Trim leading/trailing spaces
3. Copy-paste group chat topic instead of typing

---

### Issue 3: Validation Re-Entry Fails

**Symptoms**: Corrected Group Name still fails validation

**Possible Causes**:
- Group Name still incorrect
- User lost access to group chat between attempts
- Network or API issues

**Resolution**:
1. Double-check Group Name from Get All Group Chats
2. Verify user still has group chat access
3. Initiate new request instead of retrying
4. Check Microsoft 365 service health

---

### Issue 4: Multiple Group Chats with Same Name

**Symptoms**: Unexpected group chat returned

**Possible Causes**:
- Multiple group chats have the same topic
- First matching group chat is not the intended one

**Resolution**:
1. Use unique group chat topics when creating group chats
2. Consider using Get All Group Chats and filtering by creation date
3. Rename group chats to have unique topics
4. Use group chat IDs directly if known

---

## Related Catalog Requests

- [Get All Group Chats](GetAllGroupChats.md) - Retrieve all group chats to find exact names
- [Get Group Chat Messages](GetGroupChatMessages.md) - Retrieve messages from group chats
- [Send Message to Teams Group](SendMessageTeamsGroup.md) - Send messages to group chats
- [Send HTML Message to Teams Group](SendHTMLMessageTeamsGroup.md) - Send HTML messages to group chats

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure OAuth credentials
- [Authentication](Authentication.md) - OAuth 2.0 authentication guide
- [Creating a Teams App](CreatingTeamsApp.md) - Azure AD app registration

