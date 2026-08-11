# Per-Flow Sequence Diagrams

## Service Configuration Lookup (GET /config-by-env/:serviceName)

```mermaid
sequenceDiagram
  participant C as Client
  participant S as This service
  participant M as Mongo
  participant GH as GitHub raw API

  C->>S: GET /config-by-env/:serviceName
  S->>M: Read slug/deployed/latest/dependency config data

  alt Slug has empty app config
    S->>GH: Fetch conf/application.conf for service version
    GH-->>S: File content (or not found)
  end

  S->>S: Compose layered config sources and diffs
  S-->>C: 200 JSON config-by-environment payload
```

## GitHub Webhook-Driven Refresh (POST /webhook)

```mermaid
sequenceDiagram
  participant GH as GitHub webhook sender
  participant S as This service
  participant M as Mongo
  participant D as Downstream source (GitHub/API/Artifactory)

  GH->>S: POST /webhook (repo, branch)
  S->>S: Decide action by repo+branch

  alt Supported repo on main
    S->>D: Pull latest config data (zip/file/hash)
    D-->>S: Data
    S->>M: Upsert corresponding collection(s) and update hash state
    S-->>GH: 202 Accepted
  else Unknown repo or branch
    S-->>GH: 202 Accepted (no update)
  end
```
## Deployment Event Processing (SQS)

```mermaid
sequenceDiagram
  participant Q as Queue
  participant S as This service
  participant M as Mongo
  participant GH as GitHub raw API

  Q->>S: deployment event message
  S->>S: Validate event type and environment

  alt deployment-complete and update required
    S->>GH: Fetch app-config files for referenced commit IDs
    GH-->>S: Config file content
    S->>M: Write deployedConfig and deploymentConfig
    S->>M: Recompute and write appliedConfig snapshot
    S->>M: Write deploymentEvents record
    S-->>Q: Delete message
  else undeployment-complete
    S->>M: Clear env flag and delete deployed/applied/deployment config
    S-->>Q: Delete message
  else invalid/unhandled
    S-->>Q: Ignore message (not deleted)
  end
```
## Slug Event Processing (SQS)

```mermaid
sequenceDiagram
  participant Q as Queue
  participant S as This service
  participant AP as artefact-processor
  participant M as Mongo

  Q->>S: slug job message
  S->>S: Validate payload and jobType

  alt JobAvailable(slug)
    S->>AP: Get slug info
    AP-->>S: Slug info
    S->>AP: Get dependency configs
    AP-->>S: Dependency configs
    S->>M: Upsert slug info + dependency configs + app routes
    S-->>Q: Delete message

  else JobDeleted(slug)
    S->>M: Delete slug info and app routes
    S-->>Q: Delete message

  else invalid/mismatch
    S-->>Q: Ignore message (not deleted)
  end
```

## Dead-Letter Queue Processing

```mermaid
sequenceDiagram
  participant Q as Queue
  participant S as This service

  Q->>S: Dead-letter message
  S->>S: Mark as terminally failed
  S-->>Q: Delete message
```

## Scheduled Configuration Synchronisation

```mermaid
sequenceDiagram
  participant T as Scheduler trigger
  participant S as This service
  participant M as Mongo
  participant D as Downstream source (Artifactory/GitHub/API)

  T->>S: Run config-scheduler tick
  S->>M: Acquire distributed Mongo lock

  alt Lock acquired
    S->>D: Refresh alert/internal-auth/upscan sources
    D-->>S: Latest data
    S->>M: Upsert alert/internal-auth/upscan collections
    S->>M: Recompute and store resource usage snapshots
    S->>M: Release lock
  else Lock not acquired
    S->>S: Skip this run
  end
```

## Missed Webhook Catch-Up (Scheduled Job)

```mermaid
sequenceDiagram
  participant T as Scheduler trigger
  participant S as This service
  participant M as Mongo
  participant D as Downstream source (GitHub/API/Artifactory)

  T->>S: Run missed-webhook-events tick
  S->>M: Acquire distributed Mongo lock

  alt Lock acquired
    S->>D: Re-fetch all config-as-code datasets
    D-->>S: Latest datasets
    S->>M: Replace/upsert corresponding collections
    S->>M: Release lock
  else Lock not acquired
    S->>S: Skip this run
  end
```

## Slug Metadata Reconciliation (Scheduled Job)

```mermaid
sequenceDiagram
  participant T as Scheduler trigger
  participant S as This service
  participant M as Mongo
  participant R as releases-api
  participant TR as teams-and-repositories

  T->>S: Run slug-metadata-scheduler tick
  S->>M: Acquire distributed Mongo lock

  alt Lock acquired
    S->>M: Read known/latest slug state
    S->>R: Get what's running where
    R-->>S: Deployment view
    S->>TR: Get active/decommissioned repos
    TR-->>S: Repo status
    S->>S: Decide remove/update/skip per service+environment
    S->>M: Apply flag updates and deployment cleanup/update writes
    S->>M: Release lock
  else Lock not acquired
    S->>S: Skip this run
  end
```

