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

/**
 * Response contract of the auth tab endpoints (testConnection/saveCredentials). The SDK's
 * auth/index.html reads {@code r.isSuccess}, {@code r.errorMessage} and {@code r.url} — when
 * {@code url} is present the page opens it as the Microsoft OAuth popup. Field names must stay
 * in sync with the SDK page (mirrors Outlook 4's AuthenticationResponse).
 */
public class AuthenticationResponse {
    private final boolean isSuccess;
    private final String errorMessage;
    private final String url;

    public AuthenticationResponse(boolean isSuccess, String errorMessage, String url) {
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
        this.url = url;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getUrl() {
        return url;
    }
}
