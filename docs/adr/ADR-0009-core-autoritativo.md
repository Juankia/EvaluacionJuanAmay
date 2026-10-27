# ADR-0009 — El Core Bancario es autoritativo para la identidad del cliente; la vinculación digital requiere autorización explícita

| Campo | Valor |
|---|---|
| Estado | **Aceptada — enmendada** en la remediación ARG-01 (resuelve ARG01-H05 y, en el onboarding, ARG01-M02). Pendiente de verificación en ARG-01-R2 |
| Fecha | 2026-10-27 (versión 1); 2026-10-27 (versión 2, enmienda) |
| Requisitos | R2, R3; contexto del reto («La información del cliente proviene de dos fuentes […]») |
| Drivers y supuestos | S1, S2, S5, S12, S16; AQ-01, AQ-02, AQ-04; QA11 |
| Relacionados | [ADR-0001](ADR-0001-acl-por-dominio.md), [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [07 — Máquina de estados, sección 5](../07-maquina-de-estados-transaccional.md#5-máquina-de-estados-de-vinculación-digital), [ARG-01](../reviews/ARG-01-revision-arquitectonica.md) |

## Historial

| Versión | Cambio | Origen |
|---|---|---|
| 1 | Core autoritativo; estado durable del onboarding; conciliación por identificador de persona | Fase 1 |
| 2 | Se separan seis etapas: verificación de identidad, identificación del cliente, creación, autorización de la vinculación, activación del acceso y habilitación de productos. La coincidencia del número de identificación **nunca** autoriza el acceso digital. La conciliación de la creación exige **evidencia de que esta solicitud creó al cliente**. Se modela el enrolamiento de clientes preexistentes. Se distingue la indisponibilidad del proveedor de identidad del rechazo | ARG01-H05, ARG01-M02 |

## Contexto

El reto indica que la información del cliente proviene del Core Bancario y de un sistema complementario. El onboarding digital (R3) registra usuarios desde la app móvil con reconocimiento facial. El reto habla de «onboarding de **nuevos usuarios**», que pueden ser **clientes nuevos** o **clientes existentes** que se enrolan en el canal digital (Q-15).

La revisión ARG-01 demostró que la versión 1 permitía concluir `CORE_CREATED` al encontrar un cliente con el mismo número de identificación. Esa regla no distinguía un cliente creado por la solicitud de uno preexistente, y abría un vector de **toma de cuentas** (ARG01-H05).

## Problema

¿Cómo se mantiene al Core como fuente única de la identidad y se evitan los clientes duplicados **sin** que la búsqueda por identificación otorgue acceso digital a cuentas existentes a quien solo superó una verificación de identidad?

## Decisión

### 1. Autoridad del dato (sin cambios respecto de la versión 1)

1. **El Core Bancario es autoritativo** para la identidad del cliente y sus datos transaccionales.
2. **El sistema complementario es autoritativo solo para los datos extendidos** que la institución le asigne (S5). Puede almacenar información pendiente del onboarding, pero **no se convierte en autoritativo** para la identidad.
3. La plataforma accede al Core y al sistema complementario solo mediante sus interfaces y las ACL (S12).

### 2. Seis etapas separadas

| Etapa | Qué establece | Qué **no** establece |
|---|---|---|
| **E1. Verificación de identidad** | Que la persona presente es titular del documento que exhibe (KYC, reconocimiento facial, prueba de vida) | Que es cliente; que tiene derecho a acceder a ningún producto |
| **E2. Identificación del cliente** | Si existe en el Core un cliente con esa identificación (preexistente), si no existe, o si la coincidencia es ambigua | Que la persona que solicita **controla** la relación con ese cliente |
| **E3. Creación del cliente** | Alta en el Core, **solo** si E2 concluyó que no existe | Acceso digital |
| **E4. Autorización de la vinculación** | Que el solicitante está **autorizado a vincular** su acceso digital a ese cliente | Credenciales activas |
| **E5. Activación del acceso digital** | Credenciales y métodos de acceso registrados y activos | Acceso irrestricto a todos los productos |
| **E6. Habilitación del acceso a productos** | Qué productos y operaciones quedan disponibles, posiblemente con **restricciones iniciales** | — |

### 3. Reglas de vinculación

| ID | Regla |
|---|---|
| **VD-01** | **La coincidencia del número de identificación nunca autoriza, por sí sola, la vinculación ni el acceso digital** |
| **VD-02** | **Cliente creado por esta solicitud:** la autorización de la vinculación (E4) se deriva de la propia creación, porque la misma sesión de vinculación verificó la identidad (E1) y originó el alta (E3) |
| **VD-03** | **Cliente preexistente:** la vinculación requiere una **prueba de control** mediante un factor o canal **registrado previamente** ante la institución e **independiente** de la sesión y del dispositivo que solicitan la vinculación. Ejemplos de categoría: contacto registrado previamente en el Core, credencial existente o validación presencial. **El mecanismo concreto se decide en la fase de seguridad (DD-21); el requisito queda aprobado ahora** |
| **VD-04** | **Coincidencia ambigua** (varios registros) o datos inconsistentes entre E1 y el Core: `MANUAL_REVIEW` |
| **VD-05** | **Conciliación de la creación con resultado desconocido:** se concluye que **esta** solicitud creó al cliente **solo con evidencia de creación**. La evidencia es una de estas: (a) la referencia de la solicitud conservada en el Core (G2 de [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md)); o (b) la conjunción de que E2 registró «no existe» antes de invocar, el cliente aparece con fecha de creación dentro de la ventana de la invocación y sus datos coinciden con la solicitud. **Sin esa evidencia: `MANUAL_REVIEW`**. Nunca se concluye la creación por la sola existencia de un cliente con esa identificación |
| **VD-06** | Tras un resultado desconocido de la creación, **nunca se reintenta** la creación sin repetir E2 y aplicar VD-05 |
| **VD-07** | **No enumeración:** antes de completar la prueba de control, las respuestas al solicitante no revelan si su identificación corresponde a un cliente existente |
| **VD-08** | **Vigencia de la verificación:** el resultado de E1 caduca en `VIGENCIA_VERIFICACION_IDENTIDAD`. Una vinculación que no se completa en ese plazo exige repetir E1 |
| **VD-09** | **Indisponibilidad del proveedor de identidad** (sin respuesta, error técnico): estado `IDENTITY_VERIFICATION_UNAVAILABLE`, distinto del rechazo. Nunca se trata como rechazo ni como aprobación |
| **VD-10** | **Restricciones iniciales:** la habilitación de productos (E6) **puede** aplicar restricciones iniciales a un acceso recién vinculado, por ejemplo límites reducidos o periodos de enfriamiento. Su definición corresponde a la fase de seguridad |

### 4. Máquina de estados

Los estados y las transiciones se detallan en [07, sección 5](../07-maquina-de-estados-transaccional.md#5-máquina-de-estados-de-vinculación-digital). Los estados exigidos por la separación de etapas son:

- identidad verificada (`IDENTITY_VERIFIED`);
- cliente identificado (`CUSTOMER_IDENTIFIED`, con el atributo de origen `PRE_EXISTING` o `CREATED_BY_THIS_REQUEST`);
- vinculación pendiente (`LINK_PENDING`);
- vinculación autorizada (`LINK_AUTHORIZED`);
- acceso digital activado (`DIGITAL_ACCESS_ACTIVE`).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Crear el cliente primero en la plataforma o en el sistema complementario | Descartada | Dos fuentes de verdad |
| Repositorio maestro de clientes (MDM) | Fuera de alcance | Iniciativa institucional |
| Conciliación por la sola existencia de la identificación (versión 1) | **Descartada** (ARG01-H05) | Vector de toma de cuentas |
| Rechazar a toda persona que ya es cliente | Descartada | Impide el enrolamiento digital de los clientes existentes, probable caso principal de «nuevos usuarios» (Q-15) |
| Vincular a un cliente preexistente solo con KYC y biometría | Descartada | La verificación de identidad no prueba el control de la relación con la institución; ante un documento sustraído y un ataque biométrico, el impacto sería el acceso a cuentas existentes |
| **Etapas separadas + prueba de control independiente para preexistentes + evidencia de creación** | **Seleccionada** | Separa identidad, relación y acceso |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ningún acceso digital por la sola coincidencia de identificación | Mayor fricción para los clientes existentes (prueba de control) |
| Enrolamiento de clientes existentes modelado | Dependencia de factores previamente registrados de calidad suficiente en el Core (datos de contacto actualizados) |
| Creación conciliada con evidencia | Más casos en `MANUAL_REVIEW` cuando el Core no ofrece G2 |

## Consecuencias

- Las integraciones con el Core (INT-05, INT-06) requieren **búsqueda de cliente por identificación** y, preferiblemente, una **referencia de solicitud** conservada en el alta. Debe validarse (Q-02).
- La fase de seguridad debe definir el mecanismo de prueba de control (DD-21) y las restricciones iniciales (VD-10).
- La experiencia de vinculación comunica los estados intermedios sin revelar la condición de cliente (VD-07).
- Los datos biométricos y de KYC tienen tratamiento propio ([ADR-0010](ADR-0010-residencia-de-datos.md)).

## Riesgos

- **RR-15:** vinculación indebida de clientes preexistentes, que persiste hasta que la fase de seguridad defina e implemente la prueba de control (VD-03).
- Si el Core no permite buscar por identificación ni conservar una referencia de solicitud, aumentan los casos en `MANUAL_REVIEW` (relacionado con RR-04).
- Registros huérfanos en el sistema complementario si la conciliación no opera.
