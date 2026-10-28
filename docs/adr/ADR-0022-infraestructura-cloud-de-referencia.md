# ADR-0022 — Infraestructura cloud de referencia en AWS, multizona y con DR en espera templada

| Campo | Valor |
|---|---|
| Estado | **Propuesta** (arquitectura de referencia para el reto). **No aprobada para producción.** Pendiente de ADR-0010, DD-15, Q-20 y Q-21 |
| Fecha | 2026-10-27 |
| Requisitos | C2, C3, C5, C6 |
| Drivers y supuestos | AQ-05, AQ-06; S8, S10; SC-1 a SC-5 ([27 §2](../27-infraestructura-cloud.md#2-supuestos-explícitos-de-la-evaluación)) |
| Relacionados | [ADR-0010](ADR-0010-residencia-de-datos.md), [ADR-0011](ADR-0011-presupuesto-de-capacidad-del-core.md), [ADR-0015](ADR-0015-mensajeria-asincrona-con-outbox.md), [ADR-0016](ADR-0016-persistencia-y-durabilidad-del-estado.md), [ADR-0021](ADR-0021-diario-de-autorizacion-de-despacho.md), [27](../27-infraestructura-cloud.md), [28](../28-alta-disponibilidad-y-recuperacion.md) |

## Contexto

El reto pide una infraestructura en AWS o Azure con baja latencia (C6), alta disponibilidad (C2) y recuperación ante desastres (C3). DD-15 seguía diferida porque faltan la ubicación del Core (Q-20) y la validación legal de la residencia (ADR-0010).

## Problema

¿Sobre qué proveedor y con qué topología se presenta una infraestructura defendible, sin inventar la ubicación del Core, latencias, costos ni obligaciones legales?

## Decisión

1. Se adopta **AWS como arquitectura de referencia del reto**, según la comparación ponderada del [documento 27](../27-infraestructura-cloud.md#3-comparación-ponderada) (87 frente a 84). La ventaja principal es Amazon MQ for RabbitMQ, que conserva el protocolo de la base ejecutable.
2. **Región principal con tres zonas de disponibilidad**: cómputo replicado, PostgreSQL por servicio con réplica síncrona en otra zona (PT-01) y un solo escritor (PT-03), caché multizona y broker de tres nodos.
3. **Región secundaria en espera templada**, con réplica asíncrona y respaldos aislados. La conmutación es **manual** y aplica la **cuarentena financiera** (PT-02).
4. **Conectividad privada con el Core** (Direct Connect o VPN IPsec), cuya topología queda **por confirmar**.
5. El **diario de despacho no se despliega** mientras ADR-0021 no esté aprobado.
6. El entorno de ejecución (EKS o ECS/Fargate) **no se fija**. Kubernetes no es un requisito.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Azure como referencia | Alternativa válida (84/100) | Se elige si la institución es centrada en Microsoft o si la latencia medida la favorece ([27 §4](../27-infraestructura-cloud.md#4-sensibilidad-cuándo-se-elegiría-azure)) |
| Activo-activo multirregión | Descartada para el estado financiero | Viola PT-03 y multiplica los riesgos de cerebro dividido |
| Una sola zona | Descartada | No cumple C2 |
| Seguir sin proveedor (DD-15 abierta) | Descartada para la entrega | C6 exige una propuesta concreta; se presenta como referencia condicionada |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Servicios gestionados que corresponden uno a uno con el diseño | Dependencia moderada del proveedor, mitigada con componentes portables |
| Disponibilidad zonal automática | Latencia de escritura por la réplica síncrona |
| DR simple y auditable (espera templada + cuarentena) | RTO regional de horas y suspensión de la ejecución financiera durante la conmutación |

## Consecuencias

- DD-15 pasa a «referencia propuesta»; la selección definitiva sigue condicionada.
- DD-09 recibe RTO/RPO **candidatos** ([28 §7](../28-alta-disponibilidad-y-recuperacion.md#7-objetivos-candidatos-de-rto-y-rpo)), no compromisos.
- El DSL incluye el entorno de despliegue `Referencia` y su vista `Infraestructura-Referencia`.

## Riesgos

- La latencia real hacia el Core podría invalidar la región o el proveedor (Q-20).
- La semántica real de replicación de los servicios gestionados debe validarse (ADR-0016).
- Los costos no están estimados.
