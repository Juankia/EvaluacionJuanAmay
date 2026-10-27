# 22 — Seguridad entre contenedores e identidad digital

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Pendiente de ARG-02. **No se afirma ninguna certificación ni cumplimiento normativo** |
| ADR principales | [ADR-0009](adr/ADR-0009-core-autoritativo.md), [ADR-0013](adr/ADR-0013-frontera-del-idp.md), [ADR-0014](adr/ADR-0014-api-gateway-y-bff-de-canales.md), [ADR-0010](adr/ADR-0010-residencia-de-datos.md) |
| Relacionados | [14 — Límites de confianza](14-limites-de-confianza.md) |

## 1. Zonas y contenedores

| Zona (de [14](14-limites-de-confianza.md)) | Contenedores |
|---|---|
| TB-1 Canales públicos | SPA (en el navegador) y aplicación móvil (en el dispositivo) |
| TB-2 Plataforma | Gateway, BFF, IdP, servicios de dominio, almacenes, caché, broker, consola |
| TB-3 Sistemas existentes | Core, Sistema Complementario |
| TB-4 Terceros | Proveedor KYC/biométrico, proveedores de notificación (el IdP **ya no** está en TB-4: ADR-0013) |
| TB-5 Infraestructura financiera | Sistema Externo de Transferencias |

Dentro de TB-2 se distinguen **subzonas**:

- **Borde:** Gateway.
- **Aplicación:** BFF y servicios.
- **Datos:** almacenes, caché y broker.
- **Identidad:** IdP y su almacén.
- **Operación privilegiada:** consola.

## 2. Autenticación y autorización

| Aspecto | Diseño |
|---|---|
| Autenticación del cliente | En el IdP (OIDC). **Web:** el BFF es cliente confidencial y la SPA usa una sesión con cookie segura (`HttpOnly`, `Secure`, `SameSite`), con protección CSRF. **Móvil:** cliente público con Authorization Code + PKCE; tokens en el almacenamiento seguro del dispositivo |
| Flujo OAuth detallado, MFA, autenticación reforzada, vinculación de tokens (DPoP, mTLS) | **Fase de seguridad** (DD-03, DD-04). QA4 es el criterio de aceptación |
| Autorización de borde | El Gateway valida la firma, la vigencia, la audiencia y los alcances del token |
| Autorización de negocio | **Cada servicio de dominio** verifica la propiedad de las cuentas y del beneficiario (según el Core) en **cada** solicitud, incluidas las consultas por identificador de recurso (protección contra el acceso a objetos ajenos) |
| Sesiones | En el IdP y en el BFF (web), con vencimientos por inactividad y absolutos (parámetros de la fase de seguridad) |
| Autenticación entre servicios | Identidad de carga de trabajo y TLS mutuo (mecanismo con el entorno de ejecución). Credenciales de base de datos por servicio y de mínimo privilegio |
| Secretos | Gestor de secretos del proveedor cloud; ningún secreto en el repositorio ni en las imágenes |

## 3. Vinculación digital: punto de control del acceso (ADR-0009, R2-N08, DD-21)

### 3.1 Punto de control

**El acceso digital solo se activa en un lugar y por un camino:**

1. El IdP **no permite el auto-registro** ni la vinculación de cuentas a clientes del Core por iniciativa del usuario.
2. La **API de administración del IdP** que crea o activa una cuenta vinculada a un cliente es accesible **solo** con la credencial de servicio del **servicio de vinculación** (control de red + autenticación de servicio + alcance mínimo).
3. El servicio de vinculación invoca esa API **solo** en la transición `LINK_AUTHORIZED → DIGITAL_ACCESS_ACTIVE`, que exige:
   - **para `CREATED_BY_THIS_REQUEST`:** confirmación del alta o evidencia de creación (VD-02, VD-05, INV-O2), **nunca** asignada desde la revisión manual sin esa evidencia;
   - **para `PRE_EXISTING`:** prueba de control independiente (VD-03).
4. **Bloqueo en producción hasta DD-21:** la ruta `PRE_EXISTING → LINK_PENDING → LINK_AUTHORIZED` está **deshabilitada** (*fail-closed*) mientras DD-21 no esté implementada, probada y aprobada. Los clientes existentes usan los canales actuales de la institución hasta entonces.

### 3.2 Reglas adicionales que atienden R2-N08 (requisitos; mecanismo en DD-21)

| Regla | Propósito |
|---|---|
| El factor de la prueba de control debe tener una **antigüedad mínima** sin cambios (`ANTIGUEDAD_MIN_FACTOR`, por acordar). Un factor modificado recientemente no se acepta | Evitar el desvío por el cambio previo del contacto |
| Si no existe un factor confiable, **no hay vinculación remota**: vía alternativa explícita (por ejemplo, presencial) fuera del flujo digital | Evitar la presión para resolver en la revisión manual |
| La revisión manual (`MANUAL_REVIEW`) **no puede asignar** `CREATED_BY_THIS_REQUEST` sin evidencia de creación ni pasar a `LINK_AUTHORIZED` sin prueba de control | Cerrar el desvío por la vía manual |
| No enumeración (VD-07) | No revelar la condición de cliente |

### 3.3 KYC y biometría

- Las llamadas al proveedor se hacen **solo** desde el servicio de vinculación.
- El SDK del proveedor en la app captura la evidencia. La plataforma conserva **solo los resultados y las referencias** necesarios (Q-06, ADR-0010).
- Proveedor no seleccionado (DD-14).
- La **verificación de identidad no otorga** acceso a productos (E1 frente a E4).

## 4. Controles de fraude y monitoreo de seguridad

| Control | Estado |
|---|---|
| Limitación de tasa del borde por cliente y por tipo de operación | Diseñado (Gateway) |
| Controles de abuso del onboarding y del costo del proveedor KYC (M09) | **Abierto**; escenario de calidad pendiente |
| Monitoreo transaccional o de fraude institucional (Q-12) | **Pendiente**; si existe, se integra como sistema externo en la fase de seguridad |
| Eventos de seguridad (inicios de sesión, nuevos dispositivos, activaciones, resoluciones manuales) | Al servicio de auditoría y al SIEM institucional, si existe (Q-13) |

## 5. Protección de datos

- Cifrado en tránsito (TLS) en todas las relaciones. Cifrado en reposo en todos los almacenes, la caché y el broker.
- Minimización en las notificaciones (sin datos sensibles ni enlaces que soliciten datos).
- Registros sin datos sensibles ([00-estandares](00-estandares-de-ingenieria.md) §5.6).
- Acceso al almacén de auditoría solo para roles de auditoría, mediante la consola o las herramientas institucionales; nadie puede modificarlo ni borrarlo.
