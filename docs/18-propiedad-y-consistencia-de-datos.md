# 18 — Propiedad y consistencia de datos

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Pendiente de ARG-02 |
| ADR principales | [ADR-0016](adr/ADR-0016-persistencia-y-durabilidad-del-estado.md), [ADR-0015](adr/ADR-0015-mensajeria-asincrona-con-outbox.md), [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md) |

## 1. Principios

1. **El Core Bancario es la única fuente de verdad** de los saldos, los movimientos contables y la identidad del cliente (S2, ADR-0009). **La plataforma no crea un segundo libro mayor.**
2. Cada contenedor escribe **solo en su propio almacén lógico**. No hay bases de datos de aplicación compartidas para escritura.
3. No existe una transacción distribuida entre sistemas independientes. La consistencia entre el estado de un servicio y sus eventos se obtiene con el **outbox transaccional** (ADR-0015). La consistencia con el Core se obtiene con la **conciliación** (ADR-0005, ADR-0006).

## 2. Matriz de propiedad de datos

| Contenedor | Datos autoritativos (de la plataforma) | Datos derivados | Datos en caché | Datos sensibles | Consistencia | Retención | Auditoría |
|---|---|---|---|---|---|---|---|
| IdP | Cuentas digitales, hashes de credenciales, factores, sesiones | — | Sesiones (en el IdP) | **Credenciales y factores** | Fuerte (local) | Por política de seguridad (por acordar) | Eventos de inicio de sesión y de cambio de credenciales |
| Servicio de consultas | Ninguno de negocio. Solo el outbox de auditoría de consultas | Vistas de cliente, cuentas y movimientos desde el Core y el Sistema Complementario | Según la matriz de [06](06-estrategia-de-cache.md) | Datos personales y financieros (en tránsito y en la caché) | Eventual frente al Core; marca de tiempo de origen | El outbox hasta su entrega | Cada consulta del cliente |
| Servicio de vinculación | Solicitudes y su estado (E1 a E6), clave de idempotencia, evidencia de creación y de prueba de control, referencias a los resultados de KYC | Identificador del cliente obtenido del Core | — | **Resultados de KYC y biometría** (ver nota), identificación | Fuerte (local); alta conciliada con evidencia | Por acordar con cumplimiento (Q-06) | Cada etapa |
| Servicio de transferencias | Intenciones, huellas, estado global y atributos, titularidad y token, permisos de envío, evidencia huérfana, casos de conciliación y de efecto tardío | Resultado del Core según la evidencia | **Nunca** | Datos financieros de la operación | **Fuerte (CAS; PT-01 a PT-03)** | Al menos la vida de la operación + `RETENCION_CLAVE` + `VENTANA_VIGILANCIA_EFECTO_TARDIO`; política por acordar | Cada transición (INV-T7) |
| Servicio de notificaciones | Eventos recibidos (para la deduplicación), intentos de entrega, preferencias de canal | — | — | Datos de contacto; contenido minimizado | Local | Por acordar | Cada intento |
| Servicio de auditoría | **Registro de auditoría** (solo anexión, encadenado) | — | — | Contiene referencias a datos personales | Anexión ordenada por fuente | Por acordar (Q-19); archivo inmutable | Es la evidencia |
| Caché | — | Datos derivados | Sí | Datos personales y financieros | Eventual; TTL de seguridad | Volátil | — |
| Broker | — (transitorio) | Eventos | — | Según el evento (minimizados) | Al menos una vez | Retención operativa | — |

**Nota sobre biometría:** la plataforma **no almacena plantillas biométricas** en esta capa. Conserva solo los resultados y las referencias de la verificación que exijan cumplimiento y ADR-0010 (Q-06). Las imágenes y videos de la prueba de vida se tratan según el contrato con el proveedor (DD-14).

## 3. Evaluación de la persistencia relacional

| Uso | Necesidad técnica | ¿Relacional? | Comentario |
|---|---|---|---|
| Registros de idempotencia | Unicidad compuesta atómica (IC-07, IC-15), retención | **Sí** | Restricciones de unicidad nativas, incluidas las parciales (equivalentes no terminales) |
| Estado de las transacciones | CAS por estado, versión y token; transacciones locales; outbox en la misma transacción | **Sí** | Requiere PT-01 a PT-03 |
| Estado de vinculación | Máquina de estados durable; evidencia | **Sí** | — |
| Casos de conciliación | Colas de trabajo con bloqueo de filas; asignación a responsables | **Sí** | — |
| Metadatos de auditoría | Anexión, encadenamiento, consultas por cliente y periodo | **Sí**, con archivo inmutable | El archivo en almacenamiento de objetos da la inmutabilidad a largo plazo |
| Caché | Lectura de baja latencia, TTL, expiración | **No** (almacén clave-valor en memoria) | Evaluada aparte (DD-07) |

## 4. Durabilidad transaccional y durabilidad de auditoría

No existe una transacción que abarque el almacén de transferencias, el broker y el almacén de auditoría. La relación es:

1. La transición de estado y su **evento de auditoría** se escriben en la **misma transacción local** (outbox). Desde ese instante, la acción y su evidencia de auditoría son durables **en el origen**.
2. El relé publica el evento (al menos una vez). El servicio de auditoría lo anexa de forma idempotente por identificador de evento.
3. **Retraso de auditoría:** existe una ventana entre (1) y (2). Se monitorea la antigüedad del outbox y la del retraso de ingesta. Si el broker no está disponible, la evidencia **no se pierde**: permanece en el outbox de origen.
4. **Acciones rechazadas antes de cualquier cambio de estado** (por ejemplo, una solicitud sin clave): el servicio que rechaza registra el evento de auditoría en su outbox. Si la acción no llega a ningún servicio (rechazo en el Gateway), la evidencia son los registros de acceso del Gateway, que se envían a la observabilidad y al SIEM institucional (Q-13).
5. **Consultas:** *sustituido en la remediación de ARG-02 por [ADR-0020](adr/ADR-0020-politica-de-auditoria-por-categoria.md).* Las categorías A4 y A5 usan una **doble ruta durable** (publicación con confirmación en el broker **o** outbox local). La respuesta espera la confirmación de una de las dos; solo si fallan ambas, la consulta no se responde.

## 5. Requisitos de durabilidad del estado financiero

Están en [ADR-0016](adr/ADR-0016-persistencia-y-durabilidad-del-estado.md): PT-01 (RPO 0 de las escrituras confirmadas), PT-02 (cuarentena tras restauración) y PT-03 (escritor único). Su origen está en la verificación acotada de [23](23-riesgos-y-condiciones-fase-3.md).

## 6. Actualización de la remediación de ARG-02

| Contenedor | Datos autoritativos (de la plataforma) | Sensibles | Consistencia | Retención | Auditoría |
|---|---|---|---|---|---|
| Almacén de sesiones del BFF | Sesiones web, tokens cifrados, índice de sujeto a sesiones, claves de intenciones pendientes generadas por la SPA | **Tokens** | Fuerte por sesión | Vida de la sesión | Eventos de sesión (A7) |
| Diario de autorización de despacho | Entradas de despacho y época regional | Solo resúmenes criptográficos y referencias derivadas | **Linealizable con quórum** | `RETENCION_CLAVE` + `VENTANA_VIGILANCIA_EFECTO_TARDIO` | Cada entrada es evidencia (A1) |
| Servicio de notificaciones (ampliado, ARG02-M05) | **Tokens de dispositivo** (registrados por la app a través del BFF) y preferencias de canal | Datos de contacto (en tránsito, no persistidos de forma permanente) | Local | Mientras el dispositivo esté vigente | Registro y baja |

**Datos de contacto (ARG02-M05):** el número de teléfono y el correo son autoritativos en el **Core o en el Sistema Complementario** (por validar con Q-09). El servicio de notificaciones **no** los conserva como fuente: los obtiene **en el momento del envío** a través del servicio de consultas (API interna, autenticada entre servicios). Puede guardarlos en caché por un tiempo corto, `TTL_CONTACTO`.
