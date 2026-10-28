# Arquitectura de la Plataforma de Banca Digital

Repositorio de arquitectura como código (*Architecture as Code*) de la propuesta para el reto técnico **«Reto Técnico - Arquitecto de Soluciones/Aplicaciones — Plataforma de Banca Digital»**.

> **Estado de la entrega:** propuesta arquitectónica completa **con limitaciones declaradas**. Incluye una base ejecutable local para el alcance **no financiero**. **No es apta para producción.** Las transferencias y el alta de clientes en el Core están **bloqueadas** de forma deliberada (B-05).

**Empiece aquí:** [Resumen ejecutivo](docs/entrega/resumen-ejecutivo.md) · [Matriz de cumplimiento](docs/entrega/matriz-cumplimiento.md) · [Guía de defensa](docs/entrega/guia-defensa-arquitectura.md) · [Limitaciones y supuestos](docs/entrega/limitaciones-y-supuestos.md) · [Validación final](docs/entrega/validacion-final.md)

## 1. Reto y objetivos

El reto pide diseñar una plataforma de banca digital (web y app móvil) para una institución financiera que permita consultar datos del cliente y movimientos, realizar transferencias, vincular clientes nuevos con reconocimiento facial y notificar cada movimiento. Además exige atender la normativa, la alta disponibilidad, la recuperación ante desastres, la seguridad, la escalabilidad y una infraestructura en AWS o Azure (requisitos R1–R6 y C1–C6).

El objetivo de este repositorio es una arquitectura **defendible ante un comité**, con decisiones trazables:

```text
Requisito → Driver / Supuesto → Decisión (ADR) → Trade-off → Consecuencia → Evidencia
```

## 2. Visión general

| Tema | Decisión | Evidencia |
|---|---|---|
| Integración con el Core | El Core es autoritativo, externo y de capacidad limitada. ACL por dominio, sin acceso a su base de datos y con un presupuesto global de capacidad | [ADR-0001](docs/adr/ADR-0001-acl-por-dominio.md), [ADR-0009](docs/adr/ADR-0009-core-autoritativo.md), [ADR-0011](docs/adr/ADR-0011-presupuesto-de-capacidad-del-core.md), [05](docs/05-frontera-de-integracion-core.md) |
| Capa de entrada | API Gateway de borde sin lógica de negocio y BFF compartido para web y móvil | [ADR-0002](docs/adr/ADR-0002-frontera-api-gateway.md), [ADR-0014](docs/adr/ADR-0014-api-gateway-y-bff-de-canales.md) |
| Descomposición | Híbrida: cinco servicios de dominio con base de datos propia | [ADR-0012](docs/adr/ADR-0012-descomposicion-hibrida-de-contenedores.md), [16](docs/16-arquitectura-de-contenedores.md) |
| Seguridad transaccional | Máquina de estados durable, idempotencia, conciliación. **Un timeout no es una falla** | [ADR-0005](docs/adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](docs/adr/ADR-0006-idempotencia-y-conciliacion.md), [07](docs/07-maquina-de-estados-transaccional.md), [08](docs/08-idempotencia-y-conciliacion.md) |
| Eventos y notificaciones | Outbox transaccional + broker; push con respaldo SMS; deduplicación y cola de mensajes muertos | [ADR-0015](docs/adr/ADR-0015-mensajeria-asincrona-con-outbox.md), [25](docs/25-contratos-eventos-y-errores.md) |
| Auditoría y caché | Auditoría de solo anexión por categoría con cadena de hash; Cache-Aside con frescura por categoría | [ADR-0020](docs/adr/ADR-0020-politica-de-auditoria-por-categoria.md), [ADR-0004](docs/adr/ADR-0004-cache-aside.md), [06](docs/06-estrategia-de-cache.md) |
| Seguridad e identidad | IdP OIDC interno; Authorization Code + PKCE; sesión del servidor en el BFF; MFA y autenticación reforzada; KYC con prueba de vida | [ADR-0013](docs/adr/ADR-0013-frontera-del-idp.md), [ADR-0019](docs/adr/ADR-0019-sesiones-del-bff.md), [ADR-0023](docs/adr/ADR-0023-autenticacion-y-sesiones.md), [29](docs/29-seguridad-identidad-y-cumplimiento.md) |
| Nube, HA y DR | AWS como **referencia** (87/100 frente a 84/100 de Azure); multizona; DR en espera templada con cuarentena financiera | [ADR-0022](docs/adr/ADR-0022-infraestructura-cloud-de-referencia.md), [27](docs/27-infraestructura-cloud.md), [28](docs/28-alta-disponibilidad-y-recuperacion.md) |
| Tecnología | Backend Java 21 + Quarkus (provisional); frontend React + TS y React Native + TS, con Flutter como alternativa | [ADR-0017](docs/adr/ADR-0017-stack-de-backend.md), [ADR-0018](docs/adr/ADR-0018-stack-de-frontend.md), [21](docs/21-alternativas-tecnologicas.md) |

## 3. Diagramas

| Nivel | Diagrama |
|---|---|
| C4 Nivel 1 — Contexto | [Principal](diagrams/c4/structurizr-ContextoPrincipal.svg) · [Ampliado](diagrams/c4/structurizr-ContextoAmpliado.svg) |
| C4 Nivel 2 — Contenedores (21) | [Principal](diagrams/c4/structurizr-Contenedores.svg) · [Consultas](diagrams/c4/structurizr-Contenedores-Consultas.svg) · [Vinculación](diagrams/c4/structurizr-Contenedores-Vinculacion.svg) · [Transferencias](diagrams/c4/structurizr-Contenedores-Transferencias.svg) · [Notificaciones y auditoría](diagrams/c4/structurizr-Contenedores-NotificacionesAuditoria.svg) · [Integración con el Core](diagrams/c4/structurizr-Contenedores-IntegracionCore.svg) |
| C4 Nivel 3 — Componentes (52) | [BFF](diagrams/c4/structurizr-Componentes-BFF.svg) · [Consultas](diagrams/c4/structurizr-Componentes-Consultas.svg) · [Vinculación](diagrams/c4/structurizr-Componentes-Vinculacion.svg) · [Notificaciones](diagrams/c4/structurizr-Componentes-Notificaciones.svg) · [Auditoría](diagrams/c4/structurizr-Componentes-Auditoria.svg) |
| Infraestructura | [Vista ejecutiva](diagrams/infraestructura/infraestructura-referencia-resumen.svg) · [Vista detallada (DSL)](diagrams/c4/structurizr-Infraestructura-Referencia.svg) |
| Seguridad | [Arquitectura de seguridad e identidad](diagrams/seguridad/arquitectura-de-seguridad.svg) |
| Límites de confianza | [14 — Vista conceptual](docs/14-limites-de-confianza.md) |

El modelo C4 y la infraestructura detallada se generan desde [`c4/workspace.dsl`](c4/workspace.dsl), que es la fuente autoritativa. Las vistas ejecutiva y de seguridad son PlantUML (`diagrams/infraestructura`, `diagrams/seguridad`).

## 4. Prueba de concepto implementada

Base técnica autorizada por [ARG-03](docs/reviews/ARG-03-revision-componentes.md) **solo para el alcance no financiero**. Detalle en [docs/implementacion](docs/implementacion/01-alcance-de-la-base-ejecutable.md).

| Módulo | Estado |
|---|---|
| `packages/shared-observability`, `integration-platform-sdk`, `contract-testing` | Implementados y probados |
| `packages/api-contracts`: OpenAPI y sobre de eventos | Publicados; el contrato de transferencias está marcado `BLOCKED` |
| `test-fixtures/core-simulator`: datos sintéticos, fallas inyectables | Implementado y probado |
| `services/audit`: cadena de hash, consulta con registro A6 | Implementado y probado |
| `services/notifications`: proveedores **falsos**, push → SMS, DLQ | Implementado y probado |
| `services/digital-linking`: inicio, verificación de identidad (KYC **simulado**), estado; E3/E5 deshabilitados | Implementado y probado |
| Gateway, BFF, IdP, consultas, caché, consola, frontend | **No implementados** (solo diseño y contratos) |
| Transferencias y alta en el Core | **Bloqueados (B-05)**: solo contrato de frontera |

### Cómo ejecutar y validar

Requisitos: JDK 21, Maven 3.9+ y Docker. Detalle en [implementación 02](docs/implementacion/02-entorno-local-y-construccion.md).

```bash
mvn -B verify                                   # compila y ejecuta todas las pruebas (usa Testcontainers)
mvn -B -pl services/digital-linking -am verify  # un servicio y sus dependencias
```

Resultados de la última ejecución: [validación final](docs/entrega/validacion-final.md).

## 5. Disponibilidad, DR y limitaciones

- **HA:** tres zonas de disponibilidad, PostgreSQL con réplica síncrona y degradación a caché ante fallas del Core ([28](docs/28-alta-disponibilidad-y-recuperacion.md)).
- **DR:** región secundaria en espera templada con conmutación **manual** y **cuarentena financiera**. Los RTO/RPO son **candidatos** y no se han medido.
- **Limitaciones principales:**
  - sin despliegue en la nube;
  - sin latencia ni costos medidos;
  - preguntas institucionales sin responder (Q-01, Q-05, Q-20, entre otras);
  - ADR-0021 no aprobado;
  - revisiones no independientes de la autoría ([limitaciones](docs/entrega/limitaciones-y-supuestos.md)).

## 6. Historial de fases y revisiones

| Fase o revisión | Estado |
|---|---|
| Fases 0–1: drivers, supuestos, frontera con el Core | Cerradas (pendientes de revisión del comité) |
| Fase 2: C4 Nivel 1 | Cerrada. [ARG-01](docs/reviews/ARG-01-revision-arquitectonica.md) NO-GO → remediada → [ARG-01-R2](docs/reviews/ARG-01-R2-verificacion-independiente.md) CONDITIONAL GO |
| Fase 3: C4 Nivel 2 | Completada. [ARG-02](docs/reviews/ARG-02-revision-contenedores.md) CONDITIONAL GO con alcance restringido |
| Fase 4: C4 Nivel 3 | Parcial (5 de 7 contenedores con lógica). [ARG-03](docs/reviews/ARG-03-revision-componentes.md) CONDITIONAL GO para lo no financiero; NO-GO para la ejecución financiera |
| Base ejecutable | Implementada localmente (alcance no financiero) |
| Infraestructura, DR, seguridad e identidad | Diseñadas para la entrega (docs 27–29, ADR-0022 y ADR-0023). **Sin nueva revisión ARG** |

## 7. Navegación de la documentación

| Bloque | Documentos |
|---|---|
| Normativos | [Política de documentación](docs/00-politica-de-documentacion.md), [Estándares de ingeniería](docs/00-estandares-de-ingenieria.md) |
| Fundamentos (Fases 0–1) | [01 Drivers](docs/01-drivers-arquitectonicos.md), [02 Supuestos](docs/02-supuestos.md), [03 Atributos](docs/03-atributos-de-calidad.md), [04 Escenarios](docs/04-escenarios-de-calidad.md), [05 Frontera con el Core](docs/05-frontera-de-integracion-core.md), [06 Caché](docs/06-estrategia-de-cache.md), [07 Máquina de estados](docs/07-maquina-de-estados-transaccional.md), [08 Idempotencia](docs/08-idempotencia-y-conciliacion.md) |
| Gobierno | [09 Trazabilidad](docs/09-trazabilidad-de-requisitos.md), [10 Decisiones diferidas](docs/10-decisiones-diferidas.md), [11 Riesgos residuales](docs/11-riesgos-residuales.md), [23 Riesgos y condiciones](docs/23-riesgos-y-condiciones-fase-3.md), [revisiones](docs/reviews/) |
| Contexto (Fase 2) | [12 Contexto](docs/12-contexto-del-sistema.md), [13 Integraciones](docs/13-catalogo-de-integraciones.md), [14 Límites de confianza](docs/14-limites-de-confianza.md), [15 Preguntas abiertas](docs/15-preguntas-abiertas-fase-2.md) |
| Contenedores (Fase 3) | [16 Contenedores](docs/16-arquitectura-de-contenedores.md), [17 Responsabilidades](docs/17-matriz-de-responsabilidades.md), [18 Datos](docs/18-propiedad-y-consistencia-de-datos.md), [19 Comunicación](docs/19-comunicacion-entre-contenedores.md), [20 Descomposición](docs/20-alternativas-de-descomposicion.md), [21 Tecnología](docs/21-alternativas-tecnologicas.md), [22 Seguridad entre contenedores](docs/22-seguridad-entre-contenedores.md) |
| Componentes (Fase 4) | [24 Componentes](docs/24-arquitectura-de-componentes.md), [25 Contratos y eventos](docs/25-contratos-eventos-y-errores.md), [26 Validación adversarial](docs/26-validacion-adversarial-fase-4.md) |
| Infraestructura y seguridad | [27 Infraestructura cloud](docs/27-infraestructura-cloud.md), [28 HA y DR](docs/28-alta-disponibilidad-y-recuperacion.md), [29 Seguridad, identidad y cumplimiento](docs/29-seguridad-identidad-y-cumplimiento.md) |
| Implementación | [docs/implementacion](docs/implementacion/01-alcance-de-la-base-ejecutable.md) |
| Entrega | [docs/entrega](docs/entrega/resumen-ejecutivo.md) |

## 8. Registros de decisiones arquitectónicas (ADR)

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
| [ADR-0021](docs/adr/ADR-0021-diario-de-autorizacion-de-despacho.md) | Diario de autorización de despacho y recuperación regional segura | **No aprobada en ARG-03** (ARG03-C01, H01, H02); pendiente de enmienda y verificación independiente |
| [ADR-0022](docs/adr/ADR-0022-infraestructura-cloud-de-referencia.md) | Infraestructura cloud de referencia en AWS, multizona y con DR en espera templada | Propuesta (referencia del reto) |
| [ADR-0023](docs/adr/ADR-0023-autenticacion-y-sesiones.md) | Autenticación OIDC con Authorization Code + PKCE, BFF para la web y autenticación reforzada | Propuesta (fase de seguridad) |

## 9. Fuente del reto y convenciones

El enunciado original es el documento `Reto Técnico Arquitecto Jardín Azuayo.pdf`, que no se versiona en este repositorio. Los requisitos citados en la [matriz de trazabilidad](docs/09-trazabilidad-de-requisitos.md) reproducen literalmente su texto.

Toda la documentación sigue la [política de documentación e idioma](docs/00-politica-de-documentacion.md) y el código sigue los [estándares de ingeniería](docs/00-estandares-de-ingenieria.md). El stack del backend es **provisional** (ADR-0017, DD-19).

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

- **Vistas ejecutiva y de seguridad:**

  ```bash
  docker run --rm -v "$PWD/diagrams/infraestructura:/data" plantuml/plantuml -tsvg "/data/*.puml"
  docker run --rm -v "$PWD/diagrams/seguridad:/data" plantuml/plantuml -tsvg "/data/*.puml"
  ```
