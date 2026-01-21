[comment]: <> (Home page)

# Teams APIs Extension

## Overview

The Teams APIs Extension enables seamless integration with Microsoft Teams through the Microsoft Graph API. This extension allows you to automate team communications, retrieve messages, manage channels, and interact with group chats programmatically within your Krista workflows.

## What's New in Version 2.0.7

**Latest Updates**:
- **Allow Retry Parameter** - Added to all 14 catalog requests for better validation control
- **Message Length Limits Updated** - Removed validator max length, Teams API enforces ~28,000 character limit
- **User-Friendly Error Messages** - Clear guidance when messages exceed Teams API limits
- **Teams 2024+ Support** - Documentation updated for new Teams client capabilities (80K-100K chars for manual messages)

**[View Full Release Notes](/pages/release-notes)** for detailed information about all changes, improvements, and migration guidance.

---

## Key Features

**Messaging Operations**
- Send plain text and HTML messages to Teams channels
- Send 1:1 messages to users
- Send messages to group chats
- Support for rich HTML formatting

**Team & Channel Management**
- Retrieve all teams user has access to
- Get channels for specific teams
- Query team and channel information

**Message Retrieval**
- Get channel messages with date range filtering
- Retrieve group chat messages
- Access message replies and threads

**Group Chat Management**
- Get all group chats
- Find group chats by name
- Access group chat details

**Advanced Features**
- OAuth 2.0 authentication with automatic token refresh
- Input validation with re-entry flows
- Comprehensive error handling
- Telemetry and monitoring support

## Quick Start Guide

### 1. Prerequisites
- Microsoft 365 account with Teams access
- Azure AD application with appropriate permissions
- Krista platform access

### 2. Setup Steps
1. [Create a Teams App in Azure AD](/pages/CreatingTeamsApp)
2. [Configure the Extension](/pages/ExtensionConfiguration)
3. [Set up Authentication](/pages/Authentication)
4. Start using catalog requests in your workflows

### 3. First Request
Try sending your first message:
```
Catalog Request: Send Message to User
User Email: user@example.com
Message Content: Hello from Krista!
```

## Documentation Structure

### Release Information
- **[Release Notes](/pages/release-notes)** - Version history, updates, and migration guides

### Getting Started
- [Extension Configuration](/pages/ExtensionConfiguration) - Setup parameters and configuration
- [Authentication](/pages/Authentication) - OAuth 2.0 flows, scopes, and token management
- [Creating Teams App](/pages/CreatingTeamsApp) - Step-by-step Azure AD app creation

### Messaging Operations
- [Send Message Teams Group](/pages/SendMessageTeamsGroup) - Send plain text to group chats
- [Send HTML Message Teams Group](/pages/SendHTMLMessageTeamsGroup) - Send HTML to group chats
- [Send Message to Teams Channel](/pages/SendMessageToTeamsChannel) - Send plain text to channels
- [Send HTML Message to Teams Channel](/pages/SendHTMLMessageToTeamsChannel) - Send HTML to channels
- [Send Message to User](/pages/SendMessageToUser) - Send plain text 1:1 messages
- [Send HTML Message to User](/pages/SendHTMLMessageToUser) - Send HTML 1:1 messages

### Team & Channel Management
- [Get All Teams](/pages/GetAllTeams) - Retrieve all accessible teams
- [Get Channels of Team](/pages/GetChannelsOfTeam) - Get channels by team name
- [Get Channels of Team by Team ID](/pages/GetChannelsOfTeamByTeamID) - Get channels by team ID

### Message Retrieval
- [Get Channel Messages](/pages/GetChannelMessages) - Retrieve channel messages with date filtering
- [Get Group Chat Messages](/pages/GetGroupChatMessages) - Retrieve group chat messages
- [Get Channel Message Replies](/pages/GetChannelMessageReplies) - Get message thread replies

### Group Chat Management
- [Get All Group Chats](/pages/GetAllGroupChats) - Retrieve all accessible group chats
- [Get Group Chat by Name](/pages/GetGroupChatByName) - Find group chat by name

## Support & Resources

### Extension Information
- **Version**: 2.0.8
- **API**: Microsoft Graph API v1.0
- **Authentication**: OAuth 2.0
- **Supported Scopes**: Chat.ReadWrite, Team.ReadBasic.All, Channel.ReadBasic.All, ChannelMessage.Read.All
- **[Release Notes](/pages/release-notes)** - View version history and latest updates

### Additional Resources
- [Microsoft Graph API Documentation](https://docs.microsoft.com/en-us/graph/api/overview)
- [Microsoft Teams API Reference](https://docs.microsoft.com/en-us/graph/api/resources/teams-api-overview)
- [OAuth 2.0 Documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow)

### Need Help?
- Check the [Troubleshooting](pages/Authentication.md#troubleshooting) section
- Review [Common Use Cases](pages/SendMessageToUser.md#common-use-cases)
- Contact Krista support for assistance

---

**Ready to get started?** Begin with [Extension Configuration](pages/ExtensionConfiguration.md) to set up your Teams integration.