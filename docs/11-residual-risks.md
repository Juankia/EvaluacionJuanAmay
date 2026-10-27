# 11 — Registro de riesgos residuales

| Campo | Valor |
|---|---|
| Fase | 0–1 |
| Estado | Vigente |

## 1. Propósito

Este registro documenta los riesgos que **persisten después** de las decisiones de las Fases 0–1. Un riesgo residual no es un defecto del diseño: es el costo conocido y aceptado de una decisión, con su mitigación y su disparador de reevaluación.

Escala de impacto: **Alto**, **Medio**, **Bajo**.

## 2. Riesgos

### RR-01 — Lógica de integración duplicada entre las ACL

| Campo | Descripción |
|---|---|
| Riesgo | Cada ACL reimplementa preocupaciones similares (mapeo de errores, configuración de clientes, pruebas). Las implementaciones divergen |
| Origen | [ADR-0001](adr/ADR-0001-domain-acl.md) |
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
| Origen | [ADR-0006](adr/ADR-0006-idempotency-reconciliation.md) |
| Impacto | Alto. Una conciliación errónea puede marcar como fallida una operación aplicada (riesgo de duplicación si el cliente la repite) o al revés |
| Mitigación | Las coincidencias ambiguas no se resuelven automáticamente (`REQUIRES_RECONCILIATION`). Tiempo de asentamiento antes de concluir «sin coincidencia». Inhibición de operaciones equivalentes mientras otra está en `UNKNOWN`. Conciliación diaria de segunda línea. **Validación prioritaria del supuesto S3** |
| Riesgo residual | Medio. **La correlación de negocio no ofrece las mismas garantías que la idempotencia nativa** |
| Disparador de reevaluación | Resultado de la validación de S3. Tasa de casos en `REQUIRES_RECONCILIATION` por encima de lo tolerable para operaciones |

### RR-05 — Fallas en la invalidación de caché o en la entrega de eventos

| Campo | Descripción |
|---|---|
| Riesgo | Un evento de invalidación se pierde o se retrasa, o el Core no emite eventos de cambios externos a la plataforma. El cliente ve datos desactualizados |
| Origen | [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0008](adr/ADR-0008-event-driven-invalidation.md) |
| Impacto | Medio. Confusión del cliente y reclamos; no afecta la integridad, porque las decisiones transaccionales no usan la caché |
| Mitigación | TTL de seguridad por categoría. Invalidación síncrona local tras las operaciones propias. *Read-your-writes* en la sesión. Indicador de frescura. No se cachea el saldo para decisiones |
| Riesgo residual | Bajo-medio |
| Disparador de reevaluación | Reclamos por datos desactualizados. Se confirma que el Core no emite eventos |

### RR-06 — Decisión pendiente sobre residencia de datos y cumplimiento

| Campo | Descripción |
|---|---|
| Riesgo | La región cloud, los proveedores de KYC y biometría o las transferencias internacionales de datos resultan incompatibles con la legislación o con las políticas de la institución |
| Origen | [ADR-0010](adr/ADR-0010-data-residency.md), S8 |
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
| Origen | [ADR-0005](adr/ADR-0005-transaction-state-machine.md) |
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
