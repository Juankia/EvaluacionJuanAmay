# 10 — Registro de decisiones diferidas

| Campo | Valor |
|---|---|
| Fase | 0–2 |
| Estado | Vigente |

## 1. Propósito

Una **decisión diferida** es una decisión que se pospone de forma intencional porque aún no hay información suficiente o porque corresponde a una fase posterior. Diferir no es omitir: cada decisión diferida tiene un **disparador** (la condición que obliga a tomarla) y una fase prevista.

Esto evita seleccionar tecnología antes de entender el problema. En el registro no hay «Kafka por escalabilidad», «CQRS por ser banca» ni «Redis por ser caché».

## 2. Registro

| ID | Decisión | Por qué se difiere | Disparador | Fase prevista | Referencia |
|---|---|---|---|---|---|
| DD-01 | **Tecnología del bus de eventos** | No se ha confirmado qué flujos asíncronos son requisitos arquitectónicos | Se confirma como requisito la invalidación de caché por eventos, la auditoría asíncrona, las notificaciones, la analítica u otros flujos asíncronos | Infraestructura y plataforma | [ADR-0008](adr/ADR-0008-invalidacion-por-eventos.md) |
| DD-02 | **CQRS para las consultas** | No hay evidencia medida de que Cache-Aside y la optimización de lectura sean insuficientes | Se cumple alguno de los disparadores de [ADR-0007](adr/ADR-0007-cqrs-diferido.md) | Cuando haya mediciones | [ADR-0007](adr/ADR-0007-cqrs-diferido.md) |
| DD-03 | **IdP y flujo OAuth 2.0 / OpenID Connect**, incluida la **frontera del IdP** (servicio externo o parte de la plataforma) | Corresponde a la fase de seguridad. No existe un IdP moderno (S9) | **La frontera del IdP debe decidirse antes de cerrar el diagrama de contenedores (Fase 3)**; el flujo y el producto, en la fase de seguridad | Fase 3 (frontera); Seguridad (flujo y producto) | R2, S9, Q-03, [12, sección 4.2](12-contexto-del-sistema.md#42-proveedor-de-identidad-frontera-por-decidir) |
| DD-04 | **Mecanismo contra el uso de un token robado** (DPoP, mTLS, vinculación de dispositivo, autenticación reforzada, perfil FAPI) | Depende del IdP, de los canales y del modelo de riesgo | Inicio de la fase de seguridad | Seguridad | QA4 |
| DD-05 | **Región cloud y residencia de datos** | Requiere validación legal y de cumplimiento (S8) | Opinión del área legal y de cumplimiento | Infraestructura | [ADR-0010](adr/ADR-0010-residencia-de-datos.md) |
| DD-06 | **Mecanismo de propagación del plazo** (`X-Request-Deadline` u otro equivalente) | Depende de los protocolos y del stack de los servicios | Diseño de contenedores y contratos | Contenedores y componentes | [05](05-frontera-de-integracion-core.md) |
| DD-07 | **Tecnología del almacén de caché** | El patrón ya está decidido; la tecnología depende de la infraestructura, de los requisitos de seguridad y de la operación | Selección de la plataforma cloud | Infraestructura | [ADR-0004](adr/ADR-0004-cache-aside.md) |
| DD-08 | **Implementación y persistencia de la máquina de estados** (motor de flujos de trabajo o tabla de estados con un proceso de avance) | El modelo conceptual es suficiente para la Fase 1 | Diseño de componentes del servicio de transferencias | Componentes | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) |
| DD-09 | **RTO, RPO y estrategia de DR** | La institución no ha definido los valores (S10) | Resultado del BIA o definición de la institución | Infraestructura y DR | C3 |
| DD-10 | **Forma de despliegue de las ACL** (módulo del servicio o componente desplegable) y **existencia de un BFF** | Corresponde al nivel de contenedores | Diseño del diagrama de contenedores | Contenedores | [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0002](adr/ADR-0002-frontera-api-gateway.md) |
| DD-11 | **Diseño de la base de datos de auditoría** | Corresponde al nivel de contenedores y componentes | Diseño del diagrama de contenedores | Contenedores | R5, QA6 |
| DD-12 | **Canales de notificación** | Corresponde a la fase de notificaciones | Inicio de la fase de notificaciones | Notificaciones | R4 |
| DD-13 | **Stack de frontend** (2 opciones) | Corresponde a la fase de frontend | Inicio de la fase de frontend | Frontend | R6 |
| DD-14 | **Proveedores de KYC y biometría** | Depende de la residencia de datos y de la fase de seguridad | Inicio de la fase de seguridad | Seguridad | R2, R3 |
| DD-15 | **Proveedor cloud (AWS o Azure)** | Depende de la latencia hacia el Core, de la residencia de datos y de las capacidades de la institución | Fase de infraestructura | Infraestructura | C6 |
| DD-16 | **Topología de integración de las transferencias interbancarias** (directa, mediante la infraestructura de pagos existente o mediante un intermediario autorizado) | No hay evidencia sobre el mecanismo actual de la institución | Respuesta a Q-01 | Fase 3 (si es posible) o fase de integración | S13, REL-05, INT-09, INT-10 |
| DD-17 | **Uso y mecanismo de acceso a una Fuente Oficial de Identidad** | Requiere autorización legal y validación técnica | Respuesta a Q-07 | Seguridad e identidad | REL-12, INT-17 |
| DD-18 | **Interacción de los actores internos** (operador de soporte, auditoría): consola de la plataforma o sistemas administrativos existentes | El reto no menciona actores internos | Respuesta a Q-08 | Fase 3. **La resolución operativa ya tiene un rol responsable, plazo y cuatro ojos (ADR-0005, sección 6); queda pendiente el medio y el área concreta (ARG01-H03)** | REL-14, REL-15 |
| DD-19 | **Stack tecnológico del backend** (lenguaje y framework) | La tecnología se selecciona después de las decisiones de contenedores, como cualquier otra elección tecnológica. Existe un perfil de estándares condicional para Java/Quarkus que no anticipa la decisión | Diseño de contenedores cerrado (Fase 3) y antes de cualquier implementación | Fase 3 o fase de selección tecnológica | [Estándares de ingeniería, sección 6](00-estandares-de-ingenieria.md#6-perfil-javaquarkus-condicional-a-dd-19) |
| DD-20 | **Mecanismo dinámico de cuotas del Core** (M2: cuotas coordinadas o arrendadas entre réplicas) | La asignación estática (M1) garantiza el presupuesto; M2 solo optimiza el uso | Subutilización medida del Core con pocas réplicas que produce rechazos relevantes | Fase de infraestructura o posterior a mediciones | [ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md) |
| DD-21 | **Mecanismo de prueba de control para vincular clientes preexistentes** y restricciones iniciales del acceso recién vinculado | El requisito está aprobado (ADR-0009, VD-03 y VD-10); el mecanismo pertenece a la fase de seguridad | Inicio de la fase de seguridad; respuesta a Q-15 | Seguridad | [ADR-0009](adr/ADR-0009-core-autoritativo.md), QA11 |
| DD-22 | **Adopción de capacidades del Core para evidencia determinista**: consulta por referencia con consistencia fuerte y cierre de la referencia, expiración de solicitudes, retención y captura en dos fases | Ninguna se asume; dependen de lo que ofrezca el Core | Respuesta a Q-17 | Fase 3 o de integración | [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) v3 §3 y §4.3; RR-17, RR-18 |
