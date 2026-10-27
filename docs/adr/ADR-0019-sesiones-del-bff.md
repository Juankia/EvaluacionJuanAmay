# ADR-0019 — Sesiones web gestionadas por el BFF con un almacén de sesiones dedicado

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones** (remediación de ARG02-H02). Pendiente de la revisión ARG-03 |
| Fecha | 2026-10-27 |
| Requisitos | R2, R6; C4 |
| Drivers y supuestos | AQ-01, AQ-03; QA4 |
| Relacionados | [ADR-0013](ADR-0013-frontera-del-idp.md), [ADR-0014](ADR-0014-api-gateway-y-bff-de-canales.md), [22 — Seguridad](../22-seguridad-entre-contenedores.md) |

## Contexto

ADR-0014 hace del BFF el **gestor de tokens** de la SPA: los tokens no llegan al navegador. Sin embargo, el documento 16 declaraba el BFF «sin estado» (ARG02-H02), y no se había definido dónde viven los tokens ni el ciclo de vida de la sesión.

## Problema

¿Dónde y cómo se guardan la sesión web y los tokens del usuario, de modo que se puedan revocar, escalar horizontalmente y no exponer credenciales de larga duración al navegador?

## Decisión

Se adopta un **BFF con estado de sesión externo**: el proceso del BFF no guarda estado, pero la sesión vive en un **almacén de sesiones dedicado** (nuevo contenedor).

| Aspecto | Decisión |
|---|---|
| Almacenamiento de tokens | Los tokens de acceso y de renovación de la SPA se guardan **cifrados** en el almacén de sesiones, asociados a un identificador de sesión aleatorio. **El navegador solo recibe el identificador** |
| Cookie | Nombre con el prefijo `__Host-`; atributos `Secure`, `HttpOnly`, `SameSite=Strict`, `Path=/`; sin `Domain`. El retorno del inicio de sesión OIDC usa una cookie transitoria de estado con `SameSite=Lax` y vida corta |
| Expiración | Por inactividad (`SESION_INACTIVIDAD`) y absoluta (`SESION_ABSOLUTA`), por acordar en la fase de seguridad |
| Rotación de tokens de renovación | La aplica el IdP, con detección de reutilización (una reutilización revoca la familia de tokens) |
| Cierre de sesión | El BFF elimina la sesión, revoca el token de renovación en el IdP y cierra la sesión del IdP |
| Revocación | El BFF consume los eventos de seguridad del IdP (cambio de credenciales, bloqueo, revocación) e **invalida todas las sesiones del sujeto**, mediante un índice de sujeto a sesiones |
| Escalado horizontal | Cualquier réplica del BFF atiende cualquier sesión |
| Caída del almacén de sesiones | **Falla cerrada para la web:** se exige volver a iniciar sesión cuando el almacén se recupere. **Nunca** se recurre a poner tokens en el navegador. El canal móvil no se ve afectado (token *bearer* en el dispositivo) |
| CSRF | `SameSite=Strict` + token anti-CSRF ligado a la sesión en toda solicitud que cambia el estado |
| Exposición a XSS | Los tokens no son accesibles desde JavaScript. **Riesgo residual:** un XSS puede actuar dentro de la sesión. Se mitiga con una política de seguridad de contenido estricta y con la autenticación reforzada para las operaciones de alto riesgo (QA4) |
| Fronteras de autenticación | El BFF autentica la sesión y llama a los servicios con el **token de acceso del usuario** (audiencia restringida). Los servicios de dominio autorizan por sí mismos (ADR-0014) |
| Intención pendiente de la SPA (ARG02-L07) | El BFF guarda en la sesión la clave de idempotencia **que la SPA generó** para la intención pendiente, de modo que una recarga de la página la recupera. **El BFF no genera claves** (PT-04) |

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| BFF sin estado, con los tokens cifrados en la cookie | Descartada | Credencial de larga duración en manos del cliente (aunque esté cifrada); revocación del lado del servidor imposible sin una lista de denegación; tamaño de la cookie |
| SPA con tokens en el navegador | Descartada | Exposición a XSS; contradice ADR-0014 |
| **BFF con un almacén de sesiones externo** | **Seleccionada** | Revocación, cierre de sesión completo y escalado horizontal |
| Sesiones en la caché de consultas | Descartada | Mezcla datos de autenticación con datos derivados de negocio; distinto perfil de seguridad y de disponibilidad |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ningún token en el navegador; revocación inmediata | Un contenedor adicional, crítico para la disponibilidad de la web |
| Cierre de sesión e invalidación por eventos | Si el almacén cae, los usuarios web deben volver a iniciar sesión |

## Consecuencias

- Nuevo contenedor: **Almacén de sesiones del BFF** (almacén clave-valor en memoria con alta disponibilidad y cifrado; el producto, con DD-15).
- El BFF consume los eventos de seguridad del broker.

## Riesgos

- Disponibilidad de la web ligada al almacén de sesiones (RR-25).
