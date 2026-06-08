# Common Models

Use these models as cross-stack contract references. Types are expressed in neutral terms.

## Chat

### ChatMessage

Fields:

- `id: String` - unique local or remote message id.
- `text: String` - rendered message text.
- `isUser: Boolean` - true for user message.
- `createdAt: DateTime/String ISO` - creation timestamp.
- `isStreaming: Boolean` - true while assistant message is receiving deltas.

Example:

```json
{
  "id": "local-ai-1",
  "text": "Respuesta parcial",
  "isUser": false,
  "createdAt": "2026-06-06T10:00:00.000Z",
  "isStreaming": true
}
```

### ChatRequest

Fields:

- `conversationId: String`
- `message: String`
- `context: ConversationContext`

### ConversationContext

Fields:

- `userId: String`
- `channel: String`
- `journey: String`
- `locale: String`, default `es-PE`.

### ChatEvent

Variants:

- `MessageStarted { messageId: String }`
- `MessageDelta { messageId: String, delta: String }`
- `MessageCompleted { messageId: String, text: String }`
- `ChatError { messageId: String, message: String, code?: String }`
- `HandoffSuggested { messageId: String, reason: String }`

## Home

### HomeExperience

Fields:

- `userSegment: String`
- `cards: List<HomeCard>`
- `alerts: List<HomeAlert>`
- `nextBestActions: List<ActionSuggestion>`
- `isFromCache: Boolean`
- `generatedAt?: DateTime/String ISO`

Example:

```json
{
  "userSegment": "active_claim",
  "cards": [],
  "alerts": [],
  "nextBestActions": [],
  "isFromCache": false
}
```

### HomeCard

Fields:

- `id: String`
- `type: String`
- `title: String`
- `description: String`
- `priority: Int`
- `action?: ActionSuggestion`

### HomeAlert

Fields:

- `id: String`
- `title: String`
- `message: String`
- `severity: String`, default `info`.

### ActionSuggestion

Fields:

- `id: String`
- `label: String`
- `route: String`
- `type?: String`
- `payload: Map<String, Any?>`

## Search

### SearchResult

Fields:

- `query: String`
- `detectedIntent: String`
- `results: List<SearchItem>`
- `suggestedActions: List<SearchSuggestedAction>`
- `isFromCache: Boolean`

Example:

```json
{
  "query": "qué cubre mi seguro",
  "detectedIntent": "insurance_coverage",
  "results": [],
  "suggestedActions": [],
  "isFromCache": false
}
```

### SearchItem

Fields:

- `id: String`
- `title: String`
- `description: String`
- `route: String`
- `score: Double`

### SearchSuggestion

Fields:

- `id: String`
- `label: String`

Currently suggestions are modeled but remote suggestions return an empty list.

### SearchSuggestedAction

Fields:

- `id: String`
- `label: String`
- `type: String`
- `payload: Map<String, Any?>`

## Emergency

### EmergencyType

Values:

- `carAccident` - display `Accidente vehicular`.
- `medicalEmergency` - display `Emergencia médica`.
- `theftAssistance` - display `Robo / asistencia`.

### EmergencyGuide

Fields:

- `id: String`
- `type: EmergencyType`
- `title: String`
- `steps: List<String>`
- `contacts: List<EmergencyContact>`

### EmergencyContact

Fields:

- `id: String`
- `label: String`
- `phone: String`
- `available: String`

### EmergencyConversationStep

Fields:

- `id: String`
- `question: String`
- `options: List<String>`
- `order: Int`

Current deterministic steps:

```json
[
  {
    "id": "safe_zone",
    "question": "¿Te encuentras en una zona segura?",
    "options": ["Sí", "No"],
    "order": 1
  },
  {
    "id": "injured_people",
    "question": "¿Hay personas heridas?",
    "options": ["Sí", "No", "No estoy seguro"],
    "order": 2
  },
  {
    "id": "register_report",
    "question": "¿Deseas registrar un reporte para sincronizarlo más tarde?",
    "options": ["Sí", "No"],
    "order": 3
  }
]
```

### EmergencyAnswer

Fields:

- `questionId: String`
- `question: String`
- `answer: String`

### EmergencyReport

Fields:

- `id: String`
- `type: EmergencyType`
- `answers: List<EmergencyAnswer>`
- `createdAt: DateTime/String ISO`
- `status: EmergencySyncStatus`

### EmergencySyncStatus

Values:

- `pendingSync`
- `synced`

## Resilience

### ExperienceFallback

Fields:

- `type: ExperienceFallbackType`
- `reason: String`
- `actionLabel?: String`
- `metadata: Map<String, Any?>`

Types:

- `retry`
- `offlineGuide`
- `whatsappHandoff`
- `degradedExperience`
- `connectionLost`

### ExperienceContext

Fields:

- `sessionId: String`
- `userId?: String`
- `conversationId?: String`
- `locale: String`
- `metadata: Map<String, Any?>`

### ExperienceEvent

Fields:

- `name: String`
- `occurredAt: DateTime/String ISO`
- `payload: Map<String, Any?>`

### ExperienceState

Fields:

- `status: idle | loading | ready | degraded | fallback | error`
- `message?: String`
- `metadata: Map<String, Any?>`

This generic state exists in core but feature states currently use richer feature-specific models.

