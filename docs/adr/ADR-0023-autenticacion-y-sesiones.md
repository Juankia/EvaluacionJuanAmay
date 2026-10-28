# ADR-0023 — Autenticación OIDC con Authorization Code + PKCE, BFF para la web y autenticación reforzada

| Campo | Valor |
|---|---|
| Estado | **Propuesta** (fase de seguridad). Parámetros (vidas de tokens, vencimientos de sesión) por acordar. Resuelve en parte DD-03; DD-04 sigue abierta |
| Fecha | 2026-10-27 |
| Requisitos | R2, R3, R6; C4 |
| Drivers y supuestos | AQ-01, AQ-04; QA4; S9 |
| Relacionados | [ADR-0013](ADR-0013-frontera-del-idp.md), [ADR-0014](ADR-0014-api-gateway-y-bff-de-canales.md), [ADR-0019](ADR-0019-sesiones-del-bff.md), [29](../29-seguridad-identidad-y-cumplimiento.md) |

## Contexto

R2 exige OAuth 2.0 y pide recomendar el flujo más adecuado. ADR-0013 decidió que el IdP es interno y ADR-0019 que la sesión web vive en el BFF. Quedaba diferido el flujo concreto, junto con MFA, la autenticación reforzada y la recuperación (DD-03).

## Problema

¿Qué flujo y qué manejo de tokens usan la SPA y la app móvil para minimizar el robo de tokens y la toma de cuentas, sin romper las propiedades aprobadas en ADR-0019?

## Decisión

| Aspecto | Decisión |
|---|---|
| Flujo | **Authorization Code + PKCE** para todos los clientes. Se prohíben el flujo implícito y ROPC |
| Web | El **BFF** es el cliente confidencial. La SPA solo tiene la cookie de sesión (ADR-0019, sin cambios) |
| Móvil | Cliente público con PKCE y navegador del sistema. Tokens en el almacenamiento seguro del dispositivo |
| Tokens | Acceso de vida corta; renovación rotada con detección de reutilización |
| MFA | En el primer acceso desde un dispositivo nuevo y ante señales de riesgo |
| Autenticación reforzada | En las operaciones sensibles, mediante `acr`/`max_age`. El servicio de dominio verifica el `acr` |
| Biometría local | Solo desbloquea una credencial del dispositivo registrada en el IdP. **No** reemplaza la autorización del servidor |
| Recuperación | Nueva verificación de identidad, enfriamiento, notificación y revocación de sesiones |

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| SPA como cliente público con tokens en el navegador | Descartada | Expone los tokens a XSS; contradice ADR-0019 |
| ROPC (usuario y contraseña directos a la app) | Descartada | Desaconsejado; impide MFA y federación |
| DPoP o mTLS obligatorios desde el inicio | Diferida (DD-04) | Depende del producto del IdP y del soporte en los canales |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Tokens fuera del navegador; revocación central | El BFF y el almacén de sesiones pasan a ser críticos para la web |
| Fricción proporcional al riesgo (reforzada solo en lo sensible) | Complejidad del motor de riesgo y de la gestión de `acr` |

## Consecuencias

- El Gateway y los servicios validan `acr` en las operaciones sensibles.
- La fase de seguridad fija los parámetros y el producto del IdP con estos requisitos.

## Riesgos

- El SMS como factor es vulnerable al SIM swap: no se usa como factor único en operaciones de alto riesgo.
- La recuperación de cuenta es el vector principal de toma de cuentas; requiere controles operativos además de los técnicos.
