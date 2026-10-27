# ARG-02 — Revisión independiente de la arquitectura de contenedores

| Campo | Valor |
|---|---|
| Revisión | ARG-02 |
| Objeto | Fase 3 — C4 Nivel 2, commit `6ecd69f` |
| Fecha | 2026-10-27 |
| Comité (perfiles simulados) | Principal Solutions Architect; Banking Systems Architect; Distributed Systems; Application Security; IAM; Data Architecture; Cloud Infrastructure; SRE |
| Documentos relacionados | [Preparación ARG-02](ARG-02-preparacion-revision-contenedores.md), [16](../16-arquitectura-de-contenedores.md) a [23](../23-riesgos-y-condiciones-fase-3.md), ADR-0012 a ADR-0018 |
| Veredicto | **CONDITIONAL GO, con alcance restringido** (sección 15) |

> Revisión adversarial. Las afirmaciones de la Fase 3 y de su preparación se trataron como hipótesis. Todo el análisis es **conceptual**: no existe software y **no se ejecutó ninguna prueba**. No se modificaron ADR ni documentos de arquitectura.

---

## 1. Resumen ejecutivo

**La descomposición híbrida es defendible y se mantiene.**

- **Matriz de decisión:** su recálculo confirma B (4,15). La selección resiste cinco análisis de sensibilidad, **aunque empata con el monolito modular** en el escenario más desfavorable de equipo pequeño (3,76 frente a 3,72). Q-18 pasa a ser determinante antes de la implementación.
- **Contenedores:** separa correctamente el estado financiero, la biometría y la auditoría. Ninguna biblioteca ni ningún módulo aparece modelado como contenedor.
- **Gateway:** la interpretación de R1 se mantiene sin cambiar silenciosamente el alcance.

La revisión encontró, sin embargo, **un defecto CRITICAL**:

- **El protocolo de recuperación tras una pérdida de datos (PT-02) no puede proteger las operaciones cuya reserva desapareció.** Después de restaurar el almacén transaccional (o de conmutar a otra región con replicación asíncrona, que es el escenario normal de DR que exige C3), un cliente que reenvía la misma clave obtiene una **reserva nueva y un permiso de envío nuevo** para una operación que el Core quizá ya ejecutó.
- El defecto está **confinado** a un contenedor ya marcado como PROVISIONAL y con la implementación bloqueada (B-01). **No invalida la descomposición** ni el resto de los contenedores.

También encontró **cinco HIGH**:

- la degradación silenciosa de la replicación síncrona;
- un BFF declarado «sin estado» que en realidad es el gestor de tokens;
- la auditoría síncrona de cada consulta, que debilita la disponibilidad;
- el punto de control de la vinculación apoyado en una credencial de administración amplia del IdP;
- la **ausencia de cualquier supuesto sobre dónde está el Core y por qué red se llega a él**, que es el factor dominante de C6 (baja latencia) y un dominio de falla común.

| Indicador | Valor |
|---|---|
| Contenedores en el DSL | **19** (la Fase 3 informó 16) |
| Hallazgos | 1 CRITICAL · 5 HIGH · 7 MEDIUM · 7 LOW |
| Veredicto | **CONDITIONAL GO con alcance restringido** |

## 2. Inventario de evidencias

| Evidencia | Uso |
|---|---|
| `c4/workspace.dsl` y su exportación JSON (`structurizr/structurizr export -f json`) | Conteo independiente de elementos |
| `diagrams/c4/*` (16 PlantUML y 16 SVG) y PNG regenerados para inspección visual | Revisión visual de las 8 vistas |
| `docs/12` a `docs/23`, `docs/02`, `docs/04`, `docs/09` a `docs/11`, `docs/15` | Lectura completa |
| ADR-0001 a ADR-0018 | Lectura completa; coherencia |
| ARG-01, ARG-01-R2, remediación CF-01/CF-02 y preparación ARG-02 | Historial de condiciones |
| PDF del reto | Contraste de R1 a R6 y C1 a C6 |
| Búsquedas dirigidas | Ubicación del Core, datos de contacto para notificaciones, identidad del personal, JWKS, revocación, secuencia de eventos y réplicas de lectura (todas sin resultados en el diseño) |

## 3. Validación del inventario de contenedores

Conteo independiente a partir del modelo exportado:

| Tipo de elemento | Cantidad | Detalle |
|---|---|---|
| Personas | 4 | Cliente bancario, Solicitante de vinculación, Operador de soporte (candidato), Responsable de auditoría (candidato) |
| Sistemas de software | 7 | Plataforma (en diseño), Core, Sistema Complementario, Sistema Externo de Transferencias, KYC/Biométrico, Notificaciones, Fuente Oficial de Identidad (candidato) |
| **Contenedores** | **19** | Ver la tabla siguiente |
| Componentes | 0 | Correcto: corresponden a la Fase 4 |
| Nodos de despliegue | 0 | Correcto: fase de infraestructura |
| Vistas | 2 de contexto + 6 de contenedores | — |

| Clase | Contenedores |
|---|---|
| Canal (2) | SPA web, Aplicación móvil |
| Borde e identidad (3) | API Gateway, BFF de canales digitales, Proveedor de identidad (IdP) |
| Dominio (5) | Servicio de consultas, Servicio de vinculación digital, Servicio de transferencias, Servicio de notificaciones, Servicio de auditoría |
| Operación (1) | Consola de operaciones (provisional) |
| Almacenes de datos (7) | BD del IdP, BD de consultas, Caché de consultas, BD de vinculación, BD transaccional de transferencias, BD de notificaciones, Almacén de auditoría |
| Dependencia de infraestructura (1) | Broker de mensajería |

**Inconsistencias:**

- La respuesta final de la Fase 3 informó **16** contenedores. La tabla de [16 §4](../16-arquitectura-de-contenedores.md#4-inventario-de-contenedores) tiene 16 **filas**, pero la fila de `16:66` agrupa cuatro almacenes (ARG02-L01).
- **Correcto:** el Integration Platform SDK, las ACL y los roles de conciliación **no** están modelados como contenedores.
- La caché y el broker son dependencias de infraestructura modeladas como contenedores. Es aceptable en C4, porque son ejecutables separados con datos propios.

## 4. Evaluación de la descomposición

| Pregunta | Evaluación |
|---|---|
| ¿Se justifican cinco servicios de dominio? | **Sí.** Cada uno cumple un criterio de ADR-0012 (capacidad, seguridad, integridad o responsable). Las notificaciones y la auditoría separadas se justifican por la **seguridad** (el almacén de auditoría de solo anexión exige un acceso restringido distinto) y por la **disponibilidad** (los reintentos con proveedores externos no deben competir con la ingesta de auditoría) |
| ¿El BFF compartido acopla en exceso? | Acoplamiento **moderado**: está en la ruta crítica de ambos canales y combina dos modelos de autenticación. Hay un disparador de separación definido. El problema real es otro: ARG02-H02 y ARG02-M06 |
| ¿Cada servicio es dueño de sus datos? | **Sí**, en el modelo. Pero ADR-0016 permite compartir un clúster físico, **incluida la BD del IdP** (ARG02-M04) |
| ¿Hay comportamiento de monolito distribuido? | **Parcial.** La cadena síncrona Gateway → BFF → servicio → Core tiene tres saltos. El BFF compone de forma síncrona consultas y transferencias sin una degradación definida (ARG02-M06). M04 (plazo frente al timeout del Gateway) sigue abierto |
| ¿El Core sigue siendo un dominio de falla común? | **Sí**, y se reconoce (ADR-0011). Pero **la red hacia el Core no está modelada ni supuesta** (ARG02-H05) |
| ¿El escalado independiente aporta beneficios reales? | **Sí** para el servicio de consultas (CL-3) frente a las escrituras CL-1. Es limitado para los demás servicios, cuyo volumen es bajo |
| ¿La carga operativa es proporcional? | **Al límite.** 19 contenedores y un IdP autohospedado para un reto que pide «aplicaciones, bases de datos, servicios». Se justifica por los atributos críticos, pero depende de S11 y Q-18 (ARG02-M07) |

### 4.1 Recálculo de la matriz y sensibilidad

| Escenario | A (monolito modular) | B (híbrida) | C (microservicios) |
|---|---|---|---|
| Pesos originales de [20](../20-alternativas-de-descomposicion.md) | 3,27 | **4,15** | 3,13 |
| Pesos iguales en los 12 criterios | 3,25 | **4,00** | 3,25 |
| Corrección del doble conteo: A con integridad 5, porque el permiso y el envío también comparten proceso en un monolito | 3,42 | **4,15** | 3,13 |
| B con complejidad 2 y costo 2 (19 contenedores, IdP autohospedado) | 3,27 | **4,00** | 3,13 |
| Equipo pequeño: pesos de complejidad 20, equipo 16 y costo 10 | 3,59 | **4,00** | 2,84 |
| **Combinado adverso** (las tres correcciones anteriores) | 3,72 | **3,76** | 2,84 |

**Conclusión:**

- Los totales originales son **correctos**.
- B gana en todos los escenarios, pero en el combinado adverso la diferencia (0,04) **no es significativa**.
- Los criterios «dependencia del Core» e «integridad» están **correlacionados** (ARG02-L03).
- **No hay evidencia de un defecto material** que justifique cambiar la selección. Sí la hay para exigir Q-18 antes de implementar (ARG02-M07).

## 5. Evaluación de la seguridad transaccional

### 5.1 Qué garantiza el diseño y qué depende de otros

| Invariante | Garantizado por el diseño | Depende de la infraestructura | Depende de sistemas externos |
|---|---|---|---|
| Una sola operación por (ámbito, clave) | Unicidad en el almacén (IC-07) | **Que el almacén no pierda la reserva** (PT-01, PT-02); ARG02-C01 | — |
| Un solo permiso de envío por tramo | CAS cercado (INV-T3, INV-T4) | PT-01 a PT-03; ARG02-H01 | — |
| Ningún reenvío oculto | — | PT-06: bibliotecas, sidecars y proxies (ARG02-L06) | S19 (intermediarios del Core) |
| Una ejecución por solicitud recibida | — | — | **S20** |
| Sin ejecución tardía | — | — | **DD-22** (cierre por referencia o expiración en el Core) |
| Cierre sin evidencia | Prohibido (INV-T9); solo cierre excepcional con autoridad | — | — |
| Exactamente una vez de extremo a extremo | **No se afirma** (correcto) | — | — |

### 5.2 Revisión de PT-01 a PT-06

| Condición | Valoración |
|---|---|
| **PT-01** (RPO 0) | Necesaria pero **insuficiente tal como está redactada**. Algunos servicios gestionados siguen aceptando escrituras cuando la réplica síncrona deja de estar disponible. Una conmutación posterior pierde esas escrituras **sin que el sistema sepa que debe entrar en cuarentena** (ARG02-H01) |
| **PT-02** (cuarentena) | **Defecto CRITICAL** (ARG02-C01): no puede conciliar lo que no conoce. Además, una conmutación **entre regiones**, que suele ser asíncrona, es el escenario normal de DR (C3), **no** una excepción |
| **PT-03** (escritor único) | Correcta. Los servicios gestionados con conmutación aislan al primario anterior. En una topología autogestionada habría que exigir ese aislamiento |
| **PT-04** (clave en el BFF) | Correcta |
| **PT-05** (consola) | Correcta, pero **no se define cómo se autentica el personal de la consola** (ARG02-M04) |
| **PT-06** (reintentos ocultos) | Correcta. Debe nombrar explícitamente los sidecars de malla de servicios y los proxies de salida (ARG02-L06) |
| Lecturas desde réplicas | **No hay regla.** Ninguna decisión de estado (transiciones, IC-15) puede leerse de una réplica con retraso (ARG02-L05) |
| Recuperación tras una falla de proceso, permiso único, ejecución tardía, cierre excepcional | Coherentes con ADR-0005 v3. Sin observaciones nuevas |

## 6. Evaluación de la identidad (ADR-0013)

| Aspecto | Estado en el diseño | Observación |
|---|---|---|
| Disponibilidad y recuperación | Mencionadas («multizona», RR-21) | Sin RTO ni RPO propios, sin plan de restauración del IdP y sin análisis de la pérdida de sesiones o factores (ARG02-M04) |
| Emisión y validación de tokens | Gateway con JWKS | **No se define la caché de JWKS** en el Gateway ni el comportamiento cuando el IdP no está disponible (los tokens vigentes siguen siendo válidos; las renovaciones fallan) |
| Ciclo de vida de las claves de firma | **Sin evidencia** | Rotación, solapamiento de claves y compromiso de una clave (ARG02-M04) |
| Revocación de sesiones | **Sin evidencia** | Los tokens autocontenidos no se revocan antes de vencer. Hace falta definir su vida útil e introspección o verificación de revocación para las operaciones de alto riesgo |
| Gestión de secretos | Gestor de secretos del proveedor ([22 §2](../22-seguridad-entre-contenedores.md#2-autenticación-y-autorización)) | Correcto como requisito |
| Acceso administrativo | Solo el servicio de vinculación por API | La credencial de administración es **amplia** (ARG02-H04). No hay un modelo de acceso administrativo humano (personal privilegiado) |
| Separación de inquilinos | **Sin evidencia** | La identidad de **clientes** y la del **personal** (consola) deben separarse. El personal debería usar la identidad corporativa de la institución (ARG02-M04) |
| Dependencia de la vinculación y del acceso | Total: sin IdP no hay acceso ni activación | Aceptable si se trata como componente crítico (RR-21) |
| Autorización de la vinculación | Punto de control único en el servicio de vinculación | ARG02-H04 |

**Autohospedado frente a gestionado.** La frontera interna es razonable. ADR-0013 compara las opciones solo por residencia y control, pero **no** por:

- el costo operativo del ciclo de vida de las claves;
- los parches de seguridad;
- la alta disponibilidad.

Un IdP gestionado **dentro del entorno de la institución** reduce esa carga y sigue siendo un contenedor interno. **No se exige cambiar el candidato**; sí completar la comparación en la fase de seguridad con estos criterios.

## 7. Evaluación de la auditoría

### 7.1 Política propuesta por categoría

Recomendación del comité para resolver ARG02-H03; **requiere una decisión institucional** (Q-19).

| Categoría | Durabilidad requerida | Orden | ¿La respuesta espera la persistencia? | Falla aceptable | Minimización | Retención | Antimanipulación |
|---|---|---|---|---|---|---|---|
| Acciones financieras | Sin pérdida (misma transacción que el estado) | Por operación | **Sí**, de forma implícita: es el mismo *commit* local | Si no se audita, la acción no ocurre (falla cerrada natural) | Identificadores, montos, cuentas enmascaradas | Por acordar (Q-19) | Encadenamiento + archivo inmutable |
| Seguridad (vinculación, activación, cambio de credenciales o factores) | Sin pérdida | Por sujeto | **Sí** | Falla cerrada | Sin datos biométricos ni credenciales | Por acordar | Igual |
| Autenticación (inicio de sesión correcto o fallido) | Alta; pérdida **detectable** | Por sujeto | **No** (se registra en el IdP y luego se transfiere) | Se acepta un retraso; la pérdida debe detectarse (secuencias y conciliación de contadores) | Identificadores, dispositivo, resultado | Por acordar | Igual |
| Operaciones privilegiadas (consola) | Sin pérdida | Global por caso | **Sí** | Falla cerrada: sin auditoría no hay acción privilegiada | Identidad del personal, evidencia | Por acordar (probablemente mayor) | Igual |
| Consultas de solo lectura | Alta; pérdida **detectable y acotada** | No requerido | **Recomendación: no** (búfer local durable y transferencia asíncrona); salvo que la institución exija la falla cerrada | Ventana de pérdida acotada y alertada | Tipo de consulta, recurso; **sin el contenido** | Por acordar | Igual |
| Eventos operativos | Mejor esfuerzo | No | No | Pérdida aceptable | — | Operativa | No aplica (observabilidad, no auditoría) |

**Juicio:** la regla actual de [18 §4](../18-propiedad-y-consistencia-de-datos.md#4-durabilidad-transaccional-y-durabilidad-de-auditoría), «si no puede escribir el evento de auditoría de una consulta, no responde», hace de la BD de consultas un **punto único de falla de todas las consultas**. Contradice la degradación de QA1 y QA3 **sin que esa decisión se haya analizado** (ARG02-H03).

### 7.2 Integridad del almacén de auditoría

El «encadenamiento de hashes» no define:

- su granularidad (global, por fuente o por partición);
- cómo se deduplica **antes** de encadenar (las entregas son al menos una vez);
- cómo se encadena con **varias réplicas consumidoras** (una cadena global serializa la ingesta);
- un anclaje periódico externo.

Ver ARG02-M02.

## 8. Evaluación del procesamiento por eventos (ADR-0015)

| Aspecto | Estado | Observación |
|---|---|---|
| Semántica del outbox | Cubierto | Misma transacción local |
| Recuperación del publicador | Parcial | Reintento del relé (al menos una vez). Sin regla de **orden de publicación por clave** tras una recuperación |
| Entrega al menos una vez | Cubierto | **No se asume exactamente una vez** (correcto) |
| Consumidores idempotentes | Cubierto | Por identificador de evento |
| Duplicados | Cubierto | Deduplicación en notificaciones |
| **Eventos fuera de orden** | **Brecha** | Sin número de secuencia por operación. Un `REVERSED` puede llegar antes que su `DEBIT_CONFIRMED` y el cliente recibiría notificaciones contradictorias (ARG02-M01) |
| Mensajes fallidos y mensajes venenosos | Parcial | Cola de mensajes fallidos con alerta. Sin procedimiento de **reprocesamiento** ni de cuarentena de mensajes venenosos |
| Reprocesamiento (*replay*) | **Brecha** | Sin política de retención ni de reconstrucción (relevante para ARG02-C01: los eventos entregados antes de una pérdida podrían ayudar a reconstruir operaciones) |
| Versionado de eventos | Parcial | «Esquema versionado» sin una política de compatibilidad |
| Correlación de auditoría | Cubierto | Identificador de correlación y clave de idempotencia |
| Falla de notificación | Cubierto | No revierte la operación. **Pero** el servicio de notificaciones no tiene una fuente modelada de **datos de contacto ni de tokens de dispositivo** (ARG02-M05) |

## 9. Evaluación de la capacidad del Core

| Situación | ¿Protegida? | Observación |
|---|---|---|
| Varios servicios en una clase | **Sí** | `Σ_s Q_{c,s} ≤ Q_c` (RI-03) |
| Varias réplicas y escalado automático | **Sí** | Techo `N_MAX` (RI-01) |
| Despliegue rolling | **Sí** | `N_EFECTIVO_MAX` incluye el surge |
| Despliegue blue/green | **Sí** | Interruptor de admisión (RI-02) |
| Roles de conciliación | **Sí** | Cuentan en CL-2 dentro de su servicio |
| **Cuota por réplica igual a 0** | **No** | Con `floor(Q_{c,s} / N_EFECTIVO_MAX_s)`, un servicio con una cuota pequeña obtiene 0 por réplica y **no puede invocar al Core** (`19:67`). RI-03 no lo valida (ARG02-M03) |
| **Conexiones frente a concurrencia** | **No** | El Core puede limitar **sesiones** además de la concurrencia. Los pools con conexiones mínimas inactivas, multiplicados por N, pueden agotar las sesiones del Core sin carga real (ARG02-M03) |
| Seguridad frente a subutilización | Analizado | La subutilización se acepta de forma explícita. **No se exige** una tecnología de coordinación: M2 (DD-20) sigue justificadamente diferido |

## 10. Evaluación del frontend

| Aspecto | Valoración |
|---|---|
| Alternativas A y B | Comparación completa; la recomendación de B es razonable y está condicionada a Q-18 y DD-14 |
| BFF compartido | Ver ARG02-H02 (estado y sesión) y ARG02-M06 (degradación) |
| Persistencia de la clave de idempotencia | Móvil: almacenamiento seguro (adecuado). **SPA: sigue abierto** (R2-N11, ARG02-L07) |
| Gestión de sesiones y manejo seguro de tokens | Los tokens no llegan al navegador (correcto). **No se define dónde viven** los tokens en el BFF (ARG02-H02) |
| Biometría móvil | Se distingue correctamente de KYC |
| Vinculación | Ruta `PRE_EXISTING` bloqueada (correcto) |
| Consulta del estado por identificador de operación | Definida (`GET /v1/transfers/{operationId}`) |
| Transferencias inciertas y saldo sin cambios | Regla explícita: un saldo sin cambios **no** prueba la no ejecución. Riesgo residual si el BFF no puede componer las operaciones en curso (ARG02-M06); IC-15 limita el daño |
| Accesibilidad y usabilidad | Sin un objetivo verificable (ARG02-L04) |

## 11. Preparación para la nube

| Capacidad | Dependencia del servicio gestionado | Qué verificar antes de seleccionar el proveedor |
|---|---|---|
| Entorno de contenedores | Techo de réplicas, control del surge, cierre ordenado, identidad de carga de trabajo | Que el producto permita RI-01 y RI-02 sin desarrollo propio |
| PostgreSQL gestionado | **Semántica de la réplica síncrona y su comportamiento al degradarse**; conmutación; protección contra varios escritores | ARG02-H01; difiere entre ofertas y configuraciones |
| Broker gestionado | Colas de mensajes fallidos, orden por clave, retención | Las garantías de orden difieren entre servicios (ARG02-M01) |
| Caché | Alta disponibilidad gestionada | Comportamiento ante conmutación (pérdida de marcas de invalidación) |
| Red | **Conectividad privada y redundante con el Core** | ARG02-H05 |
| Gestión de secretos | Servicio gestionado | Rotación y acceso por identidad de carga de trabajo |
| Monitoreo | Métricas, trazas y registros gestionados; SIEM | Q-13 |
| Zonas de disponibilidad | Número de zonas en la región elegida | Requisito para PT-01 sin comprometer la latencia |
| Recuperación ante desastres | Replicación entre regiones, normalmente **asíncrona** | ARG02-C01 |
| Protección de datos | Cifrado gestionado y residencia | **ADR-0010 sigue pendiente.** El comité **no ha verificado** la existencia de una región del proveedor en Ecuador ni el conjunto de servicios disponibles en las regiones cercanas: debe verificarse |
| Costo | — | **No se estiman precios.** Las decisiones con mayor efecto en el costo: un clúster aislado para el almacén transaccional, la replicación síncrona, la conectividad privada y la operación del IdP |

## 12. Hallazgos

### 12.1 Resumen

| Severidad | IDs |
|---|---|
| CRITICAL | ARG02-C01 |
| HIGH | ARG02-H01 a H05 |
| MEDIUM | ARG02-M01 a M07 |
| LOW | ARG02-L01 a L07 |

### 12.2 Detalle

| ID | Severidad | Título | Evidencia | Impacto | Recomendación | Criterio de aceptación | Fase |
|---|---|---|---|---|---|---|---|
| **ARG02-C01** | **CRITICAL** | PT-02 no protege las operaciones cuya reserva se perdió; un reenvío con la misma clave obtiene un permiso nuevo | ADR-0016:42 (PT-02 exige conciliar «las creadas en la ventana de pérdida», que ya no existen); 23:50; ADR-0006 IC-07 e IC-13 (el identificador propagado no se deriva de la clave); C3 del reto (DR) | **Doble ejecución financiera originada en la plataforma** tras una restauración o una conmutación regional asíncrona | Enmendar ADR-0016 y ADR-0006: (1) la cuarentena **rechaza las nuevas iniciaciones** (sin efectos) y no solo bloquea los permisos; (2) la referencia propagada al Core se **deriva de forma determinista** de (ámbito, clave) para poder buscarla a partir del reenvío; (3) tras una pérdida, toda reserva con una clave desconocida dentro de `VENTANA_SOSPECHA_PERDIDA` exige evidencia en el Core antes del permiso, o pasa a revisión manual; (4) reconstrucción desde el almacén de auditoría y la retención del broker; (5) declarar el residual si el Core no permite buscar por referencia | ADR enmendados; tabla de intercalación «restauración + reenvío con la misma clave» sin un segundo permiso | **Antes** de los componentes de ejecución y recuperación del servicio de transferencias (Fase 4); bloquea la implementación |
| **ARG02-H01** | HIGH | Degradación silenciosa de la replicación síncrona | ADR-0016:41 | Una conmutación posterior pierde escrituras sin activar la cuarentena | Detectar el estado de la réplica síncrona. Definir el «modo seguro» (sin conceder permisos mientras esté degradada) o declarar la cuarentena automática ante cualquier conmutación ocurrida tras una degradación. Decisión institucional entre disponibilidad e integridad | Regla en ADR-0016 y requisito de infraestructura | Fase 4 (diseño) e infraestructura; bloquea la implementación |
| **ARG02-H02** | HIGH | El BFF es «sin estado» pero también el gestor de tokens; el almacenamiento de sesiones y tokens no está definido | 16:176; ADR-0014:25 | Se desconocen las garantías de seguridad del justificante principal del BFF; puede faltar un contenedor (almacén de sesiones) | Decidir entre sesión cifrada en cookie (sin contenedor nuevo) y almacén de sesiones (contenedor nuevo en el nivel 2), con su revocación y su alta disponibilidad | ADR y, si corresponde, contenedor en el DSL | **Al inicio** de la Fase 4, antes de los componentes del BFF |
| **ARG02-H03** | HIGH | La auditoría síncrona de cada consulta debilita la disponibilidad sin análisis | 18:49; QA1, QA3 | Todas las consultas caen si cae la BD de consultas | Política de auditoría por categoría (sección 7.1); decisión institucional (Q-19) | ADR de auditoría por categoría | Fase 4 |
| **ARG02-H04** | HIGH | El punto de control de la vinculación depende de una credencial de administración amplia del IdP | ADR-0013:22; 22:47 | Comprometer el servicio de vinculación equivale a una toma masiva de cuentas; el identificador de cliente del token es la base de la autorización de todos los servicios | Mínimo privilegio (operación de vinculación acotada); afirmación de vinculación firmada y verificada por el IdP; límites y alertas; auditoría inmutable de los cambios de atributos de identidad | Diseño en la fase de seguridad | Fase de seguridad; bloquea la implementación de la vinculación |
| **ARG02-H05** | HIGH | Sin supuesto ni diseño sobre la ubicación del Core y la red plataforma–Core | Búsqueda sin resultados; 09:39 (solo una pregunta en C6) | La latencia (C6), la disponibilidad y la capacidad dependen de un enlace no modelado; dominio de falla común no reconocido | Agregar un supuesto (por ejemplo, «Core en el centro de datos de la institución») y una pregunta; tratar el enlace como un dominio de falla en los timeouts y en ADR-0011; conectividad privada redundante en la infraestructura | Supuesto y pregunta registrados; requisito de infraestructura | Registro inmediato (Fase 4); diseño en infraestructura; bloquea la implementación |
| ARG02-M01 | MEDIUM | Sin orden por operación, política de versionado ni reprocesamiento de eventos | ADR-0015:31; 19:22 | Notificaciones contradictorias; mensajes venenosos sin tratamiento | Número de secuencia por operación; consumidores que descartan estados anteriores; política de compatibilidad; procedimiento de reprocesamiento | Contratos de eventos en la Fase 4 | Fase 4 |
| ARG02-M02 | MEDIUM | El encadenamiento de auditoría no está definido para consumidores concurrentes y duplicados | 18:24 | Cadena inconsistente o un cuello de botella | Granularidad de la cadena, deduplicación previa, anclaje periódico | Diseño del componente de auditoría | Fase 4 |
| ARG02-M03 | MEDIUM | M1 puede asignar 0 por réplica; las sesiones del Core no se presupuestan | 19:67; 19:84 | Servicio sin acceso al Core; agotamiento de sesiones | RI-03 valida que la cuota por réplica sea ≥ 1; presupuesto de conexiones además de la concurrencia | RI ampliados | Fase 4 |
| ARG02-M04 | MEDIUM | Modelo operativo del IdP incompleto | ADR-0013; ADR-0016:36 | Brechas de disponibilidad y de seguridad en el componente más crítico | Rotación de claves y JWKS; revocación; recuperación; **identidad del personal separada** de la de los clientes; BD del IdP aislada | Diseño de seguridad e infraestructura | Fase 4 (identidad de la consola) y fase de seguridad |
| ARG02-M05 | MEDIUM | Sin fuente modelada de los datos de contacto ni de los tokens de dispositivo para las notificaciones | 18:23; DSL (sin relación app → notificaciones ni fuente de contactos) | R4 no es realizable tal como está modelado | Modelar el registro de dispositivos (app → BFF → notificaciones) y la fuente de los contactos (Core, Sistema Complementario o eventos) | Relaciones en el DSL y propiedad de datos | Fase 4 (con actualización del nivel 2) |
| ARG02-M06 | MEDIUM | La composición del BFF depende de forma síncrona del servicio de transferencias, sin degradación definida | 16 §9; ADR-0014 | La interfaz puede mostrar un saldo sin la operación en curso (M03 reaparece) | Indicador explícito «no se pudo verificar operaciones en curso»; bloqueo por IC-15 como respaldo | Regla de degradación del BFF | Fase 4 |
| ARG02-M07 | MEDIUM | La selección de la descomposición es sensible al supuesto sobre el equipo | Sección 4.1 | Con un equipo pequeño, A y B empatan | Confirmar Q-18 antes de implementar; reevaluar ADR-0012 si cambia | Q-18 respondida | Antes de la implementación |
| ARG02-L01 | LOW | Inventario informado de 16 frente a 19 contenedores en el DSL | 16:66; respuesta de la Fase 3 | Imprecisión documental | Una fila por contenedor | Tabla corregida | Fase 4 |
| ARG02-L02 | LOW | PT-01 a PT-03 aplican a la BD de vinculación, pero no se marcan en el DSL ni en 16 §4 | ADR-0016:37; 16:66 | Inconsistencia | Marcar la BD de vinculación | DSL y tabla actualizados | Fase 4 |
| ARG02-L03 | LOW | Criterios correlacionados en la matriz de decisión | 20 §3 | Sesgo leve a favor de B | Fusionar o justificar | Nota en 20 | Fase 4 |
| ARG02-L04 | LOW | Sin un objetivo verificable de accesibilidad | 21 §7 | Requisito de experiencia de usuario sin criterio | Fijar un nivel de conformidad candidato, por acordar | Criterio registrado | Fase 4 |
| ARG02-L05 | LOW | Sin regla contra las decisiones de estado sobre réplicas de lectura | Búsqueda sin resultados | Riesgo de implementación | Regla en ADR-0016 | Regla escrita | Fase 4 |
| ARG02-L06 | LOW | PT-06 no nombra los sidecars ni los proxies de salida | 23:54 | Reintentos ocultos por la infraestructura | Ampliar la redacción | PT-06 actualizada | Fase 4 |
| ARG02-L07 | LOW | La persistencia de la clave en la SPA (R2-N11) sigue sin decisión | ADR-0018 | Más duplicados semánticos desde la web | Decidir la intención creada en el servidor o el almacenamiento local con caducidad | Decisión registrada | Fase 4 |

## 13. Riesgos residuales

| Riesgo | Evaluación del comité |
|---|---|
| RR-19 (durabilidad del almacén transaccional) | **Subestimado:** ARG02-C01 y H01 muestran que PT-01 y PT-02 no cubren la DR regional. Residual: **alto** |
| RR-21 (IdP crítico) | Correcto; ampliar con ARG02-H04 y M04 |
| RR-20 (complejidad) | Correcto; ligado a ARG02-M07 |
| RR-17 (vivacidad), RR-18 (ejecución tardía) | Sin cambios |
| **Nuevo propuesto:** dependencia de la red plataforma–Core | Alto hasta ARG02-H05 |
| **Nuevo propuesto:** pérdida de disponibilidad de consultas por auditoría síncrona | Medio-alto hasta ARG02-H03 |

## 14. Condiciones

### 14.1 Para iniciar la Fase 4 (C4 Nivel 3)

| ID | Condición | Momento |
|---|---|---|
| F4-01 | Los **componentes de ejecución y recuperación del servicio de transferencias** no se diseñan hasta resolver ARG02-C01 en ADR-0016 y ADR-0006 | Antes de esos componentes |
| F4-02 | Decidir el estado y la sesión del BFF (ARG02-H02) y actualizar el nivel 2 si se agrega un contenedor | Al inicio de la Fase 4 |
| F4-03 | Registrar el supuesto y la pregunta sobre la ubicación del Core y su red (ARG02-H05) | Al inicio de la Fase 4 |
| F4-04 | Corregir el inventario (L01, L02) | Al inicio de la Fase 4 |

### 14.2 Que deben resolverse durante la Fase 4

ARG02-H01 (diseño), H03, M01, M02, M03, M04 (identidad de la consola), M05 (con actualización del nivel 2), M06, L03 a L07.

### 14.3 Que bloquean la implementación

ARG02-C01, H01, H04, H05 y M07, además de los bloqueos vigentes:

- B-01 a B-04 de [23](../23-riesgos-y-condiciones-fase-3.md);
- la verificación independiente de CF-01 y CF-02;
- PT-01 a PT-06 verificados.

### 14.4 Que bloquean la producción

- CP-01 a CP-09 (ARG-01-R2).
- DD-21 implementada (ruta `PRE_EXISTING`).
- ARG02-M04 (claves, revocación, recuperación del IdP).
- Simulacro de DR que demuestre ARG02-C01 resuelto.
- ADR-0010 validado.

**No se penalizan** las decisiones diferidas legítimamente: proveedor cloud, productos, flujo OAuth, proveedores KYC y de notificación, M2 y CQRS.

## 15. Decisión del gate

### Veredicto: **CONDITIONAL GO, con alcance restringido**

**Fundamento:**

1. La descomposición, los límites entre contenedores, la propiedad de los datos, la interpretación del Gateway y la protección de la capacidad del Core son **sólidos y defendibles**. Ningún hallazgo invalida la selección de la arquitectura.
2. **ARG02-C01 es CRITICAL** por su impacto (duplicación financiera originada en la plataforma). El comité **no** emite NO-GO porque:
   - el defecto está **confinado** al servicio de transferencias, ya PROVISIONAL y con la implementación bloqueada (B-01);
   - **no afecta** el diseño de componentes de los demás contenedores;
   - se le impone un bloqueo de alcance explícito (F4-01).

   Esta decisión **se aparta deliberadamente** de la regla de ARG-01 («un CRITICAL abierto implica NO-GO»), que regía para las Fases 0–2. Aquí el comité dispone de un mecanismo más preciso: bloquear el alcance afectado. Si la aprobación humana no acepta ese criterio, el veredicto equivalente es **NO-GO para los componentes del servicio de transferencias y GO condicional para el resto**.
3. Los HIGH tienen condiciones con fase y criterio de aceptación definidos.
4. **El comité no aprueba la implementación productiva.**

---

**ARG-02 COMPLETADO. PENDIENTE DE APROBACIÓN HUMANA PARA INICIAR C4 LEVEL 3.**
