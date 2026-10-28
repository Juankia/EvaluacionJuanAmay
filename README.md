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
| Revisión ARG-01 | Architecture Review Gate de las Fases 0–2 | **NO-GO** (1 CRITICAL, 5 HIGH). Remediación completada; **pendiente de la revisión ARG-01-R2** ([informe](docs/reviews/ARG-01-informe-de-remediacion.md)) |
| Verificación ARG-01-R2 | Verificación independiente de la remediación | **CONDITIONAL GO**. CF-01 y CF-02 remediados en diseño; **pendientes de verificación independiente** ([remediación](docs/reviews/ARG-01-R2-remediacion-CF01-CF02.md)) |
| Fase 3 | C4 Nivel 2: Contenedores | **Completada; pendiente de la revisión ARG-02** ([preparación](docs/reviews/ARG-02-preparacion-revision-contenedores.md)). El orquestador de transferencias es **provisional**. **No se autoriza implementación productiva** |
| Revisión ARG-02 | Revisión independiente de contenedores | CONDITIONAL GO con alcance restringido; **NO-GO para el diseño de los componentes de ejecución y recuperación** de transferencias y del alta de clientes. Remediación en [registro](docs/reviews/ARG-02-registro-de-remediacion.md) |
| Fase 4 | C4 Nivel 3: Componentes | **Parcialmente completada**: BFF, consultas, vinculación (salvo el alta), notificaciones y auditoría. **Bloqueado (B-05):** servicio de transferencias y ejecutor del alta. Pendiente de ARG-03 ([lista de verificación](docs/reviews/ARG-03-lista-de-verificacion-fase-4.md)) |
| Revisión ARG-03 | Revisión de componentes y del diario de despacho | **CONDITIONAL GO para la base técnica no financiera**; NO-GO para la ejecución financiera y el alta en el Core ([informe](docs/reviews/ARG-03-revision-componentes.md)) |
| Base ejecutable | Java 21 y Quarkus: paquetes transversales, contratos, simulador del Core, auditoría, notificaciones y vinculación (subconjunto seguro, sin alta ni activación) | **Implementada para uso local**; transferencias y alta bloqueadas (B-05). No apta para producción ([alcance](docs/implementacion/01-alcance-de-la-base-ejecutable.md)) |
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
│   ├── 16-arquitectura-de-contenedores.md      C4 Nivel 2: contenedores (Fase 3)
│   ├── 17-matriz-de-responsabilidades.md       Responsabilidades por contenedor
│   ├── 18-propiedad-y-consistencia-de-datos.md Propiedad de datos y consistencia
│   ├── 19-comunicacion-entre-contenedores.md   Comunicación y capacidad del Core
│   ├── 20-alternativas-de-descomposicion.md    Monolito, híbrida y microservicios
│   ├── 21-alternativas-tecnologicas.md         Alternativas tecnológicas (incluye R6)
│   ├── 22-seguridad-entre-contenedores.md      Seguridad e identidad digital
│   ├── 23-riesgos-y-condiciones-fase-3.md      Gobierno, verificación acotada y bloqueos
│   ├── 24-arquitectura-de-componentes.md       C4 Nivel 3: componentes (Fase 4)
│   ├── 25-contratos-eventos-y-errores.md       Contratos, eventos, errores y timeouts
│   ├── 26-validacion-adversarial-fase-4.md     Escenarios adversariales y preguntas de diseño
│   ├── adr/                                    Registros de decisiones arquitectónicas
│   ├── implementacion/                         Alcance, construcción, servicios y límites de la base ejecutable
│   └── reviews/                                Revisiones de arquitectura (ARG) y remediaciones
├── c4/
│   └── workspace.dsl                           Modelo C4 en Structurizr DSL (fuente autoritativa)
├── diagrams/
│   └── c4/                                     Diagramas generados desde el DSL (no editar)
├── pom.xml                                     POM agregador del backend (Java 21, Quarkus)
├── packages/                                   Bibliotecas transversales y contratos OpenAPI
├── services/                                   Auditoría, notificaciones y vinculación digital
├── test-fixtures/                              Simulador del Core (solo pruebas, datos sintéticos)
└── infra/local/                                Entorno local (PostgreSQL por servicio y RabbitMQ)
```

## Registros de decisiones arquitectónicas (ADR)

| ADR | Título | Estado |
|---|---|---|
| [ADR-0001](docs/adr/ADR-0001-acl-por-dominio.md) | Capas anticorrupción por dominio para la integración con el Core | Aceptada — complementada por ADR-0011 |
| [ADR-0002](docs/adr/ADR-0002-frontera-api-gateway.md) | API Gateway como frontera de fachada y composición, no como orquestador de negocio | Aceptada |
| [ADR-0003](docs/adr/ADR-0003-integration-platform-sdk.md) | Integration Platform SDK: capacidad compartida, política local | Aceptada |
| [ADR-0004](docs/adr/ADR-0004-cache-aside.md) | Cache-Aside con estrategia de frescura por categoría de dato | Aceptada |
| [ADR-0005](docs/adr/ADR-0005-maquina-de-estados-transaccional.md) | Máquina de estados transaccional durable para operaciones financieras | Aceptada — versión 3 (CF-01 y CF-02 de ARG-01-R2) |
| [ADR-0006](docs/adr/ADR-0006-idempotencia-y-conciliacion.md) | Contrato de idempotencia, deduplicación y conciliación de operaciones financieras | Aceptada — versión 3 (CF-01 de ARG-01-R2) |
| [ADR-0007](docs/adr/ADR-0007-cqrs-diferido.md) | CQRS diferido hasta que se cumplan disparadores medibles | Diferida |
| [ADR-0008](docs/adr/ADR-0008-invalidacion-por-eventos.md) | Invalidación de caché dirigida por eventos; la tecnología de plataforma se difiere | Aceptada (estrategia); tecnología diferida |
| [ADR-0009](docs/adr/ADR-0009-core-autoritativo.md) | El Core es autoritativo para la identidad del cliente; la vinculación digital requiere autorización explícita | Aceptada — enmendada en ARG-01 |
| [ADR-0010](docs/adr/ADR-0010-residencia-de-datos.md) | Región cloud y residencia de datos sujetas a validación legal y de cumplimiento | Propuesta. Pendiente de validación |
| [ADR-0011](docs/adr/ADR-0011-presupuesto-de-capacidad-del-core.md) | Presupuesto global de capacidad del Core y control de admisión por clase de operación | Aceptada (ARG-01) |
| [ADR-0012](docs/adr/ADR-0012-descomposicion-hibrida-de-contenedores.md) | Descomposición híbrida de contenedores y ubicación de las ACL | Aceptada con condiciones |
| [ADR-0013](docs/adr/ADR-0013-frontera-del-idp.md) | El IdP es un contenedor interno de la plataforma | Aceptada con condiciones (frontera); producto provisional |
| [ADR-0014](docs/adr/ADR-0014-api-gateway-y-bff-de-canales.md) | API Gateway de borde y un BFF compartido de canales | Aceptada con condiciones |
| [ADR-0015](docs/adr/ADR-0015-mensajeria-asincrona-con-outbox.md) | Mensajería asíncrona con Outbox transaccional y broker gestionado | Aceptada (patrón); producto diferido |
| [ADR-0016](docs/adr/ADR-0016-persistencia-y-durabilidad-del-estado.md) | Persistencia relacional por servicio y durabilidad del estado financiero | Aceptada con condiciones |
| [ADR-0017](docs/adr/ADR-0017-stack-de-backend.md) | Stack de backend: Java 21 y Quarkus | Provisional |
| [ADR-0018](docs/adr/ADR-0018-stack-de-frontend.md) | Stack de frontend: React + TS y React Native + TS | Aceptada con condiciones |
| [ADR-0019](docs/adr/ADR-0019-sesiones-del-bff.md) | Sesiones web gestionadas por el BFF con un almacén de sesiones dedicado | Aceptada con condiciones |
| [ADR-0020](docs/adr/ADR-0020-politica-de-auditoria-por-categoria.md) | Política de auditoría por categoría de evento | Aceptada con condiciones |
| [ADR-0021](docs/adr/ADR-0021-diario-de-autorizacion-de-despacho.md) | Diario de autorización de despacho y recuperación regional segura | Aceptada con condiciones (pendiente de verificación independiente) |

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
