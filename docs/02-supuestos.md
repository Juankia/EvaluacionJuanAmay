# 02 — Supuestos arquitectónicos

| Campo | Valor |
|---|---|
| Fase | 0 — Drivers, supuestos y escenarios de calidad |
| Estado | Vigente (S13 revisado y S16–S17 agregados en la Fase 2). Cada supuesto está pendiente de validación con la institución |
| Documentos relacionados | [01 — Drivers](01-drivers-arquitectonicos.md), [11 — Riesgos residuales](11-riesgos-residuales.md) |

## 1. Propósito

Un **supuesto arquitectónico** es una condición que el equipo de arquitectura da por cierta para poder avanzar, aunque el reto no la establezca. Los supuestos **no son requisitos ni hechos confirmados**. Si un supuesto resulta falso, las decisiones que dependen de él deben revisarse. Por eso cada supuesto indica las decisiones que afecta y cómo se valida.

## 2. Registro de supuestos

### S1 — El Core Bancario es un sistema existente y externo a la plataforma

- **Enunciado:** el Core Bancario es un sistema heredado o existente. Desde la perspectiva de la Plataforma de Banca Digital es un **sistema externo**: la plataforma no lo diseña, no lo modifica y no controla su ciclo de liberaciones.
- **Base:** el reto describe el Core como una fuente de información existente y no pide rediseñarlo.
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md).
- **Validación:** confirmar con la institución el producto y la versión del Core, su propietario y su proceso de cambio.

### S2 — El Core es la fuente autoritativa del dato transaccional

- **Enunciado:** el Core es la **fuente autoritativa** (*source of truth*) de los datos transaccionales de clientes, cuentas, saldos y movimientos. Cualquier copia en la plataforma digital (caché o proyección) es derivada y prevalece siempre el Core.
- **Base:** el reto indica que el Core contiene los «datos básicos, productos, cuentas, movimientos».
- **Decisiones afectadas:** [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0007](adr/ADR-0007-cqrs-diferido.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md).
- **Validación:** confirmar que no existen otros sistemas autoritativos para alguno de esos datos (por ejemplo, un sistema separado de tarjetas o de créditos).

### S3 — Interfaces heredadas del Core y ausencia de idempotencia nativa

- **Enunciado:** el Core **podría** exponer interfaces heredadas SOAP o REST (u otras: mensajería, archivos, conectores propietarios). También **podría** no ofrecer idempotencia nativa ni búsqueda de operaciones por clave de idempotencia (*idempotency-key lookup*).
- **Base:** supuesto conservador típico de los Core Bancarios heredados. **El reto no lo menciona.**
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md), [ADR-0006](adr/ADR-0006-idempotencia-y-conciliacion.md).
- **Validación:** revisar el catálogo de servicios del Core y confirmar:
  1. el protocolo de cada operación;
  2. si acepta una referencia externa única por transacción;
  3. si permite consultar el estado de una operación por esa referencia;
  4. si rechaza duplicados.
- **Si el supuesto es falso:** si el Core ofrece idempotencia nativa y consulta por referencia, la conciliación por correlación de negocio pasa de mecanismo principal a respaldo, y el riesgo RR-04 disminuye de forma significativa.

### S4 — El Core tiene limitaciones de latencia y de disponibilidad

- **Enunciado:** el Core **podría** tener latencia variable, ventanas de mantenimiento, procesos de cierre diario (*batch*) y una capacidad limitada de conexiones concurrentes. Debe protegerse del tráfico no controlado de los canales digitales.
- **Base:** supuesto conservador. El reto no lo indica.
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md), [ADR-0004](adr/ADR-0004-cache-aside.md), [ADR-0007](adr/ADR-0007-cqrs-diferido.md).
- **Validación:** obtener el SLA del Core, las ventanas de mantenimiento, los límites de concurrencia y la latencia observada por operación.

### S5 — El sistema complementario es una dependencia externa

- **Enunciado:** el sistema complementario de datos extendidos del cliente es una **dependencia externa**, con su propio propietario y ciclo de vida.
- **Base:** el reto lo describe como una de las dos fuentes de información del cliente.
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md).
- **Validación:** confirmar sus interfaces, su disponibilidad y qué datos son autoritativos en él y no en el Core.

### S6 — El modelo interno del Core no se expone a los canales

- **Enunciado:** la plataforma **no debe** exponer a los canales digitales el modelo de datos interno del Core: códigos de producto, estructuras de registros heredadas, códigos de error propietarios. Los canales consumen el modelo de dominio de la banca digital.
- **Base:** principio arquitectónico de aislamiento. Es un supuesto de diseño, no un requisito del reto.
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md).

### S7 — Posible incorporación de otro Core u otra implementación

- **Enunciado:** la institución **podría** necesitar en el futuro otro Core o una implementación alternativa de la misma capacidad de negocio. Por ejemplo: una migración de Core, un Core para una línea de producto o un servicio externo especializado.
- **Base:** supuesto de evolución. El reto pide que la arquitectura permita «agregar nuevos componentes sin impactar los existentes» (C5).
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md). Riesgo RR-07.
- **Validación:** consultar la hoja de ruta tecnológica de la institución.

### S8 — La residencia de datos depende de validación legal

- **Enunciado:** las decisiones sobre residencia de datos y transferencia internacional de datos personales dependen de:
  - la legislación ecuatoriana, en particular la Ley Orgánica de Protección de Datos Personales;
  - los requisitos del organismo de control que corresponda;
  - las políticas de cumplimiento de la institución;
  - la validación del área legal.
- **Aclaración:** los mecanismos de otras jurisdicciones, como las cláusulas contractuales tipo (*Standard Contractual Clauses*, SCC) del Reglamento General de Protección de Datos de la Unión Europea, **no son por sí mismos un requisito legal ecuatoriano**. Pueden mencionarse solo como referencia comparativa.
- **Decisiones afectadas:** [ADR-0010](adr/ADR-0010-residencia-de-datos.md).
- **Validación:** opinión del área legal y de cumplimiento de la institución.

### S9 — No existe un proveedor de identidad moderno para clientes

- **Enunciado:** la institución **no cuenta** actualmente con un proveedor de identidad (*Identity Provider*, IdP) moderno orientado a clientes que soporte OAuth 2.0 y OpenID Connect. La selección del IdP, del flujo OAuth y de su implementación se evalúa en la fase de seguridad.
- **Base:** supuesto. El reto pide recomendar «el flujo más adecuado», lo que sugiere que no hay una solución establecida.
- **Decisiones afectadas:** decisión diferida DD-03.
- **Validación:** inventario de la infraestructura de identidad actual.

### S10 — Las cifras de capacidad y continuidad no están especificadas

- **Enunciado:** el reto no indica TPS, número de clientes, perfil de tráfico (picos por quincena, fin de mes o días feriados), objetivos de latencia, RTO (*Recovery Time Objective*, tiempo objetivo de recuperación) ni RPO (*Recovery Point Objective*, punto objetivo de recuperación). Todos estos valores se tratan como **supuestos u objetivos** hasta validarlos.
- **Decisiones afectadas:** [04 — Escenarios de calidad](04-escenarios-de-calidad.md), [ADR-0007](adr/ADR-0007-cqrs-diferido.md), decisión diferida DD-09.
- **Validación:** métricas históricas de los canales actuales, proyecciones de negocio y análisis de impacto al negocio (*Business Impact Analysis*, BIA).

### S11 — Madurez de la organización de ingeniería

- **Enunciado:** la opción arquitectónica seleccionada (ACL por dominio) supone una organización de ingeniería con madurez suficiente. Esa organización tiene equipos o responsables por dominio, y la evolución independiente de cada dominio aporta valor.
- **Contrapunto explícito:** si el equipo tuviera solo 1–2 ingenieros y la complejidad de integración fuera baja, un servicio de integración centralizado podría ser **económicamente preferible**.
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0003](adr/ADR-0003-integration-platform-sdk.md).
- **Validación:** conocer la estructura y el tamaño del equipo que construirá y operará la plataforma.

### S12 — La plataforma no accede directamente a la base de datos principal

- **Enunciado:** el reto indica que el Core y el sistema complementario «están conectados a una base de datos principal». Se asume que la plataforma digital **no accede directamente** a esa base de datos y que se integra solo mediante las interfaces de los sistemas.
- **Base:** supuesto de diseño. El acceso directo a la base de datos de un sistema autoritativo salta sus reglas de negocio y acopla la plataforma a su esquema interno (ver alternativa A en [05](05-frontera-de-integracion-core.md)).
- **Decisiones afectadas:** [ADR-0001](adr/ADR-0001-acl-por-dominio.md), [ADR-0009](adr/ADR-0009-core-autoritativo.md).
- **Validación:** confirmar con la institución que existen interfaces de servicio suficientes para todas las consultas requeridas.

### S13 — Las transferencias interbancarias dependen de una capacidad externa de procesamiento

- **Enunciado:** las transferencias interbancarias se procesan mediante una capacidad externa a la plataforma, el **Sistema Externo de Transferencias** de la vista de contexto. Esa capacidad está vinculada al sistema de pagos interbancarios administrado por el Banco Central del Ecuador, al que este repositorio llama de forma genérica **SPI**. **No se asume una integración directa de la plataforma con el BCE ni con el SPI.**
- **Modelos posibles** (revisión de la Fase 2; ver [12, sección 4.1](12-contexto-del-sistema.md#41-sistema-externo-de-transferencias-modelos-de-integración-posibles)):
  - **A.** integración directa, si es legal y técnicamente admisible;
  - **B.** mediante la infraestructura de pagos existente de la institución (Core o componente de pagos);
  - **C.** mediante un intermediario autorizado o un servicio bancario.
- **Base:** el reto indica que las transferencias invocan «servicios externos según el tipo». El mecanismo exacto no se especifica.
- **Decisiones afectadas:** [ADR-0005](adr/ADR-0005-maquina-de-estados-transaccional.md) (el nombre del estado conceptual `SPI_PROCESSING` presupone el SPI y se revisará cuando se resuelva Q-01) y DD-16.
- **Validación:** confirmar con la institución el modelo de integración, el mecanismo de participación en el sistema de pagos, los horarios y ciclos de compensación, y quién origina el mensaje de pago (Q-01).

### S14 — El fundamento normativo de la notificación no está verificado

- **Enunciado:** el reto afirma que «por normativa, los usuarios deben ser notificados de cada movimiento». Se acepta como **requisito del reto**. La norma específica, su alcance y los canales admitidos **no se han verificado** y se registran como consideración regulatoria por validar.
- **Decisiones afectadas:** fase de notificaciones (posterior).
- **Validación:** el área de cumplimiento identifica la norma aplicable al tipo de institución.

### S15 — El tipo de institución define el marco regulatorio

- **Enunciado:** el reto nombra a la institución «Banco Ecuador». La propuesta podría evaluarla una cooperativa de ahorro y crédito. El organismo de control depende del tipo de entidad:
  - para los bancos, la Superintendencia de Bancos;
  - para las cooperativas de ahorro y crédito, la Superintendencia de Economía Popular y Solidaria (SEPS).

  Por eso la arquitectura **no atribuye** normas de un sector al otro.
- **Terminología:** se usa «cliente», el término del reto. En el contexto cooperativo debe leerse como «socio».
- **Validación:** confirmar el tipo de institución destinataria.

### S16 — KYC y biometría provistos por un tercero especializado

- **Enunciado:** las capacidades de reconocimiento facial, prueba de vida, validación de documentos de identidad y verificación de identidad las provee un **tercero especializado**, no la institución.
- **Base:** el reto pide incluir «herramientas de KYC y biometría» y recomendar «herramientas de industria» (R2, R3). Esto sugiere soluciones de mercado, pero no excluye que la institución ya cuente con alguna.
- **Decisiones afectadas:** contexto del sistema (Proveedor KYC/Biométrico), DD-14, [ADR-0010](adr/ADR-0010-residencia-de-datos.md).
- **Validación:** inventario de las herramientas de identidad actuales de la institución (Q-06).

### S17 — Entrega de notificaciones mediante proveedores externos

- **Enunciado:** la entrega de notificaciones push, SMS y correo electrónico usa **proveedores externos de entrega**. La decisión de qué notificar y cuándo permanece en la plataforma.
- **Base:** el reto admite canales «propios o externos» (R4). La entrega push depende en la práctica de los servicios de los fabricantes de los sistemas operativos móviles. Para SMS y correo, el supuesto es que la institución no opera infraestructura propia.
- **Decisiones afectadas:** contexto del sistema (Proveedores de Notificaciones), DD-12.
- **Validación:** confirmar los canales y los proveedores actuales de la institución (Q-10).

## 3. Resumen

| ID | Supuesto | Criticidad si es falso | Decisiones afectadas |
|---|---|---|---|
| S1 | Core externo y existente | Alta | ADR-0001, ADR-0009 |
| S2 | Core autoritativo | Alta | ADR-0004, ADR-0007, ADR-0009 |
| S3 | Interfaces heredadas y sin idempotencia nativa | Media (si es falso, la arquitectura se simplifica) | ADR-0001, ADR-0005, ADR-0006 |
| S4 | Latencia y disponibilidad limitadas del Core | Media | ADR-0001, ADR-0003, ADR-0004, ADR-0007 |
| S5 | Sistema complementario externo | Media | ADR-0001, ADR-0009 |
| S6 | Modelo del Core no expuesto | Alta | ADR-0001 |
| S7 | Posible otro Core | Baja (afecta la justificación, no la viabilidad) | ADR-0001 |
| S8 | Residencia de datos sujeta a validación legal | Alta | ADR-0010 |
| S9 | Sin IdP moderno | Media | DD-03 |
| S10 | Cifras no especificadas | Alta | Escenarios de calidad, ADR-0007, DD-09 |
| S11 | Organización de ingeniería madura | Media | ADR-0001, ADR-0003 |
| S12 | Sin acceso directo a la base de datos principal | Alta | ADR-0001, ADR-0009 |
| S13 | Transferencias interbancarias mediante una capacidad externa; topología por validar | Alta | ADR-0005, DD-16 |
| S14 | Fundamento normativo de la notificación no verificado | Media | Fase de notificaciones |
| S15 | Tipo de institución por confirmar | Media | Todas las referencias regulatorias |
| S16 | KYC y biometría provistos por un tercero | Media | Contexto, DD-14, ADR-0010 |
| S17 | Notificaciones mediante proveedores externos | Baja | Contexto, DD-12 |
