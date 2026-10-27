# ARG-03 — Revisión de componentes y verificación del diario de despacho

| Campo | Valor |
|---|---|
| Revisión | ARG-03 |
| Objeto | Commits `4d39a3c` (remediación de ARG-02) y `18cff1d` (C4 Nivel 3); ADR-0005, 0006, 0016 y 0021; CF-01 y CF-02 |
| Fecha | 2026-10-27 |
| Comité (perfiles simulados) | Principal Banking Solutions Architect; Distributed Systems Architect; Security Architect; DevOps |
| Veredicto | **CONDITIONAL GO para la base técnica no financiera. NO-GO para la ejecución financiera y para el alta de clientes en el Core** |

> **Limitación de independencia:** esta revisión la produce el mismo agente que diseñó la arquitectura. Aplica criterios adversariales, pero **no sustituye** la verificación de un tercero humano. **No certifica preparación para producción.** Todo el análisis es conceptual.

---

## 1. Resumen ejecutivo

**El diseño de componentes de los cinco contenedores autorizados es coherente** con Clean Architecture, con la propiedad de los datos y con los ADR. No contiene defectos que impidan construir una base técnica **no financiera**: consultas, vinculación sin activación, notificaciones y auditoría local.

**ADR-0021 no se aprueba.** Su principio (ningún despacho sin una entrada durable en un diario independiente) es correcto, pero:

- **ARG03-C01 (CRITICAL):** la topología de ejemplo «dos regiones y un testigo» (`ADR-0021:54`) **puede confirmar entradas guardadas en una sola región**. Perder esa región pierde entradas confirmadas y rompe INV-DR-1.
- **ARG03-H01:** no se trata la **restauración del propio diario** desde una instantánea antigua.
- **ARG03-H02:** las garantías dependen de un mecanismo de consenso y cercado **no especificado** (DD-23).

**Tres dominios de falla no implican, por sí mismos, durabilidad ni consenso.** B-05 se mantiene, y la ejecución financiera y el alta en el Core siguen bloqueadas.

## 2. Inspección

| Evidencia | Estado |
|---|---|
| Árbol de trabajo | Limpio; `main` sincronizada con `origin/main` en `18cff1d` |
| Historial | Commits de las Fases 0 a 4 y de las revisiones ARG-01, ARG-01-R2 y ARG-02; sin trailers de coautoría |
| Documentos leídos | Reto (PDF); ADR-0001 a ADR-0021; ARG-01, ARG-01-R2, remediación CF-01/CF-02, ARG-02, su registro y su revisión focalizada; lista ARG-03; documentos 02, 04, 07, 08, 10, 11, 15, 16, 18, 19 y 22 a 26; DSL |

## 3. Verificación de ADR-0021 (diario de autorización de despacho)

| Propiedad pretendida | ¿Garantizada por el diseño? | Análisis |
|---|---|---|
| Ningún despacho sin una autorización durable | **Sí, en el protocolo** (paso 3b) | Siempre que el ejecutor espere la confirmación. Depende de la disciplina del ejecutor (CP-03) |
| Ninguna pérdida de entradas confirmadas ante fallas soportadas | **No demostrada** | **ARG03-C01:** con un testigo que solo vota, una entrada puede confirmarse con la región A + el testigo y existir solo en A. Si se pierde A, B + testigo siguen formando quórum **sin esa entrada** |
| Una sola autoridad de despacho tras la conmutación | **Condicionada** | La época regional funciona **solo si** la escritura condicional sobre la época es linealizable y la reconfiguración del grupo es segura. No está especificado (ARG03-H02) |
| Recuperación sin ejecución financiera duplicada | **Condicionada** | R-4 depende de leer el diario **completo y actualizado** (lecturas linealizables o lecturas en el líder con confirmación de quórum). Si el diario perdió entradas (C01 o H01), R-7 trata como nueva una clave despachada → **duplicado** |
| Comportamiento seguro sin quórum del diario | **Sí** | Falla cerrada (DEGRADADO_DIARIO): sin despachos |
| Lecturas consistentes durante la recuperación | **Requerida, no especificada** | ARG03-H02 |

### 3.1 Escenarios exigidos

| Escenario | Resultado | Razón |
|---|---|---|
| Partición de red entre regiones | **Seguro, si** el diario usa un consenso de mayoría | El lado minoritario no puede anexar ni, por tanto, despachar. Con un consenso no especificado: indeterminado |
| Cerebro dividido | **Seguro, si** el cambio de época es linealizable | La región con la época anterior es rechazada |
| Pérdida de quórum | Seguro (falla cerrada); disponibilidad perdida | DEGRADADO_DIARIO |
| Lectores con datos atrasados | **Inseguro** si la recuperación o IC-16 leen de una réplica atrasada | Una clave anotada pero no visible se tomaría como nueva. Exige lecturas linealizables (ARG03-H02) |
| Replicación retrasada | Seguro solo si la confirmación espera la escritura durable en **≥ 2 regiones con datos** | ARG03-C01 |
| Confirmación perdida tras un *commit* exitoso | Seguro | El ejecutor no envía → `*_UNKNOWN` sin despacho real → caso manual (vivacidad). *Mejora posible:* releer la propia entrada en forma linealizable y enviar si existe con su token (ARG03-L02) |
| Caída del proceso tras la autorización y antes del despacho | Seguro; vivacidad perdida | Igual que en ADR-0005 §4.4 |
| Despacho tardío del antiguo líder regional | Seguro (envío único tardío) | Su entrada existe; no hay un segundo permiso. La ejecución tardía es RR-18 |
| **Restauración del diario desde una instantánea antigua** | **INSEGURO** | Las entradas posteriores a la instantánea desaparecen. Las claves despachadas en ese intervalo se tratarían como nuevas (ARG03-H01) |
| Efecto en el Core tras un cierre administrativo | Detectado, no prevenido | Caso de efecto tardío (INV-T11); límite ya declarado |

## 4. Verificación de CF-01 y CF-02 (intercalaciones)

| # | Intercalación | Seguridad | Disponibilidad | Supuestos externos |
|---|---|---|---|---|
| 1 | Timeout de lectura tras la ejecución en el Core → conciliación sin coincidencias → plazo vencido | **Preservada:** sin cierre por EV-INC (INV-T9) | Caso manual | — |
| 2 | Dos ejecutores; el primero pierde el arrendamiento tras obtener el permiso | **Preservada:** permiso único (INV-T3) | Respuesta tardía como evidencia huérfana | S19 |
| 3 | `NOT_SENT` mal clasificado por la biblioteca | **No demostrada** (depende de CP-02) | — | Implementación |
| 4 | Reverso con resultado desconocido | **Preservada:** sin un segundo reverso | Manual | — |
| 5 | Cierre excepcional → nueva intención con aviso → efecto tardío de la original | **No prevenida** (límite declarado; detección por vigilancia) | — | — |
| 6 | Confirmación por correlación (EV-POS-CORR) con coincidencia errónea | **Riesgo de omisión**, no de duplicado: se marca `COMPLETED` algo que no se ejecutó | — | RR-04 |
| 7 | El intermediario frente al Core reintenta una escritura | **No garantizada** | — | **S19** |
| 8 | El Core aplica dos veces una misma solicitud | **No garantizada** | — | **S20** |

**Conclusión:** CF-01 y CF-02 se sostienen **en el diseño**. Sus garantías son de **seguridad** y no de disponibilidad, y dependen de CP-02, S19 y S20, además de **ADR-0021 corregido** para los escenarios de recuperación regional. Recomendación: que EV-POS-CORR exija revisión manual por encima de un umbral de monto (`UMBRAL_CONFIRMACION_CORRELACION`, por acordar).

## 5. Verificación de las vistas de componentes

| Criterio | BFF | Consultas | Vinculación | Notificaciones | Auditoría |
|---|---|---|---|---|---|
| Fronteras de Clean Architecture | ✔ | ✔ | ✔ | ✔ | ✔ |
| Dirección de las dependencias | ✔ | ✔ | ◐: el servicio de aplicación depende del **puerto bloqueado** del alta (ARG03-M02) | ✔ | ✔ |
| Propiedad de los datos | ✔ (sesiones) | ✔ (sin fuente de negocio) | ✔ | ✔ | ✔ |
| Puertos y adaptadores | ✔ | ◐: no se define la **fuente ni la caché** de la relación cliente-cuenta para la autorización (ARG03-M01) | ✔ | ✔ | ✔ |
| Manejo de errores | ✔ | ✔ | ✔ | ✔ | ✔ |
| Límites transaccionales | n/a | outbox | ✔ | ✔ | ✔ (deduplicación + anexión) |
| Seguridad | ✔ | ✔ | ✔ | ✔ | ✔ |
| Integración con la auditoría | A7 | A4/A5 doble ruta | A3/A6 | ◐: categoría ambigua de los intentos (ARG03-L01) | A6 en las lecturas |
| Observabilidad | ✔ | ✔ | ✔ | ✔ | ✔ |
| Consistencia de contratos | ✔ | ✔ | ✔ | ✔ | ✔ |

## 6. Hallazgos

| ID | Severidad | Hallazgo | Evidencia | Recomendación | Afecta la base no financiera |
|---|---|---|---|---|---|
| **ARG03-C01** | **CRITICAL** (alcance financiero) | La topología de ejemplo con un testigo permite confirmar entradas guardadas en una sola región con datos | `ADR-0021:54` | Exigir que **toda confirmación** requiera la escritura durable en **réplicas con datos de al menos dos regiones distintas**. Un testigo **nunca** cuenta como réplica con datos. Prohibir la reconfiguración forzada del grupo sin cuarentena global | **No** |
| **ARG03-H01** | HIGH (financiero) | No se trata la restauración del diario desde una instantánea | `ADR-0021` §6 (solo trata la BD transaccional) | Prohibir que un diario restaurado vuelva a autorizar despachos sin el **modo de diario incompleto**: cuarentena global y revisión manual o confirmación en el Core de toda clave del intervalo perdido | No |
| **ARG03-H02** | HIGH (financiero) | Las garantías dependen de un consenso y un cercado no especificados | `ADR-0021:54–55`; DD-23 | Especificar la semántica exigible y verificable: consenso de mayoría, escritura condicional linealizable para las entradas y la época, lecturas linealizables, reconfiguración segura. **Demostrar** la semántica del producto con pruebas de partición antes de levantar B-05 | No |
| ARG03-M01 | MEDIUM | La autorización de recursos del servicio de consultas no define la fuente ni la caché de la relación cliente-cuenta | 24 §4.2; DSL `conAutorizacion` | La relación se obtiene del Core mediante la ACL de clientes; caché corta por sujeto; en caso de duda se deniega (falla cerrada) | Sí: se resuelve en la implementación de la base |
| ARG03-M02 | MEDIUM | El servicio de aplicación de vinculación depende del puerto de alta bloqueado | DSL (`vinAplicacion -> vinAlta`) | Implementar el puerto con un **adaptador deshabilitado** explícito y pruebas que demuestren que E3 y E5 son inalcanzables | Sí: condición de la implementación |
| ARG03-L01 | LOW | La categoría de auditoría de los intentos de notificación es ambigua | `24:141` | A5 para los intentos de notificación de movimientos (evidencia de R4) | Sí (menor) |
| ARG03-L02 | LOW | La confirmación perdida tras un *commit* del diario cuesta vivacidad sin necesidad | ADR-0021 §3 | Releer la propia entrada de forma linealizable | No |
| ARG03-L03 | LOW (gobierno) | La revisión no es independiente de la autoría | Este documento | Revisión humana de un tercero antes de levantar B-05 | No |

## 7. Veredicto

### **CONDITIONAL GO para la base técnica no financiera. NO-GO para la ejecución financiera y para el alta en el Core.**

| Alcance | Decisión | Condiciones |
|---|---|---|
| BFF, servicio de consultas, vinculación **sin activación ni alta**, notificaciones, auditoría local, simulador del Core, entorno local, CI | **Autorizado** | ARG03-M01 y M02 se resuelven en la implementación; sin datos reales; sin conexión con el Core ni con la red de la institución |
| Servicio de transferencias ejecutable | **No autorizado** | Solo se permite un módulo de **contrato** |
| Alta de clientes en el Core y activación del acceso digital | **No autorizado** | Puertos con adaptadores deshabilitados |
| Producción | **No autorizado** | CP-01 a CP-10; DD-21; DD-23; ARG03-C01 a H02 |

**Para levantar B-05 se necesita:**

1. una enmienda de ADR-0021 que resuelva ARG03-C01, H01 y H02;
2. la verificación independiente humana de CF-01, CF-02 y ADR-0021 corregido;
3. evidencia de la semántica del producto del diario (DD-23).
