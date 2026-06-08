# MEP 3.0 – Frontend IA-First Architecture Validation POC

## Objetivo

Este proyecto tiene como objetivo validar una arquitectura frontend IA-First desacoplada mediante la implementación de Proof of Concepts (POCs) equivalentes en distintos stacks tecnológicos.

La implementación inicial se realiza en Flutter y posteriormente servirá como referencia para React Native y Kotlin Multiplatform.

La POC busca generar evidencia empírica para evaluar:

- Integración IA
- Desacoplamiento de proveedor
- Performance y experiencia de usuario
- Inferencia híbrida
- Escalabilidad arquitectónica

---

# Alcance

La aplicación implementa cuatro capacidades principales:

1. Chat conversacional con streaming
2. Home hiperpersonalizada
3. Búsqueda inteligente
4. Continuidad offline / emergencia

Estas capacidades permiten validar los principales casos de uso definidos en el IA Discovery.

---

# Principios arquitectónicos

## IA como capacidad transversal

La inteligencia artificial no se implementa como una feature aislada.

Todas las capacidades inteligentes deben exponerse mediante contratos desacoplados consumidos por la experiencia.

---

## Desacoplamiento de proveedor

La UI nunca debe conocer:

- Azure
- OpenAI
- Kore
- Genesys
- Vertex AI
- cualquier proveedor específico

Todo acceso debe realizarse mediante contratos abstractos.

---

## UI agnóstica

La UI consume únicamente estados.

No contiene lógica de negocio.

No contiene lógica de integración.

No contiene conocimiento del proveedor.

---

## Evolución progresiva

La arquitectura debe permitir evolucionar desde:

- sugerencias
- asistencia
- automatización

sin modificar la capa de presentación.

---

## Provider swap

El cambio de proveedor debe afectar únicamente implementaciones de la capa data.

No debe requerir modificaciones en:

- UI
- Cubits
- Casos de uso
- Orquestación

---

# Arquitectura lógica

## Capas

### 1. Channels / Experience

Responsable de la interacción con el usuario.

Incluye:

- Home
- Chat
- Search

Responsabilidades:

- Render UI
- Captura de eventos
- Render de estados
- Navegación

No contiene lógica IA.

---

### 2. Experience Orchestration

Responsable de coordinar la experiencia.

Responsabilidades:

- Gestión de journey
- Triggers
- Contexto de experiencia
- Coordinación entre features
- Aplicación de reglas de resiliencia

No conoce proveedores.

---

### 3. Frontend AI Capability Layer

Define las capacidades consumibles por la experiencia.

Se expresa mediante contratos.

Contratos:

- ChatService
- SearchService
- PersonalizationService
- RecommendationService
- HandoffService
- EmergencySupportService

Esta capa constituye el boundary principal de desacoplamiento.

---

### 4. Local Experience Capabilities

Capacidades disponibles sin conectividad.

Ejemplos:

- Emergency guides
- Cache local
- Reglas offline
- Información precargada

Permiten continuidad operacional.

---

### 5. Remote Integration Layer

Implementaciones concretas de contratos.

Responsabilidades:

- Consumo de APIs
- Adaptación de modelos
- Mapping de respuestas
- Manejo de errores

Puede cambiar sin afectar la experiencia.

---

# Arquitectura física

```text
lib/
│
├── app/
│
├── core/
│   ├── config/
│   ├── network/
│   ├── observability/
│   ├── resilience/
│   ├── storage/
│   └── models/
│
├── experience/
│   ├── orchestration/
│   └── contracts/
│
├── shared/
│   └── widgets/
│
└── features/
    ├── home/
    ├── chat/
    ├── search/
    └── emergency/
```
# Experience Orchestration

## Objetivo

Centralizar decisiones de experiencia independientes de una feature específica.

Ejemplos:

- timeout conversacional
- fallback experiencial
- degradación controlada
- experiencia offline
- selección local/cloud

---

## Componentes

### ExperienceOrchestrator

Coordina la experiencia.

Responsabilidades:

- Coordinar interacciones entre features
- Aplicar reglas de resiliencia
- Gestionar contexto de experiencia
- Gestionar journeys
- Delegar a contratos de capacidades

---

### ExperienceTrigger

Representa eventos disparadores de experiencia.

Ejemplos:

- userInactive
- connectivityLost
- timeoutExceeded
- personalizationRefreshRequested
- searchIntentDetected

---

### JourneyState

Representa el estado actual del journey del usuario.

Ejemplos:

- Home
- Search
- Chat
- Emergency
- Handoff

---

# Resilience Layer

## Objetivo

Garantizar continuidad de experiencia ante fallas, latencias elevadas o pérdida de conectividad.

---

## ExperienceResiliencePolicy

Define reglas de resiliencia de experiencia.

Responsabilidades:

- Activar fallback
- Seleccionar experiencia local
- Gestionar degradación controlada
- Determinar reintentos

Ejemplos:

- Mostrar experiencia offline
- Derivar a WhatsApp
- Reintentar solicitud
- Mostrar contenido cacheado

---

## SlaTimer

Mide cumplimiento del SLA de experiencia.

Valor inicial de referencia:

```text
3 segundos
```

Responsabilidades:

- Medir tiempo a primer token
- Medir tiempo de respuesta visible
- Activar políticas de resiliencia cuando corresponda

---

## ExperienceFallback

Representa las acciones de fallback posibles.

Opciones iniciales:

- Retry
- OfflineGuide
- WhatsappHandoff
- DegradedExperience

---

## NetworkMonitor

Responsable de detectar cambios de conectividad.

Responsabilidades:

- Detectar online/offline
- Notificar cambios de estado
- Permitir routing local/cloud

---

# Contratos Comunes

## Objetivo

Definir capacidades consumibles por la experiencia sin exponer detalles de implementación ni proveedores específicos.

Los contratos constituyen el principal boundary de desacoplamiento de la arquitectura.

---

## ChatService

Capacidad conversacional.

Responsabilidades:

- Enviar mensajes
- Recibir respuestas en streaming
- Gestionar conversaciones
- Cancelar solicitudes activas

---

## SearchService

Capacidad de búsqueda inteligente.

Responsabilidades:

- Buscar contenido
- Detectar intención
- Obtener sugerencias
- Proponer acciones contextuales

---

## PersonalizationService

Capacidad de personalización.

Responsabilidades:

- Obtener experiencia personalizada
- Actualizar contenido dinámicamente
- Adaptar experiencia al contexto del usuario

---

## RecommendationService

Capacidad de recomendación.

Responsabilidades:

- Generar acciones sugeridas
- Priorizar contenido
- Determinar next best actions

---

## HandoffService

Capacidad de derivación humana.

Responsabilidades:

- Transferencia a WhatsApp
- Escalamiento a atención humana
- Gestión de fallback conversacional

---

## EmergencySupportService

Capacidad de continuidad offline.

Responsabilidades:

- Proveer guías de emergencia
- Gestionar contactos críticos
- Sincronizar acciones pendientes

---

# Fake AI Gateway

## Propósito

Simular el comportamiento de proveedores reales durante la ejecución de las POCs.

Permite validar:

- Streaming
- Timeouts
- Latencia
- Fallbacks
- Desacoplamiento

sin depender de proveedores reales.

El Fake AI Gateway no representa la arquitectura final de backend.

---

## Escenarios soportados

### success

Respuesta normal.

### slow

Respuesta lenta.

### timeout

Respuesta fuera del SLA esperado.

### error

Error técnico controlado.

### offline

Sin conectividad.

### providerA

Proveedor A simulado.

### providerB

Proveedor B simulado.

---

# Observabilidad

## PerformanceTracker

Responsable de capturar métricas de performance.

Métricas iniciales:

- requestStart
- firstTokenReceived
- responseCompleted
- totalResponseTime

Estas métricas serán utilizadas posteriormente para el scoring empírico.

---

## AiEventLogger

Responsable de registrar eventos relevantes de la experiencia.

Ejemplos:

- Inicio de request
- Inicio de streaming
- Timeout
- Fallback activado
- Error
- Cambio de proveedor
- Uso de experiencia offline

---

# POCs Implementadas

## POC 1 — Chat Streaming

Valida:

- Streaming de respuestas
- SLA conversacional
- Performance
- Experiencia de usuario

Dimensiones impactadas:

- Integración IA
- Performance & UX

---

## POC 2 — Provider Decoupling

Valida:

- Independencia de proveedor
- Estabilidad de contratos
- Impacto arquitectónico de un provider swap

Dimensiones impactadas:

- Desacoplamiento
- Escalabilidad

---

## POC 3 — Continuidad Offline

Valida:

- Experiencia local
- Routing local/cloud
- Resiliencia operacional
- Fallback transparente

Dimensiones impactadas:

- Inferencia híbrida
- Performance
- Escalabilidad

---

## POC 4 — Home Hiperpersonalizada

Valida:

- Personalización dinámica
- Actualización runtime
- Re-rendering eficiente
- Recomendaciones contextuales

Dimensiones impactadas:

- Performance & UX
- Integración IA
- Escalabilidad

---

# Métricas de Evaluación

## Performance

- FPS
- CPU
- Memoria
- Cold Start

---

## Conversacional

- Time To First Token
- Tiempo a respuesta visible
- Tiempo a respuesta completa

---

## Resiliencia

- Tiempo de fallback
- Continuidad offline
- Recuperación ante errores

---

## Arquitectura

- Capas modificadas por provider swap
- Contratos afectados
- Complejidad del cambio
- Impacto en UI
- Impacto en dominio

---

# Regla de Estabilidad de Contratos

Los contratos definidos en esta arquitectura se consideran congelados para la ejecución de las POCs.

Cualquier modificación debe:

1. Documentarse.
2. Versionarse.
3. Aplicarse en todos los stacks evaluados.
4. Registrarse como evidencia arquitectónica.

Esto garantiza comparabilidad entre Flutter, React Native y Kotlin Multiplatform.

---

# Objetivo Final

Obtener evidencia empírica para determinar qué stack tecnológico soporta mejor una arquitectura frontend IA-First orientada a:

- Experiencias conversacionales
- Hiperpersonalización
- Búsqueda inteligente
- Continuidad offline
- Evolución futura hacia arquitecturas multiagente

manteniendo:

- Desacoplamiento de proveedor
- Escalabilidad
- Observabilidad
- Sostenibilidad arquitectónica
- Consistencia de experiencia