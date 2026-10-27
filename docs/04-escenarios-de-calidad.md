# 04 — Escenarios de atributos de calidad

| Campo | Valor |
|---|---|
| Fase | 0 — Drivers, supuestos y escenarios de calidad |
| Estado | Cerrado. Los valores numéricos son parámetros por acordar |
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
| Respuesta (escrituras) | Las operaciones transaccionales **fallan de forma segura**: si no se invocó el Core, se rechazan sin efectos; no se encolan a ciegas para ejecutarlas más tarde. Si la invocación ya se realizó y no hubo respuesta, la operación pasa a `UNKNOWN` y luego a conciliación. **Nunca** se reintenta a ciegas |
| Medida | 0 operaciones financieras duplicadas. 0 operaciones financieras en estado no determinado sin un caso de conciliación abierto en `T_APERTURA_CONCILIACION` (objetivo propuesto). El 100 % de las respuestas degradadas lleva un indicador de frescura |
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
| Respuesta | La operación pasa a `UNKNOWN`. **No se reintenta automáticamente.** Se consulta el estado (si el Core lo permite) o se concilia por correlación de negocio. El cliente recibe un estado «en verificación», no un error definitivo |
| Medida | 0 débitos duplicados por reintento. El 100 % de las operaciones `UNKNOWN` se resuelve (`COMPLETED` o `FAILED`) o se escala a revisión manual en `T_RESOLUCION_UNKNOWN` (por acordar) |
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
| Respuesta | Los servicios de la plataforma escalan horizontalmente. La carga adicional sobre el Core se limita con caché, limitación de concurrencia en la ACL y control de tasa en el borde |
| Medida | La latencia p95 se mantiene ≤ `T_CONSULTA_P95`. La concurrencia hacia el Core no supera `MAX_CONCURRENCIA_CORE`. Si el escenario no se cumple con Cache-Aside y optimización de lectura, se evalúa CQRS ([ADR-0007](adr/ADR-0007-cqrs-diferido.md)) |

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
| `T_RESOLUCION_UNKNOWN` | Tiempo máximo en `UNKNOWN` antes de escalar | Por acordar | Operaciones y riesgo operativo |
| `T_CONSULTA_AUDITORIA` | Tiempo de respuesta de una consulta de auditoría | Por acordar | Auditoría y cumplimiento |
| RTO, RPO | Objetivos de recuperación | Por acordar | BIA (DD-09) |
