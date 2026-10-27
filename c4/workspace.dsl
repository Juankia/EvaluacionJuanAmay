/*
 * Plataforma de Banca Digital — modelo C4 (Structurizr DSL)
 *
 * Fuente autoritativa de los diagramas de arquitectura.
 *
 * Estado: Fase 3 — C4 Nivel 2 (Contenedores). Se conserva el Nivel 1 de la Fase 2.
 *   - Vista "ContextoPrincipal": alcance confirmado por el reto y supuestos aceptados.
 *   - Vista "ContextoAmpliado": agrega actores y sistemas candidatos y relaciones por validar.
 *   - Vista "Contenedores": contenedores de aplicación (sin almacenes ni broker).
 *   - Vistas focalizadas: Consultas, Vinculación, Transferencias, NotificacionesAuditoria, IntegracionCore.
 *   Cambio de la Fase 3: el IdP candidato externo se resuelve como contenedor interno (ADR-0013).
 *   Estado de diseño de los contenedores (propiedad "EstadoDiseño"): Aprobado, Aprobado con condiciones,
 *   Provisional, Diferido o Bloqueado (docs/23-riesgos-y-condiciones-fase-3.md).
 *   Los límites de confianza NO se representan aquí. Están en docs/14-limites-de-confianza.md,
 *   porque un límite de sistema de software (C4) no es un límite de confianza de red.
 *
 * Convención de confianza de las relaciones (propiedad "Confianza" y etiqueta):
 *   - Confirmada:   la establece el enunciado del reto.
 *   - Supuesta:     supuesto arquitectónico aceptado (docs/02-supuestos.md).
 *   - Por validar:  candidata. Solo aparece en la vista ampliada.
 *   - Ruta por validar: la dependencia la establece el reto,
 *     pero no la conexión directa (REL-05; remediación ARG01-M07).
 *
 * Decisiones vigentes que el modelo respeta:
 *   - ADR-0001: integración con el Core mediante ACL por dominio (detalle interno, no visible en el nivel 1).
 *   - ADR-0002 y ADR-0014: el API Gateway y el BFF son contenedores internos; sin lógica financiera.
 *   - ADR-0005, ADR-0006: el orquestador de transferencias es PROVISIONAL (CF-01/CF-02 sin verificación independiente).
 *   - ADR-0012: descomposición híbrida; las ACL son módulos internos de los servicios, no contenedores.
 *   - ADR-0003: el Integration Platform SDK es una biblioteca; no es un contenedor.
 *   - ADR-0009: el Core Bancario es autoritativo para la identidad del cliente.
 *   - Supuesto S12: la plataforma no accede a la base de datos principal; esta no se modela.
 */
workspace "Plataforma de Banca Digital" "Arquitectura de la plataforma de banca digital web y móvil (reto técnico de Arquitecto de Soluciones)." {

    !identifiers hierarchical

    model {
        properties {
            "fase" "2"
        }

        // ================================================================
        // Actores
        // ================================================================
        cliente = person "Cliente bancario" "Cliente de la institución (socio, en el contexto cooperativo) que usa los canales web y móvil para consultar su información, cuentas y movimientos y realizar transferencias." {
            tags "Actor confirmado"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: contexto, R1, R4, R6"
            }
        }
        solicitante = person "Solicitante de vinculación" "Persona que inicia su vinculación como nuevo cliente desde la app móvil, con verificación de identidad y reconocimiento facial." {
            tags "Actor confirmado"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: R3"
            }
        }
        operadorSoporte = person "Operador de soporte" "Candidato. Personal de la institución que atiende casos de operaciones en verificación, conciliación y revisión manual de vinculaciones." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "Derivado de ADR-0005, ADR-0006 y ADR-0009. No lo menciona el reto"
            }
        }
        auditor = person "Responsable de auditoría y cumplimiento" "Candidato. Personal que consulta el registro de auditoría de las acciones del cliente." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "Derivado de R5. El reto no indica quién consulta la auditoría ni por qué medio"
            }
        }

        // ================================================================
        // Sistema en diseño
        // ================================================================
        plataforma = softwareSystem "Plataforma de Banca Digital" "Plataforma de canales digitales web y móvil que permite a clientes de una institución financiera consultar información, visualizar movimientos, realizar transferencias y completar procesos de vinculación digital, mediante integraciones seguras con sistemas bancarios y proveedores externos." {
            tags "Sistema en diseño"

            // ---------------- Canales ----------------
            spa = container "SPA web" "Aplicación de página única para la banca web: consultas y transferencias. No conserva tokens OAuth en el navegador (sesión gestionada por el BFF)." "React + TypeScript" {
                tags "Navegador"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }
            app = container "Aplicación móvil" "Aplicación multiplataforma para la banca móvil y la vinculación digital. Integra el SDK del proveedor KYC/biométrico y la biometría local del dispositivo." "React Native + TypeScript (recomendado, ADR-0018)" {
                tags "Móvil"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }

            // ---------------- Borde ----------------
            gateway = container "API Gateway" "Punto único de entrada: enrutamiento, validación de tokens, limitación de tasa, correlación y políticas de seguridad de borde. Sin lógica de negocio ni estado financiero (ADR-0002)." "Producto de API Gateway gestionado (por seleccionar, DD-15)" {
                tags "Borde"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }
            bff = container "BFF de canales digitales" "Backend for Frontend compartido por web y móvil: gestor de tokens de la SPA, composición de lecturas y adaptación por canal. No posee invariantes financieros (ADR-0014)." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }

            // ---------------- Identidad ----------------
            idp = container "Proveedor de identidad (IdP)" "Servidor de autorización OAuth 2.0 / OpenID Connect para clientes: autenticación, MFA, sesiones y emisión de tokens. Interno a la plataforma (ADR-0013)." "Producto OIDC; candidato provisional: Keycloak (ADR-0013)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }
            idpDb = container "BD del IdP" "Cuentas digitales, credenciales (con hash), factores y sesiones del IdP." "PostgreSQL (ADR-0016)" {
                tags "Almacén de datos"
            }

            // ---------------- Dominio ----------------
            consultas = container "Servicio de consultas" "Datos del cliente, cuentas, saldos y movimientos. Contiene las ACL de clientes (lectura), de movimientos y del Sistema Complementario como módulos internos. Aplica Cache-Aside (ADR-0004) y cuotas CL-3/CL-4 (ADR-0011)." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }
            consultasDb = container "BD de consultas" "Outbox de eventos de auditoría de consultas y metadatos locales. No almacena saldos ni movimientos como fuente." "PostgreSQL (ADR-0016)" {
                tags "Almacén de datos"
            }
            cache = container "Caché de consultas" "Datos derivados del Core y del Sistema Complementario según la matriz de cacheabilidad. Nunca autoritativa." "Caché distribuida compatible con Redis (provisional, DD-07)" {
                tags "Almacén de datos"
            }
            vinculacion = container "Servicio de vinculación digital" "Máquina de estados de vinculación (E1 a E6, ADR-0009): verificación de identidad, identificación y alta del cliente, autorización de la vinculación y activación del acceso digital. Contiene la ACL de alta y búsqueda de clientes." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones; ruta PRE_EXISTING bloqueada en producción hasta DD-21"
                }
            }
            vinculacionDb = container "BD de vinculación" "Estado durable de las solicitudes de vinculación, claves de idempotencia, evidencia de creación y outbox." "PostgreSQL (ADR-0016)" {
                tags "Almacén de datos"
            }
            transferencias = container "Servicio de transferencias" "Contrato de idempotencia (ADR-0006) y orquestador durable de transferencias (ADR-0005): titularidad cercada, permiso de envío único, conciliación y efecto tardío. Contiene la ACL de transferencias. PROVISIONAL: CF-01/CF-02 sin verificación independiente." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación" "Provisional"
                properties {
                    "EstadoDiseño" "Provisional"
                }
            }
            transferenciasDb = container "BD transaccional de transferencias" "Intenciones, huellas, estado de las operaciones, titularidad y tokens, evidencia huérfana, casos de conciliación y outbox. Requisitos: escritor único, RPO 0 de las escrituras confirmadas y cuarentena tras restauración (ADR-0016)." "PostgreSQL (ADR-0016)" {
                tags "Almacén de datos" "Provisional"
                properties {
                    "EstadoDiseño" "Provisional"
                }
            }

            // ---------------- Notificaciones y auditoría ----------------
            broker = container "Broker de mensajería" "Distribuye los eventos publicados desde los outbox: entrega al menos una vez, tópicos con suscripciones, colas de mensajes fallidos (ADR-0015)." "Broker gestionado de colas y tópicos (por seleccionar, DD-15)" {
                tags "Mensajería"
            }
            notif = container "Servicio de notificaciones" "Convierte eventos confirmados en notificaciones push, SMS y correo; deduplicación, reintentos y canal alternativo. Una falla de notificación nunca revierte una operación." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }
            notifDb = container "BD de notificaciones" "Registro de eventos recibidos, intentos de entrega y preferencias de canal." "PostgreSQL (ADR-0016)" {
                tags "Almacén de datos"
            }
            auditoria = container "Servicio de auditoría" "Recibe los eventos de auditoría de todos los contenedores y los registra en un almacén de solo anexión con encadenamiento de hashes (R5)." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }
            auditoriaDb = container "Almacén de auditoría" "Registro de auditoría de solo anexión, con evidencia de integridad y archivo inmutable según la retención acordada." "PostgreSQL + archivo inmutable en almacenamiento de objetos (ADR-0016)" {
                tags "Almacén de datos"
            }

            // ---------------- Operaciones ----------------
            consola = container "Consola de operaciones" "Resolución de REQUIRES_RECONCILIATION y MANUAL_REVIEW con cuatro ojos, cierre excepcional y consulta de auditoría. PROVISIONAL: puede sustituirse por un sistema administrativo existente (DD-18)." "Aplicación web interna (por definir)" {
                tags "Aplicación" "Provisional"
                properties {
                    "EstadoDiseño" "Provisional"
                }
            }
        }

        // ================================================================
        // Sistemas existentes de la institución
        // ================================================================
        core = softwareSystem "Core Bancario" "Sistema existente y autoritativo para los datos maestros del cliente, productos, cuentas, movimientos y transacciones bancarias (S1, S2, ADR-0009)." {
            tags "Sistema existente"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: contexto"
            }
        }
        complementario = softwareSystem "Sistema Complementario de Clientes" "Sistema existente con información extendida del cliente. No es autoritativo para la creación ni para la identidad del cliente (S5, ADR-0009)." {
            tags "Sistema existente"
            properties {
                "Confianza" "Confirmada"
                "Fuente" "Reto: contexto"
            }
        }

        // ================================================================
        // Infraestructura financiera externa
        // ================================================================
        transferencias = softwareSystem "Sistema Externo de Transferencias" "Capacidad externa que procesa las transferencias interbancarias. La topología de integración (directa, mediante la infraestructura de pagos existente de la institución o mediante un intermediario autorizado) está por validar (S13, DD-16)." {
            tags "Infraestructura financiera externa"
            properties {
                "Confianza" "Confirmada (dependencia); topología por validar"
                "Fuente" "Reto: R1 («invoca servicios externos según el tipo»)"
            }
        }

        // ================================================================
        // Proveedores terceros
        // ================================================================
        kyc = softwareSystem "Proveedor KYC/Biométrico" "Capacidad externa de verificación de identidad: reconocimiento facial, prueba de vida, validación de documentos de identidad y resultado de la verificación. Proveedor no seleccionado (DD-14)." {
            tags "Proveedor tercero"
            properties {
                "Confianza" "Supuesta (S16)"
                "Fuente" "Reto: R2, R3"
            }
        }
        notificaciones = softwareSystem "Proveedores de Notificaciones" "Capacidades externas de entrega de notificaciones por notificación push, SMS y correo electrónico. Proveedores no seleccionados (DD-12)." {
            tags "Proveedor tercero"
            properties {
                "Confianza" "Supuesta (S17)"
                "Fuente" "Reto: R4 («propios o externos»)"
            }
        }

        // ================================================================
        // Sistemas candidatos (solo en la vista ampliada)
        // ================================================================
        fuenteIdentidad = softwareSystem "Fuente Oficial de Identidad" "Candidato. Fuente oficial de datos de identificación de personas. Su uso requiere validar la autorización legal, la disponibilidad técnica y el mecanismo de acceso (DD-17)." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "No lo menciona el reto. Derivado de R3 y de prácticas de KYC"
            }
        }

        // ================================================================
        // Relaciones — alcance principal (Confirmada / Supuesta)
        // ================================================================
        cliente -> plataforma "Consulta su información, cuentas y movimientos, y realiza transferencias propias e interbancarias" "" "Confirmada" {
            properties {
                "ID" "REL-01"
                "Confianza" "Confirmada"
            }
        }
        solicitante -> plataforma "Inicia su vinculación digital y completa la verificación de identidad y biométrica desde la app móvil" "" "Confirmada" {
            properties {
                "ID" "REL-02"
                "Confianza" "Confirmada"
            }
        }
        plataforma -> core "Consulta datos del cliente, cuentas y movimientos, y solicita la ejecución de transferencias y la creación de clientes" "" "Confirmada" {
            properties {
                "ID" "REL-03"
                "Confianza" "Confirmada (consultas); decisión ADR-0009 (creación de clientes)"
            }
        }
        plataforma -> complementario "Consulta información extendida del cliente" "" "Confirmada" {
            properties {
                "ID" "REL-04"
                "Confianza" "Confirmada"
            }
        }
        plataforma -> transferencias "Solicita el procesamiento de transferencias interbancarias y consulta su resultado (dependencia lógica; ruta por validar)" "" "Ruta por validar" {
            properties {
                "ID" "REL-05"
                "Confianza" "Confirmada (dependencia); topología por validar"
            }
        }
        plataforma -> kyc "Verifica la identidad del solicitante mediante reconocimiento facial, prueba de vida y validación de documento" "" "Supuesta" {
            properties {
                "ID" "REL-06"
                "Confianza" "Supuesta (S16)"
            }
        }
        plataforma -> notificaciones "Solicita el envío de notificaciones de movimientos y del estado de la vinculación" "" "Supuesta" {
            properties {
                "ID" "REL-07"
                "Confianza" "Supuesta (S17)"
            }
        }
        notificaciones -> cliente "Entrega notificaciones de cada movimiento" "" "Supuesta" {
            properties {
                "ID" "REL-08"
                "Confianza" "Supuesta (S17)"
            }
        }
        notificaciones -> solicitante "Entrega notificaciones del estado de la vinculación" "" "Supuesta" {
            properties {
                "ID" "REL-09"
                "Confianza" "Supuesta (S17)"
            }
        }

        // ================================================================
        // Relaciones — por validar (solo vista ampliada)
        // ================================================================
        plataforma -> fuenteIdentidad "Contrasta los datos de identificación del solicitante (mecanismo de acceso por validar)" "" "Por validar" {
            properties {
                "ID" "REL-12"
                "Confianza" "Por validar (DD-17)"
            }
        }
        core -> plataforma "Informa movimientos originados en otros canales para su notificación" "" "Por validar" {
            properties {
                "ID" "REL-13"
                "Confianza" "Por validar (Q-04)"
            }
        }
        operadorSoporte -> plataforma "Atiende operaciones en verificación, conciliaciones y vinculaciones en revisión manual" "" "Por validar" {
            properties {
                "ID" "REL-14"
                "Confianza" "Por validar (DD-18)"
            }
        }
        auditor -> plataforma "Consulta el registro de auditoría de las acciones del cliente" "" "Por validar" {
            properties {
                "ID" "REL-15"
                "Confianza" "Por validar (DD-18)"
            }
        }

        // ================================================================
        // Relaciones de Nivel 2 — contenedores (Fase 3)
        // ================================================================
        cliente -> plataforma.spa "Usa la banca web" "HTTPS"
        cliente -> plataforma.app "Usa la banca móvil" "Aplicación nativa"
        solicitante -> plataforma.app "Realiza la vinculación digital" "Aplicación nativa"
        cliente -> plataforma.idp "Se autentica en las páginas de inicio de sesión" "HTTPS (OpenID Connect)"
        solicitante -> plataforma.idp "Registra sus credenciales al activar el acceso" "HTTPS"

        plataforma.spa -> plataforma.gateway "Invoca la API de canal con la sesión del BFF" "HTTPS/JSON; cookie segura"
        plataforma.app -> plataforma.gateway "Invoca la API de canal con un token de acceso" "HTTPS/JSON; OAuth 2.0"
        plataforma.app -> plataforma.idp "Autentica al usuario (Authorization Code + PKCE)" "HTTPS (OpenID Connect)"
        plataforma.gateway -> plataforma.idp "Obtiene las claves para validar tokens" "HTTPS (JWKS)"
        plataforma.gateway -> plataforma.bff "Enruta las solicitudes de canal" "HTTPS/JSON"
        plataforma.bff -> plataforma.idp "Intercambia códigos y renueva tokens como cliente confidencial de la SPA" "HTTPS (OAuth 2.0)"

        plataforma.bff -> plataforma.consultas "Consulta cliente, cuentas y movimientos" "HTTPS/JSON"
        plataforma.bff -> plataforma.vinculacion "Gestiona solicitudes de vinculación" "HTTPS/JSON"
        plataforma.bff -> plataforma.transferencias "Inicia transferencias y consulta su estado por identificador de operación" "HTTPS/JSON; Idempotency-Key"

        plataforma.consultas -> core "Consulta datos del cliente, cuentas y movimientos (ACL; CL-3/CL-4)" "Protocolo del Core (por validar, S3)"
        plataforma.consultas -> complementario "Consulta información extendida (ACL)" "Protocolo por validar"
        plataforma.consultas -> plataforma.cache "Lee y escribe datos derivados (Cache-Aside)" "Protocolo de la caché"
        plataforma.consultas -> plataforma.consultasDb "Registra eventos de auditoría en el outbox" "SQL/TLS"
        plataforma.consultas -> plataforma.broker "Publica eventos de auditoría y consume eventos de invalidación" "Mensajería"

        plataforma.vinculacion -> kyc "Verifica la identidad del solicitante" "HTTPS (API del proveedor)"
        plataforma.vinculacion -> core "Busca y crea clientes (ACL; CL-1/CL-2)" "Protocolo del Core (por validar, S3)"
        plataforma.vinculacion -> plataforma.idp "Crea y activa la cuenta digital vinculada (único contenedor autorizado)" "HTTPS (API de administración)"
        plataforma.vinculacion -> plataforma.vinculacionDb "Persiste el estado y la evidencia de la vinculación" "SQL/TLS"
        plataforma.vinculacion -> plataforma.broker "Publica eventos de estado y de auditoría (outbox)" "Mensajería"

        plataforma.transferencias -> core "Ejecuta transferencias y concilia (ACL; CL-1/CL-2; permiso de envío único)" "Protocolo del Core (por validar, S3)"
        plataforma.transferencias -> transferencias "Solicita el procesamiento interbancario (ruta por validar)" "Por validar (Q-01)" "Ruta por validar"
        plataforma.transferencias -> plataforma.transferenciasDb "Reserva intenciones y aplica transiciones cercadas" "SQL/TLS"
        plataforma.transferencias -> plataforma.broker "Publica movimientos confirmados, cambios de estado y auditoría (outbox)" "Mensajería"

        plataforma.idp -> plataforma.idpDb "Lee y escribe cuentas, factores y sesiones" "SQL/TLS"
        plataforma.idp -> plataforma.broker "Publica eventos de seguridad" "Mensajería"

        plataforma.notif -> plataforma.broker "Consume eventos notificables" "Mensajería"
        plataforma.notif -> plataforma.notifDb "Deduplica y registra los intentos de entrega" "SQL/TLS"
        plataforma.notif -> notificaciones "Envía notificaciones push, SMS y correo" "HTTPS (API de los proveedores)"

        plataforma.auditoria -> plataforma.broker "Consume eventos de auditoría" "Mensajería"
        plataforma.auditoria -> plataforma.auditoriaDb "Anexa registros encadenados" "SQL/TLS"

        operadorSoporte -> plataforma.consola "Resuelve casos de conciliación y revisión manual" "HTTPS" "Por validar"
        auditor -> plataforma.consola "Consulta el registro de auditoría" "HTTPS" "Por validar"
        plataforma.consola -> plataforma.transferencias "Resuelve casos de conciliación y cierres excepcionales (cuatro ojos)" "HTTPS/JSON (API de operaciones)"
        plataforma.consola -> plataforma.vinculacion "Resuelve revisiones manuales de vinculación (cuatro ojos)" "HTTPS/JSON (API de operaciones)"
        plataforma.consola -> plataforma.auditoria "Consulta el registro de auditoría" "HTTPS/JSON"
    }

    views {
        systemContext plataforma "ContextoPrincipal" "C4 Nivel 1 — Contexto principal: alcance confirmado por el reto y supuestos aceptados." {
            title "C4 Nivel 1 — Contexto principal de la Plataforma de Banca Digital"
            include *
            exclude "element.tag==Candidato"
            exclude "relationship.tag==Por validar"
            autolayout lr 300 200
        }

        systemContext plataforma "ContextoAmpliado" "C4 Nivel 1 — Contexto ampliado: agrega actores, sistemas y relaciones candidatos que requieren validación (líneas punteadas)." {
            title "C4 Nivel 1 — Contexto ampliado de la Plataforma de Banca Digital (incluye candidatos por validar)"
            include *
            autolayout lr 300 200
        }


        container plataforma "Contenedores" "C4 Nivel 2 — Contenedores de aplicación de la Plataforma de Banca Digital. Los almacenes de datos y el broker se muestran en las vistas focalizadas." {
            title "C4 Nivel 2 — Contenedores de la Plataforma de Banca Digital"
            include *
            exclude "element.tag==Almacén de datos"
            exclude "element.tag==Mensajería"
            exclude "element.tag==Candidato"
            autolayout tb 200 200
        }

        container plataforma "Contenedores-Consultas" "Vista focalizada: consultas de cliente, cuentas y movimientos." {
            title "C4 Nivel 2 — Consultas"
            include cliente plataforma.spa plataforma.app plataforma.gateway plataforma.bff plataforma.consultas plataforma.cache plataforma.consultasDb plataforma.broker core complementario
            autolayout lr 250 150
        }

        container plataforma "Contenedores-Vinculacion" "Vista focalizada: vinculación digital (ADR-0009)." {
            title "C4 Nivel 2 — Vinculación digital"
            include solicitante plataforma.app plataforma.gateway plataforma.bff plataforma.vinculacion plataforma.vinculacionDb plataforma.idp plataforma.broker plataforma.consola kyc core
            autolayout lr 250 150
        }

        container plataforma "Contenedores-Transferencias" "Vista focalizada: transferencias. El orquestador es PROVISIONAL (CF-01/CF-02 pendientes de verificación independiente)." {
            title "C4 Nivel 2 — Transferencias (provisional)"
            include cliente plataforma.spa plataforma.app plataforma.gateway plataforma.bff plataforma.transferencias plataforma.transferenciasDb plataforma.broker plataforma.consola core transferencias
            autolayout lr 250 150
        }

        container plataforma "Contenedores-NotificacionesAuditoria" "Vista focalizada: eventos, notificaciones y auditoría." {
            title "C4 Nivel 2 — Notificaciones y auditoría"
            include plataforma.consultas plataforma.vinculacion plataforma.transferencias plataforma.idp plataforma.broker plataforma.notif plataforma.notifDb plataforma.auditoria plataforma.auditoriaDb plataforma.consola notificaciones cliente
            autolayout lr 250 150
        }

        container plataforma "Contenedores-IntegracionCore" "Vista focalizada: contenedores que consumen la capacidad del Core (ADR-0011) y sistemas externos de integración." {
            title "C4 Nivel 2 — Integración con el Core y sistemas existentes"
            include plataforma.consultas plataforma.vinculacion plataforma.transferencias core complementario transferencias kyc
            autolayout lr 250 150
        }

        // TODO (Fase 4): component <contenedor> "Componentes-<contenedor>" { ... }
        // TODO (Fase de infraestructura): deployment plataforma "Producción" "Despliegue" { ... }

        terminology {
            person "Persona"
            softwareSystem "Sistema de software"
            container "Contenedor"
            relationship "Relación"
        }

        styles {
            element "Element" {
                fontSize 22
            }
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Sistema en diseño" {
                background #1168bd
                color #ffffff
            }
            element "Sistema existente" {
                background #5b6770
                color #ffffff
            }
            element "Infraestructura financiera externa" {
                background #7a5c2e
                color #ffffff
            }
            element "Proveedor tercero" {
                background #8a8a8a
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Navegador" {
                shape WebBrowser
            }
            element "Móvil" {
                shape MobileDevicePortrait
            }
            element "Almacén de datos" {
                shape Cylinder
                background #2f6fa7
                color #ffffff
            }
            element "Mensajería" {
                shape Pipe
                background #2f6fa7
                color #ffffff
            }
            element "Provisional" {
                stroke #b45f06
                strokeWidth 6
                border dashed
            }
            element "Candidato" {
                background #ffffff
                color #5b6770
                stroke #5b6770
                border dashed
            }
            relationship "Relationship" {
                fontSize 20
                style solid
            }
            relationship "Confirmada" {
                style solid
            }
            relationship "Supuesta" {
                style dashed
            }
            relationship "Ruta por validar" {
                style dashed
                color #7a5c2e
                thickness 3
            }
            relationship "Por validar" {
                style dotted
                color #a0a0a0
            }
        }
    }

    configuration {
        scope softwaresystem
    }
}
