# ADR-0017 — Stack de backend: Java 21 y Quarkus (provisional)

| Campo | Valor |
|---|---|
| Estado | **Provisional.** Resuelve DD-19 de forma provisional, pendiente de confirmar las capacidades del equipo (Q-18) |
| Fecha | 2026-10-27 |
| Requisitos | C5, C6 |
| Drivers y supuestos | S11; AQ-03, AQ-07, AQ-08, AQ-10 |
| Relacionados | [00 — Estándares de ingeniería](../00-estandares-de-ingenieria.md), [21 — Alternativas tecnológicas](../21-alternativas-tecnologicas.md), [ADR-0012](ADR-0012-descomposicion-hibrida-de-contenedores.md) |

## Contexto

Con los contenedores definidos (ADR-0012) puede evaluarse el stack del backend, como exigía DD-19. Los estándares de ingeniería incluyen un perfil condicional para Java y Quarkus, que **no** anticipaba la decisión.

## Problema

¿Qué lenguaje y qué framework se usan en los contenedores de backend (BFF y servicios de dominio)?

## Decisión

- **Java 21 (LTS)** y **Quarkus** para el BFF y los servicios de dominio, de forma **provisional**.
- **Spring Boot** es la alternativa equivalente, aceptable si las capacidades del equipo lo indican (Q-18). Ambos cumplen los criterios. La diferencia es de **operación y de eficiencia de recursos**, no de seguridad bancaria.

| Criterio | Quarkus | Spring Boot |
|---|---|---|
| Idoneidad bancaria (ecosistema Java, transacciones, seguridad) | Alta | Alta |
| Arranque y memoria por réplica (escalado horizontal y costo) | **Mejor** (orientado a contenedores) | Bueno |
| Madurez y ecosistema | Alta | **Muy alta** |
| Observabilidad (OpenTelemetry, métricas) | Integrada | Integrada |
| Capacidades del equipo | Por confirmar (Q-18) | Por confirmar (Q-18) |
| Dependencia de un proveedor | Baja (código abierto, estándares Jakarta y MicroProfile) | Baja |

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| **Java 21 + Quarkus** | **Provisional** | Eficiencia por réplica, relevante para el escalado y para las cuotas por réplica (ADR-0011) |
| Java 21 + Spring Boot | Alternativa aceptada | Mayor base de talento; equivalente en garantías |
| Otros lenguajes (Go, .NET, Node.js) | No seleccionados | Sin un driver; menor alineación con los estándares redactados y con el ecosistema Java habitual en el sector |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Menor consumo por réplica | Base de talento algo menor que la de Spring |
| Estándares abiertos | Algunas extensiones menos maduras que sus equivalentes de Spring |

## Consecuencias

- El perfil Java/Quarkus de los estándares de ingeniería se activa de forma provisional.
- Las bibliotecas cliente HTTP o SOAP de las ACL deben permitir **desactivar los reintentos automáticos** (condición PT-06, [23](../23-riesgos-y-condiciones-fase-3.md)).

## Riesgos

- Si Q-18 indica otro stack, este ADR se reemplaza sin afectar la arquitectura de contenedores.
