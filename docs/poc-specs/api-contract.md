# API Contract

Base URL for Android emulator:

```text
http://10.0.2.2:3000
```

Configured by `PocConfig.remoteGatewayBaseUrl`.

## Common Query Parameters

Remote AI endpoints use:

- `scenario`: `happyPath`, `delayed`, `timeout`, `offline`, `error`, `connectionLost`, `longStream`, `noResults` where supported.
- `provider`: `providerA` or `providerB`.

## POST /chat/stream

Transport:

- Server-Sent Events over POST.
- Client sends JSON and accepts `text/event-stream`.

Request path:

```text
/chat/stream?scenario=<scenario>&provider=<provider>
```

Request body:

```json
{
  "conversationId": "poc-chat-conversation",
  "message": "necesito ayuda",
  "context": {
    "userId": "poc-user",
    "channel": "flutter",
    "journey": "chat-streaming-poc",
    "locale": "es-PE"
  },
  "provider": "providerA",
  "scenario": "happyPath"
}
```

SSE events:

```text
event: message.started
data: {"messageId":"msg-1","provider":"providerA"}

event: message.delta
data: {"messageId":"msg-1","delta":"Respuesta parcial "}

event: message.completed
data: {"messageId":"msg-1","text":"Respuesta final opcional"}

event: error
data: {"messageId":"msg-1","message":"Controlled error","code":"fake_error"}

event: handoff.suggested
data: {"messageId":"msg-1","reason":"Whatsapp handoff suggested"}
```

Mapping rules:

- `message.started` -> `MessageStarted(messageId)`.
- `message.delta` -> `MessageDelta(messageId, delta)`.
- Delta field priority: `delta`, then `text`, then `content`.
- `message.completed` -> `MessageCompleted(messageId, text)`.
- Completed text field priority: `text`, then `content`, then `message`.
- If completed text is empty, frontend uses accumulated deltas.
- `error` -> `ChatError(messageId, message, code)`.
- `handoff.suggested` -> `HandoffSuggested(messageId, reason)`.
- If stream closes after start without completion/error, frontend emits `ChatError(code=connectionLost)`.
- HTTP 503 maps to `ChatError(code=offline)`.

Supported chat scenarios:

- `happyPath`
- `delayed`
- `timeout`
- `offline`
- `error`
- `connectionLost`
- `longStream`

## POST /home/personalization

Request path:

```text
/home/personalization?scenario=<scenario>&provider=<provider>
```

Request body:

```json
{
  "userId": "user-001",
  "context": {
    "hasActiveClaim": true,
    "hasPendingPayment": false,
    "lastInteraction": "claim_status",
    "locale": "es-PE"
  }
}
```

Important:

- `profile` is a frontend debug concept only.
- Do not add `profile`, `mockProfile`, or `userSegment` to request body.

Profile-to-context mapping:

| Profile | hasActiveClaim | hasPendingPayment | lastInteraction |
| --- | --- | --- | --- |
| `activeClaim` | true | false | `claim_status` |
| `pendingPayment` | false | true | `payment` |
| `newUser` | false | false | `first_open` |
| `benefitsFocused` | false | false | `benefits` |

Successful response shape:

```json
{
  "userSegment": "active_claim",
  "cards": [
    {
      "id": "claim-status",
      "type": "status",
      "title": "Tu solicitud está en revisión",
      "description": "Te avisaremos cuando haya una actualización.",
      "priority": 1,
      "action": {
        "id": "view-claim",
        "label": "Ver estado",
        "route": "/claims/status"
      }
    }
  ],
  "alerts": [],
  "nextBestActions": [
    {
      "id": "continue-claim",
      "label": "Continuar trámite",
      "route": "/claims"
    }
  ]
}
```

Mapping rules:

- Sort cards by `priority` ascending.
- Missing arrays map to empty lists.
- Missing `userSegment` maps to `unknown`.
- `generatedAt` is assigned by frontend.

Supported Home scenarios:

- `happyPath`
- `delayed`
- `timeout`
- `offline`
- `error`

Errors:

- HTTP 503 -> controlled offline error; use cache if available.
- Other non-2xx -> `http_error`.
- Invalid JSON shape -> `invalid_response`.

## POST /search

Request path:

```text
/search?scenario=<scenario>&provider=<provider>
```

Request body:

```json
{
  "query": "qué cubre mi seguro",
  "context": {
    "userId": "user-001",
    "journey": "insurance",
    "locale": "es-PE"
  }
}
```

Search profiles:

| Profile | journey |
| --- | --- |
| `insurance` | `insurance` |
| `payments` | `payments` |
| `claims` | `claims` |
| `benefits` | `benefits` |

Successful response shape:

```json
{
  "query": "qué cubre mi seguro",
  "detectedIntent": "insurance_coverage",
  "results": [
    {
      "id": "coverage-item",
      "title": "Cobertura de seguro",
      "description": "Detalle contextual de cobertura.",
      "route": "/coverage",
      "score": 0.96
    }
  ],
  "suggestedActions": [
    {
      "id": "view-coverage",
      "label": "Ver cobertura",
      "type": "assisted_navigation",
      "payload": {"route": "/coverage"}
    }
  ]
}
```

Mapping rules:

- Intent field priority: `detectedIntent`, then `intent`, then `unknown`.
- Results field priority: `results`, then `items`.
- Suggested actions field priority: `suggestedActions`, then `actions`.
- Action payload defaults to `{route}` if payload is empty and route exists.

Supported Search scenarios:

- `happyPath`
- `delayed`
- `timeout`
- `offline`
- `error`
- `noResults`

Errors:

- HTTP 503 -> `offline`.
- Other non-2xx -> `http_error`.
- Invalid JSON shape -> `invalid_response`.

## /handoff

Current Flutter POC does not call a remote `/handoff` endpoint. Handoff is implemented through `HandoffService` using fake/local behavior returning `ExperienceFallback.whatsappHandoff`.

If implemented in another stack, preserve the contract boundary and do not call it from UI directly.

## /emergency/offline-data

Current Flutter POC does not call a remote `/emergency/offline-data` endpoint.

Emergency offline data is local and deterministic through `LocalEmergencySupportService`.

Local data includes:

- Guides for `carAccident`, `medicalEmergency`, `theftAssistance`.
- Contacts:
  - `Central de emergencias`, phone `000000000`, available `24/7`.
  - `Asistencia MEP`, phone `111111111`, available `24/7`.

Any future remote endpoint must map back to the same local `EmergencyGuide` and `EmergencyContact` models.

