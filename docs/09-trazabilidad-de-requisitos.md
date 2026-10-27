# 09 — Matriz de trazabilidad de requisitos

| Campo | Valor |
|---|---|
| Fase | 0–1 |
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
| **E1.** «Modelo C4 hasta nivel de componentes: Diagrama de Contexto (actores y sistemas externos), Diagrama de Contenedores (aplicaciones, bases de datos, servicios), Diagrama de Componentes (detalle interno de los contenedores principales)» | Base del modelo inicializada; vistas pendientes (Fase 2 en adelante) | [`c4/workspace.dsl`](../c4/workspace.dsl) |
| **E2.** «Infraestructura: Diagrame la infraestructura cloud propuesta con la herramienta de su preferencia.» | Pendiente | `c4/workspace.dsl` (vista de despliegue) y `diagrams/` |
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
