# Arquitectura de la Plataforma de Banca Digital

Repositorio de arquitectura como código (*Architecture as Code*) de la solución propuesta para el reto técnico **«Reto Técnico - Arquitecto de Soluciones/Aplicaciones — Plataforma de Banca Digital»**.

El objetivo no es implementar la aplicación bancaria. El objetivo es construir una arquitectura que pueda defenderse ante un comité de arquitectura, con decisiones trazables desde el requisito hasta la consecuencia:

```text
Requisito → Driver / Supuesto → Decisión → Trade-off → Consecuencia
```

## Estado actual

| Fase | Contenido | Estado |
|---|---|---|
| Fase 0 | Drivers arquitectónicos, supuestos y escenarios de calidad | Cerrada (pendiente de revisión del comité) |
| Fase 1 | Decisión sobre la frontera de integración con el Core Bancario | Cerrada (pendiente de revisión del comité) |
| Fase 2 | Diagrama C4 de Contexto | Siguiente fase. No iniciada |
| Fases posteriores | C4 de Contenedores y Componentes, seguridad e identidad, infraestructura cloud, DR, justificaciones de frontend y notificaciones | No iniciadas |

> Este repositorio no anticipa decisiones de fases posteriores. Lo que todavía no se ha decidido figura como **decisión diferida**, con su disparador explícito.

## Resumen ejecutivo de las Fases 0–1

1. **El Core Bancario es un sistema externo y autoritativo** para los datos transaccionales de clientes y cuentas. Se asume heredado y con capacidad limitada; la plataforma digital lo protege del tráfico de los canales ([ADR-0009](docs/adr/ADR-0009-core-autoritativo.md)).
2. **Integración mediante capas anticorrupción por dominio** (*Anti-Corruption Layer*, ACL). Clientes, movimientos y transferencias tienen cada uno su propia ACL. No existe una capa de integración centralizada por defecto ([ADR-0001](docs/adr/ADR-0001-acl-por-dominio.md)).
3. **Integration Platform SDK:** capacidad técnica compartida en forma de biblioteca, no de servicio en tiempo de ejecución. Su principio es **capacidad compartida, política local** ([ADR-0003](docs/adr/ADR-0003-integration-platform-sdk.md)).
4. **El API Gateway es una frontera de tráfico y de fachada**, no un orquestador de negocio ([ADR-0002](docs/adr/ADR-0002-frontera-api-gateway.md)).
5. **Un timeout no equivale a una falla.** Las operaciones financieras sin resultado confirmado pasan a estado `UNKNOWN` y se concilian; nunca se reintentan a ciegas ([ADR-0005](docs/adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](docs/adr/ADR-0006-idempotencia-y-conciliacion.md)).
6. **Cache-Aside con estrategia de frescura por categoría de dato.** No hay TTL arbitrarios ([ADR-0004](docs/adr/ADR-0004-cache-aside.md)).
7. **Se difieren con disparadores medibles:** CQRS ([ADR-0007](docs/adr/ADR-0007-cqrs-diferido.md)), la tecnología del bus de eventos ([ADR-0008](docs/adr/ADR-0008-invalidacion-por-eventos.md)) y la región cloud y la residencia de datos ([ADR-0010](docs/adr/ADR-0010-residencia-de-datos.md)).

## Estructura del repositorio

```text
.
├── README.md
├── docs/
│   ├── 00-politica-de-documentacion.md         Convenciones de idioma y redacción
│   ├── 01-drivers-arquitectonicos.md           Drivers funcionales y de calidad
│   ├── 02-supuestos.md                         Supuestos arquitectónicos (S1–S15)
│   ├── 03-atributos-de-calidad.md              Priorización de atributos de calidad
│   ├── 04-escenarios-de-calidad.md             Escenarios de calidad medibles
│   ├── 05-frontera-de-integracion-core.md      Frontera de integración con el Core (Fase 1)
│   ├── 06-estrategia-de-cache.md               Estrategia de caché y matriz de cacheabilidad
│   ├── 07-maquina-de-estados-transaccional.md  Máquinas de estado de transferencias y onboarding
│   ├── 08-idempotencia-y-conciliacion.md       Idempotencia y conciliación
│   ├── 09-trazabilidad-de-requisitos.md        Matriz de trazabilidad de requisitos
│   ├── 10-decisiones-diferidas.md              Registro de decisiones diferidas
│   ├── 11-riesgos-residuales.md                Registro de riesgos residuales
│   └── adr/                                    Registros de decisiones arquitectónicas
├── c4/
│   └── workspace.dsl                           Modelo C4 en Structurizr DSL (base de la Fase 2)
└── diagrams/                                   Diagramas exportados (fases posteriores)
```

## Registros de decisiones arquitectónicas (ADR)

| ADR | Título | Estado |
|---|---|---|
| [ADR-0001](docs/adr/ADR-0001-acl-por-dominio.md) | Capas anticorrupción por dominio para la integración con el Core | Aceptada |
| [ADR-0002](docs/adr/ADR-0002-frontera-api-gateway.md) | API Gateway como frontera de fachada y composición, no como orquestador de negocio | Aceptada |
| [ADR-0003](docs/adr/ADR-0003-integration-platform-sdk.md) | Integration Platform SDK: capacidad compartida, política local | Aceptada |
| [ADR-0004](docs/adr/ADR-0004-cache-aside.md) | Cache-Aside con estrategia de frescura por categoría de dato | Aceptada |
| [ADR-0005](docs/adr/ADR-0005-maquina-de-estados-transaccional.md) | Máquina de estados transaccional durable para operaciones financieras | Aceptada (modelo conceptual) |
| [ADR-0006](docs/adr/ADR-0006-idempotencia-y-conciliacion.md) | Idempotencia generada en el dominio y conciliación | Aceptada |
| [ADR-0007](docs/adr/ADR-0007-cqrs-diferido.md) | CQRS diferido hasta que se cumplan disparadores medibles | Diferida |
| [ADR-0008](docs/adr/ADR-0008-invalidacion-por-eventos.md) | Invalidación de caché dirigida por eventos; la tecnología de plataforma se difiere | Aceptada (estrategia); tecnología diferida |
| [ADR-0009](docs/adr/ADR-0009-core-autoritativo.md) | El Core Bancario es autoritativo para la identidad del cliente | Aceptada |
| [ADR-0010](docs/adr/ADR-0010-residencia-de-datos.md) | Región cloud y residencia de datos sujetas a validación legal y de cumplimiento | Propuesta. Pendiente de validación |

## Fuente del reto

El enunciado original es el documento `Reto Técnico Arquitecto Jardín Azuayo.pdf`, que no se versiona en este repositorio. Los requisitos citados en la [matriz de trazabilidad](docs/09-trazabilidad-de-requisitos.md) reproducen literalmente su texto.

## Convenciones

Toda la documentación sigue la [política de documentación e idioma](docs/00-politica-de-documentacion.md):

- Redacción en español técnico profesional (es-EC).
- Identificadores de código, contratos y eventos en inglés.
- Mensajes de commit en inglés con Conventional Commits.

## Herramientas

- **Modelado C4:** [Structurizr DSL](https://docs.structurizr.com/dsl). Para validar el modelo:

  ```bash
  docker run --rm -v "$PWD/c4:/usr/local/structurizr:ro" structurizr/structurizr validate -workspace /usr/local/structurizr/workspace.dsl
  ```
