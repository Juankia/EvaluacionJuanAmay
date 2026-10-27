# ADR-0013 — El proveedor de identidad es un contenedor interno de la plataforma

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones — enmendada en la remediación de ARG-02** (ARG02-H04, M04). El producto queda **provisional**; el flujo OAuth y la vinculación de tokens se deciden en la fase de seguridad (DD-03, DD-04) |
| Fecha | 2026-10-27 |
| Requisitos | R2, R3; C1, C4 |
| Drivers y supuestos | S8, S9; AQ-01; QA4, QA11 |
| Relacionados | [ADR-0009](ADR-0009-core-autoritativo.md), [ADR-0010](ADR-0010-residencia-de-datos.md), [ADR-0014](ADR-0014-api-gateway-y-bff-de-canales.md), [12 — Contexto, sección 4.2](../12-contexto-del-sistema.md#42-proveedor-de-identidad-frontera-por-decidir), [22 — Seguridad entre contenedores](../22-seguridad-entre-contenedores.md) |

## Contexto

Se asume que la institución no tiene un IdP moderno para clientes (S9). En la Fase 2 se decidió que la frontera del IdP (servicio externo o parte de la plataforma) debía resolverse **antes de cerrar el diagrama de contenedores**. La residencia de datos está pendiente de validación legal (ADR-0010). El acceso digital solo puede activarse tras la autorización de la vinculación (ADR-0009).

## Problema

¿El IdP es un sistema externo (por ejemplo, un servicio gestionado por un tercero) o un contenedor operado dentro de la plataforma?

## Decisión

1. El IdP es un **contenedor interno** de la Plataforma de Banca Digital, operado por la institución dentro de su propio entorno cloud, con su propio almacén (ADR-0016).
2. **Solo el servicio de vinculación** puede crear o activar una cuenta digital vinculada a un cliente del Core, mediante la API de administración del IdP con credenciales de servicio. **No existe auto-registro** en el IdP (ADR-0009, VD-01 a VD-03; [22](../22-seguridad-entre-contenedores.md)).
3. **Producto candidato provisional:** un servidor OAuth 2.0 / OpenID Connect autohospedado con soporte para perfiles de seguridad avanzados (por ejemplo, Keycloak). La selección final se toma en la fase de seguridad con criterios explícitos ([21](../21-alternativas-tecnologicas.md)).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| **IdP interno autohospedado** | **Seleccionada** (frontera) | Credenciales y vínculos de identidad bajo control de la institución, compatible con cualquier resultado de ADR-0010. Permite que la vinculación sea el único activador del acceso. Flexibilidad para FAPI y vinculación de tokens |
| IdP gestionado del proveedor cloud dentro de la cuenta o suscripción de la institución | **Alternativa viable** | Menor carga operativa. Debe evaluarse su ubicación de procesamiento, su soporte de FAPI y sus API de administración. Se mantiene como candidato en la fase de seguridad; seguiría siendo un contenedor dentro de la frontera si se opera en el entorno de la institución |
| IdP SaaS de un tercero (fuera del entorno de la institución) | No seleccionada | Credenciales y datos de autenticación procesados por un tercero; condicionado por ADR-0010; dependencia contractual |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Control de la residencia y de las credenciales | Operación, parches, alta disponibilidad y escalado del IdP a cargo de la institución |
| Punto único de control para activar el acceso | El IdP se vuelve un componente crítico de disponibilidad (todo acceso depende de él) |

## Consecuencias

- El IdP deja de ser un sistema externo candidato en el contexto: la vista ampliada ya no lo muestra, y las relaciones REL-10 y REL-11 se reemplazan por relaciones de contenedor ([12](../12-contexto-del-sistema.md)).
- INT-16 deja de ser una integración externa ([13](../13-catalogo-de-integraciones.md)); los cruces de confianza se actualizan en [14](../14-limites-de-confianza.md).
- Requisitos de disponibilidad del IdP en la fase de infraestructura.

## Riesgos

- Disponibilidad y seguridad operativa del IdP (RR-21).

## Enmienda de la remediación de ARG-02: administración y operación del IdP

### Separación de identidades

| Dominio | Quién | Dónde |
|---|---|---|
| Identidad de clientes | Clientes y solicitantes | Dominio (*realm*) de clientes del IdP de la plataforma |
| Identidad del personal | Operadores, auditores, administradores | **IdP corporativo de la institución**, federado. Nunca en el dominio de clientes |
| Identidades de servicio | Contenedores de la plataforma | Credenciales de carga de trabajo, con un alcance por servicio |

La consola de administración del IdP **no se expone a Internet**. Se accede con la identidad del personal, MFA y acceso justo a tiempo.

### Operación de vinculación con mínimo privilegio (ARG02-H04)

1. El servicio de vinculación **no** recibe permisos generales de administración. Solo puede invocar una **operación de vinculación acotada** (extensión o punto de entrada específico del IdP) que permite exclusivamente:
   - crear una cuenta digital vinculada a un identificador de cliente;
   - activarla.
2. Cada invocación exige una **afirmación de vinculación firmada** por el servicio de vinculación con una clave del gestor de claves. Contiene el identificador de la solicitud, el identificador del cliente, el resumen de la evidencia (creación o prueba de control), el origen (`CREATED_BY_THIS_REQUEST` o `PRE_EXISTING`), un nonce y un vencimiento corto. **El IdP verifica la firma, el vencimiento y el nonce.**
3. **Una vinculación por cliente:** el IdP rechaza una nueva vinculación de un cliente que ya tiene una cuenta digital, salvo por un flujo de nueva vinculación explícito con prueba de control (DD-21).
4. **Límites de volumen y alertas:** `MAX_VINCULACIONES_POR_HORA` y alertas por anomalías. Superar el límite **suspende** la operación de vinculación (interruptor automático).
5. **Notificación al cliente** por un canal registrado previamente ante cada nueva vinculación.
6. El servicio de vinculación **no puede**:
   - restablecer credenciales;
   - leer usuarios de forma masiva;
   - modificar vinculaciones existentes;
   - gestionar clientes del IdP.

Con estas reglas, comprometer el servicio de vinculación **no permite una toma masiva silenciosa**: el volumen queda limitado, y toda vinculación es visible y notificada.

### Ciclo de vida de claves, revocación y recuperación (ARG02-M04)

| Aspecto | Requisito |
|---|---|
| Claves de firma | Rotación periódica (`ROTACION_CLAVES_FIRMA`) con solapamiento en el JWKS (clave anterior + nueva). Rotación de emergencia ante compromiso. Claves en el gestor de claves o HSM, si el producto lo permite |
| Caché de JWKS en el Gateway | Caché con vencimiento y actualización ante un identificador de clave desconocido. Si el IdP no está disponible, los tokens vigentes siguen siendo válidos hasta su vencimiento y las renovaciones fallan |
| Revocación | Tokens de acceso de vida corta (`VIDA_ACCESS_TOKEN`); tokens de renovación rotados con detección de reutilización; operaciones de alto riesgo con introspección o autenticación reforzada (QA4) |
| Secretos | Gestor de secretos; rotación automatizada de las credenciales de servicio |
| Acceso privilegiado humano | Acceso justo a tiempo con aprobación de cuatro ojos; grabación y auditoría (A6, ADR-0020) |
| Emergencia (*break-glass*) | Cuentas selladas y monitoreadas; su uso genera una alerta y obliga a rotarlas |
| Recuperación | Restaurar el IdP desde un respaldo puede **resucitar** credenciales, vinculaciones o sesiones ya revocadas. Por eso, tras una restauración se invalidan todas las sesiones y los tokens de renovación, y se reaplican las revocaciones y desvinculaciones registradas en la auditoría |
| Auditoría | Toda acción administrativa es A6 (falla cerrada) |
| Separación de funciones | Quien administra el IdP no aprueba su propio acceso; la configuración de seguridad se cambia por un procedimiento con revisión |
