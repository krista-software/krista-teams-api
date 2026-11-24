# Send HTML Message to User

## Overview

The **Send HTML Message to User** catalog request sends a 1:1 HTML-formatted chat message to a specified user in Microsoft Teams via the Microsoft Graph API. This request allows you to send rich-formatted messages with HTML styling, including bold text, colors, links, lists, and more.

If a chat conversation doesn't exist with the user, it automatically creates a new 1:1 chat thread.

This request is ideal for sending formatted notifications, styled alerts, or rich content messages to individual team members.

---

## Required Microsoft Graph Permissions

This operation requires the following delegated permissions in your Azure AD application:

| Permission | Purpose | Status |
|------------|---------|--------|
| `User.Read.All` or `Directory.Read.All` | Look up users by email address | **REQUIRED** |
| `Chat.ReadWrite` or `Chat.Create` | Create new 1:1 chat conversations | **REQUIRED** |
| `ChatMessage.Send` | Send messages to chats | **REQUIRED** |

> **Important**: All permissions listed above are required for this operation to work. The `Chat.ReadWrite` permission includes chat creation capabilities, so `Chat.Create` is not strictly required when `Chat.ReadWrite` is present.

> **Current Behavior**: With required permissions:
> - ✅ HTML messages are sent successfully to users
> - ✅ New 1:1 chat conversations are created automatically if needed
> - ✅ The operation completes without errors

> **API Endpoints Used**:
> - `GET /users` with filter - Requires `User.Read.All` (required)
> - `GET /me/chats` with filter - Requires `Chat.ReadBasic` or `Chat.ReadWrite` (required)
> - `POST /chats/{chatId}/messages` - Requires `ChatMessage.Send` (required)

> **Troubleshooting**: If you receive a 403 Forbidden error, verify that these permissions are configured and admin consent has been granted. See [Creating Teams App](CreatingTeamsApp.md#step-7-configure-api-permissions) for setup instructions.

> **Documentation**: [Microsoft Graph - Send chat message](https://learn.microsoft.com/en-us/graph/api/chat-post-messages)

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
| **Message Content** | RichText (HTML) | Yes | - | The HTML content of the message to send. Must contain meaningful content. No maximum length enforced by validator, but Microsoft Graph API limits messages to ~28,000 characters. |
| **Reuse ChatId** | Boolean | Yes | false | Whether to reuse an existing chat if found, or create a new chat thread. |
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

### HTML Message Content Validation

| Rule | Description | Error Message |
|------|-------------|---------------|
| **Required** | Message cannot be null or empty | "HTML Message Content is required" |
| **Minimum Length** | Must be at least 1 character | "HTML message content is too short. Please provide meaningful HTML content." |
| **Not Whitespace** | Cannot contain only whitespace | "HTML message content cannot contain only whitespace. Please provide meaningful HTML content." |
| **No Script Tags** | Cannot contain `<script>` tags (security) | "HTML message content cannot contain script tags for security reasons. Please remove script tags." |
| **No Iframe Tags** | Cannot contain `<iframe>` tags (security) | "HTML message content cannot contain iframe tags for security reasons. Please remove iframe tags." |
| **Meaningful Content** | Must have text content after stripping HTML tags | "HTML message content must contain meaningful text content, not just HTML tags." |

**Note**: The extension does not enforce a maximum character limit. However, Microsoft Graph API will reject messages exceeding ~28,000 characters with a user-friendly error message.

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

## Security Validation

### Blocked HTML Elements

For security reasons, the following HTML elements are **not allowed**:

| Element | Reason | Alternative |
|---------|--------|-------------|
| `<script>` | JavaScript execution risk | Use static content only |
| `<iframe>` | Embedding external content risk | Use links instead |

### Allowed HTML Elements

The following HTML elements are commonly supported in Teams messages:

| Category | Elements | Example |
|----------|----------|---------|
| **Text Formatting** | `<b>`, `<strong>`, `<i>`, `<em>`, `<u>`, `<s>` | `<b>Bold text</b>` |
| **Headings** | `<h1>`, `<h2>`, `<h3>`, `<h4>`, `<h5>`, `<h6>` | `<h2>Section Title</h2>` |
| **Paragraphs** | `<p>`, `<br>`, `<hr>` | `<p>Paragraph text</p>` |
| **Lists** | `<ul>`, `<ol>`, `<li>` | `<ul><li>Item 1</li></ul>` |
| **Links** | `<a href="">` | `<a href="https://example.com">Link</a>` |
| **Tables** | `<table>`, `<tr>`, `<td>`, `<th>` | `<table><tr><td>Cell</td></tr></table>` |
| **Styling** | `<span style="">`, `<div style="">` | `<span style="color:red">Red text</span>` |
| **Images** | `<img src="">` | `<img src="https://example.com/image.png">` |

> **Note**: Teams may render HTML differently than web browsers. Test your HTML formatting in Teams before deploying.

---

## Error Handling

### 1. Input Validation Errors (Status Code: 400)

**When**: Invalid email format, user not found, invalid HTML content, or security violations

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
  "Response Message": "The provided email address and/or HTML message content are still invalid. Please try again with a new request."
}
```

### 2. Security Validation Errors (Status Code: 400)

**When**: HTML contains script tags or iframe tags

**Example Response**:
```json
{
  "Status Code": "400",
  "Message Id": "",
  "Chat Id": "",
  "Response Message": "HTML message content cannot contain script tags for security reasons. Please remove script tags."
}
```

### 3. Permission Errors (Status Code: 403)

**When**: Application lacks required Microsoft Graph API permissions

**Example Response**:
```json
{
  "Status Code": "403",
  "Message Id": "",
  "Chat Id": "",
  "Response Message": "Error: Insufficient permissions to send HTML message. The application needs 'User.Read.All' or 'Directory.Read.All' and 'Chat.Create' permissions in Microsoft Graph API. Please contact your administrator to grant these permissions."
}
```

**Required Permissions**:
- `User.Read.All` or `Directory.Read.All` (to look up user by email)
- `Chat.Create` (to create new chat if needed)
- `ChatMessage.Send` (to send the message)

### 4. System Errors (Status Code: 500)

**When**: Unexpected errors during message sending

**Example Response**:
```json
{
  "Status Code": "500",
  "Message Id": "",
  "Chat Id": "",
  "Response Message": "Error: Failed to send HTML message - Network timeout"
}
```

### 5. Success Response (Status Code: 200)

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
- Extension validates User Email and HTML Message Content
- If validation fails, stores validation state
- Returns confirmation prompt to user

### Step 2: Confirmation Prompt
- User is asked: "Do you want to re-enter the invalid fields?"
- User can choose to retry or cancel

### Step 3: Re-Entry (if user chooses to retry)
- User provides corrected values
- Extension re-validates the input
- If validation passes, HTML message is sent
- If validation fails again, returns permanent error (no further retries)

### Step 4: Final Result
- Success: Message sent with Message ID and Chat ID
- Failure: Error message with Status Code 400

---

## Usage Examples

### Example 1: Simple Formatted Message

**Input**:
```
User Email: john.doe@company.com
Message Content: <p>Hello <b>John</b>,</p><p>Your report is <span style="color:green">ready</span> for review.</p>
Reuse ChatId: false
Log Conversation: false
```

**Output**:
```
Status Code: 200
Message Id: 1234567890
Chat Id: 19:abcd1234-5678-90ef-ghij-klmnopqrstuv_chat
Response Message: Message sent successfully
```

**Rendered in Teams**:
> Hello **John**,
> 
> Your report is <span style="color:green">ready</span> for review.

### Example 2: Styled Alert Message

**Input**:
```
User Email: jane.smith@company.com
Message Content: <div style="background-color:#fff3cd;padding:10px;border-left:4px solid #ffc107"><h3 style="color:#856404">⚠️ Alert</h3><p>System CPU usage exceeded <b>90%</b> on server <code>prod-01</code></p></div>
Reuse ChatId: true
Log Conversation: true
```

**Output**:
```
Status Code: 200
Message Id: 9876543210
Chat Id: 19:existing-chat-id_chat
Response Message: Message sent successfully
```

### Example 3: List with Links

**Input**:
```
User Email: team.lead@company.com
Message Content: <h4>Action Items:</h4><ul><li><a href="https://portal.company.com/task/123">Review PR #123</a></li><li><a href="https://portal.company.com/task/124">Approve deployment</a></li><li>Update documentation</li></ul>
Reuse ChatId: false
Log Conversation: false
```

**Rendered in Teams**:
> #### Action Items:
> - [Review PR #123](https://portal.company.com/task/123)
> - [Approve deployment](https://portal.company.com/task/124)
> - Update documentation

### Example 4: Security Violation - Script Tag

**Input**:
```
User Email: user@company.com
Message Content: <p>Hello</p><script>alert('test')</script>
Reuse ChatId: false
Log Conversation: false
```

**Validation Error**:
```
HTML message content cannot contain script tags for security reasons. Please remove script tags.
```

**Behavior**: User is prompted to remove the script tag and re-enter valid HTML.

### Example 5: Table Format

**Input**:
```
User Email: manager@company.com
Message Content: <table border="1"><tr><th>Metric</th><th>Value</th></tr><tr><td>CPU</td><td style="color:red">92%</td></tr><tr><td>Memory</td><td style="color:green">45%</td></tr><tr><td>Disk</td><td style="color:orange">78%</td></tr></table>
Reuse ChatId: false
Log Conversation: false
```

**Rendered in Teams**:
| Metric | Value |
|--------|-------|
| CPU | <span style="color:red">92%</span> |
| Memory | <span style="color:green">45%</span> |
| Disk | <span style="color:orange">78%</span> |

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

### HTML Message Delivery

1. **Immediate Delivery**:
   - Messages are sent immediately upon successful validation
   - No queuing or delayed delivery

2. **HTML Rendering**:
   - Teams renders HTML using its own engine
   - Some HTML/CSS features may not be supported
   - Test formatting in Teams before production use

3. **Security Filtering**:
   - Script tags and iframes are blocked before sending
   - Prevents XSS and injection attacks
   - Ensures safe message delivery

---

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
  Failed to send HTML message to user 'user@example.com'.
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

| Limitation | Value | Notes |
|------------|-------|-------|
| **Maximum Message Length (Graph API)** | ~28,000 characters | Microsoft Graph API enforced limit |
| **Maximum Message Length (New Teams Client)** | 80,000-100,000 characters | Manual messages only, not via API |
| **Minimum Message Length** | 1 character | Must be meaningful content |
| **Email Length** | 5-254 characters | Standard email format limits |
| **Validation Retries** | 1 retry | Only one re-entry attempt allowed |

### HTML/CSS Limitations

1. **Limited CSS Support**: Teams doesn't support all CSS properties. Stick to basic styling.
2. **No JavaScript**: Script tags are blocked for security
3. **No Iframes**: Iframe tags are blocked for security
4. **Inline Styles Preferred**: External stylesheets are not supported
5. **Image Hosting**: Images must be publicly accessible URLs

### Functional Limitations

1. **Single Recipient**: Can only send to one user at a time
2. **No Attachments**: Cannot send file attachments with HTML messages
3. **No Adaptive Cards**: For Adaptive Cards, use separate Teams card functionality
4. **Rendering Variations**: HTML may render differently across Teams clients (desktop, web, mobile)

---

## Best Practices

### 1. HTML Content

**Do**:
- Use semantic HTML (`<h1>`, `<p>`, `<ul>`, etc.)
- Keep HTML simple and clean
- Test rendering in Teams before deployment
- Use inline styles for formatting
- Validate HTML before sending

**Don't**:
- Use complex CSS or JavaScript
- Embed scripts or iframes
- Use external stylesheets
- Create overly complex layouts
- Use deprecated HTML tags

### 2. Styling

**Do**:
- Use basic colors and fonts
- Keep styling minimal and professional
- Use standard HTML colors or hex codes
- Test on multiple Teams clients

**Don't**:
- Use advanced CSS features (flexbox, grid, animations)
- Rely on specific fonts (use web-safe fonts)
- Use absolute positioning
- Create responsive designs (Teams handles this)

### 3. Security

**Do**:
- Sanitize user-generated HTML content
- Validate all HTML before sending
- Use HTTPS for image URLs
- Escape special characters properly

**Don't**:
- Allow user input without validation
- Include script tags or event handlers
- Use data: URLs for images
- Embed sensitive information in HTML

### 4. Performance

**Do**:
- Keep HTML concise (under 2000 characters recommended)
- Optimize image sizes and use CDNs
- Batch multiple messages when possible
- Monitor API rate limits

**Don't**:
- Send very large HTML messages
- Include large inline images (use URLs)
- Send messages in tight loops
- Ignore rate limit errors

---

## Common Use Cases

### 1. Formatted Notifications

**Scenario**: Send styled workflow completion notifications

**Implementation**:
```html
<div style="border-left:4px solid #28a745;padding:10px;background-color:#d4edda">
  <h4 style="color:#155724">✅ Workflow Complete</h4>
  <p>Your workflow <b>[workflow name]</b> has completed successfully.</p>
  <p><a href="[workflow URL]">View Results</a></p>
</div>
```

### 2. Alert Messages with Severity

**Scenario**: Send color-coded alerts

**Implementation**:
```html
<div style="border-left:4px solid #dc3545;padding:10px;background-color:#f8d7da">
  <h4 style="color:#721c24">🚨 Critical Alert</h4>
  <p>System CPU usage exceeded <b>90%</b> on server <code>prod-01</code></p>
  <p><b>Action Required:</b> Investigate immediately</p>
</div>
```

### 3. Task Lists

**Scenario**: Send formatted task assignments

**Implementation**:
```html
<h4>📋 Your Tasks for Today</h4>
<ol>
  <li><b>Review PR #123</b> - Due: 2:00 PM</li>
  <li><b>Approve deployment</b> - Due: 4:00 PM</li>
  <li><b>Update documentation</b> - Due: EOD</li>
</ol>
<p><a href="[task portal URL]">View All Tasks</a></p>
```

### 4. Status Reports

**Scenario**: Send formatted status updates

**Implementation**:
```html
<h3>Project Status Update</h3>
<table border="1" cellpadding="5">
  <tr><th>Phase</th><th>Status</th><th>Progress</th></tr>
  <tr><td>Design</td><td style="color:green">✅ Complete</td><td>100%</td></tr>
  <tr><td>Development</td><td style="color:orange">🔄 In Progress</td><td>75%</td></tr>
  <tr><td>Testing</td><td style="color:gray">⏳ Pending</td><td>0%</td></tr>
</table>
```

---

## Troubleshooting

### Issue: HTML not rendering as expected

**Cause**: Teams has limited HTML/CSS support

**Resolution**:
1. Simplify HTML structure
2. Use only basic HTML tags
3. Use inline styles instead of classes
4. Test in Teams before deployment
5. Refer to Teams HTML support documentation

### Issue: Script tag validation error

**Cause**: HTML contains `<script>` tags (security violation)

**Resolution**:
1. Remove all `<script>` tags from HTML
2. Use static content only
3. Move dynamic logic to workflow/backend
4. Re-submit with clean HTML

### Issue: Message appears as plain text

**Cause**: HTML not properly formatted or Teams client issue

**Resolution**:
1. Verify HTML is valid
2. Check for unclosed tags
3. Ensure proper HTML structure
4. Test on different Teams clients
5. Try sending plain text first to verify connectivity

### Issue: Images not displaying

**Cause**: Image URLs not accessible or invalid

**Resolution**:
1. Verify image URLs are publicly accessible
2. Use HTTPS URLs only
3. Check image file size (keep under 1MB)
4. Test image URL in browser first
5. Use CDN-hosted images for reliability

---

## Related Catalog Requests

- **[Send Message to User](SendMessageToUser.md)** - Send plain text messages to users
- **[Send HTML Message to Teams Channel](SendHTMLMessageToTeamsChannel.md)** - Send HTML messages to channels
- **[Send Message Teams Group](SendMessageTeamsGroup.md)** - Send messages to group chats
- **[Get Group Chat Messages](GetGroupChatMessages.md)** - Retrieve messages from group chats

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the extension
- [Authentication](Authentication.md) - OAuth 2.0 authentication details
- [Creating Teams App](CreatingTeamsApp.md) - Set up Azure AD application
- [Microsoft Graph Chat API](https://docs.microsoft.com/en-us/graph/api/resources/chat) - Official API documentation
- [Teams Message Formatting](https://docs.microsoft.com/en-us/microsoftteams/platform/task-modules-and-cards/cards/cards-format) - HTML formatting guide

