# ARG-01-R2 — Verificación independiente de la remediación

| Campo | Valor |
|---|---|
| Revisión | ARG-01-R2 (verificación de la remediación de ARG-01) |
| Objeto | Remediación del commit `5cc0c26` frente a la línea base `ef13531` |
| Fecha | 2026-10-27 |
| Comité (perfiles simulados) | Principal Solutions Architect; Banking Transaction Architect; Distributed Systems Architect; Security Architect |
| Documentos relacionados | [ARG-01 — Revisión](ARG-01-revision-arquitectonica.md), [Plan de remediación](ARG-01-plan-de-remediacion.md), [Informe de remediación](ARG-01-informe-de-remediacion.md) |
| Veredicto | **CONDITIONAL GO** (sección 1) |

> Esta verificación es adversarial. Las afirmaciones del informe de remediación se trataron como hipótesis por verificar, no como evidencia. Todo el análisis es **conceptual sobre la documentación**: no existe software y **no se ejecutó ninguna prueba**. «Verificado» significa verificado **en el diseño conceptual**; la validación por pruebas de implementación queda como condición previa a la producción (sección 11).

---

## 1. Veredicto

### **CONDITIONAL GO**

La remediación **resuelve el defecto concreto** que motivó el NO-GO de ARG-01. Una misma intención del cliente, reenviada con la misma clave, secuencial o concurrentemente, **no puede producir dos operaciones ni dos invocaciones** desde la plataforma (contrato IC-01 a IC-14). Ninguna de las seis correcciones introduce una regresión de integridad, y la dirección de las decisiones es correcta.

Sin embargo, la verificación adversarial encontró que **algunas garantías declaradas son más fuertes que el diseño que las sostiene**:

- **R2-N01 (HIGH):** la ausencia de efecto inferida por evidencia **negativa y probabilística** se trata como un estado **terminal**. Eso contradice INV-T1 e INV-T2 y habilita, junto con IC-12, una nueva intención que podría duplicar un débito no detectado.
- **R2-N02 (HIGH):** no existe **propiedad exclusiva ni token de cercado** (*fencing*) de la ejecución. Un trabajador «zombi» puede invocar al Core después de que la recuperación haya movido la operación a `*_UNKNOWN`.
- **R2-N06 (HIGH):** la regla M1 del presupuesto del Core **no se sostiene durante los despliegues** (rolling y blue/green), con réplicas en terminación ni con varios servicios compartiendo una clase.

Ninguno es CRITICAL:

- **N01 y N02** requieren condiciones compuestas: ausencia de G2, asentamiento tardío del Core o un trabajador zombi, **y** una nueva intención del usuario. Además, se corrigen con reglas adicionales en ADR-0005 que no cambian la dirección de la decisión.
- **N06** afecta la disponibilidad del Core, no la integridad.

Por esa razón el comité **no** emite NO-GO. **No** considera cerrados los hallazgos originales; quedan **condicionalmente aceptados**, con condiciones bloqueantes antes de cerrar la Fase 3 y antes de producción.

### Estado de los hallazgos

| Hallazgo | Estado ARG-01-R2 |
|---|---|
| ARG01-C01 | **CONDITIONALLY ACCEPTED** |
| ARG01-H01 | **CONDITIONALLY ACCEPTED** |
| ARG01-H02 | **CONDITIONALLY ACCEPTED** |
| ARG01-H03 | **CONDITIONALLY ACCEPTED** (dependencia institucional) |
| ARG01-H04 | **CONDITIONALLY ACCEPTED** (con regresión potencial de disponibilidad, R2-N07) |
| ARG01-H05 | **CONDITIONALLY ACCEPTED** |

**Ningún hallazgo queda VERIFIED CLOSED.** Todos dependen de condiciones de diseño (R2-Nxx) o de validación futura por pruebas.

**Nuevos hallazgos:** 3 HIGH, 6 MEDIUM, 3 LOW (sección 12).

## 2. Evidencia examinada

| Evidencia | Lectura |
|---|---|
| [ARG-01-revision-arquitectonica.md](ARG-01-revision-arquitectonica.md) | Completa: hallazgos originales y criterios de aceptación |
| [ARG-01-plan-de-remediacion.md](ARG-01-plan-de-remediacion.md) | Completa: correcciones y criterios esperados |
| [ARG-01-informe-de-remediacion.md](ARG-01-informe-de-remediacion.md) | Completa; contrastada con los documentos y no aceptada como prueba |
| [ADR-0001](../adr/ADR-0001-acl-por-dominio.md), [ADR-0005](../adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](../adr/ADR-0006-idempotencia-y-conciliacion.md), [ADR-0009](../adr/ADR-0009-core-autoritativo.md), [ADR-0011](../adr/ADR-0011-presupuesto-de-capacidad-del-core.md) | Completa |
| [07](../07-maquina-de-estados-transaccional.md), [08](../08-idempotencia-y-conciliacion.md) | Completa, incluidas las tablas de transiciones |
| [04](../04-escenarios-de-calidad.md) (QA3, QA5, QA8 a QA11), [10](../10-decisiones-diferidas.md), [11](../11-riesgos-residuales.md), [09](../09-trazabilidad-de-requisitos.md) §7 | Completa |
| `git diff ef13531 5cc0c26` | 30 archivos (3 nuevos, 27 modificados), +1235 / −297 |
| Búsquedas dirigidas | Términos de propiedad o *fencing*, expiración, despliegue, autorización de reversos y antigüedad de factores; resultados en la sección 12 |

## 3. Comparación de los dos commits

| Área | `ef13531` | `5cc0c26` | Valoración del comité |
|---|---|---|---|
| Identidad de la intención | Clave generada en el dominio; clave del canal opcional | Clave del canal obligatoria y estable; ámbito por identidad autenticada; huella; reserva atómica | **Mejora sustantiva, verificada** |
| Conflicto clave/contenido | No definido | Conflicto sin efectos | **Verificada** |
| Clasificación de fallas | «Error de conexión» = sin efecto | Taxonomía de 4 clases; en caso de duda, `UNKNOWN` | **Mejora sustantiva, verificada** (validación de implementación pendiente) |
| Estados financieros | `FAILED` alcanzable con débito | Dimensiones, estados explícitos, invariantes | **Mejora sustantiva**; contradicción residual con la evidencia negativa (R2-N01) |
| Recuperación y concurrencia | No definidas | INV-T4 (CAS), INV-T5 | **Mejora**, pero **incompleta**: sin propiedad ni *fencing* (R2-N02) y sin recuperación de los estados previos al envío (R2-N03) |
| Conciliación manual | Sin dueño | Rol, plazo, escalamiento, cuatro ojos | **Mejora**; falta una política de autorización por monto y de reversos automáticos (R2-N09) |
| Capacidad del Core | Límites por instancia | Presupuesto global, clases, M1 y M2 | **Mejora conceptual**; la garantía no cubre los despliegues (R2-N06) y subprovisiona CL-1 (R2-N07) |
| Vinculación | Coincidencia de cédula = creado | Seis etapas, prueba de control, evidencia de creación | **Mejora sustantiva**; vectores residuales (R2-N08) |
| Regresiones | — | Ninguna regresión de integridad detectada. **Regresión potencial de disponibilidad**: M1 puede rechazar escrituras críticas con el Core ocioso (R2-N07) | — |

## 4. Matriz de verificación de C01 y H01 a H05

| Hallazgo | Estado | Evidencia exacta | Justificación | Riesgo residual | Criterio de aceptación para cerrar |
|---|---|---|---|---|---|
| **C01** — Identidad lógica ante los reintentos | **CONDITIONALLY ACCEPTED** | ADR-0006:48 (IC-02), :52 (IC-06), :56 (IC-10), :58 (IC-12); 08 §3; QA9 | El defecto de ARG-01 está **resuelto en el diseño**: la misma intención con la misma clave no puede ejecutarse dos veces (sección 5, puntos 1, 2, 4 a 6). La garantía de la barrera IC-12 («solo una operación terminal **sin efecto confirmado** permite una nueva intención») depende de que `FAILED_NO_EFFECT` sea fiable, y hoy puede derivarse de evidencia probabilística (R2-N01). El bloqueo de equivalentes no es atómico ni cubre `*_IN_PROGRESS` (R2-N04) | Medio (RR-04, RR-13) | R2-N01 y R2-N04 resueltos en ADR-0005 y ADR-0006; QA9 validado con pruebas de inyección de fallas antes de producción |
| **H01** — Fallas ambiguas | **CONDITIONALLY ACCEPTED** | ADR-0005:52, :59; 05 §6 | La taxonomía es correcta y conservadora. Dos condiciones: (1) los nuevos intentos de clase `NOT_SENT` deben revalidar la propiedad antes de cada envío (R2-N02); (2) que una biblioteca cliente reporte «ningún byte escrito» es una **hipótesis de implementación** que debe demostrarse por biblioteca y por versión | Medio hasta la validación de la implementación | R2-N02 resuelto; tabla de clasificación por ACL con pruebas que demuestren cada asignación a `NOT_SENT` (antes de producción) |
| **H02** — Estados financieros | **CONDITIONALLY ACCEPTED** | ADR-0005:100–107; 07:64–102; 08:86 | El defecto de ARG-01 (`SPI_PROCESSING → FAILED` con débito) está resuelto. Persisten tres problemas: transiciones a estados terminales «sin efecto» y nuevos intentos de reverso basados en evidencia negativa probabilística (07:88, :100; 08:86), que contradicen INV-T1 e INV-T2 (R2-N01); modelo dual sin función de derivación (R2-N05); casos sin cierre excepcional (R2-N10) | Medio | R2-N01 y R2-N05 resueltos; R2-N10 planificado |
| **H03** — Conciliación manual | **CONDITIONALLY ACCEPTED** | ADR-0005:118–125; 07 §4; QA5 | Hay rol, plazo, escalamiento, cuatro ojos y no movimiento autónomo de fondos. Falta la confirmación institucional (Q-08, DD-18). Falta además una política de autorización por monto y la regla que define qué reversos pueden ser automáticos (R2-N09) | Medio | Q-08 respondida; R2-N09 resuelto antes de cerrar la Fase 3 |
| **H04** — Presupuesto del Core | **CONDITIONALLY ACCEPTED** (con regresión potencial de disponibilidad) | ADR-0011:45, :46, :48, :62, :70, :105; QA10 | El concepto (presupuesto, clases, reserva de CL-1 y CL-2, comportamiento seguro) es correcto. La garantía «total ≤ presupuesto para cualquier N ≤ `N_MAX_REPLICAS`» **es falsa** durante los despliegues, con réplicas en terminación, con cambios de configuración no atómicos y con varios servicios en la misma clase (R2-N06, sección 7). M1 subprovisiona CL-1 cuando N es bajo (R2-N07) | Alto hasta R2-N06 | Requisitos de infraestructura RI-01 a RI-07 (sección 7.3) adoptados; R2-N07 con disparador específico para CL-1 |
| **H05** — Vinculación | **CONDITIONALLY ACCEPTED** | ADR-0009:53, :55, :60; 07:161, :187, :194; QA11 | KYC separado de la autorización; la coincidencia no concede acceso; la prueba de control es obligatoria para los preexistentes; no hay enumeración. Persisten tres vectores (R2-N08): factores de contacto **recientemente modificados**; ausencia de una vía alternativa explícita cuando no hay un factor confiable; y `MANUAL_REVIEW → CUSTOMER_IDENTIFIED` sin prohibir explícitamente asignar `CREATED_BY_THIS_REQUEST` sin evidencia. DD-21 carece de un criterio bloqueante explícito para producción (R2-N12) | Medio-alto hasta DD-21 | R2-N08 resuelto en ADR-0009; DD-21 con criterio bloqueante; la ruta `PRE_EXISTING` permanece deshabilitada (*fail-closed*) en producción hasta DD-21 |

## 5. Análisis de fallas de idempotencia (ciclo de vida)

Leyenda de la garantía: **I** = interna a la plataforma (determinista); **C** = depende del Core (G2) o es probabilística (G3).

| # | Punto del ciclo | Falla analizada | Estado persistido | Comportamiento documentado | ¿Duplicación originada en la plataforma? | Garantía | Evidencia | Resultado |
|---|---|---|---|---|---|---|---|---|
| 1 | Antes de reservar la clave | Validación fallida, caída del proceso, pérdida de la respuesta | Ninguno | Nada se persiste; el reenvío con la misma clave reserva normalmente | No | I | IC-01, IC-03, IC-04 | **Correcto** |
| 2 | Tras reservar y antes de persistir la intención | Caída entre la reserva y el registro | — | **El punto no existe**: la reserva, la huella y el registro inicial son una única escritura atómica | No | I | IC-06 | **Correcto** |
| 3 | Tras persistir y antes del envío | Caída en `RECEIVED` o `VALIDATING`; caída tras persistir `CORE_IN_PROGRESS` sin enviar | `RECEIVED` / `VALIDATING` / `CORE_IN_PROGRESS` | `CORE_IN_PROGRESS` → `CORE_UNKNOWN` (INV-T5) → conciliación. **Para `RECEIVED` y `VALIDATING` no hay regla de recuperación ni de expiración**: si se reanudan tarde, ejecutan una operación que el cliente pudo dar por fallida, en condiciones distintas (contradice «no encolar») | No por la misma clave; **sí es posible un duplicado semántico** si el cliente reinició con otra clave y confirmó el posible duplicado | I | 07:64, :79, :112 | **Brecha (R2-N03)** |
| 4 | Durante el envío | Escritura parcial, reinicio de la conexión, conexión del pool cerrada | `CORE_IN_PROGRESS` | `UNKNOWN`, sin reintento | No, **si** la clasificación `NOT_SENT` es correcta y el reintento revalida la propiedad | I | ADR-0005:52, :59 | **Condicionado** (R2-N02; validación por biblioteca) |
| 5 | Ejecutado en el Core, antes de la respuesta | Timeout de lectura o respuesta perdida | `CORE_IN_PROGRESS` → `CORE_UNKNOWN` | Sin una segunda invocación (INV-T3); conciliación. Si G3 no encuentra coincidencias tras `T_ASENTAMIENTO_CORE` (por ejemplo, porque el Core asienta tarde), se declara `FAILED_NO_EFFECT` **terminal**, se libera la barrera de IC-12 y el usuario puede repetir la operación | La plataforma no reinvoca; **sí habilita** la duplicación por una nueva intención **a partir de un falso negativo** | C | 07:88; 08:86; ADR-0005:101 | **Brecha (R2-N01)** |
| 6 | Tras la respuesta y antes de persistir el resultado | Caída tras recibir la confirmación | `CORE_IN_PROGRESS` | INV-T5 → `CORE_UNKNOWN` → la conciliación encuentra la operación (determinista con G2) | No | I + C | ADR-0005:104 | **Correcto** (con G3 aplica R2-N01) |
| 7 | Recuperación tras un reinicio | Trabajador lento o pausado (no caído) mientras la recuperación toma la operación | `CORE_IN_PROGRESS` → `CORE_UNKNOWN` | INV-T4 impide transiciones duplicadas, pero **no impide que el trabajador original envíe** (por ejemplo, un nuevo intento `NOT_SENT` tras una espera) después de que la operación pasó a `CORE_UNKNOWN`. No se define cuándo una operación se considera abandonada (no hay arrendamiento) ni un token de cercado que el envío deba presentar | **Sí, ejecución oculta:** el Core ejecuta una operación que la plataforma está conciliando y puede declarar sin efecto (combinada con R2-N01) | I | ADR-0005:103–104; 07:112 | **Brecha (R2-N02)** |
| 8 | Conciliación | Falla del conciliador; coincidencia ambigua; falso negativo; reverso con resultado incierto | `*_UNKNOWN` | Si falla el conciliador, la operación permanece y se reprograma (correcto). Las ambigüedades van a revisión manual (correcto). **`REVERSAL_UNKNOWN → REVERSAL_PENDING` sobre evidencia negativa habilita un segundo reverso** (riesgo de doble crédito) | **Sí, doble reverso** posible ante un falso negativo | C | 07:100; 08:89 | **Brecha (R2-N01, R2-N09)** |

**Concurrencia con la misma clave:** IC-06, IC-07 e IC-10 garantizan una sola reserva. **Verificado** (I).

**Contenido distinto con la misma clave:** IC-09. **Verificado** (I).

**Claves distintas para la misma intención:**

- El bloqueo de ADR-0006:73 cubre solo la clase `UNKNOWN`: **no** cubre `CORE_IN_PROGRESS`, `PENDING_EXTERNAL_CONFIRMATION` ni los estados previos al envío.
- No se exige que la verificación de equivalencia sea **atómica** con la reserva, así que dos solicitudes concurrentes con claves distintas pueden pasar ambas. La confirmación de ADR-0006:74 mitiga el caso secuencial, no el concurrente (R2-N04).

**Conclusión de la sección:** el contrato de idempotencia **elimina la duplicación por la misma intención**. Las brechas restantes no están en ADR-0006. Están en la **frontera entre la idempotencia y la máquina de estados**: la evidencia negativa que libera la barrera, la propiedad de la ejecución y los estados previos al envío. La limitación del Core (G2) está **declarada**, pero **no completamente controlada**: la evidencia negativa probabilística no debe producir estados terminales.

## 6. Análisis del modelo de estados

### 6.1 Combinaciones imposibles entre dimensiones (no especificadas como prohibidas)

| Combinación | Por qué es imposible | ¿Prohibida en el diseño? |
|---|---|---|
| Ejecución en el Core = `REJECTED` y procesamiento externo ≠ `NOT_STARTED` o `NOT_APPLICABLE` | No hay débito que despachar | **No** |
| Reverso ≠ `NOT_REQUIRED` y ejecución en el Core ≠ `CONFIRMED` | Sin débito no hay reverso | **No** |
| Procesamiento externo = `CONFIRMED` y reverso = `REQUIRED` | Contradicción, salvo una devolución posterior (que se modela como otra operación) | **No** |
| Conciliación = `OPEN` sin ninguna dimensión en `UNKNOWN` | Sin incertidumbre no hay caso | **No** |
| Estado global `COMPLETED` con reverso = `REQUIRED` | Viola INV-T1 | Implícita en INV-T1 |

**Problema estructural (R2-N05):** las transiciones de 07 §3.3 se definen sobre el **estado global**, mientras que ADR-0005 §3 afirma que el global «se deriva» de las dimensiones. **No se define la función de derivación**, así que hay dos representaciones que pueden divergir.

**Simplificación propuesta, que preserva la semántica financiera:** el **estado global es la única máquina de estados autoritativa**. Las «dimensiones» pasan a ser **atributos registrados** (resultado del Core, resultado externo, resultado del reverso, caso de conciliación), que se actualizan solo en las transiciones globales. «Obligación pendiente» se define como una **función** de esos atributos. Ninguna obligación desaparece; se eliminan la ambigüedad y la superficie de prueba (RR-08).

### 6.2 Transiciones contradictorias y estados problemáticos

| Tipo | Caso | Evidencia | Hallazgo |
|---|---|---|---|
| Terminal prematuro | `CORE_UNKNOWN → FAILED_NO_EFFECT` por «coincidencia nula tras `T_ASENTAMIENTO_CORE`». La ausencia de evidencia no es evidencia de ausencia | 07:88; 08:86 | R2-N01 |
| Contradicción con un invariante | INV-T2 admite «resultado de la conciliación» como evidencia confirmada sin distinguir lo determinista (G2) de lo probabilístico (G3) | ADR-0005:101 | R2-N01 |
| Reverso no justificado | `REVERSAL_UNKNOWN → REVERSAL_PENDING` con evidencia negativa → segundo reverso | 07:100 | R2-N01, R2-N09 |
| Obligación oculta | Un terminal «sin efecto» cuyo débito aparece después: **no existe una transición ni una operación correctiva** para un efecto tardío descubierto tras un estado terminal (solo está prevista la devolución posterior a `COMPLETED`, 07 §3.6) | 07 §3.6 | R2-N01 |
| Estado sin salida | `REQUIRES_RECONCILIATION` cuando la obligación no puede cumplirse por las vías previstas (la devolución externa nunca llega). Las salidas permitidas (`COMPLETED`, `REVERSED`, `FAILED_NO_EFFECT`) no representan una **compensación institucional** ni un cierre excepcional | 07:102 | R2-N10 |
| Estados sin recuperación | `RECEIVED`, `VALIDATING` (transferencias); `STARTED`, `CUSTOMER_LOOKUP_IN_PROGRESS` (vinculación): sin regla de expiración ni de reanudación segura | 07:64, :79 | R2-N03 |

### 6.3 Verificación de los invariantes

| Invariante | Escenario concreto de prueba conceptual | Resultado |
|---|---|---|
| INV-T1 (ningún terminal con obligación) | Débito asentado tarde tras `FAILED_NO_EFFECT` por G3 | **Se viola** (R2-N01) |
| INV-T2 (sin efecto solo con evidencia confirmada) | Mismo caso: la redacción acepta la evidencia probabilística | **Ambiguo; se viola en la práctica** (R2-N01) |
| INV-T3 (≤ 1 invocación por tramo) | Trabajador zombi con un nuevo intento `NOT_SENT` tras la toma por la recuperación | **Puede violarse** (R2-N02) |
| INV-T4 (transiciones CAS) | Conciliador y consulta concurrentes | **Se cumple** para las transiciones; no protege los envíos (R2-N02) |
| INV-T5 (recuperación) | Reinicio en `CORE_IN_PROGRESS` | **Se cumple**; no cubre los estados previos al envío (R2-N03) |
| INV-T6 (reverso explícito, sin asumir reversibilidad) | Reverso imposible | **Se cumple** (`NOT_POSSIBLE` → manual); falta la política de autorización (R2-N09) |
| INV-T7 (auditoría) | Toda transición | **Se cumple** en el diseño |
| INV-T8 (notificación por movimiento) | `CORE_UNKNOWN` resuelto días después | **Se cumple**, pero sin un plazo de notificación respecto del asentamiento. Se registra como observación para la fase de notificaciones |

## 7. Validación del presupuesto global del Core

### 7.1 Verificación formal de M1

Sea `Q_c` el presupuesto de la clase *c*, `S_c` el conjunto de servicios que la usan y `n_s(t)` el número de réplicas del servicio *s* **que mantienen conexiones o solicitudes en curso con el Core** en el instante *t*. M1 asigna a cada réplica `q_{c,s} = Q_{c,s} / N_MAX_s`. La garantía requiere:

```text
Σ_{s ∈ S_c}  n_s(t) · q_{c,s}  ≤  Q_c      para todo t
```

Esta desigualdad se cumple **si y solo si** (a) `Σ_s Q_{c,s} ≤ Q_c` y (b) `n_s(t) ≤ N_MAX_s` para todo *s* y todo *t*. El diseño actual:

- **no establece (a):** ADR-0011:105 dice que cada servicio declara sus cuotas, pero no exige que la suma entre servicios respete el presupuesto de la clase;
- **asume (b) solo para el escalado automático**, no para todos los estados operativos.

### 7.2 Eventos que rompen (b)

| Evento | `n_s(t)` máximo | ¿Viola? | Comentario |
|---|---|---|---|
| Escalado automático hasta el techo | `N_MAX` | No | Caso cubierto por ADR-0011 |
| Despliegue rolling con réplicas adicionales temporales (*surge*) | `N_MAX + surge` | **Sí** | Frecuente: ocurre en cada liberación |
| Despliegue blue/green con dos flotas conectadas | `2 · N_MAX` | **Sí** | Duplica la carga potencial |
| Réplicas en terminación con solicitudes en curso | `N_MAX + terminando` | **Sí** | Las conexiones sobreviven a la retirada del tráfico |
| Cambio de `N_MAX` y de las cuotas no atómico (convivencia de configuraciones) | Mezcla de cuotas antiguas y nuevas | **Sí**, si sube `N_MAX` antes de bajar la cuota | Requiere un protocolo de orden |
| Réplicas temporales adicionales (tareas, pruebas, reprocesos) | No contabilizadas | **Sí** | Deben contar en el presupuesto |
| Falla del mecanismo dinámico M2 | Vuelve a M1 | No, **si** los arrendamientos de M2 expiran antes de que la réplica vuelva a M1 | ADR-0011:70 no lo exige explícitamente |

### 7.3 Requisitos de infraestructura propuestos (agnósticos de la tecnología)

| ID | Requisito |
|---|---|
| RI-01 | `N_MAX_REPLICAS` es un **límite automatizado y verificable** (techo del escalado), no una convención |
| RI-02 | La cuota por réplica se calcula sobre `N_EFECTIVO_MAX = N_MAX + réplicas adicionales de despliegue + solapamiento de flotas + réplicas en terminación`; o bien la estrategia de despliegue de los servicios que acceden al Core se restringe para que `n_s(t) ≤ N_MAX` (sin réplicas adicionales, o con la flota inactiva sin admisión hacia el Core hasta el corte) |
| RI-03 | `Σ_s Q_{c,s} ≤ Q_c` para cada clase, verificado **automáticamente** antes de cada cambio de configuración o de despliegue |
| RI-04 | Protocolo de cambio: al aumentar `N_MAX`, **primero** se reducen las cuotas por réplica y **después** se aumenta el techo; al disminuir, en el orden inverso |
| RI-05 | Los arrendamientos de M2 (si se activa) **expiran** en un plazo acotado; una réplica sin arrendamiento vigente usa M1 |
| RI-06 | **Control de respaldo en tiempo de ejecución:** la concurrencia real hacia el Core se mide y genera alertas cuando se acerca al presupuesto, con independencia de M1 |
| RI-07 | Si existe un intermediario frente al Core con capacidad de limitación global (Q-16), se configura como **última línea**, sin sustituir a M1 |

### 7.4 Protección de las operaciones críticas y subutilización

- **CL-1 y CL-2 reservadas:** correcto en el concepto.
- **Regresión potencial de disponibilidad (R2-N07):** con M1, la cuota de CL-1 por réplica es `CUOTA_CL1 / N_MAX`. Con pocas réplicas, las **escrituras críticas** se rechazan (`FAILED_NO_EFFECT`, «servicio ocupado») aunque el Core esté ocioso. El disparador de DD-20 habla de una «subutilización medida» en general y no considera este efecto sobre CL-1.

## 8. Seguridad de la vinculación

| Verificación | Resultado | Evidencia | Hallazgo |
|---|---|---|---|
| KYC no equivale a una autorización de acceso | **Cumple** (E1 frente a E4) | ADR-0009 §2 | — |
| La coincidencia de identificación no concede acceso | **Cumple** (VD-01, INV-O1) | ADR-0009; 07:194 | — |
| Los preexistentes requieren prueba de control | **Cumple en el requisito** (VD-03); mecanismo en DD-21 | ADR-0009:53 | — |
| Los datos de contacto desactualizados no generan un bypass | **No demostrado.** (1) No se exige una **antigüedad mínima ni estabilidad** del factor registrado: un atacante que primero cambia el contacto por otro canal (por ejemplo, ingeniería social) supera VD-03. (2) No se exige una **vía alternativa explícita** (por ejemplo, presencial) cuando no hay un factor confiable, lo que crea presión para resolver en `MANUAL_REVIEW` | ADR-0009:53 | R2-N08 |
| Sin acceso digital sin evidencia suficiente | **Parcial.** `MANUAL_REVIEW → CUSTOMER_IDENTIFIED` (07:187) no prohíbe explícitamente asignar `CREATED_BY_THIS_REQUEST`, que activaría la autorización automática de VD-02. INV-O2 lo exige, pero la tabla de transiciones no lo hace explícito para la resolución manual | 07:187, :194 | R2-N08 |
| DD-21 con un criterio bloqueante antes de producción | **No.** Su disparador es el «inicio de la fase de seguridad». No se declara que la ruta `PRE_EXISTING` permanezca **deshabilitada** (*fail-closed*) en producción mientras DD-21 no esté implementada y probada. Aunque VD-03 impide implícitamente completar `LINK_PENDING`, debe declararse | 10:38 | R2-N12 |

## 9. Riesgos residuales (reevaluación)

La severidad se evalúa por **impacto potencial**, no solo por probabilidad.

| Riesgo | Residual declarado | Residual según el comité | Justificación |
|---|---|---|---|
| RR-04 Conciliación sin idempotencia nativa | Medio | **Alto**, hasta resolver R2-N01 o validar G2 | El impacto (débito duplicado u oculto) es alto. Mientras la evidencia negativa produzca estados terminales, la limitación no está controlada |
| RR-08 Complejidad del estado durable | Bajo-medio, en aumento | **Medio** | Modelo dual sin derivación (R2-N05); más estados que probar |
| RR-13 Duplicado semántico | Medio | **Medio** (se mantiene) | Aceptable porque el usuario confirma; condicionado a R2-N04 (atomicidad y cobertura del bloqueo) |
| RR-14 Cuotas mal dimensionadas | Medio | **Alto**, hasta adoptar RI-01 a RI-07 | La garantía no cubre los despliegues (R2-N06); el impacto alcanza a todos los canales del Core |
| RR-15 Vinculación indebida | Medio | **Medio-alto** | Vectores de R2-N08; impacto de toma de cuentas |
| RR-16 Reverso no posible | Medio | **Medio** | Correcto, pero falta un cierre excepcional (R2-N10) |

### 9.1 Hallazgos MEDIUM de ARG-01 especialmente revisados

| Hallazgo | Valoración |
|---|---|
| **M03** (carrera de Cache-Aside) | Su impacto es **mayor que el declarado**: un movimiento obsoleto tras una transferencia induce al usuario a repetirla, y eso alimenta RR-13. *Read-your-writes* sobre los movimientos propios debe resolverse **antes** del diseño de la experiencia de transferencias. Se mantiene MEDIUM, con prioridad elevada |
| **M04** (Gateway frente al plazo) | Su relevancia **aumenta** con R2-N03: sin una semántica de plazo, los estados previos al envío pueden reanudarse fuera de tiempo. Debe resolverse junto con R2-N03 |
| **M06** (fraude y prevención de lavado) | La elevación de Q-12 es correcta, pero **insuficiente para el diseño de contenedores**: si existe un control de listas en la vinculación, es una etapa entre E1 y E3 que cambia la máquina de vinculación. Debe tener al menos una respuesta preliminar antes de cerrar la Fase 3 |

## 10. Condiciones para avanzar a la Fase 3

La Fase 3 **puede iniciarse**. Las siguientes condiciones son **bloqueantes**: deben cumplirse en el momento indicado o la Fase 3 no podrá aprobarse.

| ID | Condición | Momento | Hallazgos |
|---|---|---|---|
| CF-01 | Enmendar ADR-0005, 07 y 08 para separar la **evidencia de ausencia determinista** (G2, `REJECTED_CONFIRMED`, `NOT_SENT`) de la **presunta** (G3 negativa). Con evidencia presunta: estado **no terminal** que mantiene el bloqueo de equivalentes y se confirma tras la ventana de conciliación de segunda línea. Nunca un nuevo reverso sobre evidencia presunta. Definir el tratamiento de un **efecto tardío** descubierto tras un estado terminal | **Antes del diseño del contenedor del orquestador de transferencias** | R2-N01 |
| CF-02 | Definir la **propiedad de la ejecución** (arrendamiento con vencimiento) y un **token de cercado** que todo envío al Core debe validar mediante CAS inmediatamente antes de enviar, incluidos los nuevos intentos `NOT_SENT`. La recuperación solo toma una operación tras el vencimiento del arrendamiento | **Antes del diseño del contenedor del orquestador** | R2-N02 |
| CF-03 | Adoptar RI-01 a RI-07 como requisitos de la Fase 3 y de la fase de infraestructura | **Antes de cerrar la Fase 3** | R2-N06 |
| CF-04 | Regla de expiración y de reanudación de los estados previos al envío, ligada al plazo de negocio, junto con M04 | **Antes de cerrar los contratos de API de la Fase 3** | R2-N03, M04 |
| CF-05 | El bloqueo de equivalentes cubre `*_IN_PROGRESS` y los estados previos al envío, y es **atómico** con la reserva | **Antes de cerrar los contratos de API** | R2-N04 |
| CF-06 | Reglas de vinculación: antigüedad mínima del factor, vía alternativa explícita y prohibición de asignar `CREATED_BY_THIS_REQUEST` en la revisión manual sin evidencia | **Antes de cerrar la Fase 3** | R2-N08 |
| CF-07 | Política de autorización: qué reversos pueden ser automáticos (solo ante un rechazo externo determinista) y límites de autoridad por monto en la resolución manual | **Antes de cerrar la Fase 3** | R2-N09 |
| CF-08 | Respuestas preliminares de la institución a Q-02, Q-08, Q-12 y Q-16 | **Antes de cerrar la Fase 3** | H03, H04, M06 |
| CF-09 | Resolver *read-your-writes* sobre los movimientos propios (M03) | **Antes del diseño de la experiencia de transferencias** | M03, RR-13 |

## 11. Condiciones obligatorias antes de producción

| ID | Condición |
|---|---|
| CP-01 | Pruebas de inyección de fallas en los **ocho puntos del ciclo** de la sección 5, con verificación automatizada de «≤ 1 invocación por tramo» (QA3 (a), QA9) |
| CP-02 | Pruebas de la tabla de clasificación de cada ACL, que demuestren cada asignación a `NOT_SENT` con la biblioteca cliente y la versión reales (H01) |
| CP-03 | Pruebas de cercado: un trabajador pausado no puede enviar después de la toma por la recuperación (CF-02) |
| CP-04 | Resultado documentado de la validación de S3 (G2). Si G2 no existe: medición de base de `TASA_ERROR_CONCILIACION` en un entorno representativo |
| CP-05 | **DD-21 implementado y probado.** Hasta entonces, la ruta `PRE_EXISTING` permanece **deshabilitada** (*fail-closed*); pruebas de penetración del flujo de vinculación (QA11) |
| CP-06 | Pruebas de capacidad con el propietario del Core: presupuesto validado, QA10 y despliegue rolling o blue/green sin superar el presupuesto (RI-02) |
| CP-07 | Verificación de los cuatro ojos y de los límites por monto en la herramienta de resolución manual |
| CP-08 | Verificación de las notificaciones por movimiento (INV-T8), incluidas las resoluciones tardías de `CORE_UNKNOWN` |
| CP-09 | Procedimientos operativos aprobados para `REQUIRES_RECONCILIATION` por el área confirmada (Q-08) |

## 12. Hallazgos nuevos

| ID | Severidad | Título | Evidencia | Impacto | Recomendación | Criterio de aceptación | Fase |
|---|---|---|---|---|---|---|---|
| **R2-N01** | **HIGH** | La ausencia de efecto inferida por evidencia negativa probabilística produce estados terminales y nuevos intentos de reverso | 07:88, :100; 08:86; ADR-0005:100–101 | Débito oculto, o duplicado por una nueva intención habilitada por IC-12; doble reverso. Contradice INV-T1 e INV-T2 | Ver CF-01: estado no terminal con evidencia presunta, confirmación de segunda línea, sin reversos sobre evidencia presunta, tratamiento del efecto tardío | Ninguna transición a un terminal «sin efecto» ni un nuevo reverso basados solo en G3 negativa | Antes del contenedor del orquestador (Fase 3) |
| **R2-N02** | **HIGH** | Sin propiedad exclusiva ni token de cercado de la ejecución | ADR-0005:103–104, :52 (nuevo intento `NOT_SENT`); 07:112; búsqueda sin resultados de «arrendamiento», «cercado» y «*fencing*» | Envío al Core por un trabajador zombi tras la toma de la recuperación; ejecución que la plataforma concilia como posiblemente inexistente | Ver CF-02 | Regla de arrendamiento y cercado en ADR-0005; CP-03 | Antes del contenedor del orquestador (Fase 3) |
| **R2-N03** | MEDIUM | Estados previos al envío sin recuperación ni expiración | 07:64, :79; búsqueda sin resultados de «expir» en las transferencias | Ejecución tardía en condiciones no aceptadas por el cliente; posible duplicado semántico | Ver CF-04 | Regla de expiración por plazo de negocio | Contratos de API (Fase 3) |
| **R2-N04** | MEDIUM | El bloqueo de equivalentes no es atómico ni cubre los estados en curso | ADR-0006:73 | Dos operaciones equivalentes concurrentes con claves distintas | Ver CF-05 | Bloqueo atómico con la reserva y con cobertura ampliada | Contratos de API (Fase 3) |
| **R2-N05** | MEDIUM | Modelo dual (dimensiones frente a estado global) sin función de derivación ni matriz de combinaciones válidas | ADR-0005 §3; 07 §3.1–3.3 | Divergencia de representaciones; RR-08 subestimado | Simplificación de la sección 6.1 | Una sola fuente autoritativa y una función de obligación pendiente | Fase 3 |
| **R2-N06** | **HIGH** | La regla M1 no se sostiene en despliegues, terminaciones, cambios de configuración ni con varios servicios por clase | ADR-0011:45, :105; QA10; informe de remediación AT-08 | Saturación del Core durante las liberaciones; impacto en todos los canales | RI-01 a RI-07 | RI adoptados; CP-06 | Antes de cerrar la Fase 3 |
| **R2-N07** | MEDIUM | M1 subprovisiona CL-1 con pocas réplicas (regresión potencial de disponibilidad) | ADR-0011:48, :62; 10:37 (DD-20) | Rechazo de transferencias con el Core ocioso | Disparador de DD-20 específico para los rechazos de CL-1; o `N_MAX` de los servicios de escritura acotado a su carga real | Disparador y análisis documentados | Fase 3 |
| **R2-N08** | MEDIUM | Vectores residuales en la vinculación: antigüedad del factor, vía alternativa y revisión manual | ADR-0009:53; 07:187, :194 | Toma de cuentas mediante el cambio previo del contacto o una resolución manual permisiva | Ver CF-06 | Reglas en ADR-0009 y en la tabla de transiciones | Antes de cerrar la Fase 3 |
| **R2-N09** | MEDIUM | Sin política de autorización para reversos automáticos ni límites por monto en la resolución manual | ADR-0005:105, :125; búsqueda sin resultados de «autoriz» en ADR-0005 | Movimientos financieros automáticos sin un criterio de autorización explícito | Ver CF-07 | Política documentada | Antes de cerrar la Fase 3 |
| **R2-N10** | LOW | Sin estado de cierre excepcional para obligaciones que no se resuelven por las vías previstas | 07:102 | Casos indefinidos en `REQUIRES_RECONCILIATION` | Estado terminal de cierre excepcional con compensación institucional explícita, autorizada y auditada | Estado y reglas definidos | Fase 3 o de componentes |
| **R2-N11** | LOW | La garantía de IC-02 es más débil en la SPA web (persistencia del navegador) | ADR-0006:48 | Más duplicados semánticos desde la web | Evaluar en la Fase 3 una intención creada en el servidor como alternativa | Decisión documentada | Fase 3 |
| **R2-N12** | LOW | DD-21 sin un criterio bloqueante explícito para producción | 10:38 | Riesgo de habilitar la vinculación de preexistentes sin mecanismo | Declarar *fail-closed* hasta DD-21 (CP-05) | DD-21 actualizada | Antes de producción; registro inmediato |

**Hallazgos de ARG-01 no revisados en esta verificación:** M05, M08, M09 y L01 a L04 siguen **abiertos** según el informe de remediación. Esta revisión no los reevalúa, salvo M03, M04 y M06 (sección 9.1).

---

**ARG-01-R2 completado. Veredicto: CONDITIONAL GO. La Fase 3 puede iniciarse bajo las condiciones CF-01 a CF-09; CF-01 y CF-02 deben cumplirse antes de diseñar el contenedor del orquestador de transferencias. Pendiente de aprobación humana.**
