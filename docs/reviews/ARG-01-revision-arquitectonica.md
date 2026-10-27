# ARG-01 — Revisión arquitectónica (Architecture Review Gate)

| Campo | Valor |
|---|---|
| Revisión | ARG-01 |
| Objeto | Plataforma de Banca Digital — Fases 0, 1 y 2 |
| Fecha | 2026-10-27 |
| Línea base revisada | Rama `main`, commit `c1a56d7` (sincronizada con `origin/main`; árbol de trabajo limpio al iniciar) |
| Comité (perfiles simulados) | Principal Solutions Architect; Enterprise Architect — Financial Services; Banking Domain Architect; Application Security Architect; Distributed Systems Architect; Cloud & Infrastructure Architect; Software Quality / Architecture Governance Lead |
| Veredicto | **NO-GO** (ver sección 13) |
| Documentos asociados | [Plan de remediación](ARG-01-plan-de-remediacion.md), [Preguntas para el arquitecto](ARG-01-preguntas-para-el-arquitecto.md) |

> Esta revisión se realizó con un criterio deliberadamente adverso: se buscan debilidades, no se defiende el diseño. Que una decisión esté documentada en un ADR no se tomó como prueba de que sea correcta.

---

## 1. Resumen ejecutivo

### Fortalezas

Las Fases 0–2 muestran un nivel de rigor poco habitual:

- **Separación honesta** entre requisitos del reto, supuestos y decisiones.
- **Ausencia de cifras inventadas.**
- **Tecnología diferida con disparadores explícitos.**
- **Modelado correcto de la incertidumbre:** el principio «un timeout no equivale a una falla» y el estado `UNKNOWN`.
- **Prudencia regulatoria:** no se afirma cumplimiento normativo.
- **Modelo C4 de nivel 1 limpio,** validado formalmente.

### Por qué el veredicto es NO-GO

La decisión que protege la integridad financiera, la idempotencia ([ADR-0006](../adr/ADR-0006-idempotencia-y-conciliacion.md)), tiene un **hueco en su punto más expuesto: los reintentos del cliente**.

- La clave de idempotencia se genera en el dominio «al aceptar la operación».
- La clave del canal solo «se asocia» y no se declara obligatoria.
- No se define cómo se vincula la clave con el contenido (*payload*) de la operación, qué ocurre ante un conflicto ni durante cuánto tiempo se retiene.

**Consecuencia:** una app móvil que pierde la respuesta y reenvía la transferencia puede producir **dos operaciones lógicas distintas** con dos claves de dominio, y por tanto un **débito duplicado**. Este es el modo de falla más frecuente en banca digital, y el diseño actual no lo cierra (hallazgo **ARG01-C01**).

### Otros riesgos que no bloquean, pero condicionan la Fase 3

- **Fallas de conexión:** su clasificación como «sin efecto» es demasiado amplia (H01).
- **Estado `FAILED` contradictorio** en las transferencias interbancarias con débito previo (H02).
- **Conciliación manual sin responsable** confirmado (H03).
- **Protección del Core ante el escalado horizontal:** no está garantizada (H04).
- **Posible toma de cuentas** durante el onboarding de personas que ya son clientes (H05).

### Remediación

El costo es **acotado y esencialmente documental**: enmendar ADR-0006, precisar ADR-0005 y ajustar dos escenarios de calidad. Requiere **autorización humana** para modificar los ADR aprobados. Con esa enmienda aprobada, el comité anticipa un **CONDITIONAL GO**.

| Indicador | Valor |
|---|---|
| Puntuación ponderada | **7,0 / 10** |
| Hallazgos | 1 CRITICAL · 5 HIGH · 9 MEDIUM · 5 LOW |
| Veredicto | **NO-GO** hasta cerrar ARG01-C01 |

## 2. Alcance

**Incluido:**

- Fase 0: drivers, supuestos, atributos y escenarios de calidad.
- Fase 1: frontera de integración con el Core, ACL, SDK, resiliencia, caché, idempotencia, conciliación y máquinas de estado.
- Fase 2: contexto C4, actores, sistemas externos, relaciones, catálogo de integraciones y límites de confianza.
- Documentos normativos (`00-*`) y gobierno del repositorio, solo en lo que afecta a la defensa de las Fases 0–2.

**Excluido** (no se penaliza su ausencia): contenedores, componentes, infraestructura cloud, DR, diseño de OAuth/FAPI, stack de frontend, selección de proveedores e implementación.

## 3. Metodología

1. Inspección del repositorio: rama, estado, historial y archivos reales.
2. Lectura completa de los documentos de las Fases 0–2, los 10 ADR, el DSL y los diagramas exportados. Contraste con el PDF original del reto.
3. Validaciones automáticas disponibles (sección 4.2).
4. Revisión de decisiones D1–D9 con preguntas críticas.
5. Análisis de los escenarios de falla F01–F12, sin inventar mitigaciones: lo que no está documentado se clasifica como brecha.
6. Análisis STRIDE conceptual.
7. Clasificación de los hallazgos con la escala CRITICAL, HIGH, MEDIUM y LOW, y evidencia por archivo y línea.
8. Scorecard ponderado y decisión de gate con las reglas de la sección 13.

Convención de evidencia: `archivo:línea`, con líneas de la línea base `c1a56d7`.

## 4. Inventario de evidencias

### 4.1 Artefactos revisados

| Categoría | Artefactos |
|---|---|
| Fuente del reto | `~/Downloads/Reto Técnico Arquitecto Jardín Azuayo.pdf` (no versionado; R1–R6, C1–C6, E1–E4) |
| Normativos | `docs/00-politica-de-documentacion.md`, `docs/00-estandares-de-ingenieria.md` |
| Fase 0 | `docs/01` a `docs/04` |
| Fase 1 | `docs/05` a `docs/08`, `docs/10`, `docs/11`, `docs/adr/ADR-0001` a `ADR-0010` |
| Fase 2 | `docs/12` a `docs/15`, `c4/workspace.dsl`, `diagrams/c4/*` |
| Transversal | `docs/09-trazabilidad-de-requisitos.md`, `README.md`, historial git |

### 4.2 Validaciones

| ID | Verificación | Resultado | Tipo |
|---|---|---|---|
| V1 | Sintaxis de Structurizr DSL (`structurizr/structurizr validate`) | Sin errores; código de salida 0 | **VALIDADO AUTOMÁTICAMENTE** |
| V2 | Diagramas exportados frente al DSL (reexportación y comparación) | Los 4 `.puml` son idénticos a los versionados; los diagramas no están desactualizados | **VALIDADO AUTOMÁTICAMENTE** |
| V3 | Identificadores C4 y referencias entre elementos | Sin duplicados. El validador resuelve todas las referencias. Las 15 relaciones (REL-01 a REL-15) coinciden entre el DSL y `docs/12` | **VALIDADO AUTOMÁTICAMENTE** |
| V4 | Enlaces y anclas Markdown (script propio) | 28 archivos, 0 enlaces rotos | **VALIDADO AUTOMÁTICAMENTE** |
| V5 | Numeración de ADR | ADR-0001 a ADR-0010 continuos; el número del archivo coincide con el título. `ADR-0011` aparece solo como ejemplo de commit (`00-politica…:155,164`) | **VALIDADO AUTOMÁTICAMENTE** |
| V6 | Convención de nombres de archivo | 100 % conforme (kebab-case ASCII) | **VALIDADO AUTOMÁTICAMENTE** |
| V7 | IDs referenciados frente a definidos (S, DD, RR, Q, INT, QA) | Todos los referenciados están definidos | **VALIDADO AUTOMÁTICAMENTE** |
| V8 | Secretos y credenciales (patrones) | Ninguna coincidencia | **VALIDADO AUTOMÁTICAMENTE** (cobertura limitada a patrones conocidos) |
| V9 | markdownlint (`markdownlint-cli2` v0.24.0) | 822 avisos MD060 (estilo de tabla) y 3 MD036 (énfasis como encabezado) | **VALIDADO AUTOMÁTICAMENTE** — ver ARG01-L05 |
| V10 | Estado git | Rama `main`, limpia, sincronizada con `origin/main` | **VALIDADO AUTOMÁTICAMENTE** |
| V11 | Coherencia de la matriz de trazabilidad con el reto | El texto de R1–R6, C1–C6 y E1–E4 coincide con el PDF | **REVISADO MANUALMENTE** |
| V12 | Coherencia semántica entre los ADR, los documentos y los escenarios | Ver hallazgos | **REVISADO MANUALMENTE** |
| V13 | Renderizado visual de los SVG | Revisado en la Fase 2; no se repitió | **NO VALIDADO** en esta revisión |

## 5. Scorecard

| Dimensión | Peso | Puntuación | Ponderado |
|---|---|---|---|
| Requisitos y trazabilidad | 15 % | 7,5 | 1,13 |
| Decisiones y trade-offs | 20 % | 7,5 | 1,50 |
| C4 y límites del sistema | 15 % | 8,0 | 1,20 |
| Integridad e idempotencia | 20 % | 5,5 | 1,10 |
| Seguridad y cumplimiento | 10 % | 6,0 | 0,60 |
| Resiliencia y consistencia | 10 % | 6,5 | 0,65 |
| Calidad documental y validación | 10 % | 8,5 | 0,85 |
| **Total** | 100 % | — | **7,03 ≈ 7,0 / 10** |

### 5.1 Detalle por dimensión

**Requisitos y trazabilidad — 7,5**

- **Evidencia:** `docs/09` (secciones 2–6, con trazabilidad inversa) y el texto literal del reto.
- **Fortalezas:** cada requisito tiene estado y preguntas abiertas. Se separan requisito, supuesto y decisión. No se declaran como cubiertos los requisitos de fases posteriores.
- **Debilidades:**
  - el registro regulatorio que define la política (`REG`/`REG-PV`) nunca se instanció (M05);
  - el alcance de R4 («cada movimiento») depende de Q-04.
- **Recomendación:** instanciar el registro regulatorio y vincularlo a C1.

**Decisiones y trade-offs — 7,5**

- **Evidencia:** ADR-0001 a ADR-0010, `docs/05`.
- **Fortalezas:**
  - las alternativas descartadas son reales, no figuras retóricas;
  - la opción B se reconoce como válida en su contexto;
  - el supuesto organizacional es explícito (S11);
  - CQRS y el bus de eventos se difieren con disparadores.
- **Debilidades:**
  - ADR-0001 no analiza el trade-off del **presupuesto global de capacidad del Core** (H04), que es precisamente donde la opción B tiene ventaja;
  - la decisión de resiliencia y plazos no tiene ADR (M04).
- **Recomendación:** completar ADR-0001 con el análisis de la capacidad compartida.

**C4 y límites del sistema — 8,0**

- **Evidencia:** `c4/workspace.dsl`, `docs/12`, `docs/14`, V1–V3.
- **Fortalezas:**
  - un único sistema en diseño, sin componentes internos ni tecnologías;
  - vista principal y vista ampliada bien separadas;
  - el IdP no se presupone externo y no se asume integración directa con el SPI;
  - el límite de confianza está separado de la frontera de software.
- **Debilidades:**
  - REL-05 lleva la etiqueta «Confirmada» con línea continua aunque su ruta no está validada (M07);
  - no se tratan las fronteras organizacional y de despliegue (L01).

**Integridad e idempotencia — 5,5**

- **Evidencia:** ADR-0005, ADR-0006, `docs/07`, `docs/08`, `docs/04` (QA3, QA5).
- **Fortalezas:**
  - persistencia antes de invocar;
  - sin reintentos a ciegas;
  - el estado `UNKNOWN` está modelado;
  - la limitación de la correlación de negocio se reconoce honestamente (RR-04).
- **Debilidades:** C01 (identidad lógica ante los reintentos del cliente), H01 (clasificación de fallas previas al envío), H02 (`FAILED` con débito), M02 (recuperación y concurrencia) y M01 (una medida que promete lo indemostrable).
- **Recomendación:** enmendar ADR-0006 y ADR-0005 (plan de remediación).

**Seguridad y cumplimiento — 6,0**

- **Evidencia:** `docs/14`, ADR-0010, QA4, `docs/13`.
- **Fortalezas:** sin afirmaciones de cumplimiento, datos sensibles identificados por integración, cruces de límites catalogados.
- **Debilidades:** vector de toma de cuentas en el onboarding (H05), prevención de fraude y de lavado de activos sin prioridad (M06), sin escenario de ataques biométricos (M09), registro regulatorio ausente (M05).

**Resiliencia y consistencia — 6,5**

- **Evidencia:** `docs/05` (secciones 5–7), `docs/06`, ADR-0004, ADR-0008.
- **Fortalezas:** taxonomía de timeouts, plazo propagado, degradación diferenciada entre lecturas y escrituras, TTL como mecanismo de seguridad y no de consistencia.
- **Debilidades:** límites de concurrencia por instancia que no resisten el escalado (H04), la carrera clásica de Cache-Aside no se trata (M03), la relación entre el timeout del Gateway y el plazo de negocio no está definida (M04).

**Calidad documental y validación — 8,5**

- **Evidencia:** V1–V10.
- **Fortalezas:** DSL validado formalmente, diagramas regenerables y verificados frente al DSL, cero enlaces rotos, convenciones aplicadas.
- **Debilidades:** avisos de estilo de markdownlint (L05), un commit fuera de la convención (L04), estados de ADR autoasignados (L03).

## 6. Hallazgos clasificados

### Resumen

| Severidad | Cantidad | IDs |
|---|---|---|
| CRITICAL | 1 | C01 |
| HIGH | 5 | H01, H02, H03, H04, H05 |
| MEDIUM | 9 | M01 – M09 |
| LOW | 5 | L01 – L05 |

### ARG01-C01 — La identidad lógica de la operación no está garantizada ante los reintentos del cliente

| Campo | Contenido |
|---|---|
| Severidad | **CRITICAL** |
| Descripción | La clave de idempotencia se genera en el dominio «al aceptar la operación». La clave del canal solo «se asocia» para detectar duplicados; su formato y su validación se difieren, y **no se declara obligatoria**. Tampoco se definen: (a) quién la genera y en qué momento (por intención del usuario o por intento HTTP); (b) su alcance (cliente, tipo de operación); (c) la **vinculación con el contenido** de la operación (huella del *payload*); (d) el comportamiento ante **la misma clave con un payload diferente**; (e) la ventana de retención. Sin una clave de canal obligatoria y persistente en el dispositivo, cada reintento del cliente es una operación nueva con una clave de dominio nueva, y la restricción de unicidad no lo impide |
| Evidencia | `docs/adr/ADR-0006-idempotencia-y-conciliacion.md:21` («genera una clave… La clave de solicitud del canal se asocia»), `:23`; `docs/08-idempotencia-y-conciliacion.md:22` (clave del canal; «Su formato y su validación se definen en la fase de contenedores»), `:30`, `:32` |
| Requisito afectado | R1 (transferencias), R3 (creación de cliente); AQ-02; QA3, QA5 |
| ADR relacionado | ADR-0006 (principal), ADR-0005 |
| Impacto | **Débito duplicado** por un comportamiento normal de la red móvil (pérdida de la respuesta y reenvío). Ninguna conciliación lo detecta, porque son dos intenciones válidas desde la perspectiva de la plataforma. Cubre los escenarios F03, F04 y F12 |
| Recomendación | Enmendar ADR-0006 para que: (1) toda operación crítica **exija** una clave de idempotencia del canal, generada **una vez por intención del usuario**, persistida en el dispositivo antes del envío y reutilizada en todos los reintentos; (2) el alcance de unicidad sea, como mínimo, (cliente, tipo de operación, clave); (3) se almacene una **huella canónica del payload**: misma clave y misma huella devuelven el estado existente **sin nueva ejecución**; misma clave y huella distinta producen un **rechazo explícito sin efectos**; (4) la ventana de retención sea, como mínimo, el horizonte máximo de reintento del canal; (5) la clave de dominio se derive o se vincule 1:1 con la del canal; (6) quede explícita la **garantía real**: la plataforma garantiza *como máximo una iniciación por intención* (*at-most-once initiation*), **no** ejecución exactamente una vez (*exactly-once*) en el Core |
| Criterio de aceptación | ADR-0006 enmendado y aprobado. `docs/08` actualizado. F03, F04 y F12 con un comportamiento esperado documentado y verificable. Existe un escenario de calidad para el reintento del cliente con medida explícita |
| Fase de resolución | **Antes de iniciar la Fase 3** (afecta los contratos de API y el diseño del contenedor de transferencias) |
| Estado | Abierto |

### ARG01-H01 — La clasificación de «falla previa al envío» es demasiado amplia

| Campo | Contenido |
|---|---|
| Severidad | **HIGH** |
| Descripción | `docs/07` clasifica «circuito abierto o error de conexión» como indisponibilidad previa a la invocación, que pasa a `FAILED` sin efectos. Pero «error de conexión» incluye fallas **posteriores** al envío de bytes: conexión reiniciada durante la escritura o la lectura, conexiones reutilizadas del pool que estaban cerradas, o errores devueltos por intermediarios (ESB, proxies, balanceadores) entre la plataforma y el Core. `docs/05:197` es correcto en lo conceptual («la solicitud no salió»), pero la regla operativa de `docs/07:61` lo generaliza. Un `FAILED` erróneo invita al cliente a crear una **nueva intención**, que C01 no puede deduplicar |
| Evidencia | `docs/07-maquina-de-estados-transaccional.md:61`; `docs/05-frontera-de-integracion-core.md:161`, `:197` |
| Requisito afectado | R1; AQ-02; QA5 |
| ADR relacionado | ADR-0005 |
| Impacto | Duplicación por una acción del cliente tras un `FAILED` que en realidad tuvo efecto |
| Recomendación | Definir en ADR-0005 una **taxonomía cerrada**: solo son «no enviada» las fallas demostrablemente previas a la escritura de la solicitud hacia el **destino final** (circuito abierto local, rechazo o timeout del establecimiento de la conexión o del *handshake* TLS, falta de tiempo dentro del plazo antes de invocar). **Cualquier otra falla** se clasifica como `UNKNOWN`. Si existe un intermediario, el éxito de la conexión con el intermediario no prueba nada sobre el Core |
| Criterio de aceptación | Una tabla de clasificación de fallas en ADR-0005 o `docs/07`, con cada tipo asignado a `FAILED` o a `UNKNOWN`, referenciada desde `docs/05` |
| Fase de resolución | Antes de cerrar la Fase 3 |
| Estado | Abierto |

### ARG01-H02 — Modelo de estados interbancario contradictorio

| Campo | Contenido |
|---|---|
| Severidad | **HIGH** |
| Descripción | `FAILED` se define como «sin efecto financiero, o con efecto revertido y conciliado». Sin embargo, existe la transición `SPI_PROCESSING → FAILED` cuando ya hay un débito previo, sin un estado intermedio de reverso. `FAILED` es terminal: el modelo permite cerrar una operación con el dinero del cliente debitado y sin un reverso registrado. Además, no se define qué significa `COMPLETED` en una transferencia interbancaria (¿aceptada por la infraestructura de pagos o liquidada?). El nombre `SPI_PROCESSING` contradice el supuesto S13 revisado, que ya no presupone el SPI |
| Evidencia | `docs/07-maquina-de-estados-transaccional.md:36`, `:37`, `:50`; sección 3.4; `docs/02-supuestos.md` (S13) |
| Requisito afectado | R1 (transferencias interbancarias); AQ-02 |
| ADR relacionado | ADR-0005 |
| Impacto | Fondos del cliente en un limbo contable sin estado observable. Una notificación «fallida» con el dinero debitado genera reclamos y riesgo regulatorio |
| Recomendación | Agregar a ADR-0005 un estado explícito y no terminal para el reverso pendiente (con su propia conciliación). Definir la semántica de `COMPLETED` por tipo de transferencia. Renombrar el estado a un término agnóstico de la topología, por ejemplo `EXTERNAL_PROCESSING`, cuando se resuelva Q-01 |
| Criterio de aceptación | Ninguna transición lleva a `FAILED` mientras exista un efecto financiero no revertido. La semántica de `COMPLETED` está definida para las transferencias propias e interbancarias |
| Fase de resolución | Antes del diseño del contenedor de transferencias (Fase 3). El nombre del estado, al resolver Q-01 |
| Estado | Abierto |

### ARG01-H03 — `REQUIRES_RECONCILIATION` sin responsable, sin plazo y sin mecanismo confirmados

| Campo | Contenido |
|---|---|
| Severidad | **HIGH** |
| Descripción | `UNKNOWN` sí tiene salida temporal: escala al vencer `T_RESOLUCION_UNKNOWN`. Pero `REQUIRES_RECONCILIATION` solo sale por una «resolución manual autorizada y auditada», y el actor que la ejecuta (operador de soporte) es **candidato** con interacción por decidir (DD-18). No hay plazo máximo, ni escalamiento, ni comunicación al cliente, ni regla sobre sus fondos durante ese estado. En la práctica, **una transferencia puede quedar indefinidamente sin resolver** |
| Evidencia | `docs/07-maquina-de-estados-transaccional.md:39`, `:54`; `docs/12-contexto-del-sistema.md:90`; `docs/10-decisiones-diferidas.md` (DD-18) |
| Requisito afectado | R1; AQ-02, AQ-04 |
| ADR relacionado | ADR-0005, ADR-0006 |
| Impacto | Casos sin dueño, reclamos sin respuesta. La decisión de diseño de la Fase 1 depende de una capacidad organizacional no confirmada |
| Recomendación | Confirmar con la institución el área responsable y el medio (consola de la plataforma o sistema existente). Definir un plazo máximo con escalamiento como parámetro «por acordar». Documentar la comunicación al cliente y el tratamiento de sus fondos |
| Criterio de aceptación | DD-18 resuelto o con decisión provisional aprobada. Parámetro `T_RESOLUCION_MANUAL` registrado en `docs/04`. Procedimiento de escalamiento descrito |
| Fase de resolución | Antes de cerrar la Fase 3 (determina si existen contenedores administrativos) |
| Estado | Abierto |

### ARG01-H04 — La protección del Core no resiste el escalado horizontal; el aislamiento por ACL es lógico, no físico

| Campo | Contenido |
|---|---|
| Severidad | **HIGH** |
| Descripción | Las ACL aplican límites de concurrencia y pools **por operación**, y presumiblemente por instancia. Si los servicios escalan horizontalmente (QA8), el límite efectivo hacia el Core se multiplica por el número de réplicas. Además, cada ACL fija su límite por separado: **no existe un presupuesto global de capacidad del Core** repartido entre dominios. El Bulkhead protege los recursos de la plataforma, pero **no aísla el Core**: una avalancha de consultas de movimientos puede saturar el Core y degradar las transferencias. ADR-0001 no analiza este trade-off, que es justamente una ventaja natural de la opción B (capa centralizada) |
| Evidencia | `docs/05-frontera-de-integracion-core.md:97`, `:131`; `docs/04-escenarios-de-calidad.md:48`, `:121` (`MAX_CONCURRENCIA_CORE`); `docs/adr/ADR-0001-acl-por-dominio.md:43`, `:55-56` |
| Requisito afectado | C2, C5; QA2, QA8; S4 |
| ADR relacionado | ADR-0001 |
| Impacto | La protección del Core, uno de los drivers de la Fase 1, puede fallar justo en el pico de demanda. Una falla de lectura degrada las escrituras críticas |
| Recomendación | Completar ADR-0001 con: (1) la distinción explícita entre aislamiento lógico y aislamiento de infraestructura; (2) un **presupuesto de capacidad del Core** por dominio y por tipo de operación, con prioridad para las escrituras críticas; (3) cómo se respeta ese presupuesto con N réplicas (límites derivados del máximo de réplicas o mecanismo de control global). El mecanismo se decide en la Fase 3 |
| Criterio de aceptación | ADR-0001 actualizado con el análisis. Decisión de la Fase 3 que demuestre que la concurrencia total hacia el Core ≤ `MAX_CONCURRENCIA_CORE` para cualquier número de réplicas |
| Fase de resolución | Análisis en ADR-0001 antes de iniciar la Fase 3; mecanismo dentro de la Fase 3 |
| Estado | Abierto |

### ARG01-H05 — La conciliación del onboarding por identificador de persona abre un vector de toma de cuentas

| Campo | Contenido |
|---|---|
| Severidad | **HIGH** |
| Descripción | Ante un resultado incierto en la creación del cliente, la regla es consultar si «ya existe un cliente con el mismo identificador de persona» y concluir `CORE_CREATED`. Esa regla **no distingue** entre un cliente **creado por esta vinculación** y un cliente **preexistente**. Si la persona ya era cliente (un caso habitual: clientes de agencia que se enrolan al canal digital), la vinculación terminaría en `ACTIVE` y daría acceso digital a cuentas existentes con la sola superación de KYC. Además, el flujo de **enrolamiento de clientes existentes** no está modelado, aunque el reto habla de «onboarding de nuevos usuarios» (no necesariamente nuevos clientes) |
| Evidencia | `docs/07-maquina-de-estados-transaccional.md:102` y sección 4.3; `docs/08-idempotencia-y-conciliacion.md` (sección 6); `docs/adr/ADR-0009-core-autoritativo.md` (Decisión, Consecuencias) |
| Requisito afectado | R3; AQ-01 |
| ADR relacionado | ADR-0009, ADR-0006 |
| Impacto | Riesgo de suplantación con acceso a productos financieros existentes. Exposición de información sensible |
| Recomendación | Enmendar ADR-0009: la conciliación concluye `CORE_CREATED` **solo** si hay evidencia de que el cliente lo creó esta vinculación (referencia de la solicitud conservada en el Core, fecha de creación dentro de la ventana, coincidencia con los datos de la solicitud). Un cliente preexistente lleva a un flujo distinto de **enrolamiento de cliente existente**, con controles propios que se definen en la fase de seguridad. Registrar la pregunta a la institución sobre el alcance de «nuevos usuarios» |
| Criterio de aceptación | ADR-0009 distingue explícitamente entre cliente creado y cliente preexistente. El enrolamiento de clientes existentes está modelado como flujo o como pregunta abierta de impacto alto |
| Fase de resolución | Antes de cerrar la Fase 3 (afecta las integraciones del onboarding con el Core) |
| Estado | Abierto |

### ARG01-M01 — Un escenario de calidad promete una garantía indemostrable

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | QA3 fija como medida «0 operaciones financieras duplicadas». Sin idempotencia nativa en el Core, la conciliación es probabilística (RR-04, riesgo residual medio) y no puede garantizar cero duplicados. La medida contradice el propio registro de riesgos. QA5 sí está acotada correctamente («0 débitos duplicados **por reintento**») |
| Evidencia | `docs/04-escenarios-de-calidad.md:61`; `docs/11-riesgos-residuales.md:57` |
| Requisito afectado | AQ-02 |
| ADR relacionado | ADR-0006 |
| Impacto | Un comité puede refutar la medida con el propio RR-04. Debilita la credibilidad del conjunto |
| Recomendación | Reformularla como «0 duplicados originados por reintentos de la plataforma o del canal con la misma intención», más una métrica separada de **exactitud de la conciliación** y de casos escalados |
| Criterio de aceptación | QA3 reformulada y coherente con RR-04 |
| Fase de resolución | Con la remediación de C01 |
| Estado | Abierto |

### ARG01-M02 — Recuperación y concurrencia de los estados en curso no especificadas; máquina de onboarding incompleta

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | (a) No hay una regla explícita para tratar, tras un reinicio, las operaciones que quedaron en `CORE_PROCESSING` o en `PENDING_CORE`. `docs/07` solo dice que la operación «puede retomarse o conciliarse». (b) `PENDING_CORE` mezcla «pendiente» y «en curso», así que tras un reinicio no se sabe si la solicitud salió; la máquina de transferencias sí separa esos casos. (c) La máquina de onboarding no distingue el **rechazo** de KYC de la **indisponibilidad** del proveedor: un proveedor caído podría tratarse como `REJECTED`, o dejar la solicitud en `VALIDATING` sin salida. (d) El control de concurrencia de las transiciones (bloqueo optimista) solo aparece en el perfil condicional Java/Quarkus, no en ADR-0005 |
| Evidencia | `docs/07-maquina-de-estados-transaccional.md:13`, `:82`, `:83`, `:95`; `docs/00-estandares-de-ingenieria.md:170` |
| Requisito afectado | R1, R3; AQ-02, AQ-05 |
| ADR relacionado | ADR-0005, ADR-0009 |
| Impacto | Reintentos tras un reinicio que duplican efectos; solicitantes legítimos rechazados; avances concurrentes del mismo estado |
| Recomendación | Agregar a ADR-0005 que todo estado «en curso» encontrado en la recuperación se trata como `UNKNOWN` o `CORE_UNKNOWN`. Separar `PENDING_CORE` en «por enviar» y «enviado». Agregar un estado o una regla para la indisponibilidad del proveedor KYC. Elevar el control de concurrencia de transiciones a regla independiente de la tecnología en ADR-0005 |
| Criterio de aceptación | Reglas de recuperación y de concurrencia en ADR-0005. Máquina de onboarding actualizada |
| Fase de resolución | Antes del diseño de componentes; las reglas, en la Fase 3 |
| Estado | Abierto |

### ARG01-M03 — Cache-Aside: carrera de escritura obsoleta y pérdida de la invalidación local

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | No se trata la carrera clásica de Cache-Aside: una lectura obtiene un valor del Core, una transferencia invalida la entrada y, después, la lectura escribe el valor ya obsoleto en la caché. Este valor sobrevive a la invalidación y queda hasta el TTL. Tampoco se define qué pasa si la invalidación síncrona local falla (caché no disponible, reinicio entre el commit y la invalidación). *Read-your-writes* se menciona sin mecanismo y sin cubrir varios dispositivos del mismo cliente. La ventana resultante queda acotada por TTL todavía no definidos |
| Evidencia | `docs/06-estrategia-de-cache.md:59`, `:60`, `:97`, `:108`; ADR-0004 |
| Requisito afectado | R5; QA2, QA3 |
| ADR relacionado | ADR-0004, ADR-0008 |
| Impacto | Saldo o movimientos obsoletos para visualización tras una transferencia confirmada (F07, F08). No afecta la integridad porque las decisiones no usan la caché, pero sí la confianza y los reclamos |
| Recomendación | Documentar la carrera y una mitigación conceptual (versionado o marca temporal de la entrada, escritura condicional, o ventana de supresión tras la invalidación). Política de reintento de la invalidación. Definir *read-your-writes* (por ejemplo, saltar la caché para las categorías afectadas durante una ventana tras una operación propia). Fijar los TTL de seguridad del saldo y de los movimientos recientes con negocio |
| Criterio de aceptación | `docs/06` cubre la carrera y la pérdida de la invalidación. Los TTL de las categorías sensibles están registrados como objetivos |
| Fase de resolución | Fase 3 |
| Estado | Abierto |

### ARG01-M04 — Decisión de resiliencia y plazos sin ADR; relación Gateway–plazo no definida

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | La frontera de resiliencia, la taxonomía de timeouts, la propagación del plazo y la regla «timeout ≠ falla» son decisiones fundamentales, pero solo viven en `docs/05`, sin ADR. No se define la relación entre el timeout de infraestructura del Gateway y el plazo de negocio: si el Gateway corta antes, el cliente recibe un error genérico mientras la operación sigue en curso. Tampoco se define qué recibe el cliente cuando el plazo vence con la operación en `UNKNOWN` |
| Evidencia | `docs/05-frontera-de-integracion-core.md:145`, `:163`, `:173`; listado de ADR (no existe un ADR equivalente) |
| Requisito afectado | C2; QA1, QA5 |
| ADR relacionado | ADR-0002, ADR-0005 (indirectos) |
| Impacto | Respuestas incoherentes al cliente y reintentos del canal (agrava C01) |
| Recomendación | Crear un ADR de resiliencia y plazos (numeración siguiente) que fije: timeout del Gateway ≥ plazo de negocio + margen; respuesta explícita «en verificación» (o un patrón asíncrono a decidir en la Fase 3) cuando el plazo vence con la operación en curso; precedencia entre plazo y timeouts locales |
| Criterio de aceptación | ADR aprobado y referenciado desde `docs/05` y ADR-0002 |
| Fase de resolución | Antes de cerrar la Fase 3 |
| Estado | Abierto |

### ARG01-M05 — El registro regulatorio definido por la política no existe

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | La política define los identificadores `REG-xx` y `REG-PV-xx` y obliga a registrar como `REG-PV` toda norma no verificada. **Ningún documento los usa** y no existe un registro. Las consideraciones regulatorias están dispersas en S8, S14, S15, ADR-0010 y C1: protección de datos personales y biometría, norma de notificación, aplicabilidad de PCI-DSS, prevención de lavado de activos y organismo de control |
| Evidencia | `docs/00-politica-de-documentacion.md:121-139`; búsqueda de `REG-[0-9]` sin resultados; `docs/09-trazabilidad-de-requisitos.md` (fila C1) |
| Requisito afectado | C1 |
| ADR relacionado | ADR-0010 |
| Impacto | C1 queda débil frente a un comité regulado. Incumplimiento de la propia política del repositorio |
| Recomendación | Crear un registro `REG-PV` con: consideración, tipo de entidad a la que aplicaría, fuente por verificar, decisiones afectadas, responsable de la validación. **Sin afirmar el cumplimiento** |
| Criterio de aceptación | Registro creado; S8, S14, S15, ADR-0010 y C1 referencian sus IDs |
| Fase de resolución | Antes de cerrar la Fase 3 |
| Estado | Abierto |

### ARG01-M06 — Prevención de fraude y de lavado de activos fuera del contexto y con prioridad baja

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | El contexto omite los sistemas de monitoreo transaccional, de prevención de fraude y de control de listas para la prevención de lavado de activos. Solo aparecen en Q-12, clasificada como de impacto **medio**. En una institución financiera regulada, estas integraciones suelen condicionar el onboarding y las transferencias. Su aplicabilidad concreta debe verificarse y no se afirma aquí, pero si existen, cambian el contexto y los contenedores |
| Evidencia | `docs/15-preguntas-abiertas-fase-2.md:39` (bajo «Impacto medio», `:30`); `docs/12-contexto-del-sistema.md` (sección 12, pregunta 3) |
| Requisito afectado | R1, R3; C1 |
| ADR relacionado | — |
| Impacto | Riesgo de rediseño de los contenedores de onboarding y de transferencias si se confirman tarde |
| Recomendación | Elevar Q-12 a impacto alto. Registrar la consideración como `REG-PV` (M05). Agregar los sistemas como **candidatos** en la vista ampliada |
| Criterio de aceptación | Q-12 reclasificada; candidatos en la vista ampliada o justificación documentada de su exclusión |
| Fase de resolución | Antes de cerrar la Fase 3 |
| Estado | Abierto |

### ARG01-M07 — REL-05 se presenta como confirmada aunque su ruta no lo está

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | En el DSL, REL-05 (Plataforma → Sistema Externo de Transferencias) tiene la etiqueta `Confirmada` y se dibuja con línea continua. La descripción y `docs/12` (sección 4.1) aclaran que es una dependencia lógica con la topología por validar. Visualmente, sin embargo, la vista principal comunica conectividad directa confirmada, que es justo lo que no debía presuponerse |
| Evidencia | `c4/workspace.dsl:165`; `docs/12-contexto-del-sistema.md:150` |
| Requisito afectado | R1; S13 |
| ADR relacionado | — |
| Impacto | Un lector del diagrama, sin el documento, inferirá una integración directa con la infraestructura de pagos |
| Recomendación | Usar una etiqueta y un estilo propios («Dependencia confirmada, ruta por validar») o la etiqueta `Supuesta`, y explicarlo en la leyenda |
| Criterio de aceptación | El estilo de REL-05 es distinto del de una relación confirmada y está explicado en la leyenda |
| Fase de resolución | Fase 2 (corrección menor) o inicio de la Fase 3 |
| Estado | Abierto |

### ARG01-M08 — Integration Platform SDK: acoplamiento por biblioteca subestimado

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | ADR-0003 afirma que «ningún cambio del SDK puede obligar a desplegar todos los dominios a la vez». Es una aspiración sin mecanismo. Dos casos la contradicen: (1) un **parche de seguridad** en el SDK o en sus dependencias obliga, en la práctica, a redesplegar todos los dominios; (2) los cambios de **formato de comunicación** (cabeceras de correlación o de plazo, modelo de error que viaja entre servicios) exigen coordinación aunque el binario sea compatible. No se distingue entre compatibilidad binaria y compatibilidad de comunicación, ni hay una política de soporte de versiones |
| Evidencia | `docs/adr/ADR-0003-integration-platform-sdk.md:60` |
| Requisito afectado | C5; AQ-08 |
| ADR relacionado | ADR-0003 |
| Impacto | Despliegues coordinados no planificados; el riesgo RR-02 queda subestimado |
| Recomendación | Reconocer explícitamente los redespliegues coordinados por seguridad. Separar la compatibilidad de comunicación (con contratos versionados y tolerancia a versiones N-1) de la compatibilidad binaria. Definir una ventana de soporte de versiones |
| Criterio de aceptación | ADR-0003 actualizado con estas distinciones y con la consecuencia aceptada |
| Fase de resolución | Fase 3 |
| Estado | Abierto |

### ARG01-M09 — Sin escenario de calidad para ataques al onboarding ni para el abuso de costos del proveedor KYC

| Campo | Contenido |
|---|---|
| Severidad | MEDIUM |
| Descripción | El único escenario de seguridad es QA4 (token robado). No hay escenarios para: ataques de presentación o de inyección de cámara (*deepfakes*, cámaras virtuales) contra el reconocimiento facial; automatización masiva del onboarding; **abuso de costos** (cada verificación del proveedor tiene costo y puede provocarse de forma maliciosa). R3 es el flujo de mayor exposición a fraude de identidad |
| Evidencia | `docs/04-escenarios-de-calidad.md` (QA1–QA8); `docs/01-drivers-arquitectonicos.md` (DF-10) |
| Requisito afectado | R2, R3; AQ-01, AQ-10 |
| ADR relacionado | — |
| Impacto | Los criterios de selección del proveedor (DD-14) y del control del borde quedan sin requisitos verificables |
| Recomendación | Agregar un escenario QA9 (ataque biométrico) y un QA10 (abuso automatizado y de costos del onboarding), con medidas parametrizadas |
| Criterio de aceptación | Escenarios registrados con seis partes y parámetros por acordar |
| Fase de resolución | Antes de la fase de seguridad; recomendable en la Fase 3 |
| Estado | Abierto |

### ARG01-L01 — Fronteras organizacional y de despliegue no explicitadas

| Campo | Contenido |
|---|---|
| Severidad | LOW |
| Descripción | `docs/14` distingue correctamente la frontera de software de la de confianza, pero no trata la **frontera organizacional** (quién opera y quién es responsable: equipo de la plataforma, TI de la institución, terceros) ni la de **despliegue** (dónde se ejecuta cada elemento). Esto importa, por ejemplo, para el IdP y para el Core |
| Evidencia | `docs/14-limites-de-confianza.md:13-18` |
| Requisito afectado | C4 |
| ADR relacionado | — |
| Impacto | Ambigüedad en las responsabilidades de operación y de respuesta a incidentes |
| Recomendación | Ampliar la tabla de la sección 1.1 con las cuatro fronteras y un ejemplo de cada una |
| Criterio de aceptación | Las cuatro fronteras están definidas con ejemplos del sistema |
| Fase de resolución | Fase 3 |
| Estado | Abierto |

### ARG01-L02 — Sin criterio operativo entre composición (BFF) y orquestación

| Campo | Contenido |
|---|---|
| Severidad | LOW |
| Descripción | ADR-0002 permite la «composición liviana mediante un BFF» y advierte sobre su posible degeneración, pero no da un criterio verificable que separe composición de orquestación |
| Evidencia | `docs/adr/ADR-0002-frontera-api-gateway.md:37`, `:69` |
| Requisito afectado | R1 |
| ADR relacionado | ADR-0002 |
| Impacto | Riesgo de que la lógica de negocio migre al BFF |
| Recomendación | Criterio sugerido: el BFF solo agrega **lecturas** independientes y adapta formatos; toda escritura, toda secuencia con dependencia de orden y toda regla de negocio son orquestación de dominio |
| Criterio de aceptación | Criterio incluido en ADR-0002 o en la decisión DD-10 |
| Fase de resolución | Fase 3 |
| Estado | Abierto |

### ARG01-L03 — Estado «Aceptada» autoasignado y sin registro de aprobación

| Campo | Contenido |
|---|---|
| Severidad | LOW |
| Descripción | Los ADR figuran como «Aceptada» con la misma fecha y sin un registro de quién los aprobó ni en qué revisión; ADR-0001 dice «sujeta a revisión del comité». El gobierno de ADR no distingue entre propuesta y aprobación |
| Evidencia | `docs/adr/*.md:5` |
| Requisito afectado | — (gobierno) |
| ADR relacionado | Todos |
| Impacto | Trazabilidad débil de la aprobación ante una auditoría |
| Recomendación | Agregar los campos «Aprobado por» y «Revisión» (por ejemplo, ARG-01) y aplicar el ciclo Propuesta → Aceptada después de cada gate |
| Criterio de aceptación | Plantilla de ADR con campos de aprobación; ADR actualizados tras la decisión del comité |
| Fase de resolución | Tras el cierre de ARG-01 |
| Estado | Abierto |

### ARG01-L04 — Commit fuera de la convención vigente

| Campo | Contenido |
|---|---|
| Severidad | LOW |
| Descripción | El commit `c1a56d7` («Refactorizacion de codigo para mantener un estandar en todo el proyecto») no tiene tipo ni alcance de Conventional Commits, omite las tildes y describe como «refactorización de código» un cambio exclusivamente documental. Este es justamente el commit que introdujo la convención de commits en español |
| Evidencia | `git log` (`c1a56d7`); `docs/00-politica-de-documentacion.md` (sección 6) |
| Requisito afectado | E4 (formato de entrega) |
| ADR relacionado | — |
| Impacto | Inconsistencia visible para el evaluador del repositorio |
| Recomendación | Corregir el mensaje (requiere reescribir el historial publicado) o dejar constancia y aplicar la convención desde ahora |
| Criterio de aceptación | Mensaje corregido o excepción documentada |
| Fase de resolución | Inmediata (decisión del autor) |
| Estado | Abierto |

### ARG01-L05 — Avisos de estilo de markdownlint

| Campo | Contenido |
|---|---|
| Severidad | LOW |
| Descripción | markdownlint v0.42 reporta 822 avisos MD060 (estilo de las columnas de tabla: separadores compactos sin espacios) y 3 MD036 (énfasis usado como encabezado). No afectan el contenido ni el renderizado en GitHub |
| Evidencia | V9; `docs/12-contexto-del-sistema.md:44`, `:50`, `:69` (MD036) |
| Requisito afectado | — |
| ADR relacionado | — |
| Impacto | Ruido en una futura validación automática en CI |
| Recomendación | Desactivar MD060 en `.markdownlint.json` o normalizar las tablas. Convertir los tres énfasis en encabezados |
| Criterio de aceptación | `markdownlint-cli2` sin errores |
| Fase de resolución | Antes de incorporar la validación en CI |
| Estado | Abierto |

## 7. Escenarios de falla

| ID | Condición inicial | Evento de falla | Comportamiento esperado (documentado) | Decisión | Riesgo residual | Evidencia | Cobertura | Recomendación |
|---|---|---|---|---|---|---|---|---|
| F01 | Operación normal | El Core no está disponible | Lecturas degradadas a datos aprobados con indicador de frescura; escrituras con falla segura y sin encolar | ADR-0004, ADR-0005; `docs/05` §7 | TTL no definidos; política de visualización del saldo por decidir | `docs/05` §7; `docs/06` §3 | **CUBIERTO** (conceptual) | Fijar los TTL de seguridad (M03) |
| F02 | Transferencia en `CORE_PROCESSING` | El Core confirma y la respuesta se pierde | `UNKNOWN`, sin reintento, conciliación por referencia o correlación; el cliente ve «en verificación» | ADR-0005, ADR-0006 | Conciliación probabilística (RR-04) | `docs/05` §6; `docs/08` §4 | **CUBIERTO** con riesgo residual | Priorizar Q-02 (S3) |
| F03 | Transferencia enviada por el cliente | El cliente no recibe respuesta y reenvía | No definido de forma determinista: la clave del canal no es obligatoria | ADR-0006 | **Débito duplicado** | `docs/08:22`, `:30` | **BRECHA** | C01 |
| F04 | Clave usada en una operación previa | Misma clave con otro payload | **Sin evidencia** de comportamiento | — | Ejecución ambigua o sobrescritura | — | **BRECHA** | C01 (huella del payload) |
| F05 | Onboarding en `VALIDATING` | El proveedor biométrico no responde | No se distingue indisponibilidad de rechazo; sin transición definida | ADR-0009 (parcial) | Rechazo de legítimos o solicitudes colgadas | `docs/07:82`, `:95` | **BRECHA** | M02 |
| F06 | Onboarding en `PENDING_CORE` | El Core crea el cliente y falla la actualización local | Conciliación periódica de `PENDING_CORE` frente al Core | ADR-0009 | `PENDING_CORE` ambiguo; un cliente preexistente puede confundirse con uno creado | `docs/07` §4.4 | **PARCIAL** | M02, H05 |
| F07 | Saldo en caché para visualización | Saldo obsoleto tras una transferencia | Nunca se usa para decidir; invalidación local y *read-your-writes* | ADR-0004 | Carrera de Cache-Aside; *read-your-writes* sin mecanismo | `docs/06:59`, `:97` | **PARCIAL** | M03 |
| F08 | Invalidación dirigida por eventos | El evento no llega | El TTL de seguridad acota la ventana | ADR-0008 | Ventana igual a un TTL no definido | `docs/06` §4.4 | **PARCIAL** | M03 |
| F09 | Interbancaria en `SPI_PROCESSING` | El sistema externo no responde | `UNKNOWN` y conciliación | ADR-0005 | Débito previo sin estado de reverso; `COMPLETED` sin semántica | `docs/07:50` | **PARCIAL** | H02, Q-01 |
| F10 | Gateway operativo | Servicio de dominio caído | Error controlado; el Gateway no reintenta escrituras; funciones no relacionadas operativas | ADR-0002; QA1 | Recuperación de operaciones en curso del servicio caído no especificada | `docs/05` §5.1; `docs/07:13` | **PARCIAL** | M02 (recuperación), M04 |
| F11 | Token válido sustraído | Uso desde otro dispositivo | Una operación de alto riesgo exige una prueba adicional; el intento se rechaza y se audita | QA4; DD-04 | Alto hasta la fase de seguridad | `docs/04` QA4 | **PLANIFICADO PARA FASE POSTERIOR** (mecanismo diferido legítimamente) | Mantener QA4 como criterio de selección |
| F12 | Dos solicitudes concurrentes | Misma operación ejecutada en paralelo | Unicidad de la clave en el almacén. Sin clave de canal obligatoria, dos solicitudes generan dos claves. Concurrencia de transiciones solo en el perfil condicional | ADR-0006 | Duplicación (C01) y doble avance de estado (M02) | `docs/08:32`; `docs/00-estandares…:170` | **BRECHA** (parcial) | C01, M02 |

## 8. Evaluación C4 (nivel 1)

| Criterio | Evaluación | Evidencia |
|---|---|---|
| Sistema principal identificado | Correcto: un único sistema; SPA y app como futuros contenedores | `c4/workspace.dsl` (`plataforma`); `docs/12` §2 |
| Actores reales | Correcto. Separar «Solicitante» de «Cliente» está bien justificado. Los candidatos están aislados | `docs/12` §3 |
| Clasificación de los sistemas externos | Correcta: existentes, infraestructura financiera, terceros, candidatos | `docs/12` §4 |
| Core Bancario | Externo y autoritativo; la base de datos principal no se modela (S12) | `docs/12` §4 |
| Sistema Complementario | Separado y no autoritativo para la identidad | `docs/12` §4; ADR-0009 |
| Transferencias interbancarias | Capacidad lógica con tres modelos y sin integración directa con el SPI. **Defecto visual:** REL-05 como «Confirmada» (M07) | `docs/12` §4.1; DSL `:165` |
| KYC/biometría | Sin proveedor seleccionado; relación «Supuesta» (S16) | DSL |
| Identity Provider | No se presupone externo; candidato con frontera por decidir antes de la Fase 3 | `docs/12` §4.2; DD-03 |
| Notificaciones | Proveedores externos supuestos; entrega hacia las personas modelada | DSL REL-07 a REL-09 |
| Fuente oficial de identidad | Candidata, sin conectividad inventada entre sistemas externos | `docs/12` §4.3 |
| Ausencia de componentes y tecnologías | Correcta (verificado: sin `container` ni `component` en el DSL) | V3 |
| Consistencia con el catálogo de integraciones | Correcta. INT-08, INT-10 e INT-15 a INT-17 están marcadas «Por validar» de forma coherente | `docs/13` |
| Fronteras de software, confianza, organización y despliegue | Las dos primeras están bien distinguidas; las dos últimas no (L01) | `docs/14` §1.1 |
| Sistemas posiblemente faltantes | Prevención de fraude y de lavado de activos (M06); SIEM institucional (Q-13) | `docs/15` |

**Conclusión C4:** el modelo es técnicamente correcto y defendible. Los defectos son de comunicación (M07) y de completitud de candidatos (M06), no de abstracción.

## 9. Evaluación de los ADR

| ADR | Estructura completa | Justificación contextual | Observaciones del comité | Hallazgos |
|---|---|---|---|---|
| ADR-0001 | Sí | Sí. Opción B tratada con equidad; S11 explícito | No analiza el presupuesto de capacidad compartido del Core ni distingue el aislamiento lógico del físico | H04 |
| ADR-0002 | Sí | Sí. Interpretación de «orquestar» transparente y declarada | Interpretación razonable y **no silenciosa**. Falta un criterio para distinguir composición de orquestación | L02 |
| ADR-0003 | Sí | Sí | La garantía de que no hay despliegue coordinado es aspiracional | M08 |
| ADR-0004 | Sí | Sí. Ningún TTL arbitrario | No trata la carrera de Cache-Aside | M03 |
| ADR-0005 | Sí | Sí. Argumentos sólidos contra «Saga» | `FAILED` contradictorio; sin reglas de recuperación ni de concurrencia; taxonomía de fallas ambigua | H01, H02, M02 |
| ADR-0006 | Sí | Parcial | **Hueco crítico:** identidad lógica ante los reintentos del cliente y vinculación con el payload | **C01**, M01 |
| ADR-0007 | Sí | Sí. Disparadores etiquetados como propuestos | Ejemplar en el tratamiento de una decisión diferida | — |
| ADR-0008 | Sí | Sí. Sin selección prematura del *broker* | Correcto | — |
| ADR-0009 | Sí | Sí | La conciliación por identificador no distingue preexistente de creado | H05 |
| ADR-0010 | Sí | Sí. Sin afirmaciones legales; SCC tratadas correctamente | Correcto como «Propuesta». Falta su ID `REG-PV` | M05 |

**Decisiones sin ADR:** frontera de resiliencia y propagación de plazos (M04).

**ADR contradictorios o duplicados:** ninguno. La única tensión es el nombre `SPI_PROCESSING` frente al S13 revisado (H02).

**Decisiones diferidas presentadas como definitivas:** ninguna.

**Supuestos presentados como hechos:** ninguno. El rigor en este punto es alto.

**Tecnologías sin justificación:** ninguna. El perfil Java/Quarkus está correctamente condicionado a DD-19.

## 10. Evaluación de seguridad (STRIDE conceptual)

| Categoría | Riesgo identificado | Estado en el diseño actual | Resolución |
|---|---|---|---|
| **Spoofing** (suplantación) | Token robado (QA4); ataques de presentación o inyección biométrica en el onboarding; enrolamiento fraudulento sobre un cliente existente; SIM swap si se usa OTP por SMS | QA4 identificado. Biometría sin escenario (M09). Vector de toma de cuentas (H05) | Fase de seguridad. H05 y M09 antes |
| **Tampering** (manipulación) | Alteración de las órdenes de transferencia; manipulación de la app en un dispositivo no confiable; integridad de la auditoría | La validación en el servidor y la desconfianza hacia el cliente están documentadas (TB-1). La integridad de la auditoría se exige en QA6, con mecanismo pendiente | Fase de seguridad y diseño de la auditoría (DD-11) |
| **Repudiation** (repudio) | El cliente niega haber autorizado una transferencia; sin evidencia de autenticación reforzada vinculada a la operación | La auditoría de las transiciones está documentada. La evidencia de autorización por operación no está definida | Fase de seguridad (DD-04) |
| **Information disclosure** (divulgación) | Datos personales en la caché; contenido de notificaciones por SMS y correo; datos biométricos en terceros; registros; **enumeración de clientes** durante el onboarding (respuestas que revelan si una identificación ya es cliente) | Caché, registros y notificaciones cubiertos con minimización. Terceros cubiertos (RR-11). La enumeración no está identificada | Agregar la enumeración al alcance de la fase de seguridad |
| **Denial of service** (denegación de servicio) | Saturación del Core por tráfico de lectura; abuso del onboarding con costo por verificación | Protección del Core incompleta ante el escalado (H04). Abuso de costos sin escenario (M09) | H04 en la Fase 3; M09 |
| **Elevation of privilege** (escalada de privilegios) | Autorización a nivel de objeto (acceso a cuentas de terceros cambiando identificadores en la API); actores internos con capacidad de resolver conciliaciones | DF-08 menciona el «control por cliente y por operación», pero la verificación de propiedad de cuenta y beneficiario no está definida. Actores internos candidatos (DD-18) | Fase de seguridad; segregación de funciones en la resolución manual (H03) |

**Fraude transaccional:** no hay una capacidad de monitoreo de fraude identificada (M06). Los límites, la gestión de beneficiarios y los periodos de enfriamiento no se han tratado. Corresponden a la fase de seguridad, pero deberían registrarse como drivers.

## 11. Riesgos residuales

### 11.1 Registro existente (RR-01 a RR-12)

El registro es honesto y está bien estructurado. Observaciones del comité:

- **RR-02** (SDK): subestimado por M08.
- **RR-04** (conciliación): correctamente calificado, pero QA3 lo contradice (M01).
- **RR-10** (topología): correcto; debe incluir la contradicción de estados (H02).

### 11.2 Riesgos identificados por esta revisión (propuestos para el registro)

| Riesgo propuesto | Origen | Severidad estimada |
|---|---|---|
| Duplicación por reintentos del cliente sin identidad lógica estable | C01 | Alta, hasta la enmienda de ADR-0006 |
| Saturación del Core por escalado horizontal sin presupuesto global | H04 | Alta |
| Toma de cuentas mediante el onboarding de clientes preexistentes | H05 | Alta |
| Operaciones en `REQUIRES_RECONCILIATION` sin responsable | H03 | Media-alta |
| Obligaciones de prevención de fraude o de lavado de activos descubiertas tarde | M06 | Media |

Este comité **no modifica** `docs/11`. La incorporación corresponde al autor (plan de remediación).

## 12. Recomendaciones

### 12.1 Bloqueantes (antes de la Fase 3)

1. Enmendar **ADR-0006** según C01. Ajustar QA3 (M01).

### 12.2 Condiciones de la Fase 3

2. Precisar en ADR-0005 la taxonomía de fallas (H01), el modelo de reverso interbancario (H02), la recuperación y la concurrencia (M02).
3. Completar ADR-0001 con el presupuesto de capacidad del Core (H04).
4. Corregir la conciliación del onboarding en ADR-0009 (H05).
5. Confirmar el responsable y el plazo de la conciliación manual (H03, DD-18).

### 12.3 Mejoras planificadas

6. Crear un ADR de resiliencia y plazos (M04).
7. Crear el registro `REG-PV` (M05).
8. Reclasificar Q-12 (M06).
9. Agregar los escenarios QA9 y QA10 (M09).

### 12.4 Correcciones menores

10. Etiqueta de REL-05 (M07).
11. Fronteras de organización y de despliegue (L01).
12. Criterio BFF (L02).
13. Gobierno de los ADR (L03).
14. Commit (L04).
15. Configuración de markdownlint (L05).

### 12.5 Objetivos candidatos para los escenarios de calidad

> **CANDIDATOS DE INGENIERÍA — PENDIENTES DE VALIDACIÓN.** No deben adoptarse como requisitos sin la confirmación de la institución. Se proponen solo para iniciar la conversación y para dar verificabilidad a los escenarios.

| Parámetro | Candidato | Justificación |
|---|---|---|
| `T_CONSULTA_P95` | ≤ 1 s medido en el borde de la plataforma, sin incluir la red del cliente | Referencia habitual de experiencia de consulta en canales digitales |
| `DISP_FUNC_NO_AFECTADAS` | ≥ 99,9 % mensual | Valor de partida usual para canales digitales críticos |
| `T_DETECCION_FALLA` | ≤ 30 s | Compatible con ventanas típicas del Circuit Breaker |
| `T_APERTURA_CONCILIACION` | ≤ 5 min | Debe ser menor que la percepción de «operación perdida» del cliente |
| `T_RESOLUCION_UNKNOWN` | ≤ 30 min automático; escalamiento a revisión manual después | A calibrar con el tiempo de asentamiento del Core |
| `T_RESOLUCION_MANUAL` (nuevo, H03) | Por acordar con operaciones | No hay base para proponer un valor |
| `RATIO_LECTURA_CORE` | **No se propone un valor** | Requiere una medición de referencia del tráfico actual |
| RPO del almacén de estado transaccional | Cercano a cero (objetivo derivado, por validar) | Ese almacén conserva la evidencia de las operaciones en curso; perderla impide la conciliación. Es independiente del RPO general del canal |
| RTO y RPO generales | **No se proponen** | Dependen del BIA (DD-09) |

## 13. Decisión del gate

### Veredicto: **NO-GO**

**Fundamento:**

1. **Regla aplicada:** «Un hallazgo CRITICAL abierto implica NO-GO». **ARG01-C01 está abierto.** Afecta a la decisión fundamental de integridad de la Fase 1 (ADR-0006) en el modo de falla más probable de un canal digital, y su corrección condiciona los contratos de API de la Fase 3. Iniciar la Fase 3 sin cerrarlo propagaría el defecto al diseño de contenedores y de API.
2. Los HIGH H01 a H05 **no comprometen la dirección** de las decisiones fundamentales: la ACL por dominio, la máquina de estados, el Core autoritativo y la interpretación del Gateway siguen siendo correctas. Son precisiones y completitud que se pueden resolver como **condiciones** dentro de la Fase 3. Por sí solos habrían justificado un CONDITIONAL GO.
3. **La puntuación no fue el criterio:** un 7,0 / 10 sería compatible con un CONDITIONAL GO. El veredicto lo determina el CRITICAL.

**Camino a GO:**

1. El autor enmienda ADR-0006 y `docs/08` (C01) y QA3 (M01), con **autorización humana**, porque modifica un ADR aprobado.
2. El comité realiza una revisión focalizada (ARG-01a) solo sobre esos cambios.
3. Si se cierra C01, el comité anticipa un **CONDITIONAL GO**, con H01 a H05 como condiciones de la Fase 3 según el [plan de remediación](ARG-01-plan-de-remediacion.md).

---

*ARG-01 completado. Pendiente de revisión y aprobación humana antes de iniciar la Fase 3.*
