# KMP Implementation Guide

Goal: replicate the current Flutter POC behavior in Kotlin Multiplatform without copying Flutter architecture literally.

## Suggested Structure

```text
shared/
  models/
  contracts/
  usecases/
  data/
    remote/
    local/
  resilience/
  observability/
  config/

composeApp/
  chat/
  home/
  search/
  emergency/
  di/
```

## Equivalences

| Flutter | KMP |
| --- | --- |
| Cubit | ViewModel with `StateFlow` |
| Dart `Stream<ChatEvent>` | Kotlin `Flow<ChatEvent>` |
| Equatable models | Kotlin `data class` / sealed classes |
| Contracts | Kotlin interfaces |
| RemoteService | Ktor client adapter |
| `LocalCacheStore` | shared cache abstraction / in-memory implementation |
| `PocConfig` | shared immutable config data class |
| `ChatExperienceController` | shared orchestrator returning `Flow<ChatExperienceState>` |
| `debugPrint` logger | logger abstraction with platform sink |

## Shared Module

Place common models, contracts, use cases, resilience, observability, config, and most adapters in `shared`.

Recommended shared contracts:

```kotlin
interface ChatService {
    fun sendMessage(request: ChatRequest): Flow<ChatEvent>
    suspend fun cancelMessage(messageId: String)
    suspend fun getConversation(conversationId: String): List<ChatMessage>
}
```

Use sealed classes for events:

```kotlin
sealed interface ChatEvent
data class MessageStarted(val messageId: String) : ChatEvent
data class MessageDelta(val messageId: String, val delta: String) : ChatEvent
data class MessageCompleted(val messageId: String, val text: String) : ChatEvent
data class ChatError(val messageId: String, val message: String, val code: String?) : ChatEvent
```

## Networking

Use Ktor:

- JSON POST for `/home/personalization` and `/search`.
- SSE or streaming HTTP for `/chat/stream`.
- Query params must include current `scenario` and `provider`.

KMP detail:

- If a multiplatform SSE client is awkward, implement platform-specific streaming adapters behind the same `ChatService`.
- Preserve mapping rules from `api-contract.md`.

## State Holders

Compose screens should use ViewModels:

- `ChatViewModel`
- `HomeViewModel`
- `SearchViewModel`
- `EmergencyViewModel` if retaining secondary screen

Each ViewModel exposes:

- `StateFlow<FeatureState>`
- methods equivalent to Flutter Cubit actions.

Do not put provider/scenario/backend rules in Compose UI.

## Chat

Implement:

- `SendChatMessageUseCase`
- `ChatExperienceController`
- `ChatViewState`
- `ChatViewModel`

Rules:

- SLA timer belongs in controller/orchestrator.
- Timeout fallback must not cancel late stream.
- Offline and connectionLost must auto-start local emergency inline flow.
- The local emergency guided conversation can live in `ChatViewModel` or a reusable `EmergencyInlineController` in shared.

## Home

Implement:

- `PersonalizationService`
- `RemotePersonalizationService`
- `FakePersonalizationService`
- `RecommendationService`
- `GetHomeExperienceUseCase`

Cache:

- Key: `home_<providerId>_<profileName>`.
- Use in-memory cache for parity first.
- Persistent cache can be a later benchmark dimension.

## Search

Implement:

- `SearchService`
- `RemoteSearchService`
- `FakeSearchService`
- `SearchUseCase`

Cache:

- Key: `search_<providerId>_<profileName>_<normalizedQuery>`.

UI must show:

- detected intent
- results
- suggested actions
- fallback/cache banner

## Emergency Offline

Implement local deterministic service:

- `LocalEmergencySupportService`
- `GetEmergencyGuideUseCase`
- `CreateEmergencyReportUseCase`
- `SyncPendingEmergencyActionsUseCase`

Report sync is simulated:

- `pendingSync` -> `synced`.
- No backend required.

## DI

Use a simple DI container or Koin if already standard in the KMP project.

Required registrations:

- Config holder/updater.
- HTTP client.
- Remote/fake services.
- Use cases.
- ViewModels.
- Logger, metrics, cache, resilience policy.

Adapter selection:

```text
useRemoteGateway=false -> fake/local adapters
useRemoteGateway=true -> remote mock gateway adapters
```

## Do Not Implement Yet

- Real Azure/OpenAI/Kore/Genesys integration.
- LLM local inference.
- Real emergency backend sync.
- Real navigation for suggested actions.

