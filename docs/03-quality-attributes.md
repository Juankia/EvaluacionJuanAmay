# 03 — Atributos de calidad

| Campo | Valor |
|---|---|
| Fase | 0 — Drivers, supuestos y escenarios de calidad |
| Estado | Cerrado. La priorización es una propuesta del equipo de arquitectura |
| Documentos relacionados | [01 — Drivers](01-architecture-drivers.md), [04 — Escenarios de calidad](04-qa-scenarios.md) |

## 1. Naturaleza de esta priorización

El reto menciona preocupaciones de calidad (alta disponibilidad, tolerancia a fallos, DR, seguridad, monitoreo, escalabilidad, desacoplamiento y baja latencia), pero **no las prioriza**. La priorización siguiente es una **propuesta del equipo de arquitectura**. Se basa en la naturaleza de una plataforma financiera transaccional y debe ratificarla la institución.

La prioridad sirve para resolver conflictos. Cuando dos atributos compiten, prevalece el de mayor prioridad, salvo una decisión explícita registrada en un ADR.

## 2. Priorización propuesta

| ID | Atributo | Prioridad | Justificación | Vínculo con el reto |
|---|---|---|---|---|
| AQ-01 | Seguridad | Crítica | Datos personales, financieros y biométricos. El canal digital está expuesto a Internet. El fraude tiene impacto directo en el patrimonio del cliente y en la reputación de la institución | R2, R3, C1, C4 |
| AQ-02 | Integridad transaccional | Crítica | Ninguna transferencia puede duplicarse, perderse ni quedar en un estado no conciliable. Este atributo **prevalece sobre la disponibilidad** en las operaciones de escritura | Derivado de DF-04 y DF-05 |
| AQ-03 | Disponibilidad | Crítica | El canal digital suele ser el canal principal del cliente. Las fallas parciales no deben convertirse en indisponibilidad total | C2 |
| AQ-04 | Auditabilidad | Crítica | El reto exige registrar **todas** las acciones del cliente. La evidencia sostiene la atención de reclamos, la investigación de fraude y la supervisión | R5, C1 |
| AQ-05 | Resiliencia | Crítica | El Core y los proveedores externos (KYC, biometría, notificaciones, sistema de pagos) fallarán en algún momento. La plataforma debe degradarse de forma controlada y recuperarse | C2, C3 |
| AQ-06 | Rendimiento | Alta | El reto pide «garantice baja latencia». Las consultas frecuentes no deben depender en cada caso de la latencia del Core | C6, R5 |
| AQ-07 | Escalabilidad | Alta | Picos de demanda (quincena, fin de mes, feriados; S10) sin trasladar la carga al Core | C5 |
| AQ-08 | Mantenibilidad | Alta | Agregar capacidades o reemplazar integraciones sin impacto transversal | C5 |
| AQ-09 | Experiencia de usuario | Alta | El onboarding y el acceso biométrico son la primera impresión del canal. Los estados intermedios deben comunicarse con claridad | R3, R6 |
| AQ-10 | Eficiencia de costos | Media | El reto indica que «cuenta con presupuesto». El costo sigue siendo un criterio de selección, pero no domina sobre los atributos críticos | C6 |

## 3. Tensiones principales entre atributos

| Tensión | Resolución propuesta | Referencia |
|---|---|---|
| **Disponibilidad vs. integridad transaccional** | En las escrituras financieras prevalece la integridad: si el Core no está disponible, la operación falla de forma segura y no se encola a ciegas. En las lecturas puede prevalecer la disponibilidad, con datos de caché aprobados | [ADR-0005](adr/ADR-0005-transaction-state-machine.md), QA3 |
| **Rendimiento vs. consistencia** | La caché se define por categoría de dato. Los datos sensibles a la consistencia (saldo disponible, estado de una transacción) no se sirven desde la caché para decisiones transaccionales | [ADR-0004](adr/ADR-0004-cache-aside.md) |
| **Mantenibilidad vs. duplicación** | Las ACL por dominio duplican parte de la lógica de integración. El Integration Platform SDK mitiga esa duplicación sin centralizar el comportamiento | [ADR-0001](adr/ADR-0001-domain-acl.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md) |
| **Seguridad vs. experiencia de usuario** | Autenticación reforzada (*step-up*) solo en operaciones de alto riesgo. El mecanismo se decide en la fase de seguridad | QA4, DD-04 |
| **Resiliencia vs. costo** | La estrategia de DR (activo-pasivo, activo-activo) se decide según los RTO y RPO validados, no por defecto | DD-09 |
| **Auditabilidad vs. latencia** | La auditoría no debe bloquear la ruta crítica sin necesidad, pero tampoco puede perder eventos. El mecanismo se decide en la fase de contenedores | DD-01 |

## 4. Atributos de calidad y fases

| Atributo | Fase 0–1 (actual) | Fases posteriores |
|---|---|---|
| Seguridad | Se identifica el escenario de token robado (QA4); el mecanismo se difiere | Flujo OAuth, IdP, vinculación de token, cifrado, WAF |
| Integridad transaccional | Máquina de estados, idempotencia y conciliación (conceptual) | Diseño detallado y persistencia |
| Disponibilidad | Degradación controlada y aislamiento por dominio | Topología multizona y despliegue |
| Auditabilidad | Identificada como crítica | Diseño de la base de datos de auditoría (R5) |
| Resiliencia | Frontera de resiliencia en la ACL, plazos y timeouts | DR, RTO y RPO |
| Rendimiento | Cache-Aside y protección del Core | Selección de tecnología de caché, mediciones |
| Escalabilidad | CQRS diferido con disparadores | Escalado horizontal e infraestructura |
| Mantenibilidad | ACL por dominio y SDK | Gobierno de contratos y pruebas de contrato |
| Experiencia de usuario | Estados intermedios visibles (onboarding, transferencias) | Stack de frontend (R6) |
| Eficiencia de costos | No evaluada | Infraestructura cloud |
