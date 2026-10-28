# 28 — Alta disponibilidad y recuperación ante desastres

| Campo | Valor |
|---|---|
| Documento | Diseño de disponibilidad, respaldo, conmutación y recuperación |
| Requisitos | C2, C3 |
| Decisiones base | ADR-0005, ADR-0006, ADR-0011, ADR-0015, ADR-0016, ADR-0021, [ADR-0022](adr/ADR-0022-infraestructura-cloud-de-referencia.md) |
| Fecha | 2026-10-27 |
| Advertencia | **Los RTO y RPO de este documento son candidatos por acordar (DD-09). No se han medido ni probado.** ADR-0021 **no** fue aprobado en ARG-03 |

---

## 1. Principios

1. **Degradar antes que caer.** Ante una falla parcial, las consultas sirven datos en caché marcados como tales y las operaciones nuevas se rechazan con un error reintentable (QA1, QA3).
2. **Un timeout no es una falla.** Ninguna operación financiera con resultado desconocido se reintenta a ciegas; pasa a `UNKNOWN` y se concilia (ADR-0005, ADR-0006).
3. **Falla cerrada para la ejecución financiera.** Si la durabilidad del estado financiero no está garantizada, no se despacha (ADR-0016, PT-02 y PT-07).
4. **La recuperación es un procedimiento, no un botón.** La conmutación regional es manual, con cuarentena y conciliación.

## 2. Alta disponibilidad dentro de la región (C2)

| Componente | Mecanismo | Falla cubierta |
|---|---|---|
| Cómputo | Réplicas repartidas en 3 zonas de disponibilidad; sondas de salud; cierre ordenado | Caída de una réplica o de una zona |
| PostgreSQL | Primario y réplica síncrona en otra zona (PT-01); conmutación automática del servicio gestionado; un solo escritor (PT-03) | Caída del primario o de su zona |
| Caché y sesiones | Réplicas multizona. Si se pierde la caché, se lee del Core dentro de su cuota. Si se pierden sesiones, el usuario vuelve a autenticarse | Caída de un nodo |
| Broker | Clúster de 3 nodos en 3 zonas. Los eventos siguen en el outbox de cada servicio hasta su publicación confirmada | Caída de un nodo |
| Dependencias externas | Timeouts, cortacircuitos, cuotas por clase de operación y aislamiento por dominio en las ACL (ADR-0001, ADR-0011) | Core lento o caído; proveedor caído |

## 3. Escenarios de falla

| # | Escenario | Comportamiento esperado | Estado financiero |
|---|---|---|---|
| F1 | Cae una réplica de un servicio | El balanceador la retira; sin impacto visible | Sin efecto |
| F2 | Cae una zona de disponibilidad | Réplicas restantes; conmutación de PostgreSQL a la réplica síncrona | Sin pérdida de escrituras confirmadas (PT-01), **si** se valida la semántica real del servicio gestionado (ADR-0016) |
| F3 | El Core no responde | Consultas desde caché marcadas como `CACHED`/`DEGRADED`; vinculación y operaciones nuevas rechazadas con error reintentable | Operaciones en curso → `UNKNOWN` y conciliación |
| F4 | Se pierde el enlace privado con el Core | Igual que F3 | Igual que F3 |
| F5 | Cae el broker | Los eventos se acumulan en los outbox; se publican al recuperarse | Sin efecto |
| F6 | Cae el proveedor KYC | Vinculación en `IDENTITY_VERIFICATION_UNAVAILABLE`, nunca rechazo (implementado y probado) | No aplica |
| F7 | Cae un proveedor de notificaciones | Reintento acotado y canal alternativo; sin entrega → cola de mensajes muertos con alerta (implementado y probado) | No aplica |
| F8 | Se pierde la región principal | Conmutación manual a la región secundaria (§6) | **Cuarentena:** sin iniciaciones ni despachos hasta conciliar con el Core |
| F9 | Corrupción lógica o borrado (error humano, ransomware) | Restauración en un punto del tiempo desde respaldos aislados | Cuarentena (PT-02) |
| F10 | Se pierde el diario de despacho o pierde quórum | Falla cerrada (`DEGRADADO_DIARIO`): sin despachos | Seguro, sin disponibilidad. **Restaurar el diario desde una instantánea es INSEGURO** (ARG03-H01) |

## 4. Puntos únicos de falla y mitigación

| Punto | Por qué es crítico | Mitigación | Residual |
|---|---|---|---|
| Core Bancario | Autoritativo y fuera del control de la plataforma | Caché para lecturas; presupuesto de capacidad; estados `UNKNOWN` | La disponibilidad de operaciones depende del Core |
| Enlace con el Core | Un único enlace es un punto único | Enlace redundante o VPN de respaldo (por diseñar con Q-20) | Sin diseño hasta conocer la topología |
| Región principal | Todo el estado transaccional vive ahí (PT-03) | Región secundaria en espera templada | RTO de horas, no de minutos (candidato) |
| IdP | Sin IdP no hay inicio de sesión | Réplicas multizona y BD multizona | Las sesiones vigentes del BFF siguen válidas hasta su vencimiento |
| Diario de despacho | Sin diario no hay despachos (por diseño) | Quórum en ≥ 3 dominios de falla (DD-23, Q-21) | **No resuelto:** ADR-0021 no aprobado |

## 5. Respaldo y restauración

| Dato | Respaldo | Restauración | Condición tras restaurar |
|---|---|---|---|
| PostgreSQL (todas las BD) | Respaldos automáticos con recuperación en un punto del tiempo; copia a una **cuenta aislada** con bloqueo de retención | Nueva instancia desde el punto elegido | **Vinculación y transferencias:** cuarentena y conciliación con el Core (PT-02). **Resto:** reanudación normal |
| Archivo de auditoría | S3 Object Lock (WORM) y replicación entre regiones | Lectura directa del archivo | La cadena de hash permite detectar huecos |
| Outbox | Forman parte de la BD de cada servicio | Con la BD | Se republican los eventos no confirmados; los consumidores deduplican por `eventId` |
| Caché, sesiones y broker | No se respaldan: son derivados o transitorios | Se reconstruyen | Los usuarios vuelven a autenticarse |
| Configuración e infraestructura | Infraestructura como código e imágenes versionadas | Redespliegue | — |
| Claves (KMS) | Claves multirregión o réplicas gestionadas | Del proveedor | Sin la clave no se descifran los respaldos: se prueba en cada simulacro |

**Prueba de restauración:** sin ensayos periódicos los respaldos no cuentan como evidencia de recuperación. Se recomienda un simulacro trimestral de restauración y uno anual de conmutación regional (frecuencia por acordar).

## 6. Conmutación regional (runbook resumido)

| Paso | Acción | Responsable | Criterio para avanzar |
|---|---|---|---|
| 1 | Declarar el desastre según el criterio acordado (no automático) | Comité de crisis | Pérdida confirmada de la región principal |
| 2 | Activar la **cuarentena financiera global**: sin iniciaciones ni despachos (PT-02) | Operaciones | Bandera aplicada y verificada |
| 3 | Promover las réplicas de PostgreSQL de la región secundaria y registrar el punto de corte | Operaciones de datos | Bases escribibles; marca de agua registrada |
| 4 | Escalar el cómputo y redirigir DNS y borde | Plataforma | Sondas de salud correctas |
| 5 | Restablecer la conectividad con el Core desde la región secundaria | Redes | Enlace activo (topología por diseñar, Q-20) |
| 6 | Habilitar **solo lectura**: consultas y estado | Operaciones | Pruebas de humo superadas |
| 7 | Conciliar con el Core toda operación no terminal y toda clave creada después del punto de corte | Operaciones y negocio | Conciliación completa y firmada |
| 8 | Levantar la cuarentena **solo** si el diario de despacho es íntegro y autoritativo (ADR-0021) | Comité de crisis | Mientras ADR-0021 no esté aprobado, **las transferencias siguen suspendidas** |
| 9 | Republicar outbox, verificar huecos en la auditoría y comunicar | Operaciones | Sin huecos sin explicar |

## 7. Objetivos candidatos de RTO y RPO

**Candidatos para discusión con la institución (DD-09). No medidos, no probados, no comprometidos.**

| Capacidad | Falla zonal | Falla regional: RTO candidato | Falla regional: RPO candidato |
|---|---|---|---|
| Inicio de sesión y consultas | Automática, sin pérdida esperada | Del orden de horas (≤ 4 h como meta inicial) | ≤ 15 min (réplica asíncrona entre regiones) |
| Vinculación digital | Automática; RPO 0 de las confirmadas (PT-01) | Del orden de horas, después de conciliar | > 0 entre regiones → cuarentena y conciliación obligatorias |
| Notificaciones | Automática | Del orden de horas | Eventos recuperables desde los outbox; posible duplicado, nunca pérdida silenciosa |
| Auditoría | Automática | Del orden de horas | Archivo replicado; eventos recuperables desde los outbox mientras se conserven |
| Transferencias | **No aplica: bloqueadas (B-05)** | Requisito: no despachar hasta conciliar | **RPO 0** de las escrituras confirmadas (ADR-0016), no negociable |

Para cumplir un RPO regional igual a 0 en el estado financiero haría falta replicación síncrona entre regiones, o un diario por quórum en dominios independientes. Eso es precisamente lo que **no** está resuelto (DD-23, Q-21).

## 8. Dependencias de la recuperación

- Enlace con el Core desde la región secundaria (Q-20).
- Claves de KMS disponibles en la región secundaria.
- Personal y procedimiento de conciliación con el Core.
- Capacidades del Core para consultar por referencia (Q-02, Q-17, DD-22).
- Diario de despacho aprobado y desplegado (ADR-0021, DD-23).

## 9. Limitaciones explícitas de ADR-0021

ARG-03 **no aprobó** ADR-0021. Mientras no se enmiende y verifique:

| Hallazgo | Consecuencia operativa |
|---|---|
| ARG03-C01: con un testigo, una entrada puede confirmarse con datos en una sola región | No se puede afirmar «sin pérdida de entradas confirmadas» ante la pérdida de una región |
| ARG03-H01: no se trata la restauración del propio diario | **Prohibido** autorizar despachos con un diario restaurado sin el modo de diario incompleto |
| ARG03-H02: consenso, cercado y lecturas linealizables no especificados | Las garantías de recuperación sin duplicados son **condicionales** |

Por eso, la postura de DR para la ejecución financiera es **suspensión y conciliación**, no continuidad.
