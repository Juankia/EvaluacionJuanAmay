# 19 — Comunicación entre contenedores y protección de la capacidad del Core

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Pendiente de ARG-02 |
| ADR principales | [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md), [ADR-0015](adr/ADR-0015-mensajeria-asincrona-con-outbox.md), [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) |

## 1. Interacciones

| Origen → destino | Estilo | Contrato | Plazo y timeouts | Reintentos | Idempotencia |
|---|---|---|---|---|---|
| SPA → Gateway → BFF | Síncrono | HTTPS/JSON; sesión con cookie segura | Timeout del Gateway ≥ plazo de negocio + margen (M04, pendiente de ADR) | **Lecturas:** el cliente puede reintentar. **Inicio de transferencia:** solo con la misma clave | El canal genera la clave (IC-02) |
| App → Gateway → BFF | Síncrono | HTTPS/JSON; token OAuth 2.0 | Igual | Igual | Igual |
| BFF → servicios de dominio | Síncrono | HTTPS/JSON interno, autenticado entre servicios ([22](22-seguridad-entre-contenedores.md)) | Propaga el plazo (concepto `X-Request-Deadline`, DD-06) | **Ninguno** en los inicios de transferencia; lecturas con un reintento acotado | El BFF **propaga** `Idempotency-Key` **sin cambios** (PT-04) |
| Servicios → Core (ACL) | Síncrono | Protocolo del Core (por validar, S3) | Timeouts de conexión y de lectura por operación; plazo propagado | Lecturas: limitado, con espera exponencial. **Escrituras: ninguno**; permiso único (INV-T3); **reintentos automáticos de la biblioteca cliente desactivados** (PT-06) | Referencia de correlación (IC-13) |
| Vinculación → KYC | Síncrono | API del proveedor | Por operación | Limitado; la indisponibilidad no es un rechazo (VD-09) | Referencia de solicitud |
| Servicios → broker | Asíncrono (relé del outbox) | Eventos versionados | — | El relé reintenta (al menos una vez) | Identificador de evento |
| Broker → notificaciones, auditoría, consultas | Asíncrono | Suscripciones | — | Reintento y cola de mensajes fallidos | Consumidores idempotentes |
| Consola → servicios | Síncrono | API de operaciones (separada de la API de canal) | — | Ninguno en las resoluciones | Cada resolución es una transición cercada |

**Contratos:** cada API se versiona y se describe con OpenAPI; cada evento, con un esquema versionado. Los nombres de los campos van en inglés (por ejemplo, `operationId`, `idempotencyKey`, `status`).

## 2. Integración con el Core: parámetros por ACL

| Aspecto | Regla |
|---|---|
| **Pool de conexiones** | Un pool **dedicado por ACL y por clase de operación**. Las escrituras (CL-1) nunca comparten pool con las lecturas |
| **Concurrencia** | Límite por réplica, por clase y por servicio, derivado del presupuesto global (sección 3) |
| **Timeouts** | Timeout de conexión corto; timeout de lectura por operación; ninguno supera el plazo propagado |
| **Circuit Breaker** | Por ACL y por operación. Con el circuito abierto, el intento es `NOT_SENT`, y en las escrituras eso se resuelve **antes** de conceder el permiso de envío |
| **Presión de retorno** | Señales de saturación (rechazos por clase, latencia del Core) hacia la limitación de tasa del Gateway |
| **Restricciones de reintento** | Lecturas: limitado. Escrituras: **ninguno**, incluidos los reintentos ocultos de la biblioteca o de intermediarios (PT-06, S19) |
| **Fallas de conexión** | Se clasifican con la taxonomía cerrada (ADR-0005 §2). En caso de duda, `UNKNOWN` |
| **Mapeo de datos** | Solo dentro de la ACL; códigos del Core → errores de dominio; tablas de clasificación EV-REJ por contrato |
| **Dueño del contrato** | El equipo del servicio que contiene la ACL. Pruebas de contrato por ACL (QA7) |
| **Aislamiento de la dependencia** | Las ACL no exponen tipos del Core fuera del servicio (S6) |

## 3. Protección de la capacidad del Core (ADR-0011 y R2-N06)

### 3.1 Cinco mecanismos distintos

| Mecanismo | Dónde | Qué protege | Qué no protege |
|---|---|---|---|
| **Bulkhead local** | Cada réplica, por ACL y por clase | Los recursos de la réplica (hilos, conexiones) | El Core ante la suma de réplicas |
| **Presupuesto global del Core** | Configuración: `Q_c` por clase, repartido por servicio (`Q_{c,s}`) | **La capacidad del Core compartido** | — |
| **Limitación de tasa en el borde** | API Gateway, por cliente y por tipo de operación | El abuso y la avalancha de entrada | La distribución interna entre clases |
| **Priorización de cargas** | Cuotas reservadas: CL-1 > CL-2 > CL-3 > CL-4 | Las escrituras y la conciliación frente a las consultas | — |
| **Control de admisión** | ACL, antes de cada invocación | El rechazo temprano y seguro (`NOT_SENT` antes del permiso) | — |

### 3.2 Reparto del presupuesto entre consumidores

| Clase | Consumidores | Cuota |
|---|---|---|
| CL-1 (escrituras críticas) | Transferencias (ejecución y reversos); vinculación (alta) | `Q_{CL1,transferencias}` + `Q_{CL1,vinculación}` ≤ `CUOTA_CL1` |
| CL-2 (consultas de integridad) | Conciliación de transferencias; búsqueda y conciliación de la vinculación | `Q_{CL2,transferencias}` + `Q_{CL2,vinculación}` ≤ `CUOTA_CL2` |
| CL-3 (consultas interactivas) | Servicio de consultas | `CUOTA_CL3` |
| CL-4 (diferibles) | Servicio de consultas (precarga) | `CUOTA_CL4` |

Los **roles de conciliación** son consumidores de CL-2 dentro de su servicio y cuentan en sus réplicas.

### 3.3 Regla de asignación estática (M1) corregida

Para cada servicio *s* y clase *c*:

```text
q_{c,s} (por réplica) = floor( Q_{c,s} / N_EFECTIVO_MAX_s )

N_EFECTIVO_MAX_s = N_MAX_s
                 + réplicas adicionales máximas del despliegue rolling (surge)
                 + réplicas de la flota inactiva en blue/green, si tiene admisión abierta
                 + réplicas en terminación que aún mantienen solicitudes
                 + réplicas temporales (tareas, reprocesos) que usan la ACL
```

**Garantía:** si `Σ_s Q_{c,s} ≤ Q_c` y en todo momento el número de réplicas con admisión abierta es ≤ `N_EFECTIVO_MAX_s`, la concurrencia total de la clase *c* hacia el Core es ≤ `Q_c`.

### 3.4 Requisitos de infraestructura (RI-01 a RI-07, de ARG-01-R2)

| ID | Requisito | Cómo se expresa en la Fase 3 |
|---|---|---|
| RI-01 | `N_MAX_REPLICAS` es un límite automatizado y verificable | Techo del escalado automático declarado por servicio; un escalado por encima del techo es imposible por configuración |
| RI-02 | La cuota se calcula sobre `N_EFECTIVO_MAX`, o la estrategia de despliegue se restringe | **Rolling:** el surge se incluye en `N_EFECTIVO_MAX`. **Blue/green:** la flota nueva arranca con **admisión hacia el Core cerrada** (interruptor de configuración) y la abre solo después de cerrar la de la flota anterior. **Terminación:** cierre ordenado de la admisión antes de retirar la réplica |
| RI-03 | `Σ_s Q_{c,s} ≤ Q_c` verificado automáticamente **y** `q_{c,s} ≥ 1` por réplica para toda clase que el servicio use (*enmienda ARG02-M03*). Si `Q_{c,s} < N_EFECTIVO_MAX_s`, la configuración se rechaza: hay que reducir `N_MAX` o aumentar la cuota | Validación de configuración en el pipeline de despliegue; un despliegue que la viole se rechaza |
| RI-04 | Protocolo de cambio de `N_MAX` | Para subir el techo, primero se bajan las cuotas por réplica y luego se sube el techo; para bajarlo, en el orden inverso |
| RI-05 | Arrendamientos de M2 con vencimiento | Solo si se activa M2 (DD-20) |
| RI-06 | Control de respaldo en tiempo de ejecución | Métrica de la concurrencia real hacia el Core con alerta cerca del presupuesto |
| RI-08 | **Presupuesto de conexiones** (sesiones del Core) además del de concurrencia: `Σ réplicas × (conexiones mínimas + máximas del pool) ≤ SESIONES_MAX_CORE` (*nuevo, ARG02-M03*) | Validación en el pipeline junto con RI-03 |
| RI-07 | Intermediario frente al Core como última línea | Si existe (Q-16, Q-17), se configura como límite adicional, sin sustituir a M1 |

**No se requiere un sistema externo de coordinación distribuida** para la protección base. M1 es configuración; M2 queda diferida (DD-20) y, si se activa, tiene a M1 como respaldo.

### 3.5 Comportamiento con menos réplicas que el máximo

Con *n* réplicas activas < `N_EFECTIVO_MAX`, la clase solo usa `n / N_EFECTIVO_MAX` de su presupuesto.

- **Seguridad:** garantizada.
- **Costo:** **subutilización.** Para CL-1 eso significa rechazar transferencias con el Core ocioso (R2-N07).

| Medida | Efecto |
|---|---|
| `N_MAX` de los servicios de escritura **acotado a su carga real** (pocas réplicas, más grandes) | Cuota por réplica mayor |
| Despliegue rolling **sin surge** en los servicios con CL-1 | `N_EFECTIVO_MAX` = `N_MAX` (menos subutilización, despliegues más lentos) |
| Disparador de DD-20 específico: rechazos de CL-1 con utilización real del Core < `UMBRAL_UTIL_CORE` (por acordar) | Activa la evaluación de M2 |

**Trade-off explícito:** se acepta la subutilización a cambio de una protección sin dependencias en tiempo de ejecución. **No se sacrifica la protección del Core para mejorar el aprovechamiento.**

## 4. Observabilidad transversal

- Trazas distribuidas y correlación (OpenTelemetry mediante el SDK, ADR-0003) del Gateway al Core.
- Métricas obligatorias:
  - concurrencia por clase y servicio;
  - rechazos de admisión;
  - antigüedad por estado de las operaciones;
  - permisos concedidos frente a envíos;
  - antigüedad del outbox;
  - retraso de la auditoría;
  - entrega de notificaciones.
- Eventos de seguridad hacia el SIEM institucional, si existe (Q-13).

## 5. Ubicación del Core y conectividad (ARG02-H05)

**Estado: DESCONOCIDO.** El repositorio no contiene evidencia confiable sobre dónde se ejecuta el Core Bancario ni sobre la red entre la plataforma y el Core. Se registra como supuesto **S21** y como pregunta **Q-20**. **No se selecciona ningún modelo de conectividad.**

| Modelo candidato | Latencia | Disponibilidad de la red | Fronteras de seguridad | Dependencias operativas | Transferencia de datos | DR | Costo |
|---|---|---|---|---|---|---|---|
| Misma red privada (Core en el mismo entorno cloud) | Menor | La del proveedor | Dentro de la red privada | Pocas | Interna | Ligada a la región del Core | Bajo |
| Core en las instalaciones de la institución con conectividad dedicada privada hacia la nube | Depende de la distancia y del proveedor de enlace (por medir) | **Enlaces redundantes** obligatorios | TB-2 ↔ TB-3 cruza un enlace dedicado; cifrado requerido | Operador del enlace, equipos de borde de la institución | Cargos por transferencia según el proveedor | La región de DR también necesita su propio enlace | Medio-alto |
| Core en las instalaciones de la institución con VPN sitio a sitio por Internet | Mayor y variable | Menor que un enlace dedicado | Túnel cifrado sobre Internet | Equipos VPN; Internet | Igual | Igual | Bajo-medio |
| Otra conectividad empresarial (por ejemplo, un intermediario o ESB existente frente al Core) | Agrega un salto | La del intermediario | Frontera adicional | Equipo del intermediario | Variable | Variable | Variable |

**Implicaciones para el diseño, independientes del modelo:**

1. El enlace plataforma–Core es un **dominio de falla común** a todas las ACL. Su falla equivale a «Core no disponible» (QA3), y los timeouts de conexión deben distinguirlo (clase `NOT_SENT` frente a `UNKNOWN`, ADR-0005).
2. La **latencia medida** del enlace condiciona los timeouts, el plazo de negocio y el presupuesto de concurrencia (por la ley de Little, la concurrencia necesaria crece con la latencia).
3. C6 («garantice baja latencia») **no puede afirmarse** mientras no se conozca S21 y no se mida la latencia (Q-20).
