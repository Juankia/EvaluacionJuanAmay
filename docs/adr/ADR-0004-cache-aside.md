# ADR-0004 — Cache-Aside con estrategia de frescura por categoría de dato

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (Fase 1). La tecnología del almacén se difiere (DD-07) |
| Fecha | 2026-10-27 |
| Requisitos | R5, C4 |
| Drivers y supuestos | S2, S4; AQ-06; QA2, QA3, QA8 |
| Relacionados | [ADR-0008](ADR-0008-invalidacion-por-eventos.md), [ADR-0007](ADR-0007-cqrs-diferido.md), [06 — Estrategia de caché](../06-estrategia-de-cache.md) |

## Contexto

El reto exige «un mecanismo de caché para clientes frecuentes basado en patrones de diseño» (R5). Se asume que el Core tiene capacidad limitada (S4) y que es autoritativo (S2). Hay datos financieros cuya desactualización tiene consecuencias distintas según su tipo.

## Problema

¿Qué patrón de caché se usa y cómo se evita que la caché comprometa la consistencia de los datos financieros, sin asignar TTL arbitrarios?

## Decisión

1. Se adopta **Cache-Aside**: el servicio de dominio consulta la caché y, ante un fallo, obtiene el dato mediante la ACL y lo almacena. Las operaciones propias que cambian el estado invalidan las entradas afectadas.
2. La **cacheabilidad depende del dato**. Se define una [matriz de cacheabilidad](../06-estrategia-de-cache.md#3-matriz-de-cacheabilidad) con una estrategia de frescura por categoría.
3. **El TTL es un mecanismo de seguridad**, no automáticamente el mecanismo principal de consistencia. Los valores de TTL se acuerdan con negocio y con riesgo; no se fijan de forma arbitraria.
4. **Ninguna decisión transaccional usa datos de caché.** La validación de fondos y el estado de la cuenta para una operación siempre se resuelven en el Core.
5. **Nunca se cachean** en esta capa: credenciales, OTP, secretos de sesión, ni plantillas o secretos biométricos.
6. El estado de una transacción se lee del almacén durable de la máquina de estados, no de la caché de consulta.
7. Si la caché falla, el servicio se degrada a una consulta directa con la protección de la ACL.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Read-Through | No seleccionada | Comportamiento equivalente, pero oculta en la infraestructura una decisión de dominio (qué se cachea y cómo) |
| Write-Through / Write-Behind | Descartada | La plataforma no es dueña de la escritura del dato del Core. Write-Behind sobre datos financieros arriesga pérdida de datos |
| TTL global único | Descartada | Ignora las diferencias de sensibilidad entre categorías. Es demasiado largo para el saldo o demasiado corto para los catálogos |
| Sin caché (siempre al Core) | Descartada | No cumple R5 y traslada al Core toda la carga de consulta (S4) |
| CQRS con proyecciones de lectura | Diferida | Ver [ADR-0007](ADR-0007-cqrs-diferido.md) |
| **Cache-Aside con frescura por categoría** | **Seleccionada** | Control por categoría; sin acoplamiento a la escritura del Core |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Menos carga sobre el Core y menos latencia en las consultas repetidas | Ventanas de inconsistencia acotadas por categoría |
| Control explícito por tipo de dato | Más configuración y gobierno (matriz de cacheabilidad) |
| Degradación de lecturas ante la indisponibilidad del Core (QA3) | La caché contiene datos personales y es un activo que se debe proteger |

## Consecuencias

- Las respuestas degradadas incluyen un indicador de frescura.
- La seguridad del almacén de caché es un requisito para la fase de seguridad: cifrado, aislamiento por cliente y control de acceso.
- Se instrumentan métricas de tasa de aciertos, de antigüedad de los datos servidos y de llamadas al Core evitadas. Estas métricas alimentan los disparadores de [ADR-0007](ADR-0007-cqrs-diferido.md).
- La tecnología de caché se selecciona según los requisitos de seguridad, disponibilidad y operación (DD-07), no por convención.

## Riesgos

- RR-05: fallas en la invalidación o en la entrega de eventos.
- Exposición de datos personales si la caché no se protege adecuadamente (fase de seguridad).
