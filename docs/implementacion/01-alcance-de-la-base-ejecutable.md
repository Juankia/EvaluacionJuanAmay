# 01 — Alcance de la base ejecutable no financiera

| Campo | Valor |
|---|---|
| Documento | Alcance autorizado y estado de la base técnica ejecutable |
| Autorización | [ARG-03](../reviews/ARG-03-revision-componentes.md) §7: CONDITIONAL GO para la base técnica no financiera |
| Stack | Java 21 y Quarkus 3.40.1.1 ([ADR-0017](../adr/ADR-0017-stack-de-backend.md), provisional) |
| Fecha | 2026-10-27 |
| Estado | Base técnica local. **No apta para producción. Sin datos reales. Sin conexión con el Core ni con la red de la institución** |

---

## 1. Qué se construye y qué no

| Alcance (ARG-03 §7) | Estado en el repositorio |
|---|---|
| Paquetes transversales: `shared-observability`, `integration-platform-sdk`, `contract-testing` | Implementados y probados |
| Contratos OpenAPI y esquema del sobre de eventos (`packages/api-contracts`) | Publicados; tipos TypeScript sincronizados por prueba |
| Simulador del Core (`test-fixtures/core-simulator`) | Fixture de pruebas con datos sintéticos; solo lectura |
| Servicio de auditoría (`services/audit`) | Implementado: consumo de todos los eventos, cadena de hash por fuente, API de consulta con registro A6 |
| Servicio de notificaciones (`services/notifications`) | Implementado con proveedores **falsos** (push y SMS) |
| Servicio de vinculación digital (`services/digital-linking`) | Implementado el **subconjunto seguro**: inicio, verificación de identidad (E1) con un KYC **simulado** y consulta de estado |
| BFF y servicio de consultas | Solo contratos (`bff-v1.yaml`, `inquiries-v1.yaml`). Implementación pendiente; ARG03-M01 se resuelve al implementarlos |
| Servicio de transferencias ejecutable | **No autorizado (B-05).** Solo el contrato de frontera `transfers-boundary-v1.yaml`, marcado `x-status: BLOCKED` |
| Alta de clientes en el Core (E3) y activación del acceso digital (E5) | **No autorizados.** Puertos con adaptadores deshabilitados (ARG03-M02) |

## 2. Condición ARG03-M02 en la vinculación

La revisión exige que el puerto de alta exista con un **adaptador deshabilitado** explícito y que las pruebas demuestren que E3 y E5 son inalcanzables. La implementación lo cumple así:

| Control | Evidencia |
|---|---|
| Los puertos `CustomerCreationPort` y `DigitalAccessActivationPort` solo tienen adaptadores que fallan cerrado y emiten una alerta | `adapters/out/blocked/*`; `BlockedOperationsTest.disabledAdaptersFailClosed` |
| Ninguna otra clase compilada referencia esos puertos | `BlockedOperationsTest` inspecciona el *bytecode* de `target/classes` |
| La máquina de estados no tiene transiciones después de `IDENTITY_VERIFIED` ni de `IDENTITY_REJECTED` | `OnboardingStateTest.noStateLeadsBeyondIdentityVerification` |
| La API no expone rutas de alta ni de activación | `DigitalLinkingServiceTest.unknownRequestsAndBlockedStagesAreNotFound` |

Con respecto al diseño de componentes ([24](../24-arquitectura-de-componentes.md) §4.3), esto **invierte** la dependencia observada en ARG03-M02: el servicio de aplicación ya no depende del puerto bloqueado.

## 3. Documentos de implementación

| Documento | Contenido |
|---|---|
| [02 — Entorno local y construcción](02-entorno-local-y-construccion.md) | Requisitos, comandos de construcción y pruebas, entorno local |
| [03 — Servicios implementados](03-servicios-implementados.md) | Puertos, API, eventos y configuración de cada servicio |
| [04 — Pruebas de contrato](04-pruebas-de-contrato.md) | Cómo se verifican los contratos OpenAPI y sus límites |
| [05 — Limitaciones y pendientes](05-limitaciones-y-pendientes.md) | Lo que esta base no resuelve y su disparador |
