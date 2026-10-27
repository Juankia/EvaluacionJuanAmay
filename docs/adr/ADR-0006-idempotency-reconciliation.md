# ADR-0006 — Idempotencia generada en el dominio y conciliación

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (Fase 1) |
| Fecha | 2026-10-27 |
| Requisitos | R1 (transferencias), R3, C2 |
| Drivers y supuestos | S3; AQ-02; QA3, QA5 |
| Relacionados | [ADR-0005](ADR-0005-transaction-state-machine.md), [08 — Idempotencia y conciliación](../08-idempotency-and-reconciliation.md) |

## Contexto

Se asume que el Core **podría no** ofrecer idempotencia nativa ni consulta por clave de idempotencia (S3). Este supuesto no proviene del reto y debe validarse. Sin idempotencia, un reintento tras un timeout puede duplicar un débito. Sin un mecanismo de consulta, el resultado de una operación `UNKNOWN` no puede verificarse de forma determinista.

## Problema

¿Cómo se garantiza que cada intención del cliente produzca como máximo un efecto financiero, y cómo se determina el resultado real de las operaciones inciertas cuando el sistema externo no colabora?

## Decisión

1. La plataforma **genera una clave de idempotencia en la frontera del dominio o de la aplicación** para cada operación crítica. La clave de solicitud del canal se asocia a ella para detectar envíos duplicados.
2. **Persiste el estado de la operación antes** de invocar un sistema externo crítico.
3. **Garantiza la unicidad** de la clave dentro de la plataforma. Una repetición devuelve el estado existente.
4. **Propaga la clave** a los sistemas externos que la soporten (por ejemplo, como referencia externa).
5. **Conserva la clave** aunque el Core no la entienda.
6. **Usa la clave en la conciliación** interna.
7. Si el Core no permite consultar por clave, **concilia por correlación de negocio**: cuenta, monto, tipo de operación, referencia de negocio, referencia externa, ventana de tiempo e identificadores de cliente o de cuenta.
8. Las coincidencias ambiguas **no se resuelven automáticamente**: pasan a `REQUIRES_RECONCILIATION`.
9. En el onboarding, la idempotencia es **obligatoria** en la creación del cliente en el Core. Antes de cualquier reintento se verifica si el cliente ya existe por su identificador de persona.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Reintento automático con *backoff* | Descartada para escrituras | Duplica efectos si el Core aplicó la operación y no lo reconoce |
| Confiar solo en la clave del canal | Insuficiente | Protege contra el doble envío, pero no contra los reintentos internos ni la incertidumbre del resultado |
| Depender de la idempotencia del Core | No aplicable bajo S3 | Si se valida que existe, se adopta como mecanismo principal y la correlación de negocio pasa a respaldo |
| **Clave del dominio + persistencia previa + conciliación** | **Seleccionada** | Funciona con o sin soporte del Core |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ninguna duplicación originada por la plataforma | Almacén de operaciones con unicidad y persistencia previa |
| Resolución de la incertidumbre | Procesos de conciliación y procedimientos manuales |
| Independencia del soporte del Core | **La correlación de negocio no ofrece las mismas garantías que la idempotencia nativa** |

## Consecuencias

- La validación del supuesto S3 es **prioritaria**: su resultado cambia significativamente el riesgo residual.
- Si el Core conserva un campo de referencia libre, se inserta en él la clave de operación para mejorar la correlación.
- Operaciones debe tener procedimientos para `REQUIRES_RECONCILIATION`.
- Puede adoptarse una regla de experiencia de usuario que inhiba operaciones equivalentes mientras una está en `UNKNOWN` (decisión de negocio por acordar).

## Riesgos

- RR-04: conciliación sin idempotencia nativa. La correlación de negocio es probabilística, depende del tiempo de asentamiento del Core y no distingue de forma fiable dos operaciones legítimas idénticas en la misma ventana.
