# Creating Teams App in Azure AD

## Overview

To use the Teams APIs Extension, you must create an Azure AD (Microsoft Entra ID) application that will serve as the identity for your integration. This application provides the credentials (Client ID, Client Secret, Tenant ID) needed to authenticate with Microsoft Teams through the Microsoft Graph API.

This guide provides step-by-step instructions for creating and configuring your Azure AD application.

---

## Prerequisites

Before you begin, ensure you have:

- A Microsoft 365 account with Teams access
- Azure AD (Microsoft Entra ID) admin access
- Permissions to create app registrations in Azure AD
- Permissions to grant admin consent for API permissions

> **Note**: If you don't have admin access, you'll need to work with your Azure AD administrator to complete these steps.

---

## Step-by-Step Instructions

### Step 1: Access Azure Portal

1. Navigate to the [Azure Portal](https://portal.azure.com/)
2. Sign in with your Microsoft 365 admin account

![Azure Portal Home Page](../_media/CreatingTeamsApp_homePage.png)

### Step 2: Navigate to Microsoft Entra ID

1. Under **Azure services**, click **Microsoft Entra ID**
2. If not visible, click **More services** to find Microsoft Entra ID

![Azure Active Directory](../_media/CreatingTeamsApp_azureActiveDirectory.png)

> **Note**: Microsoft Entra ID is the new name for Azure Active Directory.

### Step 3: Open App Registrations

1. On the Microsoft Entra ID Overview page
2. In the left menu under **Manage**, click **App registrations**

![App Registrations](../_media/CreatingTeamsApp_appRegistration.png)

### Step 4: Create New Application

1. At the top of the App registrations page, click **+ New registration**

![New Registration](../_media/CreatingTeamsApp_newRegistration.png)

### Step 5: Register the Application

On the **Register an application** page, configure the following:

#### Application Name
- **Name**: Enter a descriptive name (e.g., "Krista Teams Integration")
- This name will be displayed to users during authentication

#### Supported Account Types
Select: **Accounts in this organizational directory only (Single tenant)**

- This is the most secure option
- Limits access to users in your organization only
- Recommended for enterprise applications

#### Redirect URI
1. Select platform: **Web**
2. Enter the redirect URI (you'll update this later with the actual Krista callback URL)
   - Temporary value: `https://localhost`
   - You'll update this after configuring the extension in Krista

3. Click **Register**

![Register Application](../_media/CreatingTeamsApp_registerApplication.png)

> **Important**: You must update the redirect URI later with the actual callback URL from your Krista extension. See [Extension Configuration](ExtensionConfiguration.md#step-2-configure-redirect-uri) for details.

### Step 6: Copy Client ID and Tenant ID

After registration, you'll be redirected to the application's Overview page.

1. Under **Essentials**, locate and copy:
   - **Application (client) ID** → Save as **Client ID**
   - **Directory (tenant) ID** → Save as **Tenant ID**

![Client ID and Tenant ID](../_media/ExtensionConfiguration_clientIDtenantID.png)

> **Note**: Keep these values secure. You'll need them to configure the Teams APIs Extension in Krista.

---

## Step 7: Configure API Permissions

The application needs specific Microsoft Graph API permissions to access Teams data.

### 7.1 Add API Permissions

1. In the left menu, click **API permissions**
2. Click **+ Add a permission**

![Add a Permission](../_media/CreatingTeamsApp_addAPermission.png)

### 7.2 Select Microsoft Graph

1. On the **Request API permissions** page
2. Click **Microsoft Graph**

![Microsoft Graph](../_media/CreatingTeamsApp_microsoftGraph.png)

### 7.3 Select Delegated Permissions

1. Click **Delegated permissions**

![Delegated Permissions](../_media/CreatingTeamsApp_delegatedPermissions.png)

> **Note**: Delegated permissions are used because the extension operates on behalf of a signed-in user.

### 7.4 Add Required Permissions

Search for and select the following permissions:

#### Core Permissions (Required)

| Permission | Type | Description | Admin Consent Required |
|------------|------|-------------|----------------------|
| `openid` | Delegated | Sign users in | No |
| `offline_access` | Delegated | Maintain access to data | No |
| `User.Read` | Delegated | Sign in and read user profile | No |

#### Teams Permissions (Required)

| Permission | Type | Description | Admin Consent Required |
|------------|------|-------------|----------------------|
| `Team.ReadBasic.All` | Delegated | Read team information | Yes |
| `Channel.ReadBasic.All` | Delegated | Read channel information | Yes |
| `ChannelMessage.Send` | Delegated | Send channel messages | Yes |
| `Chat.ReadWrite` | Delegated | Read and write chat messages | Yes |
| `ChatMessage.Send` | Delegated | Send chat messages | Yes |

**To add each permission:**
1. Type the permission name in the search box
2. Expand the permission category
3. Check the permission checkbox
4. Click **Add permissions**

![Select Permissions](../_media/CreatingTeamsApp_selectPermissions.png)

### 7.5 Grant Admin Consent

After adding all permissions, you must grant admin consent:

1. Review the list of **Configured permissions**
2. Verify all required permissions are listed
3. Click **Grant admin consent for [Your Organization]** button
4. Click **Yes** to confirm
5. Verify each permission shows **Granted for [Your Organization]** with a green checkmark

> **Important**: Admin consent is required for the application to function. Without it, users will not be able to authenticate.

---

## Step 8: Create Client Secret

The client secret is used to authenticate the application with Microsoft Graph API.

### 8.1 Navigate to Certificates & Secrets

1. In the left menu, click **Certificates & secrets**
2. Under **Client secrets**, click **+ New client secret**

![Certificates & Secrets](../_media/CreatingTeamsApp_certificatesAndSecrets.png)

### 8.2 Add Client Secret

1. **Description**: Enter a meaningful description (e.g., "Krista Teams Extension Secret")
2. **Expires**: Select an expiration period
   - Recommended: **24 months** (balance between security and maintenance)
   - For production: Set a reminder to rotate before expiration
3. Click **Add**

![Add Client Secret](../_media/CreatingTeamsApp_addClientSecret.png)

### 8.3 Copy Client Secret Value

**CRITICAL**: The secret value is only displayed once!

1. Immediately copy the **Value** (not the Secret ID)
2. Save it securely (password manager, secure note, etc.)
3. This is your **Client Secret**

![Client Secret](../_media/ExtensionConfiguration_clientSecret.png)

> **Warning**: If you lose the client secret value, you cannot retrieve it. You'll need to create a new secret.

---

## Step 9: Update Redirect URI

After configuring the Teams APIs Extension in Krista, you must update the redirect URI.

### 9.1 Get Callback URL from Krista

1. In Krista, navigate to your Teams APIs Extension
2. Go to the **Details** tab
3. Copy the **Extension Base URL**
4. Append `/teams-apis/callback` to create the full callback URL

**Example**:
```
Extension Base URL: https://your-krista-instance.com/extensions/teams-apis
Callback URL: https://your-krista-instance.com/extensions/teams-apis/teams-apis/callback
```

### 9.2 Update in Azure AD

1. In your Azure AD application, go to **Authentication**
2. Under **Web** platform, click **Add URI**
3. Enter the callback URL from Krista
4. Remove the temporary `https://localhost` URI
5. Click **Save**

> **Note**: The redirect URI must match exactly, including protocol (https://) and path.

---

## Verification

After completing all steps, verify your configuration:

### Checklist

- [ ] Application created in Azure AD
- [ ] Client ID copied and saved
- [ ] Tenant ID copied and saved
- [ ] Client Secret created and saved
- [ ] All 7 required permissions added
- [ ] Admin consent granted for all permissions
- [ ] Redirect URI updated with Krista callback URL

### Test the Configuration

1. In Krista, configure the Teams APIs Extension with your credentials
2. Click **Test Connection**
3. You should be redirected to Microsoft login
4. Sign in and grant permissions
5. You should see: "User authenticated successfully. Save the changes."

If the test succeeds, your Azure AD application is correctly configured!

---

## Security Considerations

### 1. Protect Credentials

- Never commit Client ID, Tenant ID, or Client Secret to version control
- Store credentials in secure password managers
- Limit access to credentials to authorized personnel only
- Use different applications for development and production

### 2. Manage Client Secrets

- Set expiration dates on client secrets
- Rotate secrets before expiration
- Create overlapping secrets during rotation
- Remove old secrets after rotation is complete

### 3. Monitor Application Usage

- Enable audit logging in Azure AD
- Review sign-in logs regularly
- Monitor API usage and quotas
- Set up alerts for suspicious activity

### 4. Limit Permissions

- Only grant the minimum required permissions
- Review permissions quarterly
- Remove unused permissions
- Document why each permission is needed

---

## Troubleshooting

### Issue: Cannot Create App Registration

**Cause**: Insufficient permissions in Azure AD

**Resolution**:
- Contact your Azure AD administrator
- Request "Application Developer" role or higher
- Alternatively, have admin create the application for you

### Issue: Cannot Grant Admin Consent

**Cause**: Not a Global Administrator or Privileged Role Administrator

**Resolution**:
- Contact your Global Administrator
- Request admin consent for the application
- Provide list of required permissions

### Issue: Client Secret Not Visible

**Cause**: Navigated away from the page before copying

**Resolution**:
- Create a new client secret
- Copy the value immediately
- Delete the old secret if needed

### Issue: Redirect URI Mismatch Error

**Cause**: Callback URL doesn't match configured redirect URI

**Resolution**:
- Verify redirect URI in Azure AD matches exactly
- Check for trailing slashes
- Ensure HTTPS is used (not HTTP)
- Confirm the Extension Base URL is correct

---

## Next Steps

After successfully creating your Azure AD application:

1. [Configure the Extension](ExtensionConfiguration.md) - Set up the extension in Krista
2. [Review Authentication](Authentication.md) - Understand OAuth 2.0 flows
3. [Test Your First Request](SendMessageToUser.md) - Send a message to verify setup

---

## See Also

- [Extension Configuration](ExtensionConfiguration.md) - Configure the extension with your credentials
- [Authentication](Authentication.md) - OAuth 2.0 flows and token management
- [Microsoft App Registration Documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/quickstart-register-app)
- [Microsoft Graph Permissions Reference](https://docs.microsoft.com/en-us/graph/permissions-reference)

