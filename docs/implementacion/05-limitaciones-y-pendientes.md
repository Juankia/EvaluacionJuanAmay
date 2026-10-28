# 05 — Limitaciones y pendientes de la base ejecutable

| Campo | Valor |
|---|---|
| Documento | Lo que la base técnica no resuelve, con su origen y su disparador |
| Fecha | 2026-10-27 |

---

## 1. Bloqueos que se mantienen

| Bloqueo | Efecto en el código | Condición para levantarlo |
|---|---|---|
| B-05 (ejecución y recuperación financiera) | No existe servicio de transferencias ejecutable; E3 (alta en el Core) con adaptador deshabilitado | Enmienda de ADR-0021 (ARG03-C01, H01, H02), verificación humana independiente y evidencia del producto del diario (DD-23) — ARG-03 §7 |
| E5 (activación del acceso digital) | Adaptador deshabilitado; ningún cliente obtiene acceso digital | Selección del IdP y de su operación de vinculación (PF4-04) y fase de seguridad |

## 2. Limitaciones por servicio

| Servicio | Limitación | Origen | Disparador o tratamiento |
|---|---|---|---|
| Vinculación | La sesión del solicitante **no está autenticada**: cualquiera que conozca el `requestId` (UUID aleatorio) puede consultar su estado de canal | PF4-01 | Fase de seguridad. No se expone fuera del entorno local |
| Vinculación | Una solicitud que queda en `IDENTITY_VERIFICATION_IN_PROGRESS` por la caída del proceso no se recupera sola; nuevas verificaciones responden `EQUIVALENT_OPERATION_IN_PROGRESS` | Vivacidad; sin barrido de antigüedad | Barrido por antigüedad del estado (métrica ya prevista en [24](../24-arquitectura-de-componentes.md) §4.3) al implementar la revisión manual |
| Vinculación | La clave HMAC viene de una variable de entorno | PF4-05 | Custodia en HSM o KMS |
| Vinculación | El KYC es simulado; no hay SDK ni contrato del proveedor | PF4-07; DD-14 | Selección del proveedor |
| Vinculación | Etapas E2, E4 y E6 y la revisión manual no implementadas | Alcance de la base | Siguiente incremento autorizado |
| Notificaciones | Proveedores falsos y destinatarios sintéticos; no hay lectura de contactos desde consultas (A4) | PF4-03 | Implementación del servicio de consultas |
| Notificaciones | Los eventos de movimientos (NOT-1: nunca descartados, entregados en orden con `ESPERA_SECUENCIA`) no los produce ningún servicio implementado | Depende del servicio de consultas y de transferencias | Implementación de esos servicios |
| Notificaciones | Valores de reintento provisionales | PF4-02 | Definición de `MAX_INTENTOS_CONSUMO` y relacionados |
| Auditoría | La cadena de hash por fuente **detecta** modificaciones ingenuas; no es almacenamiento WORM ni cumple la retención regulatoria | ADR-0020 | Almacenamiento inmutable y políticas de retención |
| Auditoría | El rol `auditor` lo emitiría el IdP corporativo, que no existe localmente; las pruebas usan una clave pública de prueba | ADR-0013 | Selección del IdP |
| Auditoría | Granularidad de la cadena provisional (por fuente) | PF4-06 | Decisión de partición |
| Mensajería | Entrega al menos una vez: un fallo entre la publicación y la marca del outbox puede repetir un evento; los consumidores deduplican por `eventId` | ADR-0015 | Aceptado por diseño |
| Contratos | El validador de contratos es parcial | [04](04-pruebas-de-contrato.md) §3 | Uso de composición de esquemas |

## 3. Lo que esta base no demuestra

- Preparación para producción, rendimiento o capacidad frente al Core.
- Seguridad de la red, identidad de servicio a servicio ni gestión de secretos.
- Ninguna garantía de los protocolos financieros (CF-01, CF-02, ADR-0021).
