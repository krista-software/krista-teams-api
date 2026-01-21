# Authentication

## Overview

The Teams APIs Extension uses **OAuth 2.0** authentication to securely connect with Microsoft Teams through the Microsoft Graph API. This page explains the authentication modes, OAuth 2.0 flows, required permissions, token management, and troubleshooting.

---

## Authentication Modes

The Teams APIs Extension supports **User Account Authentication** using OAuth 2.0 authorization code flow.

### User Account Authentication

**How It Works:**
- Users authenticate with their Microsoft 365 credentials
- The extension obtains an access token on behalf of the user
- All operations are performed in the user's context
- Tokens are automatically refreshed when they expire

**When to Use:**
- When you need to perform actions on behalf of a specific user
- When you need user-specific permissions and access
- When you want to track which user performed an action
- For most Teams messaging and query operations

**Configuration:**
- Set the **Email** parameter to the user's Microsoft 365 email address
- User must authenticate during initial setup
- Refresh tokens are stored securely for automatic token renewal

> **Note**: Service Account authentication is not currently supported. All operations require user context.

---

## OAuth 2.0 Scopes and Permissions

The Teams APIs Extension requires the following Microsoft Graph API scopes:

### Required Scopes

| Scope | Permission Level | Description | Used For |
|-------|-----------------|-------------|----------|
| `openid` | Delegated | OpenID Connect authentication | User identity verification |
| `offline_access` | Delegated | Maintain access to data | Refresh token generation |
| `Team.ReadBasic.All` | Delegated | Read team information | Get teams, channels |
| `Channel.ReadBasic.All` | Delegated | Read channel information | Get channel details |
| `ChannelMessage.Send` | Delegated | Send channel messages | Post messages to channels |
| `Chat.ReadWrite` | Delegated | Read and write chat messages | Send/read 1:1 and group chats |
| `ChatMessage.Send` | Delegated | Send chat messages | Send messages to users |

### Full Scope URLs

When configuring your Azure AD application, use these full scope URLs:

```
https://graph.microsoft.com/openid
https://graph.microsoft.com/offline_access
https://graph.microsoft.com/Team.ReadBasic.All
https://graph.microsoft.com/Channel.ReadBasic.All
https://graph.microsoft.com/ChannelMessage.Send
https://graph.microsoft.com/Chat.ReadWrite
https://graph.microsoft.com/ChatMessage.Send
```

### Permission Levels Explained

**Delegated Permissions:**
- Permissions are granted on behalf of a signed-in user
- The app can only access data the user has access to
- User consent or admin consent is required
- Most secure option for user-facing applications

**Why Not Application Permissions?**
- Application permissions would require admin consent for all users
- Delegated permissions provide better security and user control
- Teams operations typically require user context

---

## OAuth 2.0 Grant Types

The extension uses two OAuth 2.0 grant types for authentication and token refresh.

### 1. Authorization Code Grant

**Purpose**: Initial user authentication

**Grant Type**: `authorization_code`

**Flow**:
1. User is redirected to Microsoft login page
2. User authenticates and grants permissions
3. Microsoft redirects back with authorization code
4. Extension exchanges code for access token + refresh token

**Request Example**:
```http
POST https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token
Content-Type: application/x-www-form-urlencoded

client_id={client-id}
&scope=openid offline_access Team.ReadBasic.All ...
&code={authorization-code}
&redirect_uri={callback-url}
&grant_type=authorization_code
&client_secret={client-secret}
```

**Response Example**:
```json
{
  "token_type": "Bearer",
  "scope": "openid offline_access Team.ReadBasic.All ...",
  "expires_in": 3600,
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGc...",
  "refresh_token": "0.AXoA..."
}
```

### 2. Refresh Token Grant

**Purpose**: Automatic token refresh when access token expires

**Grant Type**: `refresh_token`

**Flow**:
1. Access token expires (after 1 hour)
2. Extension automatically uses refresh token
3. New access token is obtained
4. Operations continue seamlessly

**Request Example**:
```http
POST https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token
Content-Type: application/x-www-form-urlencoded

client_id={client-id}
&scope=openid offline_access Team.ReadBasic.All ...
&refresh_token={refresh-token}
&grant_type=refresh_token
&client_secret={client-secret}
```

**Response Example**:
```json
{
  "token_type": "Bearer",
  "scope": "openid offline_access Team.ReadBasic.All ...",
  "expires_in": 3600,
  "access_token": "eyJ0eXAiOiJKV1QiLCJhbGc..."
}
```

> **Note**: Refresh token grant does not return a new refresh token. The original refresh token remains valid until revoked or expired.

---

## Authentication Flow

### Initial Authentication

```
┌─────────┐                ┌──────────┐                ┌───────────┐
│  User   │                │  Krista  │                │ Microsoft │
│         │                │Extension │                │   Azure   │
└────┬────┘                └────┬─────┘                └─────┬─────┘
     │                          │                            │
     │  1. Configure Extension  │                            │
     ├─────────────────────────>│                            │
     │                          │                            │
     │  2. Test Connection      │                            │
     ├─────────────────────────>│                            │
     │                          │                            │
     │                          │  3. Redirect to Login      │
     │                          ├───────────────────────────>│
     │                          │                            │
     │  4. Login Page           │                            │
     │<────────────────────────────────────────────────────────┤
     │                          │                            │
     │  5. Enter Credentials    │                            │
     ├─────────────────────────────────────────────────────>│
     │                          │                            │
     │  6. Grant Permissions    │                            │
     ├─────────────────────────────────────────────────────>│
     │                          │                            │
     │                          │  7. Authorization Code     │
     │                          │<───────────────────────────┤
     │                          │                            │
     │                          │  8. Exchange Code for Token│
     │                          ├───────────────────────────>│
     │                          │                            │
     │                          │  9. Access + Refresh Token │
     │                          │<───────────────────────────┤
     │                          │                            │
     │  10. Success Message     │                            │
     │<─────────────────────────┤                            │
     │                          │                            │
```

### Subsequent Requests (Automatic Token Refresh)

```
┌─────────┐                ┌──────────┐                ┌───────────┐
│Workflow │                │  Krista  │                │ Microsoft │
│         │                │Extension │                │   Graph   │
└────┬────┘                └────┬─────┘                └─────┬─────┘
     │                          │                            │
     │  1. Execute Catalog Req  │                            │
     ├─────────────────────────>│                            │
     │                          │                            │
     │                          │  2. Check Access Token     │
     │                          │  (Expired?)                │
     │                          │                            │
     │                          │  3. Use Refresh Token      │
     │                          ├───────────────────────────>│
     │                          │                            │
     │                          │  4. New Access Token       │
     │                          │<───────────────────────────┤
     │                          │                            │
     │                          │  5. Call Graph API         │
     │                          ├───────────────────────────>│
     │                          │                            │
     │                          │  6. API Response           │
     │                          │<───────────────────────────┤
     │                          │                            │
     │  7. Catalog Response     │                            │
     │<─────────────────────────┤                            │
     │                          │                            │
```

---

## Token Management

### Access Token

**Lifetime**: 1 hour (3600 seconds)

**Characteristics**:
- Short-lived for security
- Used for all Microsoft Graph API calls
- Automatically refreshed by the extension
- Not stored permanently

**Refresh Mechanism**:
- Extension automatically detects token expiration
- Uses refresh token to obtain new access token
- Process is transparent to users and workflows
- No manual intervention required

### Refresh Token

**Lifetime**:
- **Verified Apps**: 90 days (can be extended to indefinite with continuous use)
- **Unverified Apps**: 24 hours

**Characteristics**:
- Long-lived for convenience
- Stored securely in Krista's key-value store
- Used only to obtain new access tokens
- Never sent to Microsoft Graph API directly

**Storage**:
- Encrypted at rest
- Associated with user email address
- Automatically managed by the extension
- Removed when user revokes access

---

## Token Expiration Scenarios

### 1. Access Token Expiration (Normal)

**When**: After 1 hour of inactivity

**What Happens**:
1. Extension detects expired access token
2. Automatically uses refresh token
3. Obtains new access token
4. Continues operation seamlessly

**User Impact**: None (automatic)

### 2. Refresh Token Expiration

**When**:
- Verified apps: After 90 days of inactivity
- Unverified apps: After 24 hours
- User changes password
- User revokes access
- Security event detected
- Token limit exceeded (25 tokens per app per user)

**What Happens**:
1. Extension attempts to use refresh token
2. Microsoft returns error
3. Extension throws `MustAuthorizeException`
4. User must re-authenticate

**User Impact**: Must re-authenticate through OAuth flow

**Error Message**:
```
"Refresh Token is no longer valid. Please reauthorize yourself"
```

### 3. User Revocation

**When**: User revokes app access in Microsoft account settings

**What Happens**:
1. All tokens immediately invalidated
2. Extension cannot access Microsoft Graph API
3. User must re-authenticate

**User Impact**: Must grant permissions again

### 4. Token Limit Exceeded

**When**: User has more than 25 refresh tokens for the app

**What Happens**:
1. Oldest refresh token is automatically revoked
2. If that token was in use, re-authentication required

**User Impact**: May need to re-authenticate if using oldest token

---

## Security Best Practices

### 1. Protect Tokens

- Never log or display access tokens or refresh tokens
- Tokens are automatically encrypted by Krista
- Don't share tokens between users or applications
- Revoke tokens immediately if compromised

### 2. Minimize Scope

- Only request the minimum required scopes
- Review scope usage regularly
- Remove unused scopes from Azure AD app

### 3. Monitor Access

- Enable audit logging in Azure AD
- Monitor sign-in logs for suspicious activity
- Set up alerts for failed authentication attempts
- Review app permissions periodically

### 4. Rotate Credentials

- Rotate client secrets every 6-12 months
- Use multiple secrets during rotation period
- Remove old secrets after rotation complete

### 5. Use Verified Apps

- Complete Microsoft publisher verification
- Extends refresh token lifetime to 90 days
- Builds user trust
- Required for production deployments

---

## Troubleshooting

### Issue: "You are not authorized. Please authorize yourself"

**Cause**: No refresh token found for the user

**Resolution**:
1. Click **Test Connection** in extension configuration
2. Complete the OAuth authentication flow
3. Grant requested permissions
4. Save the configuration

### Issue: "Refresh Token is no longer valid. Please reauthorize yourself"

**Cause**: Refresh token has expired or been revoked

**Common Scenarios**:
- User changed password
- User revoked app access
- Token expired (24 hours for unverified apps)
- Security event triggered revocation

**Resolution**:
1. Re-authenticate through Test Connection
2. Complete OAuth flow again
3. Consider completing publisher verification for longer token lifetime

### Issue: "Unauthorised user. User email configured in the setup does not match with authenticated user"

**Cause**: Authenticated user email doesn't match configured email

**Resolution**:
1. Verify the **Email** parameter in configuration matches the authenticating user
2. Use the correct Microsoft 365 account during authentication
3. Update the Email parameter if needed

### Issue: "Invalid state parameters!"

**Cause**: OAuth state parameter is malformed or missing

**Resolution**:
1. Clear browser cache and cookies
2. Try authentication again
3. Verify redirect URI is configured correctly in Azure AD
4. Check for browser extensions blocking redirects

### Issue: "Error occurred during authorization"

**Cause**: OAuth token exchange failed

**Common Scenarios**:
- Invalid client secret
- Incorrect tenant ID
- Network connectivity issues
- Azure AD service outage

**Resolution**:
1. Verify client secret is correct and not expired
2. Verify tenant ID matches Azure AD
3. Check network connectivity to login.microsoftonline.com
4. Review Azure AD application configuration
5. Check Microsoft service health status

### Issue: Permissions Denied Errors (403)

**Cause**: Required scopes not granted or admin consent missing

**Resolution**:
1. Verify all required scopes are added in Azure AD app
2. Grant admin consent for the application
3. Ensure user has necessary Teams permissions
4. Re-authenticate to refresh permissions

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Setup and configuration
- [Creating Teams App](CreatingTeamsApp.md) - Azure AD application setup
- [Microsoft OAuth 2.0 Documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow)
- [Microsoft Graph Permissions Reference](https://docs.microsoft.com/en-us/graph/permissions-reference)

