# 01 — Drivers arquitectónicos

| Campo | Valor |
|---|---|
| Fase | 0 — Drivers, supuestos y escenarios de calidad |
| Estado | Cerrado. Pendiente de revisión del comité |
| Documentos relacionados | [02 — Supuestos](02-assumptions.md), [03 — Atributos de calidad](03-quality-attributes.md), [09 — Trazabilidad](09-requirement-traceability.md) |

## 1. Propósito

Un **driver arquitectónico** (*architecture driver*) es un requisito, restricción u objetivo de calidad que influye de forma significativa en la estructura del sistema. Este documento identifica los drivers de la Plataforma de Banca Digital y los separa según su origen:

| Origen | Significado | Ejemplo |
|---|---|---|
| **Reto** | Lo indica explícitamente el enunciado del reto (R1–R6, C1–C6, E1–E4) | «Los usuarios se autentican mediante OAuth 2.0» (R2) |
| **Supuesto** | Lo asume el equipo de arquitectura y debe validarse con la institución (S1–S15) | El Core no ofrece idempotencia nativa (S3) |
| **Propuesta del equipo** | Priorización o criterio que propone el equipo de arquitectura | La seguridad tiene prioridad crítica |

## 2. Contexto del reto

Según el enunciado, la institución («Banco Ecuador») lanzará una plataforma de banca digital web y móvil. La plataforma debe permitir a sus clientes:

- consultar movimientos;
- realizar transferencias propias e interbancarias;
- gestionar el onboarding de nuevos usuarios.

La información del cliente proviene de dos fuentes, ambas conectadas a una base de datos principal:

- un **Core Bancario**, con los datos básicos, productos, cuentas y movimientos;
- un **sistema complementario** con los datos extendidos del cliente.

## 3. Drivers funcionales

| ID | Driver | Origen | Referencia en el reto | Relevancia arquitectónica |
|---|---|---|---|---|
| DF-01 | Información del cliente | Reto | R1, contexto | Combina dos fuentes: el Core y el sistema complementario. Exige una frontera de traducción y una regla clara de autoridad del dato |
| DF-02 | Información de cuentas | Reto | Contexto («productos, cuentas») | Dato del Core. La consistencia del saldo es sensible y condiciona la caché |
| DF-03 | Consulta de movimientos | Reto | R1, contexto | Carga de lectura con mayor volumen esperado (S10). Es el principal riesgo de sobrecarga del Core |
| DF-04 | Transferencias propias | Reto | R1, contexto | Operación financiera crítica. Requiere integridad, idempotencia y manejo del resultado desconocido |
| DF-05 | Transferencias interbancarias | Reto | R1 («invoca servicios externos según el tipo»), contexto | Agrega un sistema externo adicional (S13). Puede quedar en estado intermedio prolongado |
| DF-06 | Onboarding digital | Reto | R3, contexto | Proceso largo con varios pasos externos (KYC, biometría, creación en el Core). Requiere estado durable |
| DF-07 | Autenticación | Reto | R2, R3 | OAuth 2.0 es obligatorio. El flujo y el proveedor de identidad se deciden en la fase de seguridad (S9) |
| DF-08 | Autorización | Reto | R2 | Control de acceso por cliente y por operación. Las operaciones de alto riesgo requieren controles adicionales (QA4) |
| DF-09 | KYC (*Know Your Customer*, conocimiento del cliente) | Reto | R2 | Integración con un proveedor externo. Debe quedar en la arquitectura de forma explícita |
| DF-10 | Reconocimiento facial y verificación biométrica | Reto | R2, R3 | Datos biométricos: categoría especialmente sensible. Condiciona dónde se procesan y almacenan |
| DF-11 | Notificación de movimientos | Reto | R4 | «Por normativa» se notifica cada movimiento por al menos 2 canales. La entrega debe ser confiable y desacoplada de la transacción |
| DF-12 | Auditoría | Reto | R5 | Base de datos de auditoría que registra **todas** las acciones del cliente. La evidencia debe ser íntegra y no repudiable |
| DF-13 | Caché | Reto | R5 | Caché «para clientes frecuentes basado en patrones de diseño». Debe convivir con la consistencia del dato financiero |

## 4. Drivers de calidad

El reto no prioriza los atributos de calidad. La siguiente priorización es una **propuesta del equipo de arquitectura**, y su justificación detallada está en [03 — Atributos de calidad](03-quality-attributes.md).

| Atributo | Prioridad propuesta | Vínculo con el reto |
|---|---|---|
| Seguridad | Crítica | R2, R3, C1, C4 |
| Integridad transaccional | Crítica | DF-04, DF-05 (derivado; el reto no lo menciona explícitamente) |
| Disponibilidad | Crítica | C2 |
| Auditabilidad | Crítica | R5, C1 |
| Resiliencia | Crítica | C2, C3 |
| Rendimiento | Alta | C6 («garantice baja latencia»), R5 (caché) |
| Escalabilidad | Alta | C5 |
| Mantenibilidad | Alta | C5 («agregar nuevos componentes sin impactar los existentes») |
| Experiencia de usuario | Alta | R3, R6 |
| Eficiencia de costos | Media | C6 («Cuenta con presupuesto») |

## 5. Restricciones

| ID | Restricción | Origen |
|---|---|---|
| RS-01 | Autenticación y autorización mediante OAuth 2.0 | Reto (R2) |
| RS-02 | Debe existir un API Gateway frente a al menos 3 servicios: datos del cliente, movimientos y transferencias | Reto (R1) |
| RS-03 | Onboarding desde la app móvil con reconocimiento facial | Reto (R3) |
| RS-04 | Al menos 2 canales de notificación | Reto (R4) |
| RS-05 | SPA web y app móvil en un framework multiplataforma | Reto (R6) |
| RS-06 | Nube pública AWS o Azure, opcional («si lo considera necesario») | Reto (C6) |
| RS-07 | El Core Bancario y el sistema complementario son sistemas existentes que no se reemplazan | Supuesto (S1, S5) |
| RS-08 | Modelo C4 hasta nivel de componentes, diagrama de infraestructura y documento de justificaciones | Reto (E1–E3) |

## 6. Drivers que todavía no se pueden cuantificar

El reto no especifica volumen de transacciones por segundo (TPS), número de clientes, perfil de tráfico, objetivos de latencia, RTO ni RPO. Estos valores se tratan como **supuestos u objetivos por validar** (S10) y aparecen parametrizados en los [escenarios de calidad](04-qa-scenarios.md). Ningún documento de este repositorio los presenta como requisitos confirmados.
