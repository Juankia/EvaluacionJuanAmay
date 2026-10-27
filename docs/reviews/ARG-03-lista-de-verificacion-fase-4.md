# ARG-03 — Lista de verificación de la Fase 4 y criterios de entrada

| Campo | Valor |
|---|---|
| Objeto | Fase 4 — C4 Nivel 3 (componentes) y remediación de ARG-02 |
| Tipo | Lista de verificación preparada por el autor para el comité. **No es una revisión ni una aprobación** |
| Fecha | 2026-10-27 |

## 1. Resultado declarado por el autor

| Elemento | Estado |
|---|---|
| Remediación de ARG-02 | **Completada en diseño; verificada por el autor ([ARG-02-R](ARG-02-R-revision-focalizada.md)); aprobación independiente pendiente** |
| Arquitectura de componentes | **Parcialmente completada:** BFF, consultas, vinculación (salvo el alta), notificaciones y auditoría |
| Bloqueado pendiente de evidencia | Servicio de transferencias y ejecutor del alta (B-05); DD-23; Q-20; Q-21 |

## 2. Criterios de entrada de ARG-03

1. Los dos commits de la remediación de ARG-02 y de la Fase 4 están en el repositorio y el árbol está limpio.
2. El DSL valida y los diagramas son reproducibles desde el DSL.
3. El inventario declarado coincide con el DSL: **21 contenedores**; componentes solo en los cinco contenedores autorizados.
4. La revisión dispone de [ADR-0019](../adr/ADR-0019-sesiones-del-bff.md), [ADR-0020](../adr/ADR-0020-politica-de-auditoria-por-categoria.md) y [ADR-0021](../adr/ADR-0021-diario-de-autorizacion-de-despacho.md), y de las enmiendas de ADR-0005, 0006, 0013, 0014 y 0016.
5. ARG-03 incluye la **verificación independiente** de CF-01, CF-02 y de la remediación de ARG02-C01 (ADR-0021): sin ella, B-05 no puede levantarse.

## 3. Lista de verificación

| # | Verificación | Evidencia |
|---|---|---|
| 1 | ¿INV-DR resiste intercalaciones que la revisión focalizada no exploró (por ejemplo, la operación del diario, su migración o su restauración)? | ADR-0021; ARG-02-R §2 |
| 2 | ¿Los modos de durabilidad cubren todas las formas de conmutación del servicio gestionado candidato? | ADR-0016 (enmienda) |
| 3 | ¿La sesión del BFF es segura ante XSS, CSRF y robo de la cookie? | ADR-0019 |
| 4 | ¿La doble ruta de auditoría mantiene la durabilidad exigida para A4 y A5? | ADR-0020; QA15 |
| 5 | ¿La operación de vinculación acotada impide la toma masiva aun con la clave de firma comprometida? | ADR-0013; 26 escenario 3 |
| 6 | ¿Algún componente diseñado depende de la estructura interna del servicio de transferencias? | 24 §4.6 |
| 7 | ¿Las reglas de orden de los eventos cumplen R4 (NOT-1) sin bloquear indefinidamente? | 25 §4.3 |
| 8 | ¿Las ACL siguen ADR-0001 y no aparecen como contenedores? | DSL; 24 §3 |
| 9 | ¿Los contratos evitan la enumeración (VD-07, `RESOURCE_NOT_FOUND`)? | 25 §2.3 y §3 |
| 10 | ¿Ninguna garantía nueva carece de respaldo (exactamente una vez, latencia, cumplimiento)? | 24; 25; 26 |
| 11 | ¿La sensibilidad de la descomposición (Q-18) se trata como una dependencia antes de la implementación? | 20 §6 |
| 12 | ¿La trazabilidad de requisitos se mantiene en los componentes? | 24 §7; 09 |
