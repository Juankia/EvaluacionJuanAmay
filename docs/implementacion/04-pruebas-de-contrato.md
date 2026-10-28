# 04 — Pruebas de contrato

| Campo | Valor |
|---|---|
| Documento | Verificación de las respuestas de los servicios contra los contratos OpenAPI |
| Fecha | 2026-10-27 |

---

## 1. Fuente de verdad

Los contratos de `packages/api-contracts/openapi/*.yaml` son la fuente de verdad de las API de canal y de operaciones. El esquema `schemas/event-envelope-v1.schema.json` define el sobre de eventos. El código se ajusta a los contratos; no se generan contratos desde el código.

## 2. Mecanismos

| Mecanismo | Qué verifica | Dónde |
|---|---|---|
| `OpenApiContract` (Java) | Que el cuerpo de una respuesta real cumple el esquema del contrato: propiedades requeridas, tipos, enumeraciones, patrones, arreglos y `$ref`, incluidas las referencias a `common-v1.yaml` | Pruebas `@QuarkusTest` de cada servicio |
| `declaresResponse` | Que el contrato declara el código de estado que el servicio devuelve | Mismas pruebas |
| `contract-sync.test.ts` | Que los tipos TypeScript coinciden con los esquemas (códigos de error, claves requeridas) y que el contrato de transferencias sigue marcado `BLOCKED` | `packages/api-contracts` |

## 3. Límites conocidos

`OpenApiContract` es deliberadamente pequeño y **no** es una implementación completa de JSON Schema:

- no valida `format` (`uuid`, `date-time`), `minLength`/`maxLength` ni `minimum`;
- no valida `oneOf`, `anyOf`, `allOf` ni `additionalProperties`;
- no valida peticiones, cabeceras ni parámetros; solo cuerpos de respuesta.

Por eso las restricciones de entrada se prueban por separado (por ejemplo, `Idempotency-Key` y los cuerpos inválidos en `DigitalLinkingServiceTest`). Si los contratos empiezan a usar composición de esquemas, se sustituye por un validador completo; ese es el disparador del cambio.
