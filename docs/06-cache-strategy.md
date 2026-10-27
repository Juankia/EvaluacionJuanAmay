# 06 — Estrategia de caché

| Campo | Valor |
|---|---|
| Fase | 1. Se decide ahora porque el reto exige explícitamente un mecanismo de caché (R5) |
| Estado | Patrón adoptado: **Cache-Aside**. La tecnología de caché se difiere (DD-07) |
| ADR principales | [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0008](adr/ADR-0008-event-driven-invalidation.md) |

## 1. Requisito del reto

> «[…] más un mecanismo de caché para clientes frecuentes basado en patrones de diseño, identifique los componentes involucrados.» (R5)

**Interpretación:** «clientes frecuentes» se entiende como los clientes que acceden de forma recurrente al canal. Su información de consulta reiterada (perfil, productos, movimientos recientes) se beneficia de la caché. Cache-Aside retiene de forma natural los datos más solicitados. Una precarga al iniciar sesión (*warm-up*) es una optimización opcional que se evaluará con mediciones, no por defecto.

## 2. Patrón adoptado: Cache-Aside

Con **caché bajo demanda** (*Cache-Aside*), la aplicación gestiona la caché de forma explícita:

```text
Lectura:
  1. El servicio consulta la caché con una clave que incluye al cliente.
  2. Si encuentra el dato (acierto): lo devuelve, si cumple la política de frescura.
  3. Si no lo encuentra (fallo): lo consulta mediante la ACL al Core o al sistema complementario,
     lo guarda en la caché y lo devuelve.

Escritura (operación de la plataforma que cambia el estado):
  1. La operación se ejecuta contra el sistema autoritativo (Core).
  2. Las entradas afectadas se invalidan. No se actualizan con el valor calculado localmente.
```

### 2.1 Por qué Cache-Aside y no otros patrones

| Patrón | Evaluación |
|---|---|
| **Cache-Aside** | **Seleccionado.** El Core es el sistema autoritativo y externo (S2). La plataforma no controla sus escrituras, por lo que no puede garantizar una caché de escritura simultánea. Cache-Aside permite decidir por categoría de dato si se cachea y cómo se refresca. Si la caché falla, se degrada a una consulta directa con protección del Core |
| Read-Through | Equivalente en comportamiento, pero delega la carga en el proveedor de caché. Oculta en la infraestructura una decisión que aquí es de dominio |
| Write-Through / Write-Behind | **No aplicable** al dato del Core: la plataforma no es la dueña de la escritura. Write-Behind sobre datos financieros introduce riesgo de pérdida |
| Refresh-Ahead | Posible optimización futura para categorías estables, si las mediciones lo justifican |

### 2.2 Principios

1. **La caché es una optimización, no una fuente de verdad.** Ante cualquier duda prevalece el Core (S2).
2. **La cacheabilidad depende del dato.** No existe un TTL global.
3. **El TTL es un mecanismo de seguridad**, no automáticamente el mecanismo principal de consistencia. La consistencia preferida se obtiene por invalidación (sección 4). El TTL acota el daño si la invalidación falla.
4. **Ninguna decisión transaccional se toma con datos de caché.** Por ejemplo, la validación de fondos para una transferencia siempre la hace el Core.
5. **La caché no debe ser un punto único de falla:** si no está disponible, el servicio consulta la fuente con la protección de la ACL (Circuit Breaker y límite de concurrencia).
6. **La caché contiene datos personales:** requiere cifrado en tránsito y en reposo, claves aisladas por cliente y control de acceso. El detalle se define en la fase de seguridad.

## 3. Matriz de cacheabilidad

Los valores de TTL **no se fijan en esta fase**. El TTL de seguridad de cada categoría debe acordarse con negocio y con riesgo según el impacto de mostrar un dato desactualizado. Se representa con parámetros (por ejemplo, `TTL_SEG_PERFIL`).

| Categoría de dato | Fuente autoritativa | Sensibilidad a la consistencia | ¿Cacheable? | Estrategia de frescura | Invalidación preferida | ¿Se sirve si el Core no está disponible? |
|---|---|---|---|---|---|---|
| Datos relativamente estáticos del cliente (nombre, datos de contacto del perfil, segmento) | Core y sistema complementario | Baja | **Sí, potencialmente** | Cache-Aside con TTL de seguridad `TTL_SEG_PERFIL` | Evento de cambio de datos del cliente, si la fuente lo emite. Si no, TTL | Sí, con indicador de frescura |
| Datos extendidos del cliente (sistema complementario) | Sistema complementario | Baja o media | **Sí, potencialmente** | Cache-Aside con TTL de seguridad `TTL_SEG_EXTENDIDOS` | Evento de la fuente, si existe. Si no, TTL | Sí, con indicador |
| Catálogo de productos e información de referencia (tipos de cuenta, catálogos de instituciones financieras para transferencias) | Core o catálogos institucionales | Baja | **Sí** | Cache-Aside con TTL de seguridad `TTL_SEG_CATALOGO`; refresco programado opcional | Publicación de una nueva versión del catálogo | Sí |
| Información de cuentas (lista de cuentas, tipo, estado) | Core | Media: un bloqueo o cierre de cuenta debe reflejarse | **Condicional** | Cache-Aside con TTL de seguridad corto `TTL_SEG_CUENTAS`. El estado de la cuenta se revalida contra el Core antes de cualquier operación transaccional | Evento de cambio de estado de la cuenta; invalidación tras operaciones propias | Solo para visualización, con indicador. Nunca para autorizar una operación |
| **Saldo disponible** | Core | **Muy alta** | **Solo para visualización, con condiciones**; nunca para decisiones | Lectura desde el Core cuando es posible. Si se cachea para visualización: invalidación tras cualquier operación del cliente y garantía de «leer lo propio» (*read-your-writes*) en la sesión | `AccountBalanceChanged` (conceptual) e invalidación inmediata tras una transferencia propia | Decisión de negocio: mostrar el último saldo conocido con fecha y hora, o no mostrarlo. Por defecto, no mostrarlo como saldo disponible actual |
| Movimientos recientes | Core | Alta: el cliente espera ver su última operación | **Condicional** | Cache-Aside paginado con TTL de seguridad `TTL_SEG_MOVIMIENTOS` e invalidación tras operaciones propias (*read-your-writes*) | Evento de nuevo movimiento en la cuenta | Sí, con indicador de frescura explícito |
| Movimientos históricos (periodos cerrados) | Core | Baja: los periodos cerrados no deberían cambiar, salvo reversos | **Sí** | Cache-Aside con TTL de seguridad más amplio `TTL_SEG_HISTORICO` | Evento de reverso o ajuste, si existe | Sí |
| **Estado de una transacción** | Máquina de estados de la plataforma y Core | **Muy alta** | **No** en la caché de consulta | Se lee desde el almacén durable de la máquina de estados ([07](07-transaction-state-machine.md)) | No aplica | Se lee del almacén propio de la plataforma |
| **Credenciales, OTP, códigos de verificación, secretos de sesión** | IdP o servicio de autenticación | Seguridad | **Nunca** en esta caché | No aplica | No aplica | No aplica |
| **Plantillas o secretos biométricos** | Proveedor biométrico o almacén seguro designado | Seguridad (dato sensible) | **Nunca** en esta capa | No aplica | No aplica | No aplica |
| Tokens de acceso | IdP | Seguridad | **No** en la caché de datos de negocio. Una caché de validación de tokens en el Gateway, si existe, es una decisión de la fase de seguridad | No aplica | No aplica | No aplica |

## 4. Invalidación de caché

### 4.1 El problema

Un TTL por sí solo produce una ventana de inconsistencia: el cliente realiza una transferencia, regresa a la pantalla de movimientos y no la ve. Esto deteriora la confianza en el canal y genera reclamos.

### 4.2 Estrategia conceptual preferida: invalidación dirigida por eventos

```text
Transferencia completada
        │
        ▼
Cambia el estado de la cuenta
        │
        ▼
Evento AccountBalanceChanged (conceptual)
        │
        ▼
Se invalidan las entradas de caché afectadas (saldo y movimientos de esa cuenta)
```

### 4.3 Lo que no se decide en esta fase

- **No se selecciona la tecnología del bus de eventos.** Kafka, Amazon MSK, Azure Service Bus, Event Hubs u otras alternativas se evaluarán en la fase de infraestructura y plataforma, solo si los requisitos asíncronos se confirman (DD-01, [ADR-0008](adr/ADR-0008-event-driven-invalidation.md)).
- **No se asume que el Core emite eventos.** Si el Core no publica cambios (por ejemplo, depósitos en ventanilla), la plataforma solo puede invalidar con certeza lo que ella misma origina. Para los cambios externos, el TTL de seguridad es el mecanismo principal (RR-05).

### 4.4 Estrategias de respaldo

| Situación | Respaldo |
|---|---|
| Cambio originado por la plataforma (transferencia propia) | Invalidación síncrona de las entradas propias tras la confirmación, sin depender del bus, más *read-your-writes* en la sesión del cliente |
| Cambio originado fuera de la plataforma (otro canal, ventanilla, procesos batch) | TTL de seguridad por categoría y eventos del Core, si se confirma que existen |
| Falla en la entrega del evento de invalidación | El TTL de seguridad acota la ventana de inconsistencia |
| Falla de la caché | Consulta directa mediante la ACL, con protección del Core |

### 4.5 Trade-off

| Opción | Ventaja | Costo |
|---|---|---|
| Solo TTL | Simple, sin dependencia de un bus | Ventana de inconsistencia igual al TTL. Presiona a acortar los TTL y eso aumenta la carga sobre el Core |
| Invalidación por eventos + TTL de seguridad | Consistencia más ajustada con TTL más amplios | Requiere infraestructura de eventos y entrega confiable (por ejemplo, Outbox). Agrega modos de falla |
| Invalidación síncrona local + TTL (respaldo mínimo) | No requiere bus para las operaciones propias | No cubre los cambios externos a la plataforma |

## 5. Componentes involucrados (identificación preliminar)

El reto pide identificar los componentes. A nivel conceptual son los siguientes; su detalle se define en el diagrama de componentes de una fase posterior:

| Componente conceptual | Responsabilidad |
|---|---|
| Servicio de dominio (clientes, movimientos) | Aplica Cache-Aside y decide la política por categoría |
| Política de frescura por categoría | Configuración declarativa: cacheable o no, TTL de seguridad, uso en degradación |
| Almacén de caché | Tecnología diferida (DD-07). Debe cumplir los requisitos de seguridad y disponibilidad |
| ACL | Fuente en caso de fallo de caché, con protección del Core |
| Mecanismo de invalidación | Local síncrono (mínimo) o dirigido por eventos (diferido) |
| Observabilidad | Tasa de aciertos, latencia, llamadas al Core evitadas y antigüedad de los datos servidos |
