# ADR-0016 — Persistencia relacional por servicio y durabilidad del estado financiero

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones — enmendada en la remediación de ARG-02** (ARG02-C01, H01, M04, L02, L05). El servicio gestionado concreto se decide con el proveedor cloud (DD-15) |
| Fecha | 2026-10-27 |
| Requisitos | R1, R3, R5; C2, C3 |
| Drivers y supuestos | S2; AQ-02, AQ-04, AQ-05; QA3, QA6, QA12 |
| Relacionados | [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [ADR-0015](ADR-0015-mensajeria-asincrona-con-outbox.md), [18 — Propiedad y consistencia de datos](../18-propiedad-y-consistencia-de-datos.md), [23 — Riesgos y condiciones](../23-riesgos-y-condiciones-fase-3.md) |

## Contexto

Las garantías de ADR-0005 y ADR-0006 se apoyan en un almacén con:

- escrituras atómicas;
- restricciones de unicidad (IC-07, IC-15);
- escrituras condicionales (CAS) para la titularidad cercada;
- un reloj único para los arrendamientos.

La verificación acotada de la Fase 3 ([23](../23-riesgos-y-condiciones-fase-3.md)) encontró que esas garantías **se rompen** si el almacén pierde escrituras confirmadas (conmutación con replicación asíncrona, restauración desde un respaldo) o si admite varios escritores.

## Problema

¿Qué tipo de persistencia usa cada servicio y qué requisitos de durabilidad debe cumplir el almacén del estado financiero para que el permiso de envío único no se pierda ni se duplique?

## Decisión

1. **Motor relacional (PostgreSQL)** para:
   - las intenciones e idempotencia, el estado de las transferencias, la titularidad, la evidencia huérfana y los casos de conciliación;
   - el estado de vinculación;
   - los registros de notificación;
   - los metadatos de auditoría;
   - los outbox.

   Razones: transacciones ACID, restricciones de unicidad y escrituras condicionales nativas, bloqueo de filas para los trabajadores, madurez y disponibilidad como servicio gestionado en AWS y en Azure (sin dependencia de un proveedor único).
2. **Un almacén lógico por servicio.** Credenciales separadas y ningún acceso de escritura cruzado. Varios almacenes lógicos **pueden compartir un clúster físico** por costo, salvo el **almacén transaccional de transferencias**, que se aísla para no compartir su recuperación ni su capacidad.
3. **Requisitos obligatorios del almacén transaccional de transferencias** (y del de vinculación para el alta de clientes):

   | ID | Requisito |
   |---|---|
   | **PT-01** | **RPO 0 de las escrituras confirmadas:** una confirmación solo se reconoce tras su replicación síncrona (o por quórum). La conmutación no pierde escrituras confirmadas |
   | **PT-02** | **Cuarentena tras restauración:** después de cualquier restauración o conmutación con posible pérdida de datos, **ningún ejecutor concede permisos de envío** hasta conciliar con el Core todas las operaciones no terminales y las creadas en la ventana de pérdida |
   | **PT-03** | **Escritor único:** un solo nodo escritor; sin activo-activo multirregión para este estado |

4. **El almacén de auditoría** se compone de una tabla de solo anexión con encadenamiento de hashes y un archivo inmutable en almacenamiento de objetos con bloqueo de retención. La retención está por acordar (Q-19).
5. **La caché** (ADR-0004) no es un almacén de persistencia y **nunca** es fuente de verdad.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Base de datos documental o clave-valor para el estado | No seleccionada | Las restricciones de unicidad compuestas y el CAS multicampo son menos directos; no aporta ventajas para este volumen |
| Base de datos única compartida por todos los servicios | Descartada | Acopla esquemas y fallas; contradice ADR-0012 |
| Base de datos distribuida multirregión activa-activa | Descartada para el estado financiero | Viola PT-03 |
| Libro mayor propio en la plataforma | Descartada | Crearía una segunda fuente de verdad financiera (S2) |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Garantías de CAS y unicidad necesarias para ADR-0005 y ADR-0006 | La replicación síncrona aumenta la latencia de escritura |
| Cuarentena segura tras desastres | La recuperación de las transferencias es más lenta (se prioriza la seguridad) |
| Portabilidad entre AWS y Azure | Operación de varios almacenes lógicos |

## Consecuencias

- PT-01 a PT-03 se trasladan como requisitos a la fase de infraestructura y DR (DD-09).
- El RPO general de la plataforma sigue por acordar; **para el estado financiero no es negociable** (RPO 0 de las escrituras confirmadas).

## Riesgos

- RR-19: durabilidad del almacén transaccional y restauración.

## Enmienda de la remediación de ARG-02

| ID | Cambio | Origen |
|---|---|---|
| **Alcance de PT-01 a PT-03** | Aplican **explícitamente** a la BD transaccional de transferencias **y** a la BD de vinculación (alta de clientes) | ARG02-L02 |
| **PT-02 (reformulada)** | La cuarentena **también rechaza las nuevas iniciaciones financieras**, no solo los permisos de envío. La conciliación se apoya en el **diario de autorización de despacho**, no solo en el almacén restaurado ([ADR-0021](ADR-0021-diario-de-autorizacion-de-despacho.md) §6) | ARG02-C01 |
| **PT-07 (nueva)** | Ningún tramo se despacha sin una entrada confirmada por quórum en el diario de autorización de despacho (INV-DR-1) | ARG02-C01 |
| **PT-08 (nueva)** | La promoción de una región requiere incrementar la época regional en el diario; una región con una época anterior no puede autorizar despachos | ARG02-C01, escenario de cerebro dividido |
| **Modos de durabilidad (nuevo)** | **NORMAL:** réplica síncrona confirmada y diario disponible. **DEGRADADO_BD:** se perdió la réplica síncrona o hay retraso; se permiten despachos **solo** si el diario está disponible, porque es la evidencia independiente; alerta. **DEGRADADO_DIARIO:** el diario no está disponible; **sin despachos**. **INCIERTO:** hubo una conmutación o una restauración con escrituras confirmadas inciertas; **cuarentena** (ADR-0021 §6) | ARG02-H01 |
| **Detección (nuevo)** | El servicio publica y evalúa: el estado de la réplica síncrona, el retraso de la replicación, los eventos de conmutación del servicio gestionado, la época regional del diario y la coincidencia entre la marca de agua y el diario. Toda conmutación activa el modo INCIERTO **hasta demostrar** que no hubo pérdida | ARG02-H01 |
| **Validación de la configuración (nuevo)** | **No se presume** la semántica de replicación de ningún servicio gestionado. Antes de la implementación se valida la configuración real: comportamiento ante la pérdida de la réplica síncrona, conmutación y protección contra varios escritores (CP-10) | ARG02-H01 |
| **Lecturas de estado (nuevo)** | Ninguna decisión de estado (transiciones, IC-07, IC-15, titularidad) se toma con lecturas de réplicas; solo con el primario | ARG02-L05 |
| **BD del IdP aislada (nuevo)** | La BD del IdP **no comparte** el clúster físico con otros almacenes | ARG02-M04 |
