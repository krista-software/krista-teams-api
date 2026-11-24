# Send HTML Message to Teams Channel

## Overview

The **Send HTML Message to Teams Channel** catalog request enables you to send HTML-formatted messages to Microsoft Teams channels programmatically. This operation allows you to create rich, visually appealing messages with formatting, links, tables, and other HTML elements through the Microsoft Graph API.

Channels in Microsoft Teams are dedicated sections within a team where conversations, files, and tools are organized around specific topics or projects. This catalog request provides advanced formatting capabilities for messages posted to these channels, allowing you to create professional, structured communications that are visible to all channel members.

This operation is ideal for sending formatted reports, styled announcements, data tables, or any scenario where visual presentation enhances message clarity and impact for team-wide communications.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `Team.ReadBasic.All` | Access team information | **REQUIRED** |
| `Channel.ReadBasic.All` | Access channel information | **REQUIRED** |
| `ChannelMessage.Send` | Send messages to channels | **REQUIRED** |

> **Important**: All permissions listed above are required for this operation to work.

> **Current Behavior**: With required permissions:
> - ✅ HTML messages are sent successfully to team channels
> - ✅ The operation completes without errors

> **API Endpoints Used**:
> - `GET /me/joinedTeams` - Requires `Team.ReadBasic.All` (required)
> - `GET /teams/{teamId}/channels` - Requires `Channel.ReadBasic.All` (required)
> - `POST /teams/{teamId}/channels/{channelId}/messages` - Requires `ChannelMessage.Send` (required)

> **Troubleshooting**: If you receive a 403 Forbidden error, verify that these permissions are configured and admin consent has been granted. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **Documentation**: [Microsoft Graph - Send channel message](https://learn.microsoft.com/en-us/graph/api/channel-post-messages)

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
| **Message** | RichText | Yes | The HTML-formatted message content to send. No maximum length enforced by validator, but Microsoft Graph API limits messages to ~28,000 characters. Supports standard HTML tags with security restrictions. |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| **Is Message Delivered** | Boolean | Indicates whether the HTML message was successfully delivered to the channel. Returns `true` if successful, `false` otherwise. |

## Security Validation

### Blocked HTML Elements

For security reasons, the following HTML elements are **not allowed** and will cause validation to fail:

| Element | Reason | Example |
|---------|--------|---------|
| `<script>` | Prevents JavaScript execution | `<script>alert('XSS')</script>` |
| `<iframe>` | Prevents embedding external content | `<iframe src="malicious.com"></iframe>` |

**Error Message**: "HTML message content cannot contain script/iframe tags for security reasons. Please remove script/iframe tags."

---

### Allowed HTML Elements

The following HTML elements are supported and can be used to format your messages:

| Category | Elements | Example Usage |
|----------|----------|---------------|
| **Text Formatting** | `<b>`, `<strong>`, `<i>`, `<em>`, `<u>`, `<s>` | `<b>Bold text</b>`, `<i>Italic text</i>` |
| **Headings** | `<h1>`, `<h2>`, `<h3>`, `<h4>`, `<h5>`, `<h6>` | `<h2>Section Title</h2>` |
| **Paragraphs** | `<p>`, `<br>`, `<hr>` | `<p>Paragraph text</p><br>` |
| **Lists** | `<ul>`, `<ol>`, `<li>` | `<ul><li>Item 1</li><li>Item 2</li></ul>` |
| **Links** | `<a href="...">` | `<a href="https://example.com">Click here</a>` |
| **Tables** | `<table>`, `<tr>`, `<td>`, `<th>` | `<table><tr><td>Cell</td></tr></table>` |
| **Containers** | `<div>`, `<span>` | `<div>Content block</div>` |
| **Images** | `<img src="...">` | `<img src="https://example.com/logo.png">` |

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

### HTML Message Content Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Message content must be provided | "HTML message content is required" |
| **Not Empty** | Message cannot be only whitespace | "HTML message content cannot contain only whitespace" |
| **Minimum Length** | Must be at least 1 character | "HTML message content is too short" |
| **No Script Tags** | Cannot contain `<script>` tags | "HTML message content cannot contain script tags for security reasons" |
| **No Iframe Tags** | Cannot contain `<iframe>` tags | "HTML message content cannot contain iframe tags for security reasons" |
| **Meaningful Content** | Must contain text content after stripping HTML tags | "HTML message content must contain meaningful text content, not just HTML tags" |

**Note**: The extension does not enforce a maximum character limit. However, Microsoft Graph API will reject messages exceeding ~28,000 characters with a user-friendly error message.

## Validation Re-Entry Flow

When validation fails for team name, channel name, or HTML message content, the extension provides an opportunity to correct the input:

1. **Initial Validation**: The extension validates team name, channel name, and HTML message content
2. **Validation Failure**: If validation fails, a confirmation subcatalog request is triggered
3. **User Choice**: User can choose to re-enter the invalid data or cancel the operation
4. **Re-Validation**: If user chooses to re-enter, the new data is validated again
5. **Final Result**: If re-validation passes, the message is sent; otherwise, an error is returned

This flow ensures data quality while providing flexibility to correct mistakes without starting over.

## Error Handling

### 1. Input Validation Errors

**Scenario**: Invalid team name, channel name, or HTML message content

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- Team name is empty or too short
- Team does not exist
- Channel name is empty
- Channel does not exist in the specified team
- HTML message content is empty or exceeds 4000 characters
- HTML contains blocked tags (`<script>`, `<iframe>`)
- HTML contains only tags without meaningful text content
- User does not have access to the team or channel

---

### 2. Security Validation Errors

**Scenario**: HTML content contains blocked security elements

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- Message contains `<script>` tags
- Message contains `<iframe>` tags
- Attempting to inject malicious code

**Solution**: Remove all `<script>` and `<iframe>` tags from your HTML content.

---

### 3. Permission Errors

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

### 4. System Errors

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

### 5. Success Response

**Scenario**: HTML message successfully delivered

**Response**:
```
Is Message Delivered: true
```

## Usage Examples

### Example 1: Send Deployment Report

**Input**:
```
Team Name: "Engineering"
Channel Name: "Deployments"
Message: "<h2>🚀 Production Deployment Report</h2>
<p><b>Version:</b> 2.5.0</p>
<p><b>Deployed:</b> 2025-10-07 14:30 UTC</p>
<p><b>Status:</b> <span style='color: green;'>SUCCESS</span></p>
<hr>
<h3>Changes Included:</h3>
<ul>
  <li>Bug fix for login timeout issue</li>
  <li>Performance improvements in API layer</li>
  <li>Updated dependencies to latest versions</li>
</ul>
<p><i>Deployment completed in 12 minutes</i></p>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 2: Send Performance Metrics Table

**Input**:
```
Team Name: "DevOps"
Channel Name: "Monitoring"
Message: "<h3>📊 Daily Performance Metrics</h3>
<table border='1' cellpadding='5'>
  <tr>
    <th>Metric</th>
    <th>Current</th>
    <th>Target</th>
    <th>Status</th>
  </tr>
  <tr>
    <td>API Response Time</td>
    <td>145ms</td>
    <td>&lt;200ms</td>
    <td>✅ Good</td>
  </tr>
  <tr>
    <td>Error Rate</td>
    <td>0.02%</td>
    <td>&lt;0.1%</td>
    <td>✅ Good</td>
  </tr>
  <tr>
    <td>Uptime</td>
    <td>99.98%</td>
    <td>&gt;99.9%</td>
    <td>✅ Good</td>
  </tr>
</table>
<p><i>Report generated at 08:00 UTC</i></p>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 3: Send Critical Alert

**Input**:
```
Team Name: "IT Operations"
Channel Name: "Alerts"
Message: "<div style='border: 2px solid red; padding: 10px;'>
<h2 style='color: red;'>🚨 CRITICAL ALERT</h2>
<p><b>Server:</b> PROD-DB-01</p>
<p><b>Issue:</b> CPU usage exceeded 95% for 5 minutes</p>
<p><b>Impact:</b> Potential service degradation</p>
<p><b>Action Required:</b> Immediate investigation</p>
<hr>
<p><b>Alert Time:</b> 2025-10-07 14:35 UTC</p>
<p><a href='https://monitoring.company.com/alerts/12345'>View in Monitoring Dashboard</a></p>
</div>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 4: Send Sprint Summary

**Input**:
```
Team Name: "Product Development"
Channel Name: "Sprint Updates"
Message: "<h2>Sprint 24 Summary</h2>
<p><b>Sprint Period:</b> Oct 1-14, 2025</p>
<h3>Completed Stories:</h3>
<ol>
  <li><b>USER-123:</b> Implement user profile page</li>
  <li><b>USER-124:</b> Add password reset functionality</li>
  <li><b>USER-125:</b> Improve search performance</li>
</ol>
<h3>Metrics:</h3>
<ul>
  <li><b>Velocity:</b> 42 story points</li>
  <li><b>Completion Rate:</b> 95%</li>
  <li><b>Bugs Fixed:</b> 8</li>
</ul>
<p><i>Great work team! 🎉</i></p>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 5: Security Violation (Script Tag)

**Input**:
```
Team Name: "Engineering"
Channel Name: "General"
Message: "<p>Hello Team</p><script>alert('XSS')</script>"
```

**Output**:
```
Is Message Delivered: false
```

**Reason**: HTML contains blocked `<script>` tag

## Business Rules

1. **Channel Visibility**: Messages sent to channels are visible to all channel members. Consider the audience before sending sensitive information.

2. **HTML Rendering**: Microsoft Teams renders HTML content with its own styling. Some CSS properties may not be supported or may be overridden by Teams' default styles.

3. **Message Delivery**: Messages are delivered asynchronously. A `true` response indicates the message was accepted by the Microsoft Graph API, not that all members have read it.

4. **Team and Channel Membership**: Users must be members of the team and have access to the channel to send messages.

5. **Character Encoding**: Messages support Unicode characters, allowing international characters and emojis within HTML content.

6. **Security First**: All HTML content is validated for security before sending. Blocked elements will cause the entire message to fail validation.

7. **General Channel**: Every team has a "General" channel that cannot be deleted. This is often the default channel for team-wide announcements.

## Limitations

### Message Length Limits - Important Information

#### Microsoft Teams Message Limits (Updated 2024+)

**Historical Limit**:
- Microsoft historically documented the Teams message limit as **28,000 characters** for both chat and channel messages.

**Current Situation (2024+)**:
- **New Teams Client** (Teams 2.0, based on WebView2 + React) has quietly increased this limit in many tenants
- Many users report successfully sending messages between **80,000–100,000 characters** without error
- The limit increase applies primarily to the **new Teams desktop and web clients**

**Microsoft Graph API Limit**:
- The **Graph API still enforces the 28,000-character limit** in most cases
- This limit especially applies to:
  - App bots and automation integrations
  - Programmatic message sending (like this extension)
  - Connectors and webhooks

**What This Means for You**:
- **Manual Teams Messages**: Users can send very long messages (80K-100K chars) in new Teams clients
- **Automated Messages (This Extension)**: Limited to ~28,000 characters via Graph API
- **Error Detection**: Extension now detects message-too-large errors and provides clear guidance

**Extension Behavior**:
- **No Validator Limit**: Extension validators do not enforce a maximum character limit
- **Teams API Enforces**: Microsoft Graph API will reject messages exceeding ~28,000 characters
- **User-Friendly Errors**: When Teams API rejects a message, you'll see:
  ```
  Failed to send HTML message to channel 'ChannelName' in team 'TeamName'.
  The message is too large (35,000 characters).
  Microsoft Teams has a limit of approximately 28,000 characters for messages.
  Please reduce the message size and try again.
  ```

**Recommendations**:
- Keep messages under **28,000 characters** for reliable delivery via Graph API
- For longer content, consider:
  - Splitting into multiple messages
  - Using file attachments
  - Providing links to external content
  - Using Teams Adaptive Cards for structured data

---

### Technical Limitations

- **Maximum Message Length (Graph API)**: ~28,000 characters (Microsoft Graph API enforced limit)
- **Maximum Message Length (New Teams Client)**: 80,000-100,000 characters (manual messages only, not via API)
- **Rate Limiting**: Subject to Microsoft Graph API rate limits (varies by tenant)
- **Team/Channel Name Matching**: Case-sensitive name matching
- **No Attachments**: Cannot send file attachments
- **Limited CSS Support**: Teams may not render all CSS properties
- **No JavaScript**: All JavaScript code is blocked for security

### HTML/CSS Limitations

- **Inline Styles Only**: External stylesheets are not supported
- **Limited CSS Properties**: Not all CSS properties are rendered by Teams
- **No Custom Fonts**: Font families may be overridden by Teams defaults
- **Image Hosting**: Images must be hosted externally and accessible via HTTPS
- **No Forms**: HTML form elements (`<input>`, `<button>`, etc.) are not interactive
- **No Embedded Media**: Cannot embed videos or audio directly

### Functional Limitations

- **Channel Access**: Can only send to channels where the authenticated user is a member
- **No Delivery Confirmation**: No confirmation that members have read the message
- **No Message Editing**: Cannot edit or delete messages after sending
- **No Threading**: Messages are sent to the main channel conversation, not as replies
- **Moderated Channels**: May require approval in moderated channels

## Best Practices

### Do

- **Validate HTML Before Sending**: Test your HTML in a development environment first
- **Use Semantic HTML**: Use appropriate tags (`<h2>` for headings, `<ul>` for lists, etc.)
- **Keep HTML Simple**: Simpler HTML renders more consistently across Teams clients
- **Test Rendering**: Preview how your HTML appears in Teams before production use
- **Use Inline Styles**: Apply styles inline rather than relying on external CSS
- **Provide Alt Text**: Include alt attributes for images for accessibility
- **Handle Failures Gracefully**: Check the `Is Message Delivered` response and implement retry logic
- **Use Appropriate Channels**: Send messages to the most relevant channel for the content

### Don't

- **Don't Use Script Tags**: All JavaScript is blocked and will cause validation failure
- **Don't Use Iframes**: Iframe tags are blocked for security reasons
- **Don't Rely on Complex CSS**: Teams may not render advanced CSS properties
- **Don't Exceed Character Limit**: Keep total HTML under 4000 characters
- **Don't Send Sensitive Data**: Avoid including passwords or API keys in channel messages
- **Don't Assume Perfect Rendering**: HTML rendering may vary across Teams clients (desktop, web, mobile)
- **Don't Spam Channels**: Avoid sending excessive formatted messages that clutter the channel

## Common Use Cases

### 1. Automated Reports

Send daily, weekly, or monthly reports with structured data, tables, and formatting to team channels.

**Example**: Build reports, test results, performance metrics, deployment summaries

---

### 2. Rich Announcements

Deliver visually distinct announcements with color coding, icons, and emphasis to grab team attention.

**Example**: Product launches, policy changes, important deadlines, team achievements

---

### 3. Data Dashboards

Share structured data in table format with headers, rows, and formatting for easy reading and analysis.

**Example**: Sales figures, project status, resource utilization, budget tracking

---

### 4. Linked Notifications

Provide messages with clickable links to external resources, dashboards, or documentation for quick access.

**Example**: CI/CD pipeline results, monitoring alerts, ticket references, documentation updates

## Troubleshooting

### Issue: HTML Not Rendering Correctly

**Possible Causes**:
1. Unsupported HTML tags or CSS properties
2. Complex nested HTML structure
3. Teams client version differences

**Solutions**:
- Simplify HTML structure
- Use only supported HTML tags (see Allowed HTML Elements section)
- Test in multiple Teams clients (desktop, web, mobile)
- Use inline styles instead of CSS classes
- Avoid deeply nested HTML elements

---

### Issue: Message Validation Failed (Security)

**Possible Causes**:
1. HTML contains `<script>` tags
2. HTML contains `<iframe>` tags
3. Attempting to inject executable code

**Solutions**:
- Remove all `<script>` tags from HTML
- Remove all `<iframe>` tags from HTML
- Use only allowed HTML elements
- Validate HTML before sending

---

### Issue: Team or Channel Not Found

**Possible Causes**:
1. Team or channel name is misspelled
2. Team or channel has been deleted or archived
3. User does not have access to the team or channel
4. Case sensitivity mismatch in names

**Solutions**:
- Use the "Get All Teams" catalog request to list available teams
- Use the "Get Channels of Team" catalog request to list channels
- Verify the exact team and channel names (case-sensitive)
- Ensure the user is a member of the team
- Check if the team or channel still exists in Teams

---

### Issue: Images Not Displaying

**Possible Causes**:
1. Image URL is not accessible
2. Image requires authentication
3. Image URL uses HTTP instead of HTTPS

**Solutions**:
- Ensure image URLs are publicly accessible
- Use HTTPS URLs for images
- Host images on a reliable CDN or server
- Verify image URLs are not blocked by firewalls
- Test image URLs in a browser first

## Related Catalog Requests

- **[Send Message to Teams Channel](SendMessageToTeamsChannel.md)** - Send plain text messages to channels
- **[Send HTML Message to User](SendHTMLMessageToUser.md)** - Send HTML messages to individual users
- **[Send HTML Message Teams Group](SendHTMLMessageTeamsGroup.md)** - Send HTML messages to group chats
- **[Get All Teams](GetAllTeams.md)** - Retrieve list of all accessible teams
- **[Get Channels of Team](GetChannelsOfTeam.md)** - Get channels for a specific team
- **[Get Channel Messages](GetChannelMessages.md)** - Retrieve messages from a channel

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the Teams API extension
- [Authentication](Authentication.md) - OAuth 2.0 authentication setup
- [Creating Teams App](CreatingTeamsApp.md) - Azure AD application setup
- [Microsoft Graph API - Channel Resource](https://learn.microsoft.com/en-us/graph/api/resources/channel) - Official Microsoft documentation
- [Microsoft Teams Message Formatting](https://learn.microsoft.com/en-us/microsoftteams/platform/task-modules-and-cards/cards/cards-format) - Teams message formatting guide

