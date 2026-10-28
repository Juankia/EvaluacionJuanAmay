# 27 — Infraestructura cloud de referencia

| Campo | Valor |
|---|---|
| Documento | Comparación AWS frente a Azure y arquitectura de despliegue de referencia |
| Requisitos | C2, C5, C6 (y apoyo a C3 y C4) |
| Decisión | [ADR-0022](adr/ADR-0022-infraestructura-cloud-de-referencia.md) — **arquitectura de referencia del reto; no es un despliegue aprobado** |
| Fecha | 2026-10-27 |
| Dependencias abiertas | ADR-0010 (residencia, validación legal), DD-15 (proveedor), Q-20 (ubicación del Core y latencia), Q-21 (dominios de falla) |

---

## 1. Resumen ejecutivo

Se propone **AWS como plataforma de referencia** para el reto. La diferencia frente a Azure es **pequeña** (87 frente a 84 sobre 100) y se explica sobre todo por la disponibilidad de un **RabbitMQ gestionado** (Amazon MQ), coherente con el broker que ya usa la base ejecutable. **No es una selección definitiva:** la ubicación del Core, la latencia medida y la validación legal de la residencia de datos pueden invertir el resultado (§4).

## 2. Supuestos explícitos de la evaluación

| ID | Supuesto | Fuente |
|---|---|---|
| SC-1 | La ubicación del Core y la topología de red **no se conocen** (Q-20). Ambos proveedores ofrecen enlaces privados dedicados y VPN IPsec | Q-20 |
| SC-2 | **No se asume** que exista una región de ninguno de los dos proveedores en Ecuador. La región se elige después de verificar la oferta vigente y **medir** la latencia hacia el Core | ADR-0010 |
| SC-3 | **No existe** una obligación verificada de alojar los datos solo en Ecuador; la residencia se somete a validación legal | ADR-0010, S8 |
| SC-4 | No se conoce si la institución ya opera Microsoft 365 o Entra ID, ni sus capacidades de operación cloud (Q-18) | Q-18 |
| SC-5 | Las puntuaciones son **cualitativas** (1 a 5), basadas en la correspondencia de servicios gestionados con el diseño. **No** son mediciones de latencia, disponibilidad ni costo | — |

## 3. Comparación ponderada

| Criterio | Peso | AWS | Azure | Justificación |
|---|---|---|---|---|
| Conectividad con el Core | 15 | 4 | 4 | Direct Connect / ExpressRoute y VPN IPsec en ambos. La calidad real depende de los proveedores locales de enlace (Q-20) |
| Latencia de red | 5 | 3 | 3 | Indeterminada sin región elegida ni medición (SC-2). Peso bajo porque no discrimina hoy |
| Servicios gestionados disponibles | 10 | 5 | 4 | Ambos cubren cómputo, PostgreSQL, caché y gateway. AWS ofrece además RabbitMQ gestionado |
| Alta disponibilidad | 10 | 5 | 5 | Zonas de disponibilidad y servicios multizona en ambos |
| Recuperación ante desastres | 10 | 4 | 4 | Réplicas entre regiones y respaldos gestionados en ambos; la conmutación regional sigue siendo un procedimiento propio (doc 28) |
| Seguridad | 10 | 5 | 5 | KMS/CloudHSM frente a Key Vault/Managed HSM; WAF, anti-DDoS y detección de amenazas en ambos |
| Identidad | 5 | 4 | 5 | El IdP de clientes es interno (ADR-0013) en ambos casos. Azure gana si el personal ya usa Entra ID (SC-4, no confirmado) |
| PostgreSQL | 10 | 5 | 5 | RDS/Aurora Multi-AZ y Flexible Server con HA por zona ofrecen réplica síncrona. ADR-0016 exige **validar** su semántica real en ambos |
| Mensajería | 5 | 5 | 3 | Amazon MQ for RabbitMQ conserva el protocolo de la base ejecutable. Azure Service Bus exigiría cambiar el adaptador (AMQP 1.0); el outbox limita el impacto |
| Observabilidad | 5 | 4 | 4 | Ambos integran OpenTelemetry; el diseño no depende de herramientas propietarias |
| Escalabilidad | 5 | 5 | 5 | Escalado horizontal gestionado en ambos; el límite real es el presupuesto de capacidad del Core (ADR-0011) |
| Costo y complejidad operativa | 5 | 3 | 3 | **Sin cifras:** requiere dimensionamiento y cotización. La complejidad es similar con servicios gestionados |
| Dependencia del proveedor | 5 | 3 | 3 | Similar. Se mitiga con componentes portables: Quarkus, PostgreSQL, protocolo AMQP, OpenTelemetry e infraestructura como código |
| **Total ponderado (sobre 100)** | **100** | **87,0** | **84,0** | Suma de peso × puntuación ÷ 5 |

## 4. Sensibilidad: cuándo se elegiría Azure

| Condición | Efecto |
|---|---|
| La institución opera Microsoft 365 / Entra ID y su equipo domina Azure (SC-4, Q-18) | Identidad y operación suben para Azure; el resultado se invierte |
| La latencia **medida** hacia el Core es claramente mejor desde una región de Azure | Conectividad y latencia deciden; se elige Azure |
| La validación legal admite una región solo de Azure | Decide ADR-0010 |
| Se decide reemplazar RabbitMQ por un servicio de tópicos nativo | Desaparece la ventaja de mensajería de AWS |

## 5. Arquitectura de despliegue de referencia

| Diagrama | Uso |
|---|---|
| [Vista ejecutiva (una página)](../diagrams/infraestructura/infraestructura-referencia-resumen.svg) | Lectura rápida: zonas, servicios, HA y DR |
| [Vista detallada desde el DSL](../diagrams/c4/structurizr-Infraestructura-Referencia.svg) | Cada instancia de contenedor y sus relaciones (fuente: `c4/workspace.dsl`, entorno `Referencia`) |

| Capa | Servicio de referencia (AWS) | Decisión o requisito de origen |
|---|---|---|
| Borde | CloudFront, AWS WAF, AWS Shield | C4; ADR-0002 |
| API Gateway | Gateway gestionado o Kong (DD-15) | ADR-0014 |
| Cómputo | Amazon EKS o ECS/Fargate (por decidir); réplicas en 3 zonas de disponibilidad | C2; RI-01, RI-02 |
| PostgreSQL por servicio | RDS o Aurora PostgreSQL Multi-AZ con réplica síncrona | ADR-0016 (PT-01, PT-03) |
| Caché y sesiones del BFF | ElastiCache compatible con Redis, multizona | ADR-0004, ADR-0019 |
| Broker | Amazon MQ for RabbitMQ, clúster de 3 nodos | ADR-0015 |
| Archivo de auditoría | S3 con Object Lock (WORM), retención por acordar | ADR-0020; Q-19 |
| Claves y secretos | KMS, CloudHSM (candidato para PF4-05), Secrets Manager | C4 |
| Observabilidad y seguridad | OpenTelemetry, CloudWatch, CloudTrail, GuardDuty, Security Hub; exportación al SIEM institucional si existe | C4; Q-13 |
| Conectividad con el Core | Direct Connect o VPN IPsec (por decidir) | ADR-0001, ADR-0011; Q-20 |
| DR | Región secundaria en espera templada | C3; doc 28 |
| Diario de despacho | **No se despliega** (ADR-0021 no aprobado; DD-23) | B-05, B-06 |

**Kubernetes no es un requisito.** EKS y ECS/Fargate quedan como opciones; se elige según las capacidades del equipo (Q-18) y los requisitos del entorno de ejecución (RI) del [documento 21 §6](21-alternativas-tecnologicas.md#6-api-gateway-idp-y-entorno-de-ejecución).

## 6. Escalabilidad y desacoplamiento (C5)

- Los servicios escalan de forma independiente. Su techo lo fija el presupuesto global del Core (ADR-0011), no la nube.
- Un servicio nuevo se agrega como contenedor con su propia base de datos y su suscripción al broker, sin modificar los existentes (ADR-0012, ADR-0015).
- Las ACL aíslan los cambios de contrato del Core (ADR-0001, QA7).

## 7. Lo que este documento no decide

- La región y el proveedor definitivos (DD-15, ADR-0010).
- Costos: requieren dimensionamiento con volúmenes reales.
- La topología de la conectividad con el Core (Q-20) y con la infraestructura de pagos (Q-01).
