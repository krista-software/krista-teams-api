# Send HTML Message Teams Group

## Overview

The **Send HTML Message Teams Group** catalog request enables you to send HTML-formatted messages to Microsoft Teams group chats programmatically. This operation allows you to create rich, visually appealing messages with formatting, links, tables, and other HTML elements through the Microsoft Graph API.

Group chats in Microsoft Teams are conversations between multiple users that are not tied to a specific team or channel. This catalog request provides advanced formatting capabilities for messages sent to these group conversations, allowing you to create professional, structured communications.

This operation is ideal for sending formatted reports, styled notifications, data tables, or any scenario where visual presentation enhances message clarity and impact.

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
| **Message** | RichText | Yes | The HTML-formatted message content to send. No maximum length enforced by validator, but Microsoft Graph API limits messages to ~28,000 characters. Supports standard HTML tags with security restrictions. |
| **Allow Retry** | Boolean | No | Enable retry prompt on validation failure (default: false). When true, shows interactive re-entry prompt; when false/null, returns immediate error. |

## Output Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| **Is Message Delivered** | Boolean | Indicates whether the HTML message was successfully delivered to the group chat. Returns `true` if successful, `false` otherwise. |

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

### Group Name Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Group name must be provided | "Group name is required" |
| **Minimum Length** | Must be at least 2 characters | "Group name is too short" |
| **Existence** | Group chat must exist in Teams | "The group name is either invalid or you are not authorized to access it" |
| **Access** | User must have access to the group chat | "You are not authorized to access this group chat" |

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

## Error Handling

### 1. Input Validation Errors

**Scenario**: Invalid group name or HTML message content

**Response**:
```
Is Message Delivered: false
```

**Common Causes**:
- Group name is empty or too short
- Group chat does not exist
- HTML message content is empty or exceeds 4000 characters
- HTML contains blocked tags (`<script>`, `<iframe>`)
- HTML contains only tags without meaningful text content
- User does not have access to the group chat

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

### Example 1: Send Formatted Status Report

**Input**:
```
Group Name: "Project Alpha Team"
Message: "<h2>Daily Status Report</h2>
<p><b>Date:</b> 2025-10-07</p>
<ul>
  <li><b>Completed:</b> User authentication module</li>
  <li><b>In Progress:</b> Database migration</li>
  <li><b>Blocked:</b> None</li>
</ul>
<p><i>Next standup: Tomorrow at 9:00 AM</i></p>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 2: Send Data Table

**Input**:
```
Group Name: "Sales Team"
Message: "<h3>Q4 Sales Summary</h3>
<table border='1'>
  <tr>
    <th>Region</th>
    <th>Revenue</th>
    <th>Growth</th>
  </tr>
  <tr>
    <td>North</td>
    <td>$1.2M</td>
    <td>+15%</td>
  </tr>
  <tr>
    <td>South</td>
    <td>$980K</td>
    <td>+8%</td>
  </tr>
</table>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 3: Send Alert with Formatting

**Input**:
```
Group Name: "IT Operations"
Message: "<div style='color: red;'>
<h2>⚠️ CRITICAL ALERT</h2>
<p><b>Server:</b> PROD-DB-01</p>
<p><b>Issue:</b> CPU usage exceeded 95%</p>
<p><b>Action Required:</b> Immediate investigation</p>
<hr>
<p><i>Alert generated at 14:35 UTC</i></p>
</div>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 4: Send Message with Links

**Input**:
```
Group Name: "Development Team"
Message: "<h3>Code Review Ready</h3>
<p>Pull request <b>#456</b> is ready for review:</p>
<p><a href='https://github.com/company/repo/pull/456'>View Pull Request</a></p>
<p>Changes include:</p>
<ul>
  <li>Bug fix for login timeout</li>
  <li>Performance improvements</li>
</ul>"
```

**Output**:
```
Is Message Delivered: true
```

---

### Example 5: Security Violation (Script Tag)

**Input**:
```
Group Name: "Test Group"
Message: "<p>Hello</p><script>alert('XSS')</script>"
```

**Output**:
```
Is Message Delivered: false
```

**Reason**: HTML contains blocked `<script>` tag

## Business Rules

1. **Group Chat Identification**: Group chats are identified by their display name. If multiple group chats have the same name, the first matching group chat will be used.

2. **HTML Rendering**: Microsoft Teams renders HTML content with its own styling. Some CSS properties may not be supported or may be overridden by Teams' default styles.

3. **Message Delivery**: Messages are delivered asynchronously. A `true` response indicates the message was accepted by the Microsoft Graph API, not that all participants have read it.

4. **Group Chat Membership**: Only users who are members of the group chat can send messages to it.

5. **Character Encoding**: Messages support Unicode characters, allowing international characters and emojis within HTML content.

6. **Security First**: All HTML content is validated for security before sending. Blocked elements will cause the entire message to fail validation.

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
  Failed to send HTML message to group 'GroupName'.
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
- **Group Name Matching**: Case-sensitive group name matching
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

- **Group Chat Access**: Can only send to group chats where the authenticated user is a member
- **No Delivery Confirmation**: No confirmation that recipients have read the message
- **No Message Editing**: Cannot edit or delete messages after sending
- **No Threading**: Messages are sent to the main conversation, not as replies

## Best Practices

### Do

- **Validate HTML Before Sending**: Test your HTML in a development environment first
- **Use Semantic HTML**: Use appropriate tags (`<h2>` for headings, `<ul>` for lists, etc.)
- **Keep HTML Simple**: Simpler HTML renders more consistently across Teams clients
- **Test Rendering**: Preview how your HTML appears in Teams before production use
- **Use Inline Styles**: Apply styles inline rather than relying on external CSS
- **Provide Alt Text**: Include alt attributes for images for accessibility
- **Handle Failures Gracefully**: Check the `Is Message Delivered` response and implement retry logic

### Don't

- **Don't Use Script Tags**: All JavaScript is blocked and will cause validation failure
- **Don't Use Iframes**: Iframe tags are blocked for security reasons
- **Don't Rely on Complex CSS**: Teams may not render advanced CSS properties
- **Don't Exceed Character Limit**: Keep total HTML under 4000 characters
- **Don't Send Sensitive Data**: Avoid including passwords or API keys in messages
- **Don't Assume Perfect Rendering**: HTML rendering may vary across Teams clients (desktop, web, mobile)

## Common Use Cases

### 1. Formatted Reports

Send daily, weekly, or monthly reports with structured data, tables, and formatting to team group chats.

**Example**: Sales reports, project status updates, performance metrics

---

### 2. Rich Notifications

Deliver visually distinct alerts and notifications with color coding, icons, and emphasis to grab attention.

**Example**: Critical system alerts, urgent action items, deadline reminders

---

### 3. Data Presentations

Share structured data in table format with headers, rows, and formatting for easy reading and analysis.

**Example**: Budget summaries, inventory reports, test results

---

### 4. Linked Content

Provide messages with clickable links to external resources, documents, or web applications for quick access.

**Example**: Pull request notifications, document approvals, ticket references

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

### Issue: Message Too Long

**Possible Causes**:
1. HTML content exceeds 4000 characters
2. Excessive HTML tags inflating message size

**Solutions**:
- Reduce HTML content length
- Simplify HTML structure
- Remove unnecessary tags and whitespace
- Split long messages into multiple shorter messages
- Use external links instead of embedding large content

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

- **[Send Message Teams Group](SendMessageTeamsGroup.md)** - Send plain text messages to group chats
- **[Send HTML Message to User](SendHTMLMessageToUser.md)** - Send HTML messages to individual users
- **[Send HTML Message to Teams Channel](SendHTMLMessageToTeamsChannel.md)** - Send HTML messages to team channels
- **[Get All Group Chats](GetAllGroupChats.md)** - Retrieve list of all accessible group chats
- **[Get Group Chat by Name](GetGroupChatByName.md)** - Get details of a specific group chat
- **[Get Group Chat Messages](GetGroupChatMessages.md)** - Retrieve messages from a group chat

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the Teams API extension
- [Authentication](Authentication.md) - OAuth 2.0 authentication setup
- [Creating Teams App](CreatingTeamsApp.md) - Azure AD application setup
- [Microsoft Graph API - Chat Resource](https://learn.microsoft.com/en-us/graph/api/resources/chat) - Official Microsoft documentation
- [Microsoft Teams Message Formatting](https://learn.microsoft.com/en-us/microsoftteams/platform/task-modules-and-cards/cards/cards-format) - Teams message formatting guide

