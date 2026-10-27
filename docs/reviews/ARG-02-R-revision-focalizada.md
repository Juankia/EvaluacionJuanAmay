# ARG-02-R — Revisión focalizada de la remediación (gate previo a la Fase 4)

| Campo | Valor |
|---|---|
| Objeto | [Registro de remediación de ARG-02](ARG-02-registro-de-remediacion.md) |
| Tipo | **Revisión del autor**, focalizada y adversarial. **No sustituye una verificación independiente** |
| Fecha | 2026-10-27 |
| Resultado | **Remediación completada en diseño y verificada por el autor; aprobación independiente pendiente.** Fase 4 autorizada **solo** para el alcance no afectado |

> Análisis conceptual; no existe software y **no se ejecutó ninguna prueba**.

## 1. Verificaciones exigidas

| # | Verificación | Resultado | Evidencia |
|---|---|---|---|
| 1 | ARG02-C01 tiene un diseño de seguridad exigible | **Sí, en diseño.** INV-DR se hace cumplir con INV-DR-1 (sin entrada en el diario no hay despacho), sin depender del Core. Depende de DD-23 o del modo de suspensión | ADR-0021; sección 2 |
| 2 | La degradación de la durabilidad (H01) se detecta y se trata | **Sí, en diseño.** Cuatro modos; toda conmutación es INCIERTA hasta demostrar lo contrario; CP-10 valida la configuración real | ADR-0016 (enmienda) |
| 3 | La propiedad de la sesión del BFF (H02) es explícita | **Sí** | ADR-0019; contenedor nuevo |
| 4 | El comportamiento ante fallas de auditoría (H03) está documentado | **Sí**, por categoría | ADR-0020; QA15 |
| 5 | Las operaciones privilegiadas del IdP (H04) están restringidas | **Sí, en diseño**; mecanismo en la fase de seguridad | ADR-0013 (enmienda) |
| 6 | La ubicación del Core (H05) consta como supuesto o hecho | **Sí, como DESCONOCIDA** (S21, Q-20) | 02; 19 §5 |
| 7 | El inventario de contenedores es exacto | **Sí: 21**, contados en el DSL | 16 §4; DSL |
| 8 | Los ADR y los escenarios de calidad concuerdan | **Sí.** QA14 ↔ ADR-0021; QA15 ↔ ADR-0020; QA9 y QA12 sin cambios | 04 |
| 9 | Los requisitos siguen siendo trazables | **Sí** | 09 §9 |
| 10 | No se introdujeron garantías sin respaldo | **Sí.** INV-DR se declara distinta de la ejecución exactamente una vez; S19, S20 y DD-22 siguen abiertos; la ubicación del Core no se inventó; no se citan cifras de latencia ni precios | ADR-0021 §1 |

## 2. Búsqueda de contraejemplos contra ADR-0021

| ID | Intercalación | ¿Se preserva INV-DR? | Razón |
|---|---|---|---|
| CE-1 | Promoción (incremento de la época) concurrente con una anexión de la región anterior | Sí | El diario es linealizable: la anexión queda antes (y entra en el recorrido) o se rechaza |
| CE-2 | Anexión confirmada en el diario, permiso confirmado, envío, y después pérdida de la BD | Sí | R-4 reconstruye la operación desde el diario |
| CE-3 | Anexión con resultado incierto (timeout) | Sí | No se envía. Si la anexión se aplicó, la operación queda `*_UNKNOWN` **sin despacho real** → caso manual (vivacidad, RR-17) |
| CE-4 | Tras la recuperación, reenvío con **otra** clave (duplicado semántico) | Sí | El diario conserva la huella; las operaciones reconstruidas restablecen la unicidad IC-15 |
| CE-5 | Cerebro dividido: la región anterior sigue recibiendo solicitudes y reservando en su BD | Sí | Sin época vigente no puede anotar en el diario, y por tanto no despacha. Sus reservas no generan efectos |
| CE-6 | Envío tardío de la región anterior anotado antes del cercado | Sí (envío único tardío) | Igual que un ejecutor zombi (RR-18) |
| CE-7 | Rotación de la clave de derivación HMAC entre el despacho y el reenvío | Sí | El diario conserva el resumen del ámbito y la clave, y las versiones de la clave se retienen |
| CE-8 | Alta de cliente perdida y reenviada | Sí | Mismo protocolo (tramo `CUSTOMER_CREATION`) |
| CE-9 | No se puede formar el quórum del diario | Sí, con pérdida de disponibilidad | Modo DEGRADADO_DIARIO: sin despachos |

**No se encontró ningún contraejemplo a INV-DR en el diseño.**

**Observación (no es un hallazgo de seguridad):** el recorrido R-4 de «todas las entradas sin estado terminal» puede ser costoso si el diario solo anota despachos. Se recomienda anexar también marcadores de cierre terminal para acotar el recorrido. Se registra para la fase de infraestructura.

## 3. Lo que esta revisión **no** puede cerrar

- La **verificación independiente** de CF-01, CF-02 y ADR-0021: requisito para levantar B-05.
- DD-23 (existencia de la capacidad del diario en la topología disponible) y Q-21.
- Q-20 (ubicación del Core), Q-18 (equipo), Q-19 (retención).
- S19 y S20.

## 4. Decisión

- **Remediación:** completada en diseño; verificada por el autor; **pendiente de aprobación independiente**.
- **Fase 4:** autorizada para el BFF, el servicio de consultas, el servicio de vinculación (excepto la ejecución y recuperación del alta), el servicio de notificaciones y el servicio de auditoría.
- **Bloqueado (B-05):** los componentes de ejecución y recuperación de transferencias y del alta de clientes.
