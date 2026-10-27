# ARG-01 — Informe de remediación (Fase 2.1)

| Campo | Valor |
|---|---|
| Revisión de origen | [ARG-01 — Revisión arquitectónica](ARG-01-revision-arquitectonica.md) (conservada sin cambios como evidencia histórica) |
| Veredicto de origen | **NO-GO**: 1 CRITICAL, 5 HIGH, 9 MEDIUM, 5 LOW; puntuación 7,0 / 10 |
| Línea base | Commit `ef13531` |
| Fecha | 2026-10-27 |
| Autor | Arquitecto de la solución |
| Estado | **REMEDIACIÓN COMPLETADA — PENDIENTE DE REVISIÓN ARG-01-R2** |

> Este informe **no declara aprobado** ARG-01. Los hallazgos corregidos quedan como «Resuelto — pendiente de verificación». Solo el comité los cierra en ARG-01-R2.

---

## 1. Resumen

| Severidad | Total | Resueltos (pendiente de verificación) | Resueltos en diseño, con dependencia externa | Parciales | Abiertos (planificados) |
|---|---|---|---|---|---|
| CRITICAL | 1 | 1 (C01) | — | — | — |
| HIGH | 5 | 3 (H01, H02, H04) | 2 (H03, H05) | — | — |
| MEDIUM | 9 | 3 (M01, M02, M07) | — | 1 (M06) | 5 (M03, M04, M05, M08, M09) |
| LOW | 5 | 1 (L05) | — | — | 4 (L01, L02, L03, L04) |

**Dependencias externas que se mantienen:**

- **H03:** el rol, el plazo, el escalamiento y los cuatro ojos están definidos, pero falta que la institución confirme el área y el medio (Q-08, DD-18).
- **H05:** el requisito de autorización de la vinculación está aprobado en ADR-0009, pero el mecanismo concreto de la prueba de control corresponde a la fase de seguridad (DD-21).

Ninguno de los dos se cierra en falso.

## 2. Correcciones de los hallazgos CRITICAL y HIGH

### ARG01-C01 — Identidad lógica de la operación ante los reintentos del cliente

**Corrección:** se enmendó [ADR-0006](../adr/ADR-0006-idempotencia-y-conciliacion.md) (versión 2) y se reescribió [08](../08-idempotencia-y-conciliacion.md).

| Requisito de la remediación | Regla | Evidencia |
|---|---|---|
| 1. Clave estable por intención, provista por el canal | IC-01, IC-02 | ADR-0006 §2 |
| 2. Validación de formato, ámbito, autenticación y reutilización | IC-03, IC-04 | ADR-0006 §2 |
| 3. Vínculo con una huella canónica | IC-05 | ADR-0006 §2 |
| 4. Reserva atómica de la clave y del registro inicial | IC-06 | ADR-0006 §2 |
| 5. Unicidad por ámbito | IC-07 | ADR-0006 §2 |
| 6. Misma clave con contenido distinto: conflicto | IC-09 | ADR-0006 §2 |
| 7. Concurrencia sin doble ejecución | IC-10 | ADR-0006 §2 |
| 8. Los reintentos recuperan el estado existente | IC-08 | ADR-0006 §2 |
| 9. Retención compatible con la ventana de reintento y la vida de la operación | IC-11 | ADR-0006 §2 |
| 10. `UNKNOWN` no libera la clave | IC-12 | ADR-0006 §2 |

Además:

- **Capas de garantía separadas:** G1 deduplicación, G2 idempotencia del Core, G3 correlación, G4 conciliación (ADR-0006 §1).
- **Declaración explícita de lo no garantizable**, incluida la ejecución exactamente una vez de extremo a extremo (ADR-0006 §5; 08 §6).
- **Duplicado semántico** con claves distintas tratado: bloqueo obligatorio durante la incertidumbre, confirmación dentro de una ventana y riesgo residual **RR-13** (ADR-0006 §3).
- **QA3 reformulado (M01)** y **QA9 nuevo** ([04](../04-escenarios-de-calidad.md)).

**Estado:** Resuelto — pendiente de verificación.

### ARG01-H01 — Fallas ambiguas

**Corrección:** [ADR-0005](../adr/ADR-0005-maquina-de-estados-transaccional.md) §2 define una **taxonomía cerrada** de cuatro clases:

- `NOT_SENT`: falla confirmada antes del envío;
- `REJECTED_CONFIRMED`: rechazo definitivo confirmado;
- `UNKNOWN`: resultado desconocido tras un posible envío;
- `CONFIRMED`: confirmación definitiva.

Reglas: «En caso de duda, `UNKNOWN`»; los códigos no mapeados son `UNKNOWN`; la conexión con un intermediario no prueba nada; solo `NOT_SENT` admite un nuevo intento de ejecución.

Se corrigieron [05 §6](../05-frontera-de-integracion-core.md#6-regla-crítica-un-timeout-no-equivale-a-una-falla) (origen de la ambigüedad) y [ADR-0001](../adr/ADR-0001-acl-por-dominio.md) (la ACL mantiene la tabla de clasificación de su contrato).

**Estado:** Resuelto — pendiente de verificación.

### ARG01-H02 — Estados financieros

**Corrección:** ADR-0005 §3 a §5 y [07 §3](../07-maquina-de-estados-transaccional.md#3-máquina-de-estados-de-transferencias).

- **Cinco dimensiones:** solicitud, ejecución en el Core, procesamiento externo, reverso y conciliación.
- **Estados globales inequívocos:** `DEBIT_CONFIRMED`, `PENDING_EXTERNAL_CONFIRMATION`, `EXTERNAL_UNKNOWN`, `REVERSAL_PENDING`, `REVERSAL_UNKNOWN`, `REVERSED`, `FAILED_NO_EFFECT`, etc. Para cada uno se indica si tiene una obligación financiera pendiente.
- **Se elimina `FAILED`.**
- **Invariantes INV-T1 a INV-T8.** En particular:
  - INV-T1: ningún estado terminal con obligación pendiente;
  - INV-T2: sin `REJECTED` ni `FAILED_NO_EFFECT` sin evidencia de ausencia de efecto;
  - INV-T6: no se asume que toda operación sea reversible.
- **Tabla de transiciones válidas y prohibidas.**
- **Semántica de `COMPLETED`** en las interbancarias parametrizada (`EVENTO_CONFIRMACION_EXTERNA`).
- `SPI_PROCESSING` → `PENDING_EXTERNAL_CONFIRMATION`, agnóstico de la topología (S13).
- Las notificaciones (R4) ahora siguen a cada movimiento contable confirmado (INV-T8).

**Estado:** Resuelto — pendiente de verificación. La topología interbancaria sigue abierta (Q-01, DD-16), pero los invariantes no dependen de ella.

### ARG01-H03 — `REQUIRES_RECONCILIATION` sin responsable ni plazo

Este es el quinto HIGH, identificado leyendo el informe original (§6), no a partir del resumen.

**Corrección:** ADR-0005 §6 y 07 §4.

- Rol de conciliación y operaciones, con colas asignadas.
- `T_RESOLUCION_MANUAL` con dos niveles de escalamiento.
- **Cuatro ojos.**
- La resolución no mueve fondos por sí misma.
- Coherencia obligatoria con la evidencia de cada dimensión.
- Comunicación al cliente y monitoreo de antigüedad.
- QA5 mide la asignación y el plazo.

**Estado:** **Resuelto en diseño; pendiente de confirmación institucional** del área y del medio (Q-08, DD-18). Esa confirmación depende de la institución, no del arquitecto, y se declara como condición.

### ARG01-H04 — Presupuesto global del Core

**Corrección:** nuevo [ADR-0011](../adr/ADR-0011-presupuesto-de-capacidad-del-core.md) y [ADR-0001](../adr/ADR-0001-acl-por-dominio.md) complementado con la sección «Aislamiento lógico frente a aislamiento de capacidad». ADR-0011 define:

- un presupuesto global fijado por el propietario del Core;
- cuatro clases de operación, con las escrituras (CL-1) y las consultas de integridad (CL-2) **reservadas**;
- **M1, asignación estática** `cuota / N_MAX_REPLICAS`: obligatoria, sin dependencias en tiempo de ejecución;
- **M2, cuotas dinámicas:** diferida (DD-20);
- control de admisión por clase y presión de retorno hacia la limitación de tasa del borde;
- Bulkheads locales y globales;
- **comportamiento seguro**: si falla la coordinación se vuelve a M1, nunca a «sin límite»;
- el riesgo de que la cuota se convierta en un punto de indisponibilidad (RR-14).

No se elige ninguna tecnología. QA8 se actualizó y se agregó QA10.

**Estado:** Resuelto — pendiente de verificación. Los valores dependen de Q-16.

### ARG01-H05 — Vinculación de identidad

**Corrección:** se enmendó [ADR-0009](../adr/ADR-0009-core-autoritativo.md) (versión 2) y se reescribió [07 §5](../07-maquina-de-estados-transaccional.md#5-máquina-de-estados-de-vinculación-digital).

- **Seis etapas separadas** (E1 a E6).
- **Reglas VD-01 a VD-10.** La coincidencia de identificación **nunca** autoriza la vinculación; un cliente preexistente exige una **prueba de control independiente**; la creación requiere **evidencia**; no hay enumeración; la verificación tiene vigencia; la indisponibilidad del proveedor no es un rechazo.
- **Estados exigidos:** `IDENTITY_VERIFIED`, `CUSTOMER_IDENTIFIED` (con origen), `LINK_PENDING`, `LINK_AUTHORIZED`, `DIGITAL_ACCESS_ACTIVE`.
- **Invariantes** INV-O1 a INV-O6, escenario QA11 y riesgo RR-15.

**Estado:** **Resuelto en diseño; el requisito de autorización queda aprobado.** El mecanismo concreto de la prueba de control se difiere a la fase de seguridad (DD-21), como pedía la remediación. RR-15 se mantiene con riesgo residual medio hasta entonces.

## 3. Triaje de los hallazgos MEDIUM y LOW

| Hallazgo | Severidad confirmada | ¿Resoluble ahora sin ampliar el alcance? | Acción en esta remediación | Responsable conceptual | Criterio de aceptación | Fase objetivo | Estado |
|---|---|---|---|---|---|---|---|
| M01 — QA3 indemostrable | MEDIUM | Sí | QA3 reformulado con medidas (a) a (e) | Arquitecto | QA3 coherente con RR-04 | 2.1 | **Resuelto — pendiente de verificación** |
| M02 — Recuperación, concurrencia, onboarding | MEDIUM | Sí (ligado a H01, H02, H05) | INV-T4, INV-T5; `IDENTITY_VERIFICATION_UNAVAILABLE`; alta separada de la búsqueda | Arquitecto | Reglas en ADR-0005 y ADR-0009 | 2.1 | **Resuelto — pendiente de verificación** |
| M03 — Carrera de Cache-Aside | MEDIUM | No del todo: los TTL requieren negocio y la mitigación se diseña con los contenedores | Ninguna (no afecta la integridad: la caché no decide) | Arquitecto y negocio | `docs/06` con la carrera, el reintento de la invalidación y *read-your-writes*; TTL registrados | Fase 3 | Abierto |
| M04 — Resiliencia y plazos sin ADR | MEDIUM (su efecto sobre los reintentos del cliente queda mitigado por C01) | Parcialmente; requiere decidir el patrón de respuesta (síncrono o asíncrono), que es diseño de API de la Fase 3 | Ninguna | Arquitecto | ADR nuevo con timeout del Gateway ≥ plazo + margen y la respuesta ante el vencimiento | Fase 3 | Abierto |
| M05 — Registro regulatorio | MEDIUM | Requiere el insumo de cumplimiento y del tipo de institución (Q-05) | Ninguna | Arquitecto y cumplimiento | Registro `REG-PV` creado y referenciado | Fase 3 | Abierto |
| M06 — Fraude y prevención de lavado | MEDIUM | Parcialmente | Q-12 elevada a impacto alto | Arquitecto, riesgos y cumplimiento | Candidatos en la vista ampliada o exclusión justificada | Inicio de la Fase 3 | **Parcial** |
| M07 — REL-05 como «Confirmada» | MEDIUM | Sí | Etiqueta y estilo «Ruta por validar»; leyenda; diagramas regenerados | Arquitecto | Estilo distinto en el DSL y en los SVG | 2.1 | **Resuelto — pendiente de verificación** |
| M08 — Acoplamiento del SDK | MEDIUM | Sí, pero no es prioritario frente a la integridad | Ninguna | Arquitecto y equipo de plataforma | ADR-0003 con redespliegues por seguridad, compatibilidad de comunicación y ventana N-1 | Fase 3 | Abierto |
| M09 — Escenarios de seguridad del onboarding | MEDIUM | Parcialmente | QA11 cubre la vinculación; **no** cubre ataques de presentación ni el abuso de costos | Arquitecto y seguridad | Escenarios de ataque biométrico y de abuso de costos | Antes de la fase de seguridad | Abierto |
| L01 — Fronteras organizacional y de despliegue | LOW | Sí | Ninguna (priorización) | Arquitecto | Tabla de cuatro fronteras en `docs/14` | Fase 3 | Abierto |
| L02 — Criterio BFF | LOW | Sí | Ninguna (corresponde a DD-10) | Arquitecto | Criterio en ADR-0002 o DD-10 | Fase 3 | Abierto |
| L03 — Gobierno de los ADR | LOW | Parcialmente | Los ADR enmendados tienen historial de versiones y estado «pendiente de verificación ARG-01-R2». **No** hay campo de aprobador | Gobierno de arquitectura | Campos «Aprobado por» y «Revisión» | Tras ARG-01-R2 | Abierto |
| L04 — Commit fuera de la convención | LOW | Requiere reescribir el historial publicado (decisión del autor) | Ninguna | Autor | Mensaje corregido o excepción documentada | Decisión del autor | Abierto |
| L05 — markdownlint | LOW | Sí | MD060 desactivado (estilo de tabla compacto adoptado); 3 MD036 corregidos en `docs/12`; el informe ARG-01 original se excluye del lint para no modificarlo (`.markdownlint-cli2.jsonc`) | Arquitecto | `markdownlint-cli2` sin errores | 2.1 | **Resuelto — pendiente de verificación** |

## 4. Validación adversarial

> **Todos los resultados son de ANÁLISIS CONCEPTUAL** sobre la documentación. **No se ejecutó ninguna prueba**: no existe software. El resultado indica si el diseño documentado **satisface** el invariante.

### 4.1 Reejecución de F01 a F12

| ID | Invariante de seguridad | Comportamiento esperado (diseño actual) | Evidencia | Riesgo residual | Resultado ARG-01 → ahora |
|---|---|---|---|---|---|
| F01 Core no disponible | Ninguna escritura con efecto incierto se trata como fallida; ninguna lectura degradada sin indicador | Lecturas aprobadas con indicador; escrituras `NOT_SENT` → `FAILED_NO_EFFECT`, sin encolar | ADR-0005 §2; 05 §7; 06 §3 | TTL no definidos (M03) | CUBIERTO → **SATISFECHO** |
| F02 Core confirma, respuesta perdida | Sin una segunda invocación (INV-T3) | `UNKNOWN` → `CORE_UNKNOWN` → conciliación G2/G3 → estado confirmado o `REQUIRES_RECONCILIATION` | ADR-0005 §2–§5; 08 §5 | RR-04 | CUBIERTO → **SATISFECHO** con riesgo residual |
| F03 Cliente reenvía | Exactamente 1 operación por (ámbito, clave) | IC-02 + IC-07 + IC-08: devuelve el estado existente | ADR-0006 §2; QA9 | Canales que incumplen IC-02 (RR-13) | **BRECHA → SATISFECHO** |
| F04 Clave existente con otro contenido | Sin efectos | IC-09: conflicto auditado | ADR-0006 §2 | — | **BRECHA → SATISFECHO** |
| F05 Proveedor biométrico no responde | Indisponibilidad ≠ rechazo | `IDENTITY_VERIFICATION_UNAVAILABLE` con reintento dentro de la vigencia o `EXPIRED` | ADR-0009 VD-09; 07 §5 | Disponibilidad del proveedor | **BRECHA → SATISFECHO** |
| F06 El Core crea el cliente y falla la actualización local | Sin duplicados; sin atribuir como creado un cliente preexistente | INV-T5 → `CORE_CREATION_UNKNOWN`; VD-05 con evidencia o `MANUAL_REVIEW` | ADR-0009 VD-05 y VD-06; 07 §5.2 | Más casos manuales si el Core no ofrece G2 | PARCIAL → **SATISFECHO** con riesgo residual |
| F07 Saldo obsoleto en la caché | Ninguna decisión con datos de caché | Sin cambios: solo visualización; *read-your-writes* sin mecanismo definido | ADR-0004 | Carrera de Cache-Aside (M03) | PARCIAL → **PARCIAL** (M03 abierto) |
| F08 No llega el evento de invalidación | La ventana obsoleta está acotada | TTL de seguridad; bus diferido | ADR-0008; 06 §4 | TTL por definir | PARCIAL → **PARCIAL** (M03 abierto) |
| F09 El sistema externo no responde | Un débito confirmado nunca termina como fallido | `PENDING_EXTERNAL_CONFIRMATION` → `EXTERNAL_UNKNOWN` → conciliación → `COMPLETED` o `REVERSAL_PENDING` o `REQUIRES_RECONCILIATION`; obligación visible | ADR-0005 INV-T1, INV-T6; 07 §3.3 | RR-16; Q-01 | PARCIAL → **SATISFECHO** con dependencia de Q-01 |
| F10 Gateway operativo, servicio caído | Sin reintento de escrituras en el Gateway; recuperación segura | ADR-0002; INV-T5 al reiniciar | ADR-0002; ADR-0005 | Relación Gateway–plazo (M04) | PARCIAL → **PARCIAL** (M04 abierto) |
| F11 Token robado | Sin operación de alto riesgo solo con el token | QA4; mecanismo diferido | QA4; DD-04 | Alto hasta la fase de seguridad | PLANIFICADO → **PLANIFICADO** (legítimo) |
| F12 Concurrencia sobre la misma operación | Sin doble ejecución ni doble transición | IC-10 (reserva única); INV-T4 (transición condicional) | ADR-0006; ADR-0005 | — | **BRECHA → SATISFECHO** |

### 4.2 Pruebas conceptuales adicionales

| ID | Escenario | Invariante de seguridad | Comportamiento esperado | Evidencia | Riesgo residual | Resultado |
|---|---|---|---|---|---|---|
| AT-01 | Dos solicitudes concurrentes con la misma clave | ≤ 1 operación y ≤ 1 invocación | Una reserva atómica tiene éxito; la otra recibe el estado o el conflicto | IC-06, IC-07, IC-10 | — | SATISFECHO (conceptual) |
| AT-02 | Misma clave con otro contenido | Sin efectos; la operación original no cambia | Conflicto auditado | IC-05, IC-09 | — | SATISFECHO (conceptual) |
| AT-03 | Dos claves distintas para la misma intención financiera | Sin dos operaciones equivalentes simultáneas durante la incertidumbre; sin duplicado silencioso dentro de la ventana | Bloqueo si la primera está en `UNKNOWN`; confirmación explícita dentro de la ventana; fuera de la ventana: se mide | ADR-0006 §3 | **RR-13 (medio):** no se previene fuera de la ventana ni con confirmación del usuario | **PARCIALMENTE SATISFECHO** (limitación declarada, no oculta) |
| AT-04 | Timeout después de un débito confirmado | Sin una segunda invocación del débito; obligación visible | `CORE_UNKNOWN` → la conciliación encuentra el débito → `COMPLETED` o `DEBIT_CONFIRMED` | ADR-0005 §2, §5; 08 §5 | RR-04 | SATISFECHO (conceptual) |
| AT-05 | Débito confirmado y sistema de pagos sin respuesta | Nunca `FAILED` con débito; obligación visible | `PENDING_EXTERNAL_CONFIRMATION` → `EXTERNAL_UNKNOWN` → conciliación; si hay rechazo: `REVERSAL_PENDING` | INV-T1, INV-T6; 07 §3.3 | RR-16, Q-01 | SATISFECHO (conceptual) |
| AT-06 | Falla del propio proceso de conciliación | La operación no se pierde ni se resuelve en falso | Permanece en `*_UNKNOWN`; reprogramación con capacidad CL-2; `T_RESOLUCION_UNKNOWN` garantiza el escalamiento | 08 §5.2; ADR-0011 (CL-2) | Si el Core no está disponible por largo tiempo, la cola de casos manuales crece | SATISFECHO (conceptual) |
| AT-07 | Reinicio del orquestador con una operación `CORE_IN_PROGRESS` o `UNKNOWN` | Sin nueva invocación tras el reinicio | INV-T5: `*_IN_PROGRESS` → `*_UNKNOWN`; desde `*_UNKNOWN` solo consultas | ADR-0005 INV-T3, INV-T5 | — | SATISFECHO (conceptual) |
| AT-08 | Escalado de 2 a 20 réplicas | Concurrencia total ≤ `MAX_CONCURRENCIA_CORE`; CL-1 protegida | M1: cuota por réplica = cuota / `N_MAX_REPLICAS` (20) → con 20 réplicas, total = cuota; con 2, subutilización | ADR-0011 §2; QA10 | RR-14 (dimensionamiento); subutilización con N bajo (DD-20) | SATISFECHO (conceptual), si `N_MAX_REPLICAS` ≥ 20 está configurado. **Si se escala por encima de `N_MAX_REPLICAS` sin recalcular las cuotas, el invariante se rompe**: es una regla de configuración que debe verificarse en la Fase 3 |
| AT-09 | Vinculación de un cliente preexistente sin autorización suficiente | Sin `LINK_AUTHORIZED` sin prueba de control | `CUSTOMER_IDENTIFIED (PRE_EXISTING)` → `LINK_PENDING` → `LINK_REJECTED` si falla la prueba; sin enumeración | ADR-0009 VD-01, VD-03, VD-07; INV-O1; QA11 | **RR-15 (medio)** hasta DD-21 | SATISFECHO en el requisito; mecanismo pendiente (DD-21) |
| AT-10 | Pérdida de eventos de invalidación de caché | La ventana obsoleta está acotada; ninguna decisión con caché | TTL de seguridad; invalidación local para las operaciones propias | ADR-0004, ADR-0008; 06 §4.4 | Carrera de Cache-Aside y TTL indefinidos (M03) | **PARCIALMENTE SATISFECHO** (M03 abierto, MEDIUM, sin impacto en la integridad) |

### 4.3 Puntos que el revisor de ARG-01-R2 debería desafiar

Para no ocultar debilidades, el autor señala lo siguiente:

1. **Complejidad añadida:** el modelo por dimensiones aumenta la superficie de estados (RR-08). El revisor debería evaluar si las cinco dimensiones son necesarias o si alguna puede derivarse.
2. **Fricción para el usuario:** el bloqueo y la confirmación de posibles duplicados afectan la experiencia; sus parámetros son decisiones de negocio pendientes.
3. **Techo de escalado:** M1 impone `N_MAX_REPLICAS`. Un escalado fuera de ese techo sin recalcular las cuotas rompe la garantía (AT-08). La protección depende de la disciplina de configuración.
4. **Dependencia de los factores registrados en el Core** para la prueba de control (Q-15). Si los contactos están desactualizados, la vinculación de clientes existentes puede volverse inviable o insegura.
5. **IC-02 depende del canal:** un cliente defectuoso que genera una clave por intento degrada la garantía G1 al nivel del duplicado semántico (RR-13).

## 5. Matriz de comparación antes / después

| Aspecto | Antes (línea base `ef13531`) | Después (remediación) |
|---|---|---|
| Origen de la clave de idempotencia | Dominio, al aceptar; clave del canal opcional y «asociada» | **Canal, obligatoria y estable por intención**; identificador interno 1:1 |
| Misma clave con otro contenido | Sin definir | Conflicto sin efectos, auditado (IC-09) |
| Concurrencia con la misma clave | Unicidad, sin regla para el perdedor | Reserva atómica única; el perdedor recibe el estado o el conflicto (IC-06, IC-10) |
| Clave durante `UNKNOWN` | Sin definir | No se libera; «en verificación» (IC-12) |
| Duplicado semántico | Inhibición opcional («puede») | Bloqueo **obligatorio** + confirmación + medición (RR-13) |
| Garantía declarada | Implícita; QA3 prometía «0 duplicados» | G1 a G4 explícitas; **sin exactly-once**; QA3 medible |
| Clasificación de fallas | «Circuito abierto o error de conexión» = sin efecto | Taxonomía cerrada de cuatro clases; en caso de duda, `UNKNOWN` |
| Estado `FAILED` | Terminal, «sin efecto o revertido», alcanzable con débito | **Eliminado**: `REJECTED`, `FAILED_NO_EFFECT`, `REVERSED`, con invariantes |
| Débito confirmado + rechazo externo | `SPI_PROCESSING → FAILED` | `REVERSAL_PENDING` → `REVERSED` o `REQUIRES_RECONCILIATION` con obligación |
| Nombre del estado externo | `SPI_PROCESSING` (presupone el SPI) | `PENDING_EXTERNAL_CONFIRMATION` |
| `REQUIRES_RECONCILIATION` | Sin responsable ni plazo | Rol, `T_RESOLUCION_MANUAL`, escalamiento, cuatro ojos, cliente informado |
| Recuperación tras reinicio | «Puede retomarse o conciliarse» | INV-T5: `*_IN_PROGRESS` → `*_UNKNOWN` |
| Concurrencia de transiciones | Solo en el perfil Java/Quarkus | INV-T4, independiente de la tecnología |
| Capacidad del Core | Límites por ACL e instancia | Presupuesto global, cuatro clases, M1 obligatorio, comportamiento seguro (ADR-0011) |
| Vinculación de clientes existentes | Conciliación por identificación → `CORE_CREATED` → `ACTIVE` | Seis etapas; prueba de control independiente obligatoria; evidencia de creación; no enumeración |
| Indisponibilidad del proveedor de identidad | Sin distinguir del rechazo | `IDENTITY_VERIFICATION_UNAVAILABLE` |
| Notificación de movimientos | Solo en estados terminales | Por cada movimiento contable confirmado (INV-T8) |
| REL-05 en el contexto | «Confirmada», línea continua | «Ruta por validar», estilo propio, leyenda |
| Escenarios de calidad | QA1 a QA8 | QA1 a QA11 (QA3, QA5 y QA8 revisados) |
| ADR | 10 | 11 (ADR-0011 nuevo; ADR-0005, 0006 y 0009 enmendados; ADR-0001 complementado) |
| Riesgos | RR-01 a RR-12 | RR-01 a RR-16 |
| Decisiones diferidas | DD-01 a DD-19 | DD-01 a DD-21 |
| markdownlint | 825 avisos | 0 avisos (31 archivos; el informe original se excluye) |

## 6. Archivos modificados

| Archivo | Cambio |
|---|---|
| `docs/adr/ADR-0006-idempotencia-y-conciliacion.md` | Enmienda (versión 2): contrato IC-01 a IC-14, capas G1 a G4, duplicado semántico, no garantías |
| `docs/adr/ADR-0005-maquina-de-estados-transaccional.md` | Enmienda (versión 2): taxonomía, dimensiones, invariantes, transiciones, resolución operativa |
| `docs/adr/ADR-0009-core-autoritativo.md` | Enmienda (versión 2): seis etapas, VD-01 a VD-10 |
| `docs/adr/ADR-0011-presupuesto-de-capacidad-del-core.md` | **Nuevo** |
| `docs/adr/ADR-0001-acl-por-dominio.md` | Complemento: aislamiento lógico frente a capacidad; responsabilidades de la ACL |
| `docs/07-maquina-de-estados-transaccional.md` | Reescrito (versión 2) |
| `docs/08-idempotencia-y-conciliacion.md` | Reescrito (versión 2) |
| `docs/04-escenarios-de-calidad.md` | QA3, QA5 y QA8 revisados; QA9 a QA11 nuevos; parámetros nuevos |
| `docs/05-frontera-de-integracion-core.md` | Regla de clasificación de fallas; responsabilidades de la ACL; referencia a ADR-0011 |
| `docs/02-supuestos.md` | S13 (estado renombrado); **S18** nuevo |
| `docs/09-trazabilidad-de-requisitos.md` | Sección 7 (trazabilidad de la remediación); ADR-0011 en la trazabilidad inversa |
| `docs/10-decisiones-diferidas.md` | DD-18 actualizada; **DD-20** y **DD-21** nuevas |
| `docs/11-riesgos-residuales.md` | RR-04 actualizado; **RR-13 a RR-16** nuevos |
| `docs/12-contexto-del-sistema.md` | Referencias a los estados; convención de REL-05; 3 MD036 corregidos |
| `docs/13-catalogo-de-integraciones.md` | INT-04 a INT-06 con clase de capacidad y contrato |
| `docs/15-preguntas-abiertas-fase-2.md` | Q-01 y Q-02 ampliadas; Q-12 elevada a impacto alto; **Q-15, Q-16** nuevas |
| `docs/00-estandares-de-ingenieria.md` | Ejemplos de estados y pruebas obligatorias actualizados |
| `c4/workspace.dsl` | REL-05 con la etiqueta y el estilo «Ruta por validar» |
| `diagrams/c4/*` | Regenerados desde el DSL |
| `README.md` | Estado de ARG-01; tabla de ADR; estructura |
| `.markdownlint.json`, `.markdownlint-cli2.jsonc` | MD060 desactivado; exclusión del informe original |
| `docs/reviews/ARG-01-informe-de-remediacion.md` | **Nuevo** (este documento) |

**No modificados:** `docs/reviews/ARG-01-revision-arquitectonica.md`, `ARG-01-plan-de-remediacion.md` y `ARG-01-preguntas-para-el-arquitecto.md` (evidencia histórica), y el PDF del reto.

## 7. Riesgos residuales

| Riesgo | Residual | Comentario |
|---|---|---|
| RR-04 Conciliación sin idempotencia nativa | Medio | Mitigado por el bloqueo obligatorio y medido; depende de Q-02 |
| RR-13 Duplicado semántico (nuevo) | Medio | Limitación declarada |
| RR-14 Cuotas mal dimensionadas (nuevo) | Medio | Depende de Q-16 |
| RR-15 Vinculación de preexistentes (nuevo) | Medio | Hasta DD-21 |
| RR-16 Reverso no posible por la vía automática (nuevo) | Medio | Depende de Q-01 |
| RR-08 Complejidad del estado durable | Bajo-medio, **en aumento** | El modelo por dimensiones agrega estados |

## 8. Hallazgos todavía abiertos

- **Con dependencia externa:**
  - H03: confirmación institucional, Q-08 y DD-18;
  - H05: mecanismo de la prueba de control, DD-21.
- **MEDIUM:** M03, M04, M05, M08, M09 abiertos; M06 parcial.
- **LOW:** L01, L02, L03, L04.

Ninguno de los abiertos compromete la integridad financiera según el análisis del autor, que **debe verificar el comité**.

## 9. Decisiones diferidas nuevas o modificadas

| ID | Decisión | Disparador |
|---|---|---|
| DD-18 | Medio y área de los actores internos (la resolución operativa ya está definida) | Q-08 |
| DD-20 (nueva) | Mecanismo dinámico de cuotas del Core (M2) | Subutilización medida con N bajo |
| DD-21 (nueva) | Mecanismo de prueba de control para la vinculación de preexistentes y restricciones iniciales | Fase de seguridad; Q-15 |

## 10. Validaciones ejecutadas

| Verificación | Resultado | Tipo |
|---|---|---|
| Sintaxis de Structurizr DSL (`structurizr/structurizr validate`) | Sin errores | VALIDADO AUTOMÁTICAMENTE |
| Regeneración de diagramas (PlantUML y SVG) y revisión visual de la vista principal y su leyenda | Correctos. Se detectó y corrigió que Structurizr separa las etiquetas por comas (la primera etiqueta propuesta tenía una coma) | VALIDADO AUTOMÁTICAMENTE + REVISADO MANUALMENTE |
| Enlaces y anclas Markdown (script propio) | 33 archivos, 0 enlaces rotos | VALIDADO AUTOMÁTICAMENTE |
| markdownlint (`markdownlint-cli2` v0.24.0) | 0 avisos en 31 archivos (el informe ARG-01 original se excluye) | VALIDADO AUTOMÁTICAMENTE |
| IDs referenciados frente a definidos (S, DD, RR, Q, QA) y referencias a ADR | Todos definidos | VALIDADO AUTOMÁTICAMENTE |
| Referencias a estados eliminados (`FAILED`, `SPI_PROCESSING`, `CORE_PROCESSING`, `PENDING_CORE`, `CORE_CREATED`) | Solo en el historial o en notas de renombre | VALIDADO AUTOMÁTICAMENTE |
| Escenarios F01–F12 y AT-01 a AT-10 | Sección 4 | **ANÁLISIS CONCEPTUAL** (sin ejecución de pruebas) |
| Coherencia entre ADR, documentos y escenarios | Sección 4 | REVISADO MANUALMENTE |

## 11. Recomendación para el nuevo gate

Se solicita una **revisión independiente ARG-01-R2**, focalizada en:

1. **ARG01-C01 y M01:** verificar el contrato IC-01 a IC-14, las capas G1 a G4 y la reformulación de QA3, con especial atención a AT-03 (duplicado semántico).
2. **ARG01-H01 y H02:** verificar la taxonomía, los invariantes INV-T1 a INV-T8 y la tabla de transiciones prohibidas.
3. **ARG01-H04:** verificar que M1 mantiene el presupuesto para cualquier N ≤ `N_MAX_REPLICAS` y el comportamiento seguro (AT-08).
4. **ARG01-H05:** verificar VD-01 a VD-10 e INV-O1 a INV-O6, y decidir si el diferimiento del mecanismo (DD-21) es aceptable como condición.
5. **ARG01-H03:** decidir si la confirmación institucional pendiente se acepta como condición de la Fase 3.

**Expectativa del autor** (no vinculante; no sustituye el juicio del comité): sin hallazgos CRITICAL abiertos, el comité podría considerar un **CONDITIONAL GO**, con estas condiciones:

- confirmación de Q-08 (H03), Q-16 (presupuesto del Core) y Q-02 (capacidades del Core);
- DD-21 resuelta antes de la fase de seguridad;
- M03, M04, M05, M08 y M09 dentro de la Fase 3.

## 12. Estado final

> **REMEDIACIÓN COMPLETADA — PENDIENTE DE REVISIÓN ARG-01-R2**
