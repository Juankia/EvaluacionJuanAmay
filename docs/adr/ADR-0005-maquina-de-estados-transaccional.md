# ADR-0005 — Máquina de estados transaccional durable para operaciones financieras

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (modelo conceptual, Fase 1). La implementación se difiere (DD-08) |
| Fecha | 2026-10-27 |
| Requisitos | R1 (transferencias), R3, R4, C2 |
| Drivers y supuestos | S3, S4, S13; AQ-02, AQ-05; QA3, QA5 |
| Relacionados | [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [ADR-0009](ADR-0009-core-autoritativo.md), [07 — Máquina de estados](../07-maquina-de-estados-transaccional.md) |

## Contexto

Las transferencias propias e interbancarias implican invocaciones a sistemas externos: el Core y, para las interbancarias, el sistema de pagos (S13). Se asume que el Core no ofrece idempotencia nativa (S3). Un timeout no prueba que la operación haya fallado ([05, sección 6](../05-frontera-de-integracion-core.md#6-regla-crítica-un-timeout-no-equivale-a-una-falla)). El onboarding también tiene pasos externos y un paso crítico: la creación del cliente en el Core.

## Problema

¿Cómo se controla el ciclo de vida de una operación financiera de modo que:

- nunca se pierda;
- nunca se duplique;
- su resultado incierto se haga explícito y se resuelva;
- el sistema se recupere tras reinicios o fallas parciales?

## Decisión

1. Las operaciones financieras críticas y el onboarding se modelan como una **máquina de estados transaccional durable** (*Durable Transaction State Machine*). Para las transferencias se le llama **orquestador de transacciones de transferencia** (*Transfer Transaction Orchestrator*).
2. Estados conceptuales de una transferencia:
   - **flujo principal:** `CREATED` → `VALIDATING` → `CORE_PROCESSING` → `SPI_PROCESSING` (solo interbancarias) → `COMPLETED`;
   - **estados de falla o excepción:** `FAILED`, `UNKNOWN`, `REQUIRES_RECONCILIATION`.
3. El estado se **persiste antes** de cada invocación a un sistema externo crítico.
4. **No se reintenta a ciegas** desde los estados con posible efecto financiero. El resultado incierto pasa a `UNKNOWN` y se resuelve por conciliación ([ADR-0006](ADR-0006-idempotencia-y-conciliacion.md)).
5. Si el Core no está disponible **antes** de la invocación, la operación **falla de forma segura**. **No se encola** una transacción financiera para ejecutarla después solo porque el Core no está disponible.
6. Las compensaciones (por ejemplo, un reverso contable) se modelan como **estados explícitos, auditados y autorizados**, nunca como efectos implícitos.
7. Toda transición se audita.

### Por qué no «Saga» como modelo genérico

- Las operaciones financieras **no siempre se compensan** de forma segura con una transacción inversa simple. Un reverso es una nueva transacción con efectos visibles, contables y de reporte, y puede fallar.
- Algunos pasos (como una transferencia aceptada por el sistema de pagos) **no son reversibles unilateralmente**.
- El caso dominante es la **incertidumbre** (`UNKNOWN`), y Saga no la modela de forma natural.
- El término induce a suponer que existe una compensación automática segura.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Llamada síncrona sin estado durable | Descartada | Pierde el rastro de la operación ante reinicios. No distingue entre falla e incertidumbre. Invita a reintentos que duplican |
| Saga con compensaciones automáticas | Descartada como modelo genérico | Ver la sección anterior |
| Transacción distribuida (2PC/XA) con el Core | Descartada | Se asume que el Core no participa en protocolos de transacción distribuida (S3). Bloquea recursos y acopla la disponibilidad |
| Encolar la operación y ejecutarla cuando el Core vuelva | Descartada | Ejecuta la operación en condiciones distintas a las aceptadas por el cliente y sin su expectativa. Riesgo de integridad y de experiencia |
| **Máquina de estados durable con `UNKNOWN` y conciliación** | **Seleccionada** | Hace explícita la incertidumbre y permite la recuperación |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ninguna operación perdida ni duplicada por reintento | Persistencia adicional y procesos de avance y de conciliación |
| Recuperación tras reinicios | Complejidad de implementación y de pruebas |
| Estados intermedios comunicables al cliente («en verificación») | Procedimientos operativos para `REQUIRES_RECONCILIATION` |
| Auditoría completa del ciclo de vida | Latencia adicional por la persistencia previa a cada invocación |

## Consecuencias

- El servicio de transferencias es el dueño de la orquestación de negocio, no el Gateway ([ADR-0002](ADR-0002-frontera-api-gateway.md)).
- La notificación definitiva al cliente solo se emite en estados terminales confirmados (R4).
- Se requiere monitoreo de la antigüedad por estado y alertas para `UNKNOWN` y `REQUIRES_RECONCILIATION`.
- La secuencia exacta de la transferencia interbancaria se define cuando se valide S13.

## Riesgos

- RR-08: complejidad del estado transaccional durable.
- RR-04: conciliación sin idempotencia nativa.
