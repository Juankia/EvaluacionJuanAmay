# 00 — Estándares de ingeniería

| Campo | Valor |
|---|---|
| Documento | Estándares de ingeniería, codificación y nomenclatura del código |
| Alcance | Toda implementación futura de la Plataforma de Banca Digital |
| Estado | Vigente como norma. **No inicia ninguna implementación ni selecciona la tecnología** |
| Fecha de emisión | 2026-10-27 |
| Documentos relacionados | [Política de documentación e idioma](00-politica-de-documentacion.md), [10 — Decisiones diferidas](10-decisiones-diferidas.md) (DD-19) |

---

## 1. Propósito y alcance

Este documento fija los estándares que debe cumplir el código de la plataforma cuando se implemente. Los criterios son compatibles con los de evaluación técnica de las instituciones financieras: mantenibilidad, seguridad, trazabilidad y verificabilidad automática.

**Relación con la política de documentación (sin duplicarla):**

| Tema | Documento normativo |
|---|---|
| Idioma de la documentación, de los ADR y de los diagramas C4 | [Política de documentación](00-politica-de-documentacion.md), secciones 3 y 7 |
| Mensajes de commit (español, estructura Conventional Commits) | [Política de documentación](00-politica-de-documentacion.md), sección 6 |
| Nombres de archivos de documentación | [Política de documentación](00-politica-de-documentacion.md), sección 9 |
| Idioma y nomenclatura **del código**, glosario de dominio, estándares de diseño, pruebas y calidad automatizada | **Este documento** |

## 2. Aplicabilidad

| Bloque | Aplica | Condición |
|---|---|---|
| Secciones 3 a 5: idioma del código, glosario, principios de diseño | **Siempre**, con cualquier tecnología | — |
| Sección 6: perfil Java/Quarkus | **Condicional** | Solo si la decisión diferida **DD-19** (stack tecnológico del backend) selecciona Java y Quarkus. Si se selecciona otro stack, se redacta un perfil equivalente con los mismos principios |
| Secciones 7 y 8: pruebas y calidad en CI | **Siempre**; las herramientas concretas se eligen con el stack | — |

> **Importante:** documentar el perfil Java/Quarkus no anticipa la decisión de tecnología. La selección del stack del backend sigue diferida (DD-19) y se justificará con los atributos de calidad y las restricciones de la institución, como cualquier otra decisión tecnológica.

## 3. Idioma y nomenclatura del código

### 3.1 Reglas

| Elemento | Idioma | Regla |
|---|---|---|
| Clases, interfaces, métodos, variables, DTOs, enumeraciones | Inglés | Nombres descriptivos y consistentes con el glosario de dominio (sección 4) |
| API: rutas, campos de contratos, códigos de error | Inglés | Ejemplo: `POST /v1/transfers`, `INSUFFICIENT_FUNDS` |
| Eventos de dominio | Inglés, en pasado | Ejemplo: `TransferCompleted`, `AccountBalanceChanged` |
| Estados de las máquinas de estado | Inglés, `UPPER_SNAKE_CASE` | Los definidos en [07](07-maquina-de-estados-transaccional.md) (`CORE_IN_PROGRESS`, `CORE_UNKNOWN`, `REVERSAL_PENDING`, …) |
| Esquemas de base de datos **propios** de la plataforma | Inglés, `snake_case` | Ejemplo: `transfer_operation`, `idempotency_key` |
| Identificadores de bases de datos **existentes** (Core, Sistema Complementario) | **Nomenclatura original** | No se renombran ni se «corrigen». La traducción al vocabulario del dominio ocurre **solo** en la ACL correspondiente ([ADR-0001](adr/ADR-0001-acl-por-dominio.md)) |
| Comentarios de código | Inglés | Explican el **porqué**, no el qué |
| Mensajes de registro (*logs*) | Inglés | Estructurados y sin datos sensibles (sección 5.6) |
| Mensajes visibles para el cliente | Español (es-EC) | Se gestionan como recursos de internacionalización, no como texto fijo en el código |

### 3.2 Convenciones generales

- Un concepto, un nombre: no se usan sinónimos para el mismo concepto. Por ejemplo, no conviven `Customer` y `Client`.
- Nada de abreviaturas ambiguas ni de prefijos húngaros.
- Nada de nombres en «spanglish» (`TransferenciaService`, `getCuenta`).
- Los nombres heredados del Core **no** aparecen fuera de la ACL ([S6](02-supuestos.md)).

## 4. Glosario de dominio

El glosario establece el **lenguaje ubicuo** (*ubiquitous language*) de la plataforma: une el término de negocio en español, que se usa en la documentación y con la institución, con el término en inglés que se usa en el código. El significado de negocio de los términos financieros está en la [política de documentación, sección 4](00-politica-de-documentacion.md#4-terminología-financiera-ecuatoriana).

| Término de negocio (es-EC) | Término en código | Notas |
|---|---|---|
| Cliente / socio | `Customer` | Un solo término en código para ambos contextos (S15). El tipo de institución no cambia el modelo |
| Solicitante de vinculación | `Applicant` | Persona que aún no es cliente ([ADR-0009](adr/ADR-0009-core-autoritativo.md)) |
| Vinculación digital | `Onboarding` | Proceso con estado durable ([07](07-maquina-de-estados-transaccional.md)) |
| Cuenta | `Account` | — |
| Cuenta de ahorros | `SavingsAccount` | Tipo de producto de captación |
| Producto | `Product` | — |
| Movimiento | `AccountMovement` | Se evita el término genérico `Transaction`, que se confunde con la transacción técnica de base de datos |
| Saldo disponible / saldo contable | `AvailableBalance` / `LedgerBalance` | Semántica por validar con el Core (INT-02) |
| Transferencia propia | `OwnAccountTransfer` | Entre cuentas del mismo cliente en la institución |
| Transferencia interbancaria | `InterbankTransfer` | — |
| Beneficiario | `Beneficiary` | — |
| Monto | `Amount` | Objeto de valor con moneda (sección 5.4) |
| Captaciones | `Deposits` | Término de cartera pasiva |
| Colocaciones | `Loans` | Término de cartera activa |
| Conciliación | `Reconciliation` | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) |
| Clave de idempotencia | `IdempotencyKey` | Generada en el dominio ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md)) |
| Identificador de correlación | `CorrelationId` | Provisto por el Integration Platform SDK ([ADR-0003](adr/ADR-0003-integration-platform-sdk.md)) |
| Core Bancario | `CoreBanking` | Prefijo de los adaptadores de la ACL (por ejemplo, `CoreBankingMovementAdapter`) |
| Sistema Complementario de Clientes | `CustomerExtendedData` | Nombre del puerto o adaptador, no del producto |
| Auditoría | `Audit` | — |
| Notificación | `Notification` | — |

El glosario se amplía en cada fase. Un término nuevo se agrega **antes** de usarse en el código.

## 5. Principios de diseño (independientes de la tecnología)

### 5.1 SOLID y separación de responsabilidades

- **Responsabilidad única:** cada clase tiene un único motivo de cambio. Ejemplo: la traducción del modelo del Core vive en la ACL, no en el servicio de aplicación.
- **Abierto/cerrado e inversión de dependencias:** el dominio y la aplicación dependen de **puertos** (interfaces) que implementan los adaptadores. Esto permite reemplazar el Core o agregar otra implementación (S7) sin modificar el dominio.
- **Segregación de interfaces:** puertos pequeños por capacidad (`MovementQueryPort`, `TransferExecutionPort`), no un «CoreBankingService» con todas las operaciones.

### 5.2 Clean Architecture

| Capa | Contenido | Puede depender de | Relación con las decisiones vigentes |
|---|---|---|---|
| **Dominio** | Entidades, objetos de valor, reglas de negocio, reglas de transición de estados | Nada externo | Reglas de la máquina de estados ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md)) |
| **Aplicación** | Casos de uso, orquestación de negocio, puertos de entrada y salida, límites transaccionales | Dominio | La orquestación vive aquí, no en el Gateway ([ADR-0002](adr/ADR-0002-frontera-api-gateway.md)) |
| **Adaptadores de entrada** | Recursos REST, DTOs, validación, mapeo de errores | Aplicación | Contratos de API uniformes (sección 6.2) |
| **Adaptadores de salida** | ACL hacia el Core y el Sistema Complementario, persistencia, caché, clientes de terceros | Aplicación (implementan sus puertos) | ACL por dominio ([ADR-0001](adr/ADR-0001-acl-por-dominio.md)), Cache-Aside ([ADR-0004](adr/ADR-0004-cache-aside.md)) |
| **Infraestructura transversal** | Integration Platform SDK, configuración, observabilidad | — | [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) |

**Regla de dependencia:** las dependencias apuntan hacia el dominio. **El modelo de dominio no contiene dependencias de infraestructura:** ni anotaciones de persistencia, ni clientes HTTP, ni tipos del Core, ni el Integration Platform SDK. Las dependencias de infraestructura empiezan en la capa de adaptadores.

### 5.3 Errores

- **Errores de dominio explícitos** y con semántica de negocio: `INSUFFICIENT_FUNDS`, `ACCOUNT_BLOCKED`, `OUTCOME_UNKNOWN`. Se distinguen de los errores técnicos.
- **Un resultado desconocido no es un error genérico.** `OUTCOME_UNKNOWN` debe llegar al contrato de la API como un estado distinto, «en verificación», y no como una falla definitiva ni como un error interno ([05, sección 6](05-frontera-de-integracion-core.md#6-regla-crítica-un-timeout-no-equivale-a-una-falla)).
- Nunca se exponen trazas de pila, mensajes internos ni códigos propietarios del Core (S6).

### 5.4 Dinero y tiempo

- **Montos:** decimal exacto (nunca tipos de coma flotante), siempre acompañados de la moneda, con reglas de redondeo explícitas. Se modelan como objeto de valor `Money` o `Amount`.
- **Instantes:** UTC y formato ISO 8601 en contratos y persistencia. La conversión a la hora local de Ecuador ocurre solo en la presentación.
- **Fechas contables:** se distinguen del instante técnico. La fecha contable la determina el Core.

### 5.5 Idempotencia, reintentos y consistencia

- Las operaciones financieras aplican [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) y [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) **en el código**:
  - el estado se persiste antes de invocar;
  - la unicidad de la clave de idempotencia se garantiza con una restricción en la base de datos, no con un patrón «consultar y luego insertar»;
  - **ningún reintento automático en escrituras críticas**.
- Las políticas de reintento, timeout y Circuit Breaker se declaran explícitamente por operación en cada ACL. **Se prohíben las políticas de reintento globales o implícitas** que puedan aplicarse a una escritura ([ADR-0003](adr/ADR-0003-integration-platform-sdk.md)).
- Ninguna decisión transaccional usa datos de caché ([ADR-0004](adr/ADR-0004-cache-aside.md)).

### 5.6 Registro, trazabilidad y datos sensibles

- Registros estructurados con el identificador de correlación y el contexto de trazas propagados por el SDK ([ADR-0003](adr/ADR-0003-integration-platform-sdk.md)).
- **Nunca se registran** credenciales, tokens, OTP, datos biométricos, imágenes de documentos ni números completos de cuenta o de identificación. Cuando se requiera correlación, se enmascaran.
- El **registro técnico** (*logging*) no sustituye a la **auditoría de negocio** (R5), que tiene su propio diseño, retención e integridad.

### 5.7 Configuración y secretos

- La configuración está externalizada por entorno; no hay valores dependientes del entorno en el código.
- **Ningún secreto en el repositorio**, ni siquiera de entornos de desarrollo. La gestión de secretos se define en la fase de seguridad.

## 6. Perfil Java/Quarkus (condicional a DD-19)

> Este perfil aplica **solo si** DD-19 selecciona Java y Quarkus.

### 6.1 Inyección de dependencias

- Se usa **inyección por constructor con CDI**. Las dependencias son explícitas y la clase se puede probar sin contenedor.
- No se usa inyección por campo en el código de producción, salvo excepción justificada y documentada.
- El alcance de los beans es explícito (`@ApplicationScoped` por defecto en los servicios sin estado).
- Las clases del **dominio no son beans CDI** ni dependen de anotaciones del framework (sección 5.2).

### 6.2 Manejo centralizado de excepciones y contrato de error

- El manejo de excepciones se centraliza con **`ExceptionMapper`** (o el mecanismo equivalente de la pila REST elegida). Los recursos REST no capturan excepciones para construir respuestas de error a mano.
- Existe un **contrato de error uniforme** para todas las API. Incluye un código estable en inglés, un mensaje seguro, el identificador de correlación y, si corresponde, los detalles de validación. El formato concreto (por ejemplo, RFC 9457 *Problem Details*) se decide en el diseño de contratos de API.
- Cada error de dominio tiene una correspondencia documentada con un estado HTTP. `OUTCOME_UNKNOWN` tiene un tratamiento explícito (sección 5.3).
- Toda excepción no controlada se traduce a un error genérico sin detalles internos y se registra con su correlación.

### 6.3 DTOs y contratos

- Hay **DTOs independientes** para las solicitudes y para las respuestas, específicos de cada operación. No se usa un mismo DTO para entrada y salida.
- **Las entidades JPA nunca se exponen** a través de las API ni se serializan directamente.
- El mapeo entre DTO, dominio y entidad es explícito y está probado. Los tipos del Core no llegan a los DTOs públicos (S6).
- Los contratos de API se versionan (por ejemplo, `/v1/`) y se documentan con una especificación OpenAPI generada o mantenida junto al código.

### 6.4 Persistencia (JPA/Hibernate) y límites transaccionales

- **Los límites transaccionales se ubican en la capa de aplicación** (casos de uso), no en los recursos REST ni en los repositorios.
- **No se invoca un sistema externo dentro de una transacción de base de datos abierta.** La transición de estado se confirma (*commit*) **antes** de llamar al Core ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), regla «se persiste antes de invocar»). El resultado se registra en una transacción posterior.
- Las transiciones de estado concurrentes se protegen con **bloqueo optimista** (control de versión) para evitar avances duplicados.
- La unicidad de la clave de idempotencia se garantiza con una restricción única en la base de datos ([ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md)).
- Carga de asociaciones explícita y consciente: se evitan las consultas N+1 y no se usa el patrón *Open Session in View*.
- Las entidades JPA viven en el adaptador de persistencia, separadas del modelo de dominio.
- El esquema se versiona con migraciones controladas, no con la generación automática del esquema en entornos productivos.

### 6.5 Validación

- **Bean Validation** en todos los DTOs de entrada (`@Valid` en los recursos), con restricciones explícitas: obligatoriedad, formato, rangos y longitudes.
- La validación sintáctica en el borde **no sustituye** a la validación de las reglas de negocio en el dominio.
- **Toda entrada se valida en el servidor**, aunque el canal ya la haya validado: el canal se ejecuta en una zona no confiable (TB-1, [14](14-limites-de-confianza.md)).

### 6.6 Integraciones

- Los clientes hacia el Core y hacia terceros residen solo en los adaptadores de salida (ACL) y usan las primitivas del Integration Platform SDK.
- Los timeouts, reintentos, Circuit Breaker y Bulkhead se configuran **por operación** y quedan visibles en la configuración de cada ACL ([05, sección 5](05-frontera-de-integracion-core.md#5-frontera-de-resiliencia)).

## 7. Pruebas

| Tipo | Objetivo | Obligatoriedad | Relación |
|---|---|---|---|
| **Unitarias** | Dominio y casos de uso aislados, sin contenedor ni red | Obligatorias para el dominio y la aplicación | — |
| **De la máquina de estados** | Transiciones válidas y **prohibidas**; invariantes INV-T1 a INV-T8 e INV-O1 a INV-O6; estados `*_UNKNOWN` y `REQUIRES_RECONCILIATION`; recuperación tras reinicio | Obligatorias | RR-08, QA5 |
| **De idempotencia y resultado desconocido** | Reenvío con la misma clave, misma clave con otro contenido, solicitudes concurrentes, timeout de lectura, respuesta perdida | Obligatorias para las operaciones críticas | QA3, QA5, QA9, ADR-0006 |
| **De integración** | Adaptadores contra dependencias reales o emuladas (persistencia, caché) | Obligatorias para los adaptadores de salida | — |
| **De contrato** | Cada ACL frente al contrato del Core o del tercero; API públicas frente a su especificación | Obligatorias para cada ACL y cada API pública | QA7, ADR-0001, RR-03 |
| **De arquitectura** | Regla de dependencia de la sección 5.2; el SDK sin tipos de dominio; los tipos del Core solo en la ACL | Obligatorias en CI | ADR-0003, RR-02, S6 |
| **De resiliencia** | Comportamiento con Circuit Breaker abierto, timeouts y degradación de lecturas | Recomendadas; obligatorias antes de producción | QA1, QA3 |

Las pruebas no usan datos reales de clientes. Los datos de prueba son sintéticos.

## 8. Calidad automatizada en CI

Toda integración a la rama principal pasa por un *pipeline* de CI que, como mínimo, ejecuta:

| Control | Propósito |
|---|---|
| Compilación y pruebas (secciones 7) | Ningún cambio se integra con pruebas fallidas |
| Análisis estático de código | Defectos, complejidad, convenciones |
| Análisis de seguridad del código (SAST) | Vulnerabilidades en el código propio |
| Análisis de dependencias (SCA) | Vulnerabilidades y licencias de las bibliotecas de terceros |
| Detección de secretos | Ningún secreto llega al repositorio (sección 5.7) |
| Pruebas de arquitectura | Cumplimiento de las reglas de dependencia (sección 7) |
| Validación de la documentación de arquitectura | Validación del DSL de Structurizr y de los enlaces de la documentación (ver [README](../README.md#herramientas)) |

Las herramientas concretas y los umbrales de calidad (cobertura, severidad que bloquea la integración) se definen al seleccionar el stack (DD-19) y la plataforma de CI. Los umbrales deben ser explícitos y versionados.

## 9. Trazabilidad con las decisiones de arquitectura

| Estándar | Decisiones o atributos que lo motivan |
|---|---|
| Traducción de nombres heredados solo en la ACL (3.1) | ADR-0001, S6 |
| Puertos por capacidad y regla de dependencia (5.1, 5.2) | ADR-0001, S7, QA7 |
| `OUTCOME_UNKNOWN` como estado explícito (5.3, 6.2) | ADR-0005, ADR-0006, QA5 |
| Políticas de reintento explícitas por operación (5.5, 6.6) | ADR-0003, ADR-0005 |
| Sin invocación externa dentro de una transacción abierta (6.4) | ADR-0005, ADR-0006 |
| Sin datos sensibles en los registros (5.6) | AQ-01, ADR-0010, RR-11 |
| Validación en el servidor (6.5) | Límite de confianza X-1 ([14](14-limites-de-confianza.md)) |
| Pruebas de contrato y de arquitectura (7) | QA7, RR-02, RR-03 |

## 10. Excepciones y gobierno

- Toda excepción a estos estándares se documenta en el código o en el *pull request* con su justificación. Si tiene impacto arquitectónico, se registra en un ADR.
- Este documento se revisa al cerrar la decisión DD-19 y al inicio de la primera fase de implementación.
