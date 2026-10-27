# 08 — Idempotencia y conciliación

| Campo | Valor |
|---|---|
| Fase | 1. Modelo conceptual. Versión 2: remediación ARG-01 (ARG01-C01, ARG01-M01). Versión 3: remediación de CF-01 y CF-02 (ARG-01-R2) |
| Estado | Adoptado. Pendiente de verificación en ARG-01-R2 |
| ADR principal | [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md) (contrato normativo IC-01 a IC-14) |

## 1. Problema

Se asume que el Core no ofrece idempotencia nativa ni consulta por referencia (S3). Un timeout no prueba que la operación haya fallado. La plataforma necesita:

1. que **cada intención transaccional** del cliente produzca **como máximo una operación y una invocación de ejecución**, incluso ante reenvíos, concurrencia y reinicios;
2. determinar el resultado real de las operaciones inciertas;
3. **declarar con honestidad** lo que no puede garantizar.

La versión 1 no cumplía el punto 1: la clave nacía en el dominio y cada reenvío del cliente podía crear otra operación (ARG01-C01).

## 2. Conceptos

| Concepto | Definición |
|---|---|
| **Intención transaccional** | La decisión del usuario de ejecutar una operación concreta, expresada al confirmarla en el canal |
| **Clave de idempotencia** (*Idempotency Key*) | Identificador aleatorio que el **canal** genera una vez por intención, persiste y reutiliza en todo reenvío. **Obligatoria** en las operaciones críticas |
| **Ámbito** | (Identidad autenticada o sesión de vinculación; tipo de operación). La clave solo tiene sentido dentro de su ámbito |
| **Huella canónica** (*fingerprint*) | Hash criptográfico de la representación canónica de los campos semánticos de la operación |
| **Identificador interno de operación** | Asignado por la plataforma, vinculado 1:1 con (ámbito, clave). Se propaga al Core como referencia externa si se soporta |
| **Deduplicación (G1)** | Garantía determinista de la plataforma: una (ámbito, clave) produce como máximo una operación y una invocación por tramo |
| **Idempotencia del Core (G2)** | El Core reconoce una referencia repetida. **No se asume** (S3) |
| **Correlación de negocio (G3)** | Inferencia probabilística de que una operación se aplicó |
| **Conciliación (G4)** | Proceso que resuelve los resultados inciertos o los escala |
| **Duplicado semántico** | Dos operaciones con **claves distintas** que representan la misma intención financiera |

## 3. Contrato de idempotencia (resumen)

La definición normativa está en [ADR-0006, sección 2](adr/ADR-0006-idempotencia-y-conciliacion.md#2-contrato-de-idempotencia-para-operaciones-financieras-críticas).

```text
Canal                                      Plataforma
──────────────────────────────────────    ──────────────────────────────────────────────
Usuario confirma la operación
  └─ genera la clave K (una vez)
  └─ persiste K en el dispositivo
Envía (K, payload) ─────────────────────► Valida el formato de K (IC-03); ámbito ← identidad autenticada (IC-04)
                                           Calcula la huella H (IC-05)
                                           Reserva atómica [(ámbito, K), H, opId, RECEIVED] (IC-06, IC-07)
                                             ├─ nueva                    → ejecuta según ADR-0005
                                             ├─ existe y huella = H      → devuelve el estado actual (IC-08)
                                             └─ existe y huella ≠ H      → conflicto sin efectos (IC-09)
Sin respuesta (red) ─ reenvía (K, payload) ► Reserva falla por unicidad → devuelve el estado actual (IC-08)
```

| Situación | Resultado |
|---|---|
| Reenvío con la misma clave y el mismo contenido | Estado actual de la operación existente, **sin nueva ejecución** |
| Misma clave y contenido distinto | **Conflicto**, sin efectos, auditado |
| Dos solicitudes concurrentes con la misma clave | Una sola reserva; la otra recibe el estado o el conflicto |
| Operación existente en `*_UNKNOWN` o en `REQUIRES_RECONCILIATION` | «En verificación». La clave **no se libera** (IC-12) |
| Solicitud sin clave o con clave inválida | Rechazo sin efectos |
| Clave de otro cliente | Se trata dentro del ámbito de quien la presenta; no revela la operación ajena |

## 4. Duplicado semántico (claves distintas, misma intención)

Una clave distinta **no prueba** que la intención sea distinta. Mitigaciones ([ADR-0006, sección 3](adr/ADR-0006-idempotencia-y-conciliacion.md#3-duplicado-semántico-claves-distintas-para-la-misma-intención-financiera)):

1. **Bloqueo obligatorio (IC-15, versión 3):** no puede existir más de una operación **no terminal** equivalente (misma huella sin clave) por cliente, en ningún estado. Se garantiza de forma **atómica** con la reserva.
2. **Confirmación explícita:** si hay una operación equivalente aceptada dentro de `VENTANA_DUPLICADO_SEMANTICO`, se pide la confirmación del usuario.
3. **Riesgo residual (RR-13):** los duplicados confirmados por el usuario o los que quedan fuera de la ventana no se previenen; se miden con `TASA_DUPLICADO_SEMANTICO`.

## 5. Conciliación

### 5.1 Fuentes de evidencia

| Fuente | Naturaleza | Disponibilidad |
|---|---|---|
| Consulta de estado por referencia externa en el Core (G2) | Determinista | Por validar (Q-02) |
| Referencia de la operación insertada en un campo libre del Core | Casi determinista | Por validar |
| Correlación de negocio: cuentas, monto, moneda, tipo, ventana `VENTANA_CORRELACION`, identificadores (G3) | Probabilística | Supuesta |
| Reportes del Core y de la infraestructura de pagos (conciliación diaria) | Segunda línea | Por validar |

### 5.2 Resultados

| Resultado | Acción |
|---|---|
| EV-POS (respuesta a la invocación, consulta por referencia única o evidencia huérfana), o EV-POS-CORR con sus condiciones ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) §3) | Se resuelve hacia el estado confirmado ([07](07-maquina-de-estados-transaccional.md)) con la evidencia registrada |
| **EV-NOEXEC** únicamente: `NOT_SENT` registrado por el titular, cierre por referencia o expiración en el Core con consulta de consistencia fuerte (estas dos últimas requieren capacidades no validadas, DD-22) | `FAILED_NO_EFFECT` |
| Ninguna coincidencia, consultas vacías, falta de movimientos, plazo vencido, conciliación incompleta (**EV-INC**) | **No resuelve.** Permanece en `*_UNKNOWN`; escala a `REQUIRES_RECONCILIATION` (versión 3, R2-N01) |
| Varias coincidencias posibles, o indicios contradictorios | `REQUIRES_RECONCILIATION` (sin resolución automática) |
| Vencimiento de `T_RESOLUCION_UNKNOWN` | `REQUIRES_RECONCILIATION` |
| Falla del propio proceso de conciliación (Core no disponible para consultas, error interno) | La operación **permanece** en `*_UNKNOWN`; la conciliación se reprograma con capacidad CL-2 ([ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md)); el plazo `T_RESOLUCION_UNKNOWN` sigue corriendo y garantiza el escalamiento |

### 5.3 Limitación explícita

**La correlación de negocio no ofrece las mismas garantías que la idempotencia nativa.** Es probabilística, depende del tiempo de asentamiento del Core y no distingue de forma fiable dos operaciones legítimas idénticas. El bloqueo de la sección 4 reduce este último caso, pero no lo elimina. La exactitud se mide con `TASA_ERROR_CONCILIACION` (RR-04).

## 6. Garantías declaradas

| Se garantiza (demostrable) | No se garantiza |
|---|---|
| Como máximo una operación por (ámbito, clave) | Ejecución exactamente una vez de extremo a extremo (*exactly-once end-to-end*) sin G2 |
| **Un único permiso de envío** por tramo y operación (ADR-0005 INV-T3); un envío tardío de un ejecutor zombi es ese único envío | Ausencia de duplicados semánticos con claves distintas |
| Ningún reintento de ejecución ante un resultado incierto | Resolución automática del 100 % de los resultados inciertos |
| Ninguna liberación de clave durante la incertidumbre | Que el Core no ejecute una solicitud tardía (requiere DD-22) |
| Ningún cierre ni reenvío por evidencia negativa | Resolución de toda operación sin intervención humana (vivacidad) |

## 7. Vinculación digital

La solicitud de vinculación aplica el mismo contrato. La creación del cliente en el Core se protege además con la búsqueda previa y la **evidencia de creación** ([ADR-0009](adr/ADR-0009-core-autoritativo.md), VD-05 y VD-06; [07, sección 5](07-maquina-de-estados-transaccional.md#5-máquina-de-estados-de-vinculación-digital)).
