# 23 — Riesgos y condiciones de la Fase 3

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Vigente. Insumo para la revisión ARG-02 |
| Fuentes | [ARG-01-R2](reviews/ARG-01-R2-verificacion-independiente.md), [Remediación CF-01/CF-02](reviews/ARG-01-R2-remediacion-CF01-CF02.md), [10](10-decisiones-diferidas.md), [11](11-riesgos-residuales.md) |

## 1. Inventario de decisiones al iniciar la Fase 3

| Decisión | Estado | Fundamento |
|---|---|---|
| ADR-0001 ACL por dominio; ADR-0011 presupuesto del Core | **Aprobado con condiciones** | Condicionalmente aceptados en ARG-01-R2; R2-N06 y R2-N07 abiertos (atendidos aquí en el diseño) |
| ADR-0002 Gateway como fachada | **Aprobado con condiciones** | Sin hallazgos de ARG-01-R2 en contra |
| ADR-0003 SDK; ADR-0004 Cache-Aside; ADR-0008 invalidación | **Aprobado con condiciones** | M03 y M08 abiertos en ARG-01 |
| ADR-0005 v3 / ADR-0006 v3 (CF-01, CF-02) | **Provisional** | Remediados en diseño, **sin verificación independiente** |
| ADR-0007 CQRS | **Diferido** | Disparadores medibles |
| ADR-0009 vinculación | **Aprobado con condiciones** | R2-N08 y DD-21 abiertos |
| ADR-0010 residencia | **Diferido** (propuesta) | Validación legal |
| ADR-0012 a ADR-0016, ADR-0018 (Fase 3) | **Aprobado con condiciones** (propuestos a ARG-02) | Este documento |
| ADR-0017 backend | **Provisional** | Q-18 |
| Supuestos S19 y S20 | **No verificados** | Q-17 |
| Implementación productiva de transferencias | **Bloqueado** | Sección 4 |
| Ruta `PRE_EXISTING` de vinculación en producción | **Bloqueado** | DD-21 |

## 2. Verificación acotada de CF-01 y CF-02 frente al diseño de contenedores

**Objetivo:** buscar contraejemplos que el diseño de **contenedores** introduzca sobre los invariantes INV-T1 a INV-T11 (ADR-0005 v3) e IC-01 a IC-15 (ADR-0006 v3). **No es** una repetición de ARG-01. **No se ejecutó ninguna prueba**: es un análisis conceptual.

| ID | Escenario a nivel de contenedores | Invariante en riesgo | Resultado | Acción |
|---|---|---|---|---|
| V-1 | Dos réplicas del servicio de transferencias compiten por la misma operación | INV-T3, INV-T4 | **Se preserva** (CAS en el almacén) | — |
| V-2 | Ejecutor zombi tras la toma de titularidad | INV-T3 | **Se preserva** (permiso único) | — |
| V-3 | **Conmutación del almacén con replicación asíncrona:** se pierde la escritura confirmada del permiso; la operación reaparece en `VALIDATING` y otro ejecutor concede un **segundo permiso** | INV-T3 | **CONTRAEJEMPLO**: dos solicitudes al Core | **PT-01** (RPO 0 de las escrituras confirmadas) |
| V-4 | **Restauración desde un respaldo** anterior a los permisos concedidos: los ejecutores retoman operaciones «previas al envío» y reenvían | INV-T3, INV-T9 | **CONTRAEJEMPLO** | **PT-02** (cuarentena tras restauración) |
| V-5 | **Activo-activo multirregión** del almacén: dos regiones aceptan el CAS de la misma operación | INV-T4 | **CONTRAEJEMPLO** | **PT-03** (escritor único) |
| V-6 | El BFF o el Gateway generan o sustituyen la clave de idempotencia en un reintento | IC-02, IC-08 | **CONTRAEJEMPLO** si ocurre | **PT-04** (propagación sin cambios; el borde y el BFF nunca generan claves) |
| V-7 | La consola crea una operación financiera «en nombre» del cliente tras un cierre excepcional | IC-12 | **CONTRAEJEMPLO** si se permite | **PT-05** (la consola no inicia operaciones financieras) |
| V-8 | La **biblioteca cliente HTTP o SOAP** de la ACL reintenta automáticamente una escritura tras una conexión reiniciada | INV-T3, S19 | **CONTRAEJEMPLO** (reintento oculto) | **PT-06** (reintentos automáticos desactivados en las bibliotecas y en los intermediarios propios) |
| V-9 | Entrega al menos una vez del broker (eventos duplicados) | Ninguno financiero | **Se preserva** (consumidores idempotentes; las notificaciones no son la fuente) | — |
| V-10 | Diferencia de reloj entre réplicas en los arrendamientos | INV-T4 | **Se preserva** (reloj del almacén) | — |

**Conclusión:** **no se encontró ningún contraejemplo que el diseño no pueda cerrar** con requisitos explícitos. Los contraejemplos V-3 a V-8 se cierran con PT-01 a PT-06. Por eso el contenedor de transferencias queda **PROVISIONAL** y no **BLOQUEADO**. Su implementación **sí queda bloqueada** hasta cumplir la sección 4.

## 3. Condiciones PT (protección transaccional)

| ID | Condición | Dónde se materializa | Verificación |
|---|---|---|---|
| PT-01 | RPO 0 de las escrituras confirmadas del almacén transaccional (réplica síncrona o quórum) | [ADR-0016](adr/ADR-0016-persistencia-y-durabilidad-del-estado.md); fase de infraestructura | Prueba de conmutación sin pérdida de escrituras confirmadas |
| PT-02 | Cuarentena tras restauración o conmutación con pérdida: sin permisos de envío hasta conciliar | ADR-0016; procedimientos de DR | Simulacro de restauración |
| PT-03 | Escritor único para el estado financiero | ADR-0016 | Revisión de la topología |
| PT-04 | El BFF y el Gateway propagan `Idempotency-Key` sin cambios; nunca la generan ni reintentan inicios | [ADR-0014](adr/ADR-0014-api-gateway-y-bff-de-canales.md) | Pruebas de contrato y de arquitectura |
| PT-05 | La consola de operaciones no inicia operaciones financieras en nombre del cliente | [17](17-matriz-de-responsabilidades.md) | Revisión de la API de operaciones |
| PT-06 | Reintentos automáticos desactivados en las bibliotecas cliente y en los intermediarios propios para las escrituras CL-1 | [19](19-comunicacion-entre-contenedores.md); [ADR-0017](adr/ADR-0017-stack-de-backend.md) | Prueba de conteo de solicitudes en el lado del Core (con CP-02 y S19) |

## 4. Bloqueos antes de cualquier implementación productiva

| ID | Bloqueo | Se levanta cuando |
|---|---|---|
| B-01 | Ejecución productiva de transferencias | Verificación independiente de CF-01 y CF-02 **y** PT-01 a PT-06 verificados **y** CP-01 a CP-03 de ARG-01-R2 superadas |
| B-02 | Ruta `PRE_EXISTING` de vinculación | DD-21 implementada, probada y aprobada (CP-05) |
| B-03 | Integración interbancaria | Q-01 resuelta y modelada |
| B-04 | Selección del proveedor cloud y de la región | ADR-0010 (validación legal) y DD-15 |

## 5. Estado de las condiciones de ARG-01-R2

| Condición | Estado tras la Fase 3 |
|---|---|
| CF-01, CF-02 | Remediadas en diseño; **pendientes de verificación independiente** |
| CF-03 (RI-01 a RI-07) | Incorporadas al diseño ([19 §3](19-comunicacion-entre-contenedores.md#3-protección-de-la-capacidad-del-core-adr-0011-y-r2-n06)); pendientes de verificación |
| CF-04 (estados previos al envío + M04) | INV-T10 resuelve la expiración; **M04 (Gateway frente al plazo) sigue abierto** |
| CF-05 (bloqueo de equivalentes) | IC-15; pendiente de verificación |
| CF-06 (vinculación) | Reglas en [22 §3.2](22-seguridad-entre-contenedores.md#32-reglas-adicionales-que-atienden-r2-n08-requisitos-mecanismo-en-dd-21); pendientes de verificación |
| CF-07 (autorización de reversos y límites por monto) | **Abierta**. INV-T6 e INV-T9 limitan el reverso, pero falta la política de autorización por monto |
| CF-08 (respuestas institucionales Q-02, Q-08, Q-12, Q-16) | **Abierta** (depende de la institución) |
| CF-09 (*read-your-writes*, M03) | Resuelta en diseño ([16 §9](16-arquitectura-de-contenedores.md#9-caché-y-experiencia-del-cliente-resolución-de-diseño-de-m03)); pendiente de verificación |

## 6. Riesgos nuevos de la Fase 3

Se registran en [11](11-riesgos-residuales.md).

| ID | Riesgo | Nivel |
|---|---|---|
| RR-19 | Durabilidad del almacén transaccional y restauración (V-3 a V-5) | Alto hasta verificar PT-01 a PT-03 |
| RR-20 | Complejidad operativa de la arquitectura híbrida | Medio |
| RR-21 | El IdP interno como componente crítico de disponibilidad y de seguridad | Medio-alto |

## 7. Actualización de la remediación de ARG-02

| Elemento | Cambio |
|---|---|
| PT-02 | Reformulada: la cuarentena rechaza las nuevas iniciaciones y concilia contra el diario de despacho ([ADR-0021](adr/ADR-0021-diario-de-autorizacion-de-despacho.md)) |
| PT-06 | Redacción ampliada (ARG02-L06): incluye explícitamente los **sidecars de malla de servicios, los proxies de salida y los balanceadores** propios que puedan reintentar escrituras CL-1 |
| **PT-07** (nueva) | Ningún tramo se despacha sin una entrada confirmada por quórum en el diario (INV-DR-1) |
| **PT-08** (nueva) | Cercado regional por época en el diario |
| **B-05** (nuevo bloqueo) | Diseño de los componentes de **ejecución y recuperación** de transferencias **y del alta de clientes**: bloqueado hasta la verificación independiente de CF-01, CF-02 y la remediación de ARG02-C01 |
| **B-06** (nuevo bloqueo) | Implementación de los despachos financieros: requiere DD-23 (capacidad del diario) o la adopción explícita del modo de suspensión regional (ADR-0021 §7) |
