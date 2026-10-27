# 21 — Alternativas tecnológicas

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Decisiones en ADR-0013 a ADR-0018; productos ligados al proveedor cloud **diferidos** (DD-15) |

## 1. Criterios

Cada evaluación usa los mismos criterios: idoneidad bancaria, madurez operativa, mantenibilidad, capacidades del equipo (Q-18), escalado horizontal, observabilidad, seguridad, costo y dependencia de un proveedor.

**Ninguna tecnología se elige por popularidad ni por familiaridad.** Cuando un producto depende del proveedor cloud, se fija la **capacidad requerida** y el producto se elige en la fase de infraestructura.

## 2. Backend

Ver [ADR-0017](adr/ADR-0017-stack-de-backend.md): **Java 21 (LTS) + Quarkus, provisional**. Spring Boot es la alternativa equivalente si Q-18 lo indica.

## 3. Persistencia relacional

| Opción | Idoneidad | Unicidad y CAS | Gestionado en AWS / Azure | Dependencia | Resultado |
|---|---|---|---|---|---|
| **PostgreSQL** | Alta | Nativo; incluye índices únicos parciales | Sí / Sí | Baja (código abierto) | **Seleccionado** ([ADR-0016](adr/ADR-0016-persistencia-y-durabilidad-del-estado.md)) |
| Oracle Database | Alta, frecuente en banca | Nativo | Sí / limitado | Alta (licenciamiento) | No seleccionado: costo y dependencia sin un beneficio diferencial para este estado |
| SQL Server | Alta | Nativo | Sí / Sí | Media | Alternativa si la institución lo estandariza |
| Base de datos distribuida multirregión | Alta disponibilidad | Sí | Sí | Media-alta | Descartada para el estado financiero (PT-03) |

**Requisito independiente del producto:** el servicio gestionado debe cumplir PT-01 (réplica síncrona o quórum sin pérdida de escrituras confirmadas en la conmutación) para el almacén transaccional.

## 4. Caché distribuida

| Opción | Evaluación | Resultado |
|---|---|---|
| **Caché gestionada compatible con Redis** | TTL, operaciones atómicas para las marcas de invalidación, alta disponibilidad gestionada en ambos proveedores | **Provisional** (DD-07); el producto concreto, con el proveedor cloud |
| Caché local en memoria por réplica | Sin invalidación coherente entre réplicas; incompatible con *read-your-writes* entre réplicas | Descartada como caché principal; admisible solo para catálogos estáticos |
| Sin caché | Incumple R5 y traslada toda la carga al Core | Descartada |

## 5. Mensajería

Ver [ADR-0015](adr/ADR-0015-mensajeria-asincrona-con-outbox.md).

| Opción | Evaluación | Resultado |
|---|---|---|
| **Broker gestionado de colas y tópicos** (por ejemplo, servicio de tópicos y colas de AWS o Azure Service Bus) | Cubre la distribución a varias suscripciones, las colas de mensajes fallidos y la durabilidad con baja carga operativa | **Seleccionado como capacidad**; producto con DD-15 |
| RabbitMQ autogestionado | Funcionalmente adecuado | No seleccionado: carga operativa sin un beneficio diferencial |
| Kafka (gestionado o propio) | Potente para flujos y reprocesamiento | **No seleccionado:** sin un driver de volumen, de reprocesamiento ni de procesamiento de flujos |

## 6. API Gateway, IdP y entorno de ejecución

| Componente | Opciones | Decisión |
|---|---|---|
| API Gateway | Gateway gestionado del proveedor cloud; gateway autogestionado (por ejemplo, Kong) | **Diferido** (DD-15). Capacidades requeridas: validación de JWT, limitación de tasa por cliente, integración con WAF, correlación, configuración como código |
| IdP | Autohospedado (candidato: Keycloak); gestionado del proveedor cloud dentro del entorno de la institución; SaaS de un tercero | **Frontera interna** decidida ([ADR-0013](adr/ADR-0013-frontera-del-idp.md)). Producto **provisional**; la fase de seguridad decide con estos criterios: FAPI, DPoP o mTLS, MFA, API de administración restringible, ubicación de los datos |
| Entorno de ejecución de contenedores | Kubernetes gestionado (EKS o AKS); servicio de contenedores gestionado sin clúster (ECS/Fargate o Azure Container Apps) | **Diferido** (fase de infraestructura). **Requisitos que debe permitir:** techo de réplicas por servicio (RI-01), control del surge y de la estrategia de despliegue (RI-02), cierre ordenado (RI-02), interruptor de admisión por configuración, identidad de carga de trabajo para mTLS entre servicios |

## 7. Frontend y móvil: dos alternativas (R6)

| Criterio | **Opción A:** React + TS (web) / Flutter (móvil) | **Opción B:** React + TS (web) / React Native + TS (móvil) |
|---|---|---|
| Capacidades del equipo | Requiere TypeScript y Dart | **Un solo lenguaje** (TypeScript) |
| Mantenibilidad | Dos lenguajes y dos ecosistemas | **Tipos y validaciones compartidos** con la web |
| Accesibilidad | Buena; semántica propia del motor de renderizado | Buena; usa los componentes de accesibilidad nativos |
| Rendimiento | **Muy bueno** y uniforme (motor propio) | Bueno con la arquitectura nativa moderna |
| Biometría nativa y almacenamiento seguro | Complementos maduros | Módulos nativos maduros |
| SDK de proveedores KYC | Disponibilidad variable según el proveedor | Disponibilidad variable según el proveedor |
| Seguridad | Código compilado de forma anticipada; protección en tiempo de ejecución por complementos | Código JavaScript empaquetado; requiere ofuscación y protección en tiempo de ejecución |
| Código compartido | Bajo con la web | **Alto** (lógica no visual y clientes de API) |
| Pruebas | Herramientas propias integradas | Ecosistema JavaScript (pruebas unitarias y de extremo a extremo) |
| CI/CD | Una cadena para Flutter y otra para la web | Cadena común en gran parte |
| Soporte a largo plazo | Respaldado por un gran proveedor; ecosistema creciente | Respaldado por un gran proveedor; ecosistema muy amplio |
| Costo total | Mayor si el equipo web no domina Dart | **Menor** con un equipo TypeScript |

**Recomendación: opción B** ([ADR-0018](adr/ADR-0018-stack-de-frontend.md)), condicionada a Q-18 y DD-14.

**Advertencia:** la biometría local del dispositivo **no equivale** a la verificación de identidad ni a la prueba de vida del servidor KYC.

## 8. Despliegue en AWS o Azure

El diseño de contenedores se puede desplegar en ambos proveedores. La selección (DD-15) sigue pendiente de ADR-0010 (residencia) y de la latencia hacia el Core.

| Capacidad | AWS (ejemplos) | Azure (ejemplos) |
|---|---|---|
| Contenedores | EKS, ECS/Fargate | AKS, Container Apps |
| PostgreSQL con réplica síncrona | RDS o Aurora PostgreSQL multizona | Azure Database for PostgreSQL Flexible Server con alta disponibilidad por zona |
| Caché compatible con Redis | ElastiCache | Azure Cache for Redis |
| Tópicos y colas | Servicios gestionados de tópicos y colas | Service Bus |
| API Gateway y WAF | API Gateway y WAF | API Management, Application Gateway y WAF |
| Archivo inmutable | Almacenamiento de objetos con bloqueo de retención | Almacenamiento de blobs inmutable |

## 9. Resumen

| Decisión | Estado |
|---|---|
| Descomposición híbrida (ADR-0012) | Aceptada con condiciones |
| IdP interno (ADR-0013) | Frontera aceptada con condiciones; producto provisional |
| Gateway + BFF compartido (ADR-0014) | Aceptada con condiciones; producto de Gateway diferido |
| Outbox + broker gestionado (ADR-0015) | Patrón aceptado; producto diferido |
| PostgreSQL por servicio + PT-01 a PT-03 (ADR-0016) | Aceptada con condiciones |
| Java 21 + Quarkus (ADR-0017) | Provisional |
| React + TS / React Native + TS (ADR-0018) | Aceptada con condiciones |
| Caché compatible con Redis | Provisional (DD-07) |
| Proveedor cloud y entorno de ejecución | Diferidos (DD-15) |
