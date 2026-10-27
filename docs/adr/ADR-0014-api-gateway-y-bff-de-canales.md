# ADR-0014 — API Gateway de borde y un BFF compartido de canales

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones** (Fase 3; complementa ADR-0002 y resuelve la parte BFF de DD-10) |
| Fecha | 2026-10-27 |
| Requisitos | R1, R2, R6; C4 |
| Drivers y supuestos | AQ-01, AQ-08, AQ-09; QA4, M03 |
| Relacionados | [ADR-0002](ADR-0002-frontera-api-gateway.md), [ADR-0013](ADR-0013-frontera-del-idp.md), [ADR-0006](ADR-0006-idempotencia-y-conciliacion.md), [16 — Arquitectura de contenedores](../16-arquitectura-de-contenedores.md) |

## Contexto

ADR-0002 interpreta el «API Gateway que orqueste al menos 3 servicios» (R1) como una frontera de tráfico y de fachada, no como un orquestador de negocio. Faltaba decidir si existe un BFF (*Backend for Frontend*) y cuántos.

Para las aplicaciones de navegador, la práctica recomendada para OAuth 2.0 favorece un **backend que gestione los tokens** y no los exponga al JavaScript del navegador.

## Problema

¿Cómo se reparten las responsabilidades entre el Gateway, un eventual BFF y los servicios de dominio, sin poner invariantes financieros en el borde?

## Decisión

1. **API Gateway** (contenedor de borde): enrutamiento, validación de tokens en el borde, limitación de tasa, correlación de solicitudes, políticas de seguridad de borde e integración con la observabilidad. **No contiene lógica de negocio, ni estado financiero, ni máquinas de estado, ni reintentos de escrituras financieras.**
2. **BFF de canales digitales, uno solo, compartido por la web y el móvil**, con rutas específicas por canal. Responsabilidades:
   - **gestor de tokens de la SPA**, como cliente confidencial OAuth: la SPA usa una sesión con cookie segura y nunca maneja tokens;
   - composición de **lecturas**; por ejemplo, combina los movimientos del servicio de consultas con las operaciones en curso del servicio de transferencias para mostrar «pendiente» o «en verificación» (M03);
   - forma de las respuestas por canal.
3. **El BFF no posee invariantes financieros.** No genera, altera ni reutiliza claves de idempotencia. Propaga **sin cambios** la clave del canal (ADR-0006 IC-02) y nunca reintenta un inicio de transferencia por su cuenta.
4. **Servicios de dominio:** orquestación de negocio, autorización de negocio (incluida la propiedad de cuentas), estado financiero y seguridad transaccional.

**Cómo se cumple R1:** el Gateway es la puerta de entrada única que **expone y enruta** (a través del BFF) los tres servicios exigidos (consulta de datos del cliente y de movimientos en el servicio de consultas; transferencias en el servicio de transferencias) y servicios adicionales justificados (vinculación). La «orquestación» de las llamadas externas según el tipo de transferencia ocurre en el servicio de transferencias, que es donde puede hacerse con seguridad (ADR-0005).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Gateway sin BFF | No seleccionada | La SPA tendría que manejar tokens en el navegador; la composición recaería en los clientes (llamadas en cascada desde el móvil) y los canales quedarían acoplados a las API de dominio |
| **BFF compartido** | **Seleccionada** | Ambos canales ofrecen funciones casi idénticas; un único despliegue con rutas por canal reduce el costo operativo. Resuelve el manejo de tokens de la SPA |
| BFF separados para web y móvil | No seleccionada **por ahora** | Más independencia, pero el doble de operación. **Disparador para separarlos:** divergencia sostenida en el ritmo de liberación o en los mecanismos de seguridad por canal, o un equipo distinto por canal |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Sin tokens en el navegador | El BFF entra en la ruta crítica de la web |
| Canales desacoplados de las API de dominio | Riesgo de que el BFF acumule lógica de negocio (mitigado por la regla 3 y por pruebas de arquitectura) |
| Un solo despliegue para dos canales | Dos mecanismos de autenticación (sesión web y token móvil) en el mismo contenedor |

## Consecuencias

- El producto de Gateway se selecciona en la fase de infraestructura (DD-15).
- El BFF es uno de los puntos donde se exige la propagación sin cambios de `Idempotency-Key` (condición PT-04, [23](../23-riesgos-y-condiciones-fase-3.md)).

## Riesgos

- Acumulación de lógica en el BFF.
- Dependencia de disponibilidad de un contenedor adicional.
