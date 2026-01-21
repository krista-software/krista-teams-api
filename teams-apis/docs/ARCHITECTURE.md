# Teams APIs Extension - Architecture Documentation

## Overview

| Property | Value |
|----------|-------|
| **Extension Name** | Teams APIs |
| **Version** | 2.0.8 |
| **JAX-RS ID** | teams-apis |
| **Domain** | Collaboration |
| **Ecosystem** | Essentials |
| **Java Version** | Java 21 |
| **Primary API** | Microsoft Graph API |
| **Authentication** | OAuth 2.0 with MSAL4J |

---

## Architecture Diagram

```mermaid
flowchart TB
    subgraph Layer1["Layer 1: Extension Entry Point"]
        EXT[TeamsApisExtension]
        ATTR[TeamsApisAttributes]
        AUTH[TeamsApisRequestAuthenticator]
    end

    subgraph Layer2["Layer 2: Catalog Area Layer"]
        AREA[TeamsAPIsArea<br/>14 Catalog Requests]
        SUBCAT[TeamsAPIsAreaSubCatalogRequests<br/>Validation Sub-Catalogs]
    end

    subgraph Layer3["Layer 3: Service Layer"]
        TEAM_SVC[TeamService]
        CHAN_SVC[ChannelService]
        CHAT_SVC[ChatService]
    end

    subgraph Layer4["Layer 4: Connector Layer"]
        PROVIDER[GraphServiceClientProvider]
        FACTORY[GraphServiceClientProviderFactory]
    end

    subgraph Layer5["Layer 5: Entity & Model Layer"]
        CHANNEL[Channel]
        TEAM[Team]
        GROUPCHAT[GroupChat]
        MSG[ChannelMessage]
        REPLIES[Replies]
    end

    subgraph External["External Systems"]
        GRAPH[Microsoft Graph API]
        AZURE[Azure Active Directory]
    end

    EXT --> ATTR
    EXT --> AUTH
    EXT --> AREA
    AREA --> SUBCAT
    AREA --> TEAM_SVC
    AREA --> CHAN_SVC
    AREA --> CHAT_SVC
    TEAM_SVC --> PROVIDER
    CHAN_SVC --> PROVIDER
    CHAT_SVC --> PROVIDER
    PROVIDER --> FACTORY
    FACTORY --> GRAPH
    AUTH --> AZURE
    TEAM_SVC --> TEAM
    TEAM_SVC --> CHANNEL
    CHAN_SVC --> CHANNEL
    CHAN_SVC --> MSG
    CHAT_SVC --> GROUPCHAT
    CHAT_SVC --> MSG
    CHAT_SVC --> REPLIES
```

---

## Layer Details

### Layer 1: Extension Entry Point

```mermaid
classDiagram
    class TeamsApisExtension {
        -String routingUrl
        -TeamsApisAttributes teamsApisAttributes
        -GraphServiceClientProviderFactory clientProviderFactory
        -TeamsApisRequestAuthenticator requestAuthenticator
        +getRequestAuthenticator() RequestAuthenticator
        +customTabs() Map~String,String~
        +validateConnection() ExtensionConnectionStatus
        +testConnection() boolean
        +attributesUpdated() void
    }

    class TeamsApisAttributes {
        -String clientId
        -String clientSecret
        -String mailId
        -String tenantId
        -String routingUrl
        -OAuth20Service oAuth20Service
        +getOAuth20Service() OAuth20Service
        +getCallbackUrl() String
        +update() void
        +create() void
    }

    class TeamsApisRequestAuthenticator {
        +authenticate() void
        +validateToken() boolean
    }

    TeamsApisExtension --> TeamsApisAttributes
    TeamsApisExtension --> TeamsApisRequestAuthenticator
```

**Responsibilities:**
- Configuration management (clientId, clientSecret, tenantId, email)
- OAuth 2.0 authentication flow
- Connection validation with Microsoft Graph API
- Request authentication via `TeamsApisRequestAuthenticator`

---

### Layer 2: Catalog Area Layer

```mermaid
flowchart LR
    subgraph CatalogRequests["14 Catalog Requests"]
        CR1[Get channels of team]
        CR2[Send message teams group]
        CR3[Send HTML message teams group]
        CR4[Send message to teams channel]
        CR5[Get Channel Messages]
        CR6[Get Group Chat Messages]
        CR7[Get Group Chat by Name]
        CR8[Get All Teams]
        CR9[Get All Group Chats]
        CR10[Get Channel Message Replies]
        CR11[Get Channels of Team by ID]
        CR12[Send Message to User]
        CR13[Send HTML Message to User]
        CR14[Send HTML message to channel]
    end

    subgraph SubCatalogs["Validation Sub-Catalogs"]
        SC1[confirmReenterGetChannelMessages]
        SC2[handleReenterGetChannelMessages]
        SC3[confirmDateValidation...]
        SC4[handleDateValidation...]
        SC5[confirmGroupNameValidation...]
        SC6[handleGroupNameValidation...]
    end

    CatalogRequests --> SubCatalogs
```

**Responsibilities:**
- Handle all 14 catalog requests
- Telemetry tracking (success/failure counters)
- Validation orchestration
- Error handling with sub-catalog re-entry flows

---

### Layer 3: Service Layer

```mermaid
classDiagram
    class TeamService {
        -GraphServiceClientProvider clientProvider
        +getJoinedTeams(teamName) List~Team~
        +getTeamById(teamId) Team
        +getTeamMembers(client, teamId) List~Map~
    }

    class ChannelService {
        -GraphServiceClientProvider clientProvider
        +getChannelsOfTeam(teamId, channelName) List~Channel~
        +validateChannelExists(channelId, teamId) boolean
        +getChannelMembers(client, teamId, channelId) List~Map~
    }

    class ChatService {
        -GraphServiceClientProvider clientProvider
        -TeamService teamService
        -ChannelService channelService
        -Map userChatStorage
        +sendMessageToChannel(teamName, channelName, message, isHtml) boolean
        +sendMessageToChatGroup(groupName, message, isHtml) boolean
        +sendMessageToUser(userEmail, messageContent, reuseChatId, logConversation, isHtml) Map
        +getChannelMessagesWithDateRangeLimit(...) List~ChannelMessage~
        +getGroupChats(chatID, fromDate, toDate) List~ChannelMessage~
        +getAllGroupChats() List~GroupChat~
    }

    TeamService --> GraphServiceClientProvider
    ChannelService --> GraphServiceClientProvider
    ChatService --> GraphServiceClientProvider
    ChatService --> TeamService
    ChatService --> ChannelService
```

**Responsibilities:**
- Business logic implementation
- Microsoft Graph API operations
- Pagination handling
- Chat ID caching for performance

---

### Layer 4: Connector Layer

```mermaid
sequenceDiagram
    participant Service as Service Layer
    participant Provider as GraphServiceClientProvider
    participant Factory as GraphServiceClientProviderFactory
    participant Graph as Microsoft Graph API

    Service->>Provider: getGraphServiceClientForUser()
    Provider->>Factory: create()
    Factory->>Graph: Build authenticated client
    Graph-->>Factory: GraphServiceClient
    Factory-->>Provider: Client instance
    Provider-->>Service: Ready client
    Service->>Graph: API Request
    Graph-->>Service: API Response
```

**Responsibilities:**
- Create and manage `GraphServiceClient` instances
- Handle OAuth token management
- Provide authenticated clients to services

---

### Layer 5: Entity & Model Layer

```mermaid
classDiagram
    class Channel {
        +String iD
        +String name
        +String description
        +String email
        +String teamID
        +List~Map~ members
    }

    class Team {
        +String iD
        +String name
        +String description
        +List~Map~ members
    }

    class GroupChat {
        +String groupId
        +String topic
        +String type
        +Long createdDateTime
        +List~Map~ members
    }

    class ChannelMessage {
        +String senderName
        +String subject
        +String message
        +String messageId
        +String attachmentLinks
        +Long createdDateTime
    }

    class Replies {
        +String replyId
        +String replyMessage
        +String senderName
        +String messageId
        +String attachmentLinks
        +Long createdDateTime
    }

    Team "1" --> "*" Channel : contains
    Channel "1" --> "*" ChannelMessage : contains
    ChannelMessage "1" --> "*" Replies : has
    GroupChat "1" --> "*" ChannelMessage : contains
```

---

## Data Flow Diagrams

### Send Message to Channel Flow

```mermaid
sequenceDiagram
    participant User
    participant Area as TeamsAPIsArea
    participant ChatSvc as ChatService
    participant TeamSvc as TeamService
    participant ChanSvc as ChannelService
    participant Graph as Microsoft Graph API

    User->>Area: Send message to channel request
    Area->>Area: Start telemetry operation
    Area->>ChatSvc: sendMessageToChannel(teamName, channelName, message)
    ChatSvc->>TeamSvc: getJoinedTeams(teamName)
    TeamSvc->>Graph: GET /me/joinedTeams
    Graph-->>TeamSvc: Teams list
    TeamSvc-->>ChatSvc: Team found
    ChatSvc->>ChanSvc: getChannelsOfTeam(teamId, channelName)
    ChanSvc->>Graph: GET /teams/{id}/channels
    Graph-->>ChanSvc: Channels list
    ChanSvc-->>ChatSvc: Channel found
    ChatSvc->>Graph: POST /teams/{id}/channels/{id}/messages
    Graph-->>ChatSvc: Message sent
    ChatSvc-->>Area: true
    Area->>Area: Record success telemetry
    Area-->>User: Is Message Delivered = true
```

### Get Channel Messages with Date Filter Flow

```mermaid
sequenceDiagram
    participant User
    participant Area as TeamsAPIsArea
    participant ChatSvc as ChatService
    participant Graph as Microsoft Graph API

    User->>Area: Get channel messages request
    Area->>Area: Validate inputs (channelId, teamId, dates)
    alt Validation fails & Allow Retry = true
        Area->>Area: Trigger sub-catalog for re-entry
        Area-->>User: Prompt for correct value
    end
    Area->>ChatSvc: getChannelMessagesWithDateRangeLimit(...)
    loop Pagination (max 2000 messages)
        ChatSvc->>Graph: GET /teams/{id}/channels/{id}/messages
        Graph-->>ChatSvc: Messages page
        ChatSvc->>ChatSvc: Filter by date range
        ChatSvc->>ChatSvc: Check MAX_MESSAGES_TO_PROCESS
    end
    ChatSvc-->>Area: List<ChannelMessage>
    Area->>Area: Record success telemetry
    Area-->>User: Channel Messages list
```

---

## Performance Characteristics

### Pagination Configuration

| Parameter | Value | Description |
|-----------|-------|-------------|
| `MAX_MESSAGES_TO_PROCESS` | 2000 | Maximum messages retrieved per request |
| `MAX_SKIP_COUNT` | 1000 | Maximum pagination skip count |
| `DEFAULT_PAGE_SIZE` | 50 | Default page size for API calls |

### Performance Limitations

| Limitation | Value | Impact |
|------------|-------|--------|
| Message Size Limit | 28,000 characters | Large HTML messages will fail |
| Max Messages per Request | 2000 | Older messages may be truncated |
| API Rate Limits | Microsoft Graph limits | May cause throttling |
| Chat ID Cache | In-memory only | Lost on extension restart |

### Performance Best Practices

```mermaid
flowchart TD
    A[Performance Optimization] --> B[Use Date Filters]
    A --> C[Enable Reuse Chat ID]
    A --> D[Enable Log Conversation]
    A --> E[Use IDs over Names]
    A --> F[Split Large Messages]
    
    B --> B1[Reduces data volume]
    C --> C1[Avoids chat lookup]
    D --> D1[Caches chat IDs]
    E --> E1[Avoids name resolution]
    F --> F1[Avoids 28K limit]
```

---

## Error Scenarios

### Error Handling Flow

```mermaid
flowchart TD
    A[Request Received] --> B{Validate Inputs}
    B -->|Valid| C[Execute Operation]
    B -->|Invalid| D{Allow Retry?}
    D -->|Yes| E[Trigger Confirm Sub-Catalog]
    D -->|No| F[Return Permanent Error]
    E --> G[User Re-enters Value]
    G --> H{Validate New Value}
    H -->|Valid| C
    H -->|Invalid| F
    C --> I{Operation Success?}
    I -->|Yes| J[Record Success Telemetry]
    I -->|No| K{Error Type}
    K -->|Message Too Large| L[GraphServiceExceptionHandler]
    K -->|Auth Error| M[MustAuthorizeException]
    K -->|API Error| N[Log & Return Error]
    L --> O[Return Size Limit Error]
    M --> P[Prompt Re-authorization]
    J --> Q[Return Success Response]
```

### Error Categories

| Error Type | Cause | Detection | Solution |
|------------|-------|-----------|----------|
| **Authentication Error** | Invalid/expired OAuth token | `MustAuthorizeException` | Re-authorize extension |
| **Team Not Found** | Team name incorrect or no access | Empty team list | Verify team name |
| **Channel Not Found** | Channel doesn't exist in team | Empty channel list | Verify channel name |
| **Group Chat Not Found** | No group with specified name | Empty chat list | Verify group topic |
| **Multiple Groups Found** | Duplicate group names | `allChats.size() > 1` | Use unique names or Chat ID |
| **Message Too Large** | Exceeds 28,000 characters | HTTP 400 + InvalidParameter | Split message |
| **Invalid Date Range** | From date > To date | Validation check | Correct dates |
| **User Not Found** | Email doesn't exist in tenant | Graph API error | Verify email |
| **Permission Denied** | Missing Graph API permissions | 403 response | Grant permissions |
| **Rate Limited** | Too many API requests | 429 response | Implement backoff |

### Message Size Error Handling

```mermaid
flowchart TD
    A[Send Message Request] --> B[GraphServiceClient.post]
    B --> C{Response}
    C -->|Success| D[Return Success]
    C -->|Error| E{GraphServiceException}
    E --> F[GraphServiceExceptionHandler]
    F --> G{isMessageTooLargeError?}
    G -->|Yes| H[Check HTTP 400]
    G -->|No| I[Throw Original Error]
    H --> J{Error Code?}
    J -->|InvalidParameter| K[Create Size Limit Error]
    J -->|RequestBodyTooLarge| K
    J -->|Other| I
    K --> L[Return: Message exceeds 28,000 character limit]
```

---

## Validation Framework

```mermaid
classDiagram
    class ValidationOrchestrator {
        -List~Validator~ validators
        +registerValidator(validator) void
        +validate() ValidationResult
    }

    class Validator {
        <<interface>>
        +validate() boolean
        +getFetchFieldName() String
        +getFieldType() FieldType
        +getFetchStepMessage() String
        +getConfirmationStepMessage() String
        +getErrMessage() String
    }

    class BaseValidator {
        <<abstract>>
        #validateResource() boolean
    }

    class BaseTeamsValidator {
        <<abstract>>
        #GraphServiceClientProvider provider
    }

    class ValidationResult {
        +String confirmStepMessage
        +String fetchFieldName
        +String fetchStepMessage
        +String errMessage
        +FieldType fieldType
    }

    ValidationOrchestrator --> Validator
    Validator <|-- BaseValidator
    BaseValidator <|-- BaseTeamsValidator
    ValidationOrchestrator --> ValidationResult
```

### Validation Resources

| Resource | Description | Used By |
|----------|-------------|---------|
| `CHANNEL_ID` | Channel identifier | Get Channel Messages |
| `TEAM_ID` | Team identifier | Multiple requests |
| `CHAT_ID` | Chat identifier | Get Group Chat Messages |
| `FROM_DATE` | Start date filter | Message retrieval |
| `TO_DATE` | End date filter | Message retrieval |
| `GROUP_NAME` | Group chat topic | Get Group Chat by Name |
| `CHANNEL_TEAM_LINK` | Channel-Team association | Get Channel Messages |
| `USER_EMAIL` | User email address | Send Message to User |
| `MESSAGE_CONTENT` | Message text | Send operations |
| `HTML_MESSAGE_CONTENT` | HTML message | HTML send operations |

---

## Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Runtime** | Java 21 | Application runtime |
| **DI Framework** | HK2 | Dependency injection |
| **Graph SDK** | Microsoft Graph SDK for Java | Teams API integration |
| **Auth Library** | MSAL4J | OAuth 2.0 authentication |
| **Logging** | SLF4J | Logging framework |
| **JSON** | Gson | JSON serialization |
| **HTTP Client** | OkHttp (via Graph SDK) | HTTP communication |
| **Extension Framework** | Krista SDK | Extension annotations and APIs |

---

## Package Structure

```
teams-apis/src/main/java/app/krista/extensions/essentials/collaboration/teams/
├── TeamsApisExtension.java              # [Layer 1] Extension entry point
├── TeamsApisAttributes.java             # [Layer 1] Configuration attributes
├── catalog/
│   ├── TeamsAPIsArea.java               # [Layer 2] Main catalog (14 requests)
│   ├── TeamsAPIsAreaSubCatalogRequests.java  # [Layer 2] Validation sub-catalogs
│   └── entities/
│       ├── Channel.java                 # [Layer 5] Channel entity
│       ├── Team.java                    # [Layer 5] Team entity
│       ├── GroupChat.java               # [Layer 5] GroupChat entity
│       ├── ChannelMessage.java          # [Layer 5] ChannelMessage entity
│       └── stores/
│           └── Replies.java             # [Layer 5] Replies entity
├── service/
│   ├── TeamService.java                 # [Layer 3] Team operations
│   ├── ChannelService.java              # [Layer 3] Channel operations
│   └── ChatService.java                 # [Layer 3] Chat and messaging
├── impl/
│   ├── connectors/
│   │   ├── GraphServiceClientProvider.java      # [Layer 4] Client provider
│   │   └── GraphServiceClientProviderFactory.java  # [Layer 4] Factory
│   └── util/
│       ├── Constants.java               # Constants
│       └── TelemetryConstants.java      # Telemetry metrics
└── errorhandling/
    ├── constants/
    │   ├── SubCatalogConstants.java     # Sub-catalog constants
    │   └── TeamsResources.java          # Resource field names
    ├── generators/
    │   └── ExtensionResponseGenerator.java  # Response generation
    ├── validators/
    │   ├── Validator.java               # Validator interface
    │   ├── BaseValidator.java           # Base validator
    │   └── ValidationOrchestrator.java  # Validation orchestration
    └── utils/
        └── GraphServiceExceptionHandler.java  # Exception handling
```

---

## Security Considerations

| Aspect | Implementation |
|--------|----------------|
| **Credential Storage** | clientSecret and tenantId marked as `isSecured = true` |
| **Token Management** | OAuth 2.0 with refresh token support |
| **API Communication** | HTTPS only via Microsoft Graph API |
| **Permission Model** | Azure AD app permissions with user consent |
| **Session Handling** | TeamsApisRequestAuthenticator validates each request |

---

## Telemetry Metrics

| Operation | Success Counter | Failure Counter |
|-----------|-----------------|-----------------|
| Get Channels of Team | `teams.get_channels_of_team.request.success` | `teams.get_channels_of_team.request.failure` |
| Send Message Teams Group | `teams.send_message_teams_group.request.success` | `teams.send_message_teams_group.request.failure` |
| Send HTML Message Teams Group | `teams.send_html_message_teams_group.request.success` | `teams.send_html_message_teams_group.request.failure` |
| Send Message to Channel | `teams.send_message_to_channel.request.success` | `teams.send_message_to_channel.request.failure` |
| Get Channel Messages | `teams.get_channel_messages.request.success` | `teams.get_channel_messages.request.failure` |
| Get Group Chat Messages | `teams.get_group_chat_messages.request.success` | `teams.get_group_chat_messages.request.failure` |
| Get Group Chat by Name | `teams.get_group_chat_by_name.request.success` | `teams.get_group_chat_by_name.request.failure` |
| Get All Teams | `teams.get_all_teams.request.success` | `teams.get_all_teams.request.failure` |
| Get All Group Chats | `teams.get_all_group_chats.request.success` | `teams.get_all_group_chats.request.failure` |
| Get Channel Message Replies | `teams.get_channel_message_replies.request.success` | `teams.get_channel_message_replies.request.failure` |
| Get Channels by Team ID | `teams.get_channels_of_team_by_id.request.success` | `teams.get_channels_of_team_by_id.request.failure` |
| Send Message to User | `teams.send_message_to_user.request.success` | `teams.send_message_to_user.request.failure` |
| Send HTML Message to User | `teams.send_html_message_to_user.request.success` | `teams.send_html_message_to_user.request.failure` |
| Send HTML Message to Channel | `teams.send_html_message_to_channel.request.success` | `teams.send_html_message_to_channel.request.failure` |

