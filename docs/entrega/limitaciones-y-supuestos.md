# Limitaciones y supuestos de la entrega

| Campo | Valor |
|---|---|
| Documento | Lo que la propuesta no resuelve, no demuestra o asume |
| Fecha | 2026-10-27 |
| Detalle | [02 — Supuestos](../02-supuestos.md), [10 — Decisiones diferidas](../10-decisiones-diferidas.md), [11 — Riesgos residuales](../11-riesgos-residuales.md), [15 — Preguntas abiertas](../15-preguntas-abiertas-fase-2.md), [implementación 05](../implementacion/05-limitaciones-y-pendientes.md) |

---

## 1. Bloqueos vigentes

| Bloqueo | Alcance | Condición para levantarlo |
|---|---|---|
| B-05 | Ejecución y recuperación de transferencias; alta de clientes en el Core | Enmienda de ADR-0021 (ARG03-C01, H01, H02); verificación humana independiente; evidencia de DD-23 |
| B-06 | Despachos financieros | DD-23 o modo de suspensión regional explícito |
| B-01 | Transferencias en producción | CF-01, CF-02, PT-01 a PT-06 y CP-01 a CP-03 |
| B-02 | Vinculación de clientes preexistentes | DD-21 |
| B-03 | Integración interbancaria | Q-01 |
| B-04 | Proveedor y región definitivos | ADR-0010 y DD-15 |

## 2. Limitaciones de la evidencia

- **No es apto para producción** ni certifica cumplimiento normativo.
- No hay despliegue en la nube: la HA y el DR están diseñados, **no probados**.
- RTO y RPO son **candidatos**; no hay mediciones de latencia ni estimaciones de costo.
- Componentes sin código: Gateway, BFF, IdP, servicio de consultas, caché, consola de operaciones y frontend.
- Proveedores simulados (KYC, push, SMS) y datos sintéticos.
- La sesión del solicitante de vinculación no está autenticada en la base local (PF4-01).
- Las revisiones ARG no son independientes de la autoría (ARG03-L03).
- El validador de contratos es parcial ([implementación 04](../implementacion/04-pruebas-de-contrato.md)).

## 3. Supuestos principales

| Supuesto | Efecto si es falso |
|---|---|
| El Core es autoritativo, heredado y de capacidad limitada (ADR-0009, S1–S5) | Se simplificaría la integración |
| La plataforma no accede a la base de datos principal (S12) | Cambiaría la ACL; no se recomienda |
| Existe un canal privado posible entre la nube y el Core (Q-20) | Sin él, la latencia y la seguridad obligan a replantear C6 |
| No hay obligación verificada de alojar solo en Ecuador (ADR-0010) | Cambiaría proveedor y región |
| El alcance no incluye datos de tarjetas | PCI DSS pasaría a aplicar a un segmento |

## 4. Preguntas institucionales sin responder

Q-01 (interbancario), Q-02 y Q-17 (capacidades del Core), Q-03 (IdP), Q-04 (alcance de «cada movimiento»), Q-05 (tipo de institución), Q-06 (biometría y terceros), Q-12 (antifraude), Q-13 (SIEM), Q-16 (capacidad del Core), Q-18 (equipo), Q-19 (retención), Q-20 y Q-21 (ubicación del Core y dominios de falla). **Ninguna se respondió por supuesto.**
