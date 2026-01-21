# Send Message Teams Group

## Overview

The **Send Message Teams Group** catalog request enables you to send plain text messages to Microsoft Teams group chats programmatically. This operation allows automated communication with group chat participants through the Microsoft Graph API.

Group chats in Microsoft Teams are conversations between multiple users that are not tied to a specific team or channel. This catalog request provides a simple way to send text-based messages to these group conversations by specifying the group chat name.

This operation is ideal for automated notifications, alerts, status updates, or any scenario where you need to communicate with a specific group of users in a Teams group chat environment.

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes (automatic retry on transient failures) |

## Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| **Group Name** | Text | Yes | The name of the Teams group chat to send the message to. Must be an existing group chat that the authenticated user has access to. |
| **Message** | Text | Yes | The plain text message content to send. Maximum 4000 characters. |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| **Is Message Delivered** | Boolean | Indicates whether the message was successfully delivered to the group chat. Returns `true` if successful, `false` otherwise. |

## Validation Rules

### Group Name Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Group name must be provided | "Group name is required" |
| **Minimum Length** | Must be at least 2 characters | "Group name is too short" |
| **Existence** | Group chat must exist in Teams | "The group name is either invalid or you are not authorized to access it" |
| **Access** | User must have access to the group chat | "You are not authorized to access this group chat" |

### Message Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Message content must be provided | "Message content is required" |
| **Not Empty** | Message cannot be only whitespace | "Message cannot contain only whitespace" |
| **Maximum Length** | Maximum 4000 characters | "Message is too long (X characters). Maximum allowed is 4000 characters" |

## Error Handling

### 1. Input Validation Errors

**Scenario**: Invalid group name or message content

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- Group name is empty or too short
- Group chat does not exist
- Message content is empty or exceeds 4000 characters
- User does not have access to the group chat

---

### 2. Permission Errors

**Scenario**: Insufficient permissions to send messages to the group chat

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- User is not a member of the group chat
- User's Teams license does not allow group chat messaging
- OAuth token lacks required permissions (Chat.ReadWrite scope)

---

### 3. System Errors

**Scenario**: Microsoft Graph API errors or network issues

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- Microsoft Graph API is temporarily unavailable
- Network connectivity issues
- Authentication token has expired
- Rate limiting by Microsoft Graph API

---

### 4. Success Response

**Scenario**: Message successfully delivered

**Response**:
```
Is Message Delivered: true
```

## Usage Examples

### Example 1: Send Status Update to Project Team

**Input**:
```
Group Name: "Project Alpha Team"
Message: "Daily standup meeting will start in 15 minutes. Please join the Teams call."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 2: Send Alert to Operations Group

**Input**:
```
Group Name: "IT Operations"
Message: "ALERT: Server CPU usage has exceeded 90% threshold. Please investigate immediately."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 3: Send Reminder to Leadership Team

**Input**:
```
Group Name: "Leadership Team"
Message: "Reminder: Q4 planning session tomorrow at 10 AM. Please review the agenda document shared earlier."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 4: Invalid Group Name

**Input**:
```
Group Name: "NonExistentGroup"
Message: "This message will not be delivered"
```

**Output**:
```
Is Message Delivered: false
```

## Business Rules

1. **Group Chat Identification**: Group chats are identified by their display name. If multiple group chats have the same name, the first matching group chat will be used.

2. **Message Delivery**: Messages are delivered asynchronously. A `true` response indicates the message was accepted by the Microsoft Graph API, not that all participants have read it.

3. **Group Chat Membership**: Only users who are members of the group chat can send messages to it.

4. **Plain Text Only**: This catalog request sends plain text messages only. For HTML-formatted messages, use the "Send HTML Message Teams Group" catalog request.

5. **Character Encoding**: Messages support Unicode characters, allowing international characters and emojis.

6. **Message Ordering**: Messages are delivered in the order they are sent, but network conditions may affect delivery timing.

## Limitations

### Technical Limitations

- **Maximum Message Length**: 4000 characters per message
- **Rate Limiting**: Subject to Microsoft Graph API rate limits (varies by tenant)
- **Group Name Matching**: Case-sensitive group name matching
- **No Attachments**: Cannot send file attachments (text only)
- **No Mentions**: Cannot include @mentions in plain text messages
- **No Formatting**: No support for bold, italic, or other text formatting

### Functional Limitations

- **Group Chat Access**: Can only send to group chats where the authenticated user is a member
- **No Delivery Confirmation**: No confirmation that recipients have read the message
- **No Message Editing**: Cannot edit or delete messages after sending
- **No Threading**: Messages are sent to the main conversation, not as replies to specific messages

## Best Practices

### Do

- **Verify Group Names**: Ensure group chat names are spelled correctly and match exactly
- **Keep Messages Concise**: Shorter messages are easier to read and less likely to be truncated
- **Use Meaningful Content**: Provide clear, actionable information in messages
- **Handle Failures Gracefully**: Check the `Is Message Delivered` response and implement retry logic for critical messages
- **Test with Sample Groups**: Test your integration with non-production group chats first
- **Monitor Rate Limits**: Implement throttling to avoid hitting Microsoft Graph API rate limits

### Don't

- **Don't Spam**: Avoid sending excessive messages to group chats
- **Don't Send Sensitive Data**: Avoid including passwords, API keys, or other sensitive information in messages
- **Don't Assume Delivery**: Always check the response to confirm message delivery
- **Don't Use for Large Broadcasts**: For large-scale messaging, consider alternative approaches
- **Don't Hardcode Group Names**: Use configuration or parameters to specify group names

## Common Use Cases

### 1. Automated Notifications

Send automated alerts and notifications to specific teams or groups based on system events, monitoring thresholds, or scheduled tasks.

**Example**: "Database backup completed successfully at 2:00 AM. All systems operational."

---

### 2. Workflow Updates

Notify team members about workflow status changes, approvals, or task completions in business processes.

**Example**: "Purchase Order #12345 has been approved and is now ready for processing."

---

### 3. Meeting Reminders

Send automated reminders about upcoming meetings, deadlines, or important events to group chat participants.

**Example**: "Reminder: Sprint planning meeting starts in 30 minutes. Please review the backlog items."

---

### 4. System Alerts

Deliver real-time system alerts, error notifications, or performance warnings to operations or support teams.

**Example**: "WARNING: Application error rate has increased by 50% in the last 10 minutes. Investigating..."

## Troubleshooting

### Issue: Message Not Delivered (Is Message Delivered = false)

**Possible Causes**:
1. Group chat name is incorrect or does not exist
2. User does not have access to the group chat
3. Message content exceeds 4000 characters
4. Network or API connectivity issues

**Solutions**:
- Verify the group chat name is spelled correctly and exists in Teams
- Ensure the authenticated user is a member of the group chat
- Check message length and trim if necessary
- Verify network connectivity and Microsoft Graph API status
- Check extension configuration and OAuth token validity

---

### Issue: Group Chat Not Found

**Possible Causes**:
1. Group chat name is misspelled
2. Group chat has been deleted or archived
3. User does not have access to the group chat
4. Case sensitivity mismatch in group name

**Solutions**:
- Use the "Get All Group Chats" catalog request to list available group chats
- Verify the exact group chat name (case-sensitive)
- Ensure the user is a member of the group chat
- Check if the group chat still exists in Teams

---

### Issue: Permission Denied

**Possible Causes**:
1. User is not a member of the group chat
2. OAuth token lacks required scopes
3. User's Teams license does not support group chat messaging

**Solutions**:
- Add the user to the group chat in Teams
- Verify OAuth token includes `Chat.ReadWrite` scope
- Check user's Teams license and permissions
- Review extension configuration for correct client ID and secret

---

### Issue: Intermittent Failures

**Possible Causes**:
1. Microsoft Graph API rate limiting
2. Network connectivity issues
3. Token expiration during operation

**Solutions**:
- Implement exponential backoff retry logic
- Monitor and respect Microsoft Graph API rate limits
- Implement token refresh logic
- Add error handling and logging for diagnostics

## Related Catalog Requests

- **[Send HTML Message Teams Group](SendHTMLMessageTeamsGroup.md)** - Send HTML-formatted messages to group chats
- **[Send Message to User](SendMessageToUser.md)** - Send 1:1 messages to individual users
- **[Send Message to Teams Channel](SendMessageToTeamsChannel.md)** - Send messages to team channels
- **[Get All Group Chats](GetAllGroupChats.md)** - Retrieve list of all accessible group chats
- **[Get Group Chat by Name](GetGroupChatByName.md)** - Get details of a specific group chat
- **[Get Group Chat Messages](GetGroupChatMessages.md)** - Retrieve messages from a group chat

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the Teams API extension
- [Authentication](Authentication.md) - OAuth 2.0 authentication setup
- [Creating Teams App](CreatingTeamsApp.md) - Azure AD application setup
- [Microsoft Graph API - Chat Resource](https://learn.microsoft.com/en-us/graph/api/resources/chat) - Official Microsoft documentation

