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

package app.krista.extensions.essentials.collaboration.teams;

import app.krista.sdk.msauth.extension.annotations.MsAuthExtension;
import app.krista.sdk.msauth.extension.registry.MsAuthExtensionDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

class TeamsExtensionDescriptorTest {

    @Test
    void descriptorReturnsAnnotatedConfigClass() {
        MsAuthExtensionDescriptor descriptor = new TeamsExtensionDescriptor();
        Class<?> config = descriptor.getConfigClass();
        assertEquals(TeamsAuthConfig.class, config);
        MsAuthExtension anno = config.getAnnotation(MsAuthExtension.class);
        assertNotNull(anno);
        assertEquals("teams", anno.extensionType());
        assertEquals("/teams", anno.callbackPathPrefix());
    }

    @Test
    void descriptorIsDiscoverableViaServiceLoader() {
        boolean found = ServiceLoader.load(MsAuthExtensionDescriptor.class).stream()
                .anyMatch(p -> p.type().equals(TeamsExtensionDescriptor.class));
        assertTrue(found, "TeamsExtensionDescriptor must be registered in META-INF/services");
    }
}
