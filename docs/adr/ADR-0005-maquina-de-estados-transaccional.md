# ADR-0005 — Máquina de estados transaccional durable para operaciones financieras

| Campo | Valor |
|---|---|
| Estado | **Aceptada — enmendada** en la remediación ARG-01 (resuelve ARG01-H01, ARG01-H02, ARG01-H03 y ARG01-M02). Modelo conceptual; la implementación se difiere (DD-08). Pendiente de verificación en ARG-01-R2 |
| Fecha | 2026-10-27 (versión 1); 2026-10-27 (versión 2, enmienda) |
| Requisitos | R1 (transferencias), R3, R4, C2 |
| Drivers y supuestos | S3, S4, S13, S18; AQ-02, AQ-04, AQ-05; QA3, QA5, QA9 |
| Relacionados | [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [ADR-0009](ADR-0009-core-autoritativo.md), [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md), [07 — Máquina de estados](../07-maquina-de-estados-transaccional.md), [ARG-01](../reviews/ARG-01-revision-arquitectonica.md) |

## Historial

| Versión | Cambio | Origen |
|---|---|---|
| 1 | Máquina de estados durable con `UNKNOWN` y conciliación | Fase 1 |
| 2 | Taxonomía cerrada de resultados de invocación. Modelo por dimensiones con estados financieros inequívocos e invariantes (el estado `FAILED` genérico se elimina). Reglas de recuperación y de concurrencia. Modelo operativo de `REQUIRES_RECONCILIATION`. El estado `SPI_PROCESSING` se renombra a `PENDING_EXTERNAL_CONFIRMATION`, agnóstico de la topología | ARG01-H01, H02, H03, M02 |

## Contexto

Las transferencias propias e interbancarias invocan sistemas externos: el Core y, en las interbancarias, una capacidad externa de procesamiento cuya topología está por validar (S13, Q-01). Se asume que el Core no ofrece idempotencia nativa (S3).

La revisión ARG-01 encontró cuatro problemas en la versión 1:

- la clasificación de una «falla de conexión» como falla sin efecto era demasiado amplia (H01);
- `FAILED` podía alcanzarse con un débito aplicado y sin reverso (H02);
- `REQUIRES_RECONCILIATION` no tenía responsable ni plazo (H03);
- no había reglas de recuperación ni de concurrencia (M02).

## Problema

¿Cómo se controla el ciclo de vida de una operación financiera de modo que:

- nunca se pierda ni se duplique por acción de la plataforma;
- su resultado incierto sea explícito y se resuelva;
- **ningún estado terminal oculte una obligación financiera pendiente**;
- el sistema se recupere tras reinicios y ante concurrencia?

## Decisión

### 1. Máquina de estados durable

Las operaciones financieras críticas y el onboarding se modelan como una **máquina de estados transaccional durable** (*Durable Transaction State Machine*). Para las transferencias se le llama **orquestador de transacciones de transferencia** (*Transfer Transaction Orchestrator*).

El estado se **persiste antes** de cada invocación con posible efecto financiero. **No se encola** una transacción financiera para ejecutarla después solo porque el Core no está disponible.

### 2. Taxonomía cerrada del resultado de una invocación externa (H01)

Toda respuesta o falla de una invocación con posible efecto se clasifica en **exactamente una** clase. La ACL de cada dominio mantiene la tabla de clasificación de su contrato ([ADR-0001](ADR-0001-acl-por-dominio.md)).

| Clase | Definición | Ejemplos | Efecto en la máquina | ¿Reintento automático de la ejecución? |
|---|---|---|---|---|
| **NOT_SENT** — Falla confirmada antes del envío | Hay **evidencia local** de que ningún byte de la solicitud se escribió en una conexión de red hacia el destino **ni hacia ningún intermediario** | Circuito abierto local; rechazo del control de admisión ([ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md)); falla al establecer la conexión o el *handshake* TLS **antes de escribir**; plazo insuficiente antes de invocar; validación local fallida | Sin efecto. Se permite un nuevo intento **solo** dentro del plazo de negocio y según la política de la ACL. Si se agota, el estado es terminal sin efecto | Sí, solo en esta clase |
| **REJECTED_CONFIRMED** — Rechazo definitivo confirmado | El sistema externo responde explícitamente con un resultado que **su contrato define como «sin efecto»** | Fondos insuficientes; cuenta bloqueada; validación del Core | Terminal sin efecto | No (no aplica) |
| **UNKNOWN** — Resultado desconocido | **Cualquier** falla posterior a un posible envío, o cualquier respuesta no clasificada | Timeout de lectura; conexión reiniciada durante o después de la escritura (incluidas las conexiones reutilizadas del pool); error de un intermediario (ESB, proxy, balanceador); código no mapeado; plazo vencido con la solicitud en curso; reinicio del proceso con un tramo en curso | Estado `*_UNKNOWN` del tramo y luego conciliación | **No.** Solo se permiten **consultas** de estado, que son lecturas |
| **CONFIRMED** — Confirmación definitiva | Respuesta de éxito que el contrato define como definitiva, o evidencia de conciliación | Confirmación del débito; confirmación de la liquidación externa según su contrato | Avance al siguiente tramo o estado terminal | No aplica |

Reglas:

- **En caso de duda, la clase es `UNKNOWN`.** Un timeout, una desconexión o una respuesta perdida **nunca** se interpretan como ausencia de efecto.
- Un código del sistema externo **no mapeado** en la tabla de la ACL es `UNKNOWN`.
- El éxito de la conexión con un intermediario **no prueba nada** sobre el destino final.

### 3. Modelo de estados por dimensiones (H02)

Una transferencia tiene **dimensiones** con estado propio. El **estado global** se deriva de ellas y nunca las oculta.

| Dimensión | Estados | Aplica a |
|---|---|---|
| **Solicitud** | `RECEIVED` → `VALIDATING` → aceptada \| `REJECTED` | Todas |
| **Ejecución en el Core** | `NOT_STARTED` → `IN_PROGRESS` → `CONFIRMED` \| `REJECTED` \| `UNKNOWN` | Todas |
| **Procesamiento externo** | `NOT_APPLICABLE` \| `NOT_STARTED` → `IN_PROGRESS` → `CONFIRMED` \| `REJECTED` \| `UNKNOWN` | Interbancarias |
| **Reverso** | `NOT_REQUIRED` \| `REQUIRED` → `IN_PROGRESS` → `CONFIRMED` \| `NOT_POSSIBLE` \| `UNKNOWN` | Interbancarias con débito confirmado y rechazo externo |
| **Conciliación** | `NONE` \| `OPEN` → `RESOLVED_AUTOMATIC` \| `ESCALATED` → `RESOLVED_MANUAL` | Toda operación que pasó por la clase `UNKNOWN` |

**Estados globales** (en inglés, conforme a los [estándares de ingeniería](../00-estandares-de-ingenieria.md)). `UNKNOWN` designa la **clase** de resultado; los estados concretos llevan el prefijo del tramo.

| Estado global | Significado | ¿Terminal? | Obligación financiera pendiente |
|---|---|---|---|
| `RECEIVED` | Operación reservada con su clave ([ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), IC-06) | No | No |
| `VALIDATING` | Validaciones de dominio y de seguridad | No | No |
| `CORE_IN_PROGRESS` | Ejecución enviada o por enviarse al Core (persistido antes de invocar) | No | Posible |
| `CORE_UNKNOWN` | Resultado de la ejecución en el Core desconocido | No | Posible |
| `DEBIT_CONFIRMED` | Débito confirmado en una interbancaria; tramo externo aún no iniciado | No | **Sí**: fondos debitados sin transferencia externa |
| `PENDING_EXTERNAL_CONFIRMATION` | Tramo externo en curso, sin confirmación definitiva | No | **Sí** |
| `EXTERNAL_UNKNOWN` | Resultado del tramo externo desconocido | No | **Sí** |
| `REVERSAL_PENDING` | Rechazo externo confirmado tras el débito; **el reverso es obligatorio** | No | **Sí**: debe devolverse el débito |
| `REVERSAL_UNKNOWN` | Resultado del reverso desconocido | No | **Sí** |
| `REQUIRES_RECONCILIATION` | La conciliación automática no concluyó o el reverso no es posible por la vía automática. **Conserva el estado de todas las dimensiones** | No | Según las dimensiones |
| `COMPLETED` | Ejecución confirmada (propia) o confirmación externa **definitiva según el contrato** (interbancaria) | **Sí** | No |
| `REJECTED` | Rechazo de validación o rechazo confirmado del Core **antes de cualquier efecto** | **Sí** | No |
| `FAILED_NO_EFFECT` | Falla técnica de clase `NOT_SENT` agotada dentro del plazo, o ausencia de efecto **demostrada** por la conciliación | **Sí** | No |
| `REVERSED` | Débito devuelto, confirmado y conciliado | **Sí** | No |

El estado genérico `FAILED` de la versión 1 **se elimina**: confundía «sin efecto» con «con efecto no resuelto».

### 4. Invariantes

| ID | Invariante |
|---|---|
| **INV-T1** | Un estado terminal solo se alcanza si **no existe obligación financiera pendiente** en ninguna dimensión |
| **INV-T2** | Desde un estado con efecto posible o confirmado, solo se llega a `REJECTED` o a `FAILED_NO_EFFECT` con **evidencia confirmada de ausencia de efecto** (clase `REJECTED_CONFIRMED` o resultado de la conciliación) |
| **INV-T3** | Como máximo **una invocación de ejecución por tramo** y por operación ([ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), IC-14). Desde `*_UNKNOWN` no hay una nueva invocación de ejecución, solo consultas |
| **INV-T4** | Toda transición es **condicional sobre el estado y la versión previos** (comparar y fijar). Dos procesos no pueden aplicar la misma transición (M02) |
| **INV-T5** | **Recuperación:** al reiniciar o al retomar una operación, todo estado `*_IN_PROGRESS` sin resultado registrado se trata como el `*_UNKNOWN` correspondiente (M02) |
| **INV-T6** | **No se asume que toda operación sea reversible.** El reverso es una operación explícita, idempotente (clave derivada del identificador de la operación y del tramo), auditada y sujeta a la misma taxonomía. Si el contrato externo no permite el reverso por la vía automática (por ejemplo, cuando la devolución sigue un procedimiento interinstitucional), la dimensión de reverso queda en `NOT_POSSIBLE` y la operación pasa a `REQUIRES_RECONCILIATION` **con la obligación registrada** |
| **INV-T7** | Toda transición se audita con la causa, la evidencia, el identificador de correlación y la clave de idempotencia |
| **INV-T8** | **Notificaciones (R4):** cada **movimiento contable confirmado** (débito, crédito, reverso) se notifica cuando se confirma. El estado de la operación se comunica en sus hitos; los estados no terminales se comunican como «en verificación» o «en proceso», sin afirmar éxito ni falla |

### 5. Transiciones válidas

Ninguna transición puede violar los invariantes. La lista completa está en [07, sección 3](../07-maquina-de-estados-transaccional.md#3-máquina-de-estados-de-transferencias).

- **Transferencia propia** (S18: el Core la ejecuta como una sola operación): `RECEIVED` → `VALIDATING` → `CORE_IN_PROGRESS` → `COMPLETED` | `REJECTED` | `FAILED_NO_EFFECT` (solo clase `NOT_SENT`) | `CORE_UNKNOWN`. Desde `CORE_UNKNOWN` → `COMPLETED` | `FAILED_NO_EFFECT` (conciliación demuestra ausencia de efecto) | `REQUIRES_RECONCILIATION`.
- **Transferencia interbancaria:** tras `DEBIT_CONFIRMED` → `PENDING_EXTERNAL_CONFIRMATION` → `COMPLETED` | `REVERSAL_PENDING` | `EXTERNAL_UNKNOWN`. Desde `REVERSAL_PENDING` → `REVERSED` | `REVERSAL_UNKNOWN` | `REQUIRES_RECONCILIATION`.
- **Dependencia de la topología (Q-01, DD-16):** si el débito y el despacho externo son una única operación del Core (modelo B), `DEBIT_CONFIRMED` y `PENDING_EXTERNAL_CONFIRMATION` pueden fusionarse. Los invariantes no cambian.
- **Semántica de `COMPLETED` en las interbancarias:** corresponde al evento que el contrato de la infraestructura externa define como **definitivo** (aceptación irrevocable o liquidación). Se fija al resolver Q-01. Hasta entonces se representa con el parámetro `EVENTO_CONFIRMACION_EXTERNA`.

### 6. Resolución operativa de `REQUIRES_RECONCILIATION` (H03)

| Aspecto | Decisión |
|---|---|
| Responsable | **Rol de conciliación y operaciones** (rol conceptual). El área concreta de la institución se confirma con Q-08 y DD-18. **No hay casos sin dueño:** cada caso se asigna a una cola con responsable |
| Plazo | `T_RESOLUCION_MANUAL` por acordar, con dos niveles de escalamiento: supervisor de operaciones al vencer el plazo; riesgo operativo al vencer el doble del plazo |
| Segregación de funciones | La resolución requiere **dos personas**: una propone con evidencia y otra aprueba (*cuatro ojos*). Ningún operador resuelve un caso que inició él mismo |
| Límites de la resolución | La resolución manual **no mueve fondos por sí misma**. Si requiere un movimiento (por ejemplo, un reverso), se ejecuta como una **operación explícita** e idempotente. El estado final elegido debe ser coherente con la evidencia de cada dimensión (INV-T1, INV-T2) |
| Cliente | Ve «en verificación». Se le notifica la entrada en revisión con un plazo estimado y el canal de reclamo, y se le notifica la resolución |
| Monitoreo | Antigüedad por estado; alertas por vencimiento; métricas de volumen y causa raíz |

### 7. Por qué no «Saga» como modelo genérico

- Las operaciones financieras **no siempre se compensan** de forma segura con una transacción inversa simple. Un reverso es una nueva transacción con efectos visibles, contables y de reporte, y puede fallar (INV-T6).
- Algunos pasos **no son reversibles unilateralmente**.
- El caso dominante es la **incertidumbre**, que Saga no modela de forma natural.
- El término induce a suponer que existe una compensación automática segura.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Llamada síncrona sin estado durable | Descartada | Pierde el rastro de la operación ante reinicios. No distingue falla de incertidumbre |
| Saga con compensaciones automáticas | Descartada como modelo genérico | Ver la sección 7 |
| Transacción distribuida (2PC/XA) con el Core | Descartada | Se asume que el Core no la soporta (S3). Bloquea recursos y acopla la disponibilidad |
| Encolar la operación y ejecutarla cuando el Core vuelva | Descartada | Ejecuta en condiciones distintas a las aceptadas por el cliente |
| Estado único plano con `FAILED` genérico (versión 1) | **Descartada** (ARG01-H02) | Permite cerrar como fallida una operación con débito aplicado |
| **Máquina durable con taxonomía cerrada, dimensiones e invariantes** | **Seleccionada** | Hace explícitas la incertidumbre y las obligaciones financieras pendientes |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ninguna operación cerrada con una obligación oculta | Más estados y más transiciones que probar |
| Clasificación inequívoca de las fallas | Cada ACL debe mantener su tabla de clasificación por contrato |
| Recuperación y concurrencia seguras | Control de versión en cada transición; procesos de recuperación |
| Responsable y plazo para cada caso | Capacidad operativa dedicada (rol de conciliación) |

## Consecuencias

- El servicio de transferencias es el dueño de la orquestación de negocio, no el Gateway ([ADR-0002](ADR-0002-frontera-api-gateway.md)).
- Las notificaciones de movimientos se emiten por cada movimiento confirmado (INV-T8), no solo al llegar a un estado terminal.
- Nuevos parámetros por acordar: `T_RESOLUCION_MANUAL`, `EVENTO_CONFIRMACION_EXTERNA`.
- La tabla de clasificación de cada ACL es un **artefacto obligatorio** de la Fase 3 y de la fase de componentes.
- La organización debe confirmar el rol de conciliación (Q-08). Mientras no lo haga, H03 queda resuelto en el diseño y **pendiente de confirmación institucional**.

## Riesgos

- **RR-08:** complejidad del estado transaccional durable (aumenta con el modelo por dimensiones).
- **RR-04:** conciliación sin idempotencia nativa.
- **RR-16:** reverso no posible por la vía automática y obligación pendiente prolongada.
