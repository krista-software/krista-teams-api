# Send Message to User

## Overview

The **Send Message to User** catalog request sends a 1:1 chat message to a specified user in Microsoft Teams via the Microsoft Graph API. If a chat conversation doesn't exist with the user, it automatically creates a new 1:1 chat thread.

This request is ideal for automated notifications, alerts, or personalized messages to individual team members.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose |
|------------|---------|
| `User.Read.All` or `Directory.Read.All` | Look up users by email address |
| `Chat.Create` | Create new 1:1 chat conversations |
| `ChatMessage.Send` | Send messages to chats |

> **Important**: All permissions must be added to your Azure AD app registration and admin consent must be granted. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **Troubleshooting**: If you receive a 403 Forbidden error, verify that these permissions are configured and admin consent has been granted. The error message will indicate which specific permission may be missing.

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes (automatic validation retry) |

---

## Input Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| **User Email** | Text | Yes | - | Email address of the Teams user to send the message to. Must be a valid email format and exist in Microsoft Teams. |
| **Message Content** | Text | Yes | - | The content of the message to send. Must be 1-4000 characters with meaningful content. |
| **Reuse ChatId** | Boolean | No | false | Whether to reuse an existing chat if found, or create a new chat thread. |
| **Log Conversation** | Boolean | No | false | Whether to store the chat ID in Krista memory for future reuse. |
| **Allow Retry** | Boolean | No | false | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

---

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| **Status Code** | Text | HTTP response code from the Graph API (200 = success, 400 = validation error, 403 = permission error, 500 = system error) |
| **Message Id** | Text | The unique ID of the message sent (empty on failure) |
| **Chat Id** | Text | ID of the 1:1 chat used or created (empty on failure) |
| **Response Message** | Text | Success confirmation or detailed error message |

---

## Validation Rules

### User Email Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Email cannot be null or empty | "User Email is required" |
| **Format** | Must match valid email pattern (user@domain.com) | "Invalid email format: '{value}'. Please enter a valid email address." |
| **Length** | Must be 5-254 characters | "Email length must be between 5 and 254 characters" |
| **Existence** | User must exist in Microsoft Teams | "Email '{value}' not found in Microsoft Teams or not accessible." |

### Message Content Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Message cannot be null or empty | "Message Content is required" |
| **Length** | Must be 1-4000 characters | "Message content is too long ({length} characters). Maximum allowed is 4000 characters." |
| **Not Whitespace** | Cannot contain only whitespace | "Message content cannot contain only whitespace. Please provide a meaningful message." |
| **Not Special Chars Only** | Cannot contain only special characters | "Message content cannot contain only special characters. Please provide a meaningful message with text." |
| **No Excessive Repetition** | Same character cannot repeat 10+ times | "Message content contains excessive character repetition. Please provide a more natural message." |
| **Minimum Words** | Must contain at least 1 word | "Message content must contain at least one meaningful word." |
| **Maximum Newlines** | Cannot have more than 5 consecutive newlines | "Message content contains too many consecutive empty lines (max 5 allowed)." |
| **No Spam Patterns** | Cannot contain spam-like patterns or excessive capitalization (>70%) | "Message content appears to contain spam-like patterns or excessive repetition." |

### Allow Retry Parameter

**Type**: Boolean (true/false)
**Default**: false
**Purpose**: Controls whether retry prompt appears on validation errors

**Behavior**:
- **true** → Shows interactive re-entry prompt on validation failure
- **false/null** → Returns immediate error without retry prompt

**Use Cases**:
- Set to **true** for user-driven interactive flows where users can correct input
- Set to **false** for automated/batch jobs where retry prompts are not desired
- Default (false) avoids unnecessary prompts in most automation scenarios

**Since**: v1.0.1

---

## Error Handling

### 1. Input Validation Errors (Status Code: 400)

**When**: Invalid email format, user not found, or invalid message content

**Behavior**:
1. Validation fails with detailed error messages
2. User is prompted to re-enter invalid fields
3. Validation retry flow is triggered
4. User can correct the input or cancel

**Example Response**:
```json
{
  "Status Code": "400",
  "Message Id": "",
  "Chat Id": "",
  "Response Message": "The provided email address and/or message content are still invalid. Please try again with a new request."
}
```

### 2. Permission Errors (Status Code: 403)

**When**: Application lacks required Microsoft Graph API permissions

**Behavior**:
- Returns detailed permission error message
- Indicates which permissions are missing
- Directs user to contact administrator

**Example Response**:
```json
{
  "Status Code": "403",
  "Message Id": "",
  "Chat Id": "",
  "Response Message": "Error: Insufficient permissions to send message. The application needs 'User.Read.All' or 'Directory.Read.All' and 'Chat.Create' permissions in Microsoft Graph API. Please contact your administrator to grant these permissions."
}
```

**Required Permissions**:
- `User.Read.All` or `Directory.Read.All` (to look up user by email)
- `Chat.Create` (to create new chat if needed)
- `ChatMessage.Send` (to send the message)

### 3. System Errors (Status Code: 500)

**When**: Unexpected errors during message sending

**Behavior**:
- Returns generic error message with exception details
- Logs full error for troubleshooting

**Example Response**:
```json
{
  "Status Code": "500",
  "Message Id": "",
  "Chat Id": "",
  "Response Message": "Error: Failed to send message - Network timeout"
}
```

### 4. Success Response (Status Code: 200)

**Example Response**:
```json
{
  "Status Code": "200",
  "Message Id": "1234567890",
  "Chat Id": "19:abcd1234-5678-90ef-ghij-klmnopqrstuv_chat",
  "Response Message": "Message sent successfully"
}
```

---

## Validation Re-Entry Flow

When validation fails, the extension provides an interactive retry mechanism:

### Step 1: Initial Validation Failure
- Extension validates User Email and Message Content
- If validation fails, stores validation state
- Returns confirmation prompt to user

### Step 2: Confirmation Prompt
- User is asked: "Do you want to re-enter the invalid fields?"
- User can choose to retry or cancel

### Step 3: Re-Entry (if user chooses to retry)
- User provides corrected values
- Extension re-validates the input
- If validation passes, message is sent
- If validation fails again, returns permanent error (no further retries)

### Step 4: Final Result
- Success: Message sent with Message ID and Chat ID
- Failure: Error message with Status Code 400

---

## Usage Examples

### Example 1: Simple Message to User

**Input**:
```
User Email: john.doe@company.com
Message Content: Hello John, your report is ready for review.
Reuse ChatId: false
Log Conversation: false
Allow Retry: false
```

**Output**:
```
Status Code: 200
Message Id: 1234567890
Chat Id: 19:abcd1234-5678-90ef-ghij-klmnopqrstuv_chat
Response Message: Message sent successfully
```

### Example 2: Reuse Existing Chat

**Input**:
```
User Email: jane.smith@company.com
Message Content: Follow-up: The issue has been resolved.
Reuse ChatId: true
Log Conversation: true
Allow Retry: true
```

**Output**:
```
Status Code: 200
Message Id: 9876543210
Chat Id: 19:existing-chat-id_chat
Response Message: Message sent successfully
```

**Behavior**: If a chat with jane.smith@company.com already exists, it reuses that chat. The Chat ID is stored in memory for future use.

### Example 3: Invalid Email Format

**Input**:
```
User Email: invalid-email
Message Content: Test message
Reuse ChatId: false
Log Conversation: false
```

**Validation Error**:
```
Invalid email format: 'invalid-email'. Please enter a valid email address.
```

**Behavior**: User is prompted to re-enter a valid email address.

### Example 4: Message Too Long

**Input**:
```
User Email: user@company.com
Message Content: [5000 character message]
Reuse ChatId: false
Log Conversation: false
```

**Validation Error**:
```
Message content is too long (5000 characters). Maximum allowed is 4000 characters.
```

**Behavior**: User is prompted to shorten the message.

---

## Business Rules

### Chat Creation and Reuse

1. **New Chat Creation**:
   - If no chat exists with the user, a new 1:1 chat is created automatically
   - The new Chat ID is returned in the response

2. **Chat Reuse** (when `Reuse ChatId = true`):
   - Extension searches for existing chat with the user
   - If found, uses the existing chat
   - If not found, creates a new chat

3. **Chat ID Storage** (when `Log Conversation = true`):
   - Chat ID is stored in Krista memory
   - Can be retrieved for future operations
   - Useful for maintaining conversation context

4. **Retry Behavior** (controlled by `Allow Retry`):
   - **Allow Retry = true**: Interactive retry prompt shown on validation failure
   - **Allow Retry = false/null**: Direct error returned without retry prompt
   - Default behavior (false) suitable for most automation scenarios

### Message Delivery

1. **Immediate Delivery**:
   - Messages are sent immediately upon successful validation
   - No queuing or delayed delivery

2. **Message Format**:
   - Plain text messages only (for HTML, use "Send HTML Message to User")
   - Supports line breaks and basic formatting
   - Maximum 4000 characters (conservative limit; Teams supports up to 28KB)

3. **User Lookup**:
   - User is looked up by email address
   - Requires `User.Read.All` or `Directory.Read.All` permission
   - User must exist in the same Microsoft 365 tenant

---

## Limitations

### Technical Limitations

| Limitation | Value | Notes |
|------------|-------|-------|
| **Maximum Message Length** | 4000 characters | Conservative limit for reliability |
| **Recommended Message Length** | 2000 characters | For better readability |
| **Minimum Message Length** | 1 character | Must be meaningful content |
| **Email Length** | 5-254 characters | Standard email format limits |
| **Validation Retries** | 1 retry | Only one re-entry attempt allowed |

### Functional Limitations

1. **Plain Text Only**: This request sends plain text messages. For HTML formatting, use "Send HTML Message to User"
2. **Single Recipient**: Can only send to one user at a time. For multiple users, use multiple requests or group chat
3. **No Attachments**: Cannot send file attachments. Use Teams UI or separate file upload operations
4. **No Mentions**: Cannot @mention users in the message
5. **No Reactions**: Cannot add reactions or emojis programmatically

### Permission Requirements

- Requires delegated permissions (user context)
- Cannot use application permissions (service account)
- User must have access to send messages in Teams
- Target user must be in the same Microsoft 365 tenant

---

## Best Practices

### 1. Message Content

**Do**:
- Keep messages concise and clear (under 2000 characters)
- Use proper grammar and formatting
- Include context in the message
- Test with sample users first

**Don't**:
- Send very long messages (use multiple messages or files)
- Use excessive capitalization or special characters
- Send spam-like repetitive content
- Include sensitive data without encryption

### 2. Allow Retry Usage

**Do**:
- Use `Allow Retry = true` for interactive user-driven flows
- Use `Allow Retry = false` for batch/automated jobs
- Keep default (false) to avoid unnecessary prompts
- Ensure clear user messaging when retry is enabled

**Don't**:
- Enable retry for high-volume automated processes
- Disable retry for interactive user workflows
- Ignore the retry parameter (use explicit values)

### 3. Chat Management

**Do**:
- Use `Reuse ChatId = true` for ongoing conversations
- Use `Log Conversation = true` when you need to track chat history
- Clean up stored Chat IDs periodically

**Don't**:
- Create new chats unnecessarily
- Store Chat IDs indefinitely without cleanup
- Reuse chats for unrelated conversations

### 4. Error Handling

**Do**:
- Check Status Code before processing results
- Handle permission errors gracefully
- Log errors for troubleshooting
- Provide user-friendly error messages

**Don't**:
- Ignore validation errors
- Retry indefinitely on permission errors
- Expose technical error details to end users

### 5. Performance

**Do**:
- Batch multiple messages when possible
- Use async workflows for non-urgent messages
- Monitor API rate limits

**Don't**:
- Send messages in tight loops
- Ignore rate limit errors
- Send duplicate messages

---

## Common Use Cases

### 1. Automated Notifications

**Scenario**: Notify users when a workflow completes

**Implementation**:
```
User Email: [workflow owner email]
Message Content: "Your workflow '[workflow name]' has completed successfully."
Reuse ChatId: false
Log Conversation: false
```

### 2. Alert Messages

**Scenario**: Send urgent alerts to specific users

**Implementation**:
```
User Email: [on-call engineer email]
Message Content: "ALERT: System CPU usage exceeded 90% on server [server name]"
Reuse ChatId: true
Log Conversation: true
```

### 3. Task Assignments

**Scenario**: Notify user of new task assignment

**Implementation**:
```
User Email: [assignee email]
Message Content: "You have been assigned a new task: [task title]. Due date: [date]"
Reuse ChatId: false
Log Conversation: false
```

### 4. Status Updates

**Scenario**: Send progress updates to stakeholders

**Implementation**:
```
User Email: [stakeholder email]
Message Content: "Project [name] is now 75% complete. Expected completion: [date]"
Reuse ChatId: true
Log Conversation: true
```

---

## Troubleshooting

### Issue: "User not found" error

**Cause**: Email address doesn't exist in Microsoft Teams or is inaccessible

**Resolution**:
1. Verify the email address is correct
2. Confirm the user exists in Microsoft 365
3. Check if user is in the same tenant
4. Verify user has Teams enabled

### Issue: Permission denied (403)

**Cause**: Missing required Microsoft Graph API permissions

**Resolution**:
1. Go to Azure AD application
2. Add required permissions:
   - `User.Read.All` or `Directory.Read.All`
   - `Chat.Create`
   - `ChatMessage.Send`
3. Grant admin consent
4. Re-authenticate the extension

### Issue: Message not delivered

**Cause**: Various reasons (network, permissions, user status)

**Resolution**:
1. Check Status Code in response
2. Review Response Message for details
3. Verify user is active in Teams
4. Check network connectivity
5. Review extension logs

### Issue: Validation keeps failing

**Cause**: Input doesn't meet validation rules

**Resolution**:
1. Review validation error messages carefully
2. Check email format (must be valid email)
3. Check message length (1-4000 characters)
4. Ensure message has meaningful content
5. Remove excessive special characters or repetition

---

## Related Catalog Requests

- **[Send HTML Message to User](SendHTMLMessageToUser.md)** - Send formatted HTML messages to users
- **[Send Message to Teams Channel](SendMessageToTeamsChannel.md)** - Send messages to Teams channels
- **[Send Message Teams Group](SendMessageTeamsGroup.md)** - Send messages to group chats
- **[Get Group Chat Messages](GetGroupChatMessages.md)** - Retrieve messages from group chats

---

## Technical Implementation

### Validation Architecture

The request uses a multi-layer validation approach:

1. **Format Validation**: Checks email format and message structure
2. **Business Validation**: Verifies user exists in Teams
3. **Content Validation**: Ensures message quality and compliance

### Validators Used

- **UserEmailValidator**: Validates email format and user existence
- **MessageContentValidator**: Validates message content quality

### Error Handling Flow

```
Input → Validation → Success? → Send Message → Return Result
                   ↓ Failure
                   ↓
         Store State → Prompt User → Re-enter?
                                    ↓ Yes
                                    ↓
                          Re-validate → Success? → Send Message
                                       ↓ Failure
                                       ↓
                                Return Permanent Error
```

### Telemetry

The request records the following telemetry metrics:
- Operation start time
- Operation duration
- Success/failure status
- Error details (on failure)
- User email (for tracking)

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the extension
- [Authentication](Authentication.md) - OAuth 2.0 authentication details
- [Creating Teams App](CreatingTeamsApp.md) - Set up Azure AD application
- [Microsoft Graph Chat API](https://docs.microsoft.com/en-us/graph/api/resources/chat) - Official API documentation

