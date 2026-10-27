# ADR-0002 — API Gateway como frontera de fachada y composición, no como orquestador de negocio

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (Fase 1, interpretación arquitectónica sujeta a revisión del comité) |
| Fecha | 2026-10-27 |
| Requisitos | R1 |
| Drivers y supuestos | AQ-01, AQ-03, AQ-08; QA1, QA4 |
| Relacionados | [ADR-0001](ADR-0001-acl-por-dominio.md), [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [05 — Frontera de integración, sección 8](../05-frontera-de-integracion-core.md#8-interpretación-del-api-gateway) |

## Contexto

El requisito R1 dice textualmente: «Diseñar un API Gateway que **orqueste** al menos 3 servicios: consulta de datos del cliente, consulta de movimientos y transferencias (que invoca servicios externos según el tipo)».

## Problema

El verbo «orquestar» admite dos lecturas:

1. **Orquestación de negocio en el Gateway:** el Gateway encadena llamadas, aplica reglas y decide según el tipo de transferencia qué sistema externo invocar.
2. **Exposición y gobierno del tráfico:** el Gateway es el punto de entrada único que enruta, protege y gobierna el acceso a los servicios. La orquestación de negocio vive en los servicios.

La primera lectura pone lógica de negocio crítica (transferencias interbancarias, manejo de `UNKNOWN`, conciliación) en un componente de infraestructura.

## Decisión

Se adopta la **segunda lectura** como **interpretación arquitectónica explícita**.

El API Gateway es una **frontera de tráfico y de fachada**. Provee:

- enrutamiento hacia los servicios de dominio;
- terminación TLS donde corresponda;
- integración con la validación de tokens (OAuth 2.0);
- limitación de tasa (*rate limiting*);
- integración con WAF;
- correlación de solicitudes (identificador de correlación y contexto de trazas);
- protección de las API (validación de esquemas, límites de tamaño);
- opcionalmente, composición liviana mediante un BFF (*Backend for Frontend*), si se justifica en la fase de contenedores (DD-10).

La **orquestación de negocio** pertenece a los servicios de dominio y de aplicación. Por ejemplo, el servicio de transferencias decide según el tipo de transferencia qué sistema externo se invoca, mediante su máquina de estados ([ADR-0005](ADR-0005-maquina-de-estados-transaccional.md)).

El Gateway puede tener **timeouts de infraestructura y de solicitud**. Los **plazos de negocio** pertenecen a la capa de dominio y se propagan ([05, sección 5](../05-frontera-de-integracion-core.md#5-frontera-de-resiliencia)).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Orquestación de negocio en el Gateway (políticas, *scripting* o flujos del producto de Gateway) | Descartada | Pone lógica crítica en un componente difícil de probar, versionar y observar como código de dominio. Acopla la lógica al producto de Gateway. No modela de forma natural los estados durables ni la conciliación. Concentra cambios de todos los dominios en un mismo componente |
| Gateway sin ninguna composición | Válida, pero puede ser insuficiente | Si una pantalla requiere varias consultas, la composición puede necesitar un BFF. Se evaluará en la fase de contenedores |
| **Gateway como fachada; composición en un BFF; orquestación en los servicios** | **Seleccionada** | Separa las preocupaciones de tráfico de las de negocio |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Lógica de negocio probada y versionada como código de dominio | Requiere servicios de aplicación explícitos (y posiblemente un BFF) |
| Gateway reemplazable (bajo acoplamiento al producto) | Posible salto de red adicional si se introduce un BFF |
| Políticas de borde homogéneas | Debe explicarse la interpretación al evaluador del reto |

## Consecuencias

- La respuesta a R1 explica explícitamente la interpretación de «orquestar».
- El servicio de transferencias es el que «invoca servicios externos según el tipo».
- El producto concreto de API Gateway se selecciona en la fase de infraestructura y no condiciona la lógica de negocio.
- El Gateway no reintenta operaciones financieras.

## Riesgos

- Puede percibirse como una desviación del texto del reto. Se mitiga documentando la interpretación y su justificación, y mostrando en los diagramas C4 que el Gateway enruta hacia al menos los tres servicios requeridos.
- Un BFF mal gobernado podría acumular lógica de negocio. Debe limitarse a composición y adaptación al canal.
