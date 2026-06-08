# Frontend Architecture Contract

This contract defines the architecture to replicate in Flutter, KMP, and React Native without copying framework-specific implementation details.

## Layer Map

```text
UI
  -> State holder
  -> Experience orchestration / feature logic
  -> UseCase
  -> Contract
  -> Adapter
  -> Transport or local store
```

## UI Layer

Responsibilities:

- Render immutable state.
- Dispatch user intents to state holder methods.
- Display debug controls for scenario/provider/profile.
- Never branch on provider-specific behavior.
- Never call services, gateways, HTTP clients, cache, or local emergency service directly.

Flutter implementation:

- `ChatScreen`, `HomeScreen`, `SearchScreen`, `EmergencyScreen`.
- Widgets render state from Cubits.

KMP/RN equivalent:

- Compose screens or React components subscribe to ViewModel/store state.
- UI invokes ViewModel/store actions only.

## State Holder Layer

Responsibilities:

- Own screen state.
- Bind UI intents to use cases.
- Keep feature-specific UX state.
- Manage retry, refresh, debug config updates, and local conversation progression.

Flutter implementation:

- `ChatCubit`
- `HomeCubit`
- `SearchCubit`
- `EmergencyCubit`

Equivalent:

- KMP: ViewModel with `StateFlow`.
- RN: Zustand/Redux/Context reducer plus async actions.

Rules:

- State holder may depend on use cases, orchestration, config updater, logger, tracker.
- State holder must not depend on concrete remote provider implementations.
- Chat may depend on emergency use cases for inline offline continuity.

## Experience Orchestration

Responsibilities:

- Process AI events into view-ready events.
- Manage SLA timers.
- Resolve fallbacks through resilience policy.
- Capture chat metrics.

Flutter implementation:

- `ChatExperienceController`
- Emits `ChatViewState`: streaming, completed, fallback, error.

Equivalent:

- KMP: shared orchestrator returning `Flow<ChatExperienceState>`.
- RN: framework-independent TypeScript controller returning async iterator, observable, or callback stream.

## UseCases

Responsibilities:

- Keep feature entrypoints stable.
- Coordinate contracts and cache where needed.

Flutter implementation:

- `SendChatMessageUseCase`
- `GetHomeExperienceUseCase`
- `SearchUseCase`
- `GetEmergencyGuideUseCase`
- `CreateEmergencyReportUseCase`
- `SyncPendingEmergencyActionsUseCase`

Rules:

- UseCases depend on contracts or cache abstractions.
- UI must not bypass use cases.

## Contract Layer

Contracts:

- `ChatService`
- `PersonalizationService`
- `RecommendationService`
- `SearchService`
- `HandoffService`
- `EmergencySupportService`

Rules:

- Contracts define domain behavior, not transport details.
- Provider swap must be implemented by adapter selection/config, not UI changes.
- KMP/RN must keep equivalent interfaces.

## Remote Adapters

Flutter implementation:

- `RemoteChatService`: HTTP POST SSE to `/chat/stream`.
- `RemotePersonalizationService`: POST JSON to `/home/personalization`.
- `RemoteSearchService`: POST JSON to `/search`.

Responsibilities:

- Build URLs with current `PocConfig`.
- Map backend payloads to common models/events.
- Convert HTTP/SSE failures into controlled domain errors.

Rules:

- Adapters can know endpoint details.
- Adapters can know provider/scenario query params.
- Adapters must return common contracts only.

## Local Adapters

Flutter implementation:

- `FakeChatService`
- `FakePersonalizationService`
- `FakeSearchService`
- `FakeRecommendationService`
- `FakeHandoffService`
- `LocalEmergencySupportService`

Responsibilities:

- Provide local deterministic behavior for POCs.
- Support no-backend execution.
- Store emergency reports in local cache.

## Resilience

Flutter implementation:

- `SlaTimer`
- `ExperienceResiliencePolicy`
- `ExperienceFallback`
- `NetworkMonitor`

Rules:

- SLA is evaluated outside UI.
- Chat timeout fallback must coexist with streaming.
- Offline and connectionLost chat failures auto-start local emergency flow.
- Home/Search cache fallback is use-case-driven.

## Observability

Flutter implementation:

- `PerformanceTracker`
- `AiEventLogger`
- `MetricsCollector`
- `AiMetricEvent`

Rules:

- Capture request start, first token, completion, fallback, cache, errors, and emergency flow events.
- Logs are acceptable for POC; production should replace with platform telemetry.
- Metrics model should remain comparable across stacks.

## Cache

Flutter implementation:

- `LocalCacheStore`, in-memory.

Cache keys:

- Home: `home_<providerId>_<profileName>`.
- Search: `search_<providerId>_<profileName>_<normalizedQuery>`.
- Emergency reports: `emergency_reports`.

Rules:

- Cache isolation by provider/profile/query is required.
- Current Flutter cache is in-memory; persistent disk cache is pending.

## Config and Debug Controls

Flutter implementation:

- `PocConfig`
- `FakeGatewayScenario`
- `ProviderMock`
- `MockUserProfile`
- `SearchProfile`
- `AppDi.updatePocConfig`

Rules:

- Debug controls update config at runtime.
- Remote adapters read config lazily through function references.
- Do not store stale immutable config in remote adapters.

## Dependency Injection

Flutter implementation:

- `AppDi` singleton, no DI framework.

Rules:

- Other stacks may use native DI patterns.
- DI must register contracts, use cases, state holders, config, logger, tracker, cache, and adapters.
- Adapter selection must support fake/local vs remote through config.

