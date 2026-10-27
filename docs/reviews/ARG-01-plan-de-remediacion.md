# ARG-01 — Plan de remediación

| Campo | Valor |
|---|---|
| Revisión de origen | [ARG-01 — Revisión arquitectónica](ARG-01-revision-arquitectonica.md) |
| Veredicto vigente | **NO-GO** |
| Condición para reevaluar | Cierre de ARG01-C01 (y M01, que lo acompaña) con aprobación humana. Revisión focalizada ARG-01a |
| Responsable | Arquitecto de la solución (autor). Las enmiendas a los ADR requieren la autorización del responsable humano del repositorio |

## 1. Prioridades

| Prioridad | Significado |
|---|---|
| **P0 — Bloqueante** | Debe cerrarse antes de iniciar la Fase 3 |
| **P1 — Condición de la Fase 3** | Puede iniciarse la Fase 3, pero debe cerrarse antes de aprobarla |
| **P2 — Planificado** | Debe cerrarse en la fase indicada |
| **P3 — Menor** | Corrección de claridad o de gobierno, sin impacto en el diseño |

## 2. Plan

| Hallazgo | Prioridad | Corrección recomendada | Criterio de aceptación | Dependencias | Fase de resolución | Estado |
|---|---|---|---|---|---|---|
| **ARG01-C01** — Identidad lógica ante los reintentos del cliente | **P0** | Enmendar ADR-0006 y `docs/08`: clave de canal **obligatoria**, generada una vez por intención del usuario y persistida en el dispositivo; alcance de unicidad (cliente, tipo de operación, clave); huella canónica del payload (misma huella → estado existente sin ejecutar; huella distinta → rechazo sin efectos); ventana de retención ≥ horizonte de reintento; vínculo 1:1 con la clave de dominio; declarar la garantía real como «como máximo una iniciación por intención», no ejecución exactamente una vez | ADR-0006 enmendado y aprobado. F03, F04 y F12 con comportamiento documentado. Escenario de calidad del reintento del cliente con medida | Autorización humana para modificar un ADR aprobado | Antes de la Fase 3 | Abierto |
| ARG01-M01 — QA3 promete cero duplicados | **P0** (acompaña a C01) | Reformular la medida de QA3 y agregar una métrica de exactitud de la conciliación | QA3 coherente con RR-04 | C01 | Antes de la Fase 3 | Abierto |
| ARG01-H01 — Clasificación de fallas previas al envío | P1 | Tabla cerrada de clasificación de fallas (`FAILED` o `UNKNOWN`) en ADR-0005 o `docs/07`, considerando pools de conexión e intermediarios | Tabla aprobada y referenciada desde `docs/05` | S3 (conocer si hay intermediarios frente al Core) | Fase 3 | Abierto |
| ARG01-H02 — Modelo de estados interbancario | P1 | Estado explícito de reverso pendiente; semántica de `COMPLETED` por tipo de transferencia; renombrar `SPI_PROCESSING` al resolver Q-01 | Ninguna transición a `FAILED` con efecto no revertido | Q-01, DD-16 | Fase 3 (modelo); nombre al resolver Q-01 | Abierto |
| ARG01-H03 — Conciliación manual sin responsable | P1 | Confirmar el área y el medio (DD-18); parámetro `T_RESOLUCION_MANUAL`; escalamiento y comunicación al cliente | DD-18 resuelto o con decisión provisional; parámetro registrado | Q-08 (institución) | Fase 3 | Abierto |
| ARG01-H04 — Protección del Core ante el escalado | P1 | Completar ADR-0001: aislamiento lógico frente a físico, presupuesto de capacidad del Core por dominio y prioridad para las escrituras; mecanismo para N réplicas en la Fase 3 | Demostración de que la concurrencia total ≤ `MAX_CONCURRENCIA_CORE` | S4 (capacidad del Core) | Análisis antes de la Fase 3; mecanismo en la Fase 3 | Abierto |
| ARG01-H05 — Onboarding y clientes preexistentes | P1 | Enmendar ADR-0009: concluir `CORE_CREATED` solo con evidencia de creación por esta vinculación; flujo separado de enrolamiento de clientes existentes; pregunta a la institución sobre el alcance de «nuevos usuarios» | ADR-0009 distingue cliente creado de cliente preexistente | Q-02 (búsqueda en el Core), autorización humana | Fase 3 | Abierto |
| ARG01-M02 — Recuperación, concurrencia y onboarding incompleto | P2 | Reglas de recuperación de estados en curso; separar `PENDING_CORE`; estado o regla de indisponibilidad del proveedor KYC; concurrencia de transiciones independiente de la tecnología en ADR-0005 | Reglas en ADR-0005 y máquina de onboarding actualizada | H01 | Fase 3 | Abierto |
| ARG01-M03 — Carrera de Cache-Aside y pérdida de la invalidación | P2 | Documentar la carrera y su mitigación conceptual, el reintento de la invalidación y el mecanismo de *read-your-writes*; TTL de las categorías sensibles con negocio | `docs/06` actualizado; TTL registrados como objetivos | Negocio (TTL) | Fase 3 | Abierto |
| ARG01-M04 — Resiliencia y plazos sin ADR | P2 | Nuevo ADR (siguiente número): timeout del Gateway ≥ plazo + margen; respuesta «en verificación» o patrón asíncrono; precedencias | ADR aprobado y referenciado | C01 | Fase 3 | Abierto |
| ARG01-M05 — Registro regulatorio inexistente | P2 | Crear el registro `REG-PV` y referenciarlo desde S8, S14, S15, ADR-0010 y C1, sin afirmar cumplimiento | Registro creado y referenciado | Q-05 (tipo de institución) | Fase 3 | Abierto |
| ARG01-M06 — Fraude y prevención de lavado subpriorizados | P2 | Elevar Q-12 a impacto alto; agregar candidatos en la vista ampliada; registrar como `REG-PV` | Q-12 reclasificada; candidatos modelados o exclusión justificada | M05 | Fase 3 | Abierto |
| ARG01-M07 — REL-05 como «Confirmada» | P2 | Etiqueta y estilo propios («dependencia confirmada, ruta por validar»), explicados en la leyenda; regenerar los diagramas | Estilo diferenciado en el DSL y en los SVG | — | Inicio de la Fase 3 | Abierto |
| ARG01-M08 — Acoplamiento del SDK | P2 | Actualizar ADR-0003: redespliegues coordinados por seguridad como consecuencia aceptada; compatibilidad de comunicación frente a binaria; ventana de soporte N-1 | ADR-0003 actualizado | — | Fase 3 | Abierto |
| ARG01-M09 — Escenarios de seguridad del onboarding | P2 | Agregar QA9 (ataque biométrico de presentación o inyección) y QA10 (abuso automatizado y de costos), con seis partes y parámetros | Escenarios registrados en `docs/04` | DD-14 | Antes de la fase de seguridad | Abierto |
| ARG01-L01 — Fronteras organizacional y de despliegue | P3 | Ampliar `docs/14` §1.1 con las cuatro fronteras | Tabla ampliada | — | Fase 3 | Abierto |
| ARG01-L02 — Criterio BFF | P3 | Criterio: el BFF solo agrega lecturas independientes; escrituras, secuencias y reglas son de dominio | Criterio en ADR-0002 o en DD-10 | DD-10 | Fase 3 | Abierto |
| ARG01-L03 — Gobierno de los ADR | P3 | Agregar los campos «Aprobado por» y «Revisión»; ciclo Propuesta → Aceptada tras cada gate | Plantilla y ADR actualizados | Decisión del comité | Tras ARG-01 | Abierto |
| ARG01-L04 — Commit fuera de la convención | P3 | Corregir el mensaje de `c1a56d7` (reescritura del historial publicado) o documentar la excepción | Mensaje corregido o excepción registrada | Decisión del autor | Inmediata | Abierto |
| ARG01-L05 — markdownlint | P3 | Desactivar o normalizar MD060; corregir los 3 MD036 | `markdownlint-cli2` sin errores | — | Antes de la validación en CI | Abierto |

## 3. Secuencia recomendada

```text
1. (P0)  C01 + M01  ──► revisión focalizada ARG-01a ──► CONDITIONAL GO esperado
2. (P1)  H04 (análisis en ADR-0001) antes del diagrama de contenedores
3. (P1)  H01, H02, H03, H05 durante la Fase 3, antes de aprobarla
4. (P2)  M02–M09 dentro de la Fase 3 o en la fase indicada
5. (P3)  L01–L05 en cualquier momento
```

## 4. Seguimiento

El estado de cada hallazgo se actualiza en este documento: Abierto, En curso, Resuelto (pendiente de verificación) o Cerrado (verificado por el comité). Un hallazgo solo se cierra cuando el comité verifica su criterio de aceptación.
