# ARG-02 — Registro de decisión y de remediación (Fase 3.1)

| Campo | Valor |
|---|---|
| Revisión de origen | [ARG-02 — Revisión de contenedores](ARG-02-revision-contenedores.md) (**conservada sin cambios**) |
| Línea base | Commit `658c73e` |
| Fecha | 2026-10-27 |
| Autor | Arquitecto de la solución |
| Estado | **Remediación completada en diseño — pendiente de verificación independiente** (ver la [revisión focalizada](ARG-02-R-revision-focalizada.md)) |

---

## 1. Decisión de gobierno vigente

La política de los gates se mantiene: **un hallazgo CRITICAL no se convierte en un GO incondicional.** La decisión vigente es:

> **NO-GO para el diseño de los componentes de ejecución y recuperación de transferencias; autorización condicional para el diseño de componentes independientes no afectados.**

### 1.1 Alcance exacto de la excepción

| Alcance | Situación | Razón |
|---|---|---|
| **Servicio de transferencias, completo** (entrada de API y reserva, orquestador, permiso y despacho, ACL de escritura, conciliación, recuperación, efecto tardío) | **NO-GO** para el diseño de componentes | ARG02-C01 afecta la reserva (IC-16), el despacho (PT-07), la recuperación (ADR-0021) y la conciliación. CF-01 y CF-02 no tienen verificación independiente |
| **Servicio de vinculación: ejecución y recuperación del alta en el Core** (tramo `CUSTOMER_CREATION`) | **NO-GO** para el diseño de componentes | Usa el mismo protocolo de permiso único y la misma recuperación (ADR-0016 lo somete a PT-01 a PT-03 y PT-07). *ARG-02 no lo señaló explícitamente; esta remediación extiende el bloqueo por coherencia* |
| Servicio de vinculación: verificación de identidad, búsqueda (lectura), autorización de la vinculación, activación en el IdP, API de operaciones | **Autorización condicional** | No despachan efectos financieros ni de alta. La frontera con el ejecutor bloqueado se modela como un puerto |
| BFF, servicio de consultas, servicio de notificaciones, servicio de auditoría | **Autorización condicional** | Independientes de ARG02-C01. El BFF solo **retransmite** la API de transferencias (PT-04), sin lógica |
| IdP, Gateway, broker, almacenes | Sin componentes propios en la Fase 4 | Productos o almacenes; su diseño es de configuración |

La condición de la autorización es que la Fase 4 **no introduzca dependencias** de los componentes bloqueados más allá de sus contratos de frontera.

## 2. Clasificación de los bloqueos existentes

| Elemento | Clasificación | Evidencia o condición |
|---|---|---|
| ARG02-C01 | **Antes del diseño de componentes** (solo el alcance bloqueado) y **antes de la implementación** | Resuelto en diseño ([ADR-0021](../adr/ADR-0021-diario-de-autorizacion-de-despacho.md)); requiere verificación independiente y DD-23 |
| ARG02-H01 | **Antes de la implementación** | Resuelto en diseño (ADR-0016, modos de durabilidad); requiere validar la configuración real (CP-10) |
| ARG02-H02 | **Ya resuelto en diseño** (con evidencia) | [ADR-0019](../adr/ADR-0019-sesiones-del-bff.md); contenedor agregado en el DSL |
| ARG02-H03 | **Ya resuelto en diseño**; retención: **pendiente de validación externa** (Q-19) | [ADR-0020](../adr/ADR-0020-politica-de-auditoria-por-categoria.md) |
| ARG02-H04 | **Antes de la implementación** de la vinculación | ADR-0013 (enmienda); mecanismo en la fase de seguridad |
| ARG02-H05 | **Pendiente de validación externa** (Q-20) y **antes de la implementación** | S21 registrado como DESCONOCIDO; candidatos en [19 §5](../19-comunicacion-entre-contenedores.md#5-ubicación-del-core-y-conectividad-arg02-h05) |
| B-01 (transferencias productivas) | **Antes de la implementación** | Sin cambios |
| B-02 (`PRE_EXISTING`) | **Antes de la producción** | Sin cambios |
| B-03 (interbancaria) | **Pendiente de validación externa** (Q-01) | Sin cambios |
| B-04 (cloud y región) | **Pendiente de validación externa** (ADR-0010) | Sin cambios |
| CF-01, CF-02 | **Antes del diseño de componentes** (alcance bloqueado) | Pendientes de verificación independiente |
| PT-01 a PT-06 | **Antes de la implementación** | PT-02 reformulada; PT-06 ampliada |
| PT-07, PT-08 (nuevas) | **Antes de la implementación** | ADR-0016 y ADR-0021 |
| CP-01 a CP-09 | **Antes de la producción** | Sin cambios. **CP-10 nueva:** validación de la configuración real de replicación y conmutación |
| DD-21 | **Antes de la producción** (la ruta `PRE_EXISTING` está deshabilitada) | Sin cambios |
| Q-18 | **Pendiente de validación externa**; dependencia de la decisión **antes de la implementación** | Reforzado por ARG02-M07 |

**No todo lo diferido es un bloqueo actual.** Ejemplos: el proveedor cloud, los productos, el flujo OAuth y M2.

## 3. Remediación de los hallazgos CRITICAL y HIGH

| Hallazgo | Remediación | Evidencia | Estado propuesto |
|---|---|---|---|
| **ARG02-C01** | Diario independiente con quórum en ≥ 3 dominios de falla; INV-DR e INV-DR-1; referencia determinista; época regional; recuperación con marca de agua; falla cerrada y modo de suspensión regional si el diario no es viable | ADR-0021; ADR-0006 v4 (IC-13, IC-16); ADR-0016 (PT-02, PT-07, PT-08); QA14 | **Resuelto en diseño. INV-DR se cumple sin capacidades del Core**, condicionado a DD-23 o a la adopción del modo de suspensión. Pendiente de verificación independiente |
| **ARG02-H01** | Modos de durabilidad (NORMAL, DEGRADADO_BD, DEGRADADO_DIARIO, INCIERTO); detección; cuarentena automática ante cualquier conmutación hasta demostrar que no hubo pérdida; salida autorizada por dos personas; validación de la configuración real (CP-10) | ADR-0016 (enmienda) | Resuelto en diseño |
| **ARG02-H02** | BFF con proceso sin estado y almacén de sesiones dedicado; cookie y CSRF; rotación y revocación; falla cerrada | ADR-0019; contenedor nuevo en el DSL | Resuelto en diseño |
| **ARG02-H03** | Ocho categorías de auditoría; doble ruta durable para las lecturas; encadenamiento por partición | ADR-0020; QA15 | Resuelto en diseño (retención: Q-19) |
| **ARG02-H04** | Identidad del personal separada; operación de vinculación acotada con una afirmación firmada; una vinculación por cliente; límites de volumen y notificación; acceso justo a tiempo; ciclo de vida de las claves; recuperación del IdP | ADR-0013 (enmienda) | Resuelto en diseño; mecanismo en la fase de seguridad |
| **ARG02-H05** | Ubicación **DESCONOCIDA** (S21); cuatro modelos candidatos analizados sin seleccionar; Q-20; RR-22 | [19 §5](../19-comunicacion-entre-contenedores.md#5-ubicación-del-core-y-conectividad-arg02-h05) | Documentado como supuesto; **pendiente de evidencia externa** |

## 4. Hallazgos MEDIUM y LOW

| Hallazgo | Disposición | Evidencia |
|---|---|---|
| M01 (orden y versionado de eventos) | **Se resuelve en la Fase 4** (contratos de eventos) | Documento 25 de la Fase 4 (contratos, eventos y errores) |
| M02 (encadenamiento de auditoría) | Resuelto en diseño | ADR-0020 §2 |
| M03 (cuota 0; sesiones del Core) | Resuelto en diseño | RI-03 ampliado, RI-08 |
| M04 (operación del IdP) | Resuelto en diseño | ADR-0013 (enmienda); ADR-0016 (BD del IdP aislada) |
| M05 (contactos y tokens de dispositivo) | Resuelto en el modelo | Relaciones en el DSL; [18 §6](../18-propiedad-y-consistencia-de-datos.md#6-actualización-de-la-remediación-de-arg-02) |
| M06 (degradación del BFF) | **Se resuelve en la Fase 4** (componentes del BFF) | Documento 24 de la Fase 4 (arquitectura de componentes) |
| M07 (sensibilidad de la descomposición) | Documentado; Q-18 como dependencia de la decisión | [20 §6](../20-alternativas-de-descomposicion.md#6-actualización-de-la-remediación-de-arg-02-arg02-m07-l03) |
| L01 (inventario) | Resuelto: **21** contenedores contados en el DSL | [16 §4](../16-arquitectura-de-contenedores.md#4-inventario-de-contenedores) |
| L02 (BD de vinculación) | Resuelto | ADR-0016; DSL |
| L03 (correlación de criterios) | Resuelto (documentado) | 20 §6 |
| L04 (accesibilidad) | Objetivo **candidato**: conformidad WCAG 2.1 nivel AA, por acordar con la institución | [21 §10](../21-alternativas-tecnologicas.md#10-actualización-de-la-remediación-de-arg-02) |
| L05 (lecturas de réplicas) | Resuelto | ADR-0016 |
| L06 (PT-06) | Resuelto | 23 §7 |
| L07 (clave en la SPA) | Resuelto | ADR-0019 (intención pendiente en la sesión, sin que el BFF genere claves) |

## 5. Cambios en el modelo

- Contenedores: **19 → 21** (almacén de sesiones del BFF; diario de autorización de despacho).
- Sistema externo candidato: IdP corporativo de la institución (identidad del personal).
- Relaciones nuevas:
  - BFF ↔ almacén de sesiones;
  - BFF → notificaciones (registro de dispositivos);
  - notificaciones → consultas (contactos);
  - BFF → broker (eventos de seguridad);
  - transferencias y vinculación → diario;
  - consola → IdP corporativo.
