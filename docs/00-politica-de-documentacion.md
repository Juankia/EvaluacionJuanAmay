# 00 — Política de documentación e idioma

| Campo | Valor |
|---|---|
| Documento | Política de documentación e idioma del repositorio |
| Alcance | Todos los entregables arquitectónicos, existentes y futuros, del repositorio |
| Idioma oficial | Español técnico profesional (es-EC) |
| Estado | Vigente |
| Fecha de emisión | 2026-10-27 |
| Responsable | Juan Carlos Amay |

---

## 1. Propósito

Este documento fija las convenciones de idioma, terminología y redacción de la propuesta de **Arquitectura de Banca Digital**. Una institución financiera ecuatoriana evaluará la propuesta; esa institución puede ser una cooperativa de ahorro y crédito.

Las convenciones buscan tres objetivos:

- **Precisión técnica**: que no se pierda significado al redactar en español.
- **Trazabilidad**: que cada decisión pueda rastrearse hasta un requisito, un supuesto o una obligación.
- **Consistencia**: que todos los documentos, diagramas y artefactos de código usen el mismo vocabulario.

## 2. Públicos destinatarios

La documentación debe poder ser leída por tres públicos:

| Público | Interés principal | Implicación para la redacción |
|---|---|---|
| Comité técnico de arquitectura | Solidez de las decisiones, alternativas evaluadas, atributos de calidad | Profundidad técnica completa; justificación explícita de cada decisión |
| Responsables de tecnología y seguridad de la información | Controles, riesgos, cumplimiento, operación | Riesgos y controles identificados y vinculados a componentes concretos |
| Gerencia de tecnología | Alcance, costos relativos, riesgos de negocio, hoja de ruta | Resúmenes ejecutivos al inicio de los documentos principales |

Redactar en español no justifica simplificar el contenido arquitectónico.

## 3. Reglas de idioma

### 3.1 Contenido redactado en español

Se redactan en español técnico profesional (es-EC):

- README y documentos de arquitectura.
- Registros de decisiones arquitectónicas (*Architecture Decision Records*, ADR).
- Matrices de trazabilidad.
- Registros de riesgos y supuestos.
- Escenarios de atributos de calidad.
- Justificaciones técnicas.
- Títulos, descripciones, relaciones y etiquetas de los diagramas C4.

### 3.2 Contenido que se mantiene en inglés

Se mantienen en inglés, según las convenciones de ingeniería:

| Elemento | Ejemplo |
|---|---|
| Clases, interfaces, métodos y variables | `TransferService`, `AccountRepository`, `executeTransfer()` |
| Endpoints y rutas de API | `POST /v1/transfers`, `GET /v1/accounts/{accountId}/balance` |
| Contratos técnicos (OpenAPI, AsyncAPI, esquemas) | `TransferRequest`, `idempotencyKey` |
| Eventos de software | `TransferInitiated`, `TransferCompleted`, `PaymentRejected` |
| Nombres técnicos de componentes desplegables | `digital-banking-bff`, `transfer-service` |
| Mensajes de commit | Ver sección 6 |

Las reglas detalladas de idioma y nomenclatura del código, el glosario de dominio (término de negocio → término en código) y los estándares de diseño están en [Estándares de ingeniería](00-estandares-de-ingenieria.md).

### 3.3 Términos técnicos internacionales

Los nombres de tecnologías, estándares y patrones se mantienen en su forma original. Se traducen solo cuando la traducción no pierde precisión.

**Regla de primera aparición:** la primera vez que un concepto importante aparece en un documento se explica en español y el término original va entre paréntesis. Después puede usarse solo el término original.

> Ejemplo: «Se incorpora una capa anticorrupción (*Anti-Corruption Layer*) que traduce el modelo del Core Bancario al modelo de dominio de la banca digital. En adelante, la Anti-Corruption Layer…»

### 3.4 Glosario de referencia de términos técnicos

| Término original | Explicación en español | Observación |
|---|---|---|
| Quarkus | Framework Java nativo para la nube y orientado a contenedores | Nombre propio; no se traduce |
| OAuth 2.0 | Marco de autorización delegada para el acceso a recursos protegidos | Estándar; no se traduce |
| OpenID Connect (OIDC) | Capa de identidad sobre OAuth 2.0 para autenticar usuarios | Estándar; no se traduce |
| FAPI (*Financial-grade API*) | Perfil de seguridad de grado financiero para APIs basadas en OAuth 2.0 y OIDC | Estándar de la OpenID Foundation |
| API Gateway | Puerta de enlace de APIs: punto único de entrada para políticas transversales | Se mantiene en inglés |
| BFF (*Backend for Frontend*) | Backend especializado por tipo de canal (web, móvil) | Se mantiene en inglés |
| Anti-Corruption Layer | Capa anticorrupción que aísla el dominio propio de modelos externos o heredados | Se mantiene en inglés |
| Cache-Aside | Patrón de caché bajo demanda: la aplicación consulta la caché y, si no encuentra el dato, lo obtiene de la fuente | Se mantiene en inglés |
| Circuit Breaker | Disyuntor: corta temporalmente las llamadas a una dependencia que está fallando | Se mantiene en inglés |
| Bulkhead | Mamparo: aísla recursos para que una falla no se propague | Se mantiene en inglés |
| CQRS (*Command Query Responsibility Segregation*) | Separación de responsabilidades entre comandos (escritura) y consultas (lectura) | Se mantiene en inglés |
| Idempotency Key | Clave de idempotencia: garantiza que una operación repetida produzca un único efecto | Se mantiene en inglés |
| Outbox (*Transactional Outbox*) | Bandeja de salida transaccional: publica eventos de forma consistente con la transacción de negocio | Se mantiene en inglés |
| OpenTelemetry | Estándar abierto de observabilidad para trazas, métricas y registros | Nombre propio; no se traduce |

Este glosario se ampliará a medida que se incorporen nuevos conceptos.

## 4. Terminología financiera ecuatoriana

Se usa la terminología del sistema financiero ecuatoriano:

| Término preferido | Uso |
|---|---|
| Core Bancario | Sistema transaccional central de la institución |
| Cooperativa de ahorro y crédito | Entidad del sector financiero popular y solidario |
| Socio | Persona vinculada a una cooperativa de ahorro y crédito |
| Cliente | Persona vinculada a un banco o, en términos genéricos, a la institución |
| Captaciones | Recursos recibidos del público (depósitos de ahorro, a plazo, etc.) |
| Colocaciones | Cartera de créditos otorgados |
| Cuentas de ahorro | Producto de captación a la vista |
| Transferencias interbancarias | Transferencias entre instituciones del sistema financiero nacional |
| Auditoría | Registro y revisión de las operaciones para control interno y externo |
| Conciliación | Cuadre de saldos y movimientos entre sistemas o instituciones |

**Socio y cliente:** el enunciado del reto se refiere a la institución como «Banco Ecuador» y usa el término «cliente», pero la evaluadora puede ser una cooperativa de ahorro y crédito. Por eso los documentos usan «cliente», el término del reto, y lo entienden como «socio» en el contexto cooperativo (supuesto S15 en [02 — Supuestos](02-supuestos.md)).

## 5. Diferenciación entre requisitos, supuestos y obligaciones regulatorias

Toda afirmación relevante de la arquitectura se clasifica en una de estas categorías:

| Categoría | Identificador sugerido | Definición |
|---|---|---|
| Requisito del ejercicio | `R1`–`R6`, `C1`–`C6`, `E1`–`E4` | Lo que el enunciado del ejercicio pide de forma explícita: requerimientos (R), consideraciones técnicas (C) y entregables (E), con la numeración del reto |
| Supuesto arquitectónico | `S1`, `S2`, … | Condición que el arquitecto asume para avanzar; debe validarse con la institución |
| Obligación regulatoria verificada | `REG-xx` | Norma vigente, citada con su fuente y confirmada como aplicable al tipo de institución |
| Consideración regulatoria por validar | `REG-PV-xx` | Posible obligación cuya aplicabilidad o vigencia no se ha confirmado |

### 5.1 Contexto regulatorio de referencia

Según corresponda, se consideran estas entidades y marcos:

- **Superintendencia de Economía Popular y Solidaria (SEPS):** supervisa a las cooperativas de ahorro y crédito.
- **Superintendencia de Bancos:** supervisa a las entidades del sector financiero privado y público.
- **Junta de Política y Regulación Financiera:** emite regulación para el sistema financiero.
- **Banco Central del Ecuador (BCE):** administra el sistema nacional de pagos, incluido el procesamiento de transferencias interbancarias.
- **Autoridad de protección de datos personales:** es la autoridad de control creada por la Ley Orgánica de Protección de Datos Personales.

### 5.2 Reglas de redacción regulatoria

1. **No afirmar cumplimiento normativo sin validación.** Se escribe «la arquitectura facilita el cumplimiento de…» o «se considera el requisito de…, sujeto a validación por el área de cumplimiento». No se escribe «la arquitectura cumple con…».
2. **No atribuir al sector cooperativo normas exclusivas de los bancos**, ni al revés. Toda norma citada indica a qué tipo de entidad aplica.
3. **Citar la fuente** (cuerpo normativo, número de resolución o artículo) de toda obligación clasificada como `REG-xx`.
4. **Cuando no se ha verificado una norma**, se clasifica como `REG-PV-xx` y se registra como supuesto o como riesgo.

## 6. Convenciones de control de versiones

Los mensajes de commit se escriben en inglés y siguen [Conventional Commits](https://www.conventionalcommits.org/):

```text
<type>(<scope>): <description>
```

Tipos habituales: `docs`, `feat`, `fix`, `refactor`, `chore`, `test`, `ci`.

Ejemplos:

```text
docs(adr): add ADR-001 for API Gateway selection
docs(c4): add system context diagram
docs: add documentation and language policy
```

## 7. Convenciones para diagramas C4

- Los títulos, descripciones, relaciones y etiquetas se escriben en español.
- Los nombres técnicos de los componentes desplegables pueden ir en inglés si coinciden con el código, acompañados de una descripción en español.
- Las tecnologías se indican con su nombre original (por ejemplo, `[Quarkus]`, `[PostgreSQL]`, `[Kafka]`).
- Las relaciones se redactan como verbos en español: «Consulta saldos en», «Publica eventos en», «Autentica mediante».

## 8. Calidad de redacción

- Lenguaje formal e impersonal: «se propone», «se adopta», «se descarta».
- Cada decisión indica el contexto, las alternativas evaluadas, la decisión tomada y sus consecuencias.
- Sin anglicismos innecesarios cuando existe un término español preciso (por ejemplo, «desplegar» en lugar de «deployar»).
- No se traduce un término técnico de forma literal si la traducción pierde precisión.
- Las fechas siguen el formato ISO 8601 (`AAAA-MM-DD`).

## 9. Convención de nomenclatura de archivos y directorios

### 9.1 Reglas

| Elemento | Convención | Ejemplos |
|---|---|---|
| Directorios técnicos | En inglés, en minúsculas | `docs/`, `docs/adr/`, `c4/`, `diagrams/` |
| Archivos estándar de herramientas o de la industria | Nombre convencional en inglés | `README.md`, `workspace.dsl`, `.gitignore`, `.markdownlint.json` |
| Documentos arquitectónicos | Prefijo numérico de dos dígitos + nombre en español | `05-frontera-de-integracion-core.md` |
| ADR | `ADR-` + número de cuatro dígitos + nombre en español | `ADR-0009-core-autoritativo.md` |

Además, todos los nombres de archivo de documentos y ADR:

- usan **kebab-case**, en minúsculas y con palabras separadas por guiones (excepto el prefijo `ADR-`);
- usan solo **caracteres ASCII**: sin tildes, sin `ñ` (se escribe `n`) y sin espacios;
- **conservan los términos técnicos internacionales** cuando son el nombre del concepto: `ADR-0003-integration-platform-sdk.md`, `ADR-0004-cache-aside.md`;
- son **estables**: un renombramiento obliga a actualizar todos los enlaces internos y debe hacerse con `git mv`, para preservar el historial.

### 9.2 Numeración

- **Prefijo de los documentos:** indica el orden de lectura. Un número no se reutiliza.
- **Prefijo `00-`:** reservado para los **documentos normativos** del repositorio (políticas y estándares), que pueden compartirlo. Ejemplos: `00-politica-de-documentacion.md`, `00-estandares-de-ingenieria.md`. Los documentos arquitectónicos usan números únicos a partir de `01-`.
- **Número de ADR:** es un **identificador permanente**. No se renumera ni se reutiliza, aunque el ADR se reemplace o se deje sin efecto. Un ADR nuevo toma el siguiente número disponible.
- **Títulos:** los títulos de los documentos (encabezado `#`) se escriben en español con tildes. La restricción ASCII aplica solo a los nombres de archivo.

## 10. Aplicación

Esta política aplica a todos los archivos existentes y futuros del repositorio. Cualquier excepción se documenta y se justifica en el propio artefacto.
