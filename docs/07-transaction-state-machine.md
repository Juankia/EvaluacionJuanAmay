# 07 — Máquina de estados transaccional durable

| Campo | Valor |
|---|---|
| Fase | 1. Modelo conceptual. **La implementación se diseña en fases posteriores** |
| Estado | Modelo conceptual adoptado |
| ADR principales | [ADR-0005](adr/ADR-0005-transaction-state-machine.md), [ADR-0006](adr/ADR-0006-idempotency-reconciliation.md), [ADR-0009](adr/ADR-0009-core-authoritative.md) |

## 1. Propósito

Las operaciones financieras (transferencias propias e interbancarias) y el onboarding digital tienen pasos con sistemas externos y fallas parciales. Además, un timeout no siempre significa falla ([05, sección 6](05-core-integration-boundary.md#6-regla-crítica-un-timeout-no-equivale-a-una-falla)). Para controlarlos se adopta una **máquina de estados transaccional durable** (*Durable Transaction State Machine*), también llamada **orquestador de transacciones de transferencia** (*Transfer Transaction Orchestrator*).

«Durable» significa que el estado de la operación se persiste **antes** de cada interacción con un sistema externo crítico. Si un proceso se reinicia, el estado de la operación no se pierde y la operación puede retomarse o conciliarse.

## 2. Por qué no se usa «Saga» como nombre genérico

El patrón Saga coordina transacciones distribuidas como una secuencia de transacciones locales, y cada paso tiene una **acción compensatoria** que deshace su efecto si un paso posterior falla. No se adopta como nombre ni como modelo genérico del flujo de transferencias por estas razones:

1. **Las operaciones financieras no siempre se compensan con una transacción inversa simple.** Un reverso es una **nueva** transacción contable con efectos propios: aparece en el estado de cuenta, puede generar comisiones o notificaciones, tiene implicaciones de auditoría y de reporte, y puede fallar a su vez.
2. **Algunos pasos no son reversibles unilateralmente.** Una transferencia interbancaria aceptada por el sistema de pagos no puede «deshacerse» desde la plataforma; su devolución sigue procedimientos propios entre instituciones.
3. **El estado dominante del problema no es «falló y compensar», sino «no sé qué pasó»** (`UNKNOWN`). Saga no modela de forma natural la incertidumbre del resultado ni la conciliación.
4. **El término induce a error en la revisión.** Presupone que existe una compensación automática segura para cada paso, y en banca esa suposición es peligrosa.

Cuando haga falta una compensación (por ejemplo, un reverso contable), se modelará como un **estado explícito, auditado y autorizado** de la máquina de estados, no como un efecto implícito del patrón.

## 3. Máquina de estados de transferencias (conceptual)

### 3.1 Estados

| Estado | Significado | ¿Terminal? |
|---|---|---|
| `CREATED` | Solicitud registrada y persistida con su clave de idempotencia. Sin efectos externos | No |
| `VALIDATING` | Validaciones de dominio y de seguridad (límites, beneficiario, autorización reforzada si aplica). Sin efectos financieros | No |
| `CORE_PROCESSING` | Se envió la operación al Core. Desde aquí **puede haber efecto financiero** | No |
| `SPI_PROCESSING` | Solo para transferencias interbancarias: la operación está en el sistema de pagos (S13) | No |
| `COMPLETED` | Resultado confirmado como exitoso | Sí |
| `FAILED` | Resultado confirmado como fallido **sin efecto financiero**, o con efecto revertido y conciliado | Sí |
| `UNKNOWN` | No se conoce el resultado (timeout de lectura, respuesta perdida, error ambiguo) | No |
| `REQUIRES_RECONCILIATION` | La conciliación automática no pudo determinar el resultado o encontró una discrepancia. Requiere intervención operativa | No (sale por resolución manual autorizada y auditada) |

### 3.2 Transiciones conceptuales

```text
CREATED ──► VALIDATING ──► CORE_PROCESSING ──► COMPLETED                    (transferencia propia)
                                       └─────► SPI_PROCESSING ──► COMPLETED (transferencia interbancaria)

VALIDATING      ──► FAILED                    (rechazo de validación; sin efectos externos)
CORE_PROCESSING ──► FAILED                    (rechazo de negocio explícito del Core, p. ej. fondos insuficientes)
CORE_PROCESSING ──► UNKNOWN                   (timeout de lectura o respuesta ambigua)
SPI_PROCESSING  ──► FAILED                    (rechazo confirmado; requiere tratamiento del débito previo; ver 3.4)
SPI_PROCESSING  ──► UNKNOWN                   (resultado del sistema de pagos no confirmado)
UNKNOWN         ──► COMPLETED | FAILED        (resuelto por conciliación)
UNKNOWN         ──► REQUIRES_RECONCILIATION   (conciliación no concluyente o plazo T_RESOLUCION_UNKNOWN vencido)
REQUIRES_RECONCILIATION ──► COMPLETED | FAILED  (resolución manual autorizada y auditada)
```

### 3.3 Reglas

1. **Se persiste antes de invocar.** La transición a `CORE_PROCESSING` se persiste **antes** de enviar la solicitud al Core (ver [08](08-idempotency-and-reconciliation.md)).
2. **No se reintenta a ciegas** desde `CORE_PROCESSING` ni desde `SPI_PROCESSING`.
3. **Indisponibilidad previa a la invocación:** si el Core no está disponible antes de enviar la solicitud (circuito abierto o error de conexión), la operación pasa a `FAILED` sin efectos y el cliente puede reintentarla de forma explícita. **No se encola** para ejecución diferida.
4. **Solo los estados terminales confirmados generan la notificación definitiva** al cliente. Los estados `UNKNOWN` y `REQUIRES_RECONCILIATION` se comunican como «en verificación».
5. **Cada transición se audita** con su causa, su identificador de correlación y su clave de idempotencia.

### 3.4 Aspectos abiertos (fases posteriores)

- La secuencia exacta de la transferencia interbancaria (débito previo, retención de fondos o débito al confirmar) depende de cómo el Core y la institución participan en el sistema de pagos (S13). El tratamiento de un rechazo posterior al débito (reverso contable) se diseñará como un estado explícito.
- La tecnología de implementación no se decide: motor de flujos de trabajo, tabla de estados con un proceso de avance, u otra opción (DD-08).
- Los tiempos máximos por estado y las alertas operativas se definirán con operaciones.

## 4. Máquina de estados de onboarding (conceptual)

### 4.1 Principio

**El Core es autoritativo para la creación del cliente** ([ADR-0009](adr/ADR-0009-core-authoritative.md)). La plataforma no considera «cliente» a una persona hasta que el Core confirma su creación.

### 4.2 Estados

| Estado | Significado |
|---|---|
| `CREATED` | Solicitud de onboarding iniciada desde la app móvil |
| `VALIDATING` | Validaciones de KYC y verificación biométrica (reconocimiento facial) con proveedores externos |
| `PENDING_CORE` | Las validaciones fueron aprobadas y la creación en el Core está pendiente o en curso |
| `CORE_CREATED` | El Core confirmó la creación del cliente y devolvió su identificador |
| `ACTIVE` | Credenciales y métodos de acceso registrados; el cliente puede operar |
| `REJECTED` | Las validaciones de KYC o de biometría no se aprobaron |
| `CORE_FAILED` | El Core rechazó la creación de forma explícita y confirmada |
| `CORE_UNKNOWN` | Se desconoce el resultado de la creación en el Core (timeout) |
| `MANUAL_REVIEW` | Requiere revisión operativa: discrepancias, conciliación no concluyente o reintentos agotados |

### 4.3 Transiciones conceptuales

```text
CREATED ──► VALIDATING ──► PENDING_CORE ──► CORE_CREATED ──► ACTIVE
                 └──► REJECTED
PENDING_CORE ──► CORE_FAILED  ──► (reintento controlado con la misma clave) PENDING_CORE | MANUAL_REVIEW
PENDING_CORE ──► CORE_UNKNOWN ──► (conciliación) CORE_CREATED | PENDING_CORE | MANUAL_REVIEW
```

### 4.4 Reglas

1. **La idempotencia es obligatoria en el intento de creación en el Core.** Cada intento usa la clave de idempotencia de la solicitud de onboarding. Si el Core no la soporta (S3), antes de reintentar se consulta si ya existe un cliente con el mismo identificador de persona (por ejemplo, el número de identificación). Esto evita crear clientes duplicados.
2. **Los reintentos solo se hacen después de la conciliación.** Desde `CORE_UNKNOWN` no se reintenta sin verificar antes si el cliente ya fue creado.
3. **El sistema complementario puede guardar la información pendiente del onboarding,** pero **no debe convertirse silenciosamente en la fuente autoritativa** de la identidad del cliente. La identidad del cliente es la que confirma el Core.
4. **Conciliación obligatoria:** se debe conciliar periódicamente:
   - las solicitudes en `PENDING_CORE` o `CORE_UNKNOWN` frente a los clientes creados en el Core;
   - los datos guardados en el sistema complementario frente a la identidad del Core.

   El objetivo es detectar registros huérfanos o duplicados.
5. **Los datos biométricos y de KYC** se tratan según la política de datos sensibles. Su almacenamiento, retención y ubicación se deciden en la fase de seguridad y en [ADR-0010](adr/ADR-0010-data-residency.md).

## 5. Lo que este documento no decide

- La tecnología del motor de estados ni la de su persistencia (DD-08).
- Los proveedores de KYC y biometría (fase de seguridad e identidad).
- Los canales de notificación (fase de notificaciones).
- El despliegue de la máquina de estados (fase de contenedores).
