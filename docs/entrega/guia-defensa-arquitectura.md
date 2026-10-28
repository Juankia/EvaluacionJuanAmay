# Guía de defensa de la arquitectura

| Campo | Valor |
|---|---|
| Documento | Preguntas difíciles del comité y respuestas técnicas concisas |
| Fecha | 2026-10-27 |

---

## 1. ¿Por qué las transferencias están bloqueadas?

**Respuesta:** porque en banca un error de diseño en la ejecución significa dinero duplicado o perdido. Además, la revisión ARG-03 encontró un defecto crítico en el diario de autorización de despacho (ARG03-C01): con un testigo, una entrada confirmada puede existir en una sola región. Construir sobre ese diseño habría sido presentar como seguro algo que no lo es.

**Qué demuestra:** gobierno de riesgos. El bloqueo B-05 tiene condiciones explícitas de salida: enmendar ADR-0021, obtener una verificación humana independiente y aportar evidencia del producto del diario (DD-23). El contrato de frontera de transferencias existe y está marcado `BLOCKED`, de modo que los consumidores no dependen de su semántica interna.

## 2. ¿El API Gateway «orquesta» los tres servicios como pide R1?

El Gateway enruta, valida tokens y limita la tasa. La **composición** de lecturas la hace el BFF y la **orquestación transaccional** el servicio de transferencias (ADR-0002, ADR-0014). Poner lógica de negocio en el Gateway lo convertiría en un monolito difícil de probar, con fallas acopladas. R1 se cumple a nivel de capa de integración: Gateway + BFF + tres servicios.

## 3. ¿Qué pasa si el Core no responde a mitad de una transferencia?

La operación pasa a `CORE_UNKNOWN`, **no** a fallida, y **no** se reintenta. La conciliación consulta el Core por la referencia única hasta confirmar o descartar el efecto, y si se vence el plazo la operación va a revisión manual. Así se evita el doble débito (ADR-0005, ADR-0006). Este comportamiento depende de capacidades del Core por confirmar (Q-02, Q-17).

## 4. ¿Cómo evitan que la plataforma tumbe al Core?

Con un presupuesto global de capacidad por clase de operación y control de admisión en cada ACL (ADR-0011), más caché para las lecturas frecuentes (ADR-0004). Escalar la plataforma **no** aumenta la carga al Core por encima del presupuesto.

## 5. ¿Por qué AWS y no Azure?

En la comparación ponderada AWS obtiene 87 y Azure 84: es una diferencia pequeña y se declara así. El factor diferencial es el RabbitMQ gestionado, coherente con la base ejecutable. Se elegiría Azure si la institución usa Entra ID y su equipo domina Azure, o si la latencia medida hacia el Core la favorece ([27 §4](../27-infraestructura-cloud.md#4-sensibilidad-cuándo-se-elegiría-azure)). Es una **referencia**, no una selección definitiva.

## 6. ¿Cuál es su RTO/RPO?

No se inventan. Se proponen **candidatos** (por ejemplo, ≤ 4 h y ≤ 15 min para las capacidades no financieras ante la pérdida de una región) que la institución debe acordar con su análisis de impacto (DD-09). Para el estado financiero el requisito es **RPO 0 de las escrituras confirmadas**, y ante la pérdida de una región la postura es suspender y conciliar, no continuar ([28](../28-alta-disponibilidad-y-recuperacion.md)).

## 7. ¿Por qué no activo-activo multirregión?

El estado financiero exige un solo escritor (PT-03). El activo-activo introduce cerebro dividido y conflictos de escritura justo donde un error cuesta dinero. Se acepta un RTO regional mayor a cambio de integridad.

## 8. ¿La biometría del teléfono autentica al cliente?

Solo desbloquea una credencial guardada en el dispositivo y registrada en el IdP. La verificación facial y la prueba de vida ocurren en el proveedor KYC durante la vinculación, y **la autorización siempre la decide el servidor**. La plataforma no recibe ni guarda datos biométricos ([29 §2.2](../29-seguridad-identidad-y-cumplimiento.md#22-cuatro-conceptos-que-no-deben-confundirse)).

## 9. ¿Por qué el BFF guarda sesiones si los microservicios deben ser sin estado?

El **proceso** del BFF no guarda estado; la sesión vive en un almacén dedicado (ADR-0019). Esto mantiene los tokens fuera del navegador, protege frente a XSS y permite revocar todas las sesiones de un sujeto. El precio es un componente crítico más, que se despliega en multizona.

## 10. ¿Cumplen la LOPDP y PCI DSS?

No se afirma cumplimiento. El diseño incorpora las obligaciones relevantes de la LOPDP: la biometría es dato sensible, hay consentimiento expreso, minimización, evaluación de impacto y notificación de vulneraciones. La validación final corresponde al área legal. PCI DSS es **condicional**: el alcance actual no procesa datos de tarjetas ([29 §5](../29-seguridad-identidad-y-cumplimiento.md#5-marco-normativo-c1--referencias-no-certificación)).

## 11. ¿Por qué microservicios y no un monolito?

Es una descomposición **híbrida**, no microservicios finos: cinco servicios de dominio con ACL internas (ADR-0012). El criterio fue aislar las fallas y la propiedad de los datos donde el riesgo lo exige (transferencias, vinculación, auditoría). La alternativa monolítica modular se evaluó en el documento 20, y su sensibilidad a las capacidades del equipo (Q-18) está declarada.

## 12. ¿Las revisiones ARG son independientes?

No. Las produjo el mismo agente que diseñó la arquitectura, con criterios adversariales, y así se declara (ARG03-L03). Antes de levantar B-05 se exige una revisión humana de un tercero.

## 13. ¿Qué parte del código funciona hoy?

Funcionan y están probados localmente auditoría, notificaciones (con proveedores falsos), el subconjunto seguro de vinculación, el simulador del Core y las bibliotecas. **No existen** Gateway, BFF, IdP, consultas, caché ni frontend. La evidencia está en [validacion-final.md](validacion-final.md).

## 14. ¿Cómo se notifica «cada movimiento» si hay movimientos fuera del canal digital?

Es la pregunta abierta Q-04. La plataforma notifica lo que origina; los movimientos de ventanilla o de procesos batch requieren que el Core publique eventos o que se consulten periódicamente, y ninguna de las dos opciones se asume sin confirmar.
