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

package app.krista.extensions.essentials.collaboration.teams.service;

import org.junit.Test;

/**
 * Test class to demonstrate Microsoft Graph API responses for Get Group Chat by Name functionality.
 * This test will show the actual Microsoft responses for different input scenarios.
 *
 * NOTE: This test requires proper Microsoft Graph authentication to be configured.
 * The test will show the actual API requests and responses in the console output.
 */
public class ChatServiceTest {

    /**
     * This test demonstrates how to see Microsoft Graph API responses.
     * To run this test with actual API calls, you need to:
     * 1. Set up proper authentication
     * 2. Have access to Teams group chats
     * 3. Run the test and check console output for API requests/responses
     */

    @Test
    public void demonstrateApiResponseLogging() {
        System.out.println("=== Microsoft Graph API Response Demonstration ===");
        System.out.println("This test shows how the logging is set up to display Microsoft Graph API requests and responses.");
        System.out.println("To see actual API responses, you need to:");
        System.out.println("1. Configure proper Microsoft Graph authentication");
        System.out.println("2. Run the 'Get Group Chat by Name' functionality through the main application");
        System.out.println("3. Check the console output for the following log messages:");
        System.out.println("   - 'Microsoft Graph API Request: GET /me/chats with filter: ...'");
        System.out.println("   - 'Microsoft Graph API Raw Response: {...}'");
        System.out.println("");
        System.out.println("The ChatService.getGroupChatByName() method now includes logging that will show:");
        System.out.println("- The exact filter query sent to Microsoft Graph API");
        System.out.println("- The selected fields requested");
        System.out.println("- The raw JSON response from Microsoft Graph API");
        System.out.println("");
        System.out.println("For your test cases:");
        System.out.println("1. 'abc' - Will likely return an empty response (no matching chats)");
        System.out.println("2. 'Huddle' - Will return chat details if you have access to a group chat named 'Huddle'");
        System.out.println("3. 'Team meeting' - Will return chat details if you have access to a group chat named 'Team meeting'");
        System.out.println("=== End of Demonstration ===");
    }
}
