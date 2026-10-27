# ARG-01-R2 — Remediación de CF-01 y CF-02

| Campo | Valor |
|---|---|
| Origen | [ARG-01-R2 — Verificación independiente](ARG-01-R2-verificacion-independiente.md) (veredicto CONDITIONAL GO); condiciones bloqueantes CF-01 (R2-N01) y CF-02 (R2-N02) |
| Línea base | Commit `93c6207` |
| Fecha | 2026-10-27 |
| Autor | Arquitecto de la solución |
| Estado | **Remediado en diseño — pendiente de verificación independiente** |

> Todo el análisis es **conceptual**: no existe software y no se ejecutó ninguna prueba. Cuando una propiedad depende de un mecanismo externo no validado, se indica de forma explícita y **no** se presenta como demostrada.

---

## 1. Diagnóstico

| Defecto | Causa raíz | Manifestaciones en la línea base |
|---|---|---|
| **R2-N01 (CF-01)** | Se trataba la **ausencia de evidencia** (no encontrar la operación) como **evidencia de ausencia** | `CORE_UNKNOWN → FAILED_NO_EFFECT` por coincidencia nula tras `T_ASENTAMIENTO_CORE` (07, 08); `REVERSAL_UNKNOWN → REVERSAL_PENDING` con evidencia negativa (07); INV-T2 aceptaba cualquier «resultado de la conciliación». **Hallazgo adicional de esta remediación, no señalado por ARG-01-R2:** `CORE_CREATION_UNKNOWN → CORE_CREATION_IN_PROGRESS` reenviaba el alta del cliente cuando una búsqueda no lo encontraba (07; ADR-0009 VD-06), que es el mismo defecto aplicado a la identidad |
| **R2-N02 (CF-02)** | La recuperación y los nuevos intentos `NOT_SENT` no estaban ligados a una **titularidad exclusiva**, y no existía un límite al **número de permisos de envío** | Un ejecutor pausado podía enviar después de que la recuperación tomara la operación; los nuevos intentos `NOT_SENT` no revalidaban la titularidad |

## 2. Decisiones

| ID | Decisión | Documento |
|---|---|---|
| D-CF01-1 | **Clases de evidencia:** EV-POS, EV-POS-CORR, EV-REJ, EV-NOEXEC y EV-INC. Solo EV-NOEXEC demuestra la no ejecución, y sus fuentes automáticas (b) y (c) **requieren capacidades del Core no validadas** | ADR-0005 v3 §3.1 |
| D-CF01-2 | **Lista cerrada de lo que no prueba la no ejecución:** timeouts, consultas vacías, falta de movimientos, ausencia de confirmación, plazos, conciliación incompleta y consultas sin consistencia ni cierre garantizados | ADR-0005 v3 §3.2; INV-T9 |
| D-CF01-3 | Se distingue la **respuesta definitiva** a una solicitud de una **consulta eventualmente consistente** | ADR-0005 v3 §3.2 |
| D-CF01-4 | **Cierre excepcional** (`CLOSED_BY_EXCEPTION`) por autoridad, sin presentarlo como prueba; vigilancia y **caso de efecto tardío** sin reabrir estados ni corregir fondos automáticamente | ADR-0005 v3 §7; INV-T1, INV-T11 |
| D-CF01-5 | **Ningún segundo reverso automático**; una operación no resuelta no habilita una nueva intención equivalente de forma automática | 07 §3.3; ADR-0006 IC-12 |
| D-CF01-6 | El alta de cliente no se reenvía por una búsqueda negativa | ADR-0009 VD-06; 07 §5.2 |
| D-CF02-1 | **Titularidad cercada:** arrendamiento medido con el reloj del almacén; token monotónico; toda escritura de estado condicional sobre el estado, la versión y el token | ADR-0005 v3 §4.1; INV-T4 |
| D-CF02-2 | **Permiso de envío único por tramo** durante toda la vida de la operación; sin nuevos intentos automáticos, ni siquiera `NOT_SENT` | ADR-0005 v3 §4; INV-T3; ADR-0006 IC-14 |
| D-CF02-3 | Envío **solo tras la confirmación durable** del permiso, y dentro de `MAX_DESFASE_ENVIO` (reducción de ventana, no garantía) | ADR-0005 v3 §4.2 |
| D-CF02-4 | **Validez de la intención:** sin permiso de envío después de `PLAZO_VALIDEZ_INTENCION` (aborda R2-N03) | INV-T10 |
| D-CF02-5 | **Evidencia huérfana** de solo anexión para las respuestas recibidas por ejecutores cercados | ADR-0005 v3 §4.1 |
| D-CF02-6 | Separación explícita entre **garantías internas** (G-INT-1 a 3) y **externas** (G-EXT-1 condicionada; G-EXT-2 y G-EXT-3 **no garantizadas**) | ADR-0005 v3 §4.3 |
| D-SL | **La seguridad prevalece sobre la vivacidad**; la vivacidad solo se garantiza hasta la llegada del caso a un responsable humano | ADR-0005 v3 §8 |
| D-ADIC | IC-15: como máximo una operación equivalente no terminal por cliente, de forma atómica (aborda R2-N04, necesario para que D-CF01-5 sea efectivo). El estado global como única máquina autoritativa (aborda R2-N05) | ADR-0006 v3; ADR-0005 v3 §1 |

## 3. Alternativas descartadas

| Alternativa | Por qué se descarta |
|---|---|
| Cierre automático como «sin efecto» tras un plazo sin coincidencias | Es exactamente R2-N01: la ausencia de evidencia no es evidencia de ausencia |
| Estado «sin efecto presunto» que se confirma automáticamente con la conciliación de segunda línea | Sigue basándose en evidencia negativa. Un cierre sin prueba requiere autoridad humana (`CLOSED_BY_EXCEPTION`) |
| Arrendamiento sin token de cercado | Un titular vencido podría registrar transiciones |
| Token de cercado verificado en el almacén con nuevos intentos de envío permitidos | **El token local no impide el envío tardío** (sección 7, I-2). Con nuevos intentos habría dos solicitudes |
| Nuevos intentos `NOT_SENT` dentro del plazo (versión 2) | Su seguridad depende por completo de que la biblioteca cliente informe correctamente que «ningún byte salió», y abre una segunda solicitud si se equivoca. Se cambia vivacidad por seguridad |
| Proxy de cercado junto al Core | Reduce la ventana pero no la elimina, e introduce un punto central (tensión con ADR-0001). Se reevaluará si el Core no ofrece las capacidades de DD-22 |
| Servicio de bloqueo distribuido | Misma carrera con el Core e introducción de tecnología sin necesidad demostrada |
| Una operación crítica en curso por cuenta | Degrada la experiencia y el rendimiento; IC-15 cubre el caso de mayor riesgo (las operaciones equivalentes) |

**Alternativas condicionadas** (DD-22, Q-17), viables solo si el Core las ofrece:

- **Cierre por referencia:** convierte la no ejecución en demostrable.
- **Expiración de solicitudes en el Core:** neutraliza los envíos tardíos y habilita EV-NOEXEC (c).
- **Retención y captura en dos fases:** anular la retención es EV-NOEXEC.

| Opción condicionada | Seguridad | Disponibilidad | Complejidad | Recuperación |
|---|---|---|---|---|
| Cierre por referencia | Alta | Alta (reduce los casos manuales) | Media | Determinista |
| Expiración de solicitudes en el Core | Alta para los envíos tardíos | Alta | Baja-media | Determinista tras el vencimiento |
| Retención y captura | Alta | Media (dos invocaciones) | Alta | Determinista |
| Línea base seleccionada (sin capacidades del Core) | Alta | **Baja ante la incertidumbre** (casos manuales) | Media | Manual |

## 4. Invariantes

Los invariantes INV-T1 a INV-T11 están en [ADR-0005 v3 §6](../adr/ADR-0005-maquina-de-estados-transaccional.md#6-invariantes). Los que introduce o modifica esta remediación son:

| ID | Resumen |
|---|---|
| INV-T1 (modificado) | Terminal sin obligación, salvo `CLOSED_BY_EXCEPTION` con la incertidumbre declarada |
| INV-T2 (modificado) | Las conclusiones negativas exigen EV-REJ o EV-NOEXEC; **nunca EV-INC** |
| INV-T3 (modificado) | Un único permiso de envío por tramo |
| INV-T4 (modificado) | Escrituras cercadas por estado, versión y token |
| INV-T5 (modificado) | El nuevo titular nunca envía |
| INV-T9 (nuevo) | Ninguna evidencia negativa cierra ni habilita un reenvío o un segundo reverso |
| INV-T10 (nuevo) | Sin permiso de envío después del vencimiento de la intención |
| INV-T11 (nuevo) | Un efecto tardío no reabre el estado; abre un caso manual vinculado |

## 5. Protocolo de ejecución

Ver [ADR-0005 v3 §4.2](../adr/ADR-0005-maquina-de-estados-transaccional.md#42-secuencia-de-ejecución-de-un-tramo). En resumen:

1. Adquirir la titularidad.
2. Obtener cupo.
3. Conceder el permiso de envío único (cercado; con intención vigente).
4. Esperar la confirmación durable.
5. Verificar el desfase.
6. Enviar una vez, con la referencia de correlación.
7. Registrar el resultado de forma cercada o, si la titularidad se perdió, como evidencia huérfana.

## 6. Protocolo de recuperación

| Caso | Estado durable | Evidencia disponible | Acciones permitidas | Acciones prohibidas | Condición de resolución | Riesgo residual |
|---|---|---|---|---|---|---|
| **R1. Caída antes del envío** (antes del permiso) | `RECEIVED` / `VALIDATING` | Ningún permiso concedido | Retomar con la intención vigente y continuar el protocolo; si la intención venció, `FAILED_NO_EFFECT` | Conceder el permiso con la intención vencida | Avance normal o vencimiento | Ninguno de integridad |
| **R1b. Caída tras el permiso y antes de escribir** | `CORE_IN_PROGRESS` | Permiso concedido; **no se sabe si se envió** | Pasar a `CORE_UNKNOWN`; conciliar | Enviar; cerrar como sin efecto | EV-POS (si en realidad se envió), EV-NOEXEC (requiere DD-22), o escalamiento y cierre excepcional | **Vivacidad:** sin DD-22, el caso es manual (RR-17) |
| **R2. Caída durante el envío** | `CORE_IN_PROGRESS` | Ninguna concluyente | Igual que R1b | Igual que R1b | Igual que R1b | Igual que R1b |
| **R3. Respuesta perdida** | `CORE_IN_PROGRESS` → `CORE_UNKNOWN` | Ninguna, o evidencia huérfana si la respuesta llegó a otro ejecutor | Conciliar; consumir la evidencia huérfana | Reenviar; cerrar por evidencia negativa | EV-POS, EV-REJ o EV-NOEXEC; si no, escalamiento | RR-04 (EV-POS-CORR), RR-17 |
| **R4. Caída tras la confirmación y antes de persistirla** | `CORE_IN_PROGRESS` | La respuesta se perdió con el proceso | Igual que R3; la conciliación encuentra la operación (EV-POS por referencia o EV-POS-CORR) | Reenviar | EV-POS | Bajo con referencia de correlación; RR-04 sin ella |
| **R5. Ejecutor zombi** | `CORE_UNKNOWN` (token N+1) | Posible envío tardío del token N | El nuevo titular concilia; la respuesta tardía va a la evidencia huérfana | El nuevo titular envía; el zombi registra estado | EV-POS o EV-REJ de la evidencia huérfana, o conciliación | RR-18 (ejecución tardía única); depende de S19 y S20 |
| **R6. Arrendamiento vencido** sin zombi | Cualquiera no terminal | Ninguna nueva | Adquirir la titularidad (token + 1) y aplicar R1, R1b o R3 según el estado | Enviar si el permiso ya se concedió | Según el estado | Ninguno adicional |
| **R7. Conciliación inconclusa** | `CORE_UNKNOWN` → `REQUIRES_RECONCILIATION` | EV-INC | Reintentar las consultas (lecturas, clase CL-2); escalar; resolver manualmente con evidencia; cierre excepcional | Cerrar con EV-INC; reenviar; liberar operaciones equivalentes | Evidencia de INV-T2 o `CLOSED_BY_EXCEPTION` | RR-17; vivacidad no garantizada |
| **R8. Reverso con resultado desconocido** | `REVERSAL_UNKNOWN` | EV-INC sobre el reverso | Conciliar; escalar a `REQUIRES_RECONCILIATION` | **Segundo reverso automático**; cerrar como `REVERSED` sin EV-POS | EV-POS del reverso → `REVERSED`; si no, manual. Un nuevo reverso solo es posible como operación explícita autorizada | RR-16; vivacidad manual |

## 7. Verificación adversarial: tablas de intercalación

Convenciones:

- **E1, E2** son ejecutores; **A** es el almacén durable; **C** es el Core; **K** es el conciliador.
- «Preservado (diseño)» significa que el invariante se cumple por las reglas documentadas.
- «Preservado bajo el supuesto X» significa que depende de un supuesto externo.
- «Liveness» indica una pérdida de vivacidad sin pérdida de seguridad.

### I-1. Dos ejecutores compitiendo por la misma operación

| Paso | Evento | Estado en A |
|---|---|---|
| 1 | E1 y E2 leen `VALIDATING` (token 3) | `VALIDATING`, token 3 |
| 2 | E1 adquiere la titularidad: CAS token 3 → 4 | Titular E1, token 4 |
| 3 | E2 intenta adquirirla: CAS token 3 → 4 **falla** | Sin cambio |
| 4 | E1 concede el permiso (cercado, token 4) | `CORE_IN_PROGRESS` |

- **Invariante:** INV-T3 e INV-T4. **Preservado (diseño)**: la atomicidad del CAS en A permite un solo ganador.
- **Supuesto indispensable:** atomicidad del almacén.
- **Límite:** ninguno adicional.

### I-2. Ejecutor antiguo con arrendamiento vencido (carrera crítica)

| Paso | Evento | Estado en A |
|---|---|---|
| 1 | E1 concede el permiso (token 7), confirmado durablemente | `CORE_IN_PROGRESS`, token 7 |
| 2 | E1 verifica `MAX_DESFASE_ENVIO` (correcto) y se **pausa** | — |
| 3 | Vence el arrendamiento de E1 | — |
| 4 | E2 adquiere la titularidad (token 8) y ve `CORE_IN_PROGRESS` → `CORE_UNKNOWN` | `CORE_UNKNOWN`, token 8 |
| 5 | E1 reanuda y **escribe la solicitud** en el socket | — |
| 6 | C ejecuta la **única** solicitud | — |
| 7 | E1 recibe el éxito; su CAS con token 7 **falla** → evidencia huérfana | Evidencia huérfana: EV-POS |
| 8 | K consume la evidencia huérfana → `COMPLETED` | `COMPLETED`, token 8 |

- **Invariante:** INV-T3 (un único envío desde la plataforma). **Preservado (diseño) bajo el supuesto S19.** E2 no puede conceder un segundo permiso.
- **El cercado local NO impidió el envío del paso 5**, y no puede impedirlo: entre los pasos 2 y 5 no existe ningún punto de control. Lo que convierte el envío tardío en un hecho inocuo para la integridad es el **permiso único**, no el token.
- **Límite:** G-EXT-2 no está garantizada (el Core ejecuta tarde). Si el paso 6 ocurre después de un `CLOSED_BY_EXCEPTION`, se abre un caso de efecto tardío (I-6).

### I-3. Nuevo ejecutor intentando la recuperación

| Paso | Evento | Estado |
|---|---|---|
| 1 | E1 cae tras conceder el permiso (paso 1 de I-2) | `CORE_IN_PROGRESS`, token 7 |
| 2 | E2 adquiere la titularidad (token 8) | `CORE_IN_PROGRESS`, token 8 |
| 3 | E2 **no envía**: CAS → `CORE_UNKNOWN` | `CORE_UNKNOWN` |
| 4 | K concilia | Según la evidencia |

- **Invariante:** INV-T5. **Preservado (diseño).**
- **Límite:** si E1 nunca envió, la no ejecución **no es demostrable** sin DD-22 (vivacidad, RR-17).

### I-4. Respuesta tardía del Core

| Paso | Evento | Estado |
|---|---|---|
| 1 | E1 envía; vence el timeout de lectura → `CORE_UNKNOWN` (E1 sigue siendo titular) | `CORE_UNKNOWN` |
| 2 | Llega la respuesta tardía. Si el canal sigue abierto (por ejemplo, una notificación asíncrona del Core), se registra; con un socket cerrado, se pierde | Evidencia huérfana o nada |
| 3 | K resuelve con EV-POS o EV-REJ si se registró; si no, mediante consulta | Resolución o escalamiento |

- **Invariante:** INV-T3 (sin reenvío) e INV-T9. **Preservado (diseño).**
- **Supuesto:** para aprovechar las respuestas tardías debe existir un canal que las entregue. Si no existe, el caso se resuelve por consulta.

### I-5. Conciliación que inicialmente no encuentra la operación

| Paso | Evento | Estado |
|---|---|---|
| 1 | K consulta el Core: sin coincidencias (EV-INC) | `CORE_UNKNOWN` |
| 2 | Vence `T_ASENTAMIENTO_CORE`: **sin efecto en el estado** (versión 2: aquí se cerraba) | `CORE_UNKNOWN` |
| 3 | Vence `T_RESOLUCION_UNKNOWN` → escalamiento | `REQUIRES_RECONCILIATION` |
| 4 | El cliente intenta una operación equivalente → **rechazada** (IC-15) | Sin cambio |

- **Invariante:** INV-T2 e INV-T9. **Preservado (diseño).**
- **Límite:** vivacidad (el cliente queda bloqueado hasta la resolución).

### I-6. Operación que aparece posteriormente en el Core

| Paso | Evento | Estado |
|---|---|---|
| 6a | Aparece **antes** de cualquier cierre | `REQUIRES_RECONCILIATION` → `COMPLETED` con EV-POS |
| 6b | Aparece **después** de un `CLOSED_BY_EXCEPTION` | Se abre un caso de efecto tardío vinculado (INV-T11); manual, sin corrección automática |

- **Invariante:** INV-T1 e INV-T11. **6a: preservado (diseño).** **6b: preservado en el modelo** (la obligación queda registrada y asignada).
- **Límite:** si el cliente, tras el cierre excepcional, inició una nueva intención equivalente con aviso, **puede haber un doble efecto financiero que el diseño no puede prevenir**, solo detectar y gestionar. Este es el **límite de garantía declarado** del cierre excepcional.

### I-7. Reverso cuyo resultado es desconocido

| Paso | Evento | Estado |
|---|---|---|
| 1 | Permiso de envío del reverso concedido; timeout | `REVERSAL_UNKNOWN` |
| 2 | K no encuentra el reverso (EV-INC) | `REVERSAL_UNKNOWN` |
| 3 | Escalamiento | `REQUIRES_RECONCILIATION` |
| 4 | **No hay un segundo reverso automático**; un nuevo reverso sería una operación explícita y autorizada | — |

- **Invariante:** INV-T3 e INV-T9 (sin doble reverso). **Preservado (diseño).**
- **Límite:** vivacidad; RR-16.

### I-8. Caída inmediatamente antes y después del envío

| Variante | Estado tras la recuperación | Resultado |
|---|---|---|
| 8a: antes de conceder el permiso | `VALIDATING` | Se retoma si la intención sigue vigente; si no, `FAILED_NO_EFFECT`. **Preservado (diseño)** |
| 8b: tras conceder el permiso y antes de escribir | `CORE_UNKNOWN` | No se envía y no se puede demostrar la no ejecución → manual. **Seguridad preservada; vivacidad perdida** sin DD-22 |
| 8c: inmediatamente después de escribir | `CORE_UNKNOWN` | La conciliación encuentra la operación → `COMPLETED`. **Preservado (diseño)**; la exactitud depende de la referencia de correlación (RR-04 si es por correlación) |

### I-9. Clasificación `NOT_SENT` incorrecta (supuesto de implementación)

| Paso | Evento | Estado |
|---|---|---|
| 1 | La biblioteca informa «no enviado» pero la solicitud salió | `FAILED_NO_EFFECT` (con evidencia `NOT_SENT` errónea) |
| 2 | El Core ejecuta | — |
| 3 | La vigilancia de efecto tardío lo detecta → caso vinculado | Caso de efecto tardío |

- **Invariante:** INV-T2. **Depende de un supuesto de implementación** (CP-02): no está demostrado.
- **Límite:** si el cliente inició una nueva intención tras `FAILED_NO_EFFECT`, existe riesgo de duplicado. Por eso CP-02 es **obligatoria antes de producción**.

### Resumen de la verificación

| Caso | Seguridad | Vivacidad | Dependencias |
|---|---|---|---|
| I-1 | Preservada (diseño) | Preservada | Atomicidad del almacén |
| I-2 | Preservada (un único envío) | Preservada si la respuesta llega a la evidencia huérfana o a la conciliación | S19, S20; G-EXT-2 no garantizada |
| I-3 | Preservada | **Condicionada** a DD-22 | — |
| I-4 | Preservada | Preservada | Canal de respuestas tardías (opcional) |
| I-5 | Preservada | **Manual** | — |
| I-6 | Preservada en el modelo; doble efecto posible tras el cierre excepcional | Manual | Límite declarado |
| I-7 | Preservada | **Manual** | — |
| I-8 | Preservada | 8b: **condicionada** a DD-22 | — |
| I-9 | **No demostrada** | — | CP-02 |

## 8. Matriz de fallos (resumen)

| Falla | Antes (`93c6207`) | Después |
|---|---|---|
| Conciliación sin coincidencias | `FAILED_NO_EFFECT` (terminal) → nueva intención habilitada | Permanece abierta; escalamiento; operaciones equivalentes bloqueadas |
| Reverso con resultado desconocido y búsqueda vacía | Nuevo reverso (riesgo de doble crédito) | Sin segundo reverso automático; manual |
| Alta de cliente desconocida y búsqueda vacía | Reenvío del alta (riesgo de cliente duplicado) | `MANUAL_REVIEW` salvo EV-NOEXEC |
| Ejecutor zombi tras la toma | Podía enviar, incluso varias veces con nuevos intentos `NOT_SENT` | Como máximo el único envío ya autorizado |
| `NOT_SENT` | Nuevo intento dentro del plazo | Terminal sin efecto; nueva intención del cliente |
| Operación previa al envío retomada tarde | Sin regla | Sin permiso tras el vencimiento de la intención |
| Operaciones equivalentes con claves distintas en paralelo | Ambas aceptadas (bloqueo no atómico) | Rechazo atómico (IC-15) |

## 9. Riesgos residuales

| Riesgo | Nivel | Comentario |
|---|---|---|
| RR-04 Conciliación sin idempotencia nativa | Medio | Pasa de la duplicación a la vivacidad; persiste el riesgo de una confirmación errónea por EV-POS-CORR |
| **RR-17** Operaciones sin resolución concluyente (nuevo) | **Medio-alto** | **Costo directo de priorizar la seguridad.** Depende de Q-17 y DD-22 |
| **RR-18** Ejecución tardía única de un zombi (nuevo) | Bajo-medio | Sin duplicación desde la plataforma |
| RR-08 Complejidad | Medio | La titularidad cercada agrega complejidad; se compensa con un estado global único |
| Supuestos externos S19 y S20 (nuevos) | Alto si son falsos | Sin ellos, G-EXT-1 no se sostiene |
| Doble efecto tras `CLOSED_BY_EXCEPTION` | Declarado | Detectable, no prevenible (I-6b) |

## 10. Cambios en los ADR y en los documentos

| Archivo | Cambio |
|---|---|
| [ADR-0005](../adr/ADR-0005-maquina-de-estados-transaccional.md) | **Versión 3:** clases de evidencia; lista de lo que no prueba la no ejecución; protocolo de titularidad y permiso único; garantías internas frente a externas; carrera crítica; `CLOSED_BY_EXCEPTION`; efecto tardío; seguridad frente a vivacidad; INV-T1 a INV-T11; estado global autoritativo |
| [ADR-0006](../adr/ADR-0006-idempotencia-y-conciliacion.md) | **Versión 3:** IC-12 (liberación solo con evidencia determinista o cierre excepcional con acción explícita); IC-14 (permiso único); **IC-15** (unicidad atómica de equivalentes no terminales); §4 (la ausencia de coincidencias no resuelve) |
| [ADR-0009](../adr/ADR-0009-core-autoritativo.md) | VD-06: sin reenvío del alta por una búsqueda negativa (hallazgo adicional) |
| [07](../07-maquina-de-estados-transaccional.md) | Transiciones eliminadas o modificadas; titularidad (§3.4); cierre excepcional y efecto tardío (§4.1); alta de cliente |
| [08](../08-idempotencia-y-conciliacion.md) | Resultados de la conciliación por clase de evidencia; garantías |
| [04](../04-escenarios-de-calidad.md) | QA5 revisado; **QA12** (zombi) y **QA13** (evidencia negativa) nuevos; parámetros |
| [02](../02-supuestos.md) | **S19, S20** (supuestos externos de G-EXT-1 y G-EXT-3) |
| [10](../10-decisiones-diferidas.md) | **DD-22** (capacidades del Core para evidencia determinista) |
| [11](../11-riesgos-residuales.md) | RR-04 actualizado; **RR-17, RR-18** |
| [15](../15-preguntas-abiertas-fase-2.md) | **Q-17** |
| [09](../09-trazabilidad-de-requisitos.md) §7 | Filas R2-N01, R2-N02, R2-N04 |
| [00-estandares](../00-estandares-de-ingenieria.md) | Pruebas de la máquina de estados con intercalaciones |
| `README.md` | Estado de ARG-01-R2 y de la Fase 3 |

**No modificados:** ARG-01, el plan de remediación, las preguntas, el informe de remediación y ARG-01-R2 (evidencia histórica). Tampoco ADR-0001, 0002, 0003, 0004, 0007, 0008, 0010 ni 0011, ni el modelo C4.

## 11. Estado de las condiciones y de los hallazgos

| Condición o hallazgo | Estado propuesto por el autor |
|---|---|
| CF-01 / R2-N01 | **Remediado en diseño — pendiente de verificación** |
| CF-02 / R2-N02 | **Remediado en diseño — pendiente de verificación** |
| R2-N03 (estados previos al envío) | Abordado (INV-T10); pendiente de verificación. La relación con el timeout del Gateway (M04) sigue abierta |
| R2-N04 (bloqueo de equivalentes) | Abordado (IC-15); pendiente de verificación (era CF-05) |
| R2-N05 (modelo dual) | Abordado (estado global autoritativo); pendiente de verificación |
| R2-N10 (cierre excepcional) | Abordado (`CLOSED_BY_EXCEPTION`); pendiente de verificación |
| Hallazgo adicional (reenvío del alta por una búsqueda negativa) | Abordado (VD-06) |
| CF-03, CF-06, CF-07, CF-08, CF-09; R2-N06 a N09, N11, N12 | **Sin cambios: abiertos** |

## 12. Evidencia para la verificación posterior

El verificador independiente debería:

1. Confirmar que **ninguna** transición de 07 lleva a `FAILED_NO_EFFECT`, `REJECTED` o a un nuevo permiso de envío con EV-INC (buscar «coincidencia», «demuestra» y «nuevo intento» en 07, 08, ADR-0005, ADR-0006 y ADR-0009).
2. Recorrer las intercalaciones I-1 a I-9 y buscar **otra** intercalación que produzca dos solicitudes originadas en la plataforma para un mismo tramo.
3. Evaluar si `CLOSED_BY_EXCEPTION` y su límite (I-6b) son aceptables para la institución.
4. Evaluar si la pérdida de vivacidad (RR-17) es aceptable mientras no se responda Q-17, y si exige reforzar la capacidad operativa antes de producción.
5. Confirmar que S19 y S20 están registrados como supuestos y que no se presentan como hechos.
6. Verificar la coherencia con ADR-0011 (el cupo se obtiene **antes** del permiso de envío) y con QA9, QA12 y QA13.

**Pruebas obligatorias antes de producción** (reiteran CP-01 a CP-03 de ARG-01-R2, ampliadas):

- intercalaciones I-1 a I-9 con pausas de proceso inyectadas;
- verificación de la clasificación `NOT_SENT` por biblioteca (CP-02);
- auditoría de que ninguna transición quedó registrada con un token vencido;
- conteo de solicitudes por tramo en el lado del Core en un entorno de prueba (para validar S19).

---

> **CF-01 y CF-02 remediados en diseño, pendientes de verificación independiente. No se ha iniciado C4 Level 2.**
