# Matriz de cumplimiento del reto

| Campo | Valor |
|---|---|
| Documento | Cobertura de R1–R6 y C1–C6: decisión, diagramas, documentos, evidencia y estado |
| Fecha | 2026-10-27 |
| Detalle previo | [09 — Trazabilidad de requisitos](../09-trazabilidad-de-requisitos.md) (requisito → driver → decisión) |

---

## 1. Etiquetas de estado

La matriz separa la **cobertura arquitectónica** (diseño documentado y defendible) de la **cobertura de código** (lo que existe y se probó en la base ejecutable).

| Etiqueta | Significado |
|---|---|
| DESIGNED | Diseño documentado con decisión y diagrama |
| IMPLEMENTED | Código existente, sin prueba automatizada específica |
| TESTED | Código existente con pruebas automatizadas que pasaron (doc [validación final](validacion-final.md)) |
| PARTIAL | Cubierto solo en parte; la limitación lo indica |
| BLOCKED | Bloqueado de forma deliberada por gobierno de riesgos (B-0x) |
| DEFERRED | Diferido con disparador explícito (doc 10) |
| — | Sin código (no corresponde o no se construyó) |

Diagramas: `C4/…` = `diagrams/c4/structurizr-….svg`.

## 2. Matriz

| Req. | Requisito (resumen) | Decisión arquitectónica | Diagramas | Documentos | Evidencia implementada | Arquitectura | Código | Limitaciones |
|---|---|---|---|---|---|---|---|---|
| **R1** | API Gateway que orqueste ≥ 3 servicios: cliente, movimientos, transferencias | Gateway de borde sin lógica de negocio + BFF compartido; servicio de consultas (cliente y movimientos) y de transferencias con ACL por dominio | C4/Contenedores, C4/Contenedores-Consultas, C4/Contenedores-Transferencias, C4/Componentes-BFF, C4/Componentes-Consultas | ADR-0001, 0002, 0012, 0014; 16; 24; 25 | Contratos OpenAPI de BFF, consultas y frontera de transferencias; simulador del Core con fallas inyectables | DESIGNED (transferencias: PARTIAL, orquestador provisional) | PARTIAL: contratos y simulador TESTED; Gateway, BFF y consultas sin código | Transferencias **BLOCKED** (B-05); topología interbancaria abierta (Q-01) |
| **R2** | OAuth 2.0, flujo recomendado, KYC y biometría | IdP interno OIDC; Authorization Code + PKCE; BFF como cliente confidencial con sesión del servidor; MFA y autenticación reforzada | [Seguridad](../../diagrams/seguridad/arquitectura-de-seguridad.svg), C4/Contenedores-Vinculacion | ADR-0013, 0019, 0023; 22; 29 §1 | KYC simulado en vinculación (TESTED) | DESIGNED | PARTIAL: sin IdP ni BFF implementados | Producto del IdP provisional; DPoP/mTLS diferido (DD-04); proveedor KYC por seleccionar (DD-14) |
| **R3** | Onboarding móvil con reconocimiento facial; acceso posterior con contraseña o biometría | Máquina de estados E1–E6 (ADR-0009); KYC + prueba de vida en el servidor; biometría local solo desbloquea una credencial del dispositivo | C4/Componentes-Vinculacion, [Seguridad](../../diagrams/seguridad/arquitectura-de-seguridad.svg) | ADR-0009, 0023; 07 §6; 29 §2 | `services/digital-linking`: inicio, E1, estado, idempotencia, unicidad, outbox; E3/E5 deshabilitados con prueba de inalcanzabilidad (24 pruebas) | PARTIAL (alta y clientes preexistentes bloqueados) | TESTED (subconjunto E1) | Alta en el Core **BLOCKED** (B-05); preexistentes B-02; sesión del solicitante sin autenticar (PF4-01) |
| **R4** | Notificar cada movimiento por ≥ 2 canales | Eventos por outbox → broker → servicio de notificaciones; push con respaldo SMS (y correo); deduplicación, orden y cola de mensajes muertos | C4/Contenedores-NotificacionesAuditoria, C4/Componentes-Notificaciones | ADR-0008, 0015; 16 §8; 25 §4 | `services/notifications`: deduplicación, descarte de estados obsoletos, reintento, paso a SMS, DLQ (TESTED con proveedores falsos) | DESIGNED | TESTED (eventos de vinculación) | Proveedores reales pendientes (DD-12, Q-10); eventos de movimientos aún sin productor; alcance de «cada movimiento» (Q-04) |
| **R5** | BD de auditoría de todas las acciones + caché para clientes frecuentes | Servicio y almacén de auditoría de solo anexión, política A1–A8, cadena de hash y archivo WORM; Cache-Aside con frescura por categoría | C4/Componentes-Auditoria, C4/Contenedores-Consultas | ADR-0004, 0020; 06; 16 §8–9 | `services/audit`: anexión idempotente, cadena por fuente, huecos de secuencia, consulta con registro A6 (TESTED) | DESIGNED | PARTIAL: auditoría TESTED; caché sin código | WORM y retención por acordar (Q-19); caché pendiente de implementar con consultas |
| **R6** | SPA web + app multiplataforma; 2 opciones de stack justificadas | A: React + TS / Flutter; **B (recomendada): React + TS / React Native + TS** | — | ADR-0018; 21 §7 | — | DESIGNED | — (DEFERRED: frontend no construido) | Recomendación condicionada a Q-18 y al SDK KYC (DD-14) |
| **C1** | Elementos regulatorios (datos personales, ciberseguridad, PCI DSS) | LOPDP (biometría como dato sensible, consentimiento, evaluación de impacto, transferencias); normativa del organismo de control; PCI DSS condicional | [Seguridad](../../diagrams/seguridad/arquitectura-de-seguridad.svg) | ADR-0010; 29 §5; 14 | Consentimiento obligatorio en la API de vinculación; HMAC de identificaciones (TESTED) | DESIGNED | PARTIAL | **No certifica cumplimiento**; tipo de institución (Q-05) y residencia (ADR-0010) por validar |
| **C2** | Operación ante fallas parciales | Aislamiento por dominio, timeouts, cortacircuitos, cuotas del Core, degradación a caché, multizona | [Infraestructura](../../diagrams/infraestructura/infraestructura-referencia-resumen.svg), C4/Infraestructura-Referencia | ADR-0005, 0011, 0016, 0022; 19; 28 §2–3 | KYC no disponible → estado reintentable; paso a SMS; DLQ; fallas inyectables en el simulador (TESTED) | DESIGNED | PARTIAL | HA no probada en nube (no hay despliegue) |
| **C3** | DR: RTO/RPO, respaldos, failover | Región secundaria en espera templada; respaldos aislados; conmutación manual con cuarentena financiera; RTO/RPO **candidatos** | [Infraestructura](../../diagrams/infraestructura/infraestructura-referencia-resumen.svg) | ADR-0016, 0021, 0022; 28 | — | PARTIAL | — | RTO/RPO sin acordar ni probar (DD-09); ADR-0021 no aprobado (ARG03-C01, H01, H02) |
| **C4** | Datos en tránsito y en reposo; monitoreo continuo | TLS y mTLS, cifrado con KMS, HSM para firma, gestor de secretos, OpenTelemetry, telemetría de seguridad y SIEM | [Seguridad](../../diagrams/seguridad/arquitectura-de-seguridad.svg), [Infraestructura](../../diagrams/infraestructura/infraestructura-referencia-resumen.svg) | 22; 29 §3–4; ADR-0003, 0020 | Correlación, enmascaramiento, errores sin datos internos, HMAC (TESTED); métricas Prometheus y salud configuradas | DESIGNED | PARTIAL | Custodia HSM (PF4-05); SIEM (Q-13) |
| **C5** | Agregar componentes sin impactar los existentes | Descomposición híbrida; BD por servicio; outbox + broker; ACL por dominio; SDK sin lógica de negocio; contratos versionados | C4/Contenedores | ADR-0001, 0003, 0012, 0015; 20; 27 §6 | Módulos independientes; sobre de eventos común; pruebas de contrato contra OpenAPI (TESTED) | DESIGNED | TESTED (modularidad y contratos) | CQRS diferido con disparadores (ADR-0007) |
| **C6** | AWS o Azure con baja latencia | Comparación ponderada; **AWS como referencia** (87/100 frente a 84/100); condiciones de cambio a Azure | [Infraestructura](../../diagrams/infraestructura/infraestructura-referencia-resumen.svg), C4/Infraestructura-Referencia | ADR-0010, 0022; 21 §8; 27 | `infra/local` (solo entorno local) | PARTIAL (referencia, no aprobada) | — | Sin latencia medida ni costos; región y conectividad con el Core por confirmar (Q-20) |

## 3. Lectura de la matriz

- **Cobertura arquitectónica:** los 12 requisitos tienen decisión, documento y, salvo R6, diagrama. C3 y C6 quedan **PARTIAL** porque dependen de datos institucionales que no se inventan.
- **Cobertura de código:** la base ejecutable demuestra los patrones de resiliencia, idempotencia, outbox, auditoría, notificación y contratos. **No** demuestra Gateway, BFF, IdP, consultas, caché ni frontend.
- **Bloqueos deliberados:** la ejecución financiera y el alta en el Core (B-05) siguen bloqueadas por decisión de gobierno de riesgos ([guía de defensa](guia-defensa-arquitectura.md#1-por-qué-las-transferencias-están-bloqueadas)).
