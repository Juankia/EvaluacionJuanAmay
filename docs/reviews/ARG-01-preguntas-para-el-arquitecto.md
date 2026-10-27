# ARG-01 — Preguntas para el arquitecto

| Campo | Valor |
|---|---|
| Revisión de origen | [ARG-01 — Revisión arquitectónica](ARG-01-revision-arquitectonica.md) |
| Propósito | Las 10 preguntas técnicas más difíciles que un panel de arquitectos podría formular sobre el diseño de las Fases 0–2 |
| Nota | No se incluyen respuestas. Para cada pregunta se indica qué evidencia debería presentar el arquitecto. Si esa evidencia no existe hoy, se señala |

---

## P1. «Un cliente envía una transferencia desde la app, pierde la señal y vuelve a pulsar "Enviar". Demuéstreme que no habrá dos débitos.»

- **Qué evalúa:** si la idempotencia protege el modo de falla real del canal, y no solo los reintentos internos.
- **Por qué es difícil:** la clave se genera en el dominio «al aceptar la operación», así que cada reenvío del cliente puede ser una intención nueva. Exige distinguir deduplicación interna, identidad lógica de la intención y ejecución en el Core.
- **Evidencia esperada:** quién genera la clave y cuándo (por intención, no por intento HTTP); dónde se persiste en el dispositivo; alcance de unicidad; huella del payload; ventana de retención; trazado paso a paso de F03 y F12. **Hoy no existe** (ARG01-C01).

## P2. «Sin idempotencia nativa en el Core, ¿puede afirmar ejecución exactamente una vez? Si no, ¿qué garantiza exactamente?»

- **Qué evalúa:** la honestidad de la garantía y la comprensión de los límites teóricos de los sistemas distribuidos.
- **Por qué es difícil:** la respuesta correcta es «no». La conciliación por correlación es probabilística, y QA3 promete «0 duplicados».
- **Evidencia esperada:** una formulación precisa de la garantía («como máximo una iniciación por intención, conciliación con exactitud medible»); coherencia entre QA3 y RR-04 (ARG01-M01); el plan para validar S3 con el propietario del Core.

## P3. «Una solicitud llega con una clave ya usada pero con otro monto. ¿Qué hace la plataforma y por qué es seguro?»

- **Qué evalúa:** la vinculación entre la clave y el contenido de la operación.
- **Por qué es difícil:** devolver el resultado anterior oculta un error del cliente o un ataque; ejecutar la nueva solicitud rompe la idempotencia; sobrescribir es peor. El diseño actual no lo define.
- **Evidencia esperada:** regla de conflicto con huella canónica del payload, respuesta sin efectos y registro en la auditoría (ARG01-C01, F04).

## P4. «Sus ACL tienen Bulkheads independientes, pero todas usan el mismo Core físico. ¿Qué aísla realmente una avalancha de consultas de movimientos de las transferencias? ¿Y con 20 réplicas?»

- **Qué evalúa:** la distinción entre aislamiento lógico e infraestructura compartida, y la protección del Core bajo escalado horizontal.
- **Por qué es difícil:** los límites por instancia se multiplican con las réplicas. Sin un presupuesto global, la saturación de lecturas degrada las escrituras en el Core, y esa es una ventaja que la opción B (capa centralizada) sí tendría.
- **Evidencia esperada:** presupuesto de capacidad del Core por dominio y prioridad de las escrituras; mecanismo que respete el límite total con N réplicas; por qué esto no invalida la elección de la opción C (ARG01-H04).

## P5. «El Core confirmó el débito de una interbancaria, pero la infraestructura de pagos la rechaza. Su estado pasa a `FAILED`. ¿Dónde está el dinero del cliente?»

- **Qué evalúa:** la coherencia del modelo de estados con la realidad contable.
- **Por qué es difícil:** `FAILED` se define «sin efecto o revertido», pero la transición permite llegar a ese estado con un débito pendiente de reverso. Además, la topología interbancaria no está confirmada (Q-01).
- **Evidencia esperada:** un estado explícito de reverso pendiente con su conciliación; la semántica de `COMPLETED`; la dependencia de Q-01 explicitada (ARG01-H02).

## P6. «Una transferencia lleva tres días en `REQUIRES_RECONCILIATION`. ¿Quién es responsable, qué ve el cliente y qué pasa con sus fondos?»

- **Qué evalúa:** si el diseño técnico tiene un respaldo operativo real.
- **Por qué es difícil:** la resolución depende de un actor **candidato** (DD-18) sin plazo ni escalamiento definidos. Un comité de una institución financiera lo verá como un riesgo de reclamos y de supervisión.
- **Evidencia esperada:** el área responsable confirmada; un plazo máximo como parámetro por acordar; escalamiento; comunicación al cliente; segregación de funciones en la resolución manual (ARG01-H03).

## P7. «Una persona que ya es cliente en agencia hace la vinculación digital con su cédula. Su conciliación encuentra un cliente con esa identificación y concluye `CORE_CREATED`. ¿Acaba de darle acceso digital a cuentas existentes a quien superó un KYC?»

- **Qué evalúa:** la seguridad del onboarding y la separación entre creación y enrolamiento.
- **Por qué es difícil:** la regla diseñada para evitar duplicados abre un vector de toma de cuentas, y el reto habla de «nuevos usuarios», no necesariamente de nuevos clientes.
- **Evidencia esperada:** un criterio que distinga un cliente creado por esta vinculación de un cliente preexistente; un flujo de enrolamiento de clientes existentes con controles propios (ARG01-H05).

## P8. «El reto dice que el API Gateway debe "orquestar" tres servicios. Usted lo convirtió en una fachada. ¿No está cambiando el requisito para que se ajuste a su arquitectura?»

- **Qué evalúa:** si la interpretación del requisito es legítima o una reinterpretación conveniente.
- **Por qué es difícil:** exige argumentar contra la lectura literal sin parecer evasivo, y mostrar que el requisito del evaluador sigue cumpliéndose.
- **Evidencia esperada:** ADR-0002 con la interpretación declarada (existe); un diagrama de contenedores que muestre el Gateway enrutando hacia al menos los tres servicios; un criterio verificable entre composición (BFF) y orquestación (ARG01-L02); los riesgos concretos de la lectura literal (lógica de transferencias en un producto de Gateway).

## P9. «Su vista de contexto dibuja una línea continua y "confirmada" desde la plataforma al sistema de transferencias, pero usted dice que no sabe si la conexión es directa. ¿Qué afirma realmente el diagrama?»

- **Qué evalúa:** la precisión semántica del modelo C4 y la coherencia entre el diagrama y el texto.
- **Por qué es difícil:** el diagrama se lee sin el documento; una relación de nivel 1 puede representar una dependencia lógica, pero el estilo visual comunica certeza.
- **Evidencia esperada:** la convención de relaciones lógicas frente a conexiones; un estilo diferenciado para la «dependencia confirmada, ruta por validar»; los tres modelos de integración y su impacto en la Fase 3 (ARG01-M07, Q-01).

## P10. «Si la institución resulta ser una cooperativa supervisada por la SEPS, con obligaciones de prevención de lavado de activos sobre el onboarding y sin opción de procesar biometría fuera del país, ¿qué partes de su arquitectura cambian?»

- **Qué evalúa:** la resiliencia de la arquitectura ante la incertidumbre regulatoria y la trazabilidad de los supuestos.
- **Por qué es difícil:** combina tres supuestos abiertos (S8, S15, Q-12) que afectan el contexto, los proveedores, la región cloud y los flujos. El registro regulatorio que exige la propia política no existe.
- **Evidencia esperada:** el registro `REG-PV` con el análisis de impacto por consideración (ARG01-M05); la lista de decisiones afectadas (ADR-0010, DD-05, DD-14, DD-15); la demostración de que la arquitectura lógica de las Fases 0–2 no depende de la región; los sistemas de fraude y prevención de lavado como candidatos (ARG01-M06).
