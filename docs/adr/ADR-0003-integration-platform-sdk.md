# ADR-0003 — Integration Platform SDK: capacidad compartida, política local

| Campo | Valor |
|---|---|
| Estado | **Aceptada** (Fase 1). Reemplaza el concepto previo de «integration kernel» |
| Fecha | 2026-10-27 |
| Requisitos | R1, C2, C4, C5 |
| Drivers y supuestos | S11; QA1; RR-01, RR-02 |
| Relacionados | [ADR-0001](ADR-0001-domain-acl.md), [05 — Frontera de integración, sección 4](../05-core-integration-boundary.md#4-integration-platform-sdk) |

## Contexto

La selección de ACL por dominio ([ADR-0001](ADR-0001-domain-acl.md)) genera duplicación de preocupaciones técnicas: correlación, trazas, manejo de errores técnicos, primitivas de resiliencia y clientes seguros. Una propuesta anterior las agrupaba en un «integration kernel». Ese nombre sugería un componente central con comportamiento propio.

## Problema

¿Cómo se reduce la duplicación entre las ACL sin reintroducir, por la vía de una biblioteca, el acoplamiento central que se evitó al descartar la capa de integración centralizada?

## Decisión

1. Se renombra el concepto a **Integration Platform SDK**.
2. El SDK es una **biblioteca compartida o capacidad de plataforma**, **no un servicio centralizado en tiempo de ejecución**. No tiene despliegue propio ni está en la ruta de red.
3. Principio rector: **capacidad compartida, política local** (*shared capability, local policy*). El SDK ofrece **mecanismos**; cada ACL o servicio **declara sus políticas** propias de cada operación.

### Responsabilidades permitidas

- Soporte de identificador de correlación.
- Propagación de trazas (OpenTelemetry).
- Modelo estándar de error técnico.
- Utilidades de observabilidad, convenciones de registro y utilidades de métricas.
- Primitivas de resiliencia (Circuit Breaker, Bulkhead, reintento, timeout).
- Soporte para configurar timeouts y para propagar el plazo.
- Utilidades de cliente de seguridad.
- Convenciones comunes de integración.

### Responsabilidades prohibidas

- Lógica de negocio.
- Lógica de clientes, cuentas, movimientos o transferencias.
- Enrutamiento entre Core.
- Lógica de Saga u orquestación.
- Transformaciones específicas de un dominio.
- Flujos de integración centralizados.
- Valores por defecto de política que sustituyan la decisión explícita de cada ACL. Por ejemplo, un reintento por defecto que se aplique a una escritura.

### Ejemplo de política local

| | ACL de movimientos | ACL de transferencias |
|---|---|---|
| Timeout de lectura | Orientado a la experiencia de consulta | Más estricto, dentro del plazo de negocio |
| Reintento | Limitado; solo para lecturas seguras e idempotentes | **Ningún reintento a ciegas** |
| Circuit Breaker | Sí | Sí |
| Pool de conexiones | Dedicado | Dedicado y separado |
| Resultado incierto | No aplica | `UNKNOWN` y conciliación |

### Gobierno del SDK

- Revisión obligatoria de arquitectura para todo cambio del SDK.
- Regla verificable: **ningún tipo de dominio de negocio** en el SDK. Se comprueba con pruebas de arquitectura automatizadas.
- Versionado semántico y compatibilidad hacia atrás. Ningún cambio del SDK puede obligar a desplegar todos los dominios a la vez.
- Presupuesto de dependencias acotado, para no imponer bibliotecas pesadas a los consumidores.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Sin elementos compartidos (cada ACL implementa todo) | Descartada | Máxima duplicación e inconsistencia en la observabilidad y en el manejo de errores |
| «Integration kernel» con comportamiento y valores por defecto centralizados | Descartada | Tiende a convertirse en una «God Library» y en un acoplamiento central encubierto |
| Servicio de integración centralizado | Descartada en este contexto | Ver [ADR-0001](ADR-0001-domain-acl.md), alternativa B |
| *Sidecar* o *service mesh* para las preocupaciones de red | No decidida | Puede complementar al SDK (mTLS, reintentos de red), pero no conoce la semántica de las operaciones. Se evaluará en la fase de infraestructura. **No** debe aplicar reintentos sobre operaciones financieras |
| **SDK con mecanismos y políticas locales** | **Seleccionada** | Reduce la duplicación y conserva la autonomía |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Consistencia en la observabilidad, los errores técnicos y la resiliencia | Gobierno continuo del SDK |
| Menos código repetido | Dependencia de versión compartida entre dominios |
| Políticas explícitas por operación | Cada ACL debe configurar sus políticas; no hay «magia» por defecto |

El SDK **mitiga** la duplicación; **no elimina** el costo arquitectónico de la Opción C.

## Consecuencias

- Las ACL dependen del SDK para los mecanismos y declaran sus políticas en su propia configuración.
- Si el stack de servicios es heterogéneo, el SDK podría necesitar versiones por lenguaje. Esto es un factor a considerar en la selección de tecnología.
- Las políticas de resiliencia se documentan por ACL.

## Riesgos

- RR-02: el SDK se convierte en una «God Library».
- RR-01: duplicación residual.

Ver [11 — Riesgos residuales](../11-residual-risks.md).
