# 25 — Contratos, eventos y errores

| Campo | Valor |
|---|---|
| Fase | 4 — C4 Nivel 3: Componentes |
| Estado | Contratos **conceptuales**: no son especificaciones OpenAPI definitivas. Pendiente de ARG-03 |
| Documentos relacionados | [24 — Componentes](24-arquitectura-de-componentes.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), [ADR-0015](adr/ADR-0015-mensajeria-asincrona-con-outbox.md), [ADR-0020](adr/ADR-0020-politica-de-auditoria-por-categoria.md) |

Los identificadores de contrato están en inglés ([00-estándares](00-estandares-de-ingenieria.md) §3).

## 1. Convenciones transversales

| Elemento | Regla |
|---|---|
| Correlación | `X-Correlation-Id` (generado en el Gateway si falta) y `traceparent` (contexto de trazas) en toda llamada y todo evento |
| Idempotencia | `Idempotency-Key` **obligatoria** en los comandos con efecto (IC-01). Formato: identificador aleatorio de alta entropía (IC-03) |
| Plazo | `X-Request-Deadline` (concepto; mecanismo en DD-06) |
| Versionado de API | En la ruta (`/v1/`); cambios incompatibles → versión nueva |
| Validación | Esquema en el borde (DTO de entrada) + reglas en el dominio |
| Autorización | Token del usuario con la audiencia del servicio; API internas solo con la identidad de carga de trabajo |
| Datos sensibles | Identificadores de cuenta enmascarados en las respuestas que no los requieren completos; ningún dato biométrico en los contratos de la plataforma |

## 2. Interfaces conceptuales

### 2.1 Servicio de consultas (canal)

| Operación | Entrada (DTO) | Salida (DTO) | Autorización |
|---|---|---|---|
| `GET /v1/customers/me` | — | `CustomerProfileResponse` | Cliente autenticado |
| `GET /v1/accounts` | — | `AccountSummaryResponse[]` (con `asOf`) | Cliente autenticado |
| `GET /v1/accounts/{accountId}/movements?cursor&from&to` | Parámetros validados (rango máximo, tamaño de página) | `MovementPageResponse` | Propiedad de `accountId` (CON-1) |

`MovementPageResponse` (conceptual):

```text
items[]: { movementId, bookingDate, valueDate, amount{value, currency}, direction, description }
nextCursor
asOf                      // marca de tiempo de origen (CON-3)
freshness: FRESH | CACHED | DEGRADED
```

### 2.2 Servicio de consultas (interna)

| Operación | Salida | Autorización |
|---|---|---|
| `GET /internal/v1/customers/{customerId}/contact-points` | `ContactPointsResponse { phoneMasked?, phone, email, verifiedAt, source }` | **Solo** la identidad de carga de trabajo del servicio de notificaciones; auditado como A4 |

### 2.3 Servicio de vinculación

| Operación | Entrada | Salida | Notas |
|---|---|---|---|
| `POST /v1/onboarding-requests` | `Idempotency-Key`; `StartOnboardingRequest` | `OnboardingStatusResponse` | Ámbito: sesión de vinculación |
| `POST /v1/onboarding-requests/{requestId}/identity-verifications` | `Idempotency-Key`; referencia de la captura del SDK | `OnboardingStatusResponse` | E1 |
| `GET /v1/onboarding-requests/{requestId}` | — | `OnboardingStatusResponse` | Estados expuestos al canal **sin revelar** la condición de cliente (VD-07) |
| `POST /ops/v1/onboarding-requests/{requestId}/resolutions` | `Idempotency-Key`; `ManualResolutionRequest` (propuesta o aprobación) | `ResolutionResponse` | Cuatro ojos; A6 |

`OnboardingStatusResponse.channelStatus` ∈ `IN_PROGRESS`, `ACTION_REQUIRED`, `UNDER_REVIEW`, `COMPLETED`, `CANNOT_CONTINUE_DIGITALLY`. El estado interno (por ejemplo, `PRE_EXISTING`) **no** se expone.

### 2.4 Servicio de notificaciones

| Operación | Entrada | Salida |
|---|---|---|
| `PUT /v1/devices/{deviceId}` | `DeviceRegistrationRequest { platform, pushToken, appVersion }` | `204` |
| `DELETE /v1/devices/{deviceId}` | — | `204` |
| `PUT /v1/notification-preferences` | `NotificationPreferencesRequest` | `NotificationPreferencesResponse` |

### 2.5 Servicio de transferencias — solo el contrato de frontera (B-05)

| Operación | Contenido | Estado |
|---|---|---|
| `POST /v1/transfers` | `Idempotency-Key`; `TransferRequest`. Respuesta: `202` con `operationId` y estado del canal | Contrato de frontera; la semántica interna está **bloqueada** |
| `GET /v1/transfers/{operationId}` | `TransferStatusResponse { operationId, channelStatus, updatedAt }` | Contrato de frontera |

`channelStatus` ∈ `RECEIVED`, `IN_PROGRESS`, `IN_VERIFICATION`, `COMPLETED`, `REJECTED`, `FAILED_NO_EFFECT`, `REVERSED`, `CLOSED_BY_EXCEPTION`. La correspondencia con los estados internos se definirá cuando se levante B-05.

### 2.6 Servicio de auditoría

| Operación | Entrada | Autorización |
|---|---|---|
| `GET /audit/v1/events?subjectId&from&to&category` | Filtros obligatorios; página acotada | Rol de auditoría (IdP corporativo); la consulta se audita (A6) |

## 3. Taxonomía de errores

Contrato uniforme: `ErrorResponse { code, messageKey, correlationId, retryable, retryAfterSeconds?, details[] }`. El mensaje para el usuario se resuelve en es-EC por `messageKey` en el canal.

| Código | HTTP | ¿Reintentable? | Significado y acción del cliente |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | No | Entrada inválida; corregir |
| `IDEMPOTENCY_KEY_REQUIRED` | 400 | No | Falta la clave (IC-01) |
| `UNAUTHENTICATED` | 401 | No | Iniciar sesión |
| `FORBIDDEN` | 403 | No | Sin permiso para la operación |
| `RESOURCE_NOT_FOUND` | 404 | No | Recurso inexistente **o ajeno** (no se distingue, para evitar la enumeración) |
| `IDEMPOTENCY_CONFLICT` | 409 | No | Misma clave con otro contenido (IC-09) |
| `EQUIVALENT_OPERATION_IN_PROGRESS` | 409 | No | Existe una operación equivalente no terminal (IC-15); consultar su estado |
| `ONBOARDING_CANNOT_CONTINUE_DIGITALLY` | 422 | No | Resultado **genérico** que agrupa varias causas (VD-07) |
| `CORE_UNAVAILABLE` | 503 | Sí (lecturas) | Core no disponible; en las lecturas puede venir una respuesta degradada en su lugar |
| `CAPACITY_EXCEEDED` | 503 | Sí, con `Retry-After` | Rechazo del control de admisión (sin efectos) |
| `IDENTITY_PROVIDER_UNAVAILABLE` | 503 | Sí | Proveedor KYC no disponible (VD-09) |
| `AUDIT_UNAVAILABLE` | 503 | Sí | No se pudo establecer evidencia durable (ADR-0020) |
| `SERVICE_IN_QUARANTINE` | 503 | Sí, más tarde | Recuperación con marca de agua en curso (ADR-0021) |
| `INTERNAL_ERROR` | 500 | Según el caso | Sin detalles internos |

**Un resultado incierto no es un error:** las transferencias en verificación responden `202` o `200` con `channelStatus = IN_VERIFICATION`.

## 4. Eventos

### 4.1 Sobre común

```text
eventId          // único; deduplicación
eventType        // p. ej. AccountMovementConfirmed, TransferStatusChanged, OnboardingStatusChanged,
                 //       SecurityEventRecorded, AuditEventRecorded
schemaVersion    // versión del esquema del tipo
source           // contenedor productor
subjectType      // OPERATION | ONBOARDING_REQUEST | CUSTOMER | SESSION
subjectId        // identificador opaco
sequence         // entero monotónico por (source, subjectId)  — ARG02-M01
occurredAt
correlationId
auditCategory    // A1 … A8 (ADR-0020)
payload          // minimizado: sin datos de contacto, sin datos biométricos
```

### 4.2 Versionado

- Los cambios **compatibles** (agregar campos opcionales) mantienen `eventType` y aumentan `schemaVersion`. Los consumidores son lectores tolerantes.
- Los cambios **incompatibles** crean un tipo nuevo o una versión mayor, publicada **en paralelo** durante una ventana de migración.
- Los esquemas se registran y se validan en el pipeline.

### 4.3 Orden, duplicados, mensajes venenosos y reprocesamiento

| Situación | Regla |
|---|---|
| Duplicados | Deduplicación por `eventId` en cada consumidor |
| Orden | Por `(source, subjectId)` mediante `sequence`. El broker **no** garantiza el orden global |
| Evento con una secuencia futura (hueco) | Notificaciones: se retiene hasta `ESPERA_SECUENCIA` o hasta que llega el faltante. Vencida la espera, se entrega con el estado vigente del sujeto. Auditoría: se anexa y se señala el hueco |
| Evento de **estado** más antiguo que el último aplicado | Se descarta para la notificación (NOT-2); la auditoría lo registra igualmente |
| Evento de **movimiento** | Nunca se descarta (NOT-1); se entrega en orden de secuencia |
| Mensaje venenoso | Tras `MAX_INTENTOS_CONSUMO`, pasa a la cola de mensajes fallidos con alerta; **nunca se descarta en silencio** |
| Reprocesamiento | Redistribución desde la cola de mensajes fallidos iniciada por operaciones (acción A6); consumidores idempotentes; retención del broker ≥ `RETENCION_BROKER` |

## 5. Matriz de errores y timeouts

| Interacción | Timeout | Reintento | Circuit Breaker | Ante timeout o falla |
|---|---|---|---|---|
| BFF → consultas | Por ruta, dentro del plazo | 1 (lectura) | Sí | Respuesta degradada o 503 |
| BFF → transferencias (inicio) | Dentro del plazo | **Ninguno** | Sí | El canal consulta el estado por `operationId` o reenvía con **la misma** clave |
| BFF → transferencias (estado) | Corto | 1 | Sí | Composición degradada (ARG02-M06) |
| BFF → vinculación | Dentro del plazo | Ninguno en los pasos con efecto | Sí | El canal reenvía con la misma clave |
| BFF → almacén de sesiones | Corto | 1 | Sí | Web: falla cerrada (volver a iniciar sesión) |
| Consultas → Core | Conexión y lectura por operación | Acotado, con espera exponencial y aleatoriedad | Sí, por ACL | Degradación aprobada o `CORE_UNAVAILABLE` |
| Consultas → caché | Muy corto | No | Sí | Consulta directa al Core con cupo |
| Consultas → auditoría (doble ruta) | Corto por ruta | Cambia a la otra ruta | — | Ambas caídas → `AUDIT_UNAVAILABLE` |
| Vinculación → KYC | Por operación | Acotado sin repetir capturas | Sí | `IDENTITY_VERIFICATION_UNAVAILABLE` |
| Vinculación → Core (búsqueda) | Por operación | Acotado (lectura) | Sí | Se mantiene el estado y se reintenta después |
| Vinculación → IdP (vinculación) | Corto | **Ninguno** automático; la operación del IdP es idempotente por nonce y solicitud | Sí | Reintento con la **misma** afirmación hasta su vencimiento |
| Notificaciones → consultas (contactos) | Corto | Acotado | Sí | Reintento diferido; caché `TTL_CONTACTO` |
| Notificaciones → proveedores | Por proveedor | Acotado, con espera | Sí, por proveedor | Canal alternativo; cola de mensajes fallidos |
| Relés → broker | Corto | Ilimitado con espera (el outbox persiste) | — | Alerta por antigüedad del outbox |
| Auditoría → almacén | Corto | Reintento del consumo | — | El broker y los outbox retienen |
