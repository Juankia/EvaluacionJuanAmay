# 15 — Preguntas abiertas de la Fase 2

| Campo | Valor |
|---|---|
| Fase | 2 — C4 Nivel 1: Contexto del sistema |
| Estado | Vigente. Se revisa con la institución antes de cerrar la Fase 3 |
| Documentos relacionados | [12 — Contexto del sistema](12-contexto-del-sistema.md), [10 — Decisiones diferidas](10-decisiones-diferidas.md), [02 — Supuestos](02-supuestos.md) |

## 1. Criterio de priorización

| Impacto | Significado |
|---|---|
| **Alto** | La respuesta cambia los elementos o las relaciones del diagrama de contenedores, o una decisión crítica (integridad financiera, identidad, cumplimiento) |
| **Medio** | La respuesta cambia integraciones o responsabilidades sin alterar la estructura principal |
| **Bajo** | La respuesta precisa detalles que se pueden incorporar después |

## 2. Preguntas

### Impacto alto

| ID | Pregunta | Por qué importa | Afecta | Interlocutor sugerido |
|---|---|---|---|---|
| Q-01 | ¿Cómo se integran hoy las transferencias interbancarias? ¿Directamente, mediante la infraestructura de pagos existente (Core o componente de pagos) o mediante un intermediario autorizado? ¿Quién origina el mensaje de pago? | Define si existe una integración propia con la infraestructura de pagos, la forma de la máquina de estados (si `DEBIT_CONFIRMED` y `PENDING_EXTERNAL_CONFIRMATION` se fusionan; semántica de `COMPLETED` mediante `EVENTO_CONFIRMACION_EXTERNA`), el procedimiento de reverso (RR-16) y las integraciones críticas INT-09 e INT-10 | S13, DD-16, ADR-0005, REL-05 | Área de pagos y operaciones; propietario del Core |
| Q-02 | ¿El Core acepta y conserva una referencia externa única por transacción? ¿Permite consultar el estado por esa referencia y buscar clientes por identificación? ¿Ejecuta la transferencia propia como una operación atómica (S18)? ¿Qué códigos de respuesta define como rechazo «sin efecto» (taxonomía de ADR-0005)? | Cambia la conciliación de probabilística a determinista (RR-04) y la idempotencia de la creación de clientes | S3, ADR-0006, INT-04, INT-05, INT-06 | Propietario del Core |
| Q-03 | ¿La institución tiene un IdP para clientes? Si no, ¿prefiere un servicio gestionado externo o un producto operado dentro de la plataforma? | Cambia el diagrama de contenedores (el IdP es interno o externo) y la exposición de credenciales a terceros | S9, DD-03, REL-10, REL-11, INT-16 | Seguridad de la información; arquitectura empresarial |
| Q-04 | ¿«Notificar cada movimiento» (R4) incluye los movimientos originados fuera del canal digital (ventanilla, otros canales, procesos batch)? Si es así, ¿el Core puede informar esos movimientos? | Si la respuesta es sí, se requiere una integración entrante desde el Core (REL-13, INT-15). Esto probablemente activa DD-01 (bus de eventos) | R4, S14, REL-13, INT-15 | Cumplimiento; propietario del Core |
| Q-05 | ¿Qué tipo de institución es la destinataria: banco o cooperativa de ahorro y crédito? | Define el organismo de control y las normas aplicables | S15, RR-09 | Dirección de la institución |
| Q-06 | ¿Qué restricciones aplican al tratamiento de datos biométricos y de identidad por terceros: ubicación del procesamiento, retención, transferencia internacional? | Condiciona la selección del proveedor KYC/biométrico y, posiblemente, del IdP y de la región cloud | S8, S16, ADR-0010, INT-11 | Legal; cumplimiento; oficial de protección de datos |
| Q-12 | ¿Existen sistemas institucionales de prevención de fraude, de monitoreo transaccional o de listas de control para la prevención de lavado de activos que deban integrarse con el onboarding o con las transferencias? | El reto no los menciona. Si existen, son sistemas externos que faltan en el contexto | Contexto (posibles sistemas externos nuevos). **Elevada a impacto alto en la remediación ARG-01 (ARG01-M06)** | Riesgos; cumplimiento |
| Q-15 | ¿«Onboarding de nuevos usuarios» incluye a **clientes existentes** que se enrolan en el canal digital? ¿Qué factores registrados previamente (contactos, credenciales) tiene el Core y qué tan actualizados están? | Define el flujo de vinculación de clientes preexistentes y el mecanismo de prueba de control (ARG01-H05) | ADR-0009 (VD-03), DD-21, QA11 | Canales; seguridad de la información; propietario del Core |
| Q-16 | ¿Cuál es la capacidad del Core para los canales digitales (concurrencia y tasa máximas por tipo de operación)? ¿Existe un intermediario frente al Core que ya imponga límites globales? | Fija el presupuesto global y las cuotas por clase (ARG01-H04) | ADR-0011, QA8, QA10 | Propietario del Core; operaciones |
| Q-17 | ¿El Core ofrece alguna de estas capacidades: (a) consulta por referencia con **consistencia fuerte**; (b) **anulación o cierre** de una referencia que impida aplicar después una solicitud con esa referencia; (c) **expiración** de solicitudes por fecha de validez; (d) **retención y captura** de fondos en dos fases? ¿Los intermediarios frente al Core reintentan escrituras (S19)? ¿Puede el Core aplicar dos veces una misma solicitud (S20)? | Sin (b) o (c), ninguna operación potencialmente enviada puede cerrarse automáticamente como «sin efecto» (RR-17), y los envíos tardíos no pueden neutralizarse (RR-18) | ADR-0005 v3, DD-22, S19, S20 | Propietario del Core; arquitectura de integración |

### Impacto medio

| ID | Pregunta | Por qué importa | Afecta | Interlocutor sugerido |
|---|---|---|---|---|
| Q-07 | ¿Existe autorización legal y un mecanismo técnico para contrastar datos con una fuente oficial de identidad? ¿Lo hace la institución directamente o el proveedor KYC? | Define si la Fuente Oficial de Identidad es una integración de la plataforma, del proveedor o no aplica | DD-17, REL-12, INT-17 | Legal; cumplimiento |
| Q-08 | ¿Los operadores de soporte y los responsables de auditoría usarán una consola de la plataforma o sistemas administrativos existentes? | Define si existen actores internos de la plataforma y contenedores administrativos | DD-18, REL-14, REL-15 | Operaciones; auditoría interna |
| Q-09 | ¿El Sistema Complementario debe recibir los datos capturados durante la vinculación? | Define INT-08 y el flujo de datos del onboarding | ADR-0009, INT-08 | Propietario del Sistema Complementario |
| Q-10 | ¿Qué canales de notificación se prefieren? ¿Alguno es propio de la institución? ¿Qué contenido mínimo exige la norma? | Confirma o ajusta S17 y el contenido de INT-12 | S17, DD-12, R4 | Cumplimiento; canales |
| Q-11 | ¿Se confirma que la plataforma no debe acceder a la base de datos principal y que hay interfaces suficientes? | Confirma S12 y el uso exclusivo de interfaces | S12 | Propietario del Core |

### Impacto bajo

| ID | Pregunta | Por qué importa | Afecta | Interlocutor sugerido |
|---|---|---|---|---|
| Q-13 | ¿Existe un SOC o SIEM institucional al que la plataforma deba enviar eventos de seguridad? | Integración de monitoreo (C4 del reto) | Fase de seguridad e infraestructura | Seguridad de la información |
| Q-14 | ¿La autenticación biométrica posterior al onboarding (huella, rostro) será local al dispositivo o verificada por un proveedor? | Determina si el Proveedor KYC/Biométrico también participa en el inicio de sesión | R3, DD-04 | Seguridad de la información; canales |

## 3. Arrastradas de la Fase 0–1

Las validaciones de S3 (Q-02), S8 (Q-06), S13 (Q-01), S14 (Q-04) y S15 (Q-05) se reformularon como preguntas concretas en esta lista.
