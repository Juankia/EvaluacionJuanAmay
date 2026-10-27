# ADR-0015 — Mensajería asíncrona con Outbox transaccional y broker gestionado

| Campo | Valor |
|---|---|
| Estado | **Aceptada** en cuanto al patrón y a las capacidades requeridas. **Producto diferido** a la selección del proveedor cloud (DD-15). Activa el disparador de DD-01 |
| Fecha | 2026-10-27 |
| Requisitos | R4, R5; C2, C5 |
| Drivers y supuestos | AQ-03, AQ-04, AQ-05; QA6 |
| Relacionados | [ADR-0008](ADR-0008-invalidacion-por-eventos.md), [ADR-0016](ADR-0016-persistencia-y-durabilidad-del-estado.md), [19 — Comunicación entre contenedores](../19-comunicacion-entre-contenedores.md) |

## Contexto

ADR-0008 difirió la tecnología del bus de eventos hasta que se confirmaran los flujos asíncronos. En la Fase 3 se confirman tres:

- la **notificación de cada movimiento** (R4);
- la **auditoría de todas las acciones** (R5);
- la **invalidación de la caché** tras las operaciones propias (ADR-0004, M03).

Estos flujos no deben bloquear ni revertir la operación financiera, y no pueden perderse.

## Problema

¿Cómo se publican los eventos de forma consistente con el cambio de estado, sin una transacción distribuida entre la base de datos y el broker?

## Decisión

1. **Outbox transaccional** (*Transactional Outbox*): cada servicio que produce eventos los escribe en una tabla de outbox de **su propio almacén, en la misma transacción local** que el cambio de estado. Un relé publica el outbox en el broker.
2. **Semántica:**
   - entrega **al menos una vez**;
   - los consumidores son **idempotentes** por identificador de evento;
   - orden por clave (identificador de operación o de cliente) **solo donde se requiera**;
   - colas de mensajes fallidos y redistribución controlada.
3. **Capacidades exigidas al broker:** tópicos con varias suscripciones (notificaciones, auditoría e invalidación consumen los mismos eventos); durabilidad; colas de mensajes fallidos; retención suficiente para la recuperación; servicio gestionado en el proveedor cloud seleccionado; cifrado y control de acceso por tópico.
4. **Producto:** se selecciona con el proveedor cloud (DD-15) entre servicios gestionados de colas y tópicos. **No se selecciona Kafka**: no hay requisitos de reprocesamiento masivo, de procesamiento de flujos ni de volumen que lo justifiquen. Se reevaluará si se confirman la analítica en tiempo real o la proyección de lectura (ADR-0007).
5. **Ningún evento es la fuente autoritativa** de una operación financiera; la fuente es el almacén del servicio dueño (ADR-0016) y, en última instancia, el Core.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Publicar en el broker dentro de la lógica de negocio (escritura dual) | Descartada | Inconsistencia si falla una de las dos escrituras |
| Llamadas síncronas a notificaciones y auditoría | Descartada | Acopla la disponibilidad de la operación a la de servicios secundarios |
| Captura de cambios de datos (CDC) de la base de datos | No seleccionada como base | Acopla los consumidores al esquema interno; puede reevaluarse como mecanismo del relé |
| Kafka | No seleccionada | Sin un driver que la justifique (sección 4) |
| **Outbox + broker gestionado de colas y tópicos** | **Seleccionada** | Consistencia local sin transacción distribuida |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Eventos consistentes con el estado, sin 2PC | Latencia de entrega (asíncrona) y relé por servicio |
| Desacoplamiento de notificaciones y auditoría | Consumidores idempotentes obligatorios; posibles duplicados |

## Consecuencias

- DD-01 se resuelve en cuanto al patrón; el producto queda en DD-15.
- La auditoría de una acción se garantiza **en el origen** (outbox) desde el momento en que la acción se confirma, y **en el almacén de auditoría** con un retraso acotado y monitoreado ([18](../18-propiedad-y-consistencia-de-datos.md)).

## Riesgos

- Acumulación del outbox si el broker no está disponible (monitoreo de antigüedad).
- Duplicados de notificación (mitigados por la deduplicación del servicio de notificaciones).
