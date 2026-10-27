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
| Fase 2 | C4 Nivel 1: Contexto del sistema, catálogo de integraciones y límites de confianza | Cerrada (pendiente de aprobación del comité) |
| Fase 3 | C4 Nivel 2: Contenedores | Siguiente fase. No iniciada |
| Fases posteriores | C4 de Componentes, seguridad e identidad, infraestructura cloud, DR, justificaciones de frontend y notificaciones | No iniciadas |

> Este repositorio no anticipa decisiones de fases posteriores. Lo que todavía no se ha decidido figura como **decisión diferida**, con su disparador explícito.

## Resumen ejecutivo de las Fases 0–1

1. **El Core Bancario es un sistema externo y autoritativo** para los datos transaccionales de clientes y cuentas. Se asume heredado y con capacidad limitada; la plataforma digital lo protege del tráfico de los canales ([ADR-0009](docs/adr/ADR-0009-core-autoritativo.md)).
2. **Integración mediante capas anticorrupción por dominio** (*Anti-Corruption Layer*, ACL). Clientes, movimientos y transferencias tienen cada uno su propia ACL. No existe una capa de integración centralizada por defecto ([ADR-0001](docs/adr/ADR-0001-acl-por-dominio.md)).
3. **Integration Platform SDK:** capacidad técnica compartida en forma de biblioteca, no de servicio en tiempo de ejecución. Su principio es **capacidad compartida, política local** ([ADR-0003](docs/adr/ADR-0003-integration-platform-sdk.md)).
4. **El API Gateway es una frontera de tráfico y de fachada**, no un orquestador de negocio ([ADR-0002](docs/adr/ADR-0002-frontera-api-gateway.md)).
5. **Un timeout no equivale a una falla.** Las operaciones financieras sin resultado confirmado pasan a estado `UNKNOWN` y se concilian; nunca se reintentan a ciegas ([ADR-0005](docs/adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](docs/adr/ADR-0006-idempotencia-y-conciliacion.md)).
6. **Cache-Aside con estrategia de frescura por categoría de dato.** No hay TTL arbitrarios ([ADR-0004](docs/adr/ADR-0004-cache-aside.md)).
7. **Se difieren con disparadores medibles:** CQRS ([ADR-0007](docs/adr/ADR-0007-cqrs-diferido.md)), la tecnología del bus de eventos ([ADR-0008](docs/adr/ADR-0008-invalidacion-por-eventos.md)) y la región cloud y la residencia de datos ([ADR-0010](docs/adr/ADR-0010-residencia-de-datos.md)).

## Resumen de la Fase 2 — Contexto del sistema

- **Un único sistema en diseño:** la Plataforma de Banca Digital.
- **Dos actores confirmados:** Cliente bancario y Solicitante de vinculación.
- **Cinco sistemas externos en la vista principal:** Core Bancario, Sistema Complementario de Clientes, Sistema Externo de Transferencias, Proveedor KYC/Biométrico y Proveedores de Notificaciones.
- **Vista ampliada con candidatos por validar:** IdP (frontera por decidir), Fuente Oficial de Identidad y actores internos de soporte y auditoría.
- **Sin integración directa con el BCE/SPI:** la topología de las transferencias interbancarias es la pregunta abierta Q-01.
- **Límites de confianza en una vista conceptual separada,** distinta de la frontera de software C4.

Documentos:

- [12 — Contexto del sistema](docs/12-contexto-del-sistema.md)
- [13 — Catálogo de integraciones](docs/13-catalogo-de-integraciones.md)
- [14 — Límites de confianza](docs/14-limites-de-confianza.md)
- [15 — Preguntas abiertas](docs/15-preguntas-abiertas-fase-2.md)

## Estructura del repositorio

```text
.
├── README.md
├── docs/
│   ├── 00-politica-de-documentacion.md         Convenciones de idioma y redacción
│   ├── 00-estandares-de-ingenieria.md          Estándares de código, glosario de dominio, pruebas y CI
│   ├── 01-drivers-arquitectonicos.md           Drivers funcionales y de calidad
│   ├── 02-supuestos.md                         Supuestos arquitectónicos (S1–S17)
│   ├── 03-atributos-de-calidad.md              Priorización de atributos de calidad
│   ├── 04-escenarios-de-calidad.md             Escenarios de calidad medibles
│   ├── 05-frontera-de-integracion-core.md      Frontera de integración con el Core (Fase 1)
│   ├── 06-estrategia-de-cache.md               Estrategia de caché y matriz de cacheabilidad
│   ├── 07-maquina-de-estados-transaccional.md  Máquinas de estado de transferencias y onboarding
│   ├── 08-idempotencia-y-conciliacion.md       Idempotencia y conciliación
│   ├── 09-trazabilidad-de-requisitos.md        Matriz de trazabilidad de requisitos
│   ├── 10-decisiones-diferidas.md              Registro de decisiones diferidas
│   ├── 11-riesgos-residuales.md                Registro de riesgos residuales
│   ├── 12-contexto-del-sistema.md              C4 Nivel 1: contexto del sistema (Fase 2)
│   ├── 13-catalogo-de-integraciones.md         Catálogo de integraciones
│   ├── 14-limites-de-confianza.md              Límites de confianza (vista conceptual)
│   ├── 15-preguntas-abiertas-fase-2.md         Preguntas abiertas priorizadas
│   └── adr/                                    Registros de decisiones arquitectónicas
├── c4/
│   └── workspace.dsl                           Modelo C4 en Structurizr DSL (fuente autoritativa)
└── diagrams/
    └── c4/                                     Diagramas generados desde el DSL (no editar)
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

Toda la documentación sigue la [política de documentación e idioma](docs/00-politica-de-documentacion.md). Las futuras implementaciones seguirán los [estándares de ingeniería](docs/00-estandares-de-ingenieria.md). Estos estándares no inician la implementación ni seleccionan el stack, que sigue diferido (DD-19).

Puntos principales de la política:

- Redacción en español técnico profesional (es-EC).
- Identificadores de código, contratos y eventos en inglés.
- Mensajes de commit en español con la estructura de Conventional Commits.

## Herramientas

- **Modelado C4:** [Structurizr DSL](https://docs.structurizr.com/dsl). Para validar el modelo:

  ```bash
  docker run --rm -v "$PWD/c4:/usr/local/structurizr:ro" structurizr/structurizr validate -workspace /usr/local/structurizr/workspace.dsl
  ```

- **Regeneración de diagramas** (PlantUML y SVG en `diagrams/c4/`):

  ```bash
  docker run --rm -v "$PWD/c4:/usr/local/structurizr:ro" -v "$PWD/diagrams/c4:/out" \
    structurizr/structurizr export -w /usr/local/structurizr/workspace.dsl -f plantuml/structurizr -o /out
  docker run --rm -v "$PWD/diagrams/c4:/data" plantuml/plantuml -tsvg "/data/*.puml"
  ```

  Se usa el formato `plantuml/structurizr` porque respeta los estilos del DSL; el formato C4-PlantUML los ignora.
