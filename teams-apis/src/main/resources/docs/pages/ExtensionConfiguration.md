# Extension Configuration

## Overview

The Teams APIs Extension requires proper configuration to connect with Microsoft Teams through the Microsoft Graph API. This page provides detailed instructions for configuring the extension with the necessary credentials and settings obtained from your Azure AD application.

## Configuration Parameters

The following parameters must be configured to establish a connection with Microsoft Teams:

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| **Client ID** | Text | Yes | OAuth 2.0 client identifier from Azure AD application | `12345678-1234-1234-1234-123456789abc` |
| **Client Secret** | Text (Secured) | Yes | OAuth 2.0 client secret from Azure AD application | `abc123~XYZ789...` |
| **Tenant ID** | Text (Secured) | Yes | Azure AD tenant (directory) identifier | `87654321-4321-4321-4321-cba987654321` |
| **Email** | Text | Yes | Email address of the Microsoft 365 user for authentication | `admin@yourcompany.onmicrosoft.com` |

> **Security Note**: Client Secret and Tenant ID are marked as secured fields and will be encrypted in the Krista platform.

---

## Step-by-Step Setup

### Prerequisites

Before configuring the extension, ensure you have:

1. A Microsoft 365 account with Teams access
2. An Azure AD application created (see [Creating Teams App](CreatingTeamsApp.md))
3. Admin access to the Azure portal
4. Krista platform access with extension installation permissions

### Step 1: Obtain Azure AD Application Credentials

Navigate to your Azure AD application in the Azure portal to retrieve the required credentials.

#### 1.1 Get Client ID and Tenant ID

1. Go to [Azure Portal](https://portal.azure.com)
2. Navigate to **Azure Active Directory** > **App registrations**
3. Select your Teams API application
4. On the **Overview** page, copy:
   - **Application (client) ID** → This is your **Client ID**
   - **Directory (tenant) ID** → This is your **Tenant ID**

![Client ID and Tenant ID](../_media/ExtensionConfiguration_clientIDtenantID.png)

#### 1.2 Get Client Secret

1. In your Azure AD application, navigate to **Certificates & secrets**
2. Under **Client secrets**, click **+ New client secret**
3. Add a description (e.g., "Krista Teams Extension")
4. Select an expiration period (recommended: 24 months)
5. Click **Add**
6. **Important**: Copy the **Value** immediately (it won't be shown again)
   - This value is your **Client Secret**

![Client Secret](../_media/ExtensionConfiguration_clientSecret.png)

> **Warning**: The client secret value is only displayed once. If you lose it, you'll need to create a new secret.

### Step 2: Configure Redirect URI

The redirect URI (callback URL) must be configured in your Azure AD application to enable OAuth 2.0 authentication.

#### 2.1 Get Extension Base URL from Krista

1. In Krista, navigate to your Teams APIs Extension
2. Go to the **Details** tab
3. Copy the **Extension Base URL**
   - Example: `https://your-krista-instance.com/extensions/teams-apis`

![Extension Base URL](../_media/ExtensionConfiguration_extensionBaseURL.png)

#### 2.2 Construct Callback URL

Append `/teams-apis/callback` to the Extension Base URL:

```
Format: {Extension Base URL}/teams-apis/callback
Example: https://your-krista-instance.com/extensions/teams-apis/teams-apis/callback
```

#### 2.3 Add Redirect URI in Azure AD

1. In your Azure AD application, navigate to **Authentication**
2. Under **Platform configurations**, click **+ Add a platform**
3. Select **Web**
4. Enter your callback URL in **Redirect URIs**
5. Click **Configure**

> **Note**: The redirect URI must match exactly, including the protocol (https://) and path.

### Step 3: Configure Extension in Krista

1. In Krista, navigate to **Extensions** > **Teams APIs**
2. Click **Configure** or **Edit Configuration**
3. Enter the following values:

   | Field | Value |
   |-------|-------|
   | **Client ID** | Paste the Application (client) ID from Azure AD |
   | **Client Secret** | Paste the client secret value from Azure AD |
   | **Tenant ID** | Paste the Directory (tenant) ID from Azure AD |
   | **Email** | Enter the Microsoft 365 user email address |

4. Click **Save** or **Test Connection**

### Step 4: Test Connection

After saving the configuration:

1. Click **Test Connection** button
2. You will be redirected to Microsoft login page
3. Sign in with the email address you configured
4. Grant the requested permissions
5. You should see: **"User authenticated successfully. Save the changes."**
6. Click **Save** to finalize the configuration

---

## Authentication Type Selection

The Teams APIs Extension supports **User Account Authentication** using OAuth 2.0.

### User Account Authentication

- **How it works**: Users authenticate with their Microsoft 365 credentials
- **Use case**: When you need to perform actions on behalf of a specific user
- **Permissions**: Based on the authenticated user's Teams access
- **Token management**: Automatic refresh using refresh tokens

> **Note**: Service Account authentication is not currently supported. All operations are performed in the context of the authenticated user.

---

## Redirect URI Configuration Details

### Callback URL Format

```
{Krista Base URL}/extensions/{Extension ID}/teams-apis/callback
```

### Components

- **Krista Base URL**: Your Krista instance URL
- **Extension ID**: Unique identifier for the Teams APIs extension
- **teams-apis/callback**: Fixed path for OAuth callback

### Example

```
https://krista.yourcompany.com/extensions/teams-apis/teams-apis/callback
```

---

## Security Considerations

### 1. Protect Client Secrets

- Never commit client secrets to version control
- Store secrets in Krista's secured fields
- Rotate secrets periodically (every 6-12 months)
- Use different secrets for development and production

### 2. Limit Permissions

- Only grant the minimum required Microsoft Graph API scopes
- Review and audit permissions regularly
- Use least-privilege principle for user accounts

### 3. Monitor Access

- Enable audit logging in Azure AD
- Monitor authentication attempts
- Review token usage and refresh patterns
- Set up alerts for suspicious activity

### 4. Secure Redirect URIs

- Always use HTTPS for redirect URIs
- Validate redirect URI matches exactly
- Don't use wildcards in redirect URIs
- Keep redirect URI list minimal

---

## Troubleshooting

### Issue: "Invalid Client" Error

**Cause**: Client ID or Client Secret is incorrect

**Resolution**:
1. Verify Client ID matches the Application (client) ID in Azure AD
2. Regenerate Client Secret if needed
3. Ensure no extra spaces when copying credentials
4. Check that the secret hasn't expired

### Issue: "Redirect URI Mismatch" Error

**Cause**: Callback URL doesn't match configured redirect URI

**Resolution**:
1. Verify the redirect URI in Azure AD matches exactly
2. Check for trailing slashes or case sensitivity
3. Ensure HTTPS is used (not HTTP)
4. Confirm the Extension Base URL is correct

### Issue: "Tenant Not Found" Error

**Cause**: Tenant ID is incorrect or tenant doesn't exist

**Resolution**:
1. Verify Tenant ID matches the Directory (tenant) ID in Azure AD
2. Ensure you're using the correct Azure AD tenant
3. Check that the tenant is active

### Issue: "User Not Authorized" Error

**Cause**: User doesn't have necessary permissions

**Resolution**:
1. Verify the user has Teams access
2. Check that required API permissions are granted
3. Ensure admin consent is provided for the application
4. Verify the user's email address is correct

### Issue: "Connection Test Failed"

**Cause**: Configuration is incomplete or incorrect

**Resolution**:
1. Verify all four parameters are filled in
2. Test each credential individually in Azure portal
3. Check network connectivity to Microsoft Graph API
4. Review Krista logs for detailed error messages
5. Ensure the user account is active and not locked

---

## Required Microsoft Graph API Scopes

The extension requires the following scopes (configured automatically):

```
openid
offline_access
Team.ReadBasic.All
Channel.ReadBasic.All
ChannelMessage.Send
Chat.ReadWrite
ChatMessage.Send
```

> **Note**: These scopes are configured in the extension code and don't need to be manually set during configuration. However, they must be granted in your Azure AD application. See [Authentication](Authentication.md) for details.

---

## Next Steps

After successfully configuring the extension:

1. [Review Authentication Details](Authentication.md) - Understand OAuth 2.0 flows and token management
2. [Explore Catalog Requests](../README.md#documentation-structure) - Start using Teams operations
3. [Test Basic Operations](SendMessageToUser.md) - Send your first message

---

## See Also

- [Creating Teams App](CreatingTeamsApp.md) - Azure AD application setup
- [Authentication](Authentication.md) - OAuth 2.0 and token management
- [Send Message to User](SendMessageToUser.md) - First catalog request example
- [Microsoft Graph API Documentation](https://docs.microsoft.com/en-us/graph/api/overview)

