# 10 — Registro de decisiones diferidas

| Campo | Valor |
|---|---|
| Fase | 0–1 |
| Estado | Vigente |

## 1. Propósito

Una **decisión diferida** es una decisión que se pospone de forma intencional porque aún no hay información suficiente o porque corresponde a una fase posterior. Diferir no es omitir: cada decisión diferida tiene un **disparador** (la condición que obliga a tomarla) y una fase prevista.

Esto evita seleccionar tecnología antes de entender el problema. En el registro no hay «Kafka por escalabilidad», «CQRS por ser banca» ni «Redis por ser caché».

## 2. Registro

| ID | Decisión | Por qué se difiere | Disparador | Fase prevista | Referencia |
|---|---|---|---|---|---|
| DD-01 | **Tecnología del bus de eventos** | No se ha confirmado qué flujos asíncronos son requisitos arquitectónicos | Se confirma como requisito la invalidación de caché por eventos, la auditoría asíncrona, las notificaciones, la analítica u otros flujos asíncronos | Infraestructura y plataforma | [ADR-0008](adr/ADR-0008-event-driven-invalidation.md) |
| DD-02 | **CQRS para las consultas** | No hay evidencia medida de que Cache-Aside y la optimización de lectura sean insuficientes | Se cumple alguno de los disparadores de [ADR-0007](adr/ADR-0007-cqrs-deferred.md) | Cuando haya mediciones | [ADR-0007](adr/ADR-0007-cqrs-deferred.md) |
| DD-03 | **IdP y flujo OAuth 2.0 / OpenID Connect** | Corresponde a la fase de seguridad. No existe un IdP moderno (S9) | Inicio de la fase de seguridad e identidad | Seguridad | R2, S9 |
| DD-04 | **Mecanismo contra el uso de un token robado** (DPoP, mTLS, vinculación de dispositivo, autenticación reforzada, perfil FAPI) | Depende del IdP, de los canales y del modelo de riesgo | Inicio de la fase de seguridad | Seguridad | QA4 |
| DD-05 | **Región cloud y residencia de datos** | Requiere validación legal y de cumplimiento (S8) | Opinión del área legal y de cumplimiento | Infraestructura | [ADR-0010](adr/ADR-0010-data-residency.md) |
| DD-06 | **Mecanismo de propagación del plazo** (`X-Request-Deadline` u otro equivalente) | Depende de los protocolos y del stack de los servicios | Diseño de contenedores y contratos | Contenedores y componentes | [05](05-core-integration-boundary.md) |
| DD-07 | **Tecnología del almacén de caché** | El patrón ya está decidido; la tecnología depende de la infraestructura, de los requisitos de seguridad y de la operación | Selección de la plataforma cloud | Infraestructura | [ADR-0004](adr/ADR-0004-cache-aside.md) |
| DD-08 | **Implementación y persistencia de la máquina de estados** (motor de flujos de trabajo o tabla de estados con un proceso de avance) | El modelo conceptual es suficiente para la Fase 1 | Diseño de componentes del servicio de transferencias | Componentes | [ADR-0005](adr/ADR-0005-transaction-state-machine.md) |
| DD-09 | **RTO, RPO y estrategia de DR** | La institución no ha definido los valores (S10) | Resultado del BIA o definición de la institución | Infraestructura y DR | C3 |
| DD-10 | **Forma de despliegue de las ACL** (módulo del servicio o componente desplegable) y **existencia de un BFF** | Corresponde al nivel de contenedores | Diseño del diagrama de contenedores | Contenedores | [ADR-0001](adr/ADR-0001-domain-acl.md), [ADR-0002](adr/ADR-0002-api-gateway-boundary.md) |
| DD-11 | **Diseño de la base de datos de auditoría** | Corresponde al nivel de contenedores y componentes | Diseño del diagrama de contenedores | Contenedores | R5, QA6 |
| DD-12 | **Canales de notificación** | Corresponde a la fase de notificaciones | Inicio de la fase de notificaciones | Notificaciones | R4 |
| DD-13 | **Stack de frontend** (2 opciones) | Corresponde a la fase de frontend | Inicio de la fase de frontend | Frontend | R6 |
| DD-14 | **Proveedores de KYC y biometría** | Depende de la residencia de datos y de la fase de seguridad | Inicio de la fase de seguridad | Seguridad | R2, R3 |
| DD-15 | **Proveedor cloud (AWS o Azure)** | Depende de la latencia hacia el Core, de la residencia de datos y de las capacidades de la institución | Fase de infraestructura | Infraestructura | C6 |
