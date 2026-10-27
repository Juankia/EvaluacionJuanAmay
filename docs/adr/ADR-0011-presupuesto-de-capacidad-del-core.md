# ADR-0011 — Presupuesto global de capacidad del Core y control de admisión por clase de operación

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (remediación ARG-01; resuelve ARG01-H04). El mecanismo dinámico de cuotas se difiere (DD-20). Pendiente de verificación en ARG-01-R2 |
| Fecha | 2026-10-27 |
| Requisitos | C2, C5; R1 |
| Drivers y supuestos | S4, S10; AQ-02, AQ-03, AQ-06, AQ-07; QA2, QA8, QA10 |
| Relacionados | [ADR-0001](ADR-0001-acl-por-dominio.md), [ADR-0002](ADR-0002-frontera-api-gateway.md), [ADR-0003](ADR-0003-integration-platform-sdk.md), [ADR-0004](ADR-0004-cache-aside.md), [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [ARG-01](../reviews/ARG-01-revision-arquitectonica.md) |

## Contexto

[ADR-0001](ADR-0001-acl-por-dominio.md) adopta ACL por dominio con límites de concurrencia y pools propios. La revisión ARG-01 (H04) señaló dos debilidades:

1. Las ACL dan **aislamiento lógico** (del modelo, del código, del ciclo de liberación y de los recursos de la plataforma), pero **no aíslan la capacidad del Core físico**, que es un recurso compartido. Una avalancha de consultas de movimientos puede saturar el Core y degradar las transferencias, aunque cada ACL tenga su Bulkhead.
2. Los límites por instancia **se multiplican** con el número de réplicas. Con escalado horizontal (QA8), la concurrencia total hacia el Core puede superar su capacidad.

Una capa de integración centralizada (opción B de ADR-0001) resolvería este punto de forma natural. Esta decisión explica cómo se obtiene la misma protección **sin** abandonar la opción C.

## Problema

¿Cómo se garantiza que la carga total hacia el Core no supere su capacidad, para cualquier número de réplicas, y que las operaciones críticas no se degraden por las consultas, sin introducir un nuevo punto único de indisponibilidad?

## Decisión

### 1. Presupuesto global y clases de operación

- La capacidad del Core se expresa como un **presupuesto global**: concurrencia máxima `MAX_CONCURRENCIA_CORE` y, si corresponde, una tasa máxima `TASA_MAX_CORE`. **El presupuesto lo fija el propietario del Core** (S4, Q-16); no es un valor de la plataforma.
- El presupuesto se reparte en **clases de operación** con cuotas reservadas:

| Clase | Operaciones | Cuota | Prioridad ante contención |
|---|---|---|---|
| **CL-1 Escrituras críticas** | Ejecución de transferencias, creación de clientes, reversos | `CUOTA_CL1`, **reservada; no la toma prestada ninguna otra clase** | 1 |
| **CL-2 Consultas de integridad** | Consultas de estado y búsquedas para conciliación, búsqueda de cliente en la vinculación | `CUOTA_CL2`, **reservada** | 2 |
| **CL-3 Consultas interactivas** | Datos del cliente, cuentas, saldos y movimientos en línea | `CUOTA_CL3` | 3 |
| **CL-4 Consultas diferibles** | Precarga, refrescos y procesos no interactivos | `CUOTA_CL4` (mínima), y puede usar la capacidad ociosa de CL-3 | 4 |

- La suma de cuotas ≤ presupuesto global, con un margen de seguridad `MARGEN_CORE`.
- CL-2 se protege porque la **conciliación de los resultados inciertos es parte de la integridad**: sin capacidad para consultar, las operaciones `*_UNKNOWN` no se resuelven.

### 2. Respeto del presupuesto con N réplicas

| Mecanismo | Descripción | Rol |
|---|---|---|
| **M1. Asignación estática derivada (obligatorio)** | Cada réplica recibe, por clase, `cuota_clase / N_MAX_REPLICAS`, donde `N_MAX_REPLICAS` es el **máximo de réplicas configurado** para el servicio. El escalado automático **no puede** superar `N_MAX_REPLICAS` sin recalcular las cuotas. Así, la concurrencia total ≤ presupuesto **para cualquier N ≤ N_MAX_REPLICAS**, sin depender de ningún servicio de coordinación | **Piso de seguridad**; siempre activo |
| **M2. Cuotas dinámicas coordinadas (diferido, DD-20)** | Las réplicas obtienen cuotas arrendadas (*leases*) de un mecanismo de control de admisión distribuido, para aprovechar la capacidad que M1 deja ociosa cuando N es bajo | Optimización. Solo si las mediciones demuestran una subutilización relevante |

**Ejemplo ilustrativo** (los valores **no son** objetivos). Con un presupuesto CL-3 de 100 y `N_MAX_REPLICAS` = 20:

- con M1, cada réplica admite 5 consultas concurrentes;
- con 2 réplicas activas solo se usan 10 de 100, una protección completa con subutilización;
- con 20 réplicas se usan 100 de 100, sin superar el presupuesto.

M2 solo se justificaría si esa subutilización produce rechazos medibles con N bajo.

### 3. Control de admisión, presión de retorno y Bulkheads

- **Bulkheads locales:** por réplica, por ACL y por clase (pools y límites separados). Las escrituras críticas nunca comparten pool con las consultas.
- **Bulkhead global:** las cuotas por clase de la sección 1, aplicadas mediante M1 (y M2 si se activa).
- **Control de admisión** (*admission control*) en la ACL, antes de invocar al Core:
  - **CL-3 y CL-4:** si la cuota está agotada, se rechaza de inmediato y el servicio de dominio se degrada a los datos de caché aprobados ([ADR-0004](ADR-0004-cache-aside.md)) o responde «servicio ocupado».
  - **CL-1:** espera acotada dentro del plazo de negocio. Si no obtiene cupo, la operación termina como `FAILED_NO_EFFECT` con clase `NOT_SENT` ([ADR-0005](ADR-0005-maquina-de-estados-transaccional.md)). Es seguro porque no se envió nada.
  - **CL-2:** espera acotada. Si no hay cupo, la conciliación se reprograma; no se pierde la operación.
- **Presión de retorno** (*backpressure*): las ACL exponen señales de saturación (rechazos por clase, latencia del Core). Esas señales alimentan la **limitación de tasa** del borde (por cliente y por tipo de operación), que es una función de infraestructura del Gateway ([ADR-0002](ADR-0002-frontera-api-gateway.md)) y no introduce lógica de negocio en él.
- **Limitación de tasa distribuida:** solo cuando una limitación por cliente debe ser coherente entre réplicas (por ejemplo, abuso desde una misma identidad). El mecanismo se decide en la fase de infraestructura y seguridad.
- **Adaptación a la salud del Core** (opcional, posterior): reducir las cuotas efectivas cuando aumenta la latencia del Core.

### 4. Comportamiento seguro ante la falla del mecanismo de control

- Si M2 (coordinación) no está disponible, cada réplica **vuelve a su cuota estática M1**. Nunca se pasa a «sin límite» (*fail-open*).
- Una falla del mecanismo de control **no** debe impedir las operaciones CL-1 y CL-2 dentro de la cuota M1. **El control de admisión no es un punto único de indisponibilidad.**
- M1 no tiene dependencias en tiempo de ejecución: es configuración.

### 5. Aislamiento lógico frente a aislamiento de capacidad

| Tipo de aislamiento | Lo proporciona |
|---|---|
| Modelo, código, ciclo de liberación y propiedad por dominio | ACL por dominio ([ADR-0001](ADR-0001-acl-por-dominio.md)) |
| Recursos de la plataforma (hilos, conexiones) | Bulkheads locales |
| **Capacidad del Core compartido** | **Este ADR** (presupuesto global, cuotas por clase, control de admisión) |
| Disponibilidad física del Core | **No lo proporciona la plataforma.** Es una dependencia externa (S4) |

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Límites independientes por ACL y por instancia (versión implícita de la Fase 1) | **Descartada** (ARG01-H04) | No respeta el presupuesto con escalado ni protege las escrituras de las consultas |
| Capa de integración centralizada (opción B) como punto de control | No seleccionada | Contradice ADR-0001. Si la institución ya tiene un intermediario frente al Core que impone límites globales, se **aprovecha** como control adicional (validar con Q-16) |
| Solo cuotas dinámicas coordinadas (M2) | Descartada como base | Introduce un punto único de falla y latencia. Se mantiene como optimización diferida, con M1 como respaldo |
| Solo limitación de tasa en el Gateway | Insuficiente | El Gateway no conoce la clase de la operación hacia el Core ni la concurrencia real. Es complementaria |
| **Presupuesto global + clases + M1 obligatorio + M2 diferido** | **Seleccionada** | Protección garantizada sin dependencias en tiempo de ejecución |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Concurrencia total acotada para cualquier N ≤ `N_MAX_REPLICAS` | Subutilización del Core cuando N es bajo (M1) |
| Escrituras y conciliación protegidas de las consultas | Más configuración y gobierno de cuotas |
| Sin dependencia en tiempo de ejecución para la protección base | El escalado tiene un techo explícito (`N_MAX_REPLICAS`) |
| Se conserva la opción C de ADR-0001 | Se reconoce una ventaja de la opción B que debe compensarse con disciplina de configuración |

## Consecuencias

- `MAX_CONCURRENCIA_CORE` (y `TASA_MAX_CORE`) deben **acordarse con el propietario del Core** antes de cerrar la Fase 3 (Q-16).
- Cada servicio declara `N_MAX_REPLICAS` y sus cuotas por clase. El escalado automático se configura con ese techo.
- QA8 y QA10 verifican el presupuesto con escalado ([04](../04-escenarios-de-calidad.md)).
- Nuevos parámetros por acordar: `TASA_MAX_CORE`, `CUOTA_CL1` a `CUOTA_CL4`, `MARGEN_CORE`, `N_MAX_REPLICAS`.

## Riesgos

- **RR-14:** cuotas mal dimensionadas, que producen rechazos artificiales (demasiado bajas) o no protegen (demasiado altas o con un `N_MAX_REPLICAS` desactualizado). La coordinación dinámica, si se activa, es un posible punto de falla (mitigado por el respaldo M1).
- **Dependencia del dato de capacidad del Core:** si el propietario no puede informarlo, se requieren pruebas de capacidad controladas.

## Actualización de la Fase 3

La regla M1 se corrigió en el diseño de contenedores para considerar `N_EFECTIVO_MAX` (réplicas adicionales de despliegue, solapamiento blue/green, réplicas en terminación y réplicas temporales), el reparto de cada clase entre servicios y los requisitos RI-01 a RI-07 ([19 §3](../19-comunicacion-entre-contenedores.md#3-protección-de-la-capacidad-del-core-adr-0011-y-r2-n06); R2-N06, R2-N07). Pendiente de verificación en ARG-02.
