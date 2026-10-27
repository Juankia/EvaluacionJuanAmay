# 08 — Idempotencia y conciliación

| Campo | Valor |
|---|---|
| Fase | 1. Modelo conceptual |
| Estado | Adoptado |
| ADR principal | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) |

## 1. Problema

Se asume que el Core no ofrece idempotencia nativa ni consulta por clave de idempotencia (S3). En ese caso, un reintento de una operación crítica después de un timeout puede **duplicar el efecto financiero**. A la vez, un timeout no prueba que la operación haya fallado ([05](05-frontera-de-integracion-core.md)). La plataforma necesita:

1. evitar que ella misma procese dos veces la misma intención del cliente;
2. determinar el resultado real de las operaciones con resultado desconocido.

## 2. Conceptos

| Concepto | Definición |
|---|---|
| **Idempotencia** | Propiedad de una operación por la cual ejecutarla varias veces produce el mismo efecto que ejecutarla una vez |
| **Clave de idempotencia** (*Idempotency Key*) | Identificador único de una intención de operación. Permite reconocer las repeticiones |
| **Clave de solicitud del canal** | Clave que el canal envía para protegerse del doble envío (doble clic, reintento de red del dispositivo). Su formato y su validación se definen en la fase de contenedores y de API |
| **Clave de operación del dominio** | Clave que **genera el dominio o la aplicación** al aceptar la operación. Es la que se persiste, se propaga y se usa en la conciliación |
| **Conciliación** | Proceso para determinar el resultado real de una operación comparando el registro de la plataforma con el del sistema externo |

## 3. Modelo adoptado

La plataforma:

1. **Genera la clave de idempotencia en la frontera del dominio o de la aplicación**, al aceptar la operación crítica. La clave de solicitud del canal se asocia a ella para detectar envíos duplicados.
2. **Persiste el estado de la operación antes de invocar** un sistema externo crítico (*write-ahead*). Si el proceso falla después de invocar, el registro permite saber que la invocación pudo ocurrir.
3. **Garantiza la unicidad dentro de la plataforma.** Por ejemplo, con una restricción de unicidad en el almacén de operaciones. Una segunda solicitud con la misma clave devuelve el estado de la operación existente y no inicia otra.
4. **Propaga la clave a los sistemas externos que la soporten,** como referencia externa o identificador de transacción del cliente. El Core y el sistema de pagos pueden aceptar una referencia única.
5. **Conserva la clave aunque el Core no la entienda,** como parte del registro y de la auditoría.
6. **Usa la clave en la conciliación interna** y la vincula con las referencias que devuelvan los sistemas externos.
7. **Si el Core no permite consultar por clave, concilia con datos de correlación de negocio** (sección 4).

```text
Canal ──(clave de solicitud)──► Servicio de transferencias
                                  │ 1. Genera la clave de operación
                                  │ 2. Persiste CREATED (unicidad)
                                  │ 3. Persiste CORE_PROCESSING
                                  ▼
                               ACL de transferencias ──(referencia externa, si se soporta)──► Core
                                  │
                       Timeout de lectura ──► UNKNOWN ──► Conciliación
```

## 4. Conciliación por correlación de negocio

Si el Core no ofrece consulta por clave o por referencia externa, la conciliación busca en el Core movimientos que coincidan con la operación usando atributos de correlación. Los atributos posibles son:

| Atributo | Uso |
|---|---|
| Cuenta de origen y cuenta de destino | Coincidencia obligatoria |
| Monto y moneda | Coincidencia obligatoria |
| Tipo de operación | Coincidencia obligatoria |
| Referencia de negocio o descripción | Si el Core conserva un campo de referencia libre, se inserta en él la clave de operación |
| Referencia externa devuelta por el Core o por el sistema de pagos | Si se obtuvo en algún paso |
| Ventana de tiempo | Movimientos dentro de una ventana alrededor del instante de invocación (`VENTANA_CORRELACION`, por acordar) |
| Identificadores de cliente o de cuenta | Cuando corresponda |

### 4.1 Resultados de la conciliación

| Resultado | Acción |
|---|---|
| Exactamente una coincidencia | Se resuelve como `COMPLETED` y se registra la evidencia de la coincidencia |
| Ninguna coincidencia, después de esperar el tiempo de asentamiento del Core (`T_ASENTAMIENTO_CORE`, por acordar) | Se resuelve como `FAILED` (sin efecto) |
| Varias coincidencias posibles (por ejemplo, dos transferencias legítimas del mismo monto al mismo destino en la ventana) | **No se resuelve automáticamente**: pasa a `REQUIRES_RECONCILIATION` |
| Ninguna coincidencia, pero el sistema de pagos indica actividad | `REQUIRES_RECONCILIATION` |

### 4.2 Limitación explícita

**La conciliación por correlación de negocio no ofrece las mismas garantías que la idempotencia nativa.**

- Es **probabilística**: dos operaciones legítimas pueden ser indistinguibles.
- Depende de que el Core **muestre el movimiento a tiempo**. Si el Core lo asienta tarde, una conclusión de «sin coincidencia» puede ser errónea.
- Su exactitud depende de la calidad de los campos disponibles en el Core.

Esta limitación se registra como riesgo residual **RR-04** ([11](11-riesgos-residuales.md)). La mitigación más eficaz es **validar el supuesto S3**: si el Core acepta y permite consultar una referencia externa única, la conciliación pasa a ser determinista.

## 5. Mitigaciones complementarias

- **Inhibir operaciones equivalentes mientras otra está en `UNKNOWN`:** mientras una operación del cliente está en `UNKNOWN`, la plataforma puede bloquear o advertir antes de aceptar otra operación con la misma cuenta de destino y el mismo monto. Así se reduce la ambigüedad en la conciliación. Es una decisión de experiencia de usuario y de negocio, por acordar.
- **Conciliación diaria** frente a los reportes del Core y del sistema de pagos, como control de segunda línea.
- **Monitoreo** del número de operaciones en `UNKNOWN` y en `REQUIRES_RECONCILIATION`, y de su antigüedad.

## 6. Onboarding

En la creación del cliente en el Core, la idempotencia es **obligatoria**. La conciliación se basa en el identificador de persona: antes de cualquier reintento se verifica si el cliente ya existe en el Core ([07, sección 4](07-maquina-de-estados-transaccional.md#4-máquina-de-estados-de-onboarding-conceptual)).
