/*
 * Teams Api Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.essentials.collaboration.teams.impl.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {

    private Constants() {
    }

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String TEAMS_JAXRS_ID = "teams-apis";
    public static final String CODE = "code";
    public static final String STATE = "state";
    public static final String INVALID_STATE_PARAMETERS = "Invalid state parameters!";
    public static final String WS_CONTACT = "wsContact";
    public static final String IGNORED_EXCEPTION = "IGNORED EXCEPTION";
    public static final String UNAUTHORISED_USER = "Unauthorised user.";
    public static final String USER_AUTHENTICATED_SUCCESSFULLY_SAVE_THE_CHANGES = "User authenticated successfully. Save the changes.";
    public static final String ERROR_OCCURRED_DURING_AUTHORIZATION = "Error occurred during authorization";
    public static final String ERROR_DESCRIPTION = "error_description";
    public static final String ACCESS_TYPE_OFFLINE_APPROVAL_PROMPT_FORCE = "&access_type=offline&approval_prompt=force";
    public static final String TEXT = "Text";
    public static final String HASH = "#";
    public static final String REQUIRED_SCOPE = "openid offline_access Team.ReadBasic.All Channel.ReadBasic.All ChannelMessage.Send ChannelMessage.Read.All Chat.ReadWrite ChatMessage.Send";
    public static final String SCOPE_SEPARATOR = " ";
    public static final String AUTHORITY = "https://login.microsoftonline.com/";
    public static final String USER_ID = "userId";
    public static final String AUTH_CONTEXT_ID = "authContextId";
    public static final String CLIENT_USER_ID = "clientUserId";
    public static final String YOU_ARE_NOT_AUTHORIZED_PLEASE_AUTHORIZE_YOURSELF = "You are not authorized. Please authorize yourself";
    public static final String REFRESH_TOKEN_IS_NO_LONGER_VALID_PLEASE_REAUTHORIZE_YOURSELF = "Refresh Token is no longer valid. Please reauthorize yourself";
    public static final String FAILED_TO_GET_ACCOUNT = "Unable to retrieve the account. Please ensure you are authenticated successfully and try again.";
    public static final String GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_RE_AUTHENTICATION = "Got error for Authentication. So sending for re authentication.";
    public static final String GOT_ERROR_FOR_AUTHENTICATION_SO_SENDING_FOR_AUTHENTICATION = "Got error for Authentication. So sending for authentication";
    public static final String CONDITIONS_NOT_FOUND = "Conditions not found.";

    // ── microsoft-auth-sdk integration ──
    public static final String PUBLIC = "Public";
    public static final String PRIVATE = "Private";
    public static final String AUTH_TYPE = "authType";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String TENANT_ID = "tenantId";
    public static final String EMAIL = "email";
    public static final String UNDER_SCORE = "_";
    public static final String EMPTY_STRING = "";
    public static final String LOCAL_EXTN_URL = "https://extension.local.eng.krista.app";
    public static final String LOCAL_EXTN_REPLACE_URL = "http://localhost:8765";
    public static final String DEFAULT_CALLBACK_PATH = "/extension/api/rest/v3/oauth/callback";
    public static final String EXTENSION_FORWARD_PATH = "/teams-apis/teams/v3/oauth/callback";
    public static final String PER_USER_ATTRS_SUFFIX = "_teamsAttrs";
    public static final String UNSUPPORTED_AUTH = "Unsupported authentication type";

}
