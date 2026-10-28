# 02 — Entorno local y construcción

| Campo | Valor |
|---|---|
| Documento | Requisitos y comandos para construir, probar y ejecutar la base localmente |
| Fecha | 2026-10-27 |

---

## 1. Requisitos

| Herramienta | Versión | Uso |
|---|---|---|
| JDK | 21 | Compilación (`maven.compiler.release=21`) |
| Maven | 3.9 o superior | Construcción multimódulo |
| Docker | Motor en ejecución | Dev Services de Quarkus (PostgreSQL y RabbitMQ) en las pruebas, y entorno local |
| Node.js | 20 o superior | Solo para `packages/api-contracts` (tipos y prueba de sincronía) |

## 2. Construcción y pruebas

```bash
# Todo el backend: compila, ejecuta las pruebas unitarias y de integración
mvn -B verify

# Un solo servicio y sus dependencias
mvn -B -pl services/digital-linking -am verify

# Tipos TypeScript de los contratos
cd packages/api-contracts && npm install && npm run typecheck && npm test
```

Las pruebas `@QuarkusTest` levantan PostgreSQL y RabbitMQ con Dev Services (Testcontainers). No usan servicios compartidos ni credenciales reales. La propiedad `contracts.dir`, que fija el POM agregador, apunta a `packages/api-contracts/openapi` para las pruebas de contrato ([04](04-pruebas-de-contrato.md)).

## 3. Entorno local

`infra/local/docker-compose.yml` levanta un PostgreSQL por servicio (propiedad de los datos, [ADR-0016](../adr/ADR-0016-persistencia-y-durabilidad-del-estado.md)) y un RabbitMQ local:

```bash
cp infra/local/.env.example infra/local/.env   # editar los valores; .env no se versiona
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up -d
```

En modo de desarrollo (`mvn quarkus:dev` dentro de un servicio), Quarkus usa Dev Services y no necesita este entorno. El perfil `prod` toma la configuración de las variables de entorno listadas en `.env.example`.

| Servicio | Puerto HTTP |
|---|---|
| Vinculación digital | 8082 |
| Notificaciones | 8083 |
| Auditoría | 8084 |
| Simulador del Core | 8090 |

## 4. Reglas del entorno

- Solo datos **sintéticos**. Ningún identificador real de clientes.
- Ninguna conexión con el Core, con proveedores KYC reales ni con pasarelas reales de mensajería.
- Las claves de `.env.example` son marcadores; las claves de prueba que figuran en `application.properties` están marcadas como no secretas y valen solo para los perfiles `dev` y `test`.
