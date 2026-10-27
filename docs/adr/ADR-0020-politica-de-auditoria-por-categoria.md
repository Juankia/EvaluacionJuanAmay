# ADR-0020 — Política de auditoría por categoría de evento

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones** (remediación de ARG02-H03 y ARG02-M02). La retención y la confirmación de las categorías A4 y A5 dependen de la institución (Q-19). Pendiente de ARG-03 |
| Fecha | 2026-10-27 |
| Requisitos | R5; C1, C4 |
| Drivers y supuestos | AQ-03, AQ-04; QA6 |
| Relacionados | [ADR-0015](ADR-0015-mensajeria-asincrona-con-outbox.md), [ADR-0016](ADR-0016-persistencia-y-durabilidad-del-estado.md), [18 — Datos](../18-propiedad-y-consistencia-de-datos.md) |

## Contexto

El reto exige una base de datos de auditoría que registre **todas** las acciones del cliente (R5). La Fase 3 impuso una falla cerrada uniforme: una consulta no respondía si no podía escribir su evento de auditoría en la base de datos local. Eso hacía de esa base un punto único de falla de todas las consultas, sin análisis (ARG02-H03).

No se conocen todavía los requisitos regulatorios concretos de auditoría ni su retención (Q-19, M05). **No se debilita ningún control para ganar disponibilidad.**

## Problema

¿Qué acciones deben fallar si no se puede establecer evidencia de auditoría durable, y cuáles pueden entregarse de forma asíncrona sin perder garantías?

## Decisión

### 1. Categorías

| Cat. | Categoría | Ejemplos | ¿Falla cerrada? | Mecanismo de durabilidad | Orden | Minimización |
|---|---|---|---|---|---|---|
| **A1** | Comando financiero (autorización y ejecución) | Reserva de la intención, permiso de envío, resultado, reverso, resolución | **Sí** | Mismo *commit* local que la transición (outbox). Si no se puede auditar, la transición no ocurre | Por operación (secuencia) | Identificadores, montos, cuentas enmascaradas |
| **A2** | Acción de cuenta sensible para la seguridad | Cambio de credenciales o de factores, nuevo dispositivo, bloqueo, revocación | **Sí** | Registro en el almacén del IdP junto con el cambio; el IdP rechaza el cambio si no puede registrarlo. Requisito para el producto (fase de seguridad) | Por sujeto | Sin secretos ni factores |
| **A3** | Identidad y vinculación | Etapas E1 a E6, activación del acceso, asignación del origen del cliente | **Sí** | Outbox en la misma transacción del servicio de vinculación | Por solicitud | Sin datos biométricos; solo referencias a los resultados de KYC |
| **A4** | Acceso a datos sensibles | Datos personales completos, datos extendidos, documentos | **Sí** (hasta que Q-19 indique otra cosa) | **Doble ruta durable:** publicación con confirmación en el broker **o** outbox local. La respuesta espera la confirmación de **una** de las dos | No requerido | Tipo de dato y recurso; **sin el contenido** |
| **A5** | Consulta ordinaria | Resumen de cuentas, saldos, movimientos | **Solo si fallan ambas rutas** | Igual que A4 | No requerido | Tipo de consulta y recurso |
| **A6** | Operación privilegiada | Resoluciones en la consola, cierres excepcionales, cambios de configuración de seguridad, acciones administrativas en el IdP | **Sí** | Mismo *commit* que la acción | Global por caso | Identidad del personal, evidencia |
| **A7** | Autenticación (inicio de sesión correcto o fallido) | Eventos del IdP | **No** (el inicio de sesión no espera la entrega remota) | Persistencia en el IdP y entrega asíncrona; la pérdida es **detectable** por secuencia | Por sujeto | Identificador, dispositivo, resultado |
| **A8** | Observabilidad técnica | Métricas, trazas, registros técnicos | No | Mejor esfuerzo; **no es auditoría** | — | Sin datos sensibles |

**Garantía de la doble ruta (A4 y A5):** la evidencia queda durable en el broker (confirmación del publicador) o en el outbox local. La disponibilidad de la consulta pasa a depender de que **al menos una** de dos rutas independientes esté disponible, no de una sola. **Es la misma garantía de durabilidad** que la versión anterior.

### 2. Almacén de auditoría

| Aspecto | Decisión |
|---|---|
| Deduplicación | Por identificador de evento, **antes** de encadenar |
| Encadenamiento | **Por partición**: fuente (contenedor) y, dentro de ella, una partición por clave. Cada partición tiene su propia cadena de hashes, de modo que varias réplicas consumidoras anexan en paralelo sin una cadena global serializada (resuelve ARG02-M02) |
| Detección de huecos | Secuencia por fuente; un hueco genera una alerta |
| Anclaje | Anclaje periódico de las cabezas de las cadenas en el archivo inmutable (bloqueo de retención) |
| Acceso | Solo lectura para los roles de auditoría; ningún rol puede modificar ni borrar; el acceso de lectura también se audita (A6) |
| Retención | Por acordar (Q-19), por categoría |
| Falla de entrega | El evento permanece en el outbox de origen; alertas por antigüedad (`T_MAX_RETRASO_AUDITORIA`) |

### 3. Límites del outbox

El outbox garantiza la consistencia **entre una transición local y su evento**. **No** resuelve la consistencia con sistemas que la plataforma no controla:

- el Core: para eso existe la conciliación (ADR-0005);
- el almacén interno del IdP: para eso existe el requisito A2 sobre el producto.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Falla cerrada uniforme con una sola ruta (Fase 3) | Reemplazada | Punto único de falla de todas las consultas sin un análisis |
| Auditoría asíncrona de mejor esfuerzo para las lecturas | Descartada | Debilita R5 sin una decisión institucional |
| **Categorías con doble ruta para las lecturas** | **Seleccionada** | Conserva la durabilidad y elimina el punto único de falla |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Disponibilidad de las consultas sin debilitar la auditoría | Dos rutas que mantener y monitorear |
| Ingesta paralela con integridad por partición | La verificación de integridad se hace por partición |

## Consecuencias

- El documento 18 §4.5 queda **sustituido** por esta política.
- La categoría A2 es un requisito de selección del producto de IdP.

## Riesgos

- RR-23: pérdida de disponibilidad si fallan ambas rutas (A4 y A5) o el outbox (A1, A3, A6).
