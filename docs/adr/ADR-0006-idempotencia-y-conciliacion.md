# ADR-0006 — Contrato de idempotencia, deduplicación y conciliación de operaciones financieras

| Campo | Valor |
|---|---|
| Estado | **Aceptada — enmendada (versión 3)** por la remediación de CF-01 de ARG-01-R2 (ajusta IC-12 e IC-14; agrega IC-15, que aborda R2-N04). Pendiente de verificación independiente |
| Fecha | 2026-10-27 (versiones 1, 2 y 3) |
| Requisitos | R1 (transferencias), R3, C2 |
| Drivers y supuestos | S3, S18; AQ-02; QA3, QA5, QA9 |
| Relacionados | [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md), [ADR-0009](ADR-0009-core-autoritativo.md), [08 — Idempotencia y conciliación](../08-idempotencia-y-conciliacion.md), [ARG-01](../reviews/ARG-01-revision-arquitectonica.md) |

## Historial

| Versión | Cambio | Origen |
|---|---|---|
| 1 | Clave generada en el dominio, persistencia previa, conciliación por correlación | Fase 1 |
| 2 | La clave del canal pasa a ser **obligatoria y estable por intención**, con ámbito, huella canónica del contenido, reserva atómica, reglas de conflicto y de concurrencia, retención y bloqueo durante `UNKNOWN`. Se separan las capas de garantía y se declara explícitamente lo que no puede garantizarse. Se trata el duplicado semántico | ARG01-C01, ARG01-M01 |
| 3 | IC-12 solo libera la intención con evidencia determinista de no efecto o con un cierre excepcional por autoridad y una acción explícita del cliente. IC-14 pasa a un **permiso de envío único por tramo**, sin nuevos intentos `NOT_SENT`. **IC-15:** como máximo una operación equivalente no terminal por cliente, garantizada de forma atómica con la reserva | ARG-01-R2: CF-01 (R2-N01), R2-N04 |

## Contexto

Se asume que el Core **podría no** ofrecer idempotencia nativa ni consulta por clave o por referencia (S3). Un timeout no prueba que la operación haya fallado ([ADR-0005](ADR-0005-maquina-de-estados-transaccional.md)).

La revisión ARG-01 demostró que la versión 1 no cerraba el modo de falla más frecuente de un canal digital: la clave se generaba en el dominio «al aceptar la operación» y la clave del canal no era obligatoria. Así, una app que perdía la respuesta y reenviaba la transferencia podía crear **dos operaciones lógicas distintas** (ARG01-C01).

## Problema

¿Cómo se garantiza que **cada intención transaccional del cliente** produzca **como máximo una invocación de ejecución** hacia el Core, incluso ante reenvíos, concurrencia y reinicios? ¿Y cómo se declara con honestidad lo que **no** puede garantizarse cuando el Core no colabora?

## Decisión

### 1. Capas de garantía

Cada capa ofrece una garantía distinta. No deben confundirse.

| Capa | Qué garantiza | Naturaleza | Depende de |
|---|---|---|---|
| **G1. Deduplicación en la plataforma** | Una misma intención, identificada por (ámbito, clave), genera **como máximo una operación** y **como máximo una invocación de ejecución** hacia el Core | **Determinista**, bajo control de la plataforma | Este contrato (sección 2) |
| **G2. Idempotencia soportada por el Core** | El Core rechaza o reconoce una segunda solicitud con la misma referencia externa | Determinista **en el Core** | Validar S3 (Q-02). **Hoy no se asume** |
| **G3. Correlación de negocio** | Permite **inferir** si una operación se aplicó en el Core cuando no hay referencia consultable | **Probabilística** | Calidad de los datos del Core (S3) |
| **G4. Conciliación** | Resuelve los resultados inciertos (`UNKNOWN`) hacia un estado confirmado, o los escala | Proceso; su exactitud se mide | G2 o G3; operación manual (ADR-0005) |

### 2. Contrato de idempotencia para operaciones financieras críticas

Aplica a la iniciación de transferencias propias e interbancarias, a los reversos que inicie la plataforma y a la solicitud de vinculación que conduce a la creación de clientes ([ADR-0009](ADR-0009-core-autoritativo.md)).

| ID | Regla |
|---|---|
| **IC-01** | **Clave obligatoria.** Toda solicitud de una operación crítica incluye una clave de idempotencia del canal. Una solicitud sin clave se rechaza **sin efectos**. El nombre y el transporte (por ejemplo, la cabecera `Idempotency-Key`) se fijan en el diseño de API de la Fase 3 |
| **IC-02** | **Estable por intención.** El canal genera la clave **una sola vez por intención transaccional**, en el momento en que el usuario confirma la operación. La **persiste localmente antes del primer envío** y la **reutiliza en todo reenvío** de esa intención, incluso tras reiniciar la app. Una nueva intención (una nueva confirmación explícita del usuario) usa una clave nueva. El canal nunca reutiliza la clave de otra intención |
| **IC-03** | **Validación de formato.** El servidor valida el formato y la longitud: identificador aleatorio de alta entropía, por ejemplo un UUID aleatorio. Una clave inválida se rechaza sin efectos |
| **IC-04** | **Ámbito y autenticación.** La clave solo es válida dentro de su ámbito: (identidad autenticada del cliente, o sesión de vinculación del solicitante; tipo de operación; clave). El ámbito se deriva de la **identidad autenticada**, nunca de datos enviados por el cliente. Una misma clave presentada por otro sujeto no recupera ni revela la operación ajena: se trata dentro del ámbito de quien la presenta |
| **IC-05** | **Huella canónica.** La plataforma calcula una huella (*fingerprint*) mediante una función hash criptográfica sobre la **representación canónica de los campos semánticos** de la operación: cuentas de origen y destino o beneficiario, monto, moneda, tipo de operación y concepto del cliente. Los campos técnicos (marcas de tiempo del cliente, metadatos del dispositivo) quedan fuera. La lista de campos por operación se fija en el diseño de API |
| **IC-06** | **Reserva atómica.** La reserva de (ámbito, clave), la huella, el identificador interno de la operación y su registro durable inicial se persisten en **una única escritura atómica**. No puede existir una clave reservada sin operación ni una operación sin clave |
| **IC-07** | **Unicidad.** Una restricción de unicidad sobre (ámbito, clave) en el almacén durable de la plataforma, no un patrón «consultar y luego insertar» |
| **IC-08** | **Repetición.** Misma (ámbito, clave) y misma huella: se devuelve el **estado actual** de la operación existente, cualquiera sea ese estado, y **no se ejecuta nada nuevo** |
| **IC-09** | **Conflicto.** Misma (ámbito, clave) y **huella distinta**: se produce un **conflicto explícito**, sin efectos, registrado en la auditoría. La operación existente no se altera |
| **IC-10** | **Concurrencia.** Si llegan solicitudes concurrentes con la misma (ámbito, clave), solo una reserva tiene éxito (IC-07). Las demás reciben el estado de la operación reservada (IC-08) o el conflicto (IC-09). **Nunca hay dos ejecuciones** |
| **IC-11** | **Retención.** La clave y su huella se conservan como mínimo hasta el mayor de estos plazos: la ventana de reintento del canal (`VENTANA_REINTENTO_CANAL`) o la vida de la operación hasta un estado terminal **conciliado**, más un margen (`RETENCION_CLAVE`). **Nunca expira una clave cuya operación no es terminal** |
| **IC-12** | **Sin liberación en la incertidumbre.** Una operación no terminal **no libera su clave** ni admite una nueva ejecución con ella. Un reenvío recibe «en verificación». Una **nueva intención** equivalente (con una clave nueva) solo es posible si la operación anterior terminó en `REJECTED` o en `FAILED_NO_EFFECT` (**que, por [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md) INV-T2, ya exigen evidencia determinista**), en `COMPLETED` o `REVERSED`, o en `CLOSED_BY_EXCEPTION`. En este último caso, solo con una **acción explícita** del cliente informada de la incertidumbre residual. **Nunca se habilita de forma automática** con evidencia negativa o incompleta |
| **IC-13** | **Identificador interno y propagación.** La plataforma asigna un identificador interno de operación vinculado 1:1 con (ámbito, clave). Ese identificador se propaga al Core como referencia externa cuando el Core lo soporte (G2) o se inserta en un campo de referencia libre para mejorar G3 |
| **IC-14** | **Un único permiso de envío por tramo.** Cada tramo con efecto financiero (ejecución en el Core, procesamiento externo, reverso) recibe **un solo permiso de envío** durante toda la vida de la operación ([ADR-0005](ADR-0005-maquina-de-estados-transaccional.md) §4, INV-T3). No existen nuevos intentos automáticos, ni siquiera tras una falla `NOT_SENT` |
| **IC-15** | **Unicidad de la intención equivalente.** Como máximo una operación **no terminal** por (cliente, tipo de operación, huella canónica sin clave). Se garantiza con una **restricción de unicidad evaluada en la misma escritura atómica de la reserva** (IC-06), no con una consulta previa. Una solicitud equivalente con otra clave, mientras exista la anterior no terminal (en cualquier estado, incluidos `RECEIVED`, `*_IN_PROGRESS`, `*_UNKNOWN` y `REQUIRES_RECONCILIATION`), se rechaza sin efectos e informa el estado de la existente |

### 3. Duplicado semántico: claves distintas para la misma intención financiera

Una clave distinta **no garantiza** que la intención sea distinta. Esto ocurre, por ejemplo, cuando:

- se reinstala la app y se pierde la clave local;
- el cliente usa dos dispositivos;
- el cliente repite la operación por error;
- un cliente defectuoso genera una clave nueva por intento.

La plataforma **no puede distinguir con certeza** un duplicado accidental de dos operaciones legítimas idénticas. Por eso:

1. **Bloqueo de equivalentes no terminales (obligatorio, IC-15):** mientras exista una operación equivalente del mismo cliente **no terminal** (en cualquier estado), una nueva operación equivalente se **rechaza sin efectos**, de forma atómica con la reserva. *Versión 3:* la versión 2 solo cubría la clase `UNKNOWN` y no exigía atomicidad (R2-N04).
2. **Detección de un posible duplicado (obligatorio, parametrizado):** si existe una operación equivalente aceptada dentro de `VENTANA_DUPLICADO_SEMANTICO`, la nueva operación requiere una **confirmación explícita** del usuario antes de ejecutarse. Se excluyen las operaciones terminales sin efecto. La ventana y la presentación al usuario son decisiones de negocio por acordar.
3. **Riesgo residual declarado:** los duplicados semánticos confirmados por el usuario, o los que quedan fuera de la ventana, **no se previenen**. Se miden (`TASA_DUPLICADO_SEMANTICO`) y se registran como RR-13.

### 4. Conciliación (G3 y G4)

- Si el Core permite consultar por referencia (G2 validado), la conciliación es **determinista**.
- Si no, se concilia por correlación de negocio: cuentas, monto, tipo de operación, referencia de negocio, referencia externa, ventana de tiempo e identificadores.
- La resolución sigue las clases de evidencia de [ADR-0005](ADR-0005-maquina-de-estados-transaccional.md) §3 (versión 3): solo la evidencia **positiva** (EV-POS, o EV-POS-CORR con sus condiciones) confirma la ejecución, y solo EV-NOEXEC demuestra la no ejecución. **La ausencia de coincidencias, por sí sola, nunca resuelve**: la operación permanece en `*_UNKNOWN` y escala a `REQUIRES_RECONCILIATION`.
- La exactitud de la conciliación automática se **mide** (`TASA_ERROR_CONCILIACION`).

### 5. Lo que este diseño **no** garantiza

- **Ejecución exactamente una vez de extremo a extremo** (*exactly-once end-to-end*): no puede demostrarse sin la cooperación del Core (G2). La plataforma garantiza G1; los efectos en el Core se verifican por conciliación.
- **Ausencia de duplicados semánticos** con claves distintas (sección 3).
- **Resolución automática del 100 % de los resultados inciertos** cuando el Core no ofrece G2.

### 6. Onboarding

La solicitud de vinculación aplica IC-01 a IC-14. La creación del cliente en el Core se protege, además, con la búsqueda previa y la conciliación con evidencia de creación definidas en [ADR-0009](ADR-0009-core-autoritativo.md).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Clave generada solo en el dominio (versión 1) | **Descartada** (ARG01-C01) | Cada reenvío del cliente produce una intención nueva y la unicidad no lo impide |
| Clave del canal opcional | Descartada | Deja sin protección a los clientes que no la envían; la garantía G1 no sería universal |
| Deduplicación solo por huella del contenido, sin clave | Descartada como mecanismo principal | No distingue dos operaciones legítimas idénticas. Se usa como **mitigación** del duplicado semántico (sección 3), no como identidad |
| Reintento automático con *backoff* en escrituras | Descartada | Duplica efectos si el Core aplicó la operación |
| Depender de la idempotencia del Core | No aplicable bajo S3 | Si se valida, G2 complementa G1, pero no la reemplaza: G1 sigue siendo necesaria para los reenvíos del canal |
| **Clave del canal obligatoria y estable + ámbito + huella + reserva atómica + conciliación** | **Seleccionada** | Garantía determinista en la plataforma, independiente del Core |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Garantía determinista G1 ante reenvíos, concurrencia y reinicios | Requisitos para el canal: generar, persistir y reutilizar la clave |
| Detección de conflictos y de clientes defectuosos (IC-09) | Almacén durable con unicidad, huellas y retención |
| Menor ambigüedad de la conciliación (bloqueo durante `UNKNOWN`) | Fricción para el usuario: bloqueo y confirmación de posibles duplicados |
| Garantías declaradas con honestidad | **La correlación de negocio no ofrece las mismas garantías que la idempotencia nativa** |

## Consecuencias

- El canal (SPA y app móvil) tiene requisitos obligatorios de idempotencia. Se incorporan como criterios de la fase de frontend (DD-13) y de la Fase 3 (contratos de API).
- La validación del supuesto S3 sigue siendo **prioritaria**: G2 reduciría el riesgo RR-04 de forma significativa.
- QA3 se reformula para medir solo lo que es demostrable (G1) y la exactitud de G4 ([04](../04-escenarios-de-calidad.md)). QA9 verifica el reenvío del cliente.
- Nuevos parámetros por acordar: `VENTANA_REINTENTO_CANAL`, `RETENCION_CLAVE`, `VENTANA_DUPLICADO_SEMANTICO`, `TASA_ERROR_CONCILIACION`, `TASA_DUPLICADO_SEMANTICO`.

## Riesgos

- **RR-04:** conciliación sin idempotencia nativa (probabilística).
- **RR-13:** duplicado semántico con claves distintas.
- **Clientes que incumplen IC-02:** por ejemplo, una clave nueva por intento. Se detectan por métricas de reenvíos con clave distinta y huella igual (sección 3) y se mitigan con la confirmación explícita.
