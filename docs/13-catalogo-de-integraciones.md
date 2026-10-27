# 13 — Catálogo de integraciones

| Campo | Valor |
|---|---|
| Fase | 2 — C4 Nivel 1: Contexto del sistema |
| Estado | Vigente. Se detalla en la Fase 3 |
| Documentos relacionados | [12 — Contexto del sistema](12-contexto-del-sistema.md), [14 — Límites de confianza](14-limites-de-confianza.md) |

## 1. Propósito

Este catálogo identifica cada integración de la Plataforma de Banca Digital con los sistemas externos a nivel de **capacidad de negocio**. **No define API, endpoints ni protocolos.** El tipo de integración solo se indica cuando se conoce o cuando se deriva de una decisión vigente; en los demás casos figura como «Por validar».

## 2. Escalas

### 2.1 Sensibilidad del dato

La clasificación es una **propuesta arquitectónica** y debe alinearse con la política de clasificación de la información de la institución.

| Nivel | Descripción | Ejemplos |
|---|---|---|
| **Interna** | Información no personal de uso operativo | Catálogos de productos, estados técnicos |
| **Confidencial — personal** | Datos personales | Nombre, identificación, datos de contacto |
| **Confidencial — financiera** | Datos financieros del cliente | Cuentas, saldos, movimientos, transferencias |
| **Sensible** | Categorías especialmente sensibles o credenciales | Datos biométricos, imágenes faciales, documentos de identidad, credenciales, tokens |

### 2.2 Criticidad

| Nivel | Descripción |
|---|---|
| **Crítica** | Su falla o su error puede afectar la integridad financiera o la identidad del cliente |
| **Alta** | Su falla impide una función principal del canal |
| **Media** | Su falla degrada una función sin impedirla, o tiene alternativas |

## 3. Catálogo

| ID | Origen | Destino | Capacidad de negocio | Datos intercambiados | Sensibilidad | Dirección | Criticidad | Tipo de integración (si se conoce) | Supuestos | Preguntas abiertas |
|---|---|---|---|---|---|---|---|---|---|---|
| INT-01 | Plataforma | Core Bancario | Consulta de datos del cliente | Datos básicos del cliente | Confidencial — personal | Solicitud de la plataforma; respuesta del Core | Alta | Consulta síncrona mediante la ACL de clientes ([ADR-0001](adr/ADR-0001-acl-por-dominio.md)); protocolo por validar | S1, S2, S3, S12 | ¿Qué operaciones de consulta expone el Core? |
| INT-02 | Plataforma | Core Bancario | Consulta de productos y cuentas | Productos, cuentas, estado y saldos | Confidencial — financiera | Solicitud y respuesta | Alta | Consulta síncrona; caché según [06](06-estrategia-de-cache.md) | S2, S4 | ¿El Core distingue saldo contable y saldo disponible? |
| INT-03 | Plataforma | Core Bancario | Consulta de movimientos | Movimientos paginados | Confidencial — financiera | Solicitud y respuesta | Alta | Consulta síncrona mediante la ACL de movimientos; paginación | S4, S10 | ¿Admite paginación y filtros por fecha? ¿Hay límites de volumen? |
| INT-04 | Plataforma | Core Bancario | Ejecución de transferencias propias | Cuentas de origen y destino, monto, referencia, clave de idempotencia (si se admite) | Confidencial — financiera | Solicitud y respuesta (resultado) | **Crítica** | Escritura mediante la ACL de transferencias; sin reintento a ciegas ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md)) | S3 | ¿El Core acepta y conserva una referencia externa única? |
| INT-05 | Plataforma | Core Bancario | Creación del cliente (vinculación) | Datos de identificación y de contacto del solicitante; resultado e identificador del cliente | Confidencial — personal | Solicitud y respuesta | **Crítica** | Escritura idempotente ([ADR-0009](adr/ADR-0009-core-autoritativo.md)) | S3 | ¿El Core admite crear clientes desde un canal digital? ¿Qué datos mínimos exige? |
| INT-06 | Plataforma | Core Bancario | Consulta para conciliación | Estado de la operación por referencia, o movimientos por correlación; búsqueda de cliente por identificación | Confidencial — financiera y personal | Solicitud y respuesta | **Crítica** | Consulta; determinista o por correlación según S3 ([08](08-idempotencia-y-conciliacion.md)) | S3 | ¿Existe una consulta de estado por referencia? ¿Y una búsqueda de cliente por identificación? |
| INT-07 | Plataforma | Sistema Complementario | Consulta de información extendida | Datos extendidos del cliente | Confidencial — personal | Solicitud y respuesta | Media | Consulta síncrona mediante una ACL; caché según [06](06-estrategia-de-cache.md) | S5 | ¿Qué datos son autoritativos en este sistema? |
| INT-08 | Plataforma | Sistema Complementario | Registro de información pendiente de la vinculación | Datos extendidos capturados durante el onboarding | Confidencial — personal | Solicitud de la plataforma | Media | Por validar. Lo permite [ADR-0009](adr/ADR-0009-core-autoritativo.md), sin autoridad sobre la identidad | S5 | ¿La institución quiere que este sistema reciba datos del onboarding? (Q-09) |
| INT-09 | Plataforma | Sistema Externo de Transferencias | Procesamiento de transferencias interbancarias | Datos del ordenante y del beneficiario, institución destino, monto, referencia | Confidencial — financiera | Solicitud de la plataforma (directa o indirecta según la topología) | **Crítica** | **Por validar.** Depende de la topología A/B/C ([12, sección 4.1](12-contexto-del-sistema.md#41-sistema-externo-de-transferencias-modelos-de-integración-posibles)) | S13 | Q-01 |
| INT-10 | Sistema Externo de Transferencias | Plataforma | Resultado de la transferencia interbancaria | Estado (aceptada, rechazada, liquidada), motivo, referencia | Confidencial — financiera | Entrante, o consulta de la plataforma (por validar) | **Crítica** | Por validar (notificación entrante o consulta) | S13 | ¿Cómo y cuándo se conoce el resultado definitivo? ¿Hay ciclos de compensación? |
| INT-11 | Plataforma | Proveedor KYC/Biométrico | Verificación de identidad del solicitante | Imagen facial y evidencia de prueba de vida, imágenes del documento de identidad; resultado y puntaje de la verificación | **Sensible** | Solicitud y resultado | Alta (bloquea la vinculación) | Por validar. Puede incluir un componente (SDK) en la app móvil y un servicio del proveedor | S16, S8 | ¿Dónde procesa y almacena los datos el proveedor? ¿Qué retención aplica? (Q-06) |
| INT-12 | Plataforma | Proveedores de Notificaciones | Notificación de movimientos | Destinatario, tipo de movimiento, monto y datos mínimos necesarios | Confidencial — financiera (se minimiza) | Solicitud de la plataforma | Alta (R4) | Por validar. Desacoplada de la transacción | S17, S14 | ¿Qué contenido mínimo exige la norma? ¿Qué canales? (Q-10) |
| INT-13 | Plataforma | Proveedores de Notificaciones | Notificación del estado de la vinculación | Destinatario y estado | Confidencial — personal | Solicitud de la plataforma | Media | Por validar | S17 | — |
| INT-14 | Proveedores de Notificaciones | Cliente bancario y solicitante | Entrega de la notificación | Mensaje | Según el contenido (minimizado) | Entrega al dispositivo o al buzón | Alta | Push, SMS o correo (canales; proveedor no seleccionado) | S17 | Confirmaciones de entrega y reintentos |
| INT-15 | Core Bancario | Plataforma | Aviso de movimientos originados en otros canales | Movimiento, cuenta, cliente | Confidencial — financiera | Entrante | Alta (si R4 aplica a todo movimiento) | **Por validar.** No se asume que el Core emite eventos | — | Q-04 |
| INT-16 | Plataforma y cliente | Proveedor de Identidad | Autenticación y emisión de tokens OAuth 2.0 / OpenID Connect | Credenciales del cliente, tokens, información de la sesión | **Sensible** | Bidireccional | **Crítica** | Por validar. La frontera (externo o interno) está pendiente | S9 | Q-03 |
| INT-17 | Plataforma | Fuente Oficial de Identidad | Contraste de datos de identificación | Número de identificación y datos de identidad | Confidencial — personal | Solicitud y respuesta | Media | Por validar. Candidata; requiere autorización legal | — | Q-07 |

## 4. Observaciones

- **Integraciones críticas:** INT-04, INT-05, INT-06, INT-09, INT-10 e INT-16 concentran el riesgo de integridad financiera o de identidad. Deben diseñarse en la Fase 3 con las reglas de [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) y [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md).
- **Datos sensibles hacia terceros:** INT-11 e INT-16 (si el IdP es externo) envían datos sensibles fuera de la institución. Están condicionadas por [ADR-0010](adr/ADR-0010-residencia-de-datos.md).
- **Minimización en las notificaciones:** los mensajes SMS y de correo no son canales confidenciales. El contenido de INT-12 debe minimizarse (ver [14](14-limites-de-confianza.md)).
- **Topología de transferencias:** en el modelo B, INT-09 e INT-10 podrían convertirse en operaciones de la plataforma hacia el Core, y el catálogo se ajustaría.
