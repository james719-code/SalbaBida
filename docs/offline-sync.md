# Offline Sync

SalbaBida keeps community markers usable when the device has no signal.

## Local Storage

Markers are saved in Room as `OfflineMarker`.

Initial sync state:

```text
PENDING
```

Successful upload state:

```text
SYNCED
```

Non-network failure state:

```text
FAILED
```

## Sync Behavior

1. User creates or edits a marker.
2. The marker is saved locally first.
3. `SyncManager.syncNow()` enqueues a one-time WorkManager job.
4. WorkManager waits for network connectivity.
5. `MarkerSyncWorker` uploads pending and failed markers to Firestore.
6. The local row is updated with `SYNCED`, `syncedAt`, and `firestoreId`.

## Retry Behavior

- Network failures return `Result.retry()`.
- Non-network failures mark the marker as `FAILED`.
- A periodic sync runs every hour as a fallback.

## User Impact

- Residents can continue adding important locations offline.
- Responders can gather field data during outages.
- Data is reconciled when the phone reconnects.
