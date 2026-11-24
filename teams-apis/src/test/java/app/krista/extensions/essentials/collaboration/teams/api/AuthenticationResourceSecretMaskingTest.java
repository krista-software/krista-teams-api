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

package app.krista.extensions.essentials.collaboration.teams.api;

import app.krista.extensions.essentials.collaboration.teams.TeamsApisAttributes;
import app.krista.extensions.essentials.collaboration.teams.impl.util.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Covers the pure decision logic in {@link AuthenticationResource#resolveClientSecret}, which
 * backs the masked-placeholder round-trip for clientSecret: getCredentials never returns the
 * real secret (it returns {@link AuthenticationResource#MASKED_SECRET} instead), and
 * saveCredentials/testConnection must recognize that placeholder (or a blank field) as "the user
 * did not change the secret" and reuse the previously stored value.
 */
class AuthenticationResourceSecretMaskingTest {

    private static final String ROUTING = "https://appliance.example.com/extension/api/foo";

    private static TeamsApisAttributes existingWithSecret(String secret) {
        return new TeamsApisAttributes("cid", secret, "tid", "user@example.com", Constants.PRIVATE, ROUTING);
    }

    @Test
    void placeholderWithExistingSecretReturnsExistingSecret() {
        TeamsApisAttributes existing = existingWithSecret("real-secret-value");
        String resolved = AuthenticationResource.resolveClientSecret(AuthenticationResource.MASKED_SECRET, existing);
        assertEquals("real-secret-value", resolved);
    }

    @Test
    void blankWithExistingSecretReturnsExistingSecret() {
        TeamsApisAttributes existing = existingWithSecret("real-secret-value");
        String resolved = AuthenticationResource.resolveClientSecret("", existing);
        assertEquals("real-secret-value", resolved);

        String resolvedFromNull = AuthenticationResource.resolveClientSecret(null, existing);
        assertEquals("real-secret-value", resolvedFromNull);
    }

    @Test
    void genuinelyNewValueIsUsedAsIs() {
        TeamsApisAttributes existing = existingWithSecret("old-secret");
        String resolved = AuthenticationResource.resolveClientSecret("brand-new-secret", existing);
        assertEquals("brand-new-secret", resolved);
    }

    @Test
    void newValueWithNoExistingAttributesIsUsedAsIs() {
        String resolved = AuthenticationResource.resolveClientSecret("brand-new-secret", null);
        assertEquals("brand-new-secret", resolved);
    }

    /**
     * Fail-safe choice: if the UI somehow sends the literal placeholder but there is nothing
     * stored to fall back to (e.g. no prior save, or a load failure), we must NOT let the literal
     * "********" string be persisted/used as a real secret. Returning null (causing the caller to
     * omit clientSecret entirely) is safer than silently treating the placeholder as a real value.
     */
    @Test
    void placeholderWithNoExistingAttributesReturnsNull() {
        String resolved = AuthenticationResource.resolveClientSecret(AuthenticationResource.MASKED_SECRET, null);
        assertNull(resolved);
    }

    @Test
    void placeholderWithExistingAttributesButNoStoredSecretReturnsNull() {
        // Note: passing a null clientSecret to the TeamsApisAttributes constructor falls back to
        // the Krista-managed public secret from config.properties (by design, for Public/krista
        // mode), so to simulate "existing attrs with no real stored secret" we use blank instead.
        TeamsApisAttributes existing = existingWithSecret("");
        String resolved = AuthenticationResource.resolveClientSecret(AuthenticationResource.MASKED_SECRET, existing);
        assertNull(resolved);
    }

    @Test
    void blankWithNoExistingAttributesReturnsBlankAsIs() {
        // Blank isn't the placeholder marker, so with nothing to fall back to we pass it through
        // unchanged rather than fabricating a value.
        String resolved = AuthenticationResource.resolveClientSecret("", null);
        assertEquals("", resolved);
    }
}
