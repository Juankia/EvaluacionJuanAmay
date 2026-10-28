# 29 — Seguridad, identidad y cumplimiento

| Campo | Valor |
|---|---|
| Documento | Autenticación, vinculación con KYC y biometría, arquitectura de seguridad y marco normativo |
| Requisitos | R2, R3, C1, C4 |
| Decisiones | ADR-0009, ADR-0013, ADR-0014, ADR-0019, ADR-0020, [ADR-0023](adr/ADR-0023-autenticacion-y-sesiones.md) |
| Diagrama | [Arquitectura de seguridad e identidad](../diagrams/seguridad/arquitectura-de-seguridad.svg) |
| Fecha | 2026-10-27 |
| Advertencia | Diseño conceptual. **No certifica cumplimiento normativo.** Los productos y parámetros se confirman en la fase de seguridad (DD-03, DD-04, DD-14) |

---

## 1. Autenticación (R2)

Flujo recomendado: **OAuth 2.0 / OpenID Connect con Authorization Code + PKCE** para todos los clientes. Se descartan el flujo implícito y el de contraseña del propietario del recurso (ROPC). El detalle está en [ADR-0023](adr/ADR-0023-autenticacion-y-sesiones.md).

| Aspecto | Web (SPA) | Móvil |
|---|---|---|
| Tipo de cliente OAuth | **El BFF** es cliente confidencial; la SPA no es cliente OAuth | Cliente público |
| Flujo | Authorization Code + PKCE, ejecutado por el BFF | Authorization Code + PKCE con navegador del sistema (no WebView incrustado) |
| Dónde viven los tokens | Cifrados en el almacén de sesiones del BFF; **el navegador solo recibe la cookie de sesión** (ADR-0019) | Almacenamiento seguro del dispositivo (Keychain / Keystore con respaldo de hardware si existe) |
| Sesión | Cookie `__Host-`, `Secure`, `HttpOnly`, `SameSite=Strict`; protección CSRF; vencimiento por inactividad y absoluto | Token de acceso corto y token de renovación rotado |
| Token de acceso | Vida corta (parámetro `TTL_ACCESS_TOKEN`, por acordar) | Igual; vinculación al dispositivo con DPoP como candidato (DD-04) |
| Token de renovación | Rotación con detección de reutilización: una reutilización revoca toda la familia (ADR-0019) | Igual |
| Revocación | Cierre de sesión en el BFF y en el IdP; los eventos de seguridad del IdP invalidan todas las sesiones del sujeto | Revocación en el IdP; el Gateway rechaza tokens revocados según la estrategia elegida (introspección o vida corta) |

**Preservación de ADR-0019:** el modelo de sesión del lado del servidor conserva sus propiedades aprobadas. Ningún token llega al navegador, la sesión se revoca por sujeto, el proceso del BFF no guarda estado y la cookie no es accesible desde JavaScript.

### 1.1 MFA y autenticación reforzada

| Evento | Exigencia |
|---|---|
| Primer inicio de sesión en un dispositivo nuevo | MFA |
| Operaciones sensibles: transferencias (cuando se habiliten), alta de beneficiarios, cambio de datos de contacto, registro de dispositivos | Autenticación reforzada (*step-up*) con `acr` y `max_age`; el servicio de dominio verifica el `acr` del token |
| Señales de riesgo (dispositivo, geolocalización, velocidad) | Reto adicional o bloqueo, según el motor de riesgo (§4.8) |

**Factores candidatos:** biometría local que desbloquea una clave del dispositivo vinculada en el IdP (estilo FIDO2/passkey), OTP por app y, como último recurso, OTP por SMS. El SMS no es factor único para operaciones de alto riesgo.

### 1.2 Recuperación de cuenta

La recuperación es el vector de toma de cuenta (ATO) más frecuente, por eso:

- exige volver a verificar la identidad (KYC + prueba de vida) o pasar por un canal presencial;
- aplica un periodo de enfriamiento con límites reducidos para las operaciones sensibles;
- notifica por todos los canales registrados;
- revoca las sesiones y los dispositivos anteriores.

### 1.3 Autorización

| Nivel | Control |
|---|---|
| Borde (Gateway) | Firma, emisor, audiencia, vigencia y alcances del token |
| BFF | Asociación sesión–sujeto; CSRF |
| Servicio de dominio | **Propiedad del recurso en cada solicitud**, según el Core (vía ACL). Un recurso ajeno responde `RESOURCE_NOT_FOUND` para impedir la enumeración (25 §2.3). ARG03-M01: relación cliente–cuenta desde el Core, caché corta y falla cerrada |
| Personal | IdP corporativo (candidato); cuatro ojos en la consola de operaciones |

## 2. Vinculación digital con KYC y biometría (R3)

### 2.1 Flujo conceptual

| # | Etapa | Dónde | Estado |
|---|---|---|---|
| 1 | El solicitante inicia la vinculación en la app | App → Gateway → BFF → vinculación | **Implementado** (local) |
| 2 | Captura de la evidencia de identidad (documento y rostro) con el SDK del proveedor | App + proveedor KYC | Diseñado; SDK por seleccionar (DD-14, PF4-07) |
| 3 | Aviso de privacidad y consentimiento expreso para tratar datos biométricos | App; constancia en vinculación y auditoría | Diseñado. La API exige `consentAccepted=true` (**implementado**) |
| 4 | Verificación KYC: comparación facial 1:1 con el documento y prueba de vida | Proveedor KYC; el resultado lo consulta **el servidor**, nunca la app | **Implementado con KYC simulado** (E1); proveedor real pendiente |
| 5 | Evaluación de riesgo y fraude (listas de control, señales del dispositivo, velocidad) | Servicio de vinculación + sistemas institucionales (Q-12) | Diseñado; integración por confirmar |
| 6 | Prueba de control independiente para **clientes preexistentes** (ADR-0009, DD-21) | Vinculación | Diseñado; **B-02** |
| 7 | Integración con el Core: búsqueda o alta del cliente | Vinculación → ACL → Core | **Alta BLOQUEADA (B-05)**; adaptador deshabilitado probado |
| 8 | Activación de la identidad digital **solo** con todas las aprobaciones previas | Vinculación → IdP (operación de vinculación acotada) | **No autorizada**; adaptador deshabilitado probado |

### 2.2 Cuatro conceptos que no deben confundirse

| Concepto | Qué demuestra | Dónde ocurre | Qué **no** demuestra |
|---|---|---|---|
| Verificación facial de identidad (1:1) | Que el rostro capturado corresponde al del documento | Proveedor KYC | Que la persona esté presente (sin prueba de vida) |
| Prueba de vida (*liveness*) | Que hay una persona real frente a la cámara, no una foto, un video ni una máscara | Proveedor KYC | Quién es la persona |
| Biometría local del dispositivo (huella, rostro del SO) | Que quien usa el teléfono puede desbloquear una credencial guardada en él | Dispositivo; **la plataforma no recibe datos biométricos** | Identidad legal; solo libera una clave del dispositivo |
| Autorización del servidor | Que el sujeto autenticado puede hacer **esa** operación sobre **ese** recurso | Servicios de dominio | — |

No se asume ninguna capacidad concreta de un proveedor biométrico (certificaciones, tasas de error, ubicación del procesamiento). Son criterios de selección para DD-14.

## 3. Modelo de amenazas (STRIDE resumido)

| Amenaza | Ejemplo | Controles |
|---|---|---|
| Suplantación | Robo de credenciales, *phishing*, SIM swap | MFA, autenticación reforzada, vinculación de dispositivo, SMS no como factor único, detección de ATO |
| Manipulación | Alterar montos o identificadores en tránsito | TLS; validación de entrada; idempotencia con huella del contenido (`IDEMPOTENCY_CONFLICT`) |
| Repudio | El cliente niega una operación | Auditoría A1–A8 de solo anexión con cadena de hash y correlación |
| Divulgación | Acceso a cuentas ajenas (IDOR); fuga en registros | Propiedad en cada solicitud; enmascaramiento en registros (**implementado**); cifrado en reposo |
| Denegación de servicio | Saturar el Core a través de la plataforma | WAF, limitación de tasa, presupuesto global de capacidad del Core (ADR-0011) |
| Elevación de privilegios | Abuso de la consola de operaciones; clave de firma comprometida | Cuatro ojos; IdP corporativo; firma de afirmaciones en HSM (PF4-05); operación de vinculación acotada (ADR-0013) |

## 4. Controles de seguridad (C4)

### 4.1 Clasificación de datos

| Clase | Ejemplos | Tratamiento |
|---|---|---|
| Restringida | Datos biométricos, credenciales, tokens, claves | No se almacenan en la plataforma (biometría) o solo cifrados (tokens); nunca en registros |
| Confidencial | Identificación, saldos, movimientos, contactos | Cifrado en reposo; identificación como HMAC (**implementado** en vinculación); caché solo según la matriz de cacheabilidad |
| Interna | Estados de operación, métricas | Control de acceso por rol |
| Pública | Contratos de API | — |

### 4.2 Cifrado

- **En tránsito:** TLS 1.2 o superior hacia el exterior y TLS mutuo entre servicios (identidad de carga de trabajo), además del enlace privado cifrado hacia el Core.
- **En reposo:** cifrado de las BD, de la caché, del broker, de los respaldos y del archivo con claves de KMS gestionadas por la institución.

### 4.3 KMS, HSM y secretos

- KMS para las claves de datos.
- HSM (CloudHSM o equivalente, candidato) para la firma de afirmaciones del IdP (PF4-05).
- Gestor de secretos con rotación; ningún secreto en el repositorio ni en las imágenes. La base ejecutable usa variables de entorno; `.env` está excluido de Git.

### 4.4 Mínimo privilegio

Una credencial de BD por servicio, roles de nube por carga de trabajo, sin acceso de la plataforma a la base de datos principal del Core (S12) y acceso humano a producción solo con elevación temporal auditada.

### 4.5 Protección de las API

WAF, limitación de tasa por cliente, validación de esquemas, `Idempotency-Key` obligatoria en los POST (**implementado**), errores uniformes sin datos internos (**implementado**) y rutas bloqueadas indistinguibles de recursos inexistentes (**implementado**).

### 4.6 Registro, auditoría y monitoreo

- **Registros:** estructurados, con correlación y sin datos sensibles (**implementado**).
- **Auditoría:** política por categoría (ADR-0020) con cadena de hash (**implementada**) y archivo WORM (diseñado).
- **Telemetría de seguridad:** CloudTrail, GuardDuty y Security Hub, con exportación al SIEM o SOC institucional si existe (Q-13).

### 4.7 Respuesta a incidentes

Plan con clasificación por severidad, contención (revocar sesiones por sujeto, rotar claves, activar la cuarentena financiera), preservación de evidencia (auditoría WORM), notificación a la autoridad y a los titulares en los plazos legales aplicables (§5) y análisis posterior.

### 4.8 Fraude y toma de cuentas

Señales de dispositivo y de comportamiento, límites por operación y por día, enfriamiento después de un cambio de credenciales o de dispositivo, notificación de cada movimiento (R4) e integración con los sistemas antifraude y de prevención de lavado de la institución (Q-12, por confirmar).

### 4.9 Seguridad de dependencias y contenedores

Imágenes mínimas y firmadas, análisis de vulnerabilidades en la integración continua, SBOM, versiones fijadas (el POM fija la plataforma Quarkus) y escaneo de secretos antes de cada commit.

### 4.10 Minimización y retención

- La plataforma no guarda biometría ni copias del documento. La evidencia KYC queda en el proveedor o en un almacén dedicado, según DD-14 y Q-06.
- Las identificaciones se guardan como HMAC.
- Los eventos llevan cargas mínimas.
- La retención se fija por categoría (Q-19); al vencer se elimina o se anonimiza.

## 5. Marco normativo (C1) — referencias, no certificación

| Referencia | Aplicación al diseño | Validación pendiente |
|---|---|---|
| **Ley Orgánica de Protección de Datos Personales (LOPDP, Ecuador)** y su reglamento | Base legal y consentimiento; los datos biométricos son datos sensibles; minimización; derechos del titular; evaluación de impacto para tratamientos de alto riesgo; notificación de vulneraciones; condiciones para las transferencias internacionales | Área legal: base de licitud, plazos, encargados (proveedor KYC y cloud) y transferencia internacional (ADR-0010). **No se asume una obligación de alojar los datos solo en Ecuador** |
| Normativa del organismo de control (Superintendencia de Bancos o SEPS, según Q-05) sobre seguridad de la información, ciberseguridad, canales electrónicos y continuidad del negocio | Autenticación reforzada, monitoreo, notificación de transacciones (R4), continuidad (doc 28) | Depende del tipo de institución (Q-05) |
| **PCI DSS** | **Condicional.** El alcance actual (consultas, transferencias entre cuentas y vinculación) **no procesa datos de tarjetas**. Si se agregan funciones con tarjetas, se segmenta el entorno de datos del titular y se aplica PCI DSS a ese segmento | Confirmar que ningún flujo maneja PAN |
| Buenas prácticas: OWASP ASVS y MASVS, perfiles de seguridad FAPI para OAuth | Criterios de diseño y de prueba | Se adoptan como guía, no como certificación |

## 6. Estado y pendientes

| Elemento | Estado |
|---|---|
| Flujo OAuth, sesiones, MFA y reforzada | Diseñado (ADR-0023); parámetros por acordar |
| Producto del IdP | Provisional: Keycloak como candidato (ADR-0013) |
| Proveedor KYC | Pendiente (DD-14, PF4-07) |
| Vinculación de tokens (DPoP o mTLS) | Pendiente (DD-04) |
| Sesión del solicitante de vinculación | Pendiente (PF4-01); hoy **sin autenticar** en la base local |
| Custodia en HSM | Pendiente (PF4-05) |
