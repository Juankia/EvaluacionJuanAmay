# ADR-0007 — CQRS diferido hasta que se cumplan disparadores medibles

| Campo | Valor |
|---|---|
| Estado | **Diferida** (decisión arquitectónica diferida, Fase 1) |
| Fecha | 2026-10-27 |
| Requisitos | R1 (consulta de movimientos), C5 |
| Drivers y supuestos | S2, S4, S10; AQ-06, AQ-07; QA2, QA8 |
| Relacionados | [ADR-0004](ADR-0004-cache-aside.md), [ADR-0008](ADR-0008-event-driven-invalidation.md) |

## Contexto

La **segregación de responsabilidades entre comandos y consultas** (*Command Query Responsibility Segregation*, CQRS) separa los modelos de escritura y de lectura. Así la lectura puede atenderse desde proyecciones optimizadas. Se propone con frecuencia para las consultas de movimientos en banca digital.

Sin embargo:

- el Core es autoritativo (S2) y externo;
- un modelo de lectura propio requeriría alimentarse de cambios del Core, y no se sabe si el Core los emite;
- los volúmenes no están especificados (S10).

## Problema

¿Se justifica introducir CQRS con proyecciones de lectura propias en esta fase?

## Decisión

**No se implementa CQRS ahora.** Se registra como **decisión arquitectónica diferida**.

Estrategia actual:

```text
Cache-Aside + paginación + optimización de lectura + pool de conexiones + protección del Core
```

CQRS se justifica **solo si la evidencia medida demuestra la necesidad**.

### Disparadores de activación

Los umbrales son **disparadores de ingeniería propuestos**, no requisitos. Sus valores se acuerdan cuando se validen los objetivos de [04](../04-qa-scenarios.md).

| Disparador | Evidencia requerida |
|---|---|
| La latencia p95 de la consulta de movimientos supera el objetivo acordado (`T_CONSULTA_P95`) a pesar de Cache-Aside y de la optimización de lectura | Mediciones sostenidas en producción o en pruebas de carga representativas |
| El Core recibe un porcentaje excesivo de tráfico de lectura después de la caché (por encima de `RATIO_LECTURA_CORE`) | Métricas de la ACL de movimientos |
| El Core no soporta el volumen de lecturas proyectado | Pruebas de capacidad con el propietario del Core (S4) |
| Las cargas de lectura y de escritura requieren características de escalado materialmente distintas | Perfil de carga medido |

### Precondiciones para activarlo

- Que exista un mecanismo confiable para obtener los cambios del Core: eventos, captura de cambios de datos (*Change Data Capture*) autorizada por el propietario del Core, o consultas incrementales.
- Que se acepte una consistencia eventual explícita en las vistas afectadas.
- Que exista la plataforma de eventos (DD-01).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| CQRS desde el inicio | Descartada por ahora | Complejidad (proyecciones, sincronización, consistencia eventual, reconstrucción) sin evidencia de necesidad. Depende de capacidades del Core no validadas |
| **Cache-Aside y optimización de lectura; CQRS diferido** | **Seleccionada** | Menor complejidad, reversible, con una ruta de evolución clara |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Menor complejidad inicial | Si los disparadores se cumplen, la introducción posterior requiere migrar los consumidores de lectura |
| Sin consistencia eventual adicional | La carga de lectura sigue llegando parcialmente al Core |
| Decisión basada en evidencia | Requiere instrumentación desde el inicio para medir los disparadores |

## Consecuencias

- La ACL de movimientos y la caché deben exponer las métricas necesarias para evaluar los disparadores.
- La interfaz del servicio de movimientos se diseña de modo que su fuente de lectura (Core, caché o proyección) pueda cambiar sin afectar a los consumidores.

## Riesgos

- Si los volúmenes reales superan lo supuesto, la activación de CQRS podría llegar tarde. Se mitiga con pruebas de carga previas a la puesta en producción.
