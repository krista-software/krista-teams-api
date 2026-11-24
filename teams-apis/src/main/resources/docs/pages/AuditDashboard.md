# Teams Audit Dashboard Tab

## Overview

The **Teams Audit Dashboard** tab gives you a real-time view of how the extension is being used and whether its Microsoft connection is healthy. Every catalog request the extension runs is recorded as an audit event, and the dashboard summarizes those events with counters, charts, and a searchable history.

> The dashboard reads audit data recorded for this invoker's connection. It reflects activity for the configured Teams connection — see [Authentication](Authentication.md).

---

## What it shows

### Credential health

An at-a-glance indicator of whether the connection is currently usable — for example, whether a valid refresh token is present and authentication is working. Use this to confirm a connection is healthy before troubleshooting a workflow, or to spot when re-authorization is needed.

### Activity charts

Visual trends of request activity over time, so you can see volume and the success/failure ratio across the extension's catalog requests at a glance.

### Audit event history

A chronological list of recorded events. Each catalog request produces events in the **Catalog Requests** group:

| Event | Meaning |
|-------|---------|
| `CATALOG_REQUEST_TRIGGERED` | A catalog request started executing |
| `CATALOG_REQUEST_SUCCESS` | The request completed successfully |
| `CATALOG_REQUEST_FAILURE` | The request failed (with the captured error) |

Events also carry context such as the request name, a `triggeredBy` actor (the user/account email where available), and the outcome message — useful for tracing who ran what and why something failed.

### CSV export

The recorded audit history can be exported to CSV (file name prefixed `teams-audit`) for offline analysis, reporting, or sharing with an administrator.

---

## Common uses

- **Verify a connection works** before or after configuration — check credential health and look for a recent `CATALOG_REQUEST_SUCCESS`.
- **Diagnose failures** — find the `CATALOG_REQUEST_FAILURE` event for a request and read its captured error message.
- **Track usage** — see which requests run most often and their success rate over time.
- **Audit activity** — use the `triggeredBy` actor and timestamps to see who performed an action.

---

## Relationship to the other tabs

- The **Authentication** tab establishes the connection; the dashboard's credential-health indicator reflects that connection's state.
- The **AI Assistant** tab's queries and message sends also run as catalog activity, so they appear in the audit history alongside workflow-triggered requests.

---

## See Also

- [Authentication](Authentication.md) — configure and authorize the connection
- [AI Assistant](AIAssistant.md) — natural-language Teams queries and message sending
