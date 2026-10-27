# ADR-0008 — Invalidación de caché dirigida por eventos; la tecnología de plataforma se difiere

| Campo | Valor |
|---|---|
| Estado | **Aceptada** como estrategia conceptual. **La tecnología del bus de eventos se difiere** (DD-01) |
| Fecha | 2026-10-27 |
| Requisitos | R4, R5, C5 |
| Drivers y supuestos | S2, S4; AQ-06, AQ-08; QA2 |
| Relacionados | [ADR-0004](ADR-0004-cache-aside.md), [ADR-0007](ADR-0007-cqrs-deferred.md), [06 — Estrategia de caché, sección 4](../06-cache-strategy.md#4-invalidación-de-caché) |

## Contexto

Con Cache-Aside ([ADR-0004](ADR-0004-cache-aside.md)), un TTL por sí solo deja ventanas de inconsistencia visibles. Por ejemplo, el cliente transfiere y no ve el movimiento. Además, varios requisitos sugieren necesidades asíncronas: la notificación de cada movimiento (R4), la auditoría de todas las acciones (R5) y el desacoplamiento (C5). Sin embargo, no se ha confirmado cuáles son requisitos arquitectónicos con exigencias de entrega asíncrona.

## Problema

¿Cómo se invalidan las entradas de caché ante los cambios de estado sin introducir prematuramente una plataforma de eventos?

## Decisión

1. **Estrategia conceptual preferida:** invalidación dirigida por eventos. Cuando se completa una operación, se emite un evento de dominio (conceptualmente `AccountBalanceChanged`) y se invalidan las entradas afectadas.
2. **Respaldos obligatorios** que no dependen del bus:
   - invalidación síncrona local tras las operaciones originadas por la plataforma;
   - garantía de *read-your-writes* en la sesión del cliente en los flujos sensibles;
   - TTL de seguridad por categoría de dato.
3. **No se selecciona ahora** Kafka, Amazon MSK ni otra tecnología de eventos.
4. **No se asume** que el Core emite eventos de cambios externos a la plataforma.

### Decisión diferida: tecnología del bus de eventos (DD-01)

- **Disparador:** se confirman como requisitos arquitectónicos la invalidación de caché por eventos, la auditoría asíncrona, las notificaciones, la analítica u otros flujos asíncronos.
- **Fase:** infraestructura y plataforma.
- **Criterios de selección:**
  - garantías de entrega y de orden;
  - retención y reprocesamiento;
  - integración con el patrón Outbox para la publicación consistente;
  - operación gestionada en el proveedor cloud elegido;
  - costo;
  - residencia de datos ([ADR-0010](ADR-0010-data-residency.md)).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Solo TTL | Insuficiente como único mecanismo | Ventana de inconsistencia igual al TTL. Presiona a acortar los TTL y aumenta la carga sobre el Core |
| Seleccionar ahora un *broker* (por ejemplo, Kafka o MSK) | Descartada en esta fase | Decisión de tecnología antes de confirmar los requisitos asíncronos. Riesgo de sobredimensionar la solución («Kafka por escalabilidad») |
| **Eventos como estrategia, con respaldos locales y la tecnología diferida** | **Seleccionada** | Mantiene la dirección arquitectónica sin comprometer la tecnología |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Consistencia más ajustada en las operaciones propias desde el inicio (invalidación local) | Hasta que exista un bus, los cambios externos solo se acotan por TTL |
| Sin dependencia prematura de una plataforma de eventos | Riesgo de que varios requisitos asíncronos demanden la decisión al mismo tiempo |
| Selección de tecnología informada por requisitos confirmados | La publicación confiable (Outbox) se diseña cuando se decida el bus |

## Consecuencias

- Los eventos de dominio se nombran en inglés según las convenciones (por ejemplo, `AccountBalanceChanged`, `TransferCompleted`) y se documentan como contratos conceptuales.
- La fase de notificaciones y la de auditoría deben confirmar si requieren entrega asíncrona garantizada. Esa confirmación activa DD-01.

## Riesgos

- RR-05: fallas en la invalidación o en la entrega de eventos.
