# Metrics and Scoring

This document defines current metrics, pending metrics, and a scoring matrix for empirical stack comparison.

## Metrics Captured Today

### Chat

Captured by `PerformanceTracker`, `ChatExperienceController`, and `MetricsCollector`.

- `requestStart`
- `firstTokenReceived`
- `responseCompleted`
- `timeToFirstToken`
- `totalResponseTime`
- `fallbackTriggered`
- `fallbackType`
- `errorCode`
- `provider`
- `scenario`

Export model:

- `AiMetricEvent`
- Export format: `MetricsCollector.exportJson()`.

### Home

Captured by `PerformanceTracker` and logs.

- `homeRequestStart`
- `homeResponseCompleted`
- `totalHomeRefreshTime`
- cache hit/miss through logs
- degraded experience through logs

### Search

Captured by `PerformanceTracker` and logs.

- `searchRequestStart`
- `searchResponseCompleted`
- `totalSearchTime`
- `searchFallbackTriggered`
- detected intent log
- cache hit/miss logs

### Emergency

Captured by `PerformanceTracker` and logs.

- `emergencyGuideLoadStart`
- `emergencyGuideLoadCompleted`
- `timeToEmergencyGuide`
- `emergencySyncStart`
- `emergencySyncCompleted`
- conversation logs:
  - `conversation started`
  - `question displayed`
  - `answer selected`
  - `conversation completed`
  - `report created`
  - `report synced`

## Pending Metrics

Not implemented in Flutter POC yet:

- `coldStart`
- FPS / frame drops
- CPU usage
- memory usage
- UI interaction latency
- scroll performance under `longStream`
- cache read/write latency
- provider swap affected layers count
- bundle size / APK size comparison
- battery usage
- accessibility metrics

## Measurement Guidance by Stack

### Flutter

- Keep `PerformanceTracker` and `MetricsCollector` for POC metrics.
- Add `SchedulerBinding` / DevTools / integration test hooks for FPS and frame timing.
- Use platform tooling for CPU/memory.

### KMP

- Shared module should expose a metrics collector equivalent.
- Use Kotlin `TimeSource` or platform clock.
- Android: use Macrobenchmark, Perfetto, Android Studio profiler.
- iOS: use Instruments if KMP targets iOS.
- Compose: track recomposition and frame timing where applicable.

### React Native

- Use a TypeScript metrics collector equivalent.
- Capture JS timestamps with `performance.now()`.
- Use Flipper/React DevTools/native profilers for JS/UI thread, memory, and FPS.
- For streaming, capture first event timestamp and completion timestamp at adapter/controller level.

## Standard Metric Definitions

| Metric | Definition | Start | End |
| --- | --- | --- | --- |
| `timeToFirstToken` | Time until first chat delta | user send/request start | first `MessageDelta` |
| `totalResponseTime` | Full chat response duration | user send/request start | `MessageCompleted` |
| `fallbackTriggeredTime` | Time to fallback visibility | request start | fallback emitted |
| `homeRefreshTime` | Home personalization duration | load/refresh start | Home loaded/error/cache |
| `searchResponseTime` | Search duration | query submit | loaded/empty/error/cache |
| `timeToEmergencyGuide` | Local guide load duration | emergency type selected | guide loaded |
| `coldStart` | App launch to usable first screen | process start | first interactive frame |
| `FPS` | Render smoothness | scenario run | scenario end |
| `CPU` | Processing cost | scenario run | scenario end |
| `memory` | Runtime memory footprint | scenario run | scenario end |

## Empirical Scoring Matrix

Score each item 1-5 per stack.

| Category | Metric | Weight | Notes |
| --- | --- | ---: | --- |
| Streaming UX | timeToFirstToken | 15 | Lower is better. Include p50/p95. |
| Streaming UX | token loss / ordering | 10 | Must remain 5 if all deltas accumulate correctly. |
| Resilience | SLA fallback correctness | 15 | Timeout fallback must coexist with late stream. |
| Resilience | offline/connectionLost continuity | 15 | Emergency inline auto-start must work. |
| Personalization | homeRefreshTime | 10 | Compare remote and cache paths. |
| Search | searchResponseTime | 10 | Include intent visibility and noResults path. |
| Local capability | timeToEmergencyGuide | 10 | Should be near-instant and backend-free. |
| Architecture | provider swap affected layers | 10 | 5 means adapter/config only, no UI changes. |
| Runtime | FPS / frame drops | 10 | Especially `longStream`. |
| Runtime | CPU/memory | 10 | Compare steady state and stress scenarios. |

Suggested final score:

```text
weightedScore = sum(score * weight) / sum(weight)
```

## Benchmark Scenarios

Minimum comparable runs:

- Chat `happyPath` providerA/providerB.
- Chat `timeout`.
- Chat `connectionLost`.
- Chat `longStream`.
- Home `happyPath`, `offline` with cache, `offline` without cache.
- Home provider/profile changes.
- Search `happyPath`, `noResults`, `offline` with cache.
- Emergency inline flow through report creation and sync.

