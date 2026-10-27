/*
 * Plataforma de Banca Digital — modelo C4 (Structurizr DSL)
 *
 * Fuente autoritativa de los diagramas de arquitectura.
 *
 * Estado: Fase 4 — C4 Nivel 3 (Componentes) para los contenedores autorizados por ARG-02-R.
 *   Se conservan el Nivel 1 (Fase 2) y el Nivel 2 (Fase 3 y remediación de ARG-02).
 *   El servicio de transferencias y el ejecutor del alta de clientes NO tienen diseño de componentes (B-05).
 *   - Vista "ContextoPrincipal": alcance confirmado por el reto y supuestos aceptados.
 *   - Vista "ContextoAmpliado": agrega actores y sistemas candidatos y relaciones por validar.
 *   - Vista "Contenedores": contenedores de aplicación (sin almacenes ni broker).
 *   - Vistas focalizadas: Consultas, Vinculación, Transferencias, NotificacionesAuditoria, IntegracionCore.
 *   Cambio de la Fase 3: el IdP candidato externo se resuelve como contenedor interno (ADR-0013).
 *   Cambios de la remediación de ARG-02: almacén de sesiones del BFF (ADR-0019), diario de autorización
 *   de despacho (ADR-0021, provisional), IdP corporativo candidato para el personal (ADR-0013) y relaciones
 *   de notificación (registro de dispositivos y datos de contacto, ARG02-M05).
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
            bff = container "BFF de canales digitales" "Backend for Frontend compartido por web y móvil: gestor de tokens de la SPA (sesión en el almacén de sesiones, ADR-0019), composición de lecturas y adaptación por canal. Proceso sin estado; no posee invariantes financieros (ADR-0014)." "Java 21 / Quarkus (provisional, ADR-0017)" {
                tags "Aplicación"
                properties {
                    "EstadoDiseño" "Aprobado con condiciones"
                }
            }

            bffSesiones = container "Almacén de sesiones del BFF" "Sesiones web y tokens de la SPA cifrados, índice de sujeto a sesiones y claves de intenciones pendientes generadas por la SPA (ADR-0019)." "Almacén clave-valor en memoria con alta disponibilidad (producto con DD-15)" {
                tags "Almacén de datos"
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
            vinculacionDb = container "BD de vinculación" "Estado durable de las solicitudes de vinculación, claves de idempotencia, evidencia de creación y outbox. Sujeta a PT-01 a PT-03 y PT-07 por el alta de clientes (ADR-0016)." "PostgreSQL (ADR-0016)" {
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

            diarioDespacho = container "Diario de autorización de despacho" "Registro independiente, de solo anexión y con confirmación por quórum en al menos tres dominios de falla. Ningún tramo se despacha sin su entrada (INV-DR-1). Mantiene la época regional (ADR-0021). PROVISIONAL." "Almacén linealizable con quórum entre dominios de falla (capacidad por seleccionar, DD-23)" {
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
        idpCorporativo = softwareSystem "IdP corporativo de la institución" "Candidato. Identidad del personal (operadores, auditores, administradores), federada con la consola de operaciones y con la administración del IdP de clientes (ADR-0013). Existencia por confirmar." {
            tags "Candidato"
            properties {
                "Confianza" "Por validar"
                "Fuente" "Remediación de ARG02-M04"
            }
        }
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
        cliente -> plataforma.spa "Usa la banca web" "HTTPS" "Nivel2"
        cliente -> plataforma.app "Usa la banca móvil" "Aplicación nativa" "Nivel2"
        solicitante -> plataforma.app "Realiza la vinculación digital" "Aplicación nativa" "Nivel2"
        cliente -> plataforma.idp "Se autentica en las páginas de inicio de sesión" "HTTPS (OpenID Connect)" "Nivel2"
        solicitante -> plataforma.idp "Registra sus credenciales al activar el acceso" "HTTPS" "Nivel2"

        plataforma.spa -> plataforma.gateway "Invoca la API de canal con la sesión del BFF" "HTTPS/JSON; cookie segura" "Nivel2"
        plataforma.app -> plataforma.gateway "Invoca la API de canal con un token de acceso" "HTTPS/JSON; OAuth 2.0" "Nivel2"
        plataforma.app -> plataforma.idp "Autentica al usuario (Authorization Code + PKCE)" "HTTPS (OpenID Connect)" "Nivel2"
        plataforma.gateway -> plataforma.idp "Obtiene las claves para validar tokens" "HTTPS (JWKS)" "Nivel2"
        plataforma.gateway -> plataforma.bff "Enruta las solicitudes de canal" "HTTPS/JSON" "Nivel2"
        plataforma.bff -> plataforma.idp "Intercambia códigos y renueva tokens como cliente confidencial de la SPA" "HTTPS (OAuth 2.0)" "Nivel2"

        plataforma.bff -> plataforma.bffSesiones "Guarda y recupera sesiones y tokens cifrados" "Protocolo del almacén (TLS)" "Nivel2"
        plataforma.bff -> plataforma.notif "Registra dispositivos y preferencias de notificación" "HTTPS/JSON" "Nivel2"
        plataforma.bff -> plataforma.consultas "Consulta cliente, cuentas y movimientos" "HTTPS/JSON" "Nivel2"
        plataforma.bff -> plataforma.vinculacion "Gestiona solicitudes de vinculación" "HTTPS/JSON" "Nivel2"
        plataforma.bff -> plataforma.transferencias "Inicia transferencias y consulta su estado por identificador de operación" "HTTPS/JSON; Idempotency-Key" "Nivel2"

        plataforma.consultas -> core "Consulta datos del cliente, cuentas y movimientos (ACL; CL-3/CL-4)" "Protocolo del Core (por validar, S3)" "Nivel2"
        plataforma.consultas -> complementario "Consulta información extendida (ACL)" "Protocolo por validar" "Nivel2"
        plataforma.consultas -> plataforma.cache "Lee y escribe datos derivados (Cache-Aside)" "Protocolo de la caché" "Nivel2"
        plataforma.consultas -> plataforma.consultasDb "Registra eventos de auditoría en el outbox" "SQL/TLS" "Nivel2"
        plataforma.consultas -> plataforma.broker "Publica eventos de auditoría y consume eventos de invalidación" "Mensajería" "Nivel2"

        plataforma.vinculacion -> kyc "Verifica la identidad del solicitante" "HTTPS (API del proveedor)" "Nivel2"
        plataforma.vinculacion -> core "Busca y crea clientes (ACL; CL-1/CL-2)" "Protocolo del Core (por validar, S3)" "Nivel2"
        plataforma.vinculacion -> plataforma.idp "Crea y activa la cuenta digital vinculada (único contenedor autorizado)" "HTTPS (API de administración)" "Nivel2"
        plataforma.vinculacion -> plataforma.vinculacionDb "Persiste el estado y la evidencia de la vinculación" "SQL/TLS" "Nivel2"
        plataforma.vinculacion -> plataforma.broker "Publica eventos de estado y de auditoría (outbox)" "Mensajería" "Nivel2"

        plataforma.transferencias -> core "Ejecuta transferencias y concilia (ACL; CL-1/CL-2; permiso de envío único)" "Protocolo del Core (por validar, S3)" "Nivel2"
        plataforma.transferencias -> transferencias "Solicita el procesamiento interbancario (ruta por validar)" "Por validar (Q-01)" "Ruta por validar,Nivel2"
        plataforma.transferencias -> plataforma.transferenciasDb "Reserva intenciones y aplica transiciones cercadas" "SQL/TLS" "Nivel2"
        plataforma.transferencias -> plataforma.broker "Publica movimientos confirmados, cambios de estado y auditoría (outbox)" "Mensajería" "Nivel2"
        plataforma.transferencias -> plataforma.diarioDespacho "Anexa y confirma la autorización de despacho antes de cada envío (INV-DR-1)" "Por seleccionar (DD-23)" "Nivel2"
        plataforma.vinculacion -> plataforma.diarioDespacho "Anexa y confirma la autorización de despacho del alta de clientes" "Por seleccionar (DD-23)" "Nivel2"

        plataforma.idp -> plataforma.idpDb "Lee y escribe cuentas, factores y sesiones" "SQL/TLS" "Nivel2"
        plataforma.idp -> plataforma.broker "Publica eventos de seguridad" "Mensajería" "Nivel2"

        plataforma.notif -> plataforma.broker "Consume eventos notificables" "Mensajería" "Nivel2"
        plataforma.notif -> plataforma.notifDb "Deduplica y registra los intentos de entrega" "SQL/TLS" "Nivel2"
        plataforma.notif -> notificaciones "Envía notificaciones push, SMS y correo" "HTTPS (API de los proveedores)" "Nivel2"
        plataforma.notif -> plataforma.consultas "Obtiene los datos de contacto vigentes del destinatario" "HTTPS/JSON (API interna)" "Nivel2"
        plataforma.bff -> plataforma.broker "Consume eventos de seguridad para invalidar sesiones" "Mensajería" "Nivel2"

        plataforma.auditoria -> plataforma.broker "Consume eventos de auditoría" "Mensajería" "Nivel2"
        plataforma.auditoria -> plataforma.auditoriaDb "Anexa registros encadenados" "SQL/TLS" "Nivel2"

        operadorSoporte -> plataforma.consola "Resuelve casos de conciliación y revisión manual" "HTTPS" "Por validar,Nivel2"
        auditor -> plataforma.consola "Consulta el registro de auditoría" "HTTPS" "Por validar,Nivel2"
        plataforma.consola -> plataforma.transferencias "Resuelve casos de conciliación y cierres excepcionales (cuatro ojos)" "HTTPS/JSON (API de operaciones)" "Nivel2"
        plataforma.consola -> plataforma.vinculacion "Resuelve revisiones manuales de vinculación (cuatro ojos)" "HTTPS/JSON (API de operaciones)" "Nivel2"
        plataforma.consola -> plataforma.auditoria "Consulta el registro de auditoría" "HTTPS/JSON" "Nivel2"
        plataforma.consola -> idpCorporativo "Autentica al personal (federación)" "OpenID Connect o SAML" "Por validar,Nivel2"

        // ================================================================
        // Nivel 3 — Componentes (Fase 4). Solo contenedores autorizados por ARG-02-R.
        // El servicio de transferencias NO tiene componentes: diseño bloqueado (B-05).
        // ================================================================
        !element plataforma.bff {
            bffApi = component "API de canal" "Controladores HTTP por canal (web y móvil): validación de esquemas, correlación y propagación del plazo." "Módulo de entrada"
            bffSeguridad = component "Contexto de seguridad del canal" "Resuelve el principal desde la sesión web o desde el token móvil; comprueba la audiencia y los alcances." "Módulo de seguridad"
            bffSesion = component "Gestor de sesión web" "Cliente confidencial OIDC de la SPA: cookie __Host-, anti-CSRF, renovación y rotación de tokens, cierre de sesión e invalidación (ADR-0019)." "Módulo de seguridad"
            bffComposicion = component "Composición de lecturas" "Tablero y movimientos: combina consultas con las operaciones en curso; degradación explícita si no puede verificarlas (ARG02-M06)." "Caso de uso de canal"
            bffRelevoTransferencias = component "Relevo de transferencias" "Retransmite el inicio y la consulta de estado por identificador de operación; propaga Idempotency-Key sin cambios; sin reintentos (PT-04)." "Caso de uso de canal"
            bffVinculacion = component "Adaptador del flujo de vinculación" "Adapta al canal móvil las etapas de vinculación; sin reglas de negocio." "Caso de uso de canal"
            bffDispositivos = component "Relevo de registro de dispositivos" "Registra los tokens de dispositivo y las preferencias de notificación de la app." "Caso de uso de canal"
            bffClientes = component "Clientes de servicios de dominio" "Llamadas HTTP con timeouts, plazo propagado y Circuit Breaker por servicio (SDK, ADR-0003)." "Adaptador de salida"
            bffEventosSeguridad = component "Consumidor de eventos de seguridad" "Invalida las sesiones de un sujeto ante cambios de credenciales, bloqueos o revocaciones." "Adaptador de entrada (eventos)"
            bffPublicador = component "Publicador de eventos de sesión" "Publica los eventos de inicio y cierre de sesión (A7) con confirmación." "Adaptador de salida (eventos)"
        }
        !element plataforma.consultas {
            conApi = component "API de consultas" "Endpoints de canal para el perfil, las cuentas, los saldos y los movimientos; paginación y validación." "Módulo de entrada"
            conApiContactos = component "API interna de contactos" "Datos de contacto vigentes del destinatario para el servicio de notificaciones; solo autenticación entre servicios." "Módulo de entrada"
            conAutorizacion = component "Autorización de acceso a recursos" "Verifica que cada cuenta o recurso pertenezca al cliente autenticado (protección contra el acceso a objetos ajenos)." "Política de dominio"
            conPerfil = component "Caso de uso: perfil del cliente" "Combina datos básicos del Core y extendidos del Sistema Complementario." "Caso de uso"
            conCuentas = component "Caso de uso: cuentas, saldos y movimientos" "Lecturas con frescura por categoría; read-your-writes; indicador de antigüedad." "Caso de uso"
            conContactos = component "Caso de uso: datos de contacto" "Contactos vigentes para notificaciones, con minimización." "Caso de uso"
            conFrescura = component "Política de frescura" "Matriz de cacheabilidad, TTL de seguridad gobernados y ventana read-your-writes (ADR-0004)." "Política de dominio"
            conCache = component "Repositorio Cache-Aside" "Lectura y escritura en la caché con marcas de invalidación contra la carrera de llenado." "Adaptador de salida"
            conInvalidacion = component "Consumidor de invalidaciones" "Aplica marcas de invalidación ante eventos de movimientos confirmados." "Adaptador de entrada (eventos)"
            conAclClientes = component "ACL de clientes (lectura)" "Traduce el modelo de cliente del Core; clasificación de errores." "ACL"
            conAclMovimientos = component "ACL de movimientos" "Traduce cuentas, saldos y movimientos del Core; paginación." "ACL"
            conAclComplementario = component "ACL del Sistema Complementario" "Traduce los datos extendidos y los contactos." "ACL"
            conAdmision = component "Control de admisión hacia el Core" "Cuotas CL-3 y CL-4 por réplica, bulkheads y presión de retorno (ADR-0011, RI-01 a RI-08)." "Política técnica"
            conAuditoria = component "Registrador de auditoría de doble ruta" "Eventos A4 y A5: publicación confirmada en el broker o outbox local; responde solo con una ruta confirmada (ADR-0020)." "Adaptador de salida"
        }
        !element plataforma.vinculacion {
            vinApi = component "API de vinculación" "Endpoints del canal móvil para iniciar y avanzar la vinculación; Idempotency-Key obligatoria (ADR-0006)." "Módulo de entrada"
            vinApiOperaciones = component "API de operaciones" "Revisión manual con cuatro ojos; no puede asignar CREATED_BY_THIS_REQUEST sin evidencia." "Módulo de entrada"
            vinAplicacion = component "Servicio de aplicación de vinculación" "Orquesta las etapas E1 a E6 y los límites transaccionales; sin lógica de integración." "Caso de uso"
            vinDominio = component "Modelo de dominio de vinculación" "Estados, transiciones válidas e invariantes INV-O1 a INV-O6." "Dominio"
            vinPolitica = component "Política de autorización de vinculación" "Reglas VD-01 a VD-10; ruta PRE_EXISTING deshabilitada hasta DD-21 (falla cerrada)." "Política de dominio"
            vinKyc = component "ACL del proveedor KYC/biométrico" "Traduce resultados del proveedor; la indisponibilidad no es un rechazo (VD-09)." "ACL"
            vinBusqueda = component "ACL de búsqueda de clientes" "Búsqueda por identificación en el Core (CL-2, solo lectura)." "ACL"
            vinAlta = component "Ejecutor del alta en el Core (BLOQUEADO)" "Frontera del tramo CUSTOMER_CREATION: permiso único, diario y recuperación. Diseño bloqueado (B-05, ARG02-C01)." "Frontera bloqueada" {
                tags "Bloqueado"
            }
            vinFirmante = component "Firmante de afirmaciones de vinculación" "Firma con el gestor de claves la afirmación que exige la operación de vinculación del IdP (ADR-0013)." "Adaptador de salida"
            vinIdp = component "Adaptador de la operación de vinculación del IdP" "Invoca la operación acotada de creación y activación de la cuenta digital." "Adaptador de salida"
            vinRepo = component "Repositorio de vinculación" "Escrituras condicionales (CAS), unicidad de claves y outbox en la misma transacción." "Adaptador de persistencia"
            vinRele = component "Relé del outbox" "Publica en el broker los eventos de estado y de auditoría (A3)." "Adaptador de salida (eventos)"
        }
        !element plataforma.notif {
            notConsumidor = component "Consumidor de eventos notificables" "Deduplica por identificador de evento y ordena por secuencia de la entidad; descarta estados anteriores (ARG02-M01)." "Adaptador de entrada (eventos)"
            notApiDispositivos = component "API de registro de dispositivos" "Alta y baja de tokens de dispositivo y preferencias de canal." "Módulo de entrada"
            notPolitica = component "Política de notificación" "Qué evento se notifica, por qué canales (al menos dos para movimientos, R4) y con qué prioridad." "Política de dominio"
            notDestinatarios = component "Resolvedor de destinatarios" "Contactos vigentes desde el servicio de consultas y tokens de dispositivo propios." "Caso de uso"
            notEntrega = component "Orquestador de entrega" "Intentos, reintentos acotados, canal alternativo y Circuit Breaker por proveedor." "Caso de uso"
            notProveedores = component "Adaptadores de proveedores" "Push, SMS y correo; contenido minimizado." "Adaptador de salida"
            notRepo = component "Repositorio de notificaciones" "Eventos procesados, intentos, dispositivos y preferencias; outbox." "Adaptador de persistencia"
            notRele = component "Relé del outbox" "Publica los eventos de auditoría de los intentos de entrega." "Adaptador de salida (eventos)"
        }
        !element plataforma.auditoria {
            audConsumidor = component "Consumidor de eventos de auditoría" "Recibe los eventos de todas las fuentes (al menos una vez)." "Adaptador de entrada (eventos)"
            audValidador = component "Validador y clasificador" "Valida el esquema, la categoría A1 a A8 y la minimización." "Política de dominio"
            audDeduplicador = component "Deduplicador" "Descarta los eventos ya registrados por identificador antes de encadenar." "Caso de uso"
            audAnexador = component "Anexador encadenado por partición" "Anexa con encadenamiento de hashes por fuente y partición (ADR-0020)." "Caso de uso"
            audHuecos = component "Detector de huecos" "Detecta huecos de secuencia por fuente y genera alertas." "Caso de uso"
            audAnclaje = component "Anclaje en archivo inmutable" "Ancla periódicamente las cabezas de las cadenas en el archivo inmutable." "Caso de uso"
            audApi = component "API de consulta de auditoría" "Solo lectura para roles de auditoría; cada acceso se audita (A6)." "Módulo de entrada"
            audRepo = component "Repositorio de auditoría" "Tabla de solo anexión y archivo inmutable." "Adaptador de persistencia"
        }

        // Relaciones de componentes — BFF
        plataforma.gateway -> plataforma.bff.bffApi "Enruta solicitudes de canal" "HTTPS/JSON"
        plataforma.bff.bffApi -> plataforma.bff.bffSeguridad "Resuelve el principal"
        plataforma.bff.bffSeguridad -> plataforma.bff.bffSesion "Obtiene la sesión web y el token vigente"
        plataforma.bff.bffSesion -> plataforma.bffSesiones "Lee y escribe sesiones cifradas" "Protocolo del almacén (TLS)"
        plataforma.bff.bffSesion -> plataforma.idp "Intercambia códigos, renueva y revoca tokens" "HTTPS (OAuth 2.0)"
        plataforma.bff.bffSesion -> plataforma.bff.bffPublicador "Emite eventos de sesión"
        plataforma.bff.bffPublicador -> plataforma.broker "Publica con confirmación" "Mensajería"
        plataforma.bff.bffEventosSeguridad -> plataforma.broker "Consume eventos de seguridad" "Mensajería"
        plataforma.bff.bffEventosSeguridad -> plataforma.bff.bffSesion "Invalida las sesiones del sujeto"
        plataforma.bff.bffApi -> plataforma.bff.bffComposicion "Solicita vistas compuestas"
        plataforma.bff.bffApi -> plataforma.bff.bffRelevoTransferencias "Retransmite comandos y consultas de estado"
        plataforma.bff.bffApi -> plataforma.bff.bffVinculacion "Retransmite pasos de vinculación"
        plataforma.bff.bffApi -> plataforma.bff.bffDispositivos "Retransmite registros de dispositivo"
        plataforma.bff.bffComposicion -> plataforma.bff.bffClientes "Lee consultas y operaciones en curso"
        plataforma.bff.bffRelevoTransferencias -> plataforma.bff.bffClientes "Invoca el servicio de transferencias"
        plataforma.bff.bffVinculacion -> plataforma.bff.bffClientes "Invoca el servicio de vinculación"
        plataforma.bff.bffDispositivos -> plataforma.bff.bffClientes "Invoca el servicio de notificaciones"
        plataforma.bff.bffClientes -> plataforma.consultas "Consultas" "HTTPS/JSON"
        plataforma.bff.bffClientes -> plataforma.transferencias "Inicio y estado de transferencias" "HTTPS/JSON; Idempotency-Key"
        plataforma.bff.bffClientes -> plataforma.vinculacion "Pasos de vinculación" "HTTPS/JSON; Idempotency-Key"
        plataforma.bff.bffClientes -> plataforma.notif "Registro de dispositivos" "HTTPS/JSON"

        // Relaciones de componentes — Consultas
        plataforma.bff -> plataforma.consultas.conApi "Consulta cliente, cuentas y movimientos" "HTTPS/JSON"
        plataforma.notif -> plataforma.consultas.conApiContactos "Obtiene contactos vigentes" "HTTPS/JSON (interna)"
        plataforma.consultas.conApi -> plataforma.consultas.conAutorizacion "Verifica la propiedad del recurso"
        plataforma.consultas.conApi -> plataforma.consultas.conPerfil "Ejecuta"
        plataforma.consultas.conApi -> plataforma.consultas.conCuentas "Ejecuta"
        plataforma.consultas.conApiContactos -> plataforma.consultas.conContactos "Ejecuta"
        plataforma.consultas.conAutorizacion -> plataforma.consultas.conAclClientes "Obtiene las relaciones cliente-cuenta"
        plataforma.consultas.conPerfil -> plataforma.consultas.conFrescura "Aplica la política"
        plataforma.consultas.conCuentas -> plataforma.consultas.conFrescura "Aplica la política"
        plataforma.consultas.conFrescura -> plataforma.consultas.conCache "Lee o llena según la categoría"
        plataforma.consultas.conCache -> plataforma.cache "Lee y escribe; marcas de invalidación" "Protocolo de la caché"
        plataforma.consultas.conInvalidacion -> plataforma.broker "Consume movimientos confirmados" "Mensajería"
        plataforma.consultas.conInvalidacion -> plataforma.consultas.conCache "Marca entradas invalidadas"
        plataforma.consultas.conPerfil -> plataforma.consultas.conAclClientes "Datos básicos"
        plataforma.consultas.conPerfil -> plataforma.consultas.conAclComplementario "Datos extendidos"
        plataforma.consultas.conCuentas -> plataforma.consultas.conAclMovimientos "Cuentas, saldos y movimientos"
        plataforma.consultas.conContactos -> plataforma.consultas.conAclComplementario "Contactos"
        plataforma.consultas.conAclClientes -> plataforma.consultas.conAdmision "Solicita cupo CL-3"
        plataforma.consultas.conAclMovimientos -> plataforma.consultas.conAdmision "Solicita cupo CL-3/CL-4"
        plataforma.consultas.conAclClientes -> core "Consulta clientes" "Protocolo del Core (S3)"
        plataforma.consultas.conAclMovimientos -> core "Consulta cuentas y movimientos" "Protocolo del Core (S3)"
        plataforma.consultas.conAclComplementario -> complementario "Consulta datos extendidos y contactos" "Protocolo por validar"
        plataforma.consultas.conApi -> plataforma.consultas.conAuditoria "Registra A4/A5 antes de responder"
        plataforma.consultas.conApiContactos -> plataforma.consultas.conAuditoria "Registra el acceso a contactos (A4)"
        plataforma.consultas.conAuditoria -> plataforma.broker "Ruta 1: publicación confirmada" "Mensajería"
        plataforma.consultas.conAuditoria -> plataforma.consultasDb "Ruta 2: outbox local" "SQL/TLS"

        // Relaciones de componentes — Vinculación
        plataforma.bff -> plataforma.vinculacion.vinApi "Gestiona solicitudes de vinculación" "HTTPS/JSON"
        plataforma.consola -> plataforma.vinculacion.vinApiOperaciones "Resuelve revisiones manuales" "HTTPS/JSON"
        plataforma.vinculacion.vinApi -> plataforma.vinculacion.vinAplicacion "Ejecuta"
        plataforma.vinculacion.vinApiOperaciones -> plataforma.vinculacion.vinAplicacion "Ejecuta resoluciones"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinDominio "Aplica transiciones"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinPolitica "Evalúa la autorización de la vinculación"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinKyc "E1: verificación de identidad"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinBusqueda "E2: identificación del cliente"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinAlta "E3: alta (frontera bloqueada)"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinFirmante "Firma la afirmación de vinculación"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinIdp "E5: activa el acceso digital"
        plataforma.vinculacion.vinAplicacion -> plataforma.vinculacion.vinRepo "Persiste estado, evidencia y outbox"
        plataforma.vinculacion.vinKyc -> kyc "Verifica la identidad" "HTTPS (API del proveedor)"
        plataforma.vinculacion.vinBusqueda -> core "Busca por identificación (CL-2)" "Protocolo del Core (S3)"
        plataforma.vinculacion.vinAlta -> core "Alta del cliente (CL-1; bloqueado)" "Protocolo del Core (S3)"
        plataforma.vinculacion.vinAlta -> plataforma.diarioDespacho "Anexa la autorización de despacho (bloqueado)" "DD-23"
        plataforma.vinculacion.vinIdp -> plataforma.idp "Operación de vinculación acotada con afirmación firmada" "HTTPS"
        plataforma.vinculacion.vinRepo -> plataforma.vinculacionDb "CAS, unicidad y outbox" "SQL/TLS"
        plataforma.vinculacion.vinRele -> plataforma.vinculacionDb "Lee el outbox" "SQL/TLS"
        plataforma.vinculacion.vinRele -> plataforma.broker "Publica eventos" "Mensajería"

        // Relaciones de componentes — Notificaciones
        plataforma.bff -> plataforma.notif.notApiDispositivos "Registra dispositivos y preferencias" "HTTPS/JSON"
        plataforma.notif.notConsumidor -> plataforma.broker "Consume eventos notificables" "Mensajería"
        plataforma.notif.notConsumidor -> plataforma.notif.notRepo "Deduplica y controla la secuencia"
        plataforma.notif.notConsumidor -> plataforma.notif.notPolitica "Evalúa el evento"
        plataforma.notif.notPolitica -> plataforma.notif.notDestinatarios "Resuelve destinatarios y canales"
        plataforma.notif.notDestinatarios -> plataforma.consultas "Obtiene contactos vigentes" "HTTPS/JSON (interna)"
        plataforma.notif.notDestinatarios -> plataforma.notif.notRepo "Lee tokens de dispositivo y preferencias"
        plataforma.notif.notPolitica -> plataforma.notif.notEntrega "Solicita la entrega"
        plataforma.notif.notEntrega -> plataforma.notif.notProveedores "Envía por canal"
        plataforma.notif.notEntrega -> plataforma.notif.notRepo "Registra intentos y outbox"
        plataforma.notif.notProveedores -> notificaciones "Entrega push, SMS y correo" "HTTPS (API de los proveedores)"
        plataforma.notif.notApiDispositivos -> plataforma.notif.notRepo "Registra dispositivos"
        plataforma.notif.notRepo -> plataforma.notifDb "Lee y escribe" "SQL/TLS"
        plataforma.notif.notRele -> plataforma.notifDb "Lee el outbox" "SQL/TLS"
        plataforma.notif.notRele -> plataforma.broker "Publica eventos de auditoría" "Mensajería"

        // Relaciones de componentes — Auditoría
        plataforma.consola -> plataforma.auditoria.audApi "Consulta el registro (solo lectura)" "HTTPS/JSON"
        plataforma.auditoria.audConsumidor -> plataforma.broker "Consume eventos de auditoría" "Mensajería"
        plataforma.auditoria.audConsumidor -> plataforma.auditoria.audValidador "Valida y clasifica"
        plataforma.auditoria.audValidador -> plataforma.auditoria.audDeduplicador "Entrega eventos válidos"
        plataforma.auditoria.audDeduplicador -> plataforma.auditoria.audAnexador "Entrega eventos nuevos"
        plataforma.auditoria.audAnexador -> plataforma.auditoria.audRepo "Anexa con hash encadenado"
        plataforma.auditoria.audHuecos -> plataforma.auditoria.audRepo "Verifica secuencias por fuente"
        plataforma.auditoria.audAnclaje -> plataforma.auditoria.audRepo "Ancla las cabezas de las cadenas"
        plataforma.auditoria.audApi -> plataforma.auditoria.audRepo "Consulta"
        plataforma.auditoria.audRepo -> plataforma.auditoriaDb "Solo anexión y archivo inmutable" "SQL/TLS; almacenamiento de objetos"
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
            include solicitante plataforma.app plataforma.gateway plataforma.bff plataforma.vinculacion plataforma.vinculacionDb plataforma.diarioDespacho plataforma.idp plataforma.broker plataforma.consola kyc core
            autolayout lr 250 150
        }

        container plataforma "Contenedores-Transferencias" "Vista focalizada: transferencias. El orquestador es PROVISIONAL (CF-01/CF-02 pendientes de verificación independiente)." {
            title "C4 Nivel 2 — Transferencias (provisional)"
            include cliente plataforma.spa plataforma.app plataforma.gateway plataforma.bff plataforma.bffSesiones plataforma.transferencias plataforma.transferenciasDb plataforma.diarioDespacho plataforma.broker plataforma.consola core transferencias
            autolayout lr 250 150
        }

        container plataforma "Contenedores-NotificacionesAuditoria" "Vista focalizada: eventos, notificaciones y auditoría." {
            title "C4 Nivel 2 — Notificaciones y auditoría"
            include plataforma.bff plataforma.consultas plataforma.vinculacion plataforma.transferencias plataforma.idp plataforma.broker plataforma.notif plataforma.notifDb plataforma.auditoria plataforma.auditoriaDb plataforma.consola notificaciones cliente
            autolayout lr 250 150
        }

        container plataforma "Contenedores-IntegracionCore" "Vista focalizada: contenedores que consumen la capacidad del Core (ADR-0011) y sistemas externos de integración." {
            title "C4 Nivel 2 — Integración con el Core y sistemas existentes"
            include plataforma.consultas plataforma.vinculacion plataforma.transferencias core complementario transferencias kyc
            autolayout lr 250 150
        }


        component plataforma.bff "Componentes-BFF" "C4 Nivel 3 — BFF de canales digitales." {
            title "C4 Nivel 3 — BFF de canales digitales"
            include *
            exclude "relationship.tag==Nivel2"
            autolayout lr 200 120
        }
        component plataforma.consultas "Componentes-Consultas" "C4 Nivel 3 — Servicio de consultas." {
            title "C4 Nivel 3 — Servicio de consultas"
            include *
            exclude "relationship.tag==Nivel2"
            autolayout lr 200 120
        }
        component plataforma.vinculacion "Componentes-Vinculacion" "C4 Nivel 3 — Servicio de vinculación digital. El ejecutor del alta es una frontera BLOQUEADA (B-05)." {
            title "C4 Nivel 3 — Servicio de vinculación digital"
            include *
            exclude "relationship.tag==Nivel2"
            autolayout lr 200 120
        }
        component plataforma.notif "Componentes-Notificaciones" "C4 Nivel 3 — Servicio de notificaciones." {
            title "C4 Nivel 3 — Servicio de notificaciones"
            include *
            exclude "relationship.tag==Nivel2"
            autolayout lr 200 120
        }
        component plataforma.auditoria "Componentes-Auditoria" "C4 Nivel 3 — Servicio de auditoría." {
            title "C4 Nivel 3 — Servicio de auditoría"
            include *
            exclude "relationship.tag==Nivel2"
            autolayout lr 200 120
        }

        // Servicio de transferencias: SIN vista de componentes (B-05; ARG02-C01, CF-01, CF-02).
        // TODO (Fase de infraestructura): deployment plataforma "Producción" "Despliegue" { ... }

        terminology {
            person "Persona"
            softwareSystem "Sistema de software"
            container "Contenedor"
            component "Componente"
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
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Bloqueado" {
                background #f4cccc
                color #000000
                stroke #990000
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
