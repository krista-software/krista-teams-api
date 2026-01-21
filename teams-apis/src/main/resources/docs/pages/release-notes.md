# Release Notes - Krista Microsoft Teams-APIs Extension

---

## Version 2.0.8 (Current)

**Release Date**: January 2025
**Developer**: Vrushali Gaikwad
**Krista Service APIs Java**: 1.0.115
**Global Catalog Version**: GC-2025.11.5

### New Features

#### Allow Retry Parameter - Enhanced Validation Control

Added a new **Allow Retry** boolean parameter to all 14 catalog request methods, providing fine-grained control over validation retry behavior.

**Impacted Catalog Requests** (14 total):
1. Get Channels of Team
2. Send Message Teams Group
3. Send HTML Message Teams Group
4. Send Message to Teams Channel
5. Get Channel Messages
6. Get Group Chat Messages
7. Get Group Chat by Name
8. Get All Teams
9. Get All Group Chats
10. Get Channel Message Replies
11. Get Channels of Team by Team ID
12. Send Message to User
13. Send HTML Message to User
14. Send HTML Message to Teams Channel

**Parameter Details**:
- **Type**: Boolean (true/false)
- **Required**: No (optional)
- **Default**: false
- **Behavior**:
  - `true` → Shows interactive re-entry prompt on validation failure
  - `false/null` → Returns immediate error without retry prompt

**Benefits**:
- **User-Driven Flows**: Enable interactive retry for user-facing operations
- **Automated Workflows**: Disable retry prompts for batch/automated jobs
- **Backward Compatible**: Default behavior (false) maintains existing functionality
- **Consistent API**: Uniform parameter across all catalog requests

### Improvements

**Telemetry Integration**:
- All catalog requests log `allowRetry` parameter value
- Enhanced logging for retry vs. direct error paths
- Improved debugging and monitoring capabilities

#### HTML Message Length Validation Removed

**Updated HTML Message Validators**:
- **Previous Limit**: Validators enforced 4,000-20,000 character limits
- **New Behavior**: **No maximum length validation** in extension validators
- **Reason**: Microsoft Teams limits have evolved, and Graph API enforces its own limits
- **Teams API Limit**: ~28,000 characters (enforced by Microsoft Graph API)

**Important Information - Microsoft Teams Message Limits (2024+)**:

**Historical Context**:
- Microsoft historically documented the Teams message limit as **28,000 characters**

**Current Situation (2024+)**:
- **New Teams Client** (Teams 2.0, WebView2 + React) has quietly increased limits in many tenants
- Users report successfully sending **80,000–100,000 character messages** in new Teams clients
- Limit increase applies to **manual messages** in new Teams desktop and web clients

**Microsoft Graph API Reality**:
- **Graph API still enforces ~28,000 character limit** for programmatic access
- This affects:
  - App bots and automation integrations (like this extension)
  - Connectors and webhooks
  - All programmatic message sending

**Extension Behavior**:
- **No Validator Limit**: Extension does not enforce maximum character limits
- **API Enforces Limit**: Microsoft Graph API rejects messages > ~28,000 characters
- **User-Friendly Errors**: Clear error messages when API limit exceeded:
  ```
  Failed to send HTML message to user 'user@example.com'.
  The message is too large (35,000 characters).
  Microsoft Teams has a limit of approximately 28,000 characters for messages.
  Please reduce the message size and try again.
  ```

**Impacted Operations**:
- Send HTML Message to User
- Send HTML Message to Teams Channel
- Send HTML Message Teams Group

**Benefits**:
- No artificial validator limits blocking valid messages
- Clear, actionable error messages when Teams API limit exceeded
- Character count logged for debugging
- Future-proof as Teams limits may continue to evolve

**Recommendations**:
- Keep messages under **28,000 characters** for reliable Graph API delivery
- For longer content: split messages, use attachments, or provide links

### Documentation Updates

**Updated Documentation Files** (11 total):
1. SendMessageToUser.md
2. SendHTMLMessageToUser.md
3. SendHTMLMessageToTeamsChannel.md
4. SendHTMLMessageTeamsGroup.md
5. SendMessageToTeamsChannel.md
6. SendMessageTeamsGroup.md
7. GetChannelMessages.md
8. GetGroupChatMessages.md
9. GetGroupChatByName.md
10. GetChannelMessageReplies.md
11. GetChannelsOfTeam.md
12. GetChannelsOfTeamByTeamID.md

**Documentation Additions**:
- **Input Parameters Table**: Added Allow Retry parameter row
- **Parameter Details Section**: Comprehensive Allow Retry documentation
- **Usage Examples**: Updated with Allow Retry values
- **Business Rules**: Added retry behavior rules
- **Best Practices**: Added Allow Retry usage guidelines

### Migration Guide

### Known Limitations

- **SendHTMLMessageTeamsGroup**: Does not use validation helper, so HTML limit remains at 4,000 characters
  - **Reason**: Direct service call without validation framework
  - **Workaround**: Use Send HTML Message to Teams Channel or Send HTML Message to User for larger messages
  - **Future**: May be enhanced in future release

---

## Version 2.0.6

**Release Date**: October 2025
**Developer**: Samruddhi Pisal
**Krista Service APIs Java**: 1.0.115
Global Catalog Version : GC-2025.10.2

### Major Updates

#### Complete Documentation Rewrite
- **19 comprehensive documentation files** created following ExtensionDocumentationGuidelines.md standards
- **~64,000 words** of professional, production-ready documentation
- **100% code-verified** accuracy across all catalog requests

#### Documentation Structure
- **Getting Started Documentation** (5 files)
  - README.md - Main landing page with overview and navigation
  - _sidebar.md - Organized navigation sidebar
  - ExtensionConfiguration.md - Configuration parameters and setup (~1,800 words)
  - Authentication.md - OAuth 2.0 authentication guide (~2,500 words)
  - CreatingTeamsApp.md - Azure AD app registration guide (~2,000 words)

- **Messaging Operations** (6 files)
  - SendMessageToUser.md - Send plain text 1:1 messages (~3,500 words)
  - SendHTMLMessageToUser.md - Send HTML 1:1 messages (~4,000 words)
  - SendMessageTeamsGroup.md - Send plain text to group chats (~3,200 words)
  - SendHTMLMessageTeamsGroup.md - Send HTML to group chats (~4,200 words)
  - SendMessageToTeamsChannel.md - Send plain text to channels (~3,400 words)
  - SendHTMLMessageToTeamsChannel.md - Send HTML to channels (~4,500 words)

- **Query Operations** (5 files)
  - GetAllTeams.md - Retrieve all accessible teams (~3,800 words)
  - GetChannelsOfTeam.md - Get channels by team name (~4,200 words)
  - GetChannelsOfTeamByTeamID.md - Get channels by team ID (~4,500 words)
  - GetChannelMessages.md - Retrieve channel messages with date filtering (~5,200 words)
  - GetGroupChatMessages.md - Retrieve group chat messages (~4,800 words)

- **Advanced Operations** (3 files)
  - GetChannelMessageReplies.md - Retrieve replies to channel messages (~4,800 words)
  - GetGroupChatByName.md - Find group chat by name (~4,200 words)
  - GetAllGroupChats.md - Retrieve all group chats (~3,600 words)

#### Documentation Features
- **15-section structure** per catalog request page:
  1. Overview
  2. Request Details (Area, Type, Retry Support)
  3. Input Parameters (comprehensive tables)
  4. Output Parameters (with entity structures)
  5. Validation Rules (code-verified)
  6. Validation Re-Entry Flow (where applicable)
  7. Error Handling (INPUT_ERROR, LOGIC_ERROR, SYSTEM_ERROR)
  8. Usage Examples (3-5 real-world examples)
  9. Business Rules
  10. Limitations (Technical and Functional)
  11. Best Practices (Do/Don't lists)
  12. Common Use Cases (4 scenarios per page)
  13. Troubleshooting (4-5 issues with solutions)
  14. Related Catalog Requests (cross-referenced)
  15. See Also (links to getting started docs)

- **~60 usage examples** across all catalog requests
- **~60 common use cases** documented
- **~60 troubleshooting scenarios** with solutions
- **Comprehensive cross-referencing** between all pages
- **No internal implementation details exposed** (no catalog IDs)

#### Image Standardization
- **18 images renamed** to follow `<MD_FILE_NAME>_<IMAGE_NAME>.png` convention
- **100% naming convention compliance**
- All image references updated in documentation files

#### Legacy Documentation Cleanup
- **4 legacy files removed**:
  - supportedCatalogRequests.md (replaced by 14 individual catalog request pages)
  - overview.md (replaced by comprehensive README.md)
  - connectingWithTeams-APIsExtension.md (replaced by ExtensionConfiguration.md)
  - obtainingClientIDClientSecret.md (replaced by CreatingTeamsApp.md)

### Fixed Issues

- **Documentation Accuracy**: All validation rules, error messages, and parameters verified against source code
- **Image Naming**: All images now follow standard naming convention
- **Navigation**: Improved sidebar organization with clear functional categories
- **Cross-References**: All catalog request pages properly cross-referenced

### Documentation Coverage

- **14 catalog requests** fully documented (100% coverage)
- **10 validation patterns** documented with re-entry flows
- **6 entity structures** documented (Team, Channel, Member, ChannelMessage, GroupChat, Replies)
- **All error scenarios** documented with resolution steps

### Known Issues

- None reported

### Limitations

- Microsoft Graph APIs for Teams applications do not support sending messages to chat groups in draft mode
- HTML messages have security restrictions (no `<script>` or `<iframe>` tags allowed)
- Channel messages limited to 50 messages per request
- Message content limited to 4,000 characters
- Date range queries limited to reasonable time periods to prevent performance issues

---

## Version 2.0.4

**Release Date**: September 2025
**Developer**: Samruddhi Pisal
**Krista Service APIs Java**: 1.0.114
**Global Catalog Version**: 1.0.58
**Appliance Release Version**: 3.5.0

### New Features

- **Enhanced Error Handling**: Improved validation error messages across all catalog requests
- **Telemetry Integration**: Added comprehensive telemetry tracking for all operations
- **Performance Optimization**: Optimized message retrieval operations

### Fixed Issues

- Fixed issue with HTML message validation not properly detecting blocked tags
- Resolved date range validation edge cases in message retrieval
- Fixed channel ID validation for special characters
- Improved error handling for expired OAuth tokens

### Improvements

- Enhanced validation re-entry flows for better user experience
- Improved logging for troubleshooting
- Better handling of Microsoft Graph API rate limits
- Optimized subcatalog request flows

### Known Issues

- Documentation needs comprehensive update (addressed in 2.0.4)

### Limitations

- Microsoft Graph APIs for Teams applications do not support sending messages to chat groups in draft mode
- Message content limited to 4,000 characters

---

## Version 2.0.3

**Release Date**: August 2025
**Developer**: Samruddhi Pisal
**Krista Service APIs Java**: 1.0.113
**Global Catalog Version**: 1.0.57
**Appliance Release Version**: 3.4.9

### New Features

- **New Catalog Requests**:
  - Get Channel Message Replies - Retrieve conversation threads
  - Get Group Chat by Name - Find group chats by topic name
  - Get All Group Chats - List all accessible group chats

- **Validation Framework**: Implemented comprehensive validation framework
  - Email validation with user existence check
  - Message content validation (length, format, spam detection)
  - HTML security validation
  - Date range validation
  - Channel/Team ID validation

### Fixed Issues

- Fixed issue with group chat message retrieval
- Resolved authentication token refresh issues
- Fixed channel message pagination
- Improved error handling for invalid user emails

### Improvements

- Added helper classes for validation and error handling
- Implemented validation re-entry flows for better user experience
- Enhanced security for HTML message content
- Improved Microsoft Graph API error handling

### Known Issues

- Documentation incomplete for new catalog requests (addressed in 2.0.4)

### Limitations

- Microsoft Graph APIs for Teams applications do not support sending messages to chat groups in draft mode
- HTML messages restricted to safe tags only (no scripts or iframes)

---

## Support & Resources

### Documentation
- **Getting Started**: [Extension Configuration](ExtensionConfiguration.md), [Authentication](Authentication.md), [Creating Teams App](CreatingTeamsApp.md)
- **Catalog Requests**: See sidebar for complete list of 14 catalog requests

### Contact
- **Developer**: Samruddhi Pisal
- **Support**: Refer to Krista support documentation

### Version History
- **2.0.7** (Current) - Allow Retry parameter, HTML message length increase
- **2.0.6** - Complete documentation rewrite, image standardization
- **2.0.5** - Enhanced error handling, telemetry integration
- **2.0.4** - New catalog requests, validation framework
- **2.0.3** - Initial release with 14 catalog requests

---

## Upgrade Notes

### Upgrading to 2.0.6
- No breaking changes
- Documentation significantly improved
- All existing catalog requests remain compatible

### Upgrading to 2.0.5
- No breaking changes
- Enhanced error messages may require updating error handling logic
- Telemetry data now available for monitoring

### Upgrading to 2.0.4
- New catalog requests available
- Validation framework may reject previously accepted invalid inputs
- Review validation rules in documentation