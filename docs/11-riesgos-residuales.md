# 11 — Registro de riesgos residuales

| Campo | Valor |
|---|---|
| Fase | 0–2 (actualizado en la remediación ARG-01) |
| Estado | Vigente |

## 1. Propósito

Este registro documenta los riesgos que **persisten después** de las decisiones de las Fases 0–1. Un riesgo residual no es un defecto del diseño: es el costo conocido y aceptado de una decisión, con su mitigación y su disparador de reevaluación.

Escala de impacto: **Alto**, **Medio**, **Bajo**.

## 2. Riesgos

### RR-01 — Lógica de integración duplicada entre las ACL

| Campo | Descripción |
|---|---|
| Riesgo | Cada ACL reimplementa preocupaciones similares (mapeo de errores, configuración de clientes, pruebas). Las implementaciones divergen |
| Origen | [ADR-0001](adr/ADR-0001-acl-por-dominio.md) |
| Impacto | Medio. Más esfuerzo de mantenimiento y comportamiento inconsistente ante fallas |
| Mitigación | Integration Platform SDK para los mecanismos comunes. Catálogo de políticas por ACL revisado en el gobierno de arquitectura. Plantilla de referencia para las nuevas ACL |
| Riesgo residual | Medio-bajo. El SDK mitiga la duplicación pero no la elimina |
| Disparador de reevaluación | Incidentes causados por políticas inconsistentes, o más de una corrección idéntica aplicada en varias ACL en un mismo periodo |

### RR-02 — El SDK se convierte en una «God Library»

| Campo | Descripción |
|---|---|
| Riesgo | El Integration Platform SDK acumula lógica de negocio, transformaciones de dominio u orquestación. Se convierte en un acoplamiento central encubierto |
| Origen | [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) |
| Impacto | Alto. Anula los beneficios de la Opción C y acopla las liberaciones de todos los dominios |
| Mitigación | Lista explícita de responsabilidades permitidas y prohibidas. Revisión obligatoria de arquitectura para los cambios del SDK. Regla de «sin tipos de dominio» verificable con pruebas de arquitectura automatizadas. Versionado semántico y compatibilidad hacia atrás |
| Riesgo residual | Bajo-medio. Depende de la disciplina del gobierno |
| Disparador de reevaluación | Aparece en el SDK un tipo, un nombre o una regla de un dominio de negocio, o un cambio del SDK obliga a desplegar a la vez todos los dominios |

### RR-03 — Cambios en el contrato del Core

| Campo | Descripción |
|---|---|
| Riesgo | El proveedor o el propietario del Core modifica contratos sin aviso suficiente |
| Origen | S1, S3 |
| Impacto | Alto en el dominio afectado |
| Mitigación | El cambio queda contenido en la ACL afectada (QA7). Pruebas de contrato por ACL. Acuerdo de gestión de cambios con el propietario del Core |
| Riesgo residual | Medio. La ACL contiene el impacto, pero no evita la interrupción si el cambio no se anuncia |
| Disparador de reevaluación | Primer cambio de contrato no anunciado |

### RR-04 — Conciliación sin consulta por clave de idempotencia nativa

| Campo | Descripción |
|---|---|
| Riesgo | Si el Core no permite consultar por clave o por referencia externa (S3), la conciliación de las operaciones `UNKNOWN` depende de la correlación de negocio, que es probabilística |
| Origen | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) |
| Impacto | Alto. Una conciliación errónea puede marcar como fallida una operación aplicada (riesgo de duplicación si el cliente la repite) o al revés |
| Mitigación | Las coincidencias ambiguas no se resuelven automáticamente (`REQUIRES_RECONCILIATION`). Tiempo de asentamiento antes de concluir «sin coincidencia». **Bloqueo obligatorio** de operaciones equivalentes mientras otra está en clase `UNKNOWN` ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), sección 3; remediación ARG-01). Exactitud medida (`TASA_ERROR_CONCILIACION`). Conciliación diaria de segunda línea. **Validación prioritaria del supuesto S3** |
| Riesgo residual | Medio. **La correlación de negocio no ofrece las mismas garantías que la idempotencia nativa.** *Actualización CF-01:* como la evidencia negativa ya no cierra ni reenvía, el riesgo pasa de la **duplicación** a la **vivacidad** (RR-17). Persiste el riesgo de una confirmación errónea por EV-POS-CORR |
| Disparador de reevaluación | Resultado de la validación de S3. Tasa de casos en `REQUIRES_RECONCILIATION` por encima de lo tolerable para operaciones |

### RR-05 — Fallas en la invalidación de caché o en la entrega de eventos

| Campo | Descripción |
|---|---|
| Riesgo | Un evento de invalidación se pierde o se retrasa, o el Core no emite eventos de cambios externos a la plataforma. El cliente ve datos desactualizados |
| Origen | [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0008](adr/ADR-0008-invalidacion-por-eventos.md) |
| Impacto | Medio. Confusión del cliente y reclamos; no afecta la integridad, porque las decisiones transaccionales no usan la caché |
| Mitigación | TTL de seguridad por categoría. Invalidación síncrona local tras las operaciones propias. *Read-your-writes* en la sesión. Indicador de frescura. No se cachea el saldo para decisiones |
| Riesgo residual | Bajo-medio |
| Disparador de reevaluación | Reclamos por datos desactualizados. Se confirma que el Core no emite eventos |

### RR-06 — Decisión pendiente sobre residencia de datos y cumplimiento

| Campo | Descripción |
|---|---|
| Riesgo | La región cloud, los proveedores de KYC y biometría o las transferencias internacionales de datos resultan incompatibles con la legislación o con las políticas de la institución |
| Origen | [ADR-0010](adr/ADR-0010-residencia-de-datos.md), S8 |
| Impacto | Alto. Puede invalidar la selección de infraestructura y de proveedores |
| Mitigación | La arquitectura lógica (Fases 0–2) es independiente de la región. La decisión de región se condiciona a la validación legal |
| Riesgo residual | Alto hasta obtener la validación legal |
| Disparador de reevaluación | Opinión del área legal y de cumplimiento; cambios normativos |

### RR-07 — Complejidad futura de enrutamiento entre varios Core

| Campo | Descripción |
|---|---|
| Riesgo | Si se incorpora otro Core (S7), decidir a qué Core se envía cada operación (por cliente, por producto o durante una migración) agrega una complejidad que las ACL actuales no resuelven |
| Origen | S7 |
| Impacto | Medio |
| Mitigación | Las ACL aíslan el modelo de cada Core. El enrutamiento se diseñará como una decisión explícita de dominio, nunca dentro del SDK |
| Riesgo residual | Medio. Diferido conscientemente |
| Disparador de reevaluación | Anuncio de una migración de Core o de un segundo Core |

### RR-08 — Complejidad del estado transaccional durable

| Campo | Descripción |
|---|---|
| Riesgo | La máquina de estados durable agrega persistencia, procesos de avance, conciliación y operación manual. Pueden aparecer errores de implementación (transiciones inválidas, operaciones atascadas) |
| Origen | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) |
| Impacto | Medio |
| Mitigación | Modelo explícito de estados y transiciones válidas. Pruebas basadas en el modelo. Monitoreo de la antigüedad por estado. Procedimientos operativos para `REQUIRES_RECONCILIATION` |
| Riesgo residual | Bajo-medio. Es el costo aceptado frente al riesgo mayor de no modelar `UNKNOWN` |
| Disparador de reevaluación | Operaciones atascadas por encima de los umbrales operativos |

### RR-09 — Atribución regulatoria incorrecta

| Campo | Descripción |
|---|---|
| Riesgo | Se aplican a una cooperativa normas exclusivas de los bancos, o al revés, o se afirma un cumplimiento no verificado |
| Origen | S14, S15 |
| Impacto | Medio-alto. Pérdida de credibilidad ante el comité y decisiones mal fundamentadas |
| Mitigación | [Política de documentación](00-politica-de-documentacion.md), sección 5: clasificación `REG` y `REG-PV`, sin afirmaciones de cumplimiento |
| Riesgo residual | Bajo |
| Disparador de reevaluación | Confirmación del tipo de institución |

### RR-10 — Topología de transferencias interbancarias no validada

| Campo | Descripción |
|---|---|
| Riesgo | Se diseñan los contenedores de transferencias sin conocer si la plataforma se integra directamente, mediante la infraestructura existente o mediante un intermediario |
| Origen | S13, DD-16 (Fase 2) |
| Impacto | Alto. Afecta a la máquina de estados, a las integraciones críticas y a las obligaciones del participante del sistema de pagos |
| Mitigación | Se modela como capacidad lógica en el contexto. La máquina de estados es conceptual. La ACL de transferencias aísla la topología del dominio. Se prioriza Q-01 |
| Riesgo residual | Medio |
| Disparador de reevaluación | Respuesta a Q-01 |

### RR-11 — Datos sensibles tratados por terceros

| Campo | Descripción |
|---|---|
| Riesgo | El proveedor KYC/biométrico (y el IdP, si es externo) trata datos biométricos, de identidad o credenciales fuera de la institución |
| Origen | S16, DD-03, DD-14 (Fase 2) |
| Impacto | Alto. Exposición de datos sensibles, dependencia de proveedor y posibles restricciones de residencia |
| Mitigación | Minimización de datos. Evaluación de terceros. Condiciones de retención y eliminación. Decisión de residencia sujeta a [ADR-0010](adr/ADR-0010-residencia-de-datos.md) |
| Riesgo residual | Medio-alto hasta resolver Q-06 |
| Disparador de reevaluación | Respuesta a Q-06; selección de proveedor |

### RR-12 — Alcance incompleto de la notificación de movimientos

| Campo | Descripción |
|---|---|
| Riesgo | Si R4 abarca movimientos originados fuera del canal digital y el Core no puede informarlos, la plataforma no podría notificar «cada movimiento» |
| Origen | R4, S14, Q-04 (Fase 2) |
| Impacto | Medio-alto. Posible incumplimiento del requisito del reto y de la norma subyacente |
| Mitigación | Relación REL-13 identificada como candidata. Alternativa: que la notificación de esos movimientos la haga otro sistema institucional |
| Riesgo residual | Medio |
| Disparador de reevaluación | Respuesta a Q-04 |

### RR-13 — Duplicado semántico con claves distintas

| Campo | Descripción |
|---|---|
| Riesgo | Dos operaciones con claves de idempotencia distintas representan la misma intención financiera: reinstalación de la app, dos dispositivos, repetición por error o un canal defectuoso |
| Origen | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), sección 3 (remediación ARG01-C01) |
| Impacto | Alto. Débito duplicado que la deduplicación G1 no puede detectar |
| Mitigación | Bloqueo obligatorio durante la incertidumbre. Confirmación explícita dentro de `VENTANA_DUPLICADO_SEMANTICO`. Requisito del canal de persistir y reutilizar la clave (IC-02). Métrica `TASA_DUPLICADO_SEMANTICO` |
| Riesgo residual | Medio. Los duplicados confirmados por el usuario o fuera de la ventana no se previenen |
| Disparador de reevaluación | `TASA_DUPLICADO_SEMANTICO` o reclamos por duplicados por encima de lo tolerable |

### RR-14 — Cuotas de capacidad del Core mal dimensionadas

| Campo | Descripción |
|---|---|
| Riesgo | Cuotas demasiado bajas (rechazos artificiales), demasiado altas o con un `N_MAX_REPLICAS` desactualizado (no protegen). Si se activa la coordinación dinámica, esta puede convertirse en un punto de falla |
| Origen | [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md) (remediación ARG01-H04) |
| Impacto | Medio-alto |
| Mitigación | Presupuesto acordado con el propietario del Core (Q-16). Asignación estática M1 sin dependencias en tiempo de ejecución. Respaldo a M1 ante la falla de la coordinación. Métricas de rechazo por clase. Revisión de cuotas en cada cambio de `N_MAX_REPLICAS` |
| Riesgo residual | Medio, hasta conocer la capacidad real del Core |
| Disparador de reevaluación | Rechazos de CL-1 o CL-2; saturación del Core; cambio de capacidad del Core |

### RR-15 — Vinculación indebida de clientes preexistentes

| Campo | Descripción |
|---|---|
| Riesgo | Un suplantador que supera la verificación de identidad intenta vincular su dispositivo a un cliente existente |
| Origen | [ADR-0009](adr/ADR-0009-core-autoritativo.md) (remediación ARG01-H05) |
| Impacto | Alto. Acceso a productos financieros existentes |
| Mitigación | La coincidencia de identificación no autoriza la vinculación (VD-01). Prueba de control con un factor independiente previamente registrado (VD-03). No enumeración (VD-07). Restricciones iniciales (VD-10). Escenario QA11 |
| Riesgo residual | Medio, hasta que la fase de seguridad defina el mecanismo (DD-21) y se valide la calidad de los factores registrados en el Core (Q-15) |
| Disparador de reevaluación | Definición de DD-21; incidentes de fraude en la vinculación |

### RR-16 — Reverso no posible por la vía automática

| Campo | Descripción |
|---|---|
| Riesgo | Tras un rechazo externo posterior al débito, el reverso no puede ejecutarse automáticamente (por ejemplo, porque la devolución sigue un procedimiento interinstitucional). La obligación con el cliente queda pendiente |
| Origen | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), INV-T6 (remediación ARG01-H02) |
| Impacto | Alto para el cliente afectado (fondos debitados) |
| Mitigación | Estado `REQUIRES_RECONCILIATION` con la obligación registrada en la dimensión de reverso. Responsable, plazo y escalamiento (sección 6 de ADR-0005). Comunicación al cliente |
| Riesgo residual | Medio, hasta conocer la topología y los procedimientos de devolución (Q-01) |
| Disparador de reevaluación | Respuesta a Q-01; antigüedad de los casos con reverso pendiente |

### RR-17 — Operaciones sin resolución concluyente

| Campo | Descripción |
|---|---|
| Riesgo | Sin evidencia de no ejecución (EV-NOEXEC) del Core, las operaciones potencialmente enviadas y no encontradas no pueden cerrarse automáticamente: quedan en `REQUIRES_RECONCILIATION` y bloquean las operaciones equivalentes del cliente |
| Origen | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) v3 §3 y §8 (CF-01): se prioriza la seguridad sobre la vivacidad |
| Impacto | Medio-alto. Clientes bloqueados para una operación equivalente; carga operativa; reclamos |
| Mitigación | Escalamiento acotado; cierre excepcional con autoridad; validación prioritaria de las capacidades del Core (Q-17, DD-22); dimensionamiento del rol de conciliación |
| Riesgo residual | Medio-alto hasta conocer las capacidades del Core |
| Disparador de reevaluación | Volumen o antigüedad de `REQUIRES_RECONCILIATION`; respuesta a Q-17 |

### RR-18 — Ejecución tardía única de un ejecutor zombi

| Campo | Descripción |
|---|---|
| Riesgo | Un ejecutor que perdió la titularidad envía tarde la **única** solicitud del tramo; el Core la ejecuta fuera del tiempo que el cliente esperaba |
| Origen | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) v3 §4.4 (CF-02) |
| Impacto | Medio. No hay duplicación, pero sí una ejecución tardía; si ocurre después de `CLOSED_BY_EXCEPTION`, genera un caso de efecto tardío |
| Mitigación | `MAX_DESFASE_ENVIO`; operaciones equivalentes bloqueadas mientras la operación no sea terminal; evidencia huérfana; vigilancia de efecto tardío; expiración de solicitudes en el Core si existe (DD-22) |
| Riesgo residual | Bajo-medio |
| Disparador de reevaluación | Casos de efecto tardío; respuesta a Q-17 |

### RR-19 — Durabilidad del almacén transaccional y restauración

| Campo | Descripción |
|---|---|
| Riesgo | Una conmutación con replicación asíncrona, una restauración desde un respaldo o una topología con varios escritores pierden o duplican permisos de envío, y la plataforma reenvía al Core |
| Origen | Verificación acotada de la Fase 3, V-3 a V-5 ([23](23-riesgos-y-condiciones-fase-3.md)) |
| Impacto | **Alto**: duplicación financiera originada en la plataforma |
| Mitigación | PT-01 (RPO 0 de las escrituras confirmadas), PT-02 (cuarentena), PT-03 (escritor único) en [ADR-0016](adr/ADR-0016-persistencia-y-durabilidad-del-estado.md) |
| Riesgo residual | Alto hasta verificar PT-01 a PT-03 en la infraestructura. *Actualización ARG-02:* ARG02-C01 mostró que PT-01 y PT-02 no cubrían la DR regional. Con ADR-0021 (diario independiente y PT-07/PT-08), el residual queda en **medio**, condicionado a DD-23 |
| Disparador de reevaluación | Diseño de infraestructura y de DR; simulacros |

### RR-20 — Complejidad operativa de la arquitectura híbrida

| Campo | Descripción |
|---|---|
| Riesgo | Cinco servicios, un BFF, un IdP, varios almacenes y un broker exigen capacidades operativas y de observabilidad que el equipo podría no tener |
| Origen | [ADR-0012](adr/ADR-0012-descomposicion-hibrida-de-contenedores.md) |
| Impacto | Medio |
| Mitigación | Servicios gestionados; observabilidad estándar; reevaluar la alternativa A si S11 es falso |
| Riesgo residual | Medio |
| Disparador de reevaluación | Respuesta a Q-18; incidentes operativos |

### RR-21 — El IdP interno como componente crítico

| Campo | Descripción |
|---|---|
| Riesgo | Todo acceso depende del IdP; su operación (parches, alta disponibilidad, seguridad) recae en la institución |
| Origen | [ADR-0013](adr/ADR-0013-frontera-del-idp.md) |
| Impacto | Medio-alto |
| Mitigación | Despliegue multizona, base de datos replicada, endurecimiento y monitoreo; evaluar un IdP gestionado dentro del entorno de la institución en la fase de seguridad |
| Riesgo residual | Medio-alto hasta la fase de seguridad y de infraestructura |
| Disparador de reevaluación | Selección del producto de IdP |

### RR-22 — Dependencia de la red plataforma–Core

| Campo | Descripción |
|---|---|
| Riesgo | La red hacia el Core (de ubicación desconocida, S21) es un dominio de falla común y el factor dominante de la latencia |
| Origen | ARG02-H05 |
| Impacto | Alto |
| Mitigación | Registro de S21 y Q-20; enlaces redundantes en cualquier modelo; timeouts que distinguen la conexión de la lectura |
| Riesgo residual | Alto hasta responder Q-20 |
| Disparador de reevaluación | Respuesta a Q-20 |

### RR-23 — Disponibilidad ligada a la auditoría

| Campo | Descripción |
|---|---|
| Riesgo | Las acciones con falla cerrada (A1 a A4, A6) no se ejecutan si no hay evidencia durable |
| Origen | [ADR-0020](adr/ADR-0020-politica-de-auditoria-por-categoria.md) |
| Impacto | Medio |
| Mitigación | Doble ruta durable para A4 y A5; outbox local para A1, A3 y A6 |
| Riesgo residual | Bajo-medio |
| Disparador de reevaluación | Respuesta a Q-19 |

### RR-24 — Corrección y disponibilidad del diario de despacho

| Campo | Descripción |
|---|---|
| Riesgo | Un defecto o una indisponibilidad del diario detiene los despachos o, si su semántica no es la requerida, compromete INV-DR |
| Origen | [ADR-0021](adr/ADR-0021-diario-de-autorizacion-de-despacho.md) |
| Impacto | Alto |
| Mitigación | Requisitos de linealizabilidad y quórum (DD-23); falla cerrada; validación de la semántica antes de la implementación |
| Riesgo residual | Medio |
| Disparador de reevaluación | Selección de DD-23 |

### RR-25 — Disponibilidad de la web ligada al almacén de sesiones

| Campo | Descripción |
|---|---|
| Riesgo | Si cae el almacén de sesiones, los usuarios web deben volver a iniciar sesión |
| Origen | [ADR-0019](adr/ADR-0019-sesiones-del-bff.md) |
| Impacto | Medio |
| Mitigación | Alta disponibilidad gestionada; el canal móvil no se ve afectado |
| Riesgo residual | Bajo-medio |
| Disparador de reevaluación | Incidentes |

## 3. Resumen

| ID | Riesgo | Impacto | Riesgo residual |
|---|---|---|---|
| RR-01 | Lógica de integración duplicada | Medio | Medio-bajo |
| RR-02 | SDK como «God Library» | Alto | Bajo-medio |
| RR-03 | Cambios en el contrato del Core | Alto | Medio |
| RR-04 | Conciliación sin idempotencia nativa | Alto | Medio |
| RR-05 | Fallas en la invalidación de caché | Medio | Bajo-medio |
| RR-06 | Residencia de datos pendiente | Alto | Alto (hasta la validación) |
| RR-07 | Enrutamiento entre varios Core | Medio | Medio |
| RR-08 | Complejidad del estado durable | Medio | Bajo-medio |
| RR-09 | Atribución regulatoria incorrecta | Medio-alto | Bajo |
| RR-10 | Topología de transferencias no validada | Alto | Medio |
| RR-11 | Datos sensibles tratados por terceros | Alto | Medio-alto |
| RR-12 | Alcance incompleto de la notificación de movimientos | Medio-alto | Medio |
| RR-13 | Duplicado semántico con claves distintas | Alto | Medio |
| RR-14 | Cuotas de capacidad del Core mal dimensionadas | Medio-alto | Medio |
| RR-15 | Vinculación indebida de clientes preexistentes | Alto | Medio |
| RR-16 | Reverso no posible por la vía automática | Alto | Medio |
| RR-17 | Operaciones sin resolución concluyente | Medio-alto | Medio-alto |
| RR-18 | Ejecución tardía única de un ejecutor zombi | Medio | Bajo-medio |
| RR-19 | Durabilidad del almacén transaccional y restauración | Alto | Alto |
| RR-20 | Complejidad operativa de la arquitectura híbrida | Medio | Medio |
| RR-21 | IdP interno como componente crítico | Medio-alto | Medio-alto |
| RR-22 | Dependencia de la red plataforma–Core | Alto | Alto |
| RR-23 | Disponibilidad ligada a la auditoría | Medio | Bajo-medio |
| RR-24 | Corrección y disponibilidad del diario de despacho | Alto | Medio |
| RR-25 | Disponibilidad de la web ligada al almacén de sesiones | Medio | Bajo-medio |
