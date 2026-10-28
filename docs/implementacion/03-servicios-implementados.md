# 03 — Servicios implementados

| Campo | Valor |
|---|---|
| Documento | Puertos, API, eventos y configuración de los servicios de la base |
| Diseño de referencia | [24 — Arquitectura de componentes](../24-arquitectura-de-componentes.md); [25 — Contratos, eventos y errores](../25-contratos-eventos-y-errores.md) |
| Fecha | 2026-10-27 |

---

## 1. Convenciones comunes

| Aspecto | Implementación |
|---|---|
| Capas | `domain` (sin dependencias técnicas), `application` (casos de uso y puertos), `adapters/in` y `adapters/out` |
| Errores | `ApiException` y los *mappers* de `shared-observability` producen el `ErrorResponse` uniforme; las rutas desconocidas responden `RESOURCE_NOT_FOUND` |
| Correlación | `X-Correlation-Id` saneado o generado; se propaga al MDC y a los eventos |
| Eventos | Sobre común `EventEnvelope`; `sequence` monótona por `(source, subjectId)`; intercambio `domain-events` con la clave de enrutamiento igual al tipo de evento |
| Consumo | Deduplicación por `eventId`; mensajes inválidos o no procesables a una cola de mensajes muertos con un registro `ALERT` |

## 2. Servicio de vinculación digital

| Elemento | Detalle |
|---|---|
| API | `POST /v1/onboarding-requests`; `POST /v1/onboarding-requests/{requestId}/identity-verifications`; `GET /v1/onboarding-requests/{requestId}` ([contrato](../../packages/api-contracts/openapi/digital-linking-v1.yaml)) |
| Idempotencia | `Idempotency-Key` obligatoria en los `POST`. Misma clave y mismo contenido → reproducción (`200`); misma clave y contenido distinto → `IDEMPOTENCY_CONFLICT` |
| Unicidad | Una sola solicitud activa por identificación (índice parcial, IC-15) → `EQUIVALENT_OPERATION_IN_PROGRESS` |
| Protección de datos | La identificación y las huellas de solicitud se guardan como HMAC-SHA256 con una clave de entorno; nunca en claro. La respuesta expone solo el estado de canal (VD-07) |
| Transacciones | Cada transición, su registro de idempotencia y su evento A3 se confirman en una única transacción local (outbox) |
| Concurrencia | CAS sobre `version`, que también es la secuencia del evento |
| KYC | `MockIdentityVerificationProvider` (simulado). Timeout de 2 s; sin reintentos (no se reenvía una captura). Indisponibilidad o timeout → `IDENTITY_VERIFICATION_UNAVAILABLE` y `503 IDENTITY_PROVIDER_UNAVAILABLE`; **nunca** un rechazo (VD-09) |
| Relevo del outbox | `OutboxRelay` programado: reclama filas con `FOR UPDATE SKIP LOCKED`, publica con confirmación del broker y marca `published_at` solo después de la confirmación. Entrega al menos una vez |
| Puertos bloqueados | E3 y E5 con adaptadores deshabilitados ([01](01-alcance-de-la-base-ejecutable.md) §2) |

Comportamiento del KYC simulado según el prefijo de `captureReference`:

| Prefijo | Resultado |
|---|---|
| `reject-` | `IDENTITY_REJECTED` → `CANNOT_CONTINUE_DIGITALLY` |
| `unavailable-` | Proveedor no disponible → `ACTION_REQUIRED` |
| `slow-` | Respuesta más lenta que el timeout → `ACTION_REQUIRED` |
| Cualquier otro | `IDENTITY_VERIFIED` → `IN_PROGRESS` (la etapa siguiente no está implementada) |

Tras una indisponibilidad, el solicitante reintenta con una **nueva** clave de idempotencia; la misma clave reproduce el resultado registrado.

## 3. Servicio de notificaciones

Consume `OnboardingStatusChanged`, descarta los eventos de estado obsoletos (NOT-2), reintenta en forma acotada por canal y pasa de push a SMS. Si ningún canal entrega, el mensaje va a la cola de mensajes muertos. Los proveedores y el resolvedor de destinatarios son **falsos o sintéticos**.

## 4. Servicio de auditoría

Consume todos los eventos de dominio, los anexa de forma idempotente a una cadena de hash por fuente y marca los saltos de secuencia sin descartarlos. La API de consulta exige el rol `auditor` y registra cada consulta como A6 antes de responder. La cadena **detecta** modificaciones accidentales; no es almacenamiento a prueba de manipulación ([05](05-limitaciones-y-pendientes.md)).

## 5. Simulador del Core

Fixture de pruebas de solo lectura con datos sintéticos. La cabecera `X-Sim-Fault` (`unavailable`, `slow`) inyecta fallas para ejercitar las ACL. **No es el Core** ni modela su protocolo de escritura.
