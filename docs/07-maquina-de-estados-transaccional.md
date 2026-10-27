# 07 — Máquina de estados transaccional durable

| Campo | Valor |
|---|---|
| Fase | 1. Modelo conceptual; **la implementación se diseña en fases posteriores**. Versión 2: remediación ARG-01 |
| Estado | Modelo conceptual adoptado. Pendiente de verificación en ARG-01-R2 |
| ADR principales | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md), [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md) |

## 1. Propósito

Las transferencias y la vinculación digital tienen pasos con sistemas externos y fallas parciales. Además, un timeout no prueba que la operación haya fallado. Para controlarlas se adopta una **máquina de estados transaccional durable** (*Durable Transaction State Machine*), también llamada **orquestador de transacciones de transferencia** (*Transfer Transaction Orchestrator*).

«Durable» significa que el estado se persiste **antes** de cada interacción con un sistema externo con posible efecto. Si un proceso se reinicia, la operación no se pierde y se recupera según la regla INV-T5 de [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md).

### 1.1 Cambios de la versión 2 (remediación ARG-01)

| Hallazgo | Cambio |
|---|---|
| ARG01-H01 | Taxonomía cerrada del resultado de una invocación (sección 2) |
| ARG01-H02 | Modelo por dimensiones e invariantes. Se elimina `FAILED`. `SPI_PROCESSING` pasa a `PENDING_EXTERNAL_CONFIRMATION` (sección 3) |
| ARG01-H03 | Resolución operativa de `REQUIRES_RECONCILIATION` (sección 4) |
| ARG01-M02 | Recuperación, concurrencia e indisponibilidad del proveedor de identidad (secciones 3.4 y 5) |
| ARG01-H05 | Vinculación digital en etapas, con autorización explícita (sección 5) |

## 2. Por qué no se usa «Saga» como nombre genérico

El patrón Saga coordina una secuencia de transacciones locales con **acciones compensatorias**. No se adopta como modelo genérico porque:

- una compensación financiera (reverso) es una **nueva** transacción con efectos visibles, que puede fallar o no estar permitida;
- algunos pasos **no son reversibles unilateralmente**;
- el caso dominante es la **incertidumbre**, que Saga no modela.

Las compensaciones se modelan como **estados y operaciones explícitas, idempotentes y auditadas** (INV-T6).

### 2.1 Taxonomía del resultado de una invocación

Resumen; la definición normativa está en [ADR-0005, sección 2](adr/ADR-0005-maquina-de-estados-transaccional.md#2-taxonomía-cerrada-del-resultado-de-una-invocación-externa-h01).

| Clase | Regla | ¿Nueva ejecución? |
|---|---|---|
| `NOT_SENT` | Evidencia local de que **ningún byte** salió hacia el destino ni hacia un intermediario | Sí, dentro del plazo y según la política de la ACL |
| `REJECTED_CONFIRMED` | Rechazo que el contrato externo define como «sin efecto» | No aplica |
| `UNKNOWN` | Cualquier otra falla, incluidos timeouts, desconexiones, errores de intermediarios y códigos no mapeados | **No.** Solo consultas y conciliación |
| `CONFIRMED` | Éxito definitivo según el contrato | No aplica |

**En caso de duda, la clase es `UNKNOWN`.**

## 3. Máquina de estados de transferencias

### 3.1 Dimensiones

| Dimensión | Estados |
|---|---|
| Solicitud | `RECEIVED` → `VALIDATING` → aceptada \| `REJECTED` |
| Ejecución en el Core | `NOT_STARTED` → `IN_PROGRESS` → `CONFIRMED` \| `REJECTED` \| `UNKNOWN` |
| Procesamiento externo (interbancaria) | `NOT_APPLICABLE` \| `NOT_STARTED` → `IN_PROGRESS` → `CONFIRMED` \| `REJECTED` \| `UNKNOWN` |
| Reverso | `NOT_REQUIRED` \| `REQUIRED` → `IN_PROGRESS` → `CONFIRMED` \| `NOT_POSSIBLE` \| `UNKNOWN` |
| Conciliación | `NONE` \| `OPEN` → `RESOLVED_AUTOMATIC` \| `ESCALATED` → `RESOLVED_MANUAL` |

### 3.2 Estados globales

| Estado | ¿Terminal? | Obligación financiera pendiente |
|---|---|---|
| `RECEIVED`, `VALIDATING` | No | No |
| `CORE_IN_PROGRESS`, `CORE_UNKNOWN` | No | Posible |
| `DEBIT_CONFIRMED`, `PENDING_EXTERNAL_CONFIRMATION`, `EXTERNAL_UNKNOWN` | No | **Sí** |
| `REVERSAL_PENDING`, `REVERSAL_UNKNOWN` | No | **Sí** (devolución del débito) |
| `REQUIRES_RECONCILIATION` | No | Según las dimensiones (se conserva) |
| `COMPLETED` | **Sí** | No |
| `REJECTED` | **Sí** | No (sin efecto) |
| `FAILED_NO_EFFECT` | **Sí** | No (ausencia de efecto demostrada) |
| `REVERSED` | **Sí** | No (débito devuelto y conciliado) |

### 3.3 Transiciones válidas

| Desde | Hacia | Condición y evidencia |
|---|---|---|
| — | `RECEIVED` | Reserva atómica de (ámbito, clave) y huella ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), IC-06) |
| `RECEIVED` | `VALIDATING` | — |
| `VALIDATING` | `REJECTED` | Validación de dominio o de seguridad fallida. Sin efectos externos |
| `VALIDATING` | `CORE_IN_PROGRESS` | Validación correcta. **Persistido antes de invocar** |
| `CORE_IN_PROGRESS` | `FAILED_NO_EFFECT` | Clase `NOT_SENT` agotada dentro del plazo de negocio (incluido el rechazo del control de admisión, [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md)) |
| `CORE_IN_PROGRESS` | `REJECTED` | Clase `REJECTED_CONFIRMED` del Core |
| `CORE_IN_PROGRESS` | `CORE_UNKNOWN` | Clase `UNKNOWN`, o recuperación tras un reinicio (INV-T5) |
| `CORE_IN_PROGRESS` | `COMPLETED` | Transferencia **propia** con clase `CONFIRMED` (S18) |
| `CORE_IN_PROGRESS` | `DEBIT_CONFIRMED` | Transferencia **interbancaria** con débito `CONFIRMED` |
| `CORE_UNKNOWN` | `COMPLETED` o `DEBIT_CONFIRMED` | La conciliación confirma la ejecución (según el tipo) |
| `CORE_UNKNOWN` | `FAILED_NO_EFFECT` | La conciliación **demuestra** la ausencia de efecto (G2, o coincidencia nula tras `T_ASENTAMIENTO_CORE` sin indicios de actividad) |
| `CORE_UNKNOWN` | `REQUIRES_RECONCILIATION` | Conciliación no concluyente o vencimiento de `T_RESOLUCION_UNKNOWN` |
| `DEBIT_CONFIRMED` | `PENDING_EXTERNAL_CONFIRMATION` | Tramo externo **persistido antes de invocar** |
| `PENDING_EXTERNAL_CONFIRMATION` | `COMPLETED` | `EVENTO_CONFIRMACION_EXTERNA` recibido (definitivo según el contrato) |
| `PENDING_EXTERNAL_CONFIRMATION` | `REVERSAL_PENDING` | Rechazo externo **confirmado** después del débito |
| `PENDING_EXTERNAL_CONFIRMATION` | `EXTERNAL_UNKNOWN` | Clase `UNKNOWN` del tramo externo o recuperación (INV-T5) |
| `EXTERNAL_UNKNOWN` | `COMPLETED` o `REVERSAL_PENDING` | Conciliación concluyente |
| `EXTERNAL_UNKNOWN` | `REQUIRES_RECONCILIATION` | Conciliación no concluyente o plazo vencido |
| `REVERSAL_PENDING` | `REVERSED` | Reverso `CONFIRMED` y conciliado. Se ejecuta como una operación explícita con clave derivada (INV-T6), por la plataforma o por la infraestructura existente según Q-01 |
| `REVERSAL_PENDING` | `REVERSAL_UNKNOWN` | Clase `UNKNOWN` del reverso |
| `REVERSAL_PENDING` | `REQUIRES_RECONCILIATION` | Reverso `NOT_POSSIBLE` por la vía automática (obligación registrada) |
| `REVERSAL_UNKNOWN` | `REVERSED` | La conciliación confirma el reverso |
| `REVERSAL_UNKNOWN` | `REVERSAL_PENDING` | La conciliación **demuestra** que el reverso no se aplicó. Se habilita un nuevo intento con la **misma** clave derivada |
| `REVERSAL_UNKNOWN` | `REQUIRES_RECONCILIATION` | Conciliación no concluyente |
| `REQUIRES_RECONCILIATION` | `COMPLETED`, `REVERSED`, `FAILED_NO_EFFECT` | **Solo** por resolución manual con cuatro ojos, coherente con la evidencia de cada dimensión (sección 4). Si se requiere un movimiento, primero se ejecuta como operación explícita |

**Transiciones prohibidas** (violan INV-T1 o INV-T2):

- de cualquier estado con obligación pendiente a `REJECTED` o a `FAILED_NO_EFFECT` sin evidencia de ausencia de efecto;
- de `DEBIT_CONFIRMED`, `PENDING_EXTERNAL_CONFIRMATION`, `EXTERNAL_UNKNOWN`, `REVERSAL_*` a `FAILED_NO_EFFECT` o a `REJECTED`;
- de cualquier `*_UNKNOWN` a una nueva invocación de ejecución.

### 3.4 Recuperación y concurrencia (ARG01-M02)

- **Recuperación (INV-T5):** al reiniciar o retomar, todo `CORE_IN_PROGRESS` o `PENDING_EXTERNAL_CONFIRMATION` sin resultado registrado pasa al `*_UNKNOWN` correspondiente, **nunca** a una nueva invocación. Lo mismo vale para el reverso.
- **Concurrencia (INV-T4):** toda transición se aplica de forma condicional sobre el estado y la versión previos. Si dos procesos intentan avanzar la misma operación (por ejemplo, el conciliador y una consulta del cliente), solo uno lo consigue y el otro relee el estado.
- **Reenvíos del cliente:** devuelven el estado actual ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), IC-08); nunca crean una transición.

### 3.5 Notificaciones (R4)

- Se notifica **cada movimiento contable confirmado**: el débito al pasar a `COMPLETED` (propia) o a `DEBIT_CONFIRMED` (interbancaria), y la devolución al pasar a `REVERSED`.
- Se notifican los hitos de la operación. Los estados no terminales se comunican sin afirmar éxito ni falla.

### 3.6 Dependencias abiertas

- La secuencia interbancaria depende de la topología (Q-01, DD-16). En el modelo B, `DEBIT_CONFIRMED` y `PENDING_EXTERNAL_CONFIRMATION` pueden fusionarse sin cambiar los invariantes.
- `EVENTO_CONFIRMACION_EXTERNA` se fija con el contrato de la infraestructura externa.
- Las devoluciones posteriores a un `COMPLETED` (por ejemplo, devoluciones iniciadas por la institución destino) se modelarán como operaciones relacionadas, no reabriendo el estado terminal.

## 4. Resolución operativa de `REQUIRES_RECONCILIATION` (ARG01-H03)

| Aspecto | Regla |
|---|---|
| Responsable | Rol de conciliación y operaciones. Cada caso se asigna a una cola con responsable. El área concreta se confirma con Q-08 y DD-18 |
| Plazo y escalamiento | `T_RESOLUCION_MANUAL`; escalamiento al supervisor al vencer, y a riesgo operativo al doble del plazo |
| Cuatro ojos | Una persona propone con evidencia y otra aprueba. Nadie resuelve un caso propio |
| Movimientos | La resolución no mueve fondos por sí misma. Todo movimiento es una operación explícita e idempotente |
| Coherencia | El estado final debe ser coherente con la evidencia de cada dimensión (INV-T1, INV-T2) |
| Cliente | «En verificación», notificación de la entrada en revisión con plazo estimado y canal de reclamo, y notificación de la resolución |
| Monitoreo | Antigüedad por estado, vencimientos, volumen y causa raíz |

## 5. Máquina de estados de vinculación digital

Detalle de [ADR-0009](adr/ADR-0009-core-autoritativo.md) (reglas VD-01 a VD-10).

### 5.1 Estados

| Estado | Etapa | Significado | ¿Terminal? |
|---|---|---|---|
| `STARTED` | — | Solicitud reservada con su clave ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md)) | No |
| `IDENTITY_VERIFICATION_IN_PROGRESS` | E1 | KYC, prueba de vida y reconocimiento facial en curso | No |
| `IDENTITY_VERIFICATION_UNAVAILABLE` | E1 | El proveedor no respondió o devolvió un error técnico. **No es un rechazo** (VD-09) | No (reintento dentro de la vigencia o expiración) |
| `IDENTITY_REJECTED` | E1 | Verificación no superada | **Sí** |
| `IDENTITY_VERIFIED` | E1 | Identidad verificada, con vigencia `VIGENCIA_VERIFICACION_IDENTIDAD` | No |
| `CUSTOMER_LOOKUP_IN_PROGRESS` | E2 | Búsqueda del cliente en el Core por identificación | No |
| `CORE_CREATION_IN_PROGRESS` | E3 | Alta en el Core, solo si E2 concluyó «no existe». **Persistido antes de invocar** | No |
| `CORE_CREATION_UNKNOWN` | E3 | Resultado del alta desconocido | No |
| `CORE_CREATION_REJECTED` | E3 | El Core rechazó el alta de forma confirmada | **Sí** (o `MANUAL_REVIEW` según la causa) |
| `CUSTOMER_IDENTIFIED` | E2 o E3 | Cliente identificado con origen `PRE_EXISTING` o `CREATED_BY_THIS_REQUEST` | No |
| `LINK_PENDING` | E4 | Pendiente de la prueba de control (obligatoria si el origen es `PRE_EXISTING`, VD-03) | No |
| `LINK_AUTHORIZED` | E4 | Vinculación autorizada (VD-02 o VD-03) | No |
| `LINK_REJECTED` | E4 | Prueba de control fallida o no completada | **Sí** |
| `DIGITAL_ACCESS_ACTIVE` | E5 y E6 | Credenciales activas; productos habilitados según E6, con posibles restricciones iniciales (VD-10) | **Sí** (éxito) |
| `MANUAL_REVIEW` | Varias | Coincidencia ambigua, inconsistencias o conciliación sin evidencia | No |
| `EXPIRED` | Varias | Vencida la vigencia de la verificación o abandono | **Sí** |

### 5.2 Transiciones principales

| Desde | Hacia | Condición |
|---|---|---|
| `STARTED` | `IDENTITY_VERIFICATION_IN_PROGRESS` | — |
| `IDENTITY_VERIFICATION_IN_PROGRESS` | `IDENTITY_VERIFIED` / `IDENTITY_REJECTED` | Resultado del proveedor |
| `IDENTITY_VERIFICATION_IN_PROGRESS` | `IDENTITY_VERIFICATION_UNAVAILABLE` | Sin respuesta o error técnico (VD-09) |
| `IDENTITY_VERIFICATION_UNAVAILABLE` | `IDENTITY_VERIFICATION_IN_PROGRESS` / `EXPIRED` | Reintento dentro de la vigencia / vencimiento |
| `IDENTITY_VERIFIED` | `CUSTOMER_LOOKUP_IN_PROGRESS` | — |
| `CUSTOMER_LOOKUP_IN_PROGRESS` | `CUSTOMER_IDENTIFIED` (`PRE_EXISTING`) | Exactamente un cliente con esa identificación |
| `CUSTOMER_LOOKUP_IN_PROGRESS` | `CORE_CREATION_IN_PROGRESS` | No existe; el resultado «no existe» se registra como evidencia (VD-05) |
| `CUSTOMER_LOOKUP_IN_PROGRESS` | `MANUAL_REVIEW` | Coincidencia ambigua o datos inconsistentes (VD-04) |
| `CUSTOMER_LOOKUP_IN_PROGRESS` | (se mantiene, con reintento) | Falla de la consulta. Es una lectura de clase CL-2, sin efecto |
| `CORE_CREATION_IN_PROGRESS` | `CUSTOMER_IDENTIFIED` (`CREATED_BY_THIS_REQUEST`) | Alta `CONFIRMED` |
| `CORE_CREATION_IN_PROGRESS` | `CORE_CREATION_REJECTED` | `REJECTED_CONFIRMED` |
| `CORE_CREATION_IN_PROGRESS` | `CORE_CREATION_UNKNOWN` | Clase `UNKNOWN` o recuperación (INV-T5) |
| `CORE_CREATION_UNKNOWN` | `CUSTOMER_IDENTIFIED` (`CREATED_BY_THIS_REQUEST`) | **Solo con evidencia de creación** (VD-05) |
| `CORE_CREATION_UNKNOWN` | `CORE_CREATION_IN_PROGRESS` | Se repite E2 y se **demuestra** que no existe el cliente. Nuevo intento con la **misma** clave (VD-06) |
| `CORE_CREATION_UNKNOWN` | `MANUAL_REVIEW` | Sin evidencia concluyente |
| `CUSTOMER_IDENTIFIED` (`CREATED_BY_THIS_REQUEST`) | `LINK_AUTHORIZED` | VD-02 |
| `CUSTOMER_IDENTIFIED` (`PRE_EXISTING`) | `LINK_PENDING` | VD-01, VD-03 |
| `LINK_PENDING` | `LINK_AUTHORIZED` / `LINK_REJECTED` | Prueba de control independiente superada o fallida (mecanismo: DD-21) |
| `LINK_AUTHORIZED` | `DIGITAL_ACCESS_ACTIVE` | Credenciales registradas (E5) y productos habilitados (E6) |
| `MANUAL_REVIEW` | `CUSTOMER_IDENTIFIED`, `LINK_REJECTED`, `EXPIRED` | Resolución con cuatro ojos y evidencia |

### 5.3 Invariantes de la vinculación

| ID | Invariante |
|---|---|
| **INV-O1** | La coincidencia de identificación nunca conduce a `LINK_AUTHORIZED` sin pasar por `LINK_PENDING` con prueba de control (VD-01, VD-03) |
| **INV-O2** | `CREATED_BY_THIS_REQUEST` solo se asigna con confirmación del alta o con evidencia de creación (VD-05) |
| **INV-O3** | Nunca se reintenta el alta sin repetir E2 (VD-06) |
| **INV-O4** | Ningún acceso a productos antes de `DIGITAL_ACCESS_ACTIVE` |
| **INV-O5** | Las respuestas anteriores a la prueba de control no revelan la condición de cliente (VD-07) |
| **INV-O6** | La indisponibilidad del proveedor no se trata como rechazo ni como aprobación (VD-09) |

## 6. Lo que este documento no decide

- La tecnología del motor de estados ni la de su persistencia (DD-08).
- Los proveedores de KYC y biometría (DD-14).
- El mecanismo de la prueba de control (DD-21) y las restricciones iniciales (fase de seguridad).
- Los canales de notificación (DD-12).
- La topología interbancaria (DD-16).
