# ADR-0010 — Región cloud y residencia de datos sujetas a validación legal y de cumplimiento

| Campo | Valor |
|---|---|
| Estado | **Propuesta**. Pendiente de validación legal y de cumplimiento |
| Fecha | 2026-10-27 |
| Requisitos | C1, C6 |
| Drivers y supuestos | S8, S15; AQ-01, AQ-06 |
| Relacionados | [ADR-0008](ADR-0008-invalidacion-por-eventos.md), [Política de documentación, sección 5](../00-politica-de-documentacion.md#5-diferenciación-entre-requisitos-supuestos-y-obligaciones-regulatorias) |

## Contexto

El reto permite usar AWS o Azure «si lo considera necesario» y pide garantizar baja latencia (C6). También pide mencionar los elementos regulatorios relevantes (C1). La plataforma tratará:

- datos personales;
- datos financieros;
- datos biométricos, que son una categoría especialmente sensible.

La legislación ecuatoriana de protección de datos personales (Ley Orgánica de Protección de Datos Personales) regula, entre otros aspectos, la transferencia internacional de datos. Su aplicación concreta a esta plataforma **no ha sido validada** por el área legal.

## Problema

¿En qué región cloud se alojan la plataforma y sus datos? ¿Qué condiciones aplican a las transferencias de datos fuera del Ecuador, incluidas las que hagan los proveedores de KYC, biometría y notificaciones?

## Decisión

1. **La región cloud y la residencia de datos no se fijan en esta fase.** Quedan sujetas a:
   - la legislación ecuatoriana;
   - los requisitos del organismo de control correspondiente al tipo de institución (S15);
   - las políticas de cumplimiento de la institución;
   - la validación del área legal.
2. **No se afirma cumplimiento normativo.** La arquitectura busca facilitar el cumplimiento; la verificación corresponde a cumplimiento y a legal.
3. Los mecanismos de otras jurisdicciones, como las cláusulas contractuales tipo (*Standard Contractual Clauses*, SCC) de la Unión Europea, **no se presentan como requisito legal ecuatoriano**. Pueden citarse solo como referencia comparativa.
4. La arquitectura lógica de las Fases 0–2 se mantiene **independiente de la región** para que esta decisión no la invalide.
5. Criterios que la decisión final deberá ponderar:
   - restricciones legales de residencia y de transferencia internacional;
   - latencia hacia el Core, la base de datos principal y los usuarios (C6);
   - disponibilidad de servicios gestionados en la región;
   - estrategia de DR entre regiones (DD-09);
   - ubicación del procesamiento de los proveedores de KYC y biometría;
   - costo.

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| Fijar ahora una región extranjera por la oferta de servicios | Descartada en esta fase | Decisión con implicaciones legales sin validar |
| Exigir ahora residencia exclusiva en el Ecuador | Descartada en esta fase | Sería presentar un supuesto legal como hecho. Puede limitar sin fundamento las opciones de proveedor |
| **Diferir con criterios explícitos y arquitectura lógica independiente de la región** | **Seleccionada** | Evita afirmaciones legales no verificadas |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Ninguna afirmación legal sin respaldo | La infraestructura y la selección de proveedores quedan condicionadas |
| Arquitectura lógica estable | Posible retrabajo de infraestructura según el resultado legal |

## Consecuencias

- La fase de infraestructura no puede cerrarse sin esta validación.
- Los proveedores de KYC, biometría y notificaciones deben evaluarse también por la ubicación del procesamiento de datos.
- La consideración se registra como `REG-PV` (consideración regulatoria por validar) según la política de documentación.

## Riesgos

- RR-06: decisión pendiente sobre residencia de datos y cumplimiento.
- RR-09: atribución regulatoria incorrecta.
