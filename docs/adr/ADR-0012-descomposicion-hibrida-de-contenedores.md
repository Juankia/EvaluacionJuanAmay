# ADR-0012 — Descomposición híbrida de contenedores y ubicación de las ACL

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones** (Fase 3; pendiente de la revisión ARG-02) |
| Fecha | 2026-10-27 |
| Requisitos | R1, R3, R4, R5; C2, C5 |
| Drivers y supuestos | S4, S7, S11; AQ-02, AQ-03, AQ-07, AQ-08, AQ-10 |
| Relacionados | [ADR-0001](ADR-0001-acl-por-dominio.md), [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md), [ADR-0014](ADR-0014-api-gateway-y-bff-de-canales.md), [20 — Alternativas de descomposición](../20-alternativas-de-descomposicion.md), [16 — Arquitectura de contenedores](../16-arquitectura-de-contenedores.md) |

## Contexto

La Fase 3 debe definir los contenedores ejecutables de la plataforma. Las capacidades son:

- consultas, vinculación, transferencias, notificaciones, auditoría, identidad, conciliación y canales;
- con dependencias muy distintas del Core (CL-1 a CL-4) y exigencias de aislamiento distintas (estado financiero, datos biométricos, auditoría).

ADR-0001 fijó ACL por dominio, pero difirió su forma de despliegue (DD-10).

## Problema

¿Qué capacidades se despliegan por separado y dónde viven las ACL, sin multiplicar los servicios por defecto ni concentrar en un único despliegue riesgos con perfiles de falla incompatibles?

## Decisión

1. Se adopta una **arquitectura híbrida** con un número limitado de contenedores desplegables, seleccionada con la matriz de [20](../20-alternativas-de-descomposicion.md). Un **dominio solo se despliega por separado** si cumple al menos uno de estos criterios:
   - **(a)** un perfil de capacidad o de escalado distinto frente al Core (ADR-0011);
   - **(b)** un aislamiento de seguridad o de datos sensibles;
   - **(c)** un aislamiento de fallas que proteja la integridad financiera;
   - **(d)** un ciclo de cambio o un responsable claramente distintos.
2. **Contenedores de dominio:**
   - Servicio de consultas (cliente, cuentas, movimientos);
   - Servicio de vinculación digital;
   - Servicio de transferencias;
   - Servicio de notificaciones;
   - Servicio de auditoría.
   - Más los contenedores de canal y de borde (ADR-0014), el IdP (ADR-0013), el broker (ADR-0015), los almacenes (ADR-0016) y la consola de operaciones (provisional, DD-18).
3. **Las ACL son módulos internos** del servicio dueño del dominio, **no contenedores**:

   | ACL | Servicio que la contiene |
   |---|---|
   | ACL de clientes (lectura), ACL de movimientos, ACL del Sistema Complementario | Servicio de consultas |
   | ACL de alta y búsqueda de clientes | Servicio de vinculación |
   | ACL de transferencias (y, cuando se resuelva Q-01, la del procesamiento externo) | Servicio de transferencias |

   Así, el **permiso de envío único** y el envío al Core ocurren en el mismo proceso, sin un salto de red intermedio que agregue otro punto de resultado desconocido.
4. **La conciliación** es un rol de proceso dentro de cada servicio dueño del estado (transferencias, vinculación), **no un contenedor aparte**. Así se evita compartir bases de datos entre contenedores.
5. **El Integration Platform SDK** es una biblioteca (ADR-0003) y **no aparece como contenedor**.

## Alternativas consideradas

| Alternativa | Resultado | Razón principal |
|---|---|---|
| A. Monolito modular | No seleccionada | Mezcla en un mismo proceso y en un mismo pool de recursos el estado financiero, los datos biométricos y las consultas de alto volumen. Una falla o una saturación de las consultas afecta a las transferencias. No permite escalar las consultas sin escalar las escrituras críticas (ADR-0011) |
| **B. Híbrida** | **Seleccionada** | Equilibra aislamiento, escalado y costo operativo |
| C. Microservicios por dominio (un servicio por capacidad, más las ACL como adaptadores desplegables) | No seleccionada | Más saltos de red en la ruta crítica, más puntos de resultado desconocido, mayor costo operativo y de observabilidad sin un driver medido. Contradice «un dominio no requiere un microservicio» |
| ACL como adaptadores desplegables independientes | Descartada | Rompe la unidad entre el permiso de envío y el envío; agrega latencia y un punto de falla |
| Capa de integración central | Descartada | Ya descartada en ADR-0001 (opción B) |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Aislamiento de fallas y de capacidad entre consultas y escrituras críticas | Cinco servicios de dominio que operar, observar y desplegar |
| Datos sensibles (biometría, estado financiero, auditoría) en contenedores y almacenes separados | Duplicación del mapeo del modelo de cliente del Core en dos ACL (RR-01) |
| Permiso de envío y envío en el mismo proceso | El servicio de transferencias concentra una complejidad alta |

## Consecuencias

- DD-10 queda resuelta en cuanto a las ACL (módulos internos). El BFF se decide en ADR-0014.
- Cada servicio tiene su propio almacén lógico (ADR-0016). **Ningún almacén se comparte para escritura** entre contenedores.
- Si la organización resulta de 1–2 ingenieros (S11), se reevalúa la alternativa A.

## Riesgos

- RR-01 (duplicación de las ACL de clientes).
- RR-20 (complejidad operativa de la arquitectura híbrida).
