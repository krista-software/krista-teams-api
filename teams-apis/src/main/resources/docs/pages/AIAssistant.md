# AI Assistant Tab

## Overview

The **AI Assistant** tab lets you ask questions about Microsoft Teams in plain English and send messages, without writing catalog requests or knowing Microsoft Graph. You type a natural-language request; an AI model plans the exact Microsoft Graph API call; the extension executes it against your authenticated Teams connection and shows the result as a table.

> The AI Assistant reuses the credentials configured on the **Authentication** tab. You must complete authentication there first — see [Authentication](Authentication.md).

---

## Setup

The tab guides you through three steps: **Connect → Configure MCP → Ready**.

### 1. Connect

Confirms the extension is authenticated (via the Authentication tab). If it isn't, complete authentication first — the AI Assistant cannot call Microsoft Graph without a valid token.

### 2. Configure the AI provider

Under **AI Settings**, choose a provider and paste its API key:

| Provider | Used model family |
|----------|-------------------|
| Anthropic (Claude) | Claude |
| OpenAI | GPT |
| Google | Gemini |

The API key is stored in Krista's secure key-value store. Without a configured key the tab returns: *"AI API key is not configured. Please set it in the AI Assistant tab → AI Settings."*

### 3. Ready

Once connected and configured, use the query box to ask questions or send messages.

---

## What you can ask

The assistant plans a single Microsoft Graph call per request. It supports **reads** (any GET) and a restricted set of **writes** (sending messages only).

### Reading

- **List teams** — "List my joined teams"
- **List channels** — "What channels are in the Marketing team?"
- **Read channel messages** — "Get channel messages for channel = SimRamJalTest"
- **Read chats / chat messages** — "Show my recent chats", "Get messages in my chat with Priya"
- **Search messages (KQL)** — "Find messages about the quarterly report", "Messages from Priya last week"

### Sending

- **Send a channel message** — "Send 'Standup at 10' to the SimRamJalTest channel"
- **Send a chat message** — "Send 'on my way' to my chat with Alex"

> **Referring to teams/channels by name.** You don't need Microsoft Graph IDs. When you name a team or channel (e.g. `channel = SimRamJalTest`), the assistant resolves the name to the underlying ID automatically by listing your joined teams and their channels and matching the display name. If the name can't be found, you'll get a clear message such as *"Could not find a channel named 'SimRamJalTest'."*

---

## Safety: what the assistant is allowed to do

Because the AI plans the Graph call from free text, execution is constrained by a strict allowlist enforced **before any HTTP call**:

- **GET** — any read is allowed.
- **POST** — allowed **only** to:
  - the Microsoft Graph Search endpoint (`/search/query`, a read-only search), and
  - send a channel message (`/teams/{id}/channels/{id}/messages`) or a chat message (`/chats/{id}/messages`).
- Everything else — other POST targets, message **replies** and other sub-resources, and any `PUT`/`PATCH`/`DELETE` — is **rejected**.

If a request falls outside this set, the assistant responds: *"This request cannot be executed. Allowed: any GET (read), and POST only to Microsoft Graph Search or to send a channel/chat message."*

---

## Results

Successful queries render as a table. Columns are derived dynamically from whatever fields Microsoft Graph returns, so teams, channels, chats, and messages each show their own relevant columns. A message send returns a confirmation row.

---

## Troubleshooting

| Symptom | Cause | Resolution |
|---------|-------|------------|
| "AI API key is not configured" | No provider/key set | Set them under **AI Settings** |
| "Not connected. Configure credentials in the Authentication tab." | No valid Teams token | Complete authentication on the Authentication tab |
| "Could not parse a valid Graph API call from the AI response." | The model couldn't turn the request into a single Graph call (often an underspecified request) | Rephrase more specifically, e.g. name the team and channel |
| "Could not find a channel named '…'." | Name didn't match any channel in your joined teams | Check the exact channel display name, or name its team too |
| "This request cannot be executed. …" | The planned call is outside the allowlist (e.g. a delete, or posting to a non-message endpoint) | Only reads and channel/chat message sends are supported |
| "I can only answer questions related to Teams…" | The question isn't about Teams | Ask a Teams-related question |

---

## See Also

- [Authentication](Authentication.md) — configure the connection the assistant uses
- [Teams Audit Dashboard](AuditDashboard.md) — monitor request activity and credential health
