# Resumen ejecutivo — Plataforma de Banca Digital

| Campo | Valor |
|---|---|
| Entrega | Reto técnico de Arquitecto de Soluciones/Aplicaciones — Plataforma de Banca Digital |
| Autor | Juan Carlos Amay |
| Fecha | 2026-10-27 |
| Estado | **Propuesta arquitectónica completa, con limitaciones declaradas.** No apta para producción |

---

## 1. Qué se propone

Una plataforma de canales digitales (web y móvil) que **protege al Core Bancario** y lo trata como sistema autoritativo, externo y de capacidad limitada. La plataforma:

- **Integra** el Core mediante **capas anticorrupción por dominio**, con un presupuesto global de capacidad (ADR-0001, ADR-0011).
- **Expone** un API Gateway de borde y un **BFF compartido** sin lógica financiera (ADR-0002, ADR-0014).
- **Divide** el negocio en servicios por dominio: consultas, vinculación, transferencias, notificaciones y auditoría, cada uno con su base de datos (ADR-0012, ADR-0016).
- **Garantiza la integridad transaccional** con una máquina de estados durable, idempotencia y conciliación: **un timeout no es una falla** (ADR-0005, ADR-0006).
- **Comunica** los servicios por eventos con **outbox transaccional** y un broker gestionado (ADR-0015).
- **Autentica** con un **IdP OIDC** interno, Authorization Code + PKCE y sesiones del servidor para la web (ADR-0013, ADR-0019, ADR-0023).
- **Se despliega** sobre **AWS como referencia**: multizona, con DR en espera templada y conmutación con cuarentena financiera (ADR-0022).

## 2. Diagramas clave

| Vista | Archivo |
|---|---|
| Contexto (C4 Nivel 1) | [ContextoPrincipal](../../diagrams/c4/structurizr-ContextoPrincipal.svg) |
| Contenedores (C4 Nivel 2, 21 contenedores) | [Contenedores](../../diagrams/c4/structurizr-Contenedores.svg) |
| Componentes (C4 Nivel 3, 52 componentes en 5 contenedores) | [BFF](../../diagrams/c4/structurizr-Componentes-BFF.svg), [Consultas](../../diagrams/c4/structurizr-Componentes-Consultas.svg), [Vinculación](../../diagrams/c4/structurizr-Componentes-Vinculacion.svg), [Notificaciones](../../diagrams/c4/structurizr-Componentes-Notificaciones.svg), [Auditoría](../../diagrams/c4/structurizr-Componentes-Auditoria.svg) |
| Infraestructura (ejecutiva) | [Infraestructura de referencia](../../diagrams/infraestructura/infraestructura-referencia-resumen.svg) |
| Infraestructura (detallada, desde el DSL) | [Infraestructura-Referencia](../../diagrams/c4/structurizr-Infraestructura-Referencia.svg) |
| Seguridad e identidad | [Arquitectura de seguridad](../../diagrams/seguridad/arquitectura-de-seguridad.svg) |

## 3. Evidencia ejecutable

Base técnica en Java 21 y Quarkus, autorizada por ARG-03 solo para el alcance **no financiero**:

| Módulo | Estado |
|---|---|
| Auditoría, notificaciones, vinculación (subconjunto seguro), simulador del Core y bibliotecas transversales | Construidos y probados localmente |
| Contratos OpenAPI y sobre de eventos | Publicados y verificados por pruebas |

## 4. Gobierno de riesgos

- La arquitectura pasó por **tres revisiones adversariales** (ARG-01, ARG-02 y ARG-03), cuyos hallazgos se remediaron y quedaron registrados.
- La **ejecución de transferencias y el alta de clientes en el Core están bloqueadas** (B-05) hasta enmendar ADR-0021 y obtener una verificación humana independiente. Es una decisión deliberada: **no se construye lo que no se puede demostrar seguro**.

## 5. Cobertura

Los 12 requisitos del reto (R1–R6, C1–C6) tienen decisión arquitectónica documentada; C3 y C6 son parciales porque dependen de datos que la institución debe aportar. Detalle en la [matriz de cumplimiento](matriz-cumplimiento.md).

## 6. Qué necesita la institución para avanzar

1. **Respuestas sobre el Core:** ubicación, latencia, capacidades de consulta por referencia y capacidad (Q-02, Q-16, Q-17, Q-20).
2. **Validación legal:** residencia de datos y tratamiento de biometría (ADR-0010, Q-06).
3. **Clasificación:** tipo de institución y organismo de control (Q-05).
4. **Gobierno:** revisión humana independiente de CF-01, CF-02 y ADR-0021.

Limitaciones completas: [limitaciones y supuestos](limitaciones-y-supuestos.md).
