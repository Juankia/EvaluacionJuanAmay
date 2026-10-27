# 05 — Frontera de integración con el Core Bancario

| Campo | Valor |
|---|---|
| Fase | 1 — Decisión sobre la frontera de integración con el Core |
| Estado | Decisión adoptada: **Opción C**. Pendiente de revisión del comité |
| ADR principales | [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0002](adr/ADR-0002-frontera-api-gateway.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md) |

## 1. Problema

La plataforma depende de dos sistemas externos para la información del cliente: el Core Bancario y el sistema complementario. Se asume que el Core:

- es heredado (S1) y autoritativo (S2);
- podría exponer contratos heredados sin idempotencia (S3);
- tiene una capacidad limitada (S4).

La decisión es **dónde y cómo se cruza la frontera** entre la plataforma digital y estos sistemas, de modo que:

1. el modelo del Core no se filtre al dominio digital ni a los canales (S6);
2. una falla en una ruta de integración no se propague a otras funciones (QA1);
3. el Core quede protegido del tráfico de los canales (QA2, QA8);
4. sea viable cambiar o agregar un Core en el futuro (S7, QA7).

## 2. Alternativas evaluadas

### Opción A — Cada servicio accede directamente al Core

```text
Servicio de clientes ─────┐
Servicio de movimientos ──┼──► Core Bancario
Servicio de transferencias┘
```

Cada servicio invoca al Core y traduce su modelo dentro de la lógica de dominio.

**Se descarta** por estas razones:

- **Acoplamiento fuerte:** cada servicio depende del contrato y de la semántica del Core.
- **Filtración del modelo heredado:** códigos y estructuras del Core terminan en el dominio y, potencialmente, en las API públicas.
- **Preocupaciones de integración repetidas** sin una frontera explícita: timeouts, manejo de errores, mapeo de códigos.
- **Reemplazo del Core costoso:** cambiar de Core obliga a modificar la lógica de dominio de todos los servicios.
- **Mayor superficie de pruebas:** la lógica de dominio no puede probarse sin simular el Core.
- **Acceso no controlado al Core:** no hay un punto donde acotar la concurrencia por operación.

La variante «acceso directo a la base de datos principal» se descarta con mayor razón, porque salta las reglas de negocio del sistema autoritativo (S12).

### Opción B — Capa de integración centralizada

```text
Servicio de clientes ─────┐
Servicio de movimientos ──┼──► Capa de integración ──► Core Bancario
Servicio de transferencias┘     (servicio central)
```

Un servicio central concentra toda la comunicación con el Core.

**No es una opción incorrecta en términos absolutos.** Es válida cuando:

- la organización tiene un equipo de integración centralizado y fuerte;
- las políticas de integración se centralizan de forma intencional (por ejemplo, por gobierno corporativo);
- existe un aislamiento estricto de SLA y Bulkhead **dentro** de la capa central, por operación;
- el número de dominios es reducido;
- la propiedad operativa está centralizada.

**No se selecciona en este contexto** por estas razones:

- se convierte en un punto común de falla y de contención: si no hay aislamiento interno estricto, la saturación de la consulta de movimientos puede degradar las transferencias;
- concentra conocimiento de todos los dominios y tiende a acumular lógica de negocio;
- acopla el ciclo de liberación de todos los dominios a un único componente y a un único equipo;
- agrega un salto de red adicional en la ruta crítica.

### Opción C — Capas anticorrupción por dominio (seleccionada)

```text
Servicio de clientes        ──► ACL de clientes        ──► Core Bancario / Sistema complementario
Servicio de movimientos     ──► ACL de movimientos     ──► Core Bancario
Servicio de transferencias  ──► ACL de transferencias  ──► Core Bancario / sistema de pagos (vía Core, S13)

            Todas las ACL usan el Integration Platform SDK (biblioteca, no servicio)
```

Cada dominio tiene su propia **capa anticorrupción** (*Anti-Corruption Layer*, ACL). La ACL traduce entre el contrato externo o heredado del Core y el modelo de dominio de la banca digital.

> **Nota sobre el despliegue:** «ACL por dominio» describe una **frontera lógica de propiedad y de traducción**. La forma de despliegue se decide en la fase de contenedores: módulo dentro del servicio de dominio o componente desplegable propio. Esta decisión no presupone microservicios adicionales.

## 3. Decisión

Se adopta la **Opción C**, documentada en [ADR-0001](adr/ADR-0001-acl-por-dominio.md).

### 3.1 Responsabilidades de cada ACL

| Responsabilidad | Descripción |
|---|---|
| Traducción del modelo | Del contrato del Core a los tipos del dominio, y viceversa. El dominio nunca ve tipos del Core |
| Traducción de errores | Los códigos propietarios del Core se convierten en errores de dominio con semántica explícita (por ejemplo, `INSUFFICIENT_FUNDS`, `ACCOUNT_BLOCKED`, `OUTCOME_UNKNOWN`) |
| Política de resiliencia local | Timeouts, Circuit Breaker, Bulkhead y reintentos **específicos de cada operación** (sección 5) |
| Protección del Core | Pool de conexiones dedicado y control de admisión dentro de la cuota de su clase de operación ([ADR-0011](adr/ADR-0011-presupuesto-de-capacidad-del-core.md)) |
| Clasificación del resultado | Clasifica cada respuesta o falla en la taxonomía cerrada `NOT_SENT`, `REJECTED_CONFIRMED`, `UNKNOWN` o `CONFIRMED` ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md)), con una tabla por contrato (sección 6) |
| Pruebas de contrato | Pruebas que verifican la ACL contra el contrato del Core (QA7) |

### 3.2 Lo que la ACL no hace

- No contiene reglas de negocio del dominio (por ejemplo, límites de transferencia o validación de beneficiarios).
- No orquesta procesos de varios pasos. Esa responsabilidad es del servicio de dominio y de la máquina de estados ([07](07-maquina-de-estados-transaccional.md)).
- No decide el enrutamiento entre varios Core (ver RR-07). Si llega a necesitarse, se diseñará como una decisión explícita en el dominio.

## 4. Integration Platform SDK

El componente que en propuestas anteriores se llamó «integration kernel» pasa a llamarse **Integration Platform SDK**. Es una **biblioteca compartida o capacidad de plataforma**, **no un servicio centralizado en tiempo de ejecución**. El detalle y su gobierno están en [ADR-0003](adr/ADR-0003-integration-platform-sdk.md).

**Principio: capacidad compartida, política local** (*shared capability, local policy*).

| Permitido en el SDK | Prohibido en el SDK |
|---|---|
| Soporte de identificador de correlación | Lógica de negocio |
| Propagación de trazas (OpenTelemetry) | Lógica de clientes, cuentas, movimientos o transferencias |
| Modelo estándar de error técnico | Enrutamiento entre Core |
| Utilidades de observabilidad, registros y métricas | Lógica de Saga u orquestación |
| Primitivas de resiliencia (Circuit Breaker, Bulkhead, reintento, timeout) | Transformaciones específicas de un dominio |
| Soporte para configurar timeouts y para propagar el plazo | Flujos de integración centralizados |
| Utilidades de cliente de seguridad (mTLS, obtención de credenciales) | Valores por defecto de política que sustituyan a la decisión local |
| Convenciones comunes de integración | |

El SDK **ofrece mecanismos**. Cada ACL **declara su política**:

| Política | ACL de movimientos (lectura) | ACL de transferencias (escritura crítica) |
|---|---|---|
| Timeout de lectura | Acorde con la experiencia de consulta | Más estricto, dentro del plazo de negocio |
| Reintento | Limitado, solo para lecturas seguras e idempotentes, con *backoff* y *jitter* | **Ningún reintento a ciegas.** Ante un resultado incierto: `UNKNOWN` y conciliación |
| Circuit Breaker | Sí | Sí |
| Bulkhead y pool de conexiones | Dedicados | Dedicados y separados de los de lectura |
| Comportamiento si el Core no está disponible | Datos degradados aprobados ([06](06-estrategia-de-cache.md)) | Falla segura ([07](07-maquina-de-estados-transaccional.md)) |
| Idempotencia | No aplica (lectura) | Clave generada en el dominio y conciliación ([08](08-idempotencia-y-conciliacion.md)) |

> El SDK mitiga la duplicación entre las ACL; **no elimina el costo arquitectónico** de la Opción C.

## 5. Frontera de resiliencia

### 5.1 Dónde viven las políticas

Las políticas de resiliencia frente al Core se aplican **principalmente en la frontera de integración (la ACL)**, porque solo ahí se conoce la semántica de la operación (lectura segura o escritura crítica).

| Capa | Responsabilidad de resiliencia | Lo que no hace |
|---|---|---|
| API Gateway | Timeout de solicitud a nivel de infraestructura, límite de tasa, protección del borde | No contiene lógica de resiliencia de negocio sobre el Core: no reintenta transferencias ni decide degradaciones de datos |
| Servicio de dominio | Es dueño del **plazo de negocio**. Decide qué hacer ante una degradación (datos de caché o falla segura) | No conoce detalles del protocolo del Core |
| ACL | Timeout de conexión, timeout de lectura, Circuit Breaker, Bulkhead, reintento solo cuando es semánticamente seguro | No decide reglas de negocio |

Para cada integración se definen:

- timeout de conexión;
- timeout de lectura o de solicitud;
- Circuit Breaker;
- Bulkhead (aislamiento de concurrencia o de pool);
- reintento, **solo** cuando es semánticamente seguro.

### 5.2 Tipos de timeout

| Concepto | Definición | Dueño |
|---|---|---|
| **Timeout de conexión** (*connection timeout*) | Tiempo máximo para establecer la conexión con el sistema externo. Si se agota, la solicitud **no llegó** al Core y es seguro clasificarla como no enviada | ACL |
| **Timeout de lectura** (*read timeout*) | Tiempo máximo de espera de la respuesta después de enviar la solicitud. Si se agota, **la solicitud pudo haberse procesado** | ACL |
| **Timeout de solicitud** (*request timeout*) | Tiempo total máximo de una llamada a una dependencia, incluidos los reintentos permitidos | ACL o Gateway (cada uno en su capa) |
| **Plazo de negocio** (*business deadline*) | Instante absoluto después del cual el resultado deja de ser útil para el usuario o para el proceso. Se fija en el borde del dominio y se propaga hacia abajo | Servicio de dominio |

### 5.3 Propagación del plazo

No se asume que «el timeout exterior es la suma de los timeouts interiores». Esa regla es frágil: ignora las colas, los reintentos y el tiempo ya consumido, y tiende a timeouts exteriores excesivos o a timeouts interiores que siguen trabajando cuando el llamador ya abandonó.

Se adopta el concepto de **plazo propagado** (*deadline propagation*):

1. El servicio de dominio fija el plazo de negocio de la operación.
2. El plazo viaja con la solicitud. Conceptualmente se representa como `X-Request-Deadline`.
3. Cada salto calcula su presupuesto como el **mínimo** entre su timeout local configurado y el tiempo restante hasta el plazo.
4. Si el tiempo restante no alcanza para una llamada útil, el salto falla rápido sin invocar al Core.

El mecanismo concreto se elige más adelante (DD-06): cabecera HTTP con instante absoluto o con presupuesto relativo, *baggage* de OpenTelemetry o mecanismo nativo del protocolo. Si se usa un instante absoluto, debe considerarse la **desviación de reloj** entre nodos.

> **Advertencia:** que se agote el plazo propagado en una operación crítica **no** autoriza a cancelar ni a dar por fallida una operación que ya se envió al Core. Aplica la sección 6.

## 6. Regla crítica: un timeout no equivale a una falla

**Un timeout no significa necesariamente que la operación externa haya fallado.**

```text
Banca Digital ──► Solicitud al Core
                  Core confirma (commit) la transacción
                  La respuesta se pierde (red, balanceador, reinicio)
Banca Digital ◄── Timeout de lectura
```

En este punto la plataforma **no sabe** si la transferencia se aplicó. El resultado pertenece a la clase **`UNKNOWN`** y la operación pasa al estado `CORE_UNKNOWN` ([ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md)).

Reglas:

1. Las operaciones financieras críticas **no se reintentan automáticamente** tras un timeout de lectura. Esto es especialmente importante porque se asume que el Core no ofrece idempotencia nativa (S3): un reintento podría duplicar el débito.
2. Solo una falla con **evidencia local de que ningún byte salió** hacia el destino ni hacia un intermediario es de clase `NOT_SENT`. Cualquier otra falla, incluidas las desconexiones durante o después de la escritura, las conexiones reutilizadas del pool y los errores de intermediarios, es `UNKNOWN`. La taxonomía cerrada está en [ADR-0005, sección 2](adr/ADR-0005-maquina-de-estados-transaccional.md#2-taxonomía-cerrada-del-resultado-de-una-invocación-externa-h01) (remediación ARG01-H01). En caso de duda, `UNKNOWN`.
3. La clase `UNKNOWN` se resuelve solo por **conciliación**: consulta de estado si el Core la ofrece, o correlación de negocio ([08](08-idempotencia-y-conciliacion.md)).
4. Al cliente se le comunica un estado «en verificación», no un error definitivo ni un éxito.

## 7. Comportamiento ante la indisponibilidad del Core

| Tipo de operación | Comportamiento |
|---|---|
| **Lecturas** | Pueden degradarse a datos de caché o de proyección **aprobados**, solo si la política de frescura de la categoría lo permite ([06](06-estrategia-de-cache.md)). La respuesta indica la frescura |
| **Escrituras transaccionales** | **Falla segura.** Si el Core no se invocó, la operación se rechaza sin efectos. **No se encola una transacción financiera solo porque el Core no está disponible**: ejecutarla más tarde, con saldos y condiciones distintas y sin que el cliente lo espere, crea riesgos de integridad y de experiencia |
| **Resultado desconocido** | `UNKNOWN` → conciliación |

Una eventual funcionalidad de «transferencias programadas» sería una **capacidad de negocio explícita**, con su propio consentimiento y sus propias reglas. No sería un efecto colateral de la indisponibilidad.

## 8. Interpretación del API Gateway

El requisito R1 pide «un API Gateway que **orqueste** al menos 3 servicios».

**Interpretación arquitectónica explícita:** el término «orquestar» se interpreta como **exponer, enrutar y proteger** los servicios, y no como orquestación de negocio dentro del Gateway. Detalle en [ADR-0002](adr/ADR-0002-frontera-api-gateway.md).

| El API Gateway sí hace | El API Gateway no hace |
|---|---|
| Enrutamiento hacia los servicios de dominio | Orquestación de procesos de negocio (por ejemplo, la secuencia de una transferencia interbancaria) |
| Terminación TLS donde corresponda | Reintentos de operaciones financieras |
| Integración con la validación de tokens | Reglas de negocio, validaciones de límites o de saldos |
| Limitación de tasa (*rate limiting*) | Traducción del modelo del Core |
| Integración con WAF | Decisiones de degradación de datos |
| Correlación de solicitudes | |
| Protección de las API | |
| Composición liviana mediante un BFF (*Backend for Frontend*), si se justifica en la fase de contenedores | |

La orquestación de negocio pertenece a los servicios de dominio y de aplicación.

## 9. Trade-offs de la Opción C

| Beneficios | Costos |
|---|---|
| Aislamiento de dominios | Lógica de integración duplicada |
| Anti-Corruption Layer explícita por dominio | Más adaptadores |
| Menor filtración del modelo heredado | Más pruebas (contrato por ACL) |
| Resiliencia granular por operación | Más superficies operativas que monitorear |
| Autonomía de los equipos | Riesgo de políticas inconsistentes entre las ACL |
| Evolución independiente | |
| Reemplazo futuro del Core más sencillo | |
| Introducción de otra implementación del Core más sencilla | |

**Mitigación:** el Integration Platform SDK aporta mecanismos y convenciones comunes. Un catálogo de políticas por ACL, revisado en el gobierno de arquitectura, reduce la inconsistencia.

**El SDK mitiga la duplicación; no elimina el costo arquitectónico.**

## 10. Supuesto organizacional

La Opción C asume una organización de ingeniería suficientemente madura (S11): con responsables por dominio y con valor en la evolución independiente.

Si el equipo tuviera solo 1–2 ingenieros y la complejidad de integración fuera baja, **un servicio de integración centralizado podría ser económicamente preferible**. Esta es una decisión **contextual**, no una ley arquitectónica absoluta. Si el supuesto S11 resulta falso, la decisión debe reevaluarse.

## 11. Trazabilidad

| Requisito | Driver o supuesto | Decisión | Trade-off | Consecuencia |
|---|---|---|---|---|
| R1, C5 | S1, S4, S6, S7, QA1, QA7 | ACL por dominio ([ADR-0001](adr/ADR-0001-acl-por-dominio.md)) | Duplicación y más superficies | Aislamiento de fallas y del modelo; reemplazo del Core acotado |
| R1 | Interpretación de «orquestar» | Gateway como fachada ([ADR-0002](adr/ADR-0002-frontera-api-gateway.md)) | Requiere servicios de aplicación explícitos | Lógica de negocio fuera del borde; el Gateway es reemplazable |
| C2 | QA1, QA3 | Resiliencia en la ACL y plazo propagado | Más configuración por ACL | Fallas acotadas por operación |
| C2 | QA5, S3 | Timeout ≠ falla; `UNKNOWN` | Requiere conciliación y operación | Sin duplicación de débitos por reintento |
