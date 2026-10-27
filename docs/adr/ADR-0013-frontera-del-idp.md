# ADR-0013 — El proveedor de identidad es un contenedor interno de la plataforma

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones** en cuanto a la **frontera** (resuelve la parte de frontera de DD-03). El producto queda **provisional**; el flujo OAuth y la vinculación de tokens se deciden en la fase de seguridad (DD-03, DD-04) |
| Fecha | 2026-10-27 |
| Requisitos | R2, R3; C1, C4 |
| Drivers y supuestos | S8, S9; AQ-01; QA4, QA11 |
| Relacionados | [ADR-0009](ADR-0009-core-autoritativo.md), [ADR-0010](ADR-0010-residencia-de-datos.md), [ADR-0014](ADR-0014-api-gateway-y-bff-de-canales.md), [12 — Contexto, sección 4.2](../12-contexto-del-sistema.md#42-proveedor-de-identidad-frontera-por-decidir), [22 — Seguridad entre contenedores](../22-seguridad-entre-contenedores.md) |

## Contexto

Se asume que la institución no tiene un IdP moderno para clientes (S9). En la Fase 2 se decidió que la frontera del IdP (servicio externo o parte de la plataforma) debía resolverse **antes de cerrar el diagrama de contenedores**. La residencia de datos está pendiente de validación legal (ADR-0010). El acceso digital solo puede activarse tras la autorización de la vinculación (ADR-0009).

## Problema

¿El IdP es un sistema externo (por ejemplo, un servicio gestionado por un tercero) o un contenedor operado dentro de la plataforma?

## Decisión

1. El IdP es un **contenedor interno** de la Plataforma de Banca Digital, operado por la institución dentro de su propio entorno cloud, con su propio almacén (ADR-0016).
2. **Solo el servicio de vinculación** puede crear o activar una cuenta digital vinculada a un cliente del Core, mediante la API de administración del IdP con credenciales de servicio. **No existe auto-registro** en el IdP (ADR-0009, VD-01 a VD-03; [22](../22-seguridad-entre-contenedores.md)).
3. **Producto candidato provisional:** un servidor OAuth 2.0 / OpenID Connect autohospedado con soporte para perfiles de seguridad avanzados (por ejemplo, Keycloak). La selección final se toma en la fase de seguridad con criterios explícitos ([21](../21-alternativas-tecnologicas.md)).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| **IdP interno autohospedado** | **Seleccionada** (frontera) | Credenciales y vínculos de identidad bajo control de la institución, compatible con cualquier resultado de ADR-0010. Permite que la vinculación sea el único activador del acceso. Flexibilidad para FAPI y vinculación de tokens |
| IdP gestionado del proveedor cloud dentro de la cuenta o suscripción de la institución | **Alternativa viable** | Menor carga operativa. Debe evaluarse su ubicación de procesamiento, su soporte de FAPI y sus API de administración. Se mantiene como candidato en la fase de seguridad; seguiría siendo un contenedor dentro de la frontera si se opera en el entorno de la institución |
| IdP SaaS de un tercero (fuera del entorno de la institución) | No seleccionada | Credenciales y datos de autenticación procesados por un tercero; condicionado por ADR-0010; dependencia contractual |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Control de la residencia y de las credenciales | Operación, parches, alta disponibilidad y escalado del IdP a cargo de la institución |
| Punto único de control para activar el acceso | El IdP se vuelve un componente crítico de disponibilidad (todo acceso depende de él) |

## Consecuencias

- El IdP deja de ser un sistema externo candidato en el contexto: la vista ampliada ya no lo muestra, y las relaciones REL-10 y REL-11 se reemplazan por relaciones de contenedor ([12](../12-contexto-del-sistema.md)).
- INT-16 deja de ser una integración externa ([13](../13-catalogo-de-integraciones.md)); los cruces de confianza se actualizan en [14](../14-limites-de-confianza.md).
- Requisitos de disponibilidad del IdP en la fase de infraestructura.

## Riesgos

- Disponibilidad y seguridad operativa del IdP (RR-21).
