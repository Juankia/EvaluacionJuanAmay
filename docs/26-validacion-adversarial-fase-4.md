# 26 — Validación adversarial y preguntas abiertas de la Fase 4

| Campo | Valor |
|---|---|
| Fase | 4 — C4 Nivel 3: Componentes |
| Estado | **Razonamiento conceptual.** No existe software y **no se ejecutó ninguna prueba** |
| Documentos relacionados | [24](24-arquitectura-de-componentes.md), [25](25-contratos-eventos-y-errores.md), [ADR-0021](adr/ADR-0021-diario-de-autorizacion-de-despacho.md), [ADR-0019](adr/ADR-0019-sesiones-del-bff.md), [ADR-0020](adr/ADR-0020-politica-de-auditoria-por-categoria.md), [ADR-0013](adr/ADR-0013-frontera-del-idp.md) |

## 1. Escenarios

| # | Escenario | Comportamiento esperado | Invariante de seguridad | Consecuencia en la disponibilidad | Mecanismo de recuperación | Riesgo residual | Evidencia |
|---|---|---|---|---|---|---|---|
| 1 | Termina una réplica del BFF con una sesión activa | Otra réplica atiende la siguiente solicitud con la misma sesión | Ningún token en el navegador | Ninguna (solo la solicitud en curso) | Almacén de sesiones externo | Bajo | ADR-0019; 24 §4.1 |
| 2 | El almacén de sesiones no está disponible | La web exige volver a iniciar sesión cuando se recupere; **no** se usan tokens en el navegador | Falla cerrada de la autenticación web | Web no disponible; el móvil no se ve afectado | Alta disponibilidad gestionada; recuperación del almacén | RR-25 | ADR-0019 |
| 3 | Se compromete la credencial administrativa del IdP usada por el servicio de vinculación | Solo permite la operación acotada; cada uso exige una afirmación firmada; límites de volumen y una vinculación por cliente; notificación al cliente | Sin toma masiva silenciosa | La vinculación puede suspenderse (interruptor) | Rotación de credenciales y claves; revisión de las vinculaciones del periodo | **Medio:** el compromiso conjunto del servicio y de su clave de firma permite vinculaciones **limitadas y visibles** hasta la detección | ADR-0013 (enmienda) |
| 4 | Core sobrecargado durante un pico | Admisión por cuotas (CL-1/CL-2 reservadas); las consultas se degradan; la limitación de tasa actúa en el borde | Nunca se supera `MAX_CONCURRENCIA_CORE` ni `SESIONES_MAX_CORE` | Consultas degradadas o 503 | Presión de retorno; recuperación del Core | RR-14 | ADR-0011; 19 §3 |
| 5 | Almacén de auditoría no disponible durante un comando financiero | El comando **no depende** del almacén de auditoría: su evidencia se escribe en el outbox local en el mismo *commit* (A1) y se entrega después | Ninguna acción A1 sin evidencia durable | Ninguna para el comando; retraso de la auditoría | El outbox y el broker retienen; alerta por `T_MAX_RETRASO_AUDITORIA` | Bajo | ADR-0020 |
| 6 | Eventos de notificación fuera de orden | Movimientos: se retienen hasta `ESPERA_SECUENCIA` y se entregan en orden. Estados antiguos: se descartan | NOT-1 y NOT-2 | Retraso acotado de la notificación | Secuencia por sujeto | Bajo | 25 §4.3 |
| 7 | Datos de contacto del destinatario obsoletos | Los contactos se obtienen en el momento del envío desde el servicio de consultas, con caché corta | No se usa una copia permanente como fuente | Posible entrega a un contacto antiguo dentro de `TTL_CONTACTO` | Fuente autoritativa en el Core o en el Sistema Complementario (Q-09) | **Medio:** depende de la actualidad de los datos del Core | 18 §6 |
| 8 | Varios intentos concurrentes de vinculación del mismo cliente | Unicidad por ámbito y clave; el IdP permite **una vinculación por cliente**; revisión manual ante una ambigüedad | Sin dos vinculaciones activas | Una de las solicitudes se rechaza o se revisa | IC-07, IC-15; regla del IdP | Bajo | ADR-0006; ADR-0013 |
| 9 | El Core devuelve un timeout ambiguo | Lecturas: reintento acotado. Escrituras (transferencias y alta): `UNKNOWN`, sin reenvío | INV-T3, INV-T9 | Operación «en verificación» | Conciliación (ADR-0005) | RR-04, RR-17. **Componentes de escritura bloqueados (B-05)** | ADR-0005 |
| 10 | Un servicio de dominio se reinicia durante un flujo | Vinculación: el estado durable permite retomar; los tramos en curso pasan a `*_UNKNOWN` | INV-T5 | Retraso | Recuperación desde el estado durable | Bajo (vinculación) | 07 §5; 24 §4.3 |
| 11 | Se degradan las garantías de replicación de la base de datos | Modo DEGRADADO_BD: despachos solo con el diario disponible, con alerta. Si además cae el diario: sin despachos | INV-DR | Funciones financieras limitadas | Recuperación de la réplica; validación (CP-10) | RR-19 (medio) | ADR-0016 (enmienda) |
| 12 | La recuperación ante desastres pierde registros confirmados recientemente | Cuarentena; incremento de la época; reconstrucción desde el diario; conciliación | INV-DR, INV-DR-1 | Funciones financieras en cuarentena | Protocolo R-1 a R-7 | RR-17 (vivacidad). **Diseño de componentes bloqueado (B-05)** | ADR-0021 §6 |
| 13 | El cliente reintenta tras una conmutación regional | Clave conocida (en la BD o reconstruida desde el diario) → estado. Clave sin entrada en el diario → nueva, solo después de la cuarentena | INV-DR-1 | Rechazo durante la cuarentena | IC-16 | Bajo, condicionado a DD-23 | ADR-0021; QA14 |
| 14 | Dos regiones creen estar activas temporalmente | La región con la época anterior no puede anotar en el diario y, por tanto, no despacha. Sus reservas no generan efectos | PT-08 | La región aislada no procesa comandos financieros | Cercado por época | Bajo, condicionado a la linealizabilidad del diario | ADR-0021 §5 |
| 15 | Un evento de seguridad no puede persistirse | A2: el IdP rechaza el cambio de seguridad si no puede registrarlo. A7 (inicio de sesión): el inicio no espera; la pérdida es detectable | Ningún cambio de seguridad sin evidencia | Cambios de seguridad temporalmente no disponibles | Recuperación del almacén del IdP | **Medio:** depende de que el producto de IdP lo soporte (requisito de selección) | ADR-0020 (A2, A7) |
| 16 | El proveedor externo de verificación de identidad no está disponible | `IDENTITY_VERIFICATION_UNAVAILABLE`; reintento dentro de la vigencia; nunca rechazo ni aprobación | VD-09, INV-O6 | La vinculación no avanza | Reintento; mensaje claro al solicitante | Dependencia del proveedor (DD-14) | ADR-0009; 24 §4.3 |

## 2. Preguntas abiertas de diseño de la Fase 4

| ID | Pregunta | Afecta |
|---|---|---|
| PF4-01 | ¿Cómo se autentica la sesión de vinculación del solicitante antes de que exista una cuenta digital (vinculación al dispositivo, atestación)? | Vinculación; fase de seguridad |
| PF4-02 | ¿Qué valores corresponden a `ESPERA_SECUENCIA`, `TTL_CONTACTO`, `MAX_INTENTOS_CONSUMO` y `RETENCION_BROKER`? | Notificaciones; broker |
| PF4-03 | ¿Los contactos son autoritativos en el Core o en el Sistema Complementario? (amplía Q-09) | Consultas; notificaciones |
| PF4-04 | ¿El producto de IdP permite la operación de vinculación acotada y la regla A2 de forma nativa o con una extensión? | ADR-0013; selección del IdP |
| PF4-05 | ¿La clave de firma de las afirmaciones se custodia en un HSM? | Vinculación; fase de seguridad |
| PF4-06 | ¿Qué granularidad de partición se usa en la cadena de auditoría (por fuente, por cliente o por día)? | Auditoría |
| PF4-07 | ¿Qué SDK del proveedor KYC se integra en la app y qué se envía al servidor? | Vinculación; DD-14 |

Las preguntas institucionales siguen en [15](15-preguntas-abiertas-fase-2.md) (Q-01 a Q-21).
