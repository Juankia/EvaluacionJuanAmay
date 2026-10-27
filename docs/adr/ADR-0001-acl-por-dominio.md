# ADR-0001 — Capas anticorrupción por dominio para la integración con el Core

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (Fase 1). **Complementada** en la remediación ARG-01 por [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md) (ARG01-H04) |
| Fecha | 2026-10-27 |
| Requisitos | R1, C2, C5 |
| Drivers y supuestos | S1, S2, S3, S4, S6, S7, S11, S12; QA1, QA7 |
| Relacionados | [ADR-0002](ADR-0002-frontera-api-gateway.md), [ADR-0003](ADR-0003-integration-platform-sdk.md), [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md), [05 — Frontera de integración](../05-frontera-de-integracion-core.md) |

## Contexto

La Plataforma de Banca Digital consume información y ejecuta operaciones en dos sistemas externos: el Core Bancario y el sistema complementario de datos extendidos del cliente. Se asume que el Core es heredado (S1) y autoritativo (S2), que podría exponer contratos heredados sin idempotencia (S3) y que tiene una capacidad limitada (S4). El reto exige al menos tres servicios: datos del cliente, movimientos y transferencias (R1). También exige que se puedan agregar componentes sin impactar los existentes (C5).

## Problema

¿Dónde y cómo cruza la plataforma la frontera hacia el Core? Se busca que:

- el modelo heredado no se filtre al dominio ni a los canales;
- una falla en una ruta de integración no degrade funciones no relacionadas;
- el Core quede protegido del tráfico de los canales;
- un cambio o reemplazo del Core tenga un impacto acotado.

## Decisión

Cada dominio que se integra con el Core (clientes, movimientos, transferencias) tiene su propia **capa anticorrupción** (*Anti-Corruption Layer*, ACL), que:

1. traduce el contrato externo o heredado al modelo de dominio de la banca digital y viceversa; **el modelo del Core no se filtra al dominio**;
2. traduce los errores propietarios a errores de dominio con semántica explícita, incluido el resultado desconocido;
3. aplica las **políticas de resiliencia locales de cada operación**: timeouts, Circuit Breaker, Bulkhead y reintento solo cuando es semánticamente seguro;
4. acota la concurrencia hacia el Core con un pool de conexiones dedicado, dentro de la cuota de su clase de operación definida en [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md);
5. clasifica cada resultado de invocación según la taxonomía cerrada de [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md) y mantiene la tabla de clasificación de su contrato;
6. se verifica con pruebas de contrato.

**No se crea una capa de integración centralizada** como servicio en tiempo de ejecución por defecto. Las preocupaciones técnicas comunes se proveen mediante el Integration Platform SDK ([ADR-0003](ADR-0003-integration-platform-sdk.md)).

La forma de despliegue de cada ACL (módulo dentro del servicio de dominio o componente desplegable) se decide en la fase de contenedores (DD-10). Esta decisión **no** presupone microservicios adicionales.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| **A. Acceso directo de cada servicio al Core** | Descartada | Acoplamiento fuerte. Filtración del modelo heredado. Preocupaciones de integración repetidas y dispersas. Reemplazo del Core costoso. Mayor superficie de pruebas. Acceso no controlado al Core. El acceso directo a la base de datos principal se descarta con mayor razón (S12) |
| **B. Capa de integración centralizada** | No seleccionada en este contexto | Es **válida** cuando hay un equipo de integración centralizado y fuerte, políticas centralizadas de forma intencional, aislamiento estricto de SLA y Bulkhead dentro de la capa, pocos dominios y propiedad operativa centralizada. Aquí se priorizan el aislamiento por dominio y la resiliencia granular. Además, la opción B introduce un punto común de contención y tiende a acumular lógica de negocio |
| **C. ACL por dominio** | **Seleccionada** | Aislamiento de dominio y resiliencia granular |

La selección es **contextual**, no una ley arquitectónica. Si el equipo tuviera solo 1–2 ingenieros y la complejidad de integración fuera baja (S11), la opción B podría ser económicamente preferible.

## Trade-offs

| Se gana | Se paga |
|---|---|
| Aislamiento de dominios y de fallas | Lógica de integración duplicada |
| Menor filtración del modelo heredado | Más adaptadores que construir y mantener |
| Resiliencia granular por operación | Más pruebas (contrato por ACL) |
| Autonomía de equipos y evolución independiente | Más superficies operativas que monitorear |
| Reemplazo o incorporación de otro Core más sencillos | Riesgo de políticas inconsistentes entre las ACL |

El SDK mitiga la duplicación; **no elimina el costo arquitectónico**.

## Consecuencias

- Los servicios de dominio dependen de interfaces propias (puertos), no de contratos del Core.
- Un cambio de contrato del Core se absorbe en la ACL afectada (QA7).
- Cada ACL documenta su catálogo de políticas de resiliencia, que se revisa en el gobierno de arquitectura.
- La consulta de datos del cliente combina el Core y el sistema complementario detrás de la misma frontera de traducción. La regla de autoridad del dato se define en [ADR-0009](ADR-0009-core-autoritativo.md).
- Debe existir un responsable identificado de cada ACL.

### Aislamiento lógico frente a aislamiento de capacidad (enmienda ARG-01)

Las ACL proporcionan **aislamiento lógico**: del modelo, del código, del ciclo de liberación y de los recursos de la plataforma. **No** aíslan la capacidad del Core físico, que todas comparten. Sin una medida adicional, la saturación de consultas puede degradar las escrituras críticas en el propio Core, y los límites por instancia se multiplican con las réplicas. Esta es una ventaja natural de la opción B que la opción C debe compensar de forma explícita. Lo hace [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md): presupuesto global, cuotas por clase de operación con escrituras y conciliación reservadas, asignación estática derivada del máximo de réplicas y control de admisión con comportamiento seguro.

## Riesgos

- RR-01: lógica de integración duplicada.
- RR-03: cambios en el contrato del Core.
- RR-07: enrutamiento futuro entre varios Core.
- RR-14: cuotas de capacidad del Core mal dimensionadas ([ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md)).

Ver [11 — Riesgos residuales](../11-riesgos-residuales.md).
