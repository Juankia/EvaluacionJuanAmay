# ADR-0021 — Diario de autorización de despacho y recuperación regional segura

| Campo | Valor |
|---|---|
| Estado | **No aprobada en [ARG-03](../reviews/ARG-03-revision-componentes.md)** (ARG03-C01 CRITICAL; ARG03-H01 y H02 HIGH): requiere enmienda y verificación humana independiente antes de levantar B-05. Estado anterior: **Aceptada con condiciones** (remediación de ARG02-C01). Pendiente de verificación independiente y de la selección de la capacidad de almacenamiento (DD-23). **No autoriza el diseño de los componentes de ejecución y recuperación** mientras no se verifique |
| Fecha | 2026-10-27 |
| Requisitos | R1 (transferencias), R3 (alta del cliente); C2, C3 |
| Drivers y supuestos | S3, S19, S20; AQ-02, AQ-05; QA3, QA12, QA14 |
| Relacionados | [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [ADR-0016](ADR-0016-persistencia-y-durabilidad-del-estado.md), [ARG-02](../reviews/ARG-02-revision-contenedores.md), [Registro de remediación ARG-02](../reviews/ARG-02-registro-de-remediacion.md) |

## Contexto

ARG02-C01 demostró la siguiente secuencia:

1. una intención se persiste en la región primaria;
2. la solicitud llega al Core y este aplica el débito;
3. la región primaria falla;
4. la secundaria, con replicación asíncrona, no tiene la reserva ni la clave;
5. el cliente reenvía la operación;
6. la plataforma concede un **permiso nuevo** y envía un segundo débito.

PT-02 (cuarentena) no lo impide, porque **no puede conciliar operaciones que ya no conoce**. Se asume que el Core no ofrece idempotencia nativa ni búsqueda por referencia (S3); ninguna de esas capacidades se da por existente.

Errores de razonamiento que este ADR evita explícitamente:

- creer que la replicación síncrona garantiza por sí misma cero pérdida (ARG02-H01);
- creer que un *commit* en la base de datos significa que el Core ejecutó la operación;
- creer que una operación ausente de la base restaurada nunca se envió;
- usar **el mismo almacén** como única evidencia de ejecución y como único medio para detectar su pérdida.

## Problema

¿Cómo se garantiza, sin capacidades adicionales del Core, que tras una pérdida de datos la plataforma **nunca despache automáticamente de nuevo** una operación cuya ejecución previa no puede excluirse?

## Decisión

### 1. Invariante de seguridad

> **INV-DR:** ninguna operación se despacha automáticamente de nuevo cuando no puede excluirse de forma fiable una ejecución previa.

**Esto no es una garantía de ejecución exactamente una vez.**

- INV-DR es una propiedad de **seguridad** que la plataforma puede hacer cumplir: evita **reenviar**.
- No garantiza que el Core ejecute una vez (S20), ni que una ejecución tardía no ocurra (DD-22), ni que toda operación se resuelva sin intervención humana (vivacidad, RR-17).

### 2. Diario de autorización de despacho

Es un almacén **independiente** del almacén transaccional de transferencias y del de vinculación:

| Propiedad | Requisito |
|---|---|
| Contenido mínimo por entrada | Identificador de operación, tramo (`CORE_EXECUTION`, `EXTERNAL_PROCESSING`, `REVERSAL`, `CUSTOMER_CREATION`), resumen criptográfico del ámbito y de la clave, huella canónica, referencia externa derivada (sección 4), token de cercado, época regional, instante. **Sin datos personales en claro** |
| Semántica | **Solo anexión**, con anexión condicional (comparar y fijar) sobre la época regional; una sola entrada por (operación, tramo) |
| Durabilidad | Confirmación por **quórum en al menos tres dominios de falla independientes**, de modo que la pérdida de una región **no** pierda entradas confirmadas. Por ejemplo, dos regiones y un testigo, o la topología equivalente que ofrezca el proveedor |
| Linealizabilidad | Lecturas y escrituras linealizables para las entradas y la época |
| Independencia | Sin dependencia de la base de datos transaccional: otra tecnología, otro dominio de falla y otras credenciales |
| Retención | Al menos `RETENCION_CLAVE` + `VENTANA_VIGILANCIA_EFECTO_TARDIO` |

### 3. Regla de despacho (complementa ADR-0005 §4.2)

```text
… 3. Conceder el permiso de envío (escritura cercada en el almacén transaccional)
  3b. ANEXAR la entrada en el diario con la época regional vigente y ESPERAR su confirmación por quórum
      - si la anexión falla o es incierta → NO enviar; la operación queda en *_IN_PROGRESS → *_UNKNOWN
      - si la época del diario no coincide con la de la región → NO enviar (la región está cercada)
  4. Confirmación durable del paso 3 Y del paso 3b
  5. Verificar MAX_DESFASE_ENVIO
  6. Enviar una vez
```

**INV-DR-1:** ningún tramo se despacha sin una entrada confirmada en el diario. Por eso, **una operación sin entrada en el diario nunca fue despachada.**

### 4. Referencia externa determinista (enmienda de ADR-0006 IC-13)

La referencia que se propaga al Core es `HMAC(clave_de_derivación, ámbito ‖ clave ‖ tramo)`, truncada al formato que acepte el Core (por validar con Q-17).

- **Ventaja:** tras una pérdida, un reenvío con la misma clave permite **recalcular** la referencia y buscarla en el diario y, si el Core la conserva, en sus movimientos (evidencia EV-POS).
- **La clave de derivación es un secreto** gestionado en el gestor de claves; su rotación conserva las versiones anteriores durante la retención.

### 5. Época regional y cercado de regiones

- El diario mantiene la **época regional vigente** y la región que la posee.
- **Promover** la región secundaria **incrementa la época** mediante una escritura condicional en el diario. Desde ese momento, el diario rechaza las anexiones con la época anterior. Así, **la región anterior no puede autorizar nuevos despachos**, aunque siga viva (cerebro dividido).
- Los envíos que la región anterior ya había anotado en el diario son **envíos únicos tardíos** (igual que un ejecutor zombi, RR-18): no hay un segundo despacho.

### 6. Protocolo de recuperación con marca de agua

Se aplica tras una conmutación regional, una restauración desde un respaldo o una conmutación con durabilidad incierta (ARG02-H01).

| Paso | Acción |
|---|---|
| R-1 | **Cuarentena:** se rechazan sin efectos las nuevas iniciaciones financieras y no se conceden permisos de envío. Las consultas siguen operando |
| R-2 | Se incrementa la época regional en el diario (cercado de la región anterior) |
| R-3 | Se determina la **marca de agua**: la última posición durable del almacén restaurado |
| R-4 | Se recorren **todas las entradas del diario** sin un estado terminal confirmado en el almacén restaurado, o posteriores a la marca de agua. Toda entrada cuya operación **falte** o esté en un estado **previo al permiso** se **reconstruye** como `*_UNKNOWN`, con el diario como evidencia de despacho posible |
| R-5 | Se concilian las operaciones reconstruidas: EV-POS por referencia determinista o por evidencia huérfana; si no la hay, revisión manual (ADR-0005 §7) |
| R-6 | **Salida de la cuarentena:** verificación automática de que **todas** las entradas del diario están reflejadas **y** autorización de dos personas (rol de operaciones y rol de riesgo) |
| R-7 | **Reenvíos del cliente después de la recuperación:** si la clave corresponde a una operación (existente o reconstruida), se devuelve su estado (IC-08). Si **no existe en el almacén ni en el diario**, la operación **nunca se despachó** (INV-DR-1) y puede reservarse como nueva |

### 7. Modo degradado (falla cerrada)

| Situación | Comportamiento |
|---|---|
| El diario no está disponible o no confirma la anexión | **No se despacha.** Las transferencias y el alta de clientes dejan de estar disponibles; las consultas siguen. Es una pérdida de disponibilidad, no de seguridad |
| La institución no puede proveer tres dominios de falla independientes | **No hay conmutación regional automática para el estado financiero.** Ante un desastre regional, las transferencias y el alta **se suspenden** hasta conciliar manualmente con el Core. El RTO de estas funciones crece; INV-DR se mantiene |

### 8. Alcance

Todos los tramos con efecto en el Core o en sistemas externos: ejecución de transferencias, procesamiento externo, reversos y **alta del cliente** (servicio de vinculación).

## Alternativas consideradas

Escala: ✔ favorable · ◐ parcial · ✘ desfavorable.

| Alternativa | Seguridad financiera | Exposición a pérdida | RTO / RPO | Dependencia del Core | Disponibilidad | Complejidad | Costo | Carga operativa | Modo de falla principal | Resultado |
|---|---|---|---|---|---|---|---|---|---|---|
| A1. Solo replicación síncrona entre zonas (PT-01) | ✘ ante un desastre regional | Pérdida regional | RPO 0 zonal; regional > 0 | No | ✔ | ✔ | ✔ | ✔ | Pérdida regional = C01 | Insuficiente por sí sola |
| A2. Replicación síncrona entre regiones de **todo** el almacén transaccional | ✔ | Mínima | RPO 0; latencia entre regiones en cada escritura | No | ◐ (acoplada a dos regiones) | ◐ | ✘ | ◐ | Partición entre regiones detiene todas las escrituras | Viable; más costosa y con mayor latencia que A3 |
| **A3. Diario independiente con quórum + época regional + marca de agua** | **✔** | Solo si fallan dos de tres dominios | RPO 0 de los despachos; RTO = cuarentena + conciliación | **No** (para la seguridad) | ◐ (sin diario no hay despacho) | ◐ | ◐ | ◐ | Diario no disponible → sin despachos | **Seleccionada** |
| A4. Idempotencia o consulta por referencia del Core | ✔ si existe | — | — | **Sí** (no validada, S3) | ✔ | ✔ | ✔ | ✔ | Capacidad inexistente | Complemento condicionado (Q-17, DD-22) |
| A5. Referencia determinista + búsqueda de movimientos (G3) | ◐: la evidencia negativa no excluye | — | — | Parcial | ✔ | ✔ | ✔ | ◐ | Falso negativo → reenvío | Complemento para la vivacidad, **nunca** para excluir la ejecución |
| A6. Cuarentena + revisión manual de todo reenvío con clave desconocida en una ventana sospechosa | ✔ | — | RTO alto | No | ✘ | ✔ | ✔ | ✘ | Volumen manual inmanejable | Solo como respaldo (sección 7) |
| A7. Reconstrucción desde la auditoría o el broker (asíncronos) | ✘ para la ventana reciente | Ventana asíncrona | — | No | ✔ | ◐ | ✔ | ◐ | Eventos no entregados antes de la pérdida | Complemento de evidencia, no de exclusión |
| A8. Suspender las funciones financieras tras el desastre hasta la conciliación manual | ✔ | — | RTO alto | No | ✘ | ✔ | ✔ | ✘ | Indisponibilidad prolongada | **Respaldo obligatorio** si A3 no es viable |

## Trade-offs

| Se gana | Se paga |
|---|---|
| INV-DR se cumple **sin depender del Core** | Una escritura con quórum entre dominios de falla antes de cada despacho (latencia adicional; cifra por medir) |
| Cercado de regiones ante un cerebro dividido | Un almacén adicional, crítico para la disponibilidad de las funciones financieras |
| Reenvíos seguros después de la recuperación | Cuarentena y autorización de dos personas tras cada conmutación |

## Consecuencias

- Nuevo contenedor de datos: **Diario de autorización de despacho** (provisional) en el nivel 2.
- ADR-0006 IC-13 cambia a una referencia determinista. ADR-0016 incorpora PT-07 y PT-08 y los modos de durabilidad (ARG02-H01).
- **DD-23:** seleccionar la capacidad de almacenamiento del diario (linealizable, con quórum en ≥ 3 dominios de falla) en la fase de infraestructura. Si no existe en la topología disponible, rige la sección 7.
- **Liveness:** sin la búsqueda por referencia en el Core (Q-17), las operaciones reconstruidas se resuelven manualmente.

## Riesgos

- **RR-24:** disponibilidad y corrección del diario (un defecto en el diario es un defecto de seguridad).
- RR-17 (vivacidad tras la recuperación).
- S19 y S20 (siguen sin verificar).
