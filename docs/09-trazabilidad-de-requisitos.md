# 09 — Matriz de trazabilidad de requisitos

| Campo | Valor |
|---|---|
| Fase | 0–2 (actualizado en la remediación ARG-01) |
| Estado | Vigente. Se actualiza al cierre de cada fase |
| Fuente | `Reto Técnico Arquitecto Jardín Azuayo.pdf`. El texto de los requisitos se reproduce literalmente |

## 1. Convenciones

- **R1–R6:** «Requerimientos del Sistema» del reto.
- **C1–C6:** «Consideraciones Técnicas» del reto.
- **E1–E4:** «Entregables» del reto.
- **Estado:**
  - **Cubierto (Fase 0–1):** la decisión de esta fase responde al requisito, total o parcialmente.
  - **Parcial:** la Fase 0–1 aporta decisiones de base y el resto corresponde a fases posteriores.
  - **Pendiente:** se aborda en una fase posterior identificada.

## 2. Requerimientos del sistema

| Requisito (texto del reto) | Fuente | Driver arquitectónico | Decisión | Componente o frontera | ADR | Estado | Preguntas abiertas |
|---|---|---|---|---|---|---|---|
| **R1. Capa de integración:** «Diseñar un API Gateway que orqueste al menos 3 servicios: consulta de datos del cliente, consulta de movimientos y transferencias (que invoca servicios externos según el tipo). Puede proponer servicios adicionales si lo justifica.» | Reto | DF-01, DF-03, DF-04, DF-05; AQ-03, AQ-05, AQ-08; S1, S4, S6 | El Gateway es una frontera de tráfico y de fachada. La orquestación de negocio está en los servicios de dominio. Hay una ACL por dominio hacia el Core | API Gateway; servicios de clientes, movimientos y transferencias; ACL por dominio; Integration Platform SDK | [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0002](adr/ADR-0002-frontera-api-gateway.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) | Parcial. El producto de Gateway y los contenedores se deciden en la Fase 3 | ¿Qué catálogo de servicios expone el Core? (S3) ¿Cómo se integran hoy las transferencias interbancarias? (S13) ¿Se justifica un BFF por canal? |
| **R2. Autenticación y autorización:** «Los usuarios se autentican mediante OAuth 2.0. Recomiende el flujo más adecuado e incluya herramientas de KYC y biometría dentro de la arquitectura.» | Reto | DF-07, DF-08, DF-09, DF-10; AQ-01; S9 | Se identifica el escenario de token robado (QA4). El flujo, el IdP y los proveedores se difieren | Servidor de autorización (por definir), API Gateway (validación de tokens) | — | Pendiente (fase de seguridad e identidad) | ¿Existe un IdP actual? (S9) ¿Se exige un perfil FAPI? ¿Qué mecanismo de vinculación de token se usará? (DD-04) |
| **R3. Onboarding digital:** «Los nuevos clientes se registran desde la app móvil con reconocimiento facial. Tras el onboarding, el usuario accede mediante usuario/contraseña, huella u otro método biométrico, especifíquelo y recomiende herramientas de industria.» | Reto | DF-06, DF-10; AQ-02, AQ-09 | Máquina de estados de onboarding durable. El Core es autoritativo para la identidad. La idempotencia es obligatoria en la creación en el Core | Servicio de onboarding (conceptual), ACL de clientes | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md) | Parcial. Los proveedores biométricos y los métodos de acceso se deciden en la fase de seguridad | ¿El Core permite buscar clientes por número de identificación? ¿Qué datos biométricos pueden almacenarse y dónde? |
| **R4. Notificaciones:** «Por normativa, los usuarios deben ser notificados de cada movimiento. Proponga al menos 2 canales de notificación (propios o externos).» | Reto | DF-11; AQ-03, AQ-05; S14 | La notificación definitiva solo se emite en estados terminales confirmados. Invalidación y notificación dirigidas por eventos como estrategia conceptual | Máquina de estados (origen del evento); mecanismo de notificación (por definir) | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0008](adr/ADR-0008-invalidacion-por-eventos.md) | Pendiente (fase de notificaciones) | ¿Qué norma específica aplica y a qué tipo de institución? (S14, S15) ¿«Cada movimiento» incluye los movimientos originados fuera del canal digital? |
| **R5. Auditoría y caché:** «Diseñar una base de datos de auditoría que registre todas las acciones del cliente, más un mecanismo de caché para clientes frecuentes basado en patrones de diseño, identifique los componentes involucrados.» | Reto | DF-12, DF-13; AQ-04, AQ-06; QA2, QA6 | **Caché:** Cache-Aside con matriz de cacheabilidad y frescura por categoría. **Auditoría:** se identifica como crítica (QA6); las transiciones de estado se auditan; el diseño de la base de datos se difiere | Servicios de dominio, almacén de caché (tecnología diferida), ACL; base de datos de auditoría (por diseñar) | [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0008](adr/ADR-0008-invalidacion-por-eventos.md) | Caché: cubierto a nivel de patrón. Auditoría: pendiente | ¿Qué retención exige la institución para la auditoría? ¿Se requiere evidencia de inmutabilidad? |
| **R6. Frontend:** «La plataforma requiere una SPA web y una app móvil en un framework multiplataforma. Proponga 2 opciones de stack frontend y justifique la selección.» | Reto | AQ-09; RS-05 | — | Canales web y móvil | — | Pendiente (fase de frontend) | ¿La institución tiene un stack o unas capacidades internas preferentes? |

## 3. Consideraciones técnicas

| Consideración (texto del reto) | Fuente | Driver arquitectónico | Decisión | Componente o frontera | ADR | Estado | Preguntas abiertas |
|---|---|---|---|---|---|---|---|
| **C1. Normativa:** «Mencione elementos regulatorios relevantes (protección de datos personales, ciberseguridad financiera, estándares PCI-DSS, etc.).» | Reto | AQ-01, AQ-04; S8, S14, S15 | La residencia de datos se somete a validación legal. No se afirma cumplimiento. No se atribuyen normas de un sector a otro | Transversal | [ADR-0010](adr/ADR-0010-residencia-de-datos.md) | Parcial | ¿Tipo de institución? (S15) ¿El alcance incluye datos de tarjetas (aplicabilidad de PCI-DSS)? |
| **C2. Alta disponibilidad y tolerancia a fallos:** «¿Cómo garantiza que el sistema se mantenga operativo ante fallos parciales?» | Reto | AQ-03, AQ-05; QA1, QA3, QA5 | Aislamiento por dominio. Resiliencia en la ACL (timeouts, Circuit Breaker, Bulkhead). Plazo propagado. Degradación de lecturas y falla segura de escrituras. Un timeout no equivale a una falla | ACL, servicios de dominio, máquina de estados | [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md), [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) | Parcial. La topología de infraestructura se define en una fase posterior | ¿SLA del Core? (S4) |
| **C3. Recuperación ante desastres:** «Estrategias de DR (RTO/RPO, respaldos, failover).» | Reto | AQ-05; S10 | Los RTO y RPO se tratan como objetivos por acordar. El estado durable de las operaciones es un requisito para la recuperación | Transversal | — | Pendiente (fase de infraestructura y DR) | ¿RTO y RPO según el BIA? (DD-09) |
| **C4. Seguridad y monitoreo:** «Protección de datos en tránsito/reposo y monitoreo continuo.» | Reto | AQ-01, AQ-04 | SDK con propagación de trazas (OpenTelemetry) y correlación. Datos sensibles excluidos de la caché. Seguridad de la caché como requisito | Integration Platform SDK, caché | [ADR-0003](adr/ADR-0003-integration-platform-sdk.md), [ADR-0004](adr/ADR-0004-cache-aside.md) | Parcial | ¿Existe un SOC o SIEM institucional que deba integrarse? |
| **C5. Escalabilidad y desacoplamiento:** «¿Cómo permite agregar nuevos componentes sin impactar los existentes?» | Reto | AQ-07, AQ-08; S7; QA7, QA8 | ACL por dominio. SDK sin lógica de negocio. CQRS diferido con disparadores. Invalidación por eventos como estrategia | ACL, SDK | [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md), [ADR-0007](adr/ADR-0007-cqrs-diferido.md), [ADR-0008](adr/ADR-0008-invalidacion-por-eventos.md) | Parcial | — |
| **C6. Infraestructura cloud:** «Puede usar AWS o Azure si lo considera necesario, garantice baja latencia. Cuenta con presupuesto.» | Reto | AQ-06, AQ-10; S8 | La región y la residencia se someten a validación legal. El proveedor cloud no se selecciona todavía | Infraestructura (por definir) | [ADR-0010](adr/ADR-0010-residencia-de-datos.md) | Pendiente (fase de infraestructura) | ¿Dónde están el Core y la base de datos principal (latencia hacia ellos)? ¿Hay restricciones de residencia? |

## 4. Entregables

| Entregable (texto del reto) | Estado | Ubicación prevista |
|---|---|---|
| **E1.** «Modelo C4 hasta nivel de componentes: Diagrama de Contexto (actores y sistemas externos), Diagrama de Contenedores (aplicaciones, bases de datos, servicios), Diagrama de Componentes (detalle interno de los contenedores principales)» | Contexto: **completado** (Fase 2, vistas `ContextoPrincipal` y `ContextoAmpliado`). Contenedores y componentes: pendientes | [`c4/workspace.dsl`](../c4/workspace.dsl), [12 — Contexto del sistema](12-contexto-del-sistema.md), `diagrams/c4/` |
| **E2.** «Infraestructura: Diagrame la infraestructura cloud propuesta con la herramienta de su preferencia.» | Pendiente. Los límites de confianza conceptuales ([14](14-limites-de-confianza.md)) son un insumo | `c4/workspace.dsl` (vista de despliegue) y `diagrams/` |
| **E3.** «Justificaciones: Documento breve con las decisiones de diseño: selección de frameworks frontend, flujo OAuth, herramientas de KYC/biometría, patrones de diseño aplicados y canales de notificación.» | Parcial: patrones de diseño aplicados en ADR-0001 a ADR-0009 | `docs/` y `docs/adr/` |
| **E4.** «Formato de entrega: Repositorio en GitHub o documento compartido con los diagramas y el documento de justificaciones.» | En curso | Este repositorio |

## 5. Trazabilidad inversa: ADR → requisito

| ADR | Requisitos |
|---|---|
| [ADR-0001](adr/ADR-0001-acl-por-dominio.md) | R1, C2, C5 |
| [ADR-0002](adr/ADR-0002-frontera-api-gateway.md) | R1 |
| [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) | R1, C2, C4, C5 |
| [ADR-0004](adr/ADR-0004-cache-aside.md) | R5, C4 |
| [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) | R1 (transferencias), R3, R4, C2 |
| [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) | R1 (transferencias), R3, C2 |
| [ADR-0007](adr/ADR-0007-cqrs-diferido.md) | R1 (movimientos), C5 |
| [ADR-0008](adr/ADR-0008-invalidacion-por-eventos.md) | R4, R5, C5 |
| [ADR-0009](adr/ADR-0009-core-autoritativo.md) | R3, contexto del reto (fuentes de información) |
| [ADR-0010](adr/ADR-0010-residencia-de-datos.md) | C1, C6 |
| [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md) | R1, C2, C5 |
| [ADR-0012](adr/ADR-0012-descomposicion-hibrida-de-contenedores.md) | R1, R3, R4, R5, C2, C5 |
| [ADR-0013](adr/ADR-0013-frontera-del-idp.md) | R2, R3, C1, C4 |
| [ADR-0014](adr/ADR-0014-api-gateway-y-bff-de-canales.md) | R1, R2, R6, C4 |
| [ADR-0015](adr/ADR-0015-mensajeria-asincrona-con-outbox.md) | R4, R5, C2, C5 |
| [ADR-0016](adr/ADR-0016-persistencia-y-durabilidad-del-estado.md) | R1, R3, R5, C2, C3 |
| [ADR-0017](adr/ADR-0017-stack-de-backend.md) | C5, C6 |
| [ADR-0018](adr/ADR-0018-stack-de-frontend.md) | R6, R3 |

## 6. Representación en el contexto del sistema (Fase 2)

Esta sección indica cómo aparece cada requisito en el diagrama de contexto (C4 nivel 1) y qué falta. **Un diagrama de nivel 1 no satisface por sí solo** requisitos que se resuelven en los niveles de contenedor, de componente o de infraestructura.

| Requisito | Representación en el contexto | Actores o sistemas externos | Decisiones | Supuestos | Brechas pendientes |
|---|---|---|---|---|---|
| R1 | REL-01, REL-03, REL-04, REL-05 | Cliente bancario; Core Bancario; Sistema Complementario; Sistema Externo de Transferencias | ADR-0001, ADR-0002 | S1, S2, S5, S13 | El API Gateway y los tres servicios se modelan en la Fase 3. Topología de transferencias (Q-01, DD-16) |
| R2 | REL-06; IdP interno desde la Fase 3 (ADR-0013; REL-10 y REL-11 retiradas) | Proveedor KYC/Biométrico; IdP (contenedor interno) | — | S9, S16 | Frontera del IdP (Q-03), flujo OAuth, vinculación de tokens (DD-03, DD-04) |
| R3 | Solicitante de vinculación; REL-02, REL-06, REL-03 (creación de cliente), REL-09; Fuente Oficial candidata (REL-12) | Solicitante; Proveedor KYC/Biométrico; Core Bancario | ADR-0005, ADR-0006, ADR-0009 | S16 | Métodos de acceso posteriores al onboarding (Q-14). Herramientas de industria (DD-14). Fuente oficial (Q-07) |
| R4 | REL-07, REL-08, REL-09; REL-13 por validar | Proveedores de Notificaciones; Core Bancario (por validar) | ADR-0005 (notificación en estados terminales), ADR-0008 | S14, S17 | Alcance de «cada movimiento» (Q-04). Canales (Q-10, DD-12) |
| R5 | Auditor candidato (REL-15) | Responsable de auditoría (candidato) | ADR-0004 (caché) | — | Base de datos de auditoría y componentes de caché en la Fase 3. Actores internos (Q-08, DD-18) |
| R6 | REL-01, REL-02 (la plataforma incluye los canales web y móvil) | Cliente; Solicitante | — | — | Los stacks de frontend son de la fase de frontend (DD-13) |
| C1 | Límites de confianza y datos sensibles hacia terceros ([14](14-limites-de-confianza.md), [13](13-catalogo-de-integraciones.md)) | Proveedores terceros | ADR-0010 | S8, S15 | Tipo de institución (Q-05). Datos biométricos (Q-06) |
| C2 | Dependencias externas identificadas, cuya falla se debe tolerar | Todos los sistemas externos | ADR-0001, ADR-0005 | S4 | Topología de alta disponibilidad (fase de infraestructura) |
| C3 | — | — | — | S10 | RTO, RPO y DR (DD-09) |
| C4 | Cruces de límites de confianza X-1 a X-6 | — | — | — | Controles concretos (fase de seguridad). SIEM institucional (Q-13) |
| C5 | Sistemas externos aislados detrás de la plataforma; nuevas integraciones como nuevas relaciones del contexto | — | ADR-0001 | S7 | Detalle en la Fase 3 |
| C6 | — | — | ADR-0010 | S8 | Infraestructura cloud (DD-15) |

## 7. Trazabilidad de la remediación ARG-01

Los cambios de la remediación no agregan requisitos. Precisan las decisiones que responden a los requisitos existentes. Detalle completo en el [informe de remediación](reviews/ARG-01-informe-de-remediacion.md).

| Hallazgo | Requisitos afectados | Decisión enmendada o creada | Escenarios de calidad | Riesgos |
|---|---|---|---|---|
| ARG01-C01, ARG01-M01 | R1, R3; AQ-02 | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) (versión 2) | QA3 (reformulado), QA9 (nuevo) | RR-04, RR-13 |
| ARG01-H01 | R1; AQ-02 | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), sección 2 | QA5 | — |
| ARG01-H02 | R1, R4; AQ-02 | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), secciones 3 a 5 | QA5 | RR-16 |
| ARG01-H03 | R1; AQ-02, AQ-04 | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), sección 6 | QA5 | RR-16 |
| ARG01-H04 | R1, C2, C5 | [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md) (nuevo); [ADR-0001](adr/ADR-0001-acl-por-dominio.md) (complementado) | QA8, QA10 (nuevo) | RR-14 |
| ARG01-H05 | R2, R3; AQ-01 | [ADR-0009](adr/ADR-0009-core-autoritativo.md) (versión 2) | QA11 (nuevo) | RR-15 |
| ARG01-M02 | R1, R3 | ADR-0005 (INV-T4, INV-T5), ADR-0009 (VD-09) | QA5 | RR-08 |
| R2-N01 (CF-01) | R1, R3; AQ-02 | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) v3 §3, §7 y §8; [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) v3 (IC-12); [ADR-0009](adr/ADR-0009-core-autoritativo.md) (VD-06) | QA5 (revisado), QA13 (nuevo) | RR-04, RR-17 |
| R2-N02 (CF-02) | R1, C2; AQ-02 | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) v3 §4; [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) v3 (IC-14) | QA12 (nuevo) | RR-18 |
| R2-N04 | R1; AQ-02 | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) v3 (IC-15) | QA9 | RR-13 |

## 8. Representación en los contenedores (Fase 3)

La cobertura y lo pendiente por requisito están en [ARG-02 — Preparación, sección 9](reviews/ARG-02-preparacion-revision-contenedores.md#9-cobertura-de-requisitos). Esta tabla indica **qué contenedor** realiza cada requisito.

| Requisito | Contenedores |
|---|---|
| R1 | API Gateway, BFF, Servicio de consultas (cliente y movimientos), Servicio de transferencias (provisional) |
| R2 | IdP, BFF (gestor de tokens), API Gateway (validación), Servicio de vinculación (KYC) |
| R3 | Aplicación móvil, Servicio de vinculación, IdP (activación) |
| R4 | Servicio de notificaciones, broker, servicios productores de eventos |
| R5 | Servicio y almacén de auditoría; caché de consultas en el Servicio de consultas |
| R6 | SPA web, aplicación móvil |
| C1–C6 | Transversales: [22](22-seguridad-entre-contenedores.md), [19](19-comunicacion-entre-contenedores.md), [18](18-propiedad-y-consistencia-de-datos.md), [21](21-alternativas-tecnologicas.md) |

## 9. Trazabilidad de la remediación de ARG-02

| Hallazgo | Requisitos | Decisión | Escenarios | Riesgos |
|---|---|---|---|---|
| ARG02-C01 | R1, R3; C3 | [ADR-0021](adr/ADR-0021-diario-de-autorizacion-de-despacho.md); ADR-0006 IC-13 e IC-16; ADR-0016 PT-07 y PT-08 | QA14 | RR-19, RR-24 |
| ARG02-H01 | C2, C3 | ADR-0016 (modos de durabilidad, detección) | QA14 | RR-19 |
| ARG02-H02 | R2, R6; C4 | [ADR-0019](adr/ADR-0019-sesiones-del-bff.md) | QA4 | RR-25 |
| ARG02-H03 | R5 | [ADR-0020](adr/ADR-0020-politica-de-auditoria-por-categoria.md) | QA6, QA15 | RR-23 |
| ARG02-H04 | R2, R3; C4 | ADR-0013 (enmienda) | QA11 | RR-21 |
| ARG02-H05 | C6 | S21; Q-20; [19 §5](19-comunicacion-entre-contenedores.md#5-ubicación-del-core-y-conectividad-arg02-h05) | — | RR-22 |

## 10. Representación en los componentes (Fase 4)

La trazabilidad de requisitos a componentes está en [24 §7](24-arquitectura-de-componentes.md#7-trazabilidad-de-componentes). **R1 (transferencias) y R3 (alta del cliente en el Core) solo tienen contratos de frontera** en el nivel 3 mientras rija B-05.

## 11. Cobertura de la entrega

La matriz consolidada de R1–R6 y C1–C6, con diagramas, documentos, evidencia implementada y estado separado de arquitectura y código, está en [entrega — matriz de cumplimiento](entrega/matriz-cumplimiento.md). C2, C3, C5 y C6 se completan con [27](27-infraestructura-cloud.md), [28](28-alta-disponibilidad-y-recuperacion.md) y [ADR-0022](adr/ADR-0022-infraestructura-cloud-de-referencia.md); R2, R3, C1 y C4 con [29](29-seguridad-identidad-y-cumplimiento.md) y [ADR-0023](adr/ADR-0023-autenticacion-y-sesiones.md).
