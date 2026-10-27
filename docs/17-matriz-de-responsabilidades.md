# 17 — Matriz de responsabilidades por contenedor

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Pendiente de ARG-02 |
| Documentos relacionados | [16 — Arquitectura de contenedores](16-arquitectura-de-contenedores.md), [ADR-0012](adr/ADR-0012-descomposicion-hibrida-de-contenedores.md), [ADR-0014](adr/ADR-0014-api-gateway-y-bff-de-canales.md) |

## 1. Convención

- **R:** responsable; el contenedor dueño de la capacidad.
- **C:** colabora; invoca, compone o consume.
- **—:** no participa.

## 2. Capacidades frente a contenedores

| Capacidad | SPA / App | Gateway | BFF | IdP | Consultas | Vinculación | Transferencias | Notificaciones | Auditoría | Consola |
|---|---|---|---|---|---|---|---|---|---|---|
| Autenticación del cliente | C | C (valida tokens) | C (gestor de tokens de la SPA) | **R** | — | — | — | — | C | — |
| Autorización de borde (token válido, alcance) | — | **R** | — | C | — | — | — | — | — | — |
| Autorización de negocio (propiedad de cuentas, límites) | — | — | — | — | **R** (lecturas) | **R** (vinculación) | **R** (transferencias) | — | — | — |
| Consulta de cliente, cuentas y movimientos | C | C | C | — | **R** | — | — | — | C | — |
| Vinculación digital (E1 a E6) | C (app) | C | C | C (activación) | — | **R** | — | C | C | C |
| Verificación KYC y biométrica | C (SDK) | — | — | — | — | **R** | — | — | C | — |
| Activación del acceso digital | — | — | — | C (ejecuta) | — | **R** (autoriza) | — | C | C | — |
| Inicio de transferencias e idempotencia | C (genera la clave) | C | C (propaga sin cambios) | — | — | — | **R** | — | C | — |
| Ejecución financiera en el Core | — | — | — | — | — | — | **R** (provisional) | — | C | — |
| Conciliación y efecto tardío | — | — | — | — | — | **R** (alta) | **R** (transferencias) | C | C | C |
| Resolución manual y cierre excepcional | — | — | — | — | — | C | C | — | C | **R** (provisional) |
| Notificación de movimientos y de seguridad | C (recibe push) | — | — | C (eventos) | — | C | C (eventos) | **R** | C | — |
| Registro de auditoría | — | C (registros de acceso) | C | C | C | C | C | C | **R** | C |
| Caché de consultas | — | — | C (marca *read-your-writes*) | — | **R** | — | C (eventos) | — | — | — |
| Limitación de tasa del borde | — | **R** | — | — | — | — | — | — | — | — |
| Control de admisión hacia el Core | — | — | — | — | **R** (CL-3/CL-4) | **R** (CL-1/CL-2) | **R** (CL-1/CL-2) | — | — | — |

## 3. Lo que cada contenedor **no** hace

| Contenedor | Prohibido |
|---|---|
| API Gateway | Lógica de negocio; máquinas de estado; reintentos de escrituras financieras; transformación del modelo del Core |
| BFF | Invariantes financieros; generar, alterar o reutilizar claves de idempotencia; reintentar inicios de transferencia; decidir resultados |
| IdP | Crear o activar cuentas vinculadas a clientes sin una solicitud del servicio de vinculación; auto-registro |
| Servicio de consultas | Escrituras en el Core; usar la caché como fuente; inferir el resultado de transferencias |
| Servicio de vinculación | Activar el acceso de un cliente `PRE_EXISTING` sin la prueba de control (DD-21); reenviar el alta con evidencia negativa |
| Servicio de transferencias | Reenviar; cerrar con EV-INC; segundo reverso automático; usar la caché |
| Servicio de notificaciones | Revertir o confirmar operaciones; ser la fuente de un movimiento |
| Servicio de auditoría | Modificar o borrar registros; decidir sobre operaciones |
| Consola de operaciones | Crear operaciones financieras en nombre del cliente (PT-05); resolver sin la evidencia de INV-T2 o sin cuatro ojos |
