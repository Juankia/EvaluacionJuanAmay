# 24 — Arquitectura de componentes (C4 Nivel 3)

| Campo | Valor |
|---|---|
| Fase | 4 — C4 Nivel 3: Componentes |
| Estado | **Parcialmente completada.** Cinco contenedores diseñados. El servicio de transferencias y el ejecutor del alta de clientes están **bloqueados** (B-05). Pendiente de la revisión ARG-03 |
| Fuente autoritativa | [`c4/workspace.dsl`](../c4/workspace.dsl): vistas `Componentes-*` |
| Documentos relacionados | [25 — Contratos, eventos y errores](25-contratos-eventos-y-errores.md), [26 — Validación adversarial](26-validacion-adversarial-fase-4.md), [Registro de remediación ARG-02](reviews/ARG-02-registro-de-remediacion.md), [Revisión focalizada](reviews/ARG-02-R-revision-focalizada.md) |

## 1. Alcance

| Contenedor | Nivel 3 | Razón |
|---|---|---|
| BFF de canales digitales | **Diseñado** | Independiente de ARG02-C01 |
| Servicio de consultas | **Diseñado** | Independiente |
| Servicio de vinculación digital | **Diseñado, excepto el ejecutor del alta** | El ejecutor del alta en el Core es una **frontera bloqueada** (mismo protocolo que las transferencias) |
| Servicio de notificaciones | **Diseñado** | Independiente |
| Servicio de auditoría | **Diseñado** | Independiente |
| Servicio de transferencias | **NO diseñado** | B-05: CF-01, CF-02 y ARG02-C01 sin verificación independiente |
| IdP, API Gateway | Sin componentes propios | Productos; su diseño es de configuración (fase de seguridad e infraestructura) |
| Consola de operaciones | Sin componentes | Provisional (DD-18) |

## 2. Diagramas

| Vista | Diagrama |
|---|---|
| BFF | [structurizr-Componentes-BFF.svg](../diagrams/c4/structurizr-Componentes-BFF.svg) |
| Servicio de consultas | [structurizr-Componentes-Consultas.svg](../diagrams/c4/structurizr-Componentes-Consultas.svg) |
| Servicio de vinculación | [structurizr-Componentes-Vinculacion.svg](../diagrams/c4/structurizr-Componentes-Vinculacion.svg) |
| Servicio de notificaciones | [structurizr-Componentes-Notificaciones.svg](../diagrams/c4/structurizr-Componentes-Notificaciones.svg) |
| Servicio de auditoría | [structurizr-Componentes-Auditoria.svg](../diagrams/c4/structurizr-Componentes-Auditoria.svg) |

Las vistas de componentes muestran los contenedores externos como dependencias, **sin** sus componentes internos. Las relaciones entre contenedores externos se excluyen (etiqueta `Nivel2`) para no sobrecargar las vistas.

## 3. Principios comunes

| Principio | Aplicación |
|---|---|
| Capas | **Entrada** (API, consumidores de eventos) → **aplicación** (casos de uso, límites transaccionales) → **dominio** (modelo, invariantes, políticas) ← **adaptadores de salida** (ACL, repositorios, relés, proveedores). Las dependencias apuntan al dominio ([00-estándares](00-estandares-de-ingenieria.md) §5.2) |
| Puertos | Un puerto por capacidad y solo donde existe una variación real (Core, proveedor, almacén). **Sin capas de abstracción sin propósito** |
| Orquestación frente a dominio | Los servicios de aplicación coordinan; las reglas viven en el modelo y en las políticas de dominio |
| ACL | Módulos internos del servicio dueño ([ADR-0012](adr/ADR-0012-descomposicion-hibrida-de-contenedores.md)); usan las primitivas del Integration Platform SDK, que no es un contenedor |
| Framework | Los componentes no dependen de un framework concreto. El stack está provisional ([ADR-0017](adr/ADR-0017-stack-de-backend.md)) |
| Seguridad por defecto | Validación en el borde de cada contenedor; autorización de negocio en el dominio; minimización de datos |

## 4. Diseño por contenedor

### 4.1 BFF de canales digitales

| Aspecto | Diseño |
|---|---|
| Responsabilidad y frontera | Canal: sesión web, composición de lecturas, adaptación y retransmisión. **Sin reglas bancarias ni estado financiero** (ADR-0014) |
| Puntos de entrada | API de canal (HTTP); consumidor de eventos de seguridad |
| Consumidores de API o eventos | Recibe solicitudes del API Gateway; consume eventos de seguridad del broker |
| Casos de uso | Composición de lecturas; relevo de transferencias; flujo de vinculación; registro de dispositivos |
| Modelo e invariantes | **BFF-1:** nunca genera ni altera `Idempotency-Key` (PT-04). **BFF-2:** nunca reintenta un comando financiero. **BFF-3:** nunca infiere el resultado de una transferencia a partir de saldos |
| Puertos y adaptadores | Puerto de sesión (almacén de sesiones); puerto del IdP (OIDC); clientes de servicios de dominio |
| Adaptadores de integración | Clientes HTTP hacia los servicios de dominio; OIDC hacia el IdP; publicador y consumidor del broker |
| Persistencia | Solo el almacén de sesiones (ADR-0019) |
| Límites transaccionales | No aplica (sin estado de negocio) |
| Manejo de errores | Traduce los errores de dominio al contrato uniforme ([25](25-contratos-eventos-y-errores.md) §3); **degradación explícita** de la composición (ARG02-M06) |
| Autenticación y autorización | Sesión web o token móvil; audiencia y alcances. La autorización de negocio la hace cada servicio |
| Idempotencia | Propaga la clave sin cambios; guarda en la sesión la clave de la intención pendiente generada por la SPA (ADR-0019) |
| Auditoría | Eventos de sesión (A7) mediante el publicador con confirmación |
| Observabilidad | Trazas de extremo a extremo; métricas por ruta y canal; tasa de composiciones degradadas |
| Reintentos y timeouts | Lecturas: un reintento acotado dentro del plazo. Comandos: **ninguno**. Plazo propagado (DD-06) |
| Presión de retorno | Circuit Breaker por servicio; respuesta 503 con `Retry-After` en las lecturas |
| Fallas y recuperación | Réplica que termina: sin pérdida de sesión (almacén externo). Almacén caído: falla cerrada para la web. Transferencias no disponibles: la composición marca «no se pudo verificar operaciones en curso» |
| Dependencias | API Gateway, almacén de sesiones, IdP, servicios de dominio, broker |
| Pruebas | Contrato con cada servicio; pruebas de arquitectura (BFF-1 a BFF-3); seguridad de la sesión (CSRF, atributos de la cookie, revocación) |
| Trazabilidad | R1, R2, R6; ADR-0014, ADR-0019; ARG02-H02, M06, L07 |

**Composición degradada (ARG02-M06).** Si el servicio de transferencias no responde, la vista de movimientos incluye `inFlightOperationsStatus = UNAVAILABLE` y la interfaz muestra «no se pudieron verificar operaciones en curso». Como respaldo, el servidor bloquea igualmente cualquier operación equivalente (IC-15).

### 4.2 Servicio de consultas

| Aspecto | Diseño |
|---|---|
| Responsabilidad y frontera | Lecturas del cliente, las cuentas, los saldos, los movimientos y los contactos. **Sin escrituras en el Core** |
| Puntos de entrada | API de consultas (canal); API interna de contactos (entre servicios); consumidor de invalidaciones |
| Consumidores de API o eventos | El BFF consume la API de consultas y el servicio de notificaciones, la de contactos. El servicio consume del broker los movimientos confirmados |
| Casos de uso | Perfil; cuentas, saldos y movimientos; contactos |
| Modelo e invariantes | **CON-1:** todo recurso pertenece al cliente autenticado (autorización de recursos). **CON-2:** ningún dato en caché es autoritativo ni se usa para decidir. **CON-3:** toda respuesta derivada lleva la marca de tiempo de origen |
| Puertos y adaptadores | Puertos del Core (clientes, movimientos), del Sistema Complementario, de la caché y de la auditoría |
| Adaptadores de integración | ACL de clientes, de movimientos y del Sistema Complementario; control de admisión |
| Persistencia | Caché (derivada); outbox local de auditoría |
| Límites transaccionales | Solo el outbox de auditoría (ruta 2) |
| Manejo de errores | Taxonomía de la ACL. Core no disponible → datos degradados aprobados con indicador o `CORE_UNAVAILABLE` |
| Autenticación y autorización | Token del usuario (audiencia del servicio). Para la API interna: identidad de carga de trabajo del servicio de notificaciones |
| Idempotencia | No aplica (lecturas) |
| Auditoría | A5 (consultas ordinarias) y A4 (contactos y datos sensibles) con doble ruta (ADR-0020) |
| Observabilidad | Tasa de aciertos de la caché, antigüedad servida, consumo por clase CL-3/CL-4, rechazos de admisión |
| Reintentos y timeouts | Lecturas al Core: reintento acotado con espera exponencial y aleatoriedad, dentro del plazo |
| Presión de retorno | Cuotas por réplica (RI-01 a RI-08); un rechazo de admisión produce degradación o 503 |
| Fallas y recuperación | Caché caída → Core con cupo. Ambas rutas de auditoría caídas → no se responde (A4/A5). Reinicio → sin estado crítico |
| Dependencias | Core, Sistema Complementario, caché, BD de consultas, broker |
| Pruebas | Contrato por ACL (QA7); carrera de Cache-Aside con marcas de invalidación; *read-your-writes*; autorización de recursos (pruebas de acceso a objetos ajenos) |
| Trazabilidad | R1, R5; ADR-0001, ADR-0004, ADR-0011, ADR-0020; M03 de ARG-01; ARG02-M03, M05 |

### 4.3 Servicio de vinculación digital

| Aspecto | Diseño |
|---|---|
| Responsabilidad y frontera | Etapas E1 a E6 (ADR-0009). **El alta en el Core (E3) es una frontera bloqueada** |
| Puntos de entrada | API de vinculación (canal móvil); API de operaciones (consola) |
| Consumidores de API o eventos | El BFF y la consola invocan sus API; publica eventos de estado y de auditoría |
| Casos de uso | Iniciar la solicitud; verificar la identidad; identificar al cliente; autorizar la vinculación; activar el acceso; resolver revisiones manuales |
| Modelo e invariantes | INV-O1 a INV-O6; VD-01 a VD-10. **VIN-1:** la ruta `PRE_EXISTING` responde siempre con el resultado genérico mientras DD-21 no esté habilitada. **VIN-2:** la API de operaciones no puede asignar `CREATED_BY_THIS_REQUEST` sin evidencia de creación |
| Puertos y adaptadores | KYC; búsqueda en el Core; **puerto de alta (frontera bloqueada)**; firmante; operación de vinculación del IdP; repositorio |
| Adaptadores de integración | ACL del KYC; ACL de búsqueda (CL-2); adaptador del IdP |
| Persistencia | BD de vinculación: CAS, unicidad de la clave y outbox (PT-01 a PT-03 y PT-07 por el alta) |
| Límites transaccionales | Una transición de estado, su evidencia y su evento de auditoría por transacción local |
| Manejo de errores | Proveedor KYC no disponible → `IDENTITY_VERIFICATION_UNAVAILABLE`, no rechazo. Ambigüedades → `MANUAL_REVIEW` |
| Autenticación y autorización | Sesión de vinculación del solicitante (por definir en la fase de seguridad); personal con el IdP corporativo y cuatro ojos |
| Idempotencia | `Idempotency-Key` obligatoria por paso con efecto (ADR-0006) |
| Auditoría | A3 (cada etapa) y A6 (resoluciones), en el mismo *commit* |
| Observabilidad | Antigüedad por estado; tasa de indisponibilidad del KYC; volumen de vinculaciones frente a `MAX_VINCULACIONES_POR_HORA` |
| Reintentos y timeouts | KYC: reintento acotado sin repetir capturas biométricas ya enviadas. Búsqueda en el Core: lectura con reintento acotado. **Alta: sin diseño (bloqueada)** |
| Presión de retorno | Cuota CL-2 para la búsqueda; límite de volumen de la vinculación en el IdP |
| Fallas y recuperación | Reinicio: los estados en curso se retoman (INV-T5 para el alta, bloqueada). Solicitudes concurrentes del mismo solicitante: unicidad por ámbito y por identificación en curso |
| Dependencias | KYC, Core, IdP, diario (alta, bloqueado), BD de vinculación, broker |
| Pruebas | Máquina de estados (transiciones prohibidas); política VD; no enumeración; firma y verificación de afirmaciones; falla cerrada de `PRE_EXISTING` |
| Trazabilidad | R2, R3; ADR-0009, ADR-0013, ADR-0006, ADR-0020; ARG02-H04 |

### 4.4 Servicio de notificaciones

| Aspecto | Diseño |
|---|---|
| Responsabilidad y frontera | Notificar movimientos confirmados, estados, eventos de seguridad y vinculación por al menos dos canales (R4). **No confirma ni revierte operaciones** |
| Puntos de entrada | Consumidor de eventos notificables; API de registro de dispositivos |
| Consumidores de API o eventos | Consume eventos del broker; el BFF invoca la API de dispositivos |
| Casos de uso | Evaluar y resolver destinatarios; entregar con canal alternativo; registrar dispositivos |
| Modelo e invariantes | **NOT-1:** cada evento de movimiento se notifica (nunca se descarta). **NOT-2:** los eventos de estado más antiguos que el último aplicado se descartan. **NOT-3:** el contenido no lleva datos sensibles ni enlaces que soliciten datos |
| Puertos y adaptadores | Contactos (servicio de consultas); proveedores; repositorio |
| Adaptadores de integración | Adaptadores de push, SMS y correo; cliente de la API de contactos |
| Persistencia | BD de notificaciones: eventos procesados, secuencias, intentos, dispositivos, outbox |
| Límites transaccionales | El registro del evento procesado y del intento van en una transacción local; el envío al proveedor queda fuera (al menos una vez) |
| Manejo de errores | Proveedor caído → canal alternativo; agotados los intentos → cola de mensajes fallidos con alerta |
| Autenticación y autorización | Identidad de carga de trabajo hacia el servicio de consultas; credenciales por proveedor en el gestor de secretos |
| Idempotencia | Deduplicación por (evento, canal) |
| Auditoría | Intentos de entrega y registro de dispositivos (A8 o A5 según la política; los intentos de movimientos se registran como evidencia de R4) |
| Observabilidad | Tasa de entrega por canal, retraso, mensajes fallidos, eventos retenidos por secuencia |
| Reintentos y timeouts | Por proveedor, con espera exponencial y un plazo máximo |
| Presión de retorno | Limitación por proveedor; cola durable en el broker |
| Fallas y recuperación | Reinicio: reprocesamiento desde el broker con deduplicación. Eventos fuera de orden: retención por sujeto hasta `ESPERA_SECUENCIA` ([25](25-contratos-eventos-y-errores.md) §4.3) |
| Dependencias | Broker, servicio de consultas, proveedores, BD de notificaciones |
| Pruebas | Orden y duplicados; canal alternativo; contactos obsoletos; contenido minimizado |
| Trazabilidad | R4; ADR-0015, ADR-0020; ARG02-M01, M05 |

### 4.5 Servicio de auditoría

| Aspecto | Diseño |
|---|---|
| Responsabilidad y frontera | Registro de solo anexión, íntegro y consultable de la evidencia de todas las fuentes (R5). **No decide sobre operaciones** |
| Puntos de entrada | Consumidor de eventos de auditoría; API de consulta (solo lectura) |
| Consumidores de API o eventos | Consume eventos del broker; la consola invoca la API de consulta |
| Casos de uso | Validar y clasificar; deduplicar; anexar encadenado; detectar huecos; anclar; consultar |
| Modelo e invariantes | **AUD-1:** sin modificación ni borrado. **AUD-2:** un evento por identificador. **AUD-3:** cadena continua por partición. **AUD-4:** cada acceso de lectura se audita (A6) |
| Puertos y adaptadores | Repositorio de auditoría (tabla + archivo inmutable) |
| Adaptadores de integración | Consumidor del broker |
| Persistencia | Almacén de auditoría |
| Límites transaccionales | Deduplicación y anexión en la misma transacción por partición |
| Manejo de errores | Esquema inválido → cuarentena del mensaje con alerta, **nunca descarte silencioso** |
| Autenticación y autorización | Roles de auditoría (IdP corporativo); identidad de carga de trabajo para el consumidor |
| Idempotencia | Por identificador de evento |
| Auditoría | Las consultas propias se auditan (A6) |
| Observabilidad | Retraso de ingesta por fuente; huecos; verificación periódica de las cadenas |
| Reintentos y timeouts | Reintento del consumo; mensajes fallidos con alerta |
| Presión de retorno | Particiones paralelas; el broker amortigua |
| Fallas y recuperación | Almacén caído → los eventos se acumulan en el broker y en los outbox de origen, sin pérdida |
| Dependencias | Broker, almacén de auditoría, IdP corporativo |
| Pruebas | Integridad de la cadena; deduplicación; huecos; inmutabilidad; control de acceso |
| Trazabilidad | R5; ADR-0016, ADR-0020; ARG02-H03, M02 |

### 4.6 Servicio de transferencias — frontera bloqueada

**No se diseñan componentes (B-05).** El único contrato que consumen otros componentes es la **API de frontera** (inicio con `Idempotency-Key` y consulta de estado por `operationId`, [25](25-contratos-eventos-y-errores.md) §2.5) y los **eventos publicados** (§4). Ningún componente diseñado depende de la estructura interna del servicio.

## 5. Matriz de responsabilidades de seguridad por componente

| Control | Componente responsable |
|---|---|
| Sesión web, CSRF, cookie | BFF: gestor de sesión web |
| Principal y audiencia en el canal | BFF: contexto de seguridad del canal |
| Autorización de recursos (propiedad de cuentas) | Consultas: autorización de acceso a recursos |
| Autorización de la vinculación (VD) y falla cerrada `PRE_EXISTING` | Vinculación: política de autorización de vinculación |
| Afirmación firmada para el IdP | Vinculación: firmante de afirmaciones |
| Minimización en notificaciones | Notificaciones: política de notificación y adaptadores de proveedores |
| Auditoría de acceso a datos sensibles (A4) | Consultas: registrador de auditoría de doble ruta |
| Inmutabilidad y acceso restringido a la auditoría | Auditoría: repositorio y API de consulta |
| Invalidación de sesiones ante eventos de seguridad | BFF: consumidor de eventos de seguridad |

## 6. Datos por componente

| Componente | Dato | Propiedad | Consistencia |
|---|---|---|---|
| Gestor de sesión web | Sesiones, tokens cifrados | Propio (almacén de sesiones) | Fuerte por sesión |
| Repositorio Cache-Aside | Datos derivados | Derivado del Core | Eventual con marcas de invalidación |
| Registrador de auditoría de doble ruta | Eventos A4 y A5 | Propio hasta su entrega | Durable en una de dos rutas |
| Repositorio de vinculación | Estado, evidencia, claves | Propio | Fuerte (CAS) |
| Repositorio de notificaciones | Eventos procesados, secuencias, intentos, dispositivos | Propio | Local |
| Repositorio de auditoría | Registro | Propio | Solo anexión por partición |

## 7. Trazabilidad de componentes

| Requisito | Componentes |
|---|---|
| R1 | BFF (API de canal, composición, relevo de transferencias); Consultas (API, casos de uso, ACL) |
| R2 | BFF (sesión, contexto de seguridad); Vinculación (KYC, firmante, adaptador del IdP) |
| R3 | Vinculación (todos los componentes; alta bloqueada) |
| R4 | Notificaciones (consumidor, política, destinatarios, entrega, proveedores); BFF (dispositivos) |
| R5 | Auditoría (todos los componentes); Consultas (registrador de doble ruta, caché) |
| R6 | BFF (adaptación por canal) |
| C2, C5 | Control de admisión, Circuit Breakers, colas durables |
| C4 | Matriz de la sección 5 |
