# Send Message to Teams Channel

## Overview

The **Send Message to Teams Channel** catalog request enables you to send plain text messages to Microsoft Teams channels programmatically. This operation allows automated communication with channel members through the Microsoft Graph API.

Channels in Microsoft Teams are dedicated sections within a team where conversations, files, and tools are organized around specific topics or projects. This catalog request provides a simple way to post text-based messages to these channels, making information visible to all channel members.

This operation is ideal for automated announcements, status updates, notifications, or any scenario where you need to broadcast information to all members of a specific team channel.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose |
|------------|---------|
| `Team.ReadBasic.All` | Access team information |
| `Channel.ReadBasic.All` | Access channel information |
| `ChannelMessage.Send` | Send messages to channels |

> **Important**: All permissions must be added to your Azure AD app registration and admin consent must be granted. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **Troubleshooting**: If you receive a 403 Forbidden error, verify that these permissions are configured and admin consent has been granted. The error message will indicate which specific permission may be missing.

---

## Request Details

| Property | Value |
|----------|-------|
| **Area** | Teams APIs |
| **Type** | CHANGE_SYSTEM |
| **Retry Support** | Yes (automatic retry on transient failures) |

## Input Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| **Team Name** | Text | Yes | The name of the Microsoft Teams team containing the target channel. Must be an existing team that the authenticated user has access to. |
| **Channel Name** | Text | Yes | The name of the channel within the team to send the message to. Must be an existing channel that the user has access to. |
| **Message** | Text | Yes | The plain text message content to send. Maximum 4000 characters. |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| **Is Message Delivered** | Boolean | Indicates whether the message was successfully delivered to the channel. Returns `true` if successful, `false` otherwise. |

## Validation Rules

### Team Name Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Team name must be provided | "Team name is required" |
| **Minimum Length** | Must be at least 2 characters | "Team name is too short" |
| **Existence** | Team must exist in Microsoft Teams | "The team name is either invalid or you are not authorized to access it" |
| **Access** | User must have access to the team | "You are not authorized to access this team" |

### Channel Name Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Channel name must be provided | "Channel name is required" |
| **Minimum Length** | Must be at least 1 character | "Channel name cannot be empty" |
| **Existence** | Channel must exist within the specified team | "The channel name is either invalid or does not exist in the specified team" |
| **Access** | User must have access to the channel | "You are not authorized to access this channel" |

### Message Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Message content must be provided | "Message content is required" |
| **Not Empty** | Message cannot be only whitespace | "Message cannot contain only whitespace" |
| **Maximum Length** | Maximum 4000 characters | "Message is too long (X characters). Maximum allowed is 4000 characters" |

## Error Handling

### 1. Input Validation Errors

**Scenario**: Invalid team name, channel name, or message content

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- Team name is empty or too short
- Team does not exist
- Channel name is empty
- Channel does not exist in the specified team
- Message content is empty or exceeds 4000 characters
- User does not have access to the team or channel

---

### 2. Permission Errors

**Scenario**: Insufficient permissions to send messages to the channel

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- User is not a member of the team
- User does not have permission to post in the channel
- Channel is read-only or moderated
- OAuth token lacks required permissions (ChannelMessage.Send scope)

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

### Example 1: Send Deployment Notification

**Input**:
```
Team Name: "Engineering"
Channel Name: "Deployments"
Message: "Production deployment completed successfully at 14:30 UTC. Version 2.5.0 is now live."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 2: Send Daily Standup Reminder

**Input**:
```
Team Name: "Product Development"
Channel Name: "General"
Message: "Daily standup meeting starts in 10 minutes. Please join the Teams call and be ready to share your updates."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 3: Send Build Status Update

**Input**:
```
Team Name: "DevOps"
Channel Name: "CI/CD Pipeline"
Message: "Build #1234 failed. Error: Unit tests failed in authentication module. Please review and fix."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 4: Send Announcement to Team

**Input**:
```
Team Name: "Sales"
Channel Name: "Announcements"
Message: "New product launch scheduled for next Monday. All hands meeting at 9 AM to discuss go-to-market strategy."
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 5: Invalid Team Name

**Input**:
```
Team Name: "NonExistentTeam"
Channel Name: "General"
Message: "This message will not be delivered"
```

**Output**:
```
Is Message Delivered: false
```

## Business Rules

1. **Channel Visibility**: Messages sent to channels are visible to all channel members. Consider the audience before sending sensitive information.

2. **Message Delivery**: Messages are delivered asynchronously. A `true` response indicates the message was accepted by the Microsoft Graph API, not that all members have read it.

3. **Team and Channel Membership**: Users must be members of the team and have access to the channel to send messages.

4. **Plain Text Only**: This catalog request sends plain text messages only. For HTML-formatted messages, use the "Send HTML Message to Teams Channel" catalog request.

5. **Character Encoding**: Messages support Unicode characters, allowing international characters and emojis.

6. **Message Ordering**: Messages are delivered in the order they are sent, but network conditions may affect delivery timing.

7. **General Channel**: Every team has a "General" channel that cannot be deleted. This is often the default channel for team-wide announcements.

## Limitations

### Technical Limitations

- **Maximum Message Length**: 4000 characters per message
- **Rate Limiting**: Subject to Microsoft Graph API rate limits (varies by tenant)
- **Team/Channel Name Matching**: Case-sensitive name matching
- **No Attachments**: Cannot send file attachments (text only)
- **No Mentions**: Cannot include @mentions in plain text messages
- **No Formatting**: No support for bold, italic, or other text formatting
- **No Reactions**: Cannot add reactions or emojis to messages programmatically

### Functional Limitations

- **Channel Access**: Can only send to channels where the authenticated user is a member
- **No Delivery Confirmation**: No confirmation that members have read the message
- **No Message Editing**: Cannot edit or delete messages after sending
- **No Threading**: Messages are sent to the main channel conversation, not as replies to specific messages
- **Moderated Channels**: May require approval in moderated channels

## Best Practices

### Do

- **Verify Team and Channel Names**: Ensure names are spelled correctly and match exactly (case-sensitive)
- **Keep Messages Concise**: Shorter messages are easier to read and less likely to be truncated
- **Use Meaningful Content**: Provide clear, actionable information in messages
- **Handle Failures Gracefully**: Check the `Is Message Delivered` response and implement retry logic for critical messages
- **Test with Sample Channels**: Test your integration with non-production channels first
- **Monitor Rate Limits**: Implement throttling to avoid hitting Microsoft Graph API rate limits
- **Use Appropriate Channels**: Send messages to the most relevant channel for the content

### Don't

- **Don't Spam Channels**: Avoid sending excessive messages that clutter the channel
- **Don't Send Sensitive Data**: Avoid including passwords, API keys, or confidential information in channel messages
- **Don't Assume Delivery**: Always check the response to confirm message delivery
- **Don't Use for Private Messages**: Use "Send Message to User" for private communications
- **Don't Hardcode Names**: Use configuration or parameters to specify team and channel names
- **Don't Ignore Permissions**: Ensure the user has appropriate permissions before attempting to send

## Common Use Cases

### 1. CI/CD Pipeline Notifications

Send automated build, test, and deployment notifications to development team channels.

**Example**: "Build #5678 completed successfully. All tests passed. Ready for deployment to staging."

---

### 2. System Monitoring Alerts

Deliver real-time system alerts, performance warnings, or error notifications to operations channels.

**Example**: "WARNING: Database connection pool utilization at 85%. Consider scaling resources."

---

### 3. Workflow Status Updates

Notify team members about workflow status changes, approvals, or task completions in business processes.

**Example**: "Invoice #INV-2024-1234 has been approved and is ready for payment processing."

---

### 4. Scheduled Announcements

Send scheduled announcements, reminders, or important updates to team channels at specific times.

**Example**: "Reminder: Monthly team meeting tomorrow at 2 PM. Please review the agenda in the Files tab."

## Troubleshooting

### Issue: Message Not Delivered (Is Message Delivered = false)

**Possible Causes**:
1. Team name or channel name is incorrect
2. Team or channel does not exist
3. User does not have access to the team or channel
4. Message content exceeds 4000 characters
5. Network or API connectivity issues

**Solutions**:
- Verify the team name is spelled correctly and exists in Teams
- Verify the channel name is spelled correctly and exists in the team
- Ensure the authenticated user is a member of the team
- Check message length and trim if necessary
- Verify network connectivity and Microsoft Graph API status
- Check extension configuration and OAuth token validity

---

### Issue: Team or Channel Not Found

**Possible Causes**:
1. Team or channel name is misspelled
2. Team or channel has been deleted or archived
3. User does not have access to the team or channel
4. Case sensitivity mismatch in names

**Solutions**:
- Use the "Get All Teams" catalog request to list available teams
- Use the "Get Channels of Team" catalog request to list channels in a team
- Verify the exact team and channel names (case-sensitive)
- Ensure the user is a member of the team
- Check if the team or channel still exists in Teams

---

### Issue: Permission Denied

**Possible Causes**:
1. User is not a member of the team
2. User does not have permission to post in the channel
3. Channel is moderated and requires approval
4. OAuth token lacks required scopes

**Solutions**:
- Add the user to the team in Teams
- Verify user has permission to post in the channel
- Check if the channel is moderated and adjust workflow accordingly
- Verify OAuth token includes `ChannelMessage.Send` scope
- Check user's Teams license and permissions

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
- Consider batching messages to reduce API calls

## Related Catalog Requests

- **[Send HTML Message to Teams Channel](SendHTMLMessageToTeamsChannel.md)** - Send HTML-formatted messages to channels
- **[Send Message to User](SendMessageToUser.md)** - Send 1:1 messages to individual users
- **[Send Message Teams Group](SendMessageTeamsGroup.md)** - Send messages to group chats
- **[Get All Teams](GetAllTeams.md)** - Retrieve list of all accessible teams
- **[Get Channels of Team](GetChannelsOfTeam.md)** - Get channels for a specific team
- **[Get Channel Messages](GetChannelMessages.md)** - Retrieve messages from a channel

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the Teams API extension
- [Authentication](Authentication.md) - OAuth 2.0 authentication setup
- [Creating Teams App](CreatingTeamsApp.md) - Azure AD application setup
- [Microsoft Graph API - Channel Resource](https://learn.microsoft.com/en-us/graph/api/resources/channel) - Official Microsoft documentation
- [Microsoft Teams Channels Overview](https://learn.microsoft.com/en-us/microsoftteams/teams-channels-overview) - Teams channels documentation

