# ARG-02 — Preparación de la revisión de contenedores

| Campo | Valor |
|---|---|
| Revisión | ARG-02 (preparación; **no es una revisión ni una aprobación**) |
| Objeto | Fase 3 — C4 Nivel 2: Contenedores |
| Fecha | 2026-10-27 |
| Autor | Arquitecto de la solución |
| Estado | **Pendiente de revisión independiente ARG-02. El autor no aprueba su propio diseño** |

## 1. Resumen de la arquitectura

- **Descomposición híbrida:** cinco servicios de dominio (consultas, vinculación, transferencias, notificaciones y auditoría), un BFF compartido, un API Gateway y un IdP interno.
- **Almacenes:** PostgreSQL por servicio, almacén de auditoría de solo anexión y caché derivada.
- **Mensajería:** outbox transaccional y broker gestionado.
- **Contexto:** se conserva el de la Fase 2. El IdP candidato pasa a contenedor interno (ADR-0013).
- **Transferencias:** el orquestador está modelado como **PROVISIONAL**.

Documento principal: [16 — Arquitectura de contenedores](../16-arquitectura-de-contenedores.md).

## 2. Descomposición seleccionada y matriz

[20](../20-alternativas-de-descomposicion.md): B (híbrida) = **4,15**; A (monolito modular) = 3,27; C (microservicios) = 3,13. B se mantiene en la prueba de sensibilidad. Decisión: [ADR-0012](../adr/ADR-0012-descomposicion-hibrida-de-contenedores.md).

## 3. Inventario de contenedores C4

[16 §4](../16-arquitectura-de-contenedores.md#4-inventario-de-contenedores). Vistas: `Contenedores`, `Contenedores-Consultas`, `Contenedores-Vinculacion`, `Contenedores-Transferencias`, `Contenedores-NotificacionesAuditoria` y `Contenedores-IntegracionCore`, generadas desde [`c4/workspace.dsl`](../../c4/workspace.dsl) en `diagrams/c4/`.

## 4. Matrices

- Responsabilidades: [17](../17-matriz-de-responsabilidades.md).
- Propiedad de datos: [18 §2](../18-propiedad-y-consistencia-de-datos.md#2-matriz-de-propiedad-de-datos).

## 5. Fronteras de seguridad

[22](../22-seguridad-entre-contenedores.md): subzonas de TB-2; gestor de tokens de la SPA en el BFF; autorización de negocio en cada servicio; **punto de control único** para activar el acceso digital; ruta `PRE_EXISTING` bloqueada hasta DD-21.

## 6. Condiciones de seguridad transaccional

[23 §2 y §3](../23-riesgos-y-condiciones-fase-3.md#2-verificación-acotada-de-cf-01-y-cf-02-frente-al-diseño-de-contenedores): la verificación acotada encontró **seis contraejemplos a nivel de contenedores** (V-3 a V-8), cerrados con PT-01 a PT-06. No se encontró un contraejemplo crítico sin cierre.

## 7. Protección de la capacidad del Core

[19 §3](../19-comunicacion-entre-contenedores.md#3-protección-de-la-capacidad-del-core-adr-0011-y-r2-n06): cinco mecanismos distinguidos, reparto por clase y servicio, M1 corregida con `N_EFECTIVO_MAX`, RI-01 a RI-07 y el trade-off de subutilización.

## 8. Alternativas tecnológicas

[21](../21-alternativas-tecnologicas.md); ADR-0013 a ADR-0018.

## 9. Cobertura de requisitos

| Requisito | Cobertura tras la Fase 3 | Pendiente |
|---|---|---|
| R1 Gateway + 3 servicios | Gateway, BFF, servicio de consultas (cliente y movimientos), servicio de transferencias; interpretación de «orquestar» en ADR-0002 y ADR-0014 | Contratos de API detallados (fase de componentes); topología interbancaria (Q-01) |
| R2 OAuth 2.0, KYC y biometría | IdP interno; gestor de tokens; KYC en la vinculación | Flujo OAuth, FAPI, vinculación de tokens (DD-03, DD-04); proveedor KYC (DD-14) |
| R3 Onboarding y acceso posterior | Servicio de vinculación E1 a E6; activación del acceso; biometría local frente a KYC distinguidas | DD-21; métodos de acceso (Q-14) |
| R4 Notificaciones (≥ 2 canales) | Servicio de notificaciones: push + SMS o correo, canal alternativo, eventos por movimiento | Proveedores (DD-12); alcance de «cada movimiento» (Q-04) |
| R5 Auditoría y caché | Servicio y almacén de auditoría (solo anexión, encadenado); caché con *read-your-writes* y marcas de invalidación | Retención (Q-19); TTL con negocio |
| R6 Dos stacks de frontend | Opciones A y B comparadas; B recomendada (ADR-0018) | Q-18; SDK KYC |
| C1 Normativa | Sin afirmaciones de cumplimiento; datos sensibles identificados | Registro `REG-PV` (M05) |
| C2 Alta disponibilidad | Aislamiento por servicio; degradación; outbox | Topología de infraestructura |
| C3 DR | PT-01 y PT-02 para el estado financiero | RTO y RPO generales (DD-09) |
| C4 Seguridad y monitoreo | [22](../22-seguridad-entre-contenedores.md); observabilidad en [19 §4](../19-comunicacion-entre-contenedores.md#4-observabilidad-transversal) | SIEM (Q-13) |
| C5 Escalabilidad y desacoplamiento | Híbrida; broker; cuotas por réplica | M2 (DD-20) |
| C6 Cloud y latencia | Desplegable en AWS y en Azure ([21 §8](../21-alternativas-tecnologicas.md#8-despliegue-en-aws-o-azure)) | DD-15; ADR-0010 |

## 10. Riesgos residuales

RR-01 a RR-21 ([11](../11-riesgos-residuales.md)). Los más relevantes para ARG-02:

- **RR-17:** vivacidad ante la incertidumbre.
- **RR-19:** durabilidad del almacén transaccional.
- **RR-21:** IdP crítico.
- **RR-14:** cuotas.

## 11. Preguntas institucionales abiertas

Q-01, Q-02, Q-04, Q-05, Q-06, Q-08, Q-12, Q-15, Q-16, Q-17, Q-18 y Q-19 ([15](../15-preguntas-abiertas-fase-2.md)).

## 12. Bloqueado antes de la implementación

[23 §4](../23-riesgos-y-condiciones-fase-3.md#4-bloqueos-antes-de-cualquier-implementación-productiva): B-01 (transferencias), B-02 (`PRE_EXISTING`), B-03 (interbancaria), B-04 (cloud y región).

## 13. Lo que el autor pide al comité verificar

1. Que la verificación acotada (V-1 a V-10) no haya pasado por alto un contraejemplo **a nivel de contenedores**. En particular: la recuperación del IdP, la consola de operaciones y los consumidores del broker.
2. Si la ruta de autorización de R1 («orquestar») es aceptable para el evaluador del reto.
3. Si la subutilización de M1 (CL-1) es aceptable o exige activar DD-20 antes de producción.
4. Si la auditoría síncrona de cada consulta (outbox antes de responder) es proporcional.
5. Si el modelo provisional del servicio de transferencias es suficiente para continuar con la fase de componentes, manteniendo B-01.
