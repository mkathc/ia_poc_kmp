# Functional Contract

Source of truth extracted from the current Flutter POC implementation.

## Global Scope

The app validates an AI-First frontend architecture using mock providers, remote mock gateway adapters, local fallback capability, simple dependency injection, and feature-level state holders.

Global debug dimensions:

- `scenario`: `happyPath`, `delayed`, `timeout`, `offline`, `error`, `connectionLost`, `longStream`, `noResults`.
- `provider`: `providerA`, `providerB`. Flutter also defines `azure-like`, `kore-like`, and `fake-ai-gateway`, but UI debug selectors use Provider A/B.
- `slaThreshold`: default 3 seconds.
- `useRemoteGateway`: config flag selecting remote adapters or fake in-app adapters.

## POC 1 Chat Streaming

Purpose:

- Validate token-by-token simulated streaming.
- Validate chat SLA and timeout fallback.
- Validate SSE-to-domain event mapping.
- Keep UI decoupled from concrete providers and transport.

Main flow:

```text
ChatScreen
  -> ChatCubit
  -> ChatExperienceController
  -> SendChatMessageUseCase
  -> ChatService
  -> RemoteChatService or FakeChatService
  -> Mock Gateway or FakeAiGatewayClient
```

Expected behavior:

- User sends a message.
- UI appends a user message and an empty assistant placeholder.
- `ChatExperienceController` starts request timing and SLA timer.
- `message.started` marks stream start.
- `message.delta` accumulates assistant text.
- `message.completed` marks assistant message as no longer streaming.
- If first delta exceeds SLA, fallback banner is shown, but streaming can continue.
- Retry resends the last user message.
- WhatsApp fallback is visual only through `HandoffService`.

Supported scenarios:

- `happyPath`: streams normally and completes.
- `delayed`: delayed first token, usually still below SLA in fake local client.
- `timeout`: first token arrives after SLA; fallback remains visible while stream continues.
- `offline`: HTTP 503 or chat error code `offline`; auto-starts offline emergency support.
- `error`: controlled error event; shows retry/support actions.
- `connectionLost`: stream starts and may emit partial text, then closes without completion or emits code `connectionLost`; auto-starts offline emergency support.
- `longStream`: 80-100 deltas; verifies accumulation and UI stability.

Acceptance criteria:

- Streaming text is visible incrementally.
- No assistant bubble remains permanently empty.
- SLA fallback can coexist with streaming/completed messages.
- UI never references `FakeAiGatewayClient`, `RemoteChatService`, or provider internals.

## POC 2 Provider Swap / Decoupling

Purpose:

- Validate that provider changes do not affect UI, state holder logic, use cases, or contracts.

Main rules:

- Provider selection updates `PocConfig`.
- Remote requests include `provider=<provider.id>`.
- Fake gateway branches internally by provider but returns the same public models/events.
- UI only renders state and calls state-holder methods.

Expected behavior:

- Changing Provider A/B at runtime affects outgoing requests.
- Chat maps provider-specific SSE payloads to the same `ChatEvent` contract.
- Home maps provider-specific personalization to `HomeExperience`.
- Search maps provider-specific search responses to `SearchResult`.

Acceptance criteria:

- Provider swap does not modify UI components.
- Provider-specific behavior lives in mock backend, fake gateway, or data adapter.
- Contract layer remains stable.

## POC 3 Offline Emergency Inline Flow

Purpose:

- Validate local deterministic continuity when cloud chat fails.
- Validate offline guided conversation without LLM, NLP, or backend.
- Validate local report creation and simulated sync.

Trigger conditions:

- Automatic: `offline`, `connectionLost`.
- Manual option: `timeout`, `error`, and fallback banner actions.

Inline flow:

```text
Cloud assistant fails
  -> ChatCubit starts local emergency support
  -> User selects emergency type
  -> GetEmergencyGuideUseCase loads local guide
  -> ChatCubit displays deterministic questions
  -> User answers options
  -> CreateEmergencyReportUseCase persists pendingSync report
  -> SyncPendingEmergencyActionsUseCase marks report synced when connectivity/config permits
```

Emergency type options:

- Accidente vehicular
- Emergencia médica
- Robo / asistencia

Guided conversation for accident flow:

1. `¿Te encuentras en una zona segura?` Options: `Sí`, `No`.
2. `¿Hay personas heridas?` Options: `Sí`, `No`, `No estoy seguro`.
3. `¿Deseas registrar un reporte para sincronizarlo más tarde?` Options: `Sí`, `No`.

Expected behavior:

- Every question is shown as an assistant message.
- Every answer is shown as a user message.
- If the last answer is `Sí`, create local `EmergencyReport(status=pendingSync)`.
- If the last answer is `No`, show local guidance continuation message.
- Sync action changes pending reports to `synced` and appends assistant confirmation.
- No external emergency screen navigation is required.

Acceptance criteria:

- Works without backend and without internet.
- No LLM or generated text.
- Conversation state remains inside Chat.
- Emergency service is accessed only through use cases/contracts.

## POC 4 Home Personalization

Purpose:

- Validate runtime personalization, dynamic rendering, local cache, degraded states, and provider swap.

Main flow:

```text
HomeScreen
  -> HomeCubit
  -> GetHomeExperienceUseCase
  -> PersonalizationService
  -> RemotePersonalizationService or FakePersonalizationService
```

Debug controls:

- Scenario dropdown.
- Provider dropdown.
- Profile dropdown: `activeClaim`, `pendingPayment`, `newUser`, `benefitsFocused`.

Profile mapping:

- Profile is not sent as a new backend field.
- Profile maps to existing request context fields: `hasActiveClaim`, `hasPendingPayment`, `lastInteraction`, `locale`.

Expected behavior:

- `happyPath`: load remote/fake cards and next best actions, then cache.
- `delayed`: loading state before content.
- `timeout`: degraded loading message if request exceeds SLA; late response may still populate content.
- `offline` / `error`: use cache by key `home_<providerId>_<profileName>` if available; otherwise show controlled error.
- Provider/profile changes clear stale visual content and refresh.

Acceptance criteria:

- Cache never leaks between provider/profile combinations.
- UI renders `HomeExperience` only.
- Action taps are logged, no real navigation.

## Intelligent Search

Purpose:

- Validate IA-07 Intelligent Search, IA-08 Assisted Navigation, and IA-10 Intent Detection.
- Search behaves as intent-based experience, not a plain keyword result list.

Main flow:

```text
SearchScreen
  -> SearchCubit
  -> SearchUseCase
  -> SearchService
  -> RemoteSearchService or FakeSearchService
```

Debug controls:

- Scenario dropdown.
- Provider dropdown.
- Search profile dropdown: `insurance`, `payments`, `claims`, `benefits`.

Expected behavior:

- User submits query.
- State shows loading.
- Result shows detected intent, results, and suggested actions.
- Suggested actions log interaction only.
- Cache key: `search_<providerId>_<profileName>_<normalizedQuery>`.

Supported scenarios:

- `happyPath`: returns intent, results, and actions.
- `delayed`: visible loading.
- `timeout`: fallback message while request continues.
- `offline` / `error`: cache fallback if available; otherwise controlled error.
- `noResults`: empty state.

Acceptance criteria:

- Detected intent is visible.
- No provider logic in UI.
- Provider/profile/query cache isolation is enforced.

