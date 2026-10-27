# ADR-0018 — Stack de frontend: React + TypeScript para la web y React Native + TypeScript para el móvil

| Campo | Valor |
|---|---|
| Estado | **Aceptada con condiciones** (recomendación para R6; pendiente de las capacidades del equipo, Q-18, y del soporte del SDK del proveedor KYC, DD-14). Resuelve DD-13 |
| Fecha | 2026-10-27 |
| Requisitos | R6, R3 |
| Drivers y supuestos | S16; AQ-01, AQ-08, AQ-09, AQ-10 |
| Relacionados | [21 — Alternativas tecnológicas, sección 7](../21-alternativas-tecnologicas.md#7-frontend-y-móvil-dos-alternativas-r6), [ADR-0014](ADR-0014-api-gateway-y-bff-de-canales.md) |

## Contexto

R6 exige una SPA web y una aplicación móvil en un framework multiplataforma, y pide proponer **dos opciones** de stack y justificar la selección. La comparación detallada está en [21, sección 7](../21-alternativas-tecnologicas.md#7-frontend-y-móvil-dos-alternativas-r6).

## Problema

¿Qué stack usan la SPA y la app móvil?

## Decisión

- **Opción B (recomendada):** **React + TypeScript** para la SPA y **React Native + TypeScript** para Android e iOS.
- **Opción A (alternativa válida):** **React + TypeScript** para la SPA y **Flutter** para Android e iOS.

Razones de la recomendación:

1. **Un solo lenguaje** (TypeScript) y un solo paradigma de interfaz en ambos canales: se comparten tipos de los contratos de API, validaciones de formulario y lógica de presentación no visual.
2. **Concentración de capacidades** en un equipo de frontend, lo que reduce el costo total y el riesgo de dependencia de personas.
3. **Integración nativa:** biometría local, almacenamiento seguro y SDK de proveedores KYC disponibles mediante módulos nativos. El soporte del SDK del proveedor elegido debe confirmarse (DD-14).

**Se elegiría Flutter si:** el equipo ya domina Dart, se prioriza una interfaz muy personalizada y de rendimiento gráfico uniforme, o el proveedor KYC seleccionado ofrece un SDK oficial solo para Flutter.

**Advertencia:** la **biometría local** del dispositivo (huella, rostro del sistema operativo) solo desbloquea credenciales almacenadas en el dispositivo. **No equivale** a la verificación de identidad ni a la prueba de vida del proveedor KYC (ADR-0009, E1).

## Alternativas consideradas

| Alternativa | Resultado | Razón |
|---|---|---|
| **B. React + TS / React Native + TS** | **Recomendada** | Lenguaje único y código compartido |
| A. React + TS / Flutter | Alternativa válida | Mejor consistencia visual y rendimiento gráfico; Dart separado de la web |
| Aplicaciones nativas separadas (Swift y Kotlin) | Descartada | R6 exige un framework multiplataforma |

## Trade-offs

| Se gana | Se paga |
|---|---|
| Un lenguaje y tipos compartidos | Dependencia del puente con los módulos nativos de React Native para las funciones críticas de seguridad |
| Menor costo de equipo | Esfuerzo adicional para lograr la uniformidad visual entre plataformas frente a Flutter |

## Consecuencias

- Requisitos de seguridad móvil (almacenamiento seguro, detección de dispositivos manipulados, protección en tiempo de ejecución) en la fase de seguridad.
- El canal implementa la generación y persistencia de la clave de idempotencia por intención (ADR-0006 IC-02). En la SPA la persistencia es más débil (R2-N11).

## Riesgos

- Soporte del SDK KYC para React Native (DD-14).
