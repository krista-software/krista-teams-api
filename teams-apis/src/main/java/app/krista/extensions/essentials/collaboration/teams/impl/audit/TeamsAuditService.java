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

package app.krista.extensions.essentials.collaboration.teams.impl.audit;

import app.krista.extension.authorization.MustAuthorizeException;
import app.krista.extensions.util.KeyValueStore;
import app.krista.sdk.msauth.extension.audit.AuditData;
import app.krista.sdk.msauth.extension.audit.AuditRecorder;
import app.krista.sdk.msauth.extension.service.MsAuthAuditService;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.Map;

/**
 * Teams audit service — thin delegate around the SDK's {@link MsAuthAuditService}.
 *
 * <p>All audit features (duration tracking, user resolution via AuthorizationContext,
 * per-request log capture, auth error classification) are provided generically by
 * the SDK. This class simply wires the SDK service into HK2 for Teams.</p>
 */
@Service
public class TeamsAuditService implements AuditRecorder {

    private final MsAuthAuditService delegate;

    @Inject
    public TeamsAuditService(KeyValueStore keyValueStore) {
        // NOTE: MsAuthAuditService keys its KV entries by extension id ("teams") only,
        // not by invoker id. If multiple invokers of this extension ever share the same
        // KeyValueStore instance, their audit records would collide. This is inherited
        // SDK behavior (same as Outlook 4) and is moot if/when the platform scopes
        // KeyValueStore per invoker.
        this.delegate = new MsAuthAuditService(keyValueStore, "teams");
    }

    @Override
    public void record(String invokerId, String eventType, String status,
                       String catalogName, String reason, String exceptionClass,
                       Map<String, String> metadata) {
        delegate.record(invokerId, eventType, status, catalogName, reason, exceptionClass, metadata);
    }

    /**
     * Convenience method for recording MustAuthorizeException auth errors.
     * Uses the SDK's built-in error classification.
     */
    public void recordAuthError(String invokerId, String catalogName, MustAuthorizeException cause) {
        delegate.recordAuthError(invokerId, catalogName, cause);
    }

    /**
     * Returns filtered audit data from KV store.
     */
    public AuditData getData(String invokerId, Long fromTs, Long toTs, String type, String status) {
        return delegate.getData(invokerId, fromTs, toTs, type, status);
    }

    /**
     * Resets auth error counters and removes auth error records.
     * Called when credentials are saved successfully.
     */
    @Override
    public void resetAuthCounters() {
        delegate.resetAuthCounters();
    }

    /**
     * Clears records within [fromTs, toTs] and recalculates the summary.
     */
    public void clearRecords(String invokerId, Long fromTs, Long toTs) {
        delegate.clearRecords(invokerId, fromTs, toTs);
    }

    /** Starts log capture and timestamps the beginning of a catalog request. */
    public void startRequest() {
        delegate.startRequest();
    }

    /** Returns the all-time global counter (never resets). */
    public java.util.Map<String, Object> getGlobalCounter() {
        return delegate.getGlobalCounter();
    }

    /** Returns archive index (list of compressed archive metadata). */
    public java.util.List<java.util.Map<String, Object>> getArchiveIndex() {
        return delegate.getArchiveIndex();
    }

    /** Returns decompressed archived records by key. */
    public java.util.List<app.krista.sdk.msauth.extension.audit.AuditRecord> getArchivedRecords(String key) {
        return delegate.getArchivedRecords(key);
    }

    /** Deletes a specific archive from KV store. */
    public void deleteArchive(String key) {
        delegate.deleteArchive(key);
    }

    /** Manually archives all current records. */
    public void archiveAll() {
        delegate.archiveAll();
    }
}
