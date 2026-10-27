# ADR-0009 — El Core Bancario es autoritativo para la identidad del cliente

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (Fase 1) |
| Fecha | 2026-10-27 |
| Requisitos | R3; contexto del reto («La información del cliente proviene de dos fuentes […]») |
| Drivers y supuestos | S1, S2, S5, S12; AQ-02, AQ-04 |
| Relacionados | [ADR-0001](ADR-0001-acl-por-dominio.md), [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [07 — Máquina de estados, sección 4](../07-maquina-de-estados-transaccional.md#4-máquina-de-estados-de-onboarding-conceptual) |

## Contexto

El reto indica que la información del cliente proviene del Core Bancario (datos básicos, productos, cuentas, movimientos) y de un sistema complementario de datos extendidos, ambos conectados a una base de datos principal. El onboarding digital (R3) crea clientes nuevos y tiene pasos que pueden completarse antes de que el Core confirme la creación: KYC y biometría.

## Problema

¿Qué sistema es la fuente autoritativa de la identidad del cliente? ¿Cómo se evita que la plataforma o el sistema complementario se conviertan, de hecho, en una segunda fuente de verdad, especialmente cuando el onboarding queda incompleto?

## Decisión

1. **El Core Bancario es autoritativo** para la identidad del cliente y para sus datos transaccionales (productos, cuentas, saldos, movimientos).
2. **El sistema complementario es autoritativo solo para los datos extendidos** que la institución le asigne explícitamente. Ese inventario debe validarse (S5).
3. Una persona **no es cliente** de la plataforma hasta que el Core confirma su creación (`CORE_CREATED`).
4. El onboarding usa un **estado durable** ([ADR-0005](ADR-0005-maquina-de-estados-transaccional.md)). La creación en el Core es **idempotente** ([ADR-0006](ADR-0006-idempotencia-y-conciliacion.md)).
5. El sistema complementario **puede almacenar la información pendiente** del onboarding, pero **no debe convertirse silenciosamente en autoritativo** para la identidad del cliente.
6. Se exige una **conciliación** periódica entre las solicitudes pendientes, los datos del sistema complementario y los clientes del Core.
7. La plataforma accede al Core y al sistema complementario solo mediante sus interfaces y las ACL, nunca mediante la base de datos principal (S12).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Crear primero el cliente en la plataforma o en el sistema complementario y sincronizarlo después con el Core | Descartada | Crea dos fuentes de verdad, riesgo de clientes huérfanos o duplicados e inconsistencias de identidad |
| Repositorio maestro de clientes (*Master Data Management*) | Fuera de alcance | Es una iniciativa institucional, no del canal digital. Podría reevaluarse si la institución la tiene |
| **Core autoritativo + estado durable de onboarding + conciliación** | **Seleccionada** | Una sola fuente de verdad de la identidad |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Una sola fuente de verdad de la identidad | El onboarding depende de la disponibilidad del Core para completarse |
| Sin clientes duplicados creados por la plataforma | Estados intermedios visibles para el solicitante («en proceso») |
| Trazabilidad para auditoría | Procesos de conciliación y revisión manual |

## Consecuencias

- La experiencia del onboarding debe comunicar los estados intermedios (`PENDING_CORE`, `MANUAL_REVIEW`).
- Se requiere poder buscar en el Core por identificador de persona para la conciliación y la idempotencia. Esto debe validarse.
- Los datos biométricos y de KYC tienen un tratamiento propio, que se define en la fase de seguridad y en [ADR-0010](ADR-0010-residencia-de-datos.md).

## Riesgos

- Si el Core no permite buscar por identificador de persona, aumenta el riesgo de duplicados en los reintentos (relacionado con RR-04).
- Registros huérfanos en el sistema complementario si la conciliación no opera.
