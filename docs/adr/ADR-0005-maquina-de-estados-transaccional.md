# ADR-0005 — Máquina de estados transaccional durable para operaciones financieras

| Campo | Valor |
|---|---|
| Estado | **Aceptada — enmendada (versión 3)** por la remediación de CF-01 y CF-02 de ARG-01-R2 (R2-N01, R2-N02; también aborda R2-N03, R2-N05 y R2-N10). Modelo conceptual; la implementación se difiere (DD-08). Pendiente de verificación independiente |
| Fecha | 2026-10-27 (versiones 1, 2 y 3) |
| Requisitos | R1 (transferencias), R3, R4, C2 |
| Drivers y supuestos | S3, S4, S13, S18; AQ-02, AQ-04, AQ-05; QA3, QA5, QA9, QA12, QA13 |
| Relacionados | [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [ADR-0009](ADR-0009-core-autoritativo.md), [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md), [07 — Máquina de estados](../07-maquina-de-estados-transaccional.md), [ARG-01-R2](../reviews/ARG-01-R2-verificacion-independiente.md), [Remediación CF-01/CF-02](../reviews/ARG-01-R2-remediacion-CF01-CF02.md) |

## Historial

| Versión | Cambio | Origen |
|---|---|---|
| 1 | Máquina de estados durable con `UNKNOWN` y conciliación | Fase 1 |
| 2 | Taxonomía cerrada de resultados; estados financieros explícitos e invariantes; recuperación, concurrencia y resolución operativa | ARG01-H01, H02, H03, M02 |
| 3 | **La evidencia negativa no prueba la no ejecución:** clases de evidencia y prohibición de cerrar o reenviar con evidencia negativa o incompleta (CF-01). **Protocolo de propiedad:** arrendamiento, token de cercado monotónico, **permiso de envío único por tramo**, validez de la intención y registro de evidencia huérfana (CF-02). Cierre excepcional, efecto tardío, prioridad de la seguridad sobre la vivacidad. El estado global pasa a ser la única máquina autoritativa | ARG-01-R2: R2-N01, R2-N02, R2-N03, R2-N05, R2-N10 |

## Contexto

Las transferencias invocan sistemas externos: el Core y, en las interbancarias, una capacidad externa cuya topología está por validar (S13, Q-01). Se asume que el Core **no** ofrece idempotencia nativa, ni consultas por referencia con consistencia fuerte, ni validación de tokens de cercado (S3). Ninguna de esas capacidades se da por existente mientras no se valide (Q-02, Q-17).

La verificación ARG-01-R2 encontró dos defectos en la versión 2:

- **R2-N01:** la ausencia de coincidencias en la conciliación permitía declarar `FAILED_NO_EFFECT` (terminal) y habilitar un segundo reverso. La ausencia de evidencia no es evidencia de ausencia.
- **R2-N02:** no existía una propiedad exclusiva de la ejecución. Un ejecutor «zombi» podía enviar al Core después de que la recuperación hubiera tomado la operación.

## Problema

¿Cómo se garantiza que la plataforma **nunca** vuelva a ejecutar un tramo cuyo resultado externo podría haber tenido efecto, aun con procesos pausados, caídas, respuestas tardías y un Core que no coopera? ¿Y cómo se declara con honestidad hasta dónde puede garantizarse que toda operación se resuelva?

## Decisión

### 1. Máquina de estados durable

Las operaciones financieras críticas y la vinculación digital se modelan como una **máquina de estados transaccional durable** (*Durable Transaction State Machine*); para las transferencias, **orquestador de transacciones de transferencia** (*Transfer Transaction Orchestrator*). **El estado global es la única máquina autoritativa.** Lo que la versión 2 llamaba «dimensiones» (resultado del Core, resultado externo, resultado del reverso, caso de conciliación) son **atributos registrados** que solo cambian dentro de una transición global. «Obligación financiera pendiente» es una **función** de esos atributos (R2-N05). **No se encola** una transacción para ejecutarla después solo porque el Core no está disponible.

### 2. Taxonomía del resultado de una invocación

| Clase | Definición | Efecto en la máquina | ¿Nuevo envío? |
|---|---|---|---|
| **NOT_SENT** | Evidencia **local** de que ningún byte de la solicitud se escribió en una conexión hacia el destino ni hacia ningún intermediario | `FAILED_NO_EFFECT` | **No.** *Cambio de la versión 3:* no hay nuevos envíos automáticos ni siquiera en esta clase; el permiso de envío es único (sección 4.3). Un nuevo intento requiere una **nueva intención** del cliente |
| **REJECTED_CONFIRMED** | Respuesta del Core **a esta solicitud**, con un código que su contrato define como rechazo sin efecto | `REJECTED` | No aplica |
| **UNKNOWN** | Cualquier otra falla o respuesta no clasificada | `*_UNKNOWN` y conciliación | **Nunca** |
| **CONFIRMED** | Respuesta de éxito a esta solicitud, definitiva según el contrato | Avance | No aplica |

**En caso de duda, la clase es `UNKNOWN`.**

### 3. Evidencia y resolución (CF-01)

#### 3.1 Clases de evidencia

| Clase | Qué es | Qué permite declarar | Fuentes admisibles | Condición de uso |
|---|---|---|---|---|
| **EV-POS** — Positiva definitiva | Respuesta síncrona del Core a esta solicitud con éxito definitivo; o hallazgo de la operación en una fuente autoritativa **por su referencia única** | **Ejecución confirmada** | Respuesta de la invocación; consulta por referencia única (G2); evidencia huérfana recibida por un ejecutor cercado (sección 4.5) | La referencia única debe identificar **sin ambigüedad** esta operación |
| **EV-POS-CORR** — Positiva por correlación | Una única coincidencia por correlación de negocio (G3) en el **libro autoritativo de la cuenta** | **Ejecución confirmada**, solo si no existe otra operación equivalente no terminal (ADR-0006 IC-15) y el libro es autoritativo para esa cuenta | Movimientos del Core, no vistas derivadas | Si alguna condición falla: revisión manual |
| **EV-REJ** — Rechazo definitivo | Respuesta del Core **a esta solicitud** con un rechazo sin efecto según el contrato | **Rechazo definitivo** | Solo la respuesta a la invocación | Una respuesta **correlacionada** con la solicitud; nunca una inferencia |
| **EV-NOEXEC** — No ejecución demostrada | Prueba positiva de que el tramo **no se ejecutó y ya no puede ejecutarse** | **No ejecución demostrada** | (a) `NOT_SENT` registrado por el titular vigente; (b) **cierre por referencia**: el Core anula o rechaza permanentemente la referencia, de modo que una solicitud tardía con esa referencia no puede aplicarse; (c) **expiración en el Core**: el Core garantiza rechazar las solicitudes cuya validez venció, y una consulta por referencia **con consistencia fuerte**, posterior a ese vencimiento, no la encuentra | (b) y (c) requieren **capacidades del Core no validadas** (Q-17, DD-22). Mientras no se validen, la única fuente automática es (a) |
| **EV-INC** — No concluyente | Todo lo demás | **Resultado todavía desconocido** | — | — |

#### 3.2 Lo que **no** prueba la no ejecución

Una operación **potencialmente enviada** nunca pasa a `FAILED_NO_EFFECT` ni a `REJECTED`, ni habilita un nuevo envío o un segundo reverso, por ninguna de estas razones, **solas o combinadas**:

- un timeout, una desconexión o una respuesta perdida;
- la ausencia del resultado en consultas, sobre todo si son eventualmente consistentes (réplicas, vistas de reporte, extractos);
- la falta de movimientos visibles en la cuenta;
- la ausencia de confirmación;
- el vencimiento de un plazo de la plataforma (`T_ASENTAMIENTO_CORE`, `T_RESOLUCION_UNKNOWN` u otros);
- un resultado incompleto o fallido de la conciliación;
- una consulta «por referencia» cuya consistencia o cuyo cierre no estén garantizados por contrato.

**Respuesta definitiva frente a consulta eventualmente consistente:** una respuesta a la invocación es una afirmación del Core sobre **esa** solicitud. Una consulta posterior es una lectura de un estado que puede no reflejar todavía una solicitud en curso dentro del Core (colas, procesos batch, réplicas). Solo la primera, o una consulta con consistencia fuerte **y** cierre de la referencia, puede sostener una conclusión negativa.

#### 3.3 Resultado de la conciliación

| Evidencia reunida | Resultado |
|---|---|
| EV-POS | Ejecución confirmada: `COMPLETED` o `DEBIT_CONFIRMED`, según el tipo |
| EV-POS-CORR con sus condiciones | Ejecución confirmada |
| EV-NOEXEC | `FAILED_NO_EFFECT` |
| EV-INC | Permanece en `*_UNKNOWN`. Al vencer `T_RESOLUCION_UNKNOWN` pasa a `REQUIRES_RECONCILIATION`. **No se cierra ni se reenvía** |

### 4. Protocolo de propiedad y ejecución (CF-02)

#### 4.1 Elementos

| Elemento | Definición |
|---|---|
| **Titularidad** | Cada operación no terminal tiene, en su registro durable, un titular `(ejecutor, token, vencimiento)`. El ejecutor se identifica de forma única por proceso o instancia |
| **Arrendamiento** (*lease*) | La titularidad vence en `LEASE_DURACION`, medida con el **reloj del almacén durable** (fuente única de tiempo). El titular puede renovarla antes de que venza |
| **Token de cercado** | Un entero **monotónico** por operación que se incrementa en cada adquisición de titularidad |
| **Escritura cercada** | **Toda** transición, registro de resultado o renovación es condicional (comparar y fijar) sobre el estado, la versión **y el token**. Una escritura con un token antiguo se rechaza |
| **Permiso de envío** | Para cada tramo con efecto (ejecución en el Core, procesamiento externo, reverso) existe **un único permiso de envío** durante toda la vida de la operación. Se concede mediante una escritura cercada que pasa el tramo a `*_IN_PROGRESS` y registra el intento. **Una vez concedido no se vuelve a conceder.** El envío ocurre **solo después** de que el almacén confirme durablemente el permiso; si esa confirmación es incierta, no se envía |
| **Validez de la intención** | La intención del cliente vale hasta `PLAZO_VALIDEZ_INTENCION`. Después de ese instante, medido con el reloj del almacén, **no se concede** el permiso de envío |
| **Desfase máximo de envío** | El ejecutor no escribe la solicitud si desde la concesión del permiso, medido con su reloj monotónico local, transcurrió más de `MAX_DESFASE_ENVIO`. Es una **reducción de ventana**, no una garantía (sección 4.4) |
| **Correlación con el Core** | Toda solicitud lleva el identificador interno de la operación como referencia externa o en un campo libre (ADR-0006 IC-13), para que EV-POS y EV-POS-CORR sean posibles |
| **Evidencia huérfana** | Si un ejecutor que perdió la titularidad recibe una respuesta, no puede cambiar el estado: la registra en un **registro de evidencia de solo anexión**, indexado por el identificador de la operación, que la conciliación consume como EV-POS o EV-REJ |

#### 4.2 Secuencia de ejecución de un tramo

```text
1. Adquirir titularidad (escritura cercada: token ← token + 1; vencimiento ← ahora_almacén + LEASE_DURACION)
2. Obtener cupo de capacidad (ADR-0011). Si no hay cupo dentro del plazo → FAILED_NO_EFFECT (no hubo permiso)
3. Conceder el permiso de envío (escritura cercada; exige token vigente, arrendamiento vigente,
   intención vigente y que el permiso del tramo NO se haya concedido antes) → *_IN_PROGRESS
4. Esperar la confirmación durable del paso 3. Si es incierta → NO enviar (la recuperación lo tratará como UNKNOWN)
5. Verificar MAX_DESFASE_ENVIO con el reloj local. Si se excedió → NO enviar
6. Enviar UNA vez, con la referencia de correlación
7. Clasificar la respuesta (sección 2) y registrarla con una escritura cercada.
   Si la escritura falla por un token antiguo → registrar la respuesta como evidencia huérfana
```

#### 4.3 Garantías internas frente a garantías externas

| Garantía | Tipo | Se sostiene porque | Límite |
|---|---|---|---|
| **G-INT-1:** como máximo un titular puede **registrar** transiciones en cada momento | Coordinación interna | Escrituras cercadas sobre el almacén durable | Depende de la atomicidad del almacén |
| **G-INT-2:** como máximo **un permiso de envío** por tramo y por operación, durante toda su vida | Coordinación interna | El permiso es una transición única, cercada y no repetible | — |
| **G-INT-3:** ningún ejecutor (titular, nuevo titular ni zombi) **inicia un segundo envío** de un tramo | Coordinación interna + **disciplina del ejecutor** | Nadie envía sin haber obtenido un permiso nuevo, y no existe un segundo permiso | Supone que el código del ejecutor respeta el protocolo (verificación: CP-03) |
| **G-EXT-1:** el Core recibe **como máximo una solicitud** por tramo originada en la plataforma | Externa, **condicionada** | G-INT-2 y G-INT-3 | Supone que la red y los intermediarios **no duplican** solicitudes (supuesto externo SE-1). Supone que la clase `NOT_SENT` se determina correctamente (CP-02) |
| **G-EXT-2:** el Core **no ejecuta** una solicitud tardía | **No garantizada** | — | **El cercado local no lo impide.** Requiere capacidades del Core: cierre por referencia o expiración de solicitudes (DD-22) |
| **G-EXT-3:** el Core ejecuta como máximo una vez cada solicitud recibida | **No garantizada por la plataforma** | — | Supuesto externo SE-2 sobre el comportamiento del Core |

#### 4.4 La carrera crítica: validación, pérdida de titularidad, nuevo ejecutor y envío tardío

```text
t1  E1 concede el permiso de envío (token 7) → CORE_IN_PROGRESS            [confirmado durablemente]
t2  E1 se pausa (GC, red, VM) después de verificar MAX_DESFASE_ENVIO
t3  vence el arrendamiento de E1
t4  E2 adquiere la titularidad (token 8); ve CORE_IN_PROGRESS → solo puede pasar a CORE_UNKNOWN
    E2 NO puede conceder otro permiso de envío (el permiso del tramo ya se consumió)
t5  E1 reanuda y escribe la solicitud en el socket: ENVÍO TARDÍO
t6  El Core ejecuta (o no) esa única solicitud
t7  E1 recibe la respuesta; su escritura cercada falla (token 7 < 8) → evidencia huérfana
t8  La conciliación de E2 consume la evidencia huérfana (EV-POS o EV-REJ) → resolución
```

- **El cercado local NO impide el envío de t5.** Es imposible impedirlo sin la cooperación del Core: entre la verificación (t2) y la escritura en el socket (t5) no hay ningún punto donde el almacén pueda intervenir.
- **Lo que el diseño garantiza** es que el envío tardío sea el **único** envío de ese tramo (G-INT-2). No hay duplicación desde la plataforma, sino una **ejecución tardía única**. El estado no puede cerrarse como «sin efecto» mientras tanto (sección 3), así que la ejecución tardía se resuelve como `COMPLETED` o `DEBIT_CONFIRMED` cuando aparezca.
- **Costo:** si E1 nunca envía (por ejemplo, porque se agotó `MAX_DESFASE_ENVIO` o porque cayó entre t1 y t5), la operación queda en `CORE_UNKNOWN` **sin posibilidad de prueba automática** de no ejecución mientras el Core no ofrezca EV-NOEXEC (b) o (c). Es una pérdida de **vivacidad**, no de seguridad.

### 5. Estados globales

| Estado | ¿Terminal? | Obligación financiera pendiente |
|---|---|---|
| `RECEIVED`, `VALIDATING` | No | No (sin permiso de envío) |
| `CORE_IN_PROGRESS` | No | Posible (permiso concedido) |
| `CORE_UNKNOWN` | No | Posible |
| `DEBIT_CONFIRMED`, `PENDING_EXTERNAL_CONFIRMATION`, `EXTERNAL_UNKNOWN` | No | **Sí** |
| `REVERSAL_PENDING`, `REVERSAL_UNKNOWN` | No | **Sí** |
| `REQUIRES_RECONCILIATION` | No | Según los atributos (se conserva) |
| `COMPLETED` | **Sí** | No |
| `REJECTED` | **Sí** | No (EV-REJ o rechazo de validación) |
| `FAILED_NO_EFFECT` | **Sí** | No (**solo con EV-NOEXEC**, o porque nunca se concedió el permiso de envío) |
| `REVERSED` | **Sí** | No (reverso confirmado con EV-POS) |
| `CLOSED_BY_EXCEPTION` (nuevo) | **Sí** | **Incertidumbre residual declarada**: cierre por autoridad, sin prueba. Activa la vigilancia de efecto tardío (sección 7) |

### 6. Invariantes

| ID | Invariante |
|---|---|
| **INV-T1** | Un estado terminal solo se alcanza sin obligación financiera pendiente, **salvo** `CLOSED_BY_EXCEPTION`, que registra explícitamente la incertidumbre residual, la autoridad que cerró y la evidencia disponible, y activa la vigilancia de efecto tardío |
| **INV-T2** | `REJECTED` solo con EV-REJ o con un rechazo de validación previo al permiso. `FAILED_NO_EFFECT` solo con EV-NOEXEC o sin permiso de envío concedido. **Nunca con EV-INC** |
| **INV-T3** | **Un único permiso de envío por tramo durante toda la vida de la operación** (G-INT-2). No existe el «nuevo intento» de envío |
| **INV-T4** | Toda escritura de estado es **cercada**: condicional sobre el estado, la versión y el token vigente |
| **INV-T5** | **Recuperación:** un nuevo titular que encuentra un tramo en `*_IN_PROGRESS` solo puede pasarlo a `*_UNKNOWN`; jamás lo envía |
| **INV-T6** | No se asume que toda operación sea reversible. El reverso es un tramo con su propio permiso único (INV-T3) |
| **INV-T7** | Toda transición se audita con la causa, la evidencia, el token, el identificador de correlación y la clave de idempotencia |
| **INV-T8** | Cada movimiento contable confirmado se notifica (R4). Los estados no terminales no afirman éxito ni falla |
| **INV-T9** | **Ninguna evidencia de la sección 3.2 cierra una operación ni habilita un nuevo envío o un segundo reverso** |
| **INV-T10** | No se concede el permiso de envío después de `PLAZO_VALIDEZ_INTENCION` (medido con el reloj del almacén). Una operación sin permiso cuya intención venció pasa a `FAILED_NO_EFFECT` (R2-N03) |
| **INV-T11** | Un efecto descubierto después de un estado terminal (`FAILED_NO_EFFECT`, `CLOSED_BY_EXCEPTION`) **no reabre** el estado: abre un **caso de efecto tardío** vinculado, de resolución manual con cuatro ojos. Nunca hay corrección automática de fondos |

### 7. Escalamiento, cierre excepcional y efecto tardío

| Regla | Contenido |
|---|---|
| Escalamiento | `*_UNKNOWN` con EV-INC pasa a `REQUIRES_RECONCILIATION` al vencer `T_RESOLUCION_UNKNOWN`. Se asigna a una cola con responsable y escala según `T_RESOLUCION_MANUAL` (sección 9) |
| Resolución con evidencia | El rol de conciliación puede resolver hacia `COMPLETED`, `DEBIT_CONFIRMED`, `REVERSED`, `REJECTED` o `FAILED_NO_EFFECT` **solo con la evidencia que exige INV-T2**, ya sea obtenida de fuentes del Core, de la evidencia huérfana o de documentos oficiales del propietario del Core |
| **Cierre excepcional** (`CLOSED_BY_EXCEPTION`) | Solo cuando, vencido `PLAZO_CIERRE_EXCEPCIONAL`, no existe evidencia concluyente. Requiere **cuatro ojos y una autoridad de nivel superior** a la resolución ordinaria; un expediente con la evidencia reunida; y una decisión explícita sobre el cliente: comunicación y, si corresponde, una **compensación institucional** como operación explícita y autorizada (R2-N10) |
| Efecto sobre el cliente | Mientras la operación no sea terminal, las operaciones **equivalentes** del cliente están bloqueadas (ADR-0006 IC-15). Tras `CLOSED_BY_EXCEPTION`, el cliente puede iniciar una nueva intención **solo** mediante una acción explícita, con aviso de la incertidumbre residual. **Nunca de forma automática** |
| Vigilancia de efecto tardío | Durante `VENTANA_VIGILANCIA_EFECTO_TARDIO`, la conciliación de segunda línea (extractos y cierres del Core) busca efectos de las operaciones `FAILED_NO_EFFECT` y `CLOSED_BY_EXCEPTION`. Si los encuentra, abre un caso de efecto tardío (INV-T11) |

### 8. Seguridad frente a vivacidad

| Propiedad | Definición | Prioridad | Qué se garantiza |
|---|---|---|---|
| **Seguridad** (*safety*) | La plataforma no vuelve a ejecutar un tramo cuyo resultado externo podría haber tenido efecto, ni cierra una obligación sin evidencia suficiente | **Prioritaria e innegociable** | G-INT-1 a G-INT-3, INV-T2, INV-T3, INV-T9. G-EXT-1 bajo los supuestos SE-1 y SE-2 |
| **Vivacidad** (*liveness*) | Toda operación avanza o se resuelve en algún momento | **Subordinada** | **Solo se garantiza que toda operación incierta llegue a un responsable humano** en `T_RESOLUCION_UNKNOWN` + `T_RESOLUCION_MANUAL`. **No se garantiza** su resolución: depende de la evidencia del Core, de terceros y de la conciliación manual |

Cuando el Core no aporta garantías suficientes, **se sacrifica la vivacidad**: la operación queda abierta o escalada, y el cliente bloqueado para operaciones equivalentes. **Nunca la integridad.** Ningún problema de disponibilidad se resuelve con un reenvío o un cierre sin evidencia.

### 9. Resolución operativa de `REQUIRES_RECONCILIATION`

| Aspecto | Decisión |
|---|---|
| Responsable | Rol de conciliación y operaciones; cada caso en una cola con responsable. Área concreta: Q-08, DD-18 |
| Plazo | `T_RESOLUCION_MANUAL` con escalamiento al supervisor y a riesgo operativo; `PLAZO_CIERRE_EXCEPCIONAL` para el cierre excepcional |
| Segregación de funciones | Cuatro ojos; nadie resuelve un caso propio; el cierre excepcional requiere una autoridad superior |
| Límites | La resolución no mueve fondos por sí misma; todo movimiento es una operación explícita, idempotente y autorizada |
| Cliente | «En verificación»; aviso de la entrada en revisión con plazo estimado y canal de reclamo; aviso de la resolución |

### 10. Por qué no «Saga» como modelo genérico

Las compensaciones financieras no siempre son seguras ni posibles. El caso dominante es la incertidumbre, que Saga no modela, y el término sugiere una compensación automática que aquí está prohibida sin evidencia (INV-T9, INV-T11).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Cerrar como «sin efecto» al no encontrar coincidencias tras un plazo (versión 2) | **Descartada** (R2-N01) | La ausencia de evidencia no es evidencia de ausencia |
| Estado «sin efecto presunto» no terminal que se confirma automáticamente tras la conciliación de segunda línea | Descartada como cierre automático | Sigue basándose en evidencia negativa. Su función se cubre con `REQUIRES_RECONCILIATION` y el cierre excepcional, ambos con autoridad humana |
| Arrendamiento sin token de cercado | Descartada | Un titular vencido podría registrar transiciones |
| Token de cercado validado solo en el almacén, permitiendo reenvíos tras validar | Descartada | No impide el envío tardío; si además hubiera nuevos intentos, habría dos solicitudes |
| **Permiso de envío único por tramo + cercado interno + evidencia huérfana** | **Seleccionada** | Convierte el envío tardío en el **único** envío. No depende del Core |
| Proxy de cercado junto al Core que valida el token antes de reenviar | No seleccionada como base | Reduce la ventana, pero no la elimina (la carrera se traslada al proxy). Introduce un componente central (tensión con ADR-0001). Se reevaluará si el Core no ofrece DD-22 |
| Retención de fondos y captura en dos fases en el Core | **Condicionada** (DD-22) | Daría determinismo (anular la retención es EV-NOEXEC), pero requiere una capacidad del Core no validada |
| Expiración de solicitudes en el Core | **Condicionada** (DD-22) | Haría inocuos los envíos tardíos tras el vencimiento y habilitaría EV-NOEXEC (c) |
| Una sola operación crítica en curso por cuenta de origen | No seleccionada | Elimina la ambigüedad de la correlación por cuenta, pero degrada la experiencia y el rendimiento. El bloqueo de equivalentes (IC-15) cubre el caso de mayor riesgo |
| Servicio de bloqueo distribuido externo | Descartada | No resuelve la carrera con el Core e introduce una tecnología sin necesidad demostrada |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ningún cierre ni reenvío basado en evidencia negativa | Más operaciones en revisión manual mientras el Core no ofrezca EV-NOEXEC |
| El envío tardío de un zombi no duplica | Las caídas entre la concesión del permiso y el envío dejan operaciones irresolubles automáticamente |
| Ningún reintento automático, ni siquiera en `NOT_SENT` | Más fallas visibles para el cliente ante fallas transitorias previas al envío |
| Garantías internas y externas separadas con honestidad | La vivacidad depende de humanos y del Core |

## Consecuencias

- **El Core condiciona la vivacidad.** Q-17 y DD-22 (consulta con consistencia fuerte y cierre por referencia, expiración de solicitudes, retención y captura) pasan a ser las validaciones de **mayor valor** para reducir los casos manuales.
- La capacidad del rol de conciliación debe dimensionarse para el volumen de `REQUIRES_RECONCILIATION`, que deja de ser residual mientras no exista EV-NOEXEC automática (RR-17).
- Nuevos parámetros: `LEASE_DURACION`, `PLAZO_VALIDEZ_INTENCION`, `MAX_DESFASE_ENVIO`, `PLAZO_CIERRE_EXCEPCIONAL`, `VENTANA_VIGILANCIA_EFECTO_TARDIO`.
- Las tablas de clasificación de cada ACL distinguen EV-REJ (respuesta correlacionada) de cualquier inferencia.
- Verificación antes de producción: CP-02 (clasificación `NOT_SENT` real) y CP-03 (cercado y permiso único).

## Riesgos

- **RR-04:** pasa de un riesgo de **duplicación** a un riesgo de **vivacidad**: la evidencia negativa ya no cierra.
- **RR-08:** complejidad del protocolo de propiedad.
- **RR-16:** reverso no posible por la vía automática.
- **RR-17:** operaciones sin resolución concluyente (vivacidad y carga manual).
- **RR-18:** ejecución tardía única de un ejecutor zombi.
