# 04 — Escenarios de atributos de calidad

| Campo | Valor |
|---|---|
| Fase | 0 — Drivers, supuestos y escenarios de calidad |
| Estado | Vigente. Los valores numéricos son parámetros por acordar. QA3 y QA5 se reformularon y QA9 a QA11 se agregaron en la remediación ARG-01 |
| Documentos relacionados | [03 — Atributos de calidad](03-atributos-de-calidad.md), [02 — Supuestos (S10)](02-supuestos.md) |

## 1. Formato

Cada escenario sigue la estructura de seis partes de los escenarios de atributos de calidad (*quality attribute scenarios*): **fuente del estímulo, estímulo, artefacto, entorno, respuesta y medida de la respuesta**.

### 1.1 Tratamiento de los valores numéricos

El reto no define cifras (S10). Por eso las medidas de respuesta se expresan con **parámetros con nombre** (por ejemplo, `T_CONSULTA_P95`). Cada parámetro tiene uno de estos estados:

| Estado | Significado |
|---|---|
| **Por acordar** | La institución debe fijar el valor (negocio, riesgo u operaciones) |
| **Objetivo propuesto** | El equipo de arquitectura propone un valor de ingeniería, sujeto a validación |
| **Validado** | La institución confirmó el valor |

En esta fase **ningún parámetro está validado**. El registro está en la sección 3.

## 2. Escenarios

### QA1 — Disponibilidad: degradación controlada ante fallas parciales

| Parte | Descripción |
|---|---|
| Fuente | Falla de infraestructura o de dependencia |
| Estímulo | Deja de estar disponible una instancia de un servicio de la Plataforma de Banca Digital, o una ruta de integración con el Core (por ejemplo, la operación de consulta de movimientos) |
| Artefacto | Servicios de dominio y sus ACL |
| Entorno | Operación normal en horario de alta demanda |
| Respuesta | Las funciones no relacionadas con el componente afectado siguen operativas. Por ejemplo, si falla la ruta de movimientos, la consulta de datos del cliente sigue funcionando. La función afectada responde con un error controlado o con datos degradados aprobados, y nunca con un bloqueo prolongado. Se generan alertas |
| Medida | Las funciones no relacionadas mantienen su tasa de éxito dentro de `DISP_FUNC_NO_AFECTADAS` (por acordar). El circuito de la ruta afectada se abre en `T_DETECCION_FALLA` como máximo (objetivo propuesto). Ningún hilo ni conexión de otro dominio se agota por la falla (aislamiento con Bulkhead) |
| Decisiones | [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) |

### QA2 — Rendimiento: consultas responsivas sin saturar el Core

| Parte | Descripción |
|---|---|
| Fuente | Clientes en canales web y móvil |
| Estímulo | Consultas de datos del cliente y de movimientos a volumen normal y en pico (`TPS_NORMAL`, `TPS_PICO`; por acordar) |
| Artefacto | Servicios de consulta, caché y ACL de lectura |
| Entorno | Operación normal y pico estacional (quincena, fin de mes) |
| Respuesta | Las consultas se atienden desde la caché cuando la categoría del dato lo permite ([06](06-estrategia-de-cache.md)). Las demás llegan al Core con paginación y con concurrencia acotada por la ACL |
| Medida | Latencia p95 de la consulta de movimientos ≤ `T_CONSULTA_P95` (por acordar). La tasa de llamadas al Core por consulta del canal se mantiene ≤ `RATIO_LECTURA_CORE` (objetivo propuesto). La concurrencia hacia el Core nunca supera `MAX_CONCURRENCIA_CORE` (depende de S4) |
| Decisiones | [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0007](adr/ADR-0007-cqrs-diferido.md) |

### QA3 — Resiliencia: el Core no está disponible

| Parte | Descripción |
|---|---|
| Fuente | Core Bancario |
| Estímulo | El Core deja de responder (falla, mantenimiento o cierre diario) |
| Artefacto | ACL de clientes, movimientos y transferencias; máquina de estados transaccional |
| Entorno | Operación normal; indisponibilidad de duración desconocida |
| Respuesta (lecturas) | Las lecturas pueden degradarse a datos de caché o de proyección **aprobados**, solo para las categorías cuya política de frescura lo permite. La respuesta indica al canal que los datos pueden no estar actualizados |
| Respuesta (escrituras) | Las operaciones transaccionales **fallan de forma segura**. Si la falla es de clase `NOT_SENT`, terminan en `FAILED_NO_EFFECT`, sin encolarse para después. Si la invocación pudo enviarse, la clase es `UNKNOWN`: la operación pasa a `CORE_UNKNOWN` y luego a conciliación ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md)). **Nunca** se reintenta la ejecución |
| Medida (reformulada en la remediación ARG-01, ARG01-M01) | **(a)** 0 invocaciones de ejecución duplicadas hacia el Core por operación y tramo, verificable por auditoría: cada identificador de operación registra como máximo una invocación por tramo. **(b)** 0 operaciones nuevas creadas con una (ámbito, clave) existente. **(c)** El 100 % de las operaciones en clase `UNKNOWN` tiene un caso de conciliación abierto en `T_APERTURA_CONCILIACION`. **(d)** Exactitud de la conciliación automática: proporción de resoluciones automáticas corregidas después por revisión ≤ `TASA_ERROR_CONCILIACION` (por acordar), medida por muestreo. **(e)** El 100 % de las respuestas degradadas lleva un indicador de frescura. **No se mide ni se promete «0 duplicados» de extremo a extremo:** sin idempotencia nativa del Core no es demostrable (RR-04), y los duplicados semánticos con claves distintas se miden aparte (`TASA_DUPLICADO_SEMANTICO`, RR-13) |
| Decisiones | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), [ADR-0004](adr/ADR-0004-cache-aside.md) |

### QA4 — Seguridad: token de acceso robado

| Parte | Descripción |
|---|---|
| Fuente | Atacante externo que obtuvo un token de acceso válido (por ejemplo, con malware en el dispositivo o por una fuga en los registros) |
| Estímulo | Intento de iniciar una transferencia de alto riesgo desde un dispositivo o una sesión no autorizados |
| Artefacto | Canal, API Gateway, servidor de autorización y servicio de transferencias |
| Entorno | Operación normal |
| Respuesta | La posesión del token **no basta** para ejecutar una operación de alto riesgo. La operación exige una prueba adicional ligada al dispositivo o a la sesión legítima, o una autenticación reforzada (*step-up authentication*). El intento se rechaza y se registra en auditoría |
| Medida | 0 transacciones de alto riesgo ejecutadas solo con un token robado en las pruebas de seguridad definidas. El 100 % de los intentos rechazados queda auditado |
| Mecanismo | **Diferido** a la fase de seguridad (DD-04). Hay varias alternativas: DPoP (*Demonstrating Proof-of-Possession*), mTLS con tokens vinculados a certificado, vinculación de dispositivo (*device binding*), autenticación reforzada y perfiles FAPI. Aún no se selecciona ninguna |

### QA5 — Integridad transaccional: timeout en una operación crítica

| Parte | Descripción |
|---|---|
| Fuente | Red o Core Bancario |
| Estímulo | Se envía al Core una transferencia, el Core la confirma internamente y la respuesta se pierde. La plataforma recibe un timeout |
| Artefacto | ACL de transferencias y máquina de estados transaccional |
| Entorno | Operación normal o degradada |
| Respuesta | Clase `UNKNOWN`: la operación pasa a `CORE_UNKNOWN`. **No se reintenta la ejecución.** Se consulta el estado (si el Core lo permite) o se concilia por correlación de negocio. El cliente recibe «en verificación», no un error definitivo. Una operación equivalente del mismo cliente se bloquea mientras dure la incertidumbre ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), sección 3) |
| Medida | 0 débitos duplicados por reintento de la plataforma. **0 transiciones a `FAILED_NO_EFFECT` o `REJECTED` sustentadas solo en evidencia negativa o incompleta (EV-INC)**, verificable por auditoría de la evidencia registrada en cada transición (versión 3, CF-01). El 100 % de las operaciones en `CORE_UNKNOWN` se resuelve con la evidencia que exige INV-T2 o se escala a `REQUIRES_RECONCILIATION` en `T_RESOLUCION_UNKNOWN` (por acordar). El 100 % de los casos en `REQUIRES_RECONCILIATION` tiene un responsable asignado y se resuelve o se escala en `T_RESOLUCION_MANUAL` |
| Decisiones | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) |

### QA6 — Auditabilidad: reconstrucción de las acciones de un cliente

| Parte | Descripción |
|---|---|
| Fuente | Área de auditoría, de cumplimiento o de atención de reclamos |
| Estímulo | Solicitud de reconstruir todas las acciones de un cliente en un periodo |
| Artefacto | Base de datos de auditoría (diseño en fases posteriores) |
| Entorno | Operación normal |
| Respuesta | Se obtiene la secuencia completa de las acciones del cliente, correlacionadas por identificador de correlación y por clave de idempotencia, con evidencia de integridad |
| Medida | El 100 % de las acciones del cliente es trazable. La consulta se resuelve en `T_CONSULTA_AUDITORIA` (por acordar). Cualquier alteración del registro se puede detectar |
| Decisiones | Pendiente (R5, fase de contenedores) |

### QA7 — Mantenibilidad: cambio del contrato del Core

| Parte | Descripción |
|---|---|
| Fuente | Proveedor o equipo propietario del Core |
| Estímulo | Cambio en el contrato de una operación del Core (por ejemplo, la consulta de movimientos) |
| Artefacto | ACL del dominio afectado |
| Entorno | Ciclo de cambio planificado |
| Respuesta | El cambio se absorbe en la ACL del dominio afectado. El modelo de dominio, las API públicas y los demás dominios no cambian |
| Medida | Componentes modificados fuera de la ACL afectada = 0. Pruebas de contrato de la ACL actualizadas antes del despliegue |
| Decisiones | [ADR-0001](adr/ADR-0001-acl-por-dominio.md) |

### QA8 — Escalabilidad: pico de demanda

| Parte | Descripción |
|---|---|
| Fuente | Clientes |
| Estímulo | Pico de consultas de `FACTOR_PICO` × el tráfico normal (por acordar; S10) |
| Artefacto | Servicios de consulta, caché y ACL |
| Entorno | Pico estacional |
| Respuesta | Los servicios de la plataforma escalan horizontalmente hasta `N_MAX_REPLICAS`. La carga adicional sobre el Core se limita con caché, cuotas por clase de operación y control de admisión ([ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md)), y con control de tasa en el borde |
| Medida | La latencia p95 se mantiene ≤ `T_CONSULTA_P95`. La concurrencia total hacia el Core no supera `MAX_CONCURRENCIA_CORE` **para cualquier número de réplicas ≤ `N_MAX_REPLICAS`**. Si el escenario no se cumple con Cache-Aside y optimización de lectura, se evalúa CQRS ([ADR-0007](adr/ADR-0007-cqrs-diferido.md)) |

### QA9 — Integridad transaccional: reenvío de la misma intención por el cliente

*Agregado en la remediación ARG-01 (ARG01-C01).*

| Parte | Descripción |
|---|---|
| Fuente | Canal web o móvil del cliente |
| Estímulo | El cliente confirma una transferencia y no recibe respuesta (pérdida de red o reinicio de la app). El canal reenvía la **misma intención** con la **misma clave**, una o varias veces, incluso en paralelo. Variante: reenvío con la misma clave y **otro contenido** |
| Artefacto | Contrato de idempotencia del servicio de transferencias; almacén durable de operaciones |
| Entorno | Operación normal o degradada; cualquier estado de la operación original, incluido `CORE_UNKNOWN` |
| Respuesta | Los reenvíos con la misma huella devuelven el estado de la operación original sin nueva ejecución. El contenido distinto produce un conflicto sin efectos. Las solicitudes concurrentes producen una sola reserva ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), IC-08 a IC-10) |
| Medida | Para N reenvíos (secuenciales o concurrentes) de una misma (ámbito, clave): exactamente **1** operación y **≤ 1** invocación de ejecución por tramo. El 100 % de los reenvíos con huella distinta produce un conflicto auditado sin efectos. El 100 % de las solicitudes sin clave se rechaza sin efectos |

### QA10 — Disponibilidad y protección del Core: escalado de réplicas

*Agregado en la remediación ARG-01 (ARG01-H04).*

| Parte | Descripción |
|---|---|
| Fuente | Escalado automático ante un pico de consultas |
| Estímulo | El servicio de consultas pasa de 2 a `N_MAX_REPLICAS` réplicas mientras continúan las transferencias |
| Artefacto | ACL, control de admisión y cuotas por clase ([ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md)) |
| Entorno | Pico estacional; mecanismo de coordinación dinámica (si existe) disponible o no disponible |
| Respuesta | La concurrencia por réplica se limita a su cuota derivada. Las consultas excedentes se degradan o se rechazan. Las escrituras críticas y las consultas de integridad conservan su cuota reservada. Si la coordinación falla, se vuelve a la cuota estática |
| Medida | Concurrencia total hacia el Core ≤ `MAX_CONCURRENCIA_CORE` en todo momento. Tasa de éxito de CL-1 ≥ `EXITO_MIN_CL1` (por acordar) durante el pico. 0 casos de funcionamiento sin límite por falla del mecanismo de control |

### QA11 — Seguridad: vinculación digital de un cliente preexistente

*Agregado en la remediación ARG-01 (ARG01-H05).*

| Parte | Descripción |
|---|---|
| Fuente | Persona que supera la verificación de identidad con el documento de un cliente existente (titular legítimo o suplantador) |
| Estímulo | Solicita vincular su dispositivo y su acceso digital a ese cliente |
| Artefacto | Máquina de estados de vinculación ([07, sección 5](07-maquina-de-estados-transaccional.md#5-máquina-de-estados-de-vinculación-digital)); [ADR-0009](adr/ADR-0009-core-autoritativo.md) |
| Entorno | Operación normal |
| Respuesta | El cliente se identifica como `PRE_EXISTING`, la vinculación queda en `LINK_PENDING` y solo se autoriza tras una prueba de control con un factor previamente registrado e independiente del dispositivo solicitante. Las respuestas previas no revelan la condición de cliente |
| Medida | 0 vinculaciones autorizadas de clientes preexistentes sin prueba de control (verificable en auditoría y en pruebas). 0 respuestas que revelen la condición de cliente antes de la prueba de control. Mecanismo de prueba: DD-21 |

### QA12 — Integridad transaccional: ejecutor zombi y toma de titularidad

*Agregado en la remediación de CF-02 (ARG-01-R2, R2-N02).*

| Parte | Descripción |
|---|---|
| Fuente | Proceso ejecutor del orquestador |
| Estímulo | Un ejecutor obtiene el permiso de envío y se pausa. Su arrendamiento vence, otro ejecutor toma la operación y el primero reanuda y envía |
| Artefacto | Protocolo de titularidad y permiso de envío ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) §4) |
| Entorno | Pausas de proceso, particiones de red, reinicios |
| Respuesta | El nuevo titular nunca envía: solo pasa el tramo a `*_UNKNOWN`. El envío tardío es el único del tramo. La respuesta tardía se registra como evidencia huérfana y la conciliación la usa |
| Medida | Para cualquier intercalación: **≤ 1 permiso de envío** y **≤ 1 solicitud originada en la plataforma** por tramo (esta última, bajo el supuesto S19). 0 transiciones registradas con un token vencido. El 100 % de las respuestas recibidas por ejecutores cercados queda registrado como evidencia huérfana |

### QA13 — Integridad transaccional: evidencia negativa y efecto tardío

*Agregado en la remediación de CF-01 (ARG-01-R2, R2-N01).*

| Parte | Descripción |
|---|---|
| Fuente | Core Bancario (asentamiento tardío) |
| Estímulo | La conciliación no encuentra una operación potencialmente enviada; días después, la operación aparece en el Core |
| Artefacto | Clases de evidencia, cierre excepcional y vigilancia de efecto tardío ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) §3 y §7) |
| Entorno | Core sin idempotencia nativa ni cierre por referencia (S3) |
| Respuesta | La operación permanece en `CORE_UNKNOWN` o `REQUIRES_RECONCILIATION`, con las operaciones equivalentes bloqueadas. Si aparece antes del cierre, se resuelve con EV-POS. Si aparece después de `CLOSED_BY_EXCEPTION`, se abre un caso de efecto tardío |
| Medida | 0 cierres o reenvíos basados en EV-INC. El 100 % de los efectos tardíos detectados en `VENTANA_VIGILANCIA_EFECTO_TARDIO` genera un caso vinculado. **No se mide ni se garantiza el tiempo de resolución** (depende del Core y de la revisión manual), pero sí que el caso llegue a un responsable en `T_RESOLUCION_UNKNOWN` + `T_RESOLUCION_MANUAL` |

### QA14 — Integridad transaccional: reenvío del cliente tras una conmutación regional

*Agregado en la remediación de ARG-02 (ARG02-C01).*

| Parte | Descripción |
|---|---|
| Fuente | Desastre en la región primaria; cliente que reenvía |
| Estímulo | Conmutación a la región secundaria con replicación asíncrona; se perdieron reservas y permisos recientes; el cliente reenvía una transferencia con la misma clave |
| Artefacto | Diario de autorización de despacho, protocolo de recuperación con marca de agua ([ADR-0021](adr/ADR-0021-diario-de-autorizacion-de-despacho.md)) |
| Entorno | Recuperación ante desastres |
| Respuesta | Cuarentena (sin iniciaciones ni permisos); época incrementada; reconstrucción de las operaciones anotadas en el diario como `*_UNKNOWN`; el reenvío recibe «en verificación». Una clave sin entrada en el diario se trata como nueva solo después de la cuarentena |
| Medida | **0 segundos despachos** de operaciones anotadas en el diario (INV-DR). El 100 % de las entradas del diario posteriores a la marca de agua se reconstruye antes de salir de la cuarentena. La salida de la cuarentena requiere la verificación automática y la autorización de dos personas |

### QA15 — Disponibilidad: auditoría de consultas con una ruta caída

*Agregado en la remediación de ARG-02 (ARG02-H03).*

| Parte | Descripción |
|---|---|
| Fuente | Infraestructura |
| Estímulo | La BD de consultas (outbox) o el broker no están disponibles |
| Artefacto | Política de auditoría A4 y A5 ([ADR-0020](adr/ADR-0020-politica-de-auditoria-por-categoria.md)) |
| Entorno | Operación normal |
| Respuesta | La consulta se responde si **una** de las dos rutas durables confirma el evento de auditoría. Si fallan ambas, la consulta no se responde y se alerta |
| Medida | Ninguna consulta respondida sin evidencia durable. Disponibilidad de las consultas ante la falla de una sola ruta ≥ `DISP_FUNC_NO_AFECTADAS` |

## 3. Registro de parámetros

| Parámetro | Descripción | Estado | Fuente para fijarlo |
|---|---|---|---|
| `TPS_NORMAL`, `TPS_PICO`, `FACTOR_PICO` | Volumen de solicitudes | Por acordar | Métricas de los canales actuales y proyección de negocio |
| `T_CONSULTA_P95` | Latencia p95 de las consultas | Por acordar | Negocio y experiencia de usuario |
| `DISP_FUNC_NO_AFECTADAS` | Disponibilidad de las funciones no afectadas por una falla | Por acordar | SLA del canal |
| `T_DETECCION_FALLA` | Tiempo hasta abrir el circuito | Objetivo propuesto (se calibra en pruebas) | Ingeniería |
| `RATIO_LECTURA_CORE` | Proporción de consultas que llegan al Core | Objetivo propuesto | Ingeniería y capacidad del Core (S4) |
| `MAX_CONCURRENCIA_CORE` | Concurrencia máxima permitida hacia el Core | Por acordar | Propietario del Core (S4) |
| `T_APERTURA_CONCILIACION` | Tiempo hasta abrir un caso de conciliación | Objetivo propuesto | Operaciones |
| `T_RESOLUCION_UNKNOWN` | Tiempo máximo en clase `UNKNOWN` antes de escalar | Por acordar | Operaciones y riesgo operativo |
| `T_RESOLUCION_MANUAL` | Tiempo máximo de un caso en `REQUIRES_RECONCILIATION` antes de escalar | Por acordar | Operaciones y riesgo operativo (ARG01-H03) |
| `T_ASENTAMIENTO_CORE`, `VENTANA_CORRELACION` | Tiempo de asentamiento y ventana de correlación de la conciliación | Por acordar | Propietario del Core |
| `TASA_ERROR_CONCILIACION` | Proporción máxima de resoluciones automáticas corregidas después | Por acordar | Riesgo operativo (ARG01-M01) |
| `TASA_DUPLICADO_SEMANTICO` | Indicador de duplicados semánticos (se mide, no se garantiza) | Indicador | Negocio (RR-13) |
| `VENTANA_REINTENTO_CANAL`, `RETENCION_CLAVE` | Ventana de reenvío del canal y retención de las claves de idempotencia | Por acordar | Canales e ingeniería (ARG01-C01) |
| `VENTANA_DUPLICADO_SEMANTICO` | Ventana para pedir la confirmación de un posible duplicado | Por acordar | Negocio |
| `EVENTO_CONFIRMACION_EXTERNA` | Evento que el contrato externo define como definitivo para `COMPLETED` interbancario | Por acordar | Q-01 |
| `TASA_MAX_CORE`, `CUOTA_CL1` a `CUOTA_CL4`, `MARGEN_CORE`, `N_MAX_REPLICAS`, `EXITO_MIN_CL1` | Presupuesto de capacidad del Core y cuotas por clase | Por acordar | Propietario del Core y operaciones (ARG01-H04, Q-16) |
| `VIGENCIA_VERIFICACION_IDENTIDAD` | Vigencia del resultado de la verificación de identidad | Por acordar | Cumplimiento y seguridad (ARG01-H05) |
| `LEASE_DURACION`, `MAX_DESFASE_ENVIO` | Duración del arrendamiento de titularidad y desfase máximo entre el permiso y el envío | Objetivo propuesto (se calibra en pruebas) | Ingeniería (CF-02) |
| `PLAZO_VALIDEZ_INTENCION` | Vigencia de la intención del cliente para conceder el permiso de envío | Por acordar | Negocio y experiencia de usuario (CF-02, R2-N03) |
| `PLAZO_CIERRE_EXCEPCIONAL`, `VENTANA_VIGILANCIA_EFECTO_TARDIO` | Plazo mínimo antes del cierre excepcional y ventana de vigilancia de efectos tardíos | Por acordar | Operaciones, riesgo operativo y propietario del Core (CF-01) |
| `SESION_INACTIVIDAD`, `SESION_ABSOLUTA`, `VIDA_ACCESS_TOKEN`, `ROTACION_CLAVES_FIRMA` | Sesiones y tokens | Por acordar | Seguridad de la información (ADR-0019, ADR-0013) |
| `MAX_VINCULACIONES_POR_HORA` | Límite de la operación de vinculación | Por acordar | Seguridad y canales (ARG02-H04) |
| `T_MAX_RETRASO_AUDITORIA`, `TTL_CONTACTO` | Retraso máximo de la auditoría; caché de contactos | Por acordar | Auditoría; canales |
| `SESIONES_MAX_CORE` | Sesiones o conexiones máximas hacia el Core | Por acordar | Propietario del Core (RI-08) |
| `T_CONSULTA_AUDITORIA` | Tiempo de respuesta de una consulta de auditoría | Por acordar | Auditoría y cumplimiento |
| RTO, RPO | Objetivos de recuperación | Por acordar | BIA (DD-09) |
