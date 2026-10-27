# 20 — Alternativas de descomposición

| Campo | Valor |
|---|---|
| Fase | 3 — C4 Nivel 2: Contenedores |
| Estado | Decisión adoptada en [ADR-0012](adr/ADR-0012-descomposicion-hibrida-de-contenedores.md) (aceptada con condiciones; pendiente de ARG-02) |

## 1. Principio

**Un límite de dominio no exige un microservicio.** Un dominio se despliega por separado solo si lo justifica un criterio verificable:

- perfil de capacidad frente al Core;
- aislamiento de seguridad;
- aislamiento de fallas con impacto en la integridad;
- ciclo de cambio o responsable distintos.

## 2. Alternativas

| | A. Monolito modular | B. Híbrida | C. Microservicios por dominio |
|---|---|---|---|
| Despliegues de backend | 1 (más el IdP) | BFF + 5 servicios de dominio (más el IdP) | BFF + un servicio por capacidad (cliente, cuentas, movimientos, transferencias, vinculación, KYC, notificaciones, auditoría, conciliación) + adaptadores de ACL desplegables (≈ 12–15) |
| Límites de dominio | Módulos con reglas de dependencia | Módulos dentro de cada servicio + servicios por criterio | Un servicio por dominio |
| ACL | Módulos | Módulos dentro del servicio dueño | Adaptadores desplegables |

## 3. Matriz de decisión

Escala de 1 (peor) a 5 (mejor). Los pesos reflejan la priorización de [03](03-atributos-de-calidad.md): integridad, seguridad y disponibilidad son críticas.

| Criterio | Peso | A | B | C | Comentario |
|---|---|---|---|---|---|
| Dependencia del Core y protección de su capacidad | 15 % | 3 | **5** | 3 | B separa las clases CL-1/CL-2 (transferencias, vinculación) de CL-3/CL-4 (consultas) y permite cuotas por servicio (ADR-0011). En A comparten proceso; en C hay más consumidores que coordinar |
| Integridad transaccional | 15 % | 4 | **5** | 2 | En B el permiso de envío y el envío ocurren en el mismo proceso, aislado de las consultas. C agrega saltos de red (adaptadores) que crean nuevos puntos `UNKNOWN` |
| Aislamiento de seguridad | 12 % | 2 | **4** | 5 | La biometría, el estado financiero y la auditoría quedan en contenedores y almacenes separados en B y C; en A comparten proceso |
| Disponibilidad y aislamiento de fallas | 12 % | 2 | **4** | 4 | Una saturación de las consultas no tumba las transferencias en B ni en C |
| Complejidad operativa | 10 % | **5** | 3 | 1 | — |
| Supuesto sobre el tamaño del equipo (S11) | 8 % | **5** | 4 | 2 | B es viable con un equipo mediano; C exige varios equipos |
| Escalado independiente | 8 % | 2 | **4** | 5 | — |
| Independencia de despliegue | 5 % | 1 | **4** | 5 | — |
| Observabilidad | 5 % | **5** | 4 | 2 | C multiplica las trazas distribuidas y los puntos de correlación |
| Costo de infraestructura | 5 % | **5** | 3 | 2 | — |
| Mantenimiento | 3 % | 3 | **4** | 3 | — |
| Evolución | 2 % | 2 | 4 | **5** | Un servicio de B puede dividirse más adelante con evidencia |
| **Total ponderado** | 100 % | **3,27** | **4,15** | **3,13** | — |

Cálculo (peso × puntuación):

- A = 0,45 + 0,60 + 0,24 + 0,24 + 0,50 + 0,40 + 0,16 + 0,05 + 0,25 + 0,25 + 0,09 + 0,04 = **3,27**
- B = 0,75 + 0,75 + 0,48 + 0,48 + 0,30 + 0,32 + 0,32 + 0,20 + 0,20 + 0,15 + 0,12 + 0,08 = **4,15**
- C = 0,45 + 0,30 + 0,60 + 0,48 + 0,10 + 0,16 + 0,40 + 0,25 + 0,10 + 0,10 + 0,09 + 0,10 = **3,13**

**Sensibilidad.** B sigue siendo la preferida aunque se duplique el peso de la complejidad operativa y se renormalice (A = 3,43; B = 4,05; C = 2,94). A solo supera a B si el equipo es de 1–2 personas (S11), en cuyo caso se reevalúa.

## 4. Selección

Se selecciona **B. Arquitectura híbrida** ([ADR-0012](adr/ADR-0012-descomposicion-hibrida-de-contenedores.md)).

| Servicio | Criterio que justifica su despliegue separado |
|---|---|
| Servicio de consultas | Capacidad (CL-3/CL-4, alto volumen, caché); escalado independiente |
| Servicio de vinculación | Seguridad (biometría, alta de clientes, activación del acceso); ciclo de cambio propio |
| Servicio de transferencias | Integridad (estado financiero, CL-1/CL-2); aislamiento de fallas |
| Servicio de notificaciones | Disponibilidad (proveedores externos, reintentos) sin afectar la operación |
| Servicio de auditoría | Seguridad (almacén de solo anexión, acceso restringido); responsable distinto (auditoría) |
| BFF de canales | Seguridad de la SPA (gestor de tokens); adaptación por canal ([ADR-0014](adr/ADR-0014-api-gateway-y-bff-de-canales.md)) |

**Capacidades que no se separan:** cliente, cuentas y movimientos (las tres son lecturas del mismo Core con el mismo perfil); la conciliación (es un rol interno del dueño del estado); KYC (es una integración de la vinculación).

## 5. Disparadores de reevaluación

- El equipo es de 1–2 ingenieros → reevaluar A.
- Divergencia de capacidad o de responsables entre cliente y movimientos → dividir el servicio de consultas.
- Activación de CQRS (ADR-0007) → posible contenedor de proyección.

## 6. Actualización de la remediación de ARG-02 (ARG02-M07, L03)

ARG-02 recalculó la matriz con cinco variantes:

| Escenario | A | B | C |
|---|---|---|---|
| Pesos originales | 3,27 | 4,15 | 3,13 |
| Pesos iguales | 3,25 | 4,00 | 3,25 |
| Doble conteo corregido (A con integridad 5) | 3,42 | 4,15 | 3,13 |
| B con complejidad y costo 2 | 3,27 | 4,00 | 3,13 |
| Equipo pequeño | 3,59 | 4,00 | 2,84 |
| **Combinado adverso** | **3,72** | **3,76** | 2,84 |

**Conclusión corregida:** B **no es incuestionablemente superior**. En el escenario combinado adverso (equipo pequeño, complejidad real de 21 contenedores, IdP autohospedado) es **prácticamente equivalente** a A.

- La selección de B se **mantiene** porque gana en todos los escenarios y aísla mejor el estado financiero.
- Queda **condicionada** a Q-18 (tamaño y capacidades del equipo), que pasa a ser una **dependencia explícita de la decisión** antes de la implementación.
- Si Q-18 indica un equipo pequeño, se reevalúa ADR-0012 hacia un monolito modular con la misma separación lógica (módulos, ACL internas, almacenes lógicos), lo que conservaría la mayor parte del diseño de componentes.
- Los criterios «dependencia del Core» e «integridad transaccional» están correlacionados (ARG02-L03). El escenario «doble conteo corregido» muestra que la conclusión no depende de esa correlación.
