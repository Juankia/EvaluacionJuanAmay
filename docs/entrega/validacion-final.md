# Validación final de la entrega

| Campo | Valor |
|---|---|
| Documento | Comandos ejecutados y resultados reales de la validación previa a la entrega |
| Fecha | 2026-10-27 (hora de Ecuador) |
| Entorno | Windows 11, Git Bash, Docker Desktop 27.1.1, JDK Temurin 21.0.12, Maven 3.9.14, Node.js |
| Commit base | `b34cb73` (los cambios de esta entrega se confirman en el commit siguiente) |

---

## 1. Resumen

| # | Verificación | Resultado |
|---|---|---|
| 1 | Validación del DSL de Structurizr | **Correcta** (código de salida 0) |
| 2 | Exportación de vistas y generación de SVG | **Correcta**: 14 vistas C4 (incluida `Infraestructura-Referencia`) y 2 vistas PlantUML |
| 3 | Lint de Markdown | **0 errores** en 76 archivos |
| 4 | Enlaces relativos y anclas | **0 enlaces rotos** de 862 verificados (tras corregir 2 anclas y crear este documento) |
| 5 | Coherencia de requisitos e identificadores | R1–R6 y C1–C6 presentes en la matriz; todos los identificadores citados existen en los documentos previos |
| 6 | Índice de ADR | 23 de 23 ADR enlazados desde el README |
| 7 | Escaneo de secretos | **Sin hallazgos**; solo una clave **pública** de prueba ya versionada |
| 8 | Construcción y pruebas del backend | Ver §3 |

## 2. Comandos y resultados

### 2.1 DSL y diagramas

```bash
docker run --rm -v "<repo>/c4:/usr/local/structurizr:ro" structurizr/structurizr \
  validate -workspace /usr/local/structurizr/workspace.dsl
# exit=0

docker run --rm -v "<repo>/c4:/usr/local/structurizr:ro" -v "<repo>/diagrams/c4:/out" \
  structurizr/structurizr export -w /usr/local/structurizr/workspace.dsl -f plantuml/structurizr -o /out
# ... Writing /out/structurizr-Infraestructura-Referencia.puml ... Finished (exit=0)

docker run --rm -v "<repo>/diagrams/c4:/data" plantuml/plantuml -tsvg "/data/*.puml"                   # exit=0
docker run --rm -v "<repo>/diagrams/infraestructura:/data" plantuml/plantuml -tsvg -tpng "/data/*.puml"  # exit=0
docker run --rm -v "<repo>/diagrams/seguridad:/data" plantuml/plantuml -tsvg -tpng "/data/*.puml"        # exit=0
```

Los `.puml` previos regenerados solo diferían en los finales de línea; se restauraron para no introducir cambios sin contenido. Las tres vistas nuevas se revisaron visualmente.

### 2.2 Markdown

```bash
npx --yes markdownlint-cli2 "**/*.md" "#**/node_modules" "#**/target"
# markdownlint-cli2 v0.22.1 (markdownlint v0.40.0) — Linting: 76 file(s) — Summary: 0 error(s)
```

Los enlaces se verificaron con un script de Node que resuelve rutas relativas y anclas con el algoritmo de GitHub. La primera pasada encontró 6 fallos: 4 por este documento, que aún no existía, y 2 anclas `README.md#herramientas` rotas por la renumeración. Se corrigieron.

### 2.3 Coherencia y secretos

- Requisitos: cada uno de R1–R6 y C1–C6 tiene una fila en la [matriz](matriz-cumplimiento.md).
- Identificadores: cada Q, DD, PF4, PT, B, CP, S, QA, AQ y RI citado en los documentos nuevos existe en un documento anterior. **No se crearon preguntas ni respuestas institucionales nuevas.**
- Secretos: patrones de claves de AWS y GitHub, claves privadas PEM, tokens de Slack y Google, y asignaciones de contraseñas o claves sobre 218 archivos versionados o nuevos: sin hallazgos. `infra/local` contiene solo `.env.example` con marcadores.

## 3. Construcción y pruebas del backend

```bash
# RabbitMQ independiente: en esta máquina tarda unos 60 s en arrancar, al límite del tiempo de
# espera de Testcontainers. PostgreSQL sí se levanta con Dev Services.
docker run -d --rm --name linking-test-rmq -p 127.0.0.1:5679:5672 \
  -e RABBITMQ_DEFAULT_USER=test -e RABBITMQ_DEFAULT_PASS=test rabbitmq:4.1-management

mvn -B -ntp -o verify -Dquarkus.rabbitmq.devservices.enabled=false \
  -Drabbitmq-host=localhost -Drabbitmq-port=5679 -Drabbitmq-username=test -Drabbitmq-password=test
# BUILD SUCCESS — Total time: 09:14 min
```

| Módulo | Pruebas | Fallos | Errores |
|---|---|---|---|
| Shared Observability | 5 | 0 | 0 |
| Integration Platform SDK | 8 | 0 | 0 |
| Contract Testing | 3 | 0 | 0 |
| Core Simulator | 4 | 0 | 0 |
| Audit Service | 10 | 0 | 0 |
| Notifications Service | 9 | 0 | 0 |
| Digital Linking Service | 24 | 0 | 0 |
| **Total** | **63** | **0** | **0** |

Las pruebas de integración (`@QuarkusTest`) usan PostgreSQL real (Dev Services) y RabbitMQ real. **Desviación declarada:** RabbitMQ no se levantó con Dev Services, sino como contenedor independiente, por el tiempo de arranque del entorno. La configuración del proyecto no se modificó.

## 4. Lo que no se validó

- No hay despliegue en la nube: la HA, el DR, la latencia y los costos **no** se probaron.
- La prueba TypeScript de `packages/api-contracts` (`npm test`) no se ejecutó en esta sesión.
- No hubo análisis de vulnerabilidades de dependencias ni de imágenes de contenedor.
- No se realizó una nueva revisión ARG (fuera del alcance de la entrega).
